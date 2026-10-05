using Microsoft.Maui.Handlers;
using Android.Content.Res;

namespace MakerTally.App;

internal static class PlatformAppearance
{
    public static void Configure()
    {
        EntryHandler.Mapper.AppendToMapping("MakerTally", (handler, _) => handler.PlatformView.BackgroundTintList = ColorStateList.ValueOf(Android.Graphics.Color.Transparent));
        PickerHandler.Mapper.AppendToMapping("MakerTally", (handler, _) => handler.PlatformView.BackgroundTintList = ColorStateList.ValueOf(Android.Graphics.Color.Transparent));
    }
}
