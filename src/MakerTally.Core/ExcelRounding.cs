namespace MakerTally.Core;

public static class ExcelRounding
{
    /// <summary>
    /// Excel ROUNDUP for 0..28 decimal places: directed rounding toward positive
    /// infinity for nonnegative values and negative infinity for negative values.
    /// This is not nearest rounding with MidpointRounding.AwayFromZero:
    /// at two decimal places, 9.34101 becomes 9.35 and 5.23411 becomes 5.24.
    /// </summary>
    public static decimal RoundUp(decimal value, int decimalPlaces = 2)
        => decimal.Round(value, decimalPlaces, value >= 0 ? MidpointRounding.ToPositiveInfinity : MidpointRounding.ToNegativeInfinity);
}
