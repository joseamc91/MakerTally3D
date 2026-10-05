using MakerTally.App.Presentation.ViewModels;
using MakerTally.App.Services;
namespace MakerTally.App;
public partial class MainPage : ContentPage
{
    public MainPage(MainViewModel viewModel, PageDialogs dialogs)
    {
        InitializeComponent();
        BindingContext = viewModel;
        dialogs.Host = this;
    }
    private void RootSizeChanged(object? sender, EventArgs e)
    {
        if (RootGrid.Width <= 0) return;
        double contentWidth = Math.Min(RootGrid.Width, 520);
        PageHost.WidthRequest = contentWidth;
        NavigationGrid.WidthRequest = Math.Max(0, contentWidth - 8);
    }
}
