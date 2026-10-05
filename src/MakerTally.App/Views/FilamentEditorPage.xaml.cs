using MakerTally.Core;
using MakerTally.App.Presentation.ViewModels;
namespace MakerTally.App.Views;
public partial class FilamentEditorPage : ContentPage
{
    private readonly TaskCompletionSource<FilamentProfile?> _completion;
    private readonly FilamentEditorViewModel _viewModel;
    private bool _closing;
    public FilamentEditorPage(FilamentEditorViewModel viewModel, TaskCompletionSource<FilamentProfile?> completion)
    {
        InitializeComponent();
        _viewModel = viewModel;
        _completion = completion;
        BindingContext = viewModel;
        viewModel.SaveRequested += SaveRequested;
    }
    private async void SaveRequested(object? sender, EventArgs e) => await CloseAsync(_viewModel.Build());
    private async void CancelClicked(object? sender, EventArgs e) => await CloseAsync(null);
    private async Task CloseAsync(FilamentProfile? result)
    {
        if (_closing) return;
        _closing = true;
        await Navigation.PopModalAsync();
        _completion.TrySetResult(result);
    }
    protected override void OnDisappearing()
    {
        base.OnDisappearing();
        if (!_closing) _completion.TrySetResult(null);
        _viewModel.SaveRequested -= SaveRequested;
    }
}
