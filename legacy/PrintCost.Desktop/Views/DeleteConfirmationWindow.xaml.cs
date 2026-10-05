using System.Windows;
using System.Windows.Markup;
using PrintCost.Desktop.Infrastructure;

namespace PrintCost.Desktop.Views;

public partial class DeleteConfirmationWindow : Window
{
    public DeleteConfirmationWindow(string filamentName)
    {
        InitializeComponent();
        var language = LocalizationService.Instance;
        Language = XmlLanguage.GetLanguage(language.Culture.Name);
        ConfirmationText.Text = string.Format(language.Culture, language.Get("DeleteConfirmation"), filamentName);
    }
    private void ConfirmClick(object sender, RoutedEventArgs e) => DialogResult = true;
}
