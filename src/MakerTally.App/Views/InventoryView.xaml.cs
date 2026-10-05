using MakerTally.App.Presentation.ViewModels;
namespace MakerTally.App.Views;
public partial class InventoryView : ContentView
{
    public InventoryView() => InitializeComponent();
    private async void FilamentActions(object? sender, EventArgs e)
    {
        if (BindingContext is MainViewModel vm && sender is Button { BindingContext: FilamentCardViewModel card })
        {
            try { await vm.OpenFilamentActionsAsync(card); }
            catch (Exception) { vm.ActionError(); }
        }
    }
}
