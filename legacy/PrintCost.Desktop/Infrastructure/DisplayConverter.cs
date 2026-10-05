using System.Globalization;
using System.Windows.Data;

namespace PrintCost.Desktop.Infrastructure;

public sealed class DisplayConverter : IMultiValueConverter
{
    public object Convert(object[] values, Type targetType, object parameter, CultureInfo culture)
    {
        var localizer = LocalizationService.Instance;
        if (values[0] is not decimal number) return localizer.Get("Unavailable");
        return (parameter as string) switch
        {
            "Money" => localizer.Currency(number),
            "Weight" => localizer.WithUnit(number, "Grams"),
            "Power" => localizer.WithUnit(number, "Watts"),
            "Kg" => localizer.Currency(number) + localizer.Get("PerKg"),
            _ => localizer.Number(number)
        };
    }
    public object[] ConvertBack(object value, Type[] targetTypes, object parameter, CultureInfo culture) => throw new NotSupportedException();
}
