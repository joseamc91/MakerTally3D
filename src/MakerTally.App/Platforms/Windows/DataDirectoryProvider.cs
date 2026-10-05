using MakerTally.App.Presentation.Services;
using MakerTally.Core;
namespace MakerTally.App;
internal static class DataDirectoryProvider
{
    private static readonly Lazy<string> ActiveDirectory = new(() =>
    {
        string localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
        string destination = DataDirectoryPolicy.Windows(localAppData);
        DataDirectoryMigration.CopyMissingFiles(Path.Combine(localAppData, DataDirectoryMigration.LegacyDirectoryName), destination);
        return destination;
    });
    // Resolve before loading settings and before restoring native window placement.
    public static string DirectoryPath => ActiveDirectory.Value;
}
