using System.IO;
using System.Runtime.ExceptionServices;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Threading;
using PrintCost.Core;
using PrintCost.Desktop;
using PrintCost.Desktop.Infrastructure;
using PrintCost.Desktop.ViewModels;
using PrintCost.Desktop.Views;

namespace PrintCost.Tests;

public sealed class DesktopTests
{
    [Fact]
    public void WpfBindingsLanguageValidationPersistenceAndInventoryCommandsWork()
    {
        Exception? error = null;
        var thread = new Thread(() =>
        {
            var path = Path.Combine(Path.GetTempPath(), "PrintCost.Desktop.Tests", Guid.NewGuid().ToString());
            MainWindow? window = null;
            try
            {
                var app = new Application { ShutdownMode = ShutdownMode.OnExplicitShutdown };
                app.Resources.MergedDictionaries.Add(new ResourceDictionary
                {
                    Source = new Uri("/PrintCost;component/Resources/Theme.xaml", UriKind.Relative)
                });
                var language = LocalizationService.Instance;
                language.SetLanguage("es-ES");
                var store = new JsonDataStore(path);
                var vm = new MainViewModel(store, store.Load());
                window = new MainWindow(vm);
                app.MainWindow = window;
                window.Show();
                Pump();

                Assert.True(vm.HasCalculation);
                Assert.Equal("ASA eSun", vm.SelectedFilament?.Name);
                Assert.Equal(9.34767m, vm.Result?.SuggestedSalePrice);
                Assert.Contains("9,35", ((TextBlock)window.FindName("SuggestedPriceText")).Text);
                var weight = (TextBox)window.FindName("WeightInput");
                weight.Text = ""; Pump();
                Assert.True(vm.Weight.HasErrors); Assert.False(vm.HasCalculation); Assert.True(Validation.GetHasError(weight));
                weight.Text = ","; Pump(); Assert.False(vm.HasCalculation);
                weight.Text = "141.0"; Pump(); Assert.True(vm.HasCalculation);
                weight.Text = "141,0"; Pump(); Assert.True(vm.HasCalculation);
                vm.Multiplier.Text = decimal.MaxValue.ToString(System.Globalization.CultureInfo.InvariantCulture);
                Assert.False(vm.HasCalculation); vm.Multiplier.Text = "3";

                vm.SettingsCommand.Execute(null); Pump();
                Assert.Equal(Visibility.Visible, ((ScrollViewer)window.FindName("SettingsPage")).Visibility);
                var combo = (ComboBox)window.FindName("LanguageSelector");
                combo.SelectedValue = "en-US"; Pump();
                Assert.Equal("en-US", vm.SelectedLanguage);
                Assert.Equal("Calculator", ((RadioButton)window.FindName("CalculatorNav")).Content);
                Assert.Equal("Suggested sale price", language.Get("SalePrice"));
                Assert.Equal("€9.35", vm.SalePrice);
                Assert.Equal("0.1349", vm.ElectricityPrice.Text);
                Assert.Equal("en-US", window.Language.IetfLanguageTag, ignoreCase: true);

                vm.MachineRate.Text = "0,75"; Pump();
                vm.Multiplier.Text = "4";
                vm.DefaultMultiplier.Text = "2,5";
                vm.FlushSettings();
                Assert.Equal(4m, vm.Multiplier.Value);
                Assert.Equal(2.5m, store.Load().Settings.DefaultSaleMultiplier);
                Assert.Equal(0.75m, store.Load().Settings.MachineCostPerHour);
                Assert.Equal("en-US", store.Load().Settings.Language);
                vm.MachineRate.Text = "-"; Pump(); Assert.False(vm.HasCalculation);
                vm.FlushSettings(); Assert.Equal(0.75m, store.Load().Settings.MachineCostPerHour);
                vm.MachineRate.Text = "0.25";

                // Complete real modal commands on the WPF dispatcher with isolated test data.
                QueueModal<FilamentEditorWindow>(dialog =>
                {
                    var editor = (FilamentEditorViewModel)dialog.DataContext;
                    Assert.False(editor.SaveCommand.CanExecute(null));
                    editor.Name = "Test filament"; editor.Brand = "Test brand"; editor.MaterialType = "PLA";
                    editor.Weight.Text = "500"; editor.Price.Text = "8,5"; editor.Power.Text = "100";
                    Assert.Equal("€17.00/kg", editor.PricePerKg);
                    Assert.True(editor.SaveCommand.CanExecute(null));
                    editor.SaveCommand.Execute(null);
                });
                vm.AddCommand.Execute(null); Pump();
                Assert.Equal(8, store.Load().Filaments.Count);
                var added = vm.Filaments.Single(f => f.Name == "Test filament");
                Assert.Contains(added, vm.ActiveFilaments);
                vm.SelectedFilament = added; vm.InventorySelection = added;
                QueueModal<FilamentEditorWindow>(dialog =>
                {
                    var editor = (FilamentEditorViewModel)dialog.DataContext;
                    editor.Name = "Changed filament"; editor.IsActive = false; editor.Notes = "Saved note";
                    editor.SaveCommand.Execute(null);
                });
                vm.EditCommand.Execute(null); Pump();
                Assert.DoesNotContain(vm.ActiveFilaments, f => f.Id == added.Id);
                var edited = store.Load().Filaments.Single(f => f.Id == added.Id);
                Assert.Equal("Changed filament", edited.Name); Assert.Equal("Saved note", edited.Notes); Assert.False(edited.IsActive);
                QueueModal<DeleteConfirmationWindow>(dialog => dialog.DialogResult = false);
                vm.DeleteCommand.Execute(null); Assert.Equal(8, vm.Filaments.Count);
                QueueModal<DeleteConfirmationWindow>(dialog => dialog.DialogResult = true);
                vm.DeleteCommand.Execute(null); Pump(); Assert.Equal(7, store.Load().Filaments.Count);
                Assert.Null(vm.InventorySelection);

                vm.SelectedLanguage = "es-ES"; Pump();
                Assert.Contains("0,1349", vm.ElectricitySummary);
                Assert.Equal("Calculadora", ((RadioButton)window.FindName("CalculatorNav")).Content);
                vm.CalculatorCommand.Execute(null);
                window.Width = 820; window.Height = 560; Pump();
                Assert.True(window.IsVisible);
                var calculatorPage = (ScrollViewer)window.FindName("CalculatorPage");
                Assert.True(calculatorPage.ScrollableWidth > 0);
                var multiplierInput = (TextBox)window.FindName("MultiplierInput");
                multiplierInput.BringIntoView(); Pump();
                Assert.True(calculatorPage.VerticalOffset > 0);
                multiplierInput.Text = "3,25"; Pump();
                Assert.True(vm.HasCalculation);
                Assert.Equal(3.25m, vm.Multiplier.Value);
                vm.Filaments.Clear();
                // Reloading a deliberately empty inventory never reseeds the user's list.
                store.SaveFilaments([]);
                using var emptyVm = new MainViewModel(store, store.Load());
                Assert.Empty(emptyVm.ActiveFilaments); Assert.False(emptyVm.HasCalculation);
            }
            catch (Exception ex) { error = ex; }
            finally
            {
                window?.Close();
                if (Directory.Exists(path)) Directory.Delete(path, true);
            }
        });
        thread.SetApartmentState(ApartmentState.STA);
        thread.Start();
        Assert.True(thread.Join(TimeSpan.FromSeconds(30)), "WPF test timed out.");
        if (error is not null) ExceptionDispatchInfo.Capture(error).Throw();
    }

    private static void Pump() => Dispatcher.CurrentDispatcher.Invoke(() => { }, DispatcherPriority.ApplicationIdle);
    private static void QueueModal<T>(Action<T> action) where T : Window
        => Dispatcher.CurrentDispatcher.BeginInvoke(DispatcherPriority.ApplicationIdle, new Action(() =>
        {
            var dialog = Application.Current.Windows.OfType<T>().Single(w => w.IsVisible);
            try { action(dialog); }
            catch { dialog.Close(); throw; }
        }));
}
