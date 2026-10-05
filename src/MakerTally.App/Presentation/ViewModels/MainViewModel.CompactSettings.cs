using MakerTally.App.Presentation.Infrastructure;

namespace MakerTally.App.Presentation.ViewModels;

public sealed partial class MainViewModel
{
    private RelayCommand? _increasePower, _decreasePower, _increaseHeatingTime, _decreaseHeatingTime, _increaseMachine, _decreaseMachine;
    public RelayCommand IncreaseHeatingPowerCommand => _increasePower ??= SettingCommand(HeatingPower, 100m);
    public RelayCommand DecreaseHeatingPowerCommand => _decreasePower ??= SettingCommand(HeatingPower, -100m);
    public RelayCommand IncreaseHeatingTimeCommand => _increaseHeatingTime ??= SettingCommand(HeatingMinutes, 1m);
    public RelayCommand DecreaseHeatingTimeCommand => _decreaseHeatingTime ??= SettingCommand(HeatingMinutes, -1m);
    public RelayCommand IncreaseMachineRateCommand => _increaseMachine ??= SettingCommand(MachineRate, 0.05m);
    public RelayCommand DecreaseMachineRateCommand => _decreaseMachine ??= SettingCommand(MachineRate, -0.05m);
    public string DetailsChevron => _language.Get(DetailsExpanded ? "ChevronDown" : "ChevronRight");
    public string VersionFooter => _language.Get("Version") + " · " + _language.Get("AlphaShort");

    private RelayCommand SettingCommand(NumericField field, decimal step)
    {
        // Subtracting a fractional step from decimal.MaxValue can round back to MaxValue.
        bool CanStep() => !field.HasErrors && (step > 0
            ? field.Value < decimal.MaxValue && field.Value <= decimal.MaxValue - step
            : field.Value > 0);
        return new(() =>
        {
            if (CanStep()) field.Text = _language.Number(Math.Max(0m, field.Value + step), "0.############################");
        }, CanStep);
    }

    private void InitializeCompactSettings()
    {
        foreach (var field in SettingsFields) field.Edited += (_, _) =>
        {
            _increasePower?.Refresh(); _decreasePower?.Refresh();
            _increaseHeatingTime?.Refresh(); _decreaseHeatingTime?.Refresh();
            _increaseMachine?.Refresh(); _decreaseMachine?.Refresh();
        };
        PropertyChanged += (_, e) =>
        {
            if (e.PropertyName == nameof(DetailsExpanded)) Notify(nameof(DetailsChevron));
        };
    }
}
