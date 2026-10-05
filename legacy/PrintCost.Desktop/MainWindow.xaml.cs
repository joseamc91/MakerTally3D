using System.Windows;
using System.Windows.Markup;
using PrintCost.Desktop.Infrastructure;
using PrintCost.Desktop.ViewModels;

namespace PrintCost.Desktop;

public partial class MainWindow : Window
{
    public MainWindow(MainViewModel viewModel)
    {
        InitializeComponent();
        Language = XmlLanguage.GetLanguage(LocalizationService.Instance.Culture.Name);
        DataContext = viewModel;
        Closed += (_, _) => viewModel.Dispose();
    }
}
