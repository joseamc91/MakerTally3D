using MakerTally.App.Presentation.Services;
using System.Collections.ObjectModel;
using System.IO;
using MakerTally.Core;
using MakerTally.App.Presentation.Infrastructure;

namespace MakerTally.App.Presentation.ViewModels;

public sealed partial class MainViewModel : ObservableObject, IDisposable
{
    private readonly IAppDataStorage _store;
    private readonly MakerTallyCalculationService _calculator = new();
    private readonly LocalizationService _language;
    private readonly IUserDialogs _dialogs;
    private readonly Action<Action> _dispatch;
    private bool _disposed;
    private readonly Timer _saveTimer;
    private readonly List<StorageNotice> _notices;
    private AppSettings _settings;
    private FilamentProfile? _selectedFilament;
    private FilamentProfile? _inventorySelection;
    private string _selectedLanguage;
    private string _selectedTheme;
    private bool _detailsExpanded;
    private int _page;
    private bool _actionFailed;
    private bool _settingsDirty;
    private bool _hasCalculation;
    private bool _settingsSaved = true;
    private PrintCalculationResult? _result;
    public MainViewModel(IAppDataStorage store, LoadedData data, IUserDialogs dialogs, LocalizationService? language = null, Action<Action>? dispatch = null)
    {
        _store = store;
        _dialogs = dialogs;
        _language = language ?? LocalizationService.Instance;
        LanguageNames = [_language.Get("Spanish"), _language.Get("English")];
        _dispatch = dispatch ?? (action => action());
        _saveTimer = new(_ => _dispatch(() => { if (!_disposed) FlushSettings(); }), null, Timeout.Infinite, Timeout.Infinite);
        _settings = data.Settings;
        _notices = data.Notices.ToList();
        _selectedLanguage = _settings.Language;
        _selectedTheme = _settings.Theme;
        Filaments = new(data.Filaments);
        Weight = new(141m, language: _language);
        Hours = new(6m, language: _language, integer: true);
        Minutes = new(0m, language: _language, integer: true, maximum: 59);
        ElectricityPrice = new(_settings.ElectricityPricePerKWh, language: _language);
        HeatingPower = new(_settings.HeatingPowerWatts, language: _language);
        HeatingMinutes = new(_settings.HeatingTimeMinutes, language: _language);
        MachineRate = new(_settings.MachineCostPerHour, language: _language);
        foreach (var field in CalculationFields) field.Edited += (_, _) => Recalculate();
        foreach (var field in SettingsFields) field.Edited += (_, _) => UpdateSettings();
        InitializeCompactSettings();
        AddCommand = new(() => ShowEditorAsync(null), onError: ActionError);
        EditCommand = new(() => ShowEditorAsync(InventorySelection), () => InventorySelection is not null, ActionError);
        DeleteCommand = new(DeleteFilamentAsync, () => InventorySelection is not null, ActionError);
        ToggleActiveCommand = new(ToggleActive, () => InventorySelection is not null);
        IncreaseMultiplierCommand = new(() => ChangeMultiplier(0.5m), () => _settings.DefaultSaleMultiplier <= decimal.MaxValue - 0.5m);
        DecreaseMultiplierCommand = new(() => ChangeMultiplier(-0.5m), () => _settings.DefaultSaleMultiplier > 1m);
        ToggleDetailsCommand = new(() => { _detailsExpanded = !_detailsExpanded; Notify(nameof(DetailsExpanded)); Notify(nameof(DetailsToggleText)); });
        CalculatorCommand = new(() => Page = 0);
        InventoryCommand = new(() => Page = 1);
        SettingsCommand = new(() => Page = 2);
        _language.Changed += LanguageChanged;
        RefreshActiveFilaments(data.Filaments.FirstOrDefault(f => f.Name == "ASA eSun")?.Id);
    }

    public LocalizationService L => _language;
    public string[] LanguageNames { get; }
    public int SelectedLanguageIndex
    {
        get => SelectedLanguage == "en-US" ? 1 : 0;
        set { if (value is 0 or 1) { SelectedLanguage = value == 1 ? "en-US" : "es-ES"; Notify(nameof(SelectedLanguageIndex)); } }
    }
    public int SelectedThemeIndex
    {
        get => SelectedTheme == "Dark" ? 2 : SelectedTheme == "Light" ? 1 : 0;
        set { if (value is >= 0 and <= 2) { SelectedTheme = new[] { "System", "Light", "Dark" }[value]; Notify(nameof(SelectedThemeIndex)); } }
    }
    public string[] ThemeNames => [_language.Get("SystemTheme"), _language.Get("LightTheme"), _language.Get("DarkTheme")];
    public bool DetailsExpanded => _detailsExpanded;
    public string DetailsToggleText => _language.Get(_detailsExpanded ? "DetailsExpanded" : "DetailsCollapsed");
    public decimal SaleMultiplier => _settings.DefaultSaleMultiplier;
    public string MultiplierText => _language.Get("TimesGlyph") + _language.Number(SaleMultiplier, "0.0###########################");
    public string MultiplierNotice => SaleMultiplier < 1 ? _language.Get("LegacyMultiplierHint") : "";
    public bool HasLegacyMultiplier => SaleMultiplier < 1;
    public string SelectedTheme
    {
        get => _selectedTheme;
        set
        {
            if (value is not ("System" or "Light" or "Dark") || !Set(ref _selectedTheme, value)) return;
            _settings = _settings with { Theme = value };
            Notify(nameof(SelectedThemeIndex));
            ThemeChanged?.Invoke(this, EventArgs.Empty);
            ScheduleSave();
        }
    }
    public event EventHandler? ThemeChanged;
    private void ChangeMultiplier(decimal step)
    {
        // Preserve any old value on load; the first deliberate step adopts the minimum of 1.
        _settings = _settings with { DefaultSaleMultiplier = Math.Max(1m, SaleMultiplier + step) };
        ScheduleSave();
        IncreaseMultiplierCommand.Refresh(); DecreaseMultiplierCommand.Refresh();
        Recalculate();
    }
    public ObservableCollection<FilamentCardViewModel> InventoryCards { get; } = [];
    public string MaterialDetails => SelectedFilament is { } f ? string.Join(" · ", new[] { f.MaterialType, f.Brand }.Where(v => !string.IsNullOrWhiteSpace(v))) : _language.Get("Unavailable");
    public bool HasInvalidCalculation => !HasCalculation;
    public ObservableCollection<FilamentProfile> Filaments { get; }
    public ObservableCollection<FilamentProfile> ActiveFilaments { get; } = [];
    public FilamentProfile? SelectedFilament { get => _selectedFilament; set { if (Set(ref _selectedFilament, value)) Recalculate(); } }
    public FilamentProfile? InventorySelection
    {
        get => _inventorySelection;
        set { if (Set(ref _inventorySelection, value)) { EditCommand.Refresh(); DeleteCommand.Refresh(); ToggleActiveCommand.Refresh(); } }
    }
    public int Page { get => _page; set { if (Set(ref _page, value)) { Notify(nameof(IsCalculator)); Notify(nameof(IsInventory)); Notify(nameof(IsSettings)); } } }
    public bool IsCalculator => Page == 0;
    public bool IsInventory => Page == 1;
    public bool IsSettings => Page == 2;
    public NumericField Weight { get; }
    public NumericField Hours { get; }
    public NumericField Minutes { get; }
    public NumericField ElectricityPrice { get; }
    public NumericField HeatingPower { get; }
    public NumericField HeatingMinutes { get; }
    public NumericField MachineRate { get; }
    private NumericField[] CalculationFields => [Weight, Hours, Minutes];
    private NumericField[] SettingsFields => [ElectricityPrice, HeatingPower, HeatingMinutes, MachineRate];
    public string SelectedLanguage
    {
        get => _selectedLanguage;
        set
        {
            if (!Set(ref _selectedLanguage, value) || value is not ("es-ES" or "en-US")) return;
            _language.SetLanguage(value);
            Notify(nameof(SelectedLanguageIndex));
            // Language persists even when a different settings field is temporarily invalid.
            _settings = _settings with { Language = value };
            ScheduleSave();
        }
    }
    public string MaterialType => SelectedFilament?.MaterialType ?? _language.Get("Unavailable");
    public string MaterialPrice => SelectedFilament is { } f ? _language.Currency(f.PricePerKg) + _language.Get("PerKg") : _language.Get("Unavailable");
    public string MaterialPower => SelectedFilament is { } f ? _language.WithUnit(f.PrintPowerWatts, "Watts") : _language.Get("Unavailable");
    public string ElectricitySummary => _language.Currency(_settings.ElectricityPricePerKWh, 4) + _language.Get("PerKWh");
    public string HeatingPowerSummary => _language.WithUnit(_settings.HeatingPowerWatts, "Watts");
    public string HeatingTimeSummary => _language.WithUnit(_settings.HeatingTimeMinutes, "Minutes");
    public string MachineSummary => _language.Currency(_settings.MachineCostPerHour) + _language.Get("PerHour");
    public PrintCalculationResult? Result => _result;
    public bool HasCalculation => _hasCalculation;
    public string CalculationMessage => ActiveFilaments.Count == 0 ? _language.Get("NoActiveFilaments")
        : _language.Get(SettingsFields.Any(f => f.HasErrors) ? "InvalidSettings" : "InvalidCalculation");
    public string SettingsStatus => _language.Get(SettingsFields.Any(f => f.HasErrors) ? "InvalidSettings" : _settingsSaved ? "SavedAutomatically"
        : _notices.Any(n => n.Issue == StorageIssue.SaveFailed && n.FileName == "settings.json") ? "SaveError" : "Saving");
    public string StorageMessage => string.Join(Environment.NewLine, _notices.Select(n => string.Format(_language.Culture,
        _language.Get(n.Issue.ToString()), n.FileName))) + (_actionFailed ? Environment.NewLine + _language.Get("ActionFailed") : "");
    public bool HasStorageMessage => _notices.Count > 0 || _actionFailed;
    private string Money(Func<PrintCalculationResult, decimal> select) => !_hasCalculation || _result is null ? _language.Get("Unavailable") : _language.Currency(select(_result));
    public string MaterialCost => Money(r => r.MaterialCost);
    public string ElectricityCost => Money(r => r.ElectricityCost);
    public string PieceCost => Money(r => r.PieceCost);
    public string MachineCost => Money(r => r.MachineCost);
    public string SalePrice => Money(r => r.SuggestedSalePrice);
    public string Profit => Money(r => r.GrossMargin);
    public string PricePerGram => !_hasCalculation || _result is null ? _language.Get("Unavailable") : _language.Currency(_result.PricePerGram, 4) + _language.Get("PerGram");
    public string HeatingEnergy => Energy(r => r.HeatingEnergyKWh);
    public string PrintingEnergy => Energy(r => r.PrintingEnergyKWh);
    public string TotalEnergy => Energy(r => r.TotalEnergyKWh);
    public string PreciseElectricity => !_hasCalculation || _result is null ? _language.Get("Unavailable") : _language.Currency(_result.ElectricityCost, 4);
    public string PrecisePieceCost => !_hasCalculation || _result is null ? _language.Get("Unavailable") : _language.Currency(_result.PieceCost, 5);
    public string RawSalePrice => !_hasCalculation || _result is null ? _language.Get("Unavailable") : _language.Currency(_result.RawSalePrice, 5);
    private string Energy(Func<PrintCalculationResult, decimal> select) => !_hasCalculation || _result is null ? _language.Get("Unavailable") : _language.WithUnit(select(_result), "KWh");
    public AsyncRelayCommand AddCommand { get; }
    public AsyncRelayCommand EditCommand { get; }
    public AsyncRelayCommand DeleteCommand { get; }
    public RelayCommand ToggleActiveCommand { get; }
    public RelayCommand IncreaseMultiplierCommand { get; }
    public RelayCommand DecreaseMultiplierCommand { get; }
    public RelayCommand ToggleDetailsCommand { get; }
    public RelayCommand CalculatorCommand { get; }
    public RelayCommand InventoryCommand { get; }
    public RelayCommand SettingsCommand { get; }

    private void Recalculate()
    {
        _hasCalculation = SelectedFilament is not null && CalculationFields.All(f => !f.HasErrors) && SettingsFields.All(f => !f.HasErrors);
        if (_hasCalculation)
        {
            try { _result = _calculator.Calculate(new(SelectedFilament!, Weight.Value, PrintDuration.ToHours(Hours.Value, Minutes.Value), _settings.DefaultSaleMultiplier, _settings)); }
            catch (OverflowException) { _hasCalculation = false; }
        }
        // Keep the last valid result internally, but hide it while inputs are invalid.
        Notify(null);
    }
    private void UpdateSettings()
    {
        if (SettingsFields.All(f => !f.HasErrors))
        {
            _settings = _settings with
            {
                ElectricityPricePerKWh = ElectricityPrice.Value, HeatingPowerWatts = HeatingPower.Value,
                HeatingTimeMinutes = HeatingMinutes.Value, MachineCostPerHour = MachineRate.Value
            };
            ScheduleSave();
        }
        Recalculate();
    }
    private void ScheduleSave()
    {
        _settingsDirty = true;
        _settingsSaved = false;
        _saveTimer.Change(400, Timeout.Infinite);
        Notify(nameof(SettingsStatus));
    }
    public void FlushSettings()
    {
        if (_disposed) return;
        _saveTimer.Change(Timeout.Infinite, Timeout.Infinite);
        if (!_settingsDirty) return;
        if (TrySave(() => _store.SaveSettings(_settings), "settings.json")) { _settingsDirty = false; _settingsSaved = true; }
        Notify(nameof(SettingsStatus));
    }
    private bool TrySave(Action action, string fileName)
    {
        try
        {
            action();
            _notices.RemoveAll(n => n.Issue == StorageIssue.SaveFailed && n.FileName == fileName);
            Notify(nameof(StorageMessage)); Notify(nameof(HasStorageMessage));
            return true;
        }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
        {
            if (!_notices.Any(n => n.Issue == StorageIssue.SaveFailed && n.FileName == fileName)) _notices.Add(new(StorageIssue.SaveFailed, fileName));
            Notify(nameof(StorageMessage)); Notify(nameof(HasStorageMessage));
            return false;
        }
    }
    private void RefreshActiveFilaments(Guid? preferredId)
    {
        InventoryCards.Clear();
        foreach (var profile in Filaments) InventoryCards.Add(new(profile, _language));
        ActiveFilaments.Clear();
        foreach (var filament in Filaments.Where(f => f.IsActive)) ActiveFilaments.Add(filament);
        SelectedFilament = ActiveFilaments.FirstOrDefault(f => f.Id == preferredId) ?? ActiveFilaments.FirstOrDefault();
        Recalculate();
    }
    private async Task ShowEditorAsync(FilamentProfile? original)
    {
        var model = await _dialogs.EditFilamentAsync(original);
        if (model is null || !model.IsValid()) return;
        var candidate = Filaments.ToList();
        if (original is null) candidate.Add(model); else candidate[candidate.IndexOf(original)] = model;
        if (!TrySave(() => _store.SaveFilaments(candidate), "filaments.json")) return;
        var selectedId = SelectedFilament?.Id;
        if (original is null) Filaments.Add(model); else Filaments[Filaments.IndexOf(original)] = model;
        InventorySelection = model;
        RefreshActiveFilaments(selectedId);
    }
    private async Task DeleteFilamentAsync()
    {
        if (InventorySelection is not { } item) return;
        if (!await _dialogs.ConfirmDeleteAsync(item.DisplayName)) return;
        var candidate = Filaments.Where(f => f.Id != item.Id).ToList();
        if (!TrySave(() => _store.SaveFilaments(candidate), "filaments.json")) return;
        var selectedId = SelectedFilament?.Id;
        Filaments.Remove(item);
        InventorySelection = null;
        RefreshActiveFilaments(selectedId);
    }
    private void ToggleActive()
    {
        if (InventorySelection is not { } original) return;
        var model = original with { IsActive = !original.IsActive };
        var candidate = Filaments.ToList();
        candidate[candidate.IndexOf(original)] = model;
        if (!TrySave(() => _store.SaveFilaments(candidate), "filaments.json")) return;
        var selectedId = SelectedFilament?.Id;
        Filaments[Filaments.IndexOf(original)] = model;
        InventorySelection = model;
        RefreshActiveFilaments(selectedId);
    }
    public async Task OpenFilamentActionsAsync(FilamentCardViewModel card)
    {
        InventorySelection = card.Profile;
        switch (await _dialogs.ChooseFilamentActionAsync(card.Profile))
        {
            case FilamentAction.Edit: await ShowEditorAsync(card.Profile); break;
            case FilamentAction.ToggleActive: ToggleActive(); break;
            case FilamentAction.Delete: await DeleteFilamentAsync(); break;
        }
    }
    public void ActionError()
    {
        _actionFailed = true;
        Notify(nameof(StorageMessage)); Notify(nameof(HasStorageMessage));
    }
    private void LanguageChanged(object? sender, EventArgs e)
    {
        foreach (var field in CalculationFields.Concat(SettingsFields)) field.RefreshLanguage();
        foreach (var card in InventoryCards) card.RefreshLanguage();
        Notify(null);
        Notify(nameof(SelectedThemeIndex));
    }
    public void Dispose()
    {
        if (_disposed) return;
        FlushSettings();
        _language.Changed -= LanguageChanged;
        _disposed = true;
        _saveTimer.Dispose();
    }
}
