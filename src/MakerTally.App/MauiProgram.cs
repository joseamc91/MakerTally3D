using Microsoft.Extensions.DependencyInjection;
using MakerTally.Core;
using MakerTally.App.Presentation.Services;
using MakerTally.App.Presentation.ViewModels;
using MakerTally.App.Services;
namespace MakerTally.App;
public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        PlatformAppearance.Configure();
        var builder = MauiApp.CreateBuilder();
        builder.UseMauiApp<App>().ConfigureFonts(fonts =>
        {
            fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
            fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
        });
        builder.Services.AddSingleton<IAppDataStorage>(_ => new JsonDataStore(DataDirectoryProvider.DirectoryPath));
        builder.Services.AddSingleton(sp => sp.GetRequiredService<IAppDataStorage>().Load());
        builder.Services.AddSingleton(LocalizationService.Instance);
        builder.Services.AddSingleton<PageDialogs>();
        builder.Services.AddSingleton<IUserDialogs>(sp => sp.GetRequiredService<PageDialogs>());
        builder.Services.AddSingleton(sp => new MainViewModel(sp.GetRequiredService<IAppDataStorage>(),
            sp.GetRequiredService<LoadedData>(), sp.GetRequiredService<IUserDialogs>(),
            sp.GetRequiredService<LocalizationService>(), action => MainThread.BeginInvokeOnMainThread(action)));
        builder.Services.AddSingleton<MainPage>();
        return builder.Build();
    }
}
