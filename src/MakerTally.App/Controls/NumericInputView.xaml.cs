using MakerTally.App.Presentation.ViewModels;

namespace MakerTally.App.Controls;

public partial class NumericInputView : ContentView
{
    public static readonly BindableProperty FieldProperty = BindableProperty.Create(nameof(Field), typeof(NumericField), typeof(NumericInputView));
    public static readonly BindableProperty LabelTextProperty = BindableProperty.Create(nameof(LabelText), typeof(string), typeof(NumericInputView), "");
    public static readonly BindableProperty InputIdProperty = BindableProperty.Create(nameof(InputId), typeof(string), typeof(NumericInputView), "");
    public NumericField? Field { get => (NumericField?)GetValue(FieldProperty); set => SetValue(FieldProperty, value); }
    public string LabelText { get => (string)GetValue(LabelTextProperty); set => SetValue(LabelTextProperty, value); }
    public string InputId { get => (string)GetValue(InputIdProperty); set => SetValue(InputIdProperty, value); }
    public NumericInputView() => InitializeComponent();
}
