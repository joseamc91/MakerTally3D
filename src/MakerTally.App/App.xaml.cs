using Microsoft.Extensions.DependencyInjection;
using MakerTally.Core;
using MakerTally.App.Presentation.Services;
using MakerTally.App.Presentation.ViewModels;
namespace MakerTally.App;
public partial class App : Application
{
    private readonly IServiceProvider _services;
    public App(IServiceProvider services)
    {
        InitializeComponent();
        ApplyTheme(services.GetRequiredService<LoadedData>().Settings.Theme);
        _services = services;
        LocalizationService.Instance.SetLanguage(services.GetRequiredService<LoadedData>().Settings.Language);
    }
    private void ApplyTheme(string theme)
    {
        UserAppTheme = theme switch
        {
            "Light" => AppTheme.Light, "Dark" => AppTheme.Dark, _ => AppTheme.Unspecified
        };
        ApplyPlatformTheme(UserAppTheme);
    }
    partial void ApplyPlatformTheme(AppTheme theme);
    protected override Window CreateWindow(IActivationState? activationState)
    {
        var page = _services.GetRequiredService<MainPage>();
        var window = new Window(page) { Title = LocalizationService.Instance.Get("AppName") };
        PlatformWindow.Configure(window);
        window.Created += (_, _) => ApplyPlatformTheme(UserAppTheme);
        var viewModel = _services.GetRequiredService<MainViewModel>();
        viewModel.ThemeChanged += (_, _) => ApplyTheme(viewModel.SelectedTheme);
        window.Stopped += (_, _) => viewModel.FlushSettings();
        window.Destroying += (_, _) => viewModel.Dispose();
        return window;
    }
}
