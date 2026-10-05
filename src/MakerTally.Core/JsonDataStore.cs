using System.Text.Json;

namespace MakerTally.Core;

public enum StorageIssue { InvalidData, ReadFailed, SaveFailed }
public sealed record StorageNotice(StorageIssue Issue, string FileName);
public sealed record LoadedData(AppSettings Settings, List<FilamentProfile> Filaments, IReadOnlyList<StorageNotice> Notices);

public sealed class JsonDataStore : IAppDataStorage
{
    private static readonly JsonSerializerOptions Options = new() { WriteIndented = true };
    public string DirectoryPath { get; }

    public JsonDataStore(string? directoryPath = null) => DirectoryPath = directoryPath
        ?? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), DataDirectoryMigration.ActiveDirectoryName);

    public LoadedData Load()
    {
        List<StorageNotice> notices = [];
        var settings = LoadFile("settings.json", () => new AppSettings(), s => s.IsValid(), notices);
        var filaments = LoadFile("filaments.json", DefaultData.Filaments,
            fs => fs.All(f => f is not null && f.IsValid()) && fs.Select(f => f.Id).Distinct().Count() == fs.Count,
            notices, MigrateLegacyFilaments);
        return new(settings, filaments, notices);
    }

    private static List<FilamentProfile> MigrateLegacyFilaments(List<FilamentProfile> filaments, JsonElement json)
    {
        // Infer only missing fields from the same JSON snapshot; do not rewrite files on load.
        for (int i = 0; i < filaments.Count; i++)
            if (!json[i].TryGetProperty("Variant", out _))
                filaments[i] = LegacyFilamentMigration.AddVariant(filaments[i]);
        return filaments;
    }

    private T LoadFile<T>(string fileName, Func<T> defaults, Func<T, bool> validate, List<StorageNotice> notices,
        Func<T, JsonElement, T>? adapt = null)
    {
        string path = Path.Combine(DirectoryPath, fileName);
        try
        {
            if (File.Exists(path))
            {
                string json = File.ReadAllText(path);
                var data = JsonSerializer.Deserialize<T>(json, Options);
                if (data is null || !validate(data)) throw new JsonException();
                if (adapt is not null)
                {
                    using var document = JsonDocument.Parse(json);
                    return adapt(data, document.RootElement);
                }
                return data;
            }
            T initial = defaults();
            Save(fileName, initial);
            return initial;
        }
        catch (JsonException)
        {
            notices.Add(new(StorageIssue.InvalidData, fileName));
            // Preserve malformed data before replacing it with usable defaults.
            try
            {
                File.Copy(path, path + ".invalid", true);
                T initial = defaults();
                Save(fileName, initial);
                return initial;
            }
            catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
            { notices.Add(new(StorageIssue.SaveFailed, fileName)); }
        }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
        { notices.Add(new(StorageIssue.ReadFailed, fileName)); }
        return defaults();
    }

    public void SaveSettings(AppSettings settings)
    {
        if (!settings.IsValid()) throw new ArgumentException("Invalid settings.", nameof(settings));
        Save("settings.json", settings);
    }

    public void SaveFilaments(IEnumerable<FilamentProfile> filaments)
    {
        var items = filaments.ToList();
        if (items.Any(f => !f.IsValid()) || items.Select(f => f.Id).Distinct().Count() != items.Count)
            throw new ArgumentException("Invalid filaments.", nameof(filaments));
        Save("filaments.json", items);
    }

    private void Save<T>(string fileName, T value)
    {
        Directory.CreateDirectory(DirectoryPath);
        string path = Path.Combine(DirectoryPath, fileName);
        string temp = path + ".tmp";
        try
        {
            File.WriteAllText(temp, JsonSerializer.Serialize(value, Options));
            File.Move(temp, path, true);
        }
        finally { if (File.Exists(temp)) File.Delete(temp); }
    }
}
