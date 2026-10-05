using System.Windows.Input;

namespace MakerTally.App.Presentation.Infrastructure;

public sealed class AsyncRelayCommand(Func<Task> execute, Func<bool>? canExecute = null, Action? onError = null) : ICommand
{
    private bool _running;
    public bool CanExecute(object? parameter) => !_running && (canExecute?.Invoke() ?? true);
    public async void Execute(object? parameter)
    {
        try { await ExecuteAsync(); }
        catch (Exception) { onError?.Invoke(); }
    }
    public async Task ExecuteAsync()
    {
        if (!CanExecute(null)) return;
        _running = true; Refresh();
        try { await execute(); }
        finally { _running = false; Refresh(); }
    }
    public event EventHandler? CanExecuteChanged;
    public void Refresh() => CanExecuteChanged?.Invoke(this, EventArgs.Empty);
}
