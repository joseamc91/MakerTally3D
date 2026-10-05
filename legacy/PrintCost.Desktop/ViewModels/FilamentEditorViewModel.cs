using PrintCost.Core;
using PrintCost.Desktop.Infrastructure;

namespace PrintCost.Desktop.ViewModels;

public sealed class FilamentEditorViewModel : ObservableObject
{
    private readonly Guid _id;
    private string _name;
    public FilamentEditorViewModel(FilamentProfile? original)
    {
        var model = original ?? new FilamentProfile();
        _id = model.Id;
        _name = model.Name;
        Brand = model.Brand;
        MaterialType = model.MaterialType;
        Notes = model.Notes;
        IsActive = model.IsActive;
        Weight = new(model.SpoolWeightGrams, true);
        Price = new(model.PurchasePrice);
        Power = new(model.PrintPowerWatts);
        Title = LocalizationService.Instance.Get(original is null ? "AddFilament" : "EditFilament");
        SaveCommand = new(() => SaveRequested?.Invoke(this, EventArgs.Empty), () => IsValid);
        foreach (var field in new[] { Weight, Price, Power }) field.Edited += (_, _) => Refresh();
    }
    public string Title { get; }
    public string Name { get => _name; set { if (Set(ref _name, value)) Refresh(); } }
    public string Brand { get; set; }
    public string MaterialType { get; set; }
    public string Notes { get; set; }
    public bool IsActive { get; set; }
    public NumericField Weight { get; }
    public NumericField Price { get; }
    public NumericField Power { get; }
    public bool IsValid => !string.IsNullOrWhiteSpace(Name) && !Weight.HasErrors && !Price.HasErrors && !Power.HasErrors && Build().IsValid();
    public string PricePerKg
    {
        get
        {
            if (!Weight.HasErrors && !Price.HasErrors)
                try { return LocalizationService.Instance.Currency(Build().PricePerKg) + LocalizationService.Instance.Get("PerKg"); }
                catch (OverflowException) { }
            return LocalizationService.Instance.Get("Unavailable");
        }
    }
    public RelayCommand SaveCommand { get; }
    public event EventHandler? SaveRequested;
    private void Refresh() { Notify(nameof(IsValid)); Notify(nameof(PricePerKg)); SaveCommand.Refresh(); }
    public FilamentProfile Build() => new()
    {
        Id = _id, Name = Name.Trim(), Brand = Brand.Trim(), MaterialType = MaterialType.Trim(),
        SpoolWeightGrams = Weight.Value, PurchasePrice = Price.Value, PrintPowerWatts = Power.Value,
        IsActive = IsActive, Notes = Notes.Trim()
    };
}
