using System.IO;
using MakerTally.Core;
using MakerTally.App.Presentation.Services;

namespace MakerTally.Tests;

public sealed class CompatibilityTests : IDisposable
{
    private readonly string _directory = Path.Combine(Path.GetTempPath(), "MakerTally.Compatibility.Tests", Guid.NewGuid().ToString());
    [Fact] public void WindowsUsesTheMakerTallyDirectory()
        => Assert.Equal(Path.Combine(_directory, "MakerTally"), DataDirectoryPolicy.Windows(_directory));
    [Fact] public void AndroidUsesItsPrivateDirectoryWithoutWindowsSubdirectories()
        => Assert.Equal(_directory, DataDirectoryPolicy.Android(_directory));
    [Fact] public void LegacySettingsAndEditedInactiveFilamentsAreReadWithoutRewriting()
    {
        Directory.CreateDirectory(_directory);
        const string settings = """{"Language":"en-US","ElectricityPricePerKWh":0.2345,"HeatingPowerWatts":987,"HeatingTimeMinutes":2.5,"MachineCostPerHour":0.85,"DefaultSaleMultiplier":4.2}""";
        const string filaments = """[{"Id":"62031164-2bd6-42bb-a388-4d561cb63f08","Name":"My edited profile","Brand":"Custom","MaterialType":"PETG","SpoolWeightGrams":750,"PurchasePrice":12.95,"PrintPowerWatts":157,"IsActive":false,"Notes":"Do not replace"}]""";
        File.WriteAllText(Path.Combine(_directory, "settings.json"), settings);
        File.WriteAllText(Path.Combine(_directory, "filaments.json"), filaments);
        IAppDataStorage store = new JsonDataStore(_directory);
        for (int i = 0; i < 2; i++)
        {
            var loaded = store.Load(); Assert.Empty(loaded.Notices); Assert.Equal("System", loaded.Settings.Theme);
            Assert.Equal("en-US", loaded.Settings.Language); Assert.Equal(0.2345m, loaded.Settings.ElectricityPricePerKWh);
            Assert.Equal(987m, loaded.Settings.HeatingPowerWatts); Assert.Equal(2.5m, loaded.Settings.HeatingTimeMinutes);
            Assert.Equal(0.85m, loaded.Settings.MachineCostPerHour); Assert.Equal(4.2m, loaded.Settings.DefaultSaleMultiplier);
            var profile = Assert.Single(loaded.Filaments); Assert.False(profile.IsActive); Assert.Equal("", profile.Variant); Assert.Equal("PETG · Custom", profile.DisplayName);
            Assert.Equal("My edited profile", profile.Name); Assert.Equal("Do not replace", profile.Notes);
            Assert.Equal(750m, profile.SpoolWeightGrams); Assert.Equal(12.95m, profile.PurchasePrice); Assert.Equal(157m, profile.PrintPowerWatts);
            Assert.Equal(settings, File.ReadAllText(Path.Combine(_directory, "settings.json")));
            Assert.Equal(filaments, File.ReadAllText(Path.Combine(_directory, "filaments.json")));
        }
    }
    public void Dispose() { if (Directory.Exists(_directory)) Directory.Delete(_directory, true); }
}
