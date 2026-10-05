using System.IO;
using MakerTally.Core;

namespace MakerTally.App.Presentation.Services;

public static class DataDirectoryPolicy
{
    public static string Windows(string localAppData) => Path.Combine(localAppData, DataDirectoryMigration.ActiveDirectoryName);
    public static string Android(string privateAppData) => privateAppData;
}
