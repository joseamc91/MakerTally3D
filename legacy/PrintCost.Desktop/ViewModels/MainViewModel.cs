using System.Collections.ObjectModel;
using System.IO;
using System.Windows;
using System.Windows.Threading;
using PrintCost.Core;
using PrintCost.Desktop.Infrastructure;
using PrintCost.Desktop.Views;

namespace PrintCost.Desktop.ViewModels;

public sealed class MainViewModel : ObservableObject, IDisposable
{
    private readonly JsonDataStore _store;
    private readonly PrintCostCalculationService _calculator = new();
    private readonly LocalizationService _language = LocalizationService.Instance;
    private readonly DispatcherTimer _saveTimer = new() { Interval = TimeSpan.FromMilliseconds(400) };
    private readonly List<StorageNotice> _notices;
    private AppSettings _settings;
    private FilamentProfile? _selectedFilament;
    private FilamentProfile? _inventorySelection;
    private string _selectedLanguage;
    private int _page;
    private bool _settingsDirty;
    private bool _hasCalculation;
    private bool _settingsSaved = true;
    private PrintCalculationResult? _result;
    public MainViewModel(JsonDataStore store, LoadedData data)
    {
        _store = store;
        _settings = data.Settings;
        _notices = data.Notices.ToList();
        _selectedLanguage = _settings.Language;
        Filaments = new(data.Filaments);
        Weight = new(141m);
        Hours = new(6m);
        Multiplier = new(_settings.DefaultSaleMultiplier);
        ElectricityPrice = new(_settings.ElectricityPricePerKWh);
        HeatingPower = new(_settings.HeatingPowerWatts);
        HeatingMinutes = new(_settings.HeatingTimeMinutes);
        MachineRate = new(_settings.MachineCostPerHour);
        DefaultMultiplier = new(_settings.DefaultSaleMultiplier);
        foreach (var field in CalculationFields) field.Edited += (_, _) => Recalculate();
        foreach (var field in SettingsFields) field.Edited += (_, _) => UpdateSettings();
        AddCommand = new(AddFilament);
        EditCommand = new(EditFilament, () => InventorySelection is not null);
        DeleteCommand = new(DeleteFilament, () => InventorySelection is not null);
        CalculatorCommand = new(() => Page = 0);
        InventoryCommand = new(() => Page = 1);
        SettingsCommand = new(() => Page = 2);
        _saveTimer.Tick += SaveTimerTick;
        _language.Changed += LanguageChanged;
        RefreshActiveFilaments(data.Filaments.FirstOrDefault(f => f.Name == "ASA eSun")?.Id);
    }

    public ObservableCollection<FilamentProfile> Filaments { get; }
    public ObservableCollection<FilamentProfile> ActiveFilaments { get; } = [];
    public FilamentProfile? SelectedFilament { get => _selectedFilament; set { if (Set(ref _selectedFilament, value)) Recalculate(); } }
    public FilamentProfile? InventorySelection
    {
        get => _inventorySelection;
        set { if (Set(ref _inventorySelection, value)) { EditCommand.Refresh(); DeleteCommand.Refresh(); } }
    }
    public int Page { get => _page; set { if (Set(ref _page, value)) { Notify(nameof(IsCalculator)); Notify(nameof(IsInventory)); Notify(nameof(IsSettings)); } } }
    public bool IsCalculator => Page == 0;
    public bool IsInventory => Page == 1;
    public bool IsSettings => Page == 2;
    public NumericField Weight { get; }
    public NumericField Hours { get; }
    public NumericField Multiplier { get; }
    public NumericField ElectricityPrice { get; }
    public NumericField HeatingPower { get; }
    public NumericField HeatingMinutes { get; }
    public NumericField MachineRate { get; }
    public NumericField DefaultMultiplier { get; }
    private NumericField[] CalculationFields => [Weight, Hours, Multiplier];
    private NumericField[] SettingsFields => [ElectricityPrice, HeatingPower, HeatingMinutes, MachineRate, DefaultMultiplier];
    public string SelectedLanguage
    {
        get => _selectedLanguage;
        set
        {
            if (!Set(ref _selectedLanguage, value) || value is not ("es-ES" or "en-US")) return;
            _language.SetLanguage(value);
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
        _language.Get(n.Issue.ToString()), n.FileName)));
    public bool HasStorageMessage => _notices.Count > 0;
    private string Money(Func<PrintCalculationResult, decimal> select) => _result is null ? _language.Get("Unavailable") : _language.Currency(select(_result));
    public string MaterialCost => Money(r => r.MaterialCost);
    public string ElectricityCost => Money(r => r.ElectricityCost);
    public string PrintCost => Money(r => r.PrintCost);
    public string MachineCost => Money(r => r.MachineCost);
    public string FullCost => Money(r => r.FullCost);
    public string SalePrice => Money(r => r.SuggestedSalePrice);
    public string Profit => Money(r => r.GrossProfit);
    public string Margin => _result is null ? _language.Get("Unavailable") : _language.WithUnit(_result.MarginPercentage, "Percent", "0.00");
    public string PricePerGram => _result is null ? _language.Get("Unavailable") : _language.Currency(_result.PricePerGram, 4) + _language.Get("PerGram");
    public string HeatingEnergy => Energy(r => r.HeatingEnergyKWh);
    public string PrintingEnergy => Energy(r => r.PrintingEnergyKWh);
    public string TotalEnergy => Energy(r => r.TotalEnergyKWh);
    public string PreciseElectricity => _result is null ? _language.Get("Unavailable") : _language.Currency(_result.ElectricityCost, 4);
    private string Energy(Func<PrintCalculationResult, decimal> select) => _result is null ? _language.Get("Unavailable") : _language.WithUnit(select(_result), "KWh");
    public RelayCommand AddCommand { get; }
    public RelayCommand EditCommand { get; }
    public RelayCommand DeleteCommand { get; }
    public RelayCommand CalculatorCommand { get; }
    public RelayCommand InventoryCommand { get; }
    public RelayCommand SettingsCommand { get; }

    private void Recalculate()
    {
        _hasCalculation = SelectedFilament is not null && CalculationFields.All(f => !f.HasErrors) && SettingsFields.All(f => !f.HasErrors);
        if (_hasCalculation)
        {
            try { _result = _calculator.Calculate(new(SelectedFilament!, Weight.Value, Hours.Value, Multiplier.Value, _settings)); }
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
                HeatingTimeMinutes = HeatingMinutes.Value, MachineCostPerHour = MachineRate.Value,
                DefaultSaleMultiplier = DefaultMultiplier.Value
            };
            ScheduleSave();
        }
        Recalculate();
    }
    private void ScheduleSave()
    {
        _settingsDirty = true;
        _settingsSaved = false;
        _saveTimer.Stop();
        _saveTimer.Start();
        Notify(nameof(SettingsStatus));
    }
    private void SaveTimerTick(object? sender, EventArgs e) => FlushSettings();
    public void FlushSettings()
    {
        _saveTimer.Stop();
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
        ActiveFilaments.Clear();
        foreach (var filament in Filaments.Where(f => f.IsActive)) ActiveFilaments.Add(filament);
        SelectedFilament = ActiveFilaments.FirstOrDefault(f => f.Id == preferredId) ?? ActiveFilaments.FirstOrDefault();
        Recalculate();
    }
    private void AddFilament() => ShowEditor(null);
    private void EditFilament() { if (InventorySelection is { } item) ShowEditor(item); }
    private void ShowEditor(FilamentProfile? original)
    {
        var editor = new FilamentEditorViewModel(original);
        var window = new FilamentEditorWindow(editor) { Owner = Application.Current.MainWindow };
        if (window.ShowDialog() != true) return;
        var model = editor.Build();
        var candidate = Filaments.ToList();
        if (original is null) candidate.Add(model); else candidate[candidate.IndexOf(original)] = model;
        if (!TrySave(() => _store.SaveFilaments(candidate), "filaments.json")) return;
        var selectedId = SelectedFilament?.Id;
        if (original is null) Filaments.Add(model); else Filaments[Filaments.IndexOf(original)] = model;
        InventorySelection = model;
        RefreshActiveFilaments(selectedId);
    }
    private void DeleteFilament()
    {
        if (InventorySelection is not { } item) return;
        var confirmation = new DeleteConfirmationWindow(item.Name) { Owner = Application.Current.MainWindow };
        if (confirmation.ShowDialog() != true) return;
        var candidate = Filaments.Where(f => f.Id != item.Id).ToList();
        if (!TrySave(() => _store.SaveFilaments(candidate), "filaments.json")) return;
        var selectedId = SelectedFilament?.Id;
        Filaments.Remove(item);
        InventorySelection = null;
        RefreshActiveFilaments(selectedId);
    }
    private void LanguageChanged(object? sender, EventArgs e)
    {
        foreach (var field in CalculationFields.Concat(SettingsFields)) field.RefreshLanguage();
        Notify(null);
    }
    public void Dispose()
    {
        FlushSettings();
        _language.Changed -= LanguageChanged;
        _saveTimer.Tick -= SaveTimerTick;
    }
}
