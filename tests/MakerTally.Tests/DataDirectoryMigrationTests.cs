using MakerTally.App;
using MakerTally.Core;

namespace MakerTally.Tests;

public sealed class DataDirectoryMigrationTests : IDisposable
{
    private readonly string _root = Path.Combine(Path.GetTempPath(), "MakerTally.Migration.Tests", Guid.NewGuid().ToString("N"));
    private string Source => Path.Combine(_root, DataDirectoryMigration.LegacyDirectoryName);
    private string Destination => Path.Combine(_root, DataDirectoryMigration.ActiveDirectoryName);

    [Theory]
    [InlineData("settings.json")]
    [InlineData("filaments.json")]
    [InlineData("window.json")]
    public void EachSupportedFileIsCopiedByteForByteIntoMissingDestination(string name)
    {
        Directory.CreateDirectory(Source);
        byte[] content = [0xef, 0xbb, 0xbf, 0x7b, 0x7d, 0x0d, 0x0a];
        File.WriteAllBytes(Path.Combine(Source, name), content);
        Assert.False(Directory.Exists(Destination));
        Assert.Equal(1, DataDirectoryMigration.CopyMissingFiles(Source, Destination));
        Assert.Equal(content, File.ReadAllBytes(Path.Combine(Destination, name)));
        Assert.Equal(content, File.ReadAllBytes(Path.Combine(Source, name)));
        Assert.Single(Directory.GetFiles(Destination));
    }

    [Fact]
    public void MissingSourceDoesNotCreateDestinationOrDefaults()
    {
        Assert.Equal(0, DataDirectoryMigration.CopyMissingFiles(Source, Destination));
        Assert.False(Directory.Exists(Destination));
    }

    [Fact]
    public void ExistingDestinationFilesWinButMissingEquivalentFilesAreCopied()
    {
        Directory.CreateDirectory(Source); Directory.CreateDirectory(Destination);
        foreach (string name in new[] { "settings.json", "filaments.json", "window.json" })
            File.WriteAllText(Path.Combine(Source, name), "old " + name);
        File.WriteAllText(Path.Combine(Destination, "settings.json"), "new settings");
        File.WriteAllText(Path.Combine(Destination, "filaments.json"), "new filaments");
        Assert.Equal(1, DataDirectoryMigration.CopyMissingFiles(Source, Destination));
        Assert.Equal("new settings", File.ReadAllText(Path.Combine(Destination, "settings.json")));
        Assert.Equal("new filaments", File.ReadAllText(Path.Combine(Destination, "filaments.json")));
        Assert.Equal("old window.json", File.ReadAllText(Path.Combine(Destination, "window.json")));
        Assert.Equal(3, Directory.GetFiles(Source).Length);
    }

    [Fact]
    public void RepeatedMigrationDoesNotOverwriteNewerDestinationEvenIfSourceChanges()
    {
        Directory.CreateDirectory(Source);
        File.WriteAllText(Path.Combine(Source, "settings.json"), "first");
        Assert.Equal(1, DataDirectoryMigration.CopyMissingFiles(Source, Destination));
        File.WriteAllText(Path.Combine(Destination, "settings.json"), "new user setting");
        File.WriteAllText(Path.Combine(Source, "settings.json"), "changed source");
        Assert.Equal(0, DataDirectoryMigration.CopyMissingFiles(Source, Destination));
        Assert.Equal("new user setting", File.ReadAllText(Path.Combine(Destination, "settings.json")));
        Assert.Equal("changed source", File.ReadAllText(Path.Combine(Source, "settings.json")));
    }

    [Fact]
    public void OnlyTheThreeKnownDataFilesAreEligible()
    {
        Directory.CreateDirectory(Source);
        File.WriteAllText(Path.Combine(Source, "settings.json.tmp"), "unfinished");
        File.WriteAllText(Path.Combine(Source, "filaments.json.invalid"), "backup");
        File.WriteAllText(Path.Combine(Source, "unrelated.txt"), "unrelated");
        Assert.Equal(0, DataDirectoryMigration.CopyMissingFiles(Source, Destination));
        Assert.False(Directory.Exists(Destination));
        Assert.Equal(3, Directory.GetFiles(Source).Length);
    }

    [Fact]
    public void CopyFailurePreservesSourceAndDoesNotPublishPartialJson()
    {
        Directory.CreateDirectory(Source);
        File.WriteAllText(Path.Combine(Source, "settings.json"), "old settings");
        File.WriteAllText(Destination, "a file occupies the destination directory");
        Assert.ThrowsAny<IOException>(() => DataDirectoryMigration.CopyMissingFiles(Source, Destination));
        Assert.Equal("old settings", File.ReadAllText(Path.Combine(Source, "settings.json")));
        Assert.Equal("a file occupies the destination directory", File.ReadAllText(Destination));
        Assert.Empty(Directory.GetFiles(_root, "*.tmp", SearchOption.AllDirectories));
    }

    [Fact]
    public void LegacyJsonRetainsSettingsInactiveFilamentAndWindowWithoutSeedingOrRewriting()
    {
        Directory.CreateDirectory(Source);
        const string settings = """{"Language":"en-US","Theme":"Dark","ElectricityPricePerKWh":0.2345,"HeatingPowerWatts":987,"HeatingTimeMinutes":2.5,"MachineCostPerHour":0.85,"DefaultSaleMultiplier":4.2}""";
        const string filaments = """[{"Id":"62031164-2bd6-42bb-a388-4d561cb63f08","Name":"My edited profile","Brand":"Custom","MaterialType":"PETG","SpoolWeightGrams":750,"PurchasePrice":12.95,"PrintPowerWatts":157,"IsActive":false,"Notes":"Keep my note"}]""";
        const string window = """{"X":-1100,"Y":80,"Width":450,"Height":760}""";
        var files = new Dictionary<string, string> { ["settings.json"] = settings, ["filaments.json"] = filaments, ["window.json"] = window };
        foreach (var (name, content) in files) File.WriteAllText(Path.Combine(Source, name), content);
        Assert.Equal(3, DataDirectoryMigration.CopyMissingFiles(Source, Destination));
        var loaded = new JsonDataStore(Destination).Load();
        Assert.Empty(loaded.Notices); Assert.Equal("en-US", loaded.Settings.Language); Assert.Equal("Dark", loaded.Settings.Theme);
        Assert.Equal(0.2345m, loaded.Settings.ElectricityPricePerKWh); Assert.Equal(4.2m, loaded.Settings.DefaultSaleMultiplier);
        var filament = Assert.Single(loaded.Filaments);
        Assert.False(filament.IsActive); Assert.Equal(12.95m, filament.PurchasePrice); Assert.Equal("Keep my note", filament.Notes);
        Assert.Equal(new WindowBounds(-1100, 80, 450, 760), new WindowPlacementStore(Path.Combine(Destination, "window.json")).Load());
        foreach (var (name, content) in files)
        {
            Assert.Equal(content, File.ReadAllText(Path.Combine(Source, name)));
            Assert.Equal(content, File.ReadAllText(Path.Combine(Destination, name)));
        }
        Assert.Equal(0, DataDirectoryMigration.CopyMissingFiles(Source, Destination));
    }

    public void Dispose() { if (Directory.Exists(_root)) Directory.Delete(_root, recursive: true); }
}
