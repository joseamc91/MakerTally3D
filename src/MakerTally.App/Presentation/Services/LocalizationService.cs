using System.Globalization;
using System.Text.Json;
using MakerTally.App.Presentation.Infrastructure;

namespace MakerTally.App.Presentation.Services;

public sealed class LocalizationService : ObservableObject
{
    public static LocalizationService Instance { get; } = new();
    private readonly Dictionary<string, Dictionary<string, string>> _resources = [];
    public CultureInfo Culture { get; private set; } = CreateCulture("es-ES");
    public event EventHandler? Changed;

    public LocalizationService()
    {
        foreach (string language in new[] { "es-ES", "en-US" })
        {
            using var stream = typeof(LocalizationService).Assembly.GetManifestResourceStream($"MakerTally.Strings.{language}.json")
                ?? throw new InvalidOperationException($"Missing language resource: {language}");
            _resources[language] = JsonSerializer.Deserialize<Dictionary<string, string>>(stream)!;
        }
    }
    public string this[string key] => Get(key);
    public IReadOnlyCollection<string> Keys => _resources[Culture.Name].Keys;
    public void SetLanguage(string language)
    {
        if (!_resources.ContainsKey(language)) throw new ArgumentOutOfRangeException(nameof(language));
        Culture = CreateCulture(language);
        CultureInfo.CurrentCulture = Culture;
        CultureInfo.CurrentUICulture = Culture;
        CultureInfo.DefaultThreadCurrentCulture = Culture;
        CultureInfo.DefaultThreadCurrentUICulture = Culture;
        // MAUI indexer bindings refresh reliably with the standard all-properties signal.
        Notify(null);
        Changed?.Invoke(this, EventArgs.Empty);
    }
    private static CultureInfo CreateCulture(string language)
    {
        var culture = (CultureInfo)CultureInfo.GetCultureInfo(language).Clone();
        culture.NumberFormat.CurrencySymbol = "€";
        return culture;
    }
    public string Get(string key) => _resources[Culture.Name][key];
    public string Currency(decimal value, int decimals = 2) => value.ToString($"C{decimals}", Culture);
    public string Number(decimal value, string format = "0.####") => value.ToString(format, Culture);
    public string WithUnit(decimal value, string unitKey, string format = "0.####") => Number(value, format) + " " + Get(unitKey);
}
