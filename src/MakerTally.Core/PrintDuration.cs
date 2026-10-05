namespace MakerTally.Core;

public static class PrintDuration
{
    public static decimal ToHours(decimal hours, decimal minutes)
    {
        if (hours < 0 || hours != decimal.Truncate(hours)) throw new ArgumentOutOfRangeException(nameof(hours));
        if (minutes < 0 || minutes > 59 || minutes != decimal.Truncate(minutes)) throw new ArgumentOutOfRangeException(nameof(minutes));
        return hours + minutes / 60m;
    }
}
