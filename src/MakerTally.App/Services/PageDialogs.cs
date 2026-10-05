using MakerTally.Core;
using MakerTally.App.Presentation.Services;
using MakerTally.App.Presentation.ViewModels;
using MakerTally.App.Views;

namespace MakerTally.App.Services;

public sealed class PageDialogs : IUserDialogs
{
    public Page? Host { get; set; }
    private Page Page => Host ?? throw new InvalidOperationException("Dialog host is unavailable.");
    private readonly LocalizationService _language = LocalizationService.Instance;
    public async Task<FilamentProfile?> EditFilamentAsync(FilamentProfile? original)
    {
        var editor = new FilamentEditorViewModel(original);
        var completion = new TaskCompletionSource<FilamentProfile?>();
        var page = new FilamentEditorPage(editor, completion);
        await Page.Navigation.PushModalAsync(page);
        return await completion.Task;
    }
    public Task<bool> ConfirmDeleteAsync(string name) => Page.DisplayAlertAsync(_language.Get("DeleteFilament"),
        string.Format(_language.Culture, _language.Get("DeleteConfirmation"), name), _language.Get("Delete"), _language.Get("Cancel"));
    public async Task<FilamentAction> ChooseFilamentActionAsync(FilamentProfile filament)
    {
        string toggle = _language.Get(filament.IsActive ? "Deactivate" : "Activate");
        string answer = await Page.DisplayActionSheetAsync(filament.DisplayName, _language.Get("Cancel"), null,
            _language.Get("Edit"), toggle, _language.Get("Delete"));
        if (answer == _language.Get("Edit")) return FilamentAction.Edit;
        if (answer == toggle) return FilamentAction.ToggleActive;
        if (answer == _language.Get("Delete")) return FilamentAction.Delete;
        return FilamentAction.Cancel;
    }
}
