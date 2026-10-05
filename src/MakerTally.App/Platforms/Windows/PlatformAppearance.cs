using Microsoft.Maui.Handlers;
using Thickness = Microsoft.UI.Xaml.Thickness;
using CornerRadius = Microsoft.UI.Xaml.CornerRadius;
using SolidColorBrush = Microsoft.UI.Xaml.Media.SolidColorBrush;

namespace MakerTally.App;

internal static class PlatformAppearance
{
    public static void Configure()
    {
        EntryHandler.Mapper.AppendToMapping("MakerTally", (handler, _) =>
        {
            handler.PlatformView.BorderThickness = new Thickness(0);
            handler.PlatformView.Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent);
            handler.PlatformView.Padding = new Thickness(0);
        });
        PickerHandler.Mapper.AppendToMapping("MakerTally", (handler, _) =>
        {
            handler.PlatformView.BorderThickness = new Thickness(0);
            handler.PlatformView.Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent);
            handler.PlatformView.CornerRadius = new CornerRadius(10);
        });
    }
}
