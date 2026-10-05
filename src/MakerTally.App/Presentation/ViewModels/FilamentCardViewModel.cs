using MakerTally.Core;
using MakerTally.App.Presentation.Infrastructure;
using MakerTally.App.Presentation.Services;

namespace MakerTally.App.Presentation.ViewModels;

public sealed class FilamentCardViewModel(FilamentProfile profile, LocalizationService language) : ObservableObject
{
    public FilamentProfile Profile { get; } = profile;
    public string Name => Profile.DisplayName;
    public string Weight => language.WithUnit(Profile.SpoolWeightGrams, "Grams");
    public string PricePerKg => language.Currency(Profile.PricePerKg) + language.Get("PerKg");
    public string Power => language.WithUnit(Profile.PrintPowerWatts, "Watts");
    public string PurchasePrice => language.Get("PurchaseColumn") + ": " + language.Currency(Profile.PurchasePrice);
    public string Status => language.Get(Profile.IsActive ? "Active" : "Inactive");
    public bool IsInactive => !Profile.IsActive;
    public void RefreshLanguage() => Notify(null);
}
