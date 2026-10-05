using Microsoft.UI.Windowing;
using Windows.Graphics;

namespace MakerTally.App;
internal static class PlatformWindow
{
    public static void Configure(Window window)
    {
        window.Width = 450; window.Height = 760;
        window.MinimumWidth = 400; window.MinimumHeight = 560;
        window.Created += (_, _) => ConfigureNative(window);
    }
    private static void ConfigureNative(Window window)
    {
        if (window.Handler?.PlatformView is not Microsoft.UI.Xaml.Window native) return;
        var appWindow = native.AppWindow;
        var store = new WindowPlacementStore(Path.Combine(DataDirectoryProvider.DirectoryPath, "window.json"));
        var displays = DisplayArea.FindAll().OrderByDescending(d => d.IsPrimary).ToArray();
        if (displays.Length == 0) return;
        var areas = displays.Select(d => new WindowBounds(d.OuterBounds.X + d.WorkArea.X, d.OuterBounds.Y + d.WorkArea.Y, d.WorkArea.Width, d.WorkArea.Height)).ToArray();
        double scale = native.Content?.XamlRoot?.RasterizationScale ?? DeviceDisplay.Current.MainDisplayInfo.Density;
        int Pixels(int value) => (int)Math.Round(value * scale);
        var bounds = WindowPlacementPolicy.Restore(store.Load(), areas, Pixels(450), Pixels(760), Pixels(400), Pixels(560));
        appWindow.MoveAndResize(new RectInt32(bounds.X, bounds.Y, bounds.Width, bounds.Height));
        WindowBounds lastNormal = bounds;
        void Capture()
        {
            if (appWindow.Presenter is OverlappedPresenter { State: OverlappedPresenterState.Restored })
                lastNormal = new(appWindow.Position.X, appWindow.Position.Y, appWindow.Size.Width, appWindow.Size.Height);
        }
        void Changed(AppWindow sender, AppWindowChangedEventArgs args)
        {
            if (args.DidSizeChange || args.DidPositionChange) Capture();
        }
        appWindow.Changed += Changed;
        window.Destroying += (_, _) =>
        {
            // The last normal geometry survives minimization/maximization and native teardown.
            appWindow.Changed -= Changed;
            store.Save(lastNormal);
        };
    }
}
