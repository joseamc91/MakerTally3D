namespace MakerTally.Core;

public sealed class MakerTallyCalculationService
{
    public PrintCalculationResult Calculate(PrintCalculationInput input)
    {
        ArgumentNullException.ThrowIfNull(input);
        ArgumentNullException.ThrowIfNull(input.Filament);
        ArgumentNullException.ThrowIfNull(input.Settings);
        if (!input.Filament.IsValid() || !input.Settings.IsValid()
            || input.PieceWeightGrams < 0 || input.PrintTimeHours < 0 || input.SaleMultiplier < 0)
            throw new ArgumentOutOfRangeException(nameof(input));

        decimal pricePerGram = input.Filament.PricePerGram;
        decimal material = input.PieceWeightGrams * pricePerGram;
        decimal heating = (input.Settings.HeatingPowerWatts / 1000m) * (input.Settings.HeatingTimeMinutes / 60m);
        decimal printing = (input.Filament.PrintPowerWatts / 1000m) * input.PrintTimeHours;
        decimal totalEnergy = heating + printing;
        decimal electricity = totalEnergy * input.Settings.ElectricityPricePerKWh;
        decimal print = material + electricity;
        decimal machine = input.PrintTimeHours * input.Settings.MachineCostPerHour;
        decimal rawSale = (print * input.SaleMultiplier) + machine;
        decimal sale = ExcelRounding.RoundUp(rawSale, 2);
        decimal rawGross = sale - print - machine;
        decimal gross = ExcelRounding.RoundUp(rawGross, 2);
        return new(pricePerGram, material, heating, printing, totalEnergy, electricity,
            print, machine, rawSale, sale, rawGross, gross);
    }
}
