using System.Globalization;

namespace MakerTally.Core;

public static class NumericInput
{
    // Both decimal separators are accepted; grouping separators are intentionally unsupported.
    public static bool TryParse(string? text, out decimal value)
        => decimal.TryParse(text?.Trim().Replace(',', '.'),
            NumberStyles.AllowDecimalPoint | NumberStyles.AllowLeadingSign,
            CultureInfo.InvariantCulture, out value);
}
