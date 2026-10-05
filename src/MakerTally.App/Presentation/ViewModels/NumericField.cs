using MakerTally.App.Presentation.Services;
using System.Collections;
using System.ComponentModel;
using MakerTally.Core;
using MakerTally.App.Presentation.Infrastructure;

namespace MakerTally.App.Presentation.ViewModels;

public sealed class NumericField : ObservableObject, INotifyDataErrorInfo
{
    private readonly LocalizationService _language;
    private string _text;
    private readonly bool _positive;
    private readonly bool _integer;
    private readonly decimal? _maximum;
    public NumericField(decimal value, bool positive = false, LocalizationService? language = null, bool integer = false, decimal? maximum = null)
    {
        _language = language ?? LocalizationService.Instance;
        _positive = positive;
        _integer = integer;
        _maximum = maximum;
        _text = _language.Number(value);
        Validate();
    }
    public string Text
    {
        get => _text;
        set { if (Set(ref _text, value)) { Validate(); Edited?.Invoke(this, EventArgs.Empty); } }
    }
    public string ErrorText => HasErrors ? _language.Get(_integer ? (_maximum == 59 ? "MinutesRange" : "NonNegativeInteger") : _positive ? "PositiveNumber" : "NonNegativeNumber") : "";
    public decimal Value { get; private set; }
    public bool HasErrors { get; private set; }
    public event EventHandler<DataErrorsChangedEventArgs>? ErrorsChanged;
    public event EventHandler? Edited;
    public IEnumerable GetErrors(string? propertyName) => HasErrors
        ? new[] { _language.Get(_integer ? (_maximum == 59 ? "MinutesRange" : "NonNegativeInteger") : _positive ? "PositiveNumber" : "NonNegativeNumber") } : Array.Empty<string>();
    private void Validate()
    {
        HasErrors = !NumericInput.TryParse(_text, out decimal value) || (_positive ? value <= 0 : value < 0)
            || (_integer && value != decimal.Truncate(value)) || (_maximum.HasValue && value > _maximum.Value);
        if (!HasErrors) Value = value;
        ErrorsChanged?.Invoke(this, new(nameof(Text)));
        Notify(nameof(HasErrors)); Notify(nameof(ErrorText));
    }
    public void RefreshLanguage()
    {
        if (!HasErrors) { _text = _language.Number(Value); Notify(nameof(Text)); }
        ErrorsChanged?.Invoke(this, new(nameof(Text)));
        Notify(nameof(ErrorText));
    }
}
