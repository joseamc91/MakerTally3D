using System.Text.Json.Serialization;

namespace MakerTally.Core;

public sealed record AppSettings
{
    public string Theme { get; init; } = "System";
    public string Language { get; init; } = "es-ES";
    public decimal ElectricityPricePerKWh { get; init; } = 0.1349m;
    public decimal HeatingPowerWatts { get; init; } = 1200m;
    public decimal HeatingTimeMinutes { get; init; } = 1m;
    public decimal MachineCostPerHour { get; init; } = 0.25m;
    public decimal DefaultSaleMultiplier { get; init; } = 3m;

    public bool IsValid() => Language is "es-ES" or "en-US"
        && Theme is "System" or "Light" or "Dark"
        && ElectricityPricePerKWh >= 0 && HeatingPowerWatts >= 0
        && HeatingTimeMinutes >= 0 && MachineCostPerHour >= 0 && DefaultSaleMultiplier >= 0;
}

public sealed record FilamentProfile
{
    public Guid Id { get; init; } = Guid.NewGuid();
    public string Name { get; init; } = "";
    public string Brand { get; init; } = "";
    public string? Variant { get; init; } = "";
    public string MaterialType { get; init; } = "";
    public decimal SpoolWeightGrams { get; init; } = 1000m;
    public decimal PurchasePrice { get; init; }
    public decimal PrintPowerWatts { get; init; }
    public bool IsActive { get; init; } = true;
    public string Notes { get; init; } = "";
    [JsonIgnore] public string DisplayName => string.IsNullOrWhiteSpace(MaterialType) || string.IsNullOrWhiteSpace(Brand)
        ? Name : $"{MaterialType.Trim()}{(string.IsNullOrWhiteSpace(Variant) ? "" : " " + Variant.Trim())} · {Brand.Trim()}";
    [JsonIgnore] public decimal PricePerGram => SpoolWeightGrams > 0 ? PurchasePrice / SpoolWeightGrams : 0;
    [JsonIgnore] public decimal PricePerKg => PricePerGram * 1000m;
    public bool IsValid()
    {
        if (Id == Guid.Empty || string.IsNullOrWhiteSpace(Name) || Brand is null || MaterialType is null || Notes is null
            || SpoolWeightGrams <= 0 || PurchasePrice < 0 || PrintPowerWatts < 0) return false;
        try { _ = PricePerKg; return true; }
        catch (OverflowException) { return false; }
    }
}

public sealed record PrintCalculationInput(FilamentProfile Filament, decimal PieceWeightGrams,
    decimal PrintTimeHours, decimal SaleMultiplier, AppSettings Settings);

public sealed record PrintCalculationResult(decimal PricePerGram, decimal MaterialCost,
    decimal HeatingEnergyKWh, decimal PrintingEnergyKWh, decimal TotalEnergyKWh,
    decimal ElectricityCost, decimal PieceCost, decimal MachineCost, decimal RawSalePrice,
    decimal SuggestedSalePrice, decimal RawGrossMargin, decimal GrossMargin);

public static class DefaultData
{
    public static List<FilamentProfile> Filaments() =>
    [
        Create("PLA BambuLab Spool", "BambuLab", "PLA", 13.20m, 120m, "Spool"),
        Create("PLA BambuLab", "BambuLab", "PLA", 11.69m, 120m),
        Create("PETG BambuLab", "BambuLab", "PETG", 11.69m, 140m),
        Create("PLA Jayo", "Jayo", "PLA", 10.90m, 120m),
        Create("ASA eSun", "eSun", "ASA", 17.50m, 180m),
        Create("PLA 850 Sakata", "Sakata", "PLA", 16m, 120m, "850"),
        Create("PETG Elegoo", "Elegoo", "PETG", 15m, 140m)
    ];

    private static FilamentProfile Create(string name, string brand, string material, decimal price, decimal power, string variant = "")
        => new() { Name = name, Variant = variant, Brand = brand, MaterialType = material, PurchasePrice = price, PrintPowerWatts = power };
}
