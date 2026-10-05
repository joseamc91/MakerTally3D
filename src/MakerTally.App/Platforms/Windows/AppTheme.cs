namespace MakerTally.App;

public partial class App
{
    // MAUI palette bindings and WinUI native controls must use the same theme.
    partial void ApplyPlatformTheme(AppTheme theme)
    {
        foreach (var window in Windows)
        {
            if (window.Handler?.PlatformView is not Microsoft.UI.Xaml.Window nativeWindow) continue;
            if (nativeWindow.Content is Microsoft.UI.Xaml.FrameworkElement root)
                root.RequestedTheme = theme switch
                {
                    AppTheme.Light => Microsoft.UI.Xaml.ElementTheme.Light,
                    AppTheme.Dark => Microsoft.UI.Xaml.ElementTheme.Dark,
                    _ => Microsoft.UI.Xaml.ElementTheme.Default
                };
            if (Microsoft.UI.Windowing.AppWindowTitleBar.IsCustomizationSupported()) nativeWindow.AppWindow.TitleBar.PreferredTheme = theme switch
            {
                AppTheme.Light => Microsoft.UI.Windowing.TitleBarTheme.Light,
                AppTheme.Dark => Microsoft.UI.Windowing.TitleBarTheme.Dark,
                _ => Microsoft.UI.Windowing.TitleBarTheme.UseDefaultAppMode
            };
        }
    }
}
