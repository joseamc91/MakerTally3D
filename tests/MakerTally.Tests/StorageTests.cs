using System.Text.Json;
using System.IO;
using MakerTally.Core;

namespace MakerTally.Tests;

public sealed class StorageTests : IDisposable
{
    private readonly string _directory = Path.Combine(Path.GetTempPath(), "MakerTally.Tests", Guid.NewGuid().ToString());
    private JsonDataStore Store => new(_directory);

    [Fact] public void FirstRunCreatesExactDefaultsAndSevenEditableProfiles()
    {
        var data = Store.Load();
        Assert.Empty(data.Notices); Assert.Equal(new AppSettings(), data.Settings);
        Assert.Equal(7, data.Filaments.Count); Assert.All(data.Filaments, f => Assert.True(f.IsValid() && f.IsActive));
        Assert.True(File.Exists(Path.Combine(_directory, "settings.json")));
        Assert.True(File.Exists(Path.Combine(_directory, "filaments.json")));
    }
    [Fact] public void SettingsAndInventoryRoundTripIncludingInactiveNotesAndIdentifiers()
    {
        var data = Store.Load();
        var settings = data.Settings with { Language = "en-US", MachineCostPerHour = 0.73m, DefaultSaleMultiplier = 2.5m };
        var filament = data.Filaments[0] with { IsActive = false, Notes = "Árbol / example", PurchasePrice = 19.99m };
        Store.SaveSettings(settings); Store.SaveFilaments([filament]);
        var loaded = Store.Load();
        Assert.Equal(settings, loaded.Settings); Assert.Equal(filament, Assert.Single(loaded.Filaments));
        Assert.DoesNotContain("PricePerKg", File.ReadAllText(Path.Combine(_directory, "filaments.json")));
    }
    [Theory] [InlineData("settings.json")] [InlineData("filaments.json")]
    public void CorruptJsonFallsBackAndPreservesOriginal(string name)
    {
        Store.Load(); File.WriteAllText(Path.Combine(_directory, name), "{broken");
        var loaded = Store.Load();
        Assert.Contains(loaded.Notices, n => n.Issue == StorageIssue.InvalidData && n.FileName == name);
        Assert.Equal("{broken", File.ReadAllText(Path.Combine(_directory, name + ".invalid")));
        Assert.True(loaded.Settings.IsValid()); Assert.Equal(7, loaded.Filaments.Count);
        Assert.Empty(Store.Load().Notices);
    }
    [Theory] [InlineData("settings.json")] [InlineData("filaments.json")]
    public void MissingOneFilePreservesOtherFile(string name)
    {
        var initial = Store.Load();
        Store.SaveSettings(initial.Settings with { Language = "en-US" });
        Store.SaveFilaments([]);
        File.Delete(Path.Combine(_directory, name));
        var loaded = Store.Load();
        Assert.Empty(loaded.Notices);
        Assert.Equal(name == "settings.json" ? "es-ES" : "en-US", loaded.Settings.Language);
        Assert.Equal(name == "filaments.json" ? 7 : 0, loaded.Filaments.Count);
    }
    [Fact] public void SemanticallyInvalidDataUsesDefaults()
    {
        Store.Load();
        File.WriteAllText(Path.Combine(_directory, "settings.json"), JsonSerializer.Serialize(new AppSettings { HeatingTimeMinutes = -1 }));
        File.WriteAllText(Path.Combine(_directory, "filaments.json"), "[null]");
        var loaded = Store.Load();
        Assert.Equal(2, loaded.Notices.Count); Assert.Equal(new AppSettings(), loaded.Settings); Assert.Equal(7, loaded.Filaments.Count);
    }
    [Fact] public void UnwritableLocationDoesNotCrashLoading()
    {
        Directory.CreateDirectory(_directory);
        var path = Path.Combine(_directory, "file-instead-of-directory"); File.WriteAllText(path, "x");
        var loaded = new JsonDataStore(path).Load();
        Assert.Equal(2, loaded.Notices.Count); Assert.True(loaded.Settings.IsValid()); Assert.Equal(7, loaded.Filaments.Count);
    }
    [Fact] public void DuplicateIdsAreRejected()
    {
        var f = DefaultData.Filaments()[0];
        Assert.Throws<ArgumentException>(() => Store.SaveFilaments([f, f]));
    }
    public void Dispose() { if (Directory.Exists(_directory)) Directory.Delete(_directory, true); }
}
