namespace MakerTally.Core;

/// <summary>Small persistence boundary shared by Windows and Android presentation.</summary>
public interface IAppDataStorage
{
    string DirectoryPath { get; }
    LoadedData Load();
    void SaveSettings(AppSettings settings);
    void SaveFilaments(IEnumerable<FilamentProfile> filaments);
}
