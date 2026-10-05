using System.Windows;
using System.Windows.Markup;
using PrintCost.Desktop.Infrastructure;
using PrintCost.Desktop.ViewModels;

namespace PrintCost.Desktop.Views;

public partial class FilamentEditorWindow : Window
{
    public FilamentEditorWindow(FilamentEditorViewModel viewModel)
    {
        InitializeComponent();
        Language = XmlLanguage.GetLanguage(LocalizationService.Instance.Culture.Name);
        DataContext = viewModel;
        viewModel.SaveRequested += (_, _) => DialogResult = true;
    }
}
