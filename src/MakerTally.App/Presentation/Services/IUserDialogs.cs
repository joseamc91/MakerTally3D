using MakerTally.Core;

namespace MakerTally.App.Presentation.Services;

public enum FilamentAction { Cancel, Edit, ToggleActive, Delete }
public interface IUserDialogs
{
    Task<FilamentProfile?> EditFilamentAsync(FilamentProfile? original);
    Task<bool> ConfirmDeleteAsync(string name);
    Task<FilamentAction> ChooseFilamentActionAsync(FilamentProfile filament);
}
