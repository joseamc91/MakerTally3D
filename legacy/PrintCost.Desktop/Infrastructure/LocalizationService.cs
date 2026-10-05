using System.Globalization;
using System.Windows;
using System.Windows.Markup;

namespace PrintCost.Desktop.Infrastructure;

public sealed class LocalizationService
{
    public static LocalizationService Instance { get; } = new();
    private ResourceDictionary? _resources;
    public CultureInfo Culture { get; private set; } = CreateCulture("es-ES");
    public event EventHandler? Changed;

    public void SetLanguage(string language)
    {
        Culture = CreateCulture(language);
        CultureInfo.CurrentCulture = Culture;
        CultureInfo.CurrentUICulture = Culture;
        CultureInfo.DefaultThreadCurrentCulture = Culture;
        CultureInfo.DefaultThreadCurrentUICulture = Culture;
        var resources = new ResourceDictionary
        {
            Source = new Uri($"/PrintCost;component/Resources/Strings.{language}.xaml", UriKind.Relative)
        };
        if (_resources is not null) Application.Current.Resources.MergedDictionaries.Remove(_resources);
        Application.Current.Resources.MergedDictionaries.Add(resources);
        _resources = resources;
        foreach (Window window in Application.Current.Windows)
            window.Language = XmlLanguage.GetLanguage(language);
        Changed?.Invoke(this, EventArgs.Empty);
    }

    private static CultureInfo CreateCulture(string language)
    {
        var culture = (CultureInfo)CultureInfo.GetCultureInfo(language).Clone();
        culture.NumberFormat.CurrencySymbol = "€";
        return culture;
    }

    public string Get(string key) => (string)(_resources?[key] ?? key);
    public string Currency(decimal value, int decimals = 2) => value.ToString($"C{decimals}", Culture);
    public string Number(decimal value, string format = "0.####") => value.ToString(format, Culture);
    public string WithUnit(decimal value, string unitKey, string format = "0.####")
        => Number(value, format) + " " + Get(unitKey);
}
