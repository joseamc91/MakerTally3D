using System.Windows;
using PrintCost.Core;
using PrintCost.Desktop.Infrastructure;
using PrintCost.Desktop.ViewModels;

namespace PrintCost.Desktop;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        var store = new JsonDataStore();
        var data = store.Load();
        LocalizationService.Instance.SetLanguage(data.Settings.Language);
        var window = new MainWindow(new MainViewModel(store, data));
        MainWindow = window;
        window.Show();
    }
}
