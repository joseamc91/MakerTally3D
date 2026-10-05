using System.Collections;
using System.ComponentModel;
using PrintCost.Core;
using PrintCost.Desktop.Infrastructure;

namespace PrintCost.Desktop.ViewModels;

public sealed class NumericField : ObservableObject, INotifyDataErrorInfo
{
    private string _text;
    private readonly bool _positive;
    public NumericField(decimal value, bool positive = false)
    {
        _positive = positive;
        _text = LocalizationService.Instance.Number(value);
        Validate();
    }
    public string Text
    {
        get => _text;
        set { if (Set(ref _text, value)) { Validate(); Edited?.Invoke(this, EventArgs.Empty); } }
    }
    public decimal Value { get; private set; }
    public bool HasErrors { get; private set; }
    public event EventHandler<DataErrorsChangedEventArgs>? ErrorsChanged;
    public event EventHandler? Edited;
    public IEnumerable GetErrors(string? propertyName) => HasErrors
        ? new[] { LocalizationService.Instance.Get(_positive ? "PositiveNumber" : "NonNegativeNumber") } : Array.Empty<string>();
    private void Validate()
    {
        HasErrors = !NumericInput.TryParse(_text, out decimal value) || (_positive ? value <= 0 : value < 0);
        if (!HasErrors) Value = value;
        ErrorsChanged?.Invoke(this, new(nameof(Text)));
        Notify(nameof(HasErrors));
    }
    public void RefreshLanguage()
    {
        if (!HasErrors) { _text = LocalizationService.Instance.Number(Value); Notify(nameof(Text)); }
        ErrorsChanged?.Invoke(this, new(nameof(Text)));
    }
}
