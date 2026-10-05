using MakerTally.Core;

namespace MakerTally.Tests;

public sealed class CalculationTests
{
    private readonly MakerTallyCalculationService _service = new();
    private static PrintCalculationInput Example() => new(
        DefaultData.Filaments().Single(f => f.Name == "ASA eSun"), 141m, 6m, 3m, new AppSettings());

    [Fact]
    public void AsaExamplePreservesPrecisionThroughEveryFormula()
    {
        var result = _service.Calculate(Example());
        Assert.Equal(0.0175m, result.PricePerGram);
        Assert.Equal(2.4675m, result.MaterialCost);
        Assert.Equal(0.020m, result.HeatingEnergyKWh);
        Assert.Equal(1.080m, result.PrintingEnergyKWh);
        Assert.Equal(1.100m, result.TotalEnergyKWh);
        Assert.Equal(0.14839m, result.ElectricityCost);
        Assert.Equal(2.61589m, result.PieceCost);
        Assert.Equal(1.50m, result.MachineCost);
        Assert.Equal(9.34767m, result.RawSalePrice);
        Assert.Equal(5.23411m, result.RawGrossMargin);
        Assert.Equal(9.35m, result.SuggestedSalePrice);
        Assert.Equal(5.24m, result.GrossMargin);
    }

    [Fact] public void PricePerGramAndKgAccountForActualSpoolWeight()
    {
        var filament = Example().Filament with { SpoolWeightGrams = 750m, PurchasePrice = 15m };
        Assert.Equal(0.02m, filament.PricePerGram);
        Assert.Equal(20m, filament.PricePerKg);
        Assert.Equal(2.82m, _service.Calculate(Example() with { Filament = filament }).MaterialCost);
    }
    [Fact] public void FractionalHoursAndMinutesAreUsedWithoutRounding()
    {
        var result = _service.Calculate(Example() with { PrintTimeHours = 5.75m, Settings = new() { HeatingTimeMinutes = 1.5m } });
        Assert.Equal(1.035m, result.PrintingEnergyKWh);
        Assert.Equal(0.03m, result.HeatingEnergyKWh);
        Assert.Equal(1.4375m, result.MachineCost);
    }
    [Fact] public void AllZeroCostsHaveZeroSaleAndGrossMargin()
    {
        var input = Example() with
        {
            PieceWeightGrams = 0, PrintTimeHours = 0, SaleMultiplier = 0,
            Settings = new() { ElectricityPricePerKWh = 0, HeatingTimeMinutes = 0, MachineCostPerHour = 0 }
        };
        var r = _service.Calculate(input);
        Assert.Equal(0m, r.MaterialCost); Assert.Equal(0m, r.TotalEnergyKWh);
        Assert.Equal(0m, r.ElectricityCost); Assert.Equal(0m, r.PieceCost);
        Assert.Equal(0m, r.MachineCost);
        Assert.Equal(0m, r.SuggestedSalePrice); Assert.Equal(0m, r.GrossMargin);
    }
    [Fact] public void ZeroSaleDoesNotDivideByZeroEvenWithCosts()
    {
        var r = _service.Calculate(Example() with { SaleMultiplier = 0, Settings = new() { MachineCostPerHour = 0 } });
        Assert.Equal(0m, r.SuggestedSalePrice);
        Assert.Equal(-2.62m, r.GrossMargin);
        Assert.Equal(-r.PieceCost, r.RawGrossMargin);
    }
    [Fact] public void ZeroTimeStillIncludesSpecifiedWarmup()
    {
        var r = _service.Calculate(Example() with { PieceWeightGrams = 0, PrintTimeHours = 0 });
        Assert.Equal(0.02m, r.TotalEnergyKWh);
        Assert.Equal(0.002698m, r.ElectricityCost);
        Assert.Equal(0m, r.MachineCost);
    }
    [Theory] [InlineData(0)] [InlineData(-1)]
    public void InvalidSpoolCannotDivideByZero(int grams)
    {
        var f = Example().Filament with { SpoolWeightGrams = grams };
        Assert.Equal(0m, f.PricePerGram); Assert.Equal(0m, f.PricePerKg);
        Assert.Throws<ArgumentOutOfRangeException>(() => _service.Calculate(Example() with { Filament = f }));
    }
    [Theory] [InlineData(-1, 6, 3)] [InlineData(141, -1, 3)] [InlineData(141, 6, -1)]
    public void NegativeInputsAreRejected(int grams, int hours, int multiplier)
        => Assert.Throws<ArgumentOutOfRangeException>(() => _service.Calculate(Example() with { PieceWeightGrams = grams, PrintTimeHours = hours, SaleMultiplier = multiplier }));
    [Fact] public void NegativeSettingIsRejected()
        => Assert.Throws<ArgumentOutOfRangeException>(() => _service.Calculate(Example() with { Settings = new() { ElectricityPricePerKWh = -1m } }));
    [Fact] public void ZeroPriceAndPowerAreValid()
    {
        var f = Example().Filament with { PurchasePrice = 0, PrintPowerWatts = 0 };
        var r = _service.Calculate(Example() with { Filament = f, Settings = new() { HeatingPowerWatts = 0 } });
        Assert.Equal(0m, r.MaterialCost); Assert.Equal(0m, r.ElectricityCost);
        Assert.Equal(r.MachineCost, r.SuggestedSalePrice);
        Assert.Equal(0m, r.GrossMargin);
    }
    [Fact] public void MultiplierOneStillRoundsSaleAndGrossMarginUp()
    {
        var r = _service.Calculate(Example() with { SaleMultiplier = 1 });
        Assert.Equal(4.12m, r.SuggestedSalePrice); Assert.Equal(0.01m, r.GrossMargin);
    }
}
