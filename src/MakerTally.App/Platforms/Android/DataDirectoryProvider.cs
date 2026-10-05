using MakerTally.App.Presentation.Services;
namespace MakerTally.App;
internal static class DataDirectoryProvider
{
    public static string DirectoryPath => DataDirectoryPolicy.Android(FileSystem.AppDataDirectory);
}
