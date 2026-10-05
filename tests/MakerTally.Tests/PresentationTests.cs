using System.IO;
using MakerTally.Core;
using MakerTally.App.Presentation.Services;
using MakerTally.App.Presentation.ViewModels;

namespace MakerTally.Tests;

public sealed class PresentationTests : IDisposable
{
    private readonly string _directory = Path.Combine(Path.GetTempPath(), "MakerTally.Alpha3.Tests", Guid.NewGuid().ToString());
    private readonly LocalizationService _language = new();
    private JsonDataStore Store => new(_directory);

    [Fact]
    public async Task SharedPresentationPreservesCrudCultureAndValidationWithAlpha3Calculations()
    {
        _language.SetLanguage("es-ES");
        var dialogs = new FakeDialogs();
        using var vm = new MainViewModel(Store, Store.Load(), dialogs, _language);
        Assert.True(vm.HasCalculation); Assert.Equal("ASA eSun", vm.SelectedFilament?.Name);
        Assert.Equal(9.35m, vm.Result?.SuggestedSalePrice); Assert.Equal(5.24m, vm.Result?.GrossMargin);
        Assert.Contains("9,35", vm.SalePrice);
        foreach (string invalid in new[] { "", ",", ".", "-", "abc" })
        {
            vm.Weight.Text = invalid;
            Assert.True(vm.Weight.HasErrors); Assert.False(vm.HasCalculation);
            Assert.Equal("—", vm.SalePrice);
        }
        vm.Weight.Text = "141.0"; Assert.True(vm.HasCalculation);
        vm.Weight.Text = "141,0"; Assert.True(vm.HasCalculation);
        vm.Minutes.Text = "59"; vm.Hours.Text = decimal.MaxValue.ToString(System.Globalization.CultureInfo.InvariantCulture);
        Assert.False(vm.HasCalculation); vm.Hours.Text = "6"; vm.Minutes.Text = "0";

        vm.SettingsCommand.Execute(null); Assert.True(vm.IsSettings);
        vm.SelectedLanguage = "en-US";
        Assert.Equal("Calculator", vm.L["Calculator"]);
        Assert.Equal("€9.35", vm.SalePrice); Assert.Equal("0.1349", vm.ElectricityPrice.Text);
        vm.MachineRate.Text = "0,75";
        vm.DecreaseMultiplierCommand.Execute(null);
        vm.FlushSettings();
        Assert.Equal(2.5m, vm.SaleMultiplier); Assert.Equal(2.5m, Store.Load().Settings.DefaultSaleMultiplier);
        Assert.Equal(0.75m, Store.Load().Settings.MachineCostPerHour); Assert.Equal("en-US", Store.Load().Settings.Language);
        vm.MachineRate.Text = "-"; Assert.False(vm.HasCalculation);
        vm.FlushSettings(); Assert.Equal(0.75m, Store.Load().Settings.MachineCostPerHour);
        vm.MachineRate.Text = "0.25";

        var editor = new FilamentEditorViewModel(null, _language);
        Assert.False(editor.SaveCommand.CanExecute(null));
        editor.Brand = "Test brand"; editor.MaterialType = "PLA";
        editor.Weight.Text = "500"; editor.Price.Text = "8,5"; editor.Power.Text = "100";
        Assert.Equal("€17.00/kg", editor.PricePerKg); Assert.True(editor.IsValid);
        dialogs.Edited = editor.Build();
        await vm.AddCommand.ExecuteAsync();
        Assert.Equal(8, Store.Load().Filaments.Count);
        var added = vm.Filaments.Single(f => f.DisplayName == "PLA · Test brand");
        Assert.Contains(added, vm.ActiveFilaments); vm.SelectedFilament = added; vm.InventorySelection = added;
        dialogs.Edited = added with { Name = "Changed filament", IsActive = false, Notes = "Saved note" };
        await vm.EditCommand.ExecuteAsync();
        Assert.DoesNotContain(vm.ActiveFilaments, f => f.Id == added.Id);
        var edited = Store.Load().Filaments.Single(f => f.Id == added.Id);
        Assert.Equal("Changed filament", edited.Name); Assert.Equal("Saved note", edited.Notes); Assert.False(edited.IsActive);
        dialogs.ConfirmDelete = false;
        await vm.DeleteCommand.ExecuteAsync(); Assert.Equal(8, vm.Filaments.Count);
        dialogs.ConfirmDelete = true;
        await vm.DeleteCommand.ExecuteAsync(); Assert.Equal(7, Store.Load().Filaments.Count);
        Assert.Null(vm.InventorySelection);

        vm.SelectedLanguage = "es-ES";
        Assert.Contains("0,1349", vm.ElectricitySummary); Assert.Equal("Calculadora", vm.L["Calculator"]);
        vm.Minutes.Text = "30"; Assert.True(vm.HasCalculation); Assert.Equal(0.78m, vm.Result?.PrintingEnergyKWh);
        Store.SaveFilaments([]);
        using var empty = new MainViewModel(Store, Store.Load(), dialogs, _language);
        Assert.Empty(empty.ActiveFilaments); Assert.False(empty.HasCalculation);
    }

    [Fact] public void TogglingActivePersistsAndRefreshesCalculatorAndCards()
    {
        using var vm = new MainViewModel(Store, Store.Load(), new FakeDialogs(), _language);
        var original = vm.SelectedFilament!;
        vm.InventorySelection = original; vm.ToggleActiveCommand.Execute(null);
        Assert.DoesNotContain(vm.ActiveFilaments, f => f.Id == original.Id);
        Assert.False(Store.Load().Filaments.Single(f => f.Id == original.Id).IsActive);
        Assert.Equal("Inactivo", vm.InventoryCards.Single(c => c.Profile.Id == original.Id).Status);
        vm.ToggleActiveCommand.Execute(null);
        Assert.Contains(vm.ActiveFilaments, f => f.Id == original.Id);
        Assert.True(Store.Load().Filaments.Single(f => f.Id == original.Id).IsActive);
    }

    [Fact] public void InvalidEditorInputsCannotSave()
    {
        var editor = new FilamentEditorViewModel(null, _language) { MaterialType = "PLA", Brand = "Brand" };
        editor.Weight.Text = "0"; Assert.False(editor.IsValid);
        editor.Weight.Text = "1000"; editor.Price.Text = "-1"; Assert.False(editor.IsValid);
        editor.Price.Text = "0"; editor.Power.Text = "-1"; Assert.False(editor.IsValid);
        editor.Power.Text = "0"; Assert.True(editor.IsValid);
        editor.MaterialType = "  "; Assert.False(editor.IsValid);
        editor.MaterialType = "PLA";
        editor.Weight.Text = "0.0000000000000000000000000001";
        editor.Price.Text = decimal.MaxValue.ToString(System.Globalization.CultureInfo.InvariantCulture);
        Assert.False(editor.IsValid); Assert.Equal("—", editor.PricePerKg);
    }

    [Fact] public void LanguageResourcesHaveMatchingKeysAndCorrectCulture()
    {
        _language.SetLanguage("es-ES");
        var keys = _language.Keys.Order().ToArray();
        Assert.Contains("17,50", _language.Currency(17.5m));
        Assert.Contains("0,1349", _language.Currency(0.1349m, 4));
        _language.SetLanguage("en-US");
        Assert.Equal(keys, _language.Keys.Order().ToArray());
        Assert.Equal("€17.50", _language.Currency(17.5m)); Assert.Equal("€0.1349", _language.Currency(0.1349m, 4));
        Assert.Equal("v0.1-alpha4", _language["Version"]);
        Assert.Throws<ArgumentOutOfRangeException>(() => _language.SetLanguage("fr-FR"));
    }

    [Fact] public void ChangingLanguageNotifiesAllBindingsIncludingMauiIndexers()
    {
        var notifications = new List<string?>();
        _language.PropertyChanged += (_, e) => notifications.Add(e.PropertyName);
        _language.SetLanguage("en-US");
        Assert.Contains(null, notifications);
        Assert.Equal("Settings", _language["Settings"]);
        _language.SetLanguage("es-ES");
        Assert.Equal("Ajustes", _language["Settings"]);
    }

    [Fact] public void NumericErrorsTranslateWithoutLosingIncompleteInput()
    {
        var field = new NumericField(1, language: _language) { Text = "," };
        Assert.True(field.HasErrors); Assert.Contains("Introduce", field.ErrorText);
        _language.SetLanguage("en-US"); field.RefreshLanguage();
        Assert.Equal(",", field.Text); Assert.Contains("Enter", field.ErrorText);
    }
    [Fact] public void SaveFailureShowsLocalizedNoticeAndKeepsInventoryUnchanged()
    {
        var loaded = Store.Load();
        var unavailable = new FailingStorage(Store.DirectoryPath, loaded);
        using var vm = new MainViewModel(unavailable, loaded, new FakeDialogs(), _language);
        vm.InventorySelection = vm.Filaments[0]; vm.ToggleActiveCommand.Execute(null);
        Assert.True(vm.Filaments[0].IsActive); Assert.True(vm.HasStorageMessage);
        Assert.Contains("filaments.json", vm.StorageMessage);
        vm.MachineRate.Text = "0.7"; vm.FlushSettings();
        Assert.Contains("No se", vm.SettingsStatus);
    }
    public void Dispose() { if (Directory.Exists(_directory)) Directory.Delete(_directory, true); }

    private sealed class FakeDialogs : IUserDialogs
    {
        public FilamentProfile? Edited { get; set; }
        public bool ConfirmDelete { get; set; }
        public Task<FilamentProfile?> EditFilamentAsync(FilamentProfile? original) => Task.FromResult(Edited);
        public Task<bool> ConfirmDeleteAsync(string name) => Task.FromResult(ConfirmDelete);
        public Task<FilamentAction> ChooseFilamentActionAsync(FilamentProfile filament) => Task.FromResult(FilamentAction.Cancel);
    }
    private sealed class FailingStorage(string path, LoadedData data) : IAppDataStorage
    {
        public string DirectoryPath => path;
        public LoadedData Load() => data;
        public void SaveSettings(AppSettings settings) => throw new IOException();
        public void SaveFilaments(IEnumerable<FilamentProfile> filaments) => throw new IOException();
    }
}
