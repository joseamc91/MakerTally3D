using MakerTally.App.Presentation.Services;
using MakerTally.Core;
using MakerTally.App.Presentation.Infrastructure;

namespace MakerTally.App.Presentation.ViewModels;

public sealed class FilamentEditorViewModel : ObservableObject
{
    private readonly LocalizationService _language;
    private readonly FilamentProfile _original;
    private string _brand, _type, _variant;
    public FilamentEditorViewModel(FilamentProfile? original, LocalizationService? language = null)
    {
        _language = language ?? LocalizationService.Instance;
        _original = original ?? new FilamentProfile { PrintPowerWatts = 120m };
        _brand = _original.Brand; _type = _original.MaterialType; _variant = _original.Variant ?? "";
        Weight = new(_original.SpoolWeightGrams, true, _language);
        Price = new(_original.PurchasePrice, language: _language);
        Power = new(_original.PrintPowerWatts, language: _language);
        Title = _language.Get(original is null ? "AddFilament" : "EditFilament");
        SaveCommand = new(() => SaveRequested?.Invoke(this, EventArgs.Empty), () => IsValid);
        foreach (var field in new[] { Weight, Price, Power }) field.Edited += (_, _) => Refresh();
        if (original is null) Price.Text = "";
    }
    public string Title { get; }
    public string Brand { get => _brand; set { if (Set(ref _brand, value)) Refresh(); } }
    public string MaterialType { get => _type; set { if (Set(ref _type, value)) Refresh(); } }
    public string Variant { get => _variant; set { if (Set(ref _variant, value)) Refresh(); } }
    public string BrandError => string.IsNullOrWhiteSpace(Brand) ? _language.Get("BrandRequired") : "";
    public string TypeError => string.IsNullOrWhiteSpace(MaterialType) ? _language.Get("TypeRequired") : "";
    public bool HasBrandError => string.IsNullOrWhiteSpace(Brand);
    public bool HasTypeError => string.IsNullOrWhiteSpace(MaterialType);
    public NumericField Weight { get; }
    public NumericField Price { get; }
    public NumericField Power { get; }
    public bool IsValid => !string.IsNullOrWhiteSpace(Brand) && !string.IsNullOrWhiteSpace(MaterialType)
        && !Weight.HasErrors && !Price.HasErrors && !Power.HasErrors && Build().IsValid();
    public string PricePerKg
    {
        get
        {
            if (!Weight.HasErrors && !Price.HasErrors)
                try { return _language.Currency(Build().PricePerKg) + _language.Get("PerKg"); }
                catch (OverflowException) { }
            return _language.Get("Unavailable");
        }
    }
    public RelayCommand SaveCommand { get; }
    public event EventHandler? SaveRequested;
    private void Refresh()
    {
        Notify(nameof(IsValid)); Notify(nameof(BrandError)); Notify(nameof(TypeError)); Notify(nameof(PricePerKg)); SaveCommand.Refresh();
        Notify(nameof(HasBrandError)); Notify(nameof(HasTypeError));
    }
    public FilamentProfile Build()
    {
        var profile = _original with
        {
            Brand = Brand.Trim(), MaterialType = MaterialType.Trim(), Variant = Variant.Trim(),
            SpoolWeightGrams = Weight.Value, PurchasePrice = Price.Value, PrintPowerWatts = Power.Value
        };
        // Existing Name, Notes, Id and activation state remain exact; new profiles get a compatibility name.
        return string.IsNullOrWhiteSpace(profile.Name) ? profile with { Name = profile.DisplayName } : profile;
    }
}
