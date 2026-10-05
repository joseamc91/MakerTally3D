namespace MakerTally.Core;

/// <summary>Copies legacy data without changing its bytes or replacing newer files.</summary>
public static class DataDirectoryMigration
{
    public const string ActiveDirectoryName = "MakerTally";
    public const string LegacyDirectoryName = "PrintCost";

    public static int CopyMissingFiles(string sourceDirectory, string destinationDirectory)
    {
        int copied = 0;
        foreach (string name in new[] { "settings.json", "filaments.json", "window.json" })
        {
            string source = Path.Combine(sourceDirectory, name);
            string destination = Path.Combine(destinationDirectory, name);
            if (!File.Exists(source) || File.Exists(destination)) continue;
            Directory.CreateDirectory(destinationDirectory);
            // Promote a complete temporary copy atomically. A failed copy must not
            // leave a partial final JSON or cause defaults to hide the legacy data.
            string temporary = destination + ".migration-" + Guid.NewGuid().ToString("N") + ".tmp";
            try
            {
                File.Copy(source, temporary, overwrite: false);
                try { File.Move(temporary, destination, overwrite: false); copied++; }
                catch (IOException) when (File.Exists(destination))
                { /* Another instance created the destination; keep its data. */ }
            }
            finally { if (File.Exists(temporary)) File.Delete(temporary); }
        }
        return copied;
    }
}
