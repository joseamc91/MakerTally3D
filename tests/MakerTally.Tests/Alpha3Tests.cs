using System.Text.Json;
using MakerTally.Core;
using MakerTally.App.Presentation.Services;
using MakerTally.App.Presentation.ViewModels;

namespace MakerTally.Tests;

public sealed class Alpha3Tests : IDisposable
{
    private readonly string _directory = Path.Combine(Path.GetTempPath(), "MakerTally.Alpha3.Tests", Guid.NewGuid().ToString());
    private JsonDataStore Store => new(_directory);
    private readonly LocalizationService _language = new();

    [Theory]
    [InlineData(6, 0, "6")]
    [InlineData(6, 30, "6.5")]
    [InlineData(5, 45, "5.75")]
    [InlineData(0, 0, "0")]
    public void DurationUsesExactDecimalHours(int hours, int minutes, string expected)
        => Assert.Equal(decimal.Parse(expected, System.Globalization.CultureInfo.InvariantCulture), PrintDuration.ToHours(hours, minutes));

    [Theory] [InlineData(-1, 0)] [InlineData(1, -1)] [InlineData(1, 60)]
    public void DurationRejectsInvalidRanges(int hours, int minutes)
        => Assert.Throws<ArgumentOutOfRangeException>(() => PrintDuration.ToHours(hours, minutes));
    [Fact] public void DurationRejectsFractionalHoursAndMinutes()
    {
        Assert.Throws<ArgumentOutOfRangeException>(() => PrintDuration.ToHours(1.5m, 0));
        Assert.Throws<ArgumentOutOfRangeException>(() => PrintDuration.ToHours(1, 0.5m));
    }
    [Theory]
    [InlineData("9.34000", "9.34")]
    [InlineData("9.34001", "9.35")]
    [InlineData("9.34101", "9.35")]
    [InlineData("9.34767", "9.35")]
    [InlineData("9.34999", "9.35")]
    [InlineData("9.35000", "9.35")]
    [InlineData("5.23000", "5.23")]
    [InlineData("5.23001", "5.24")]
    [InlineData("5.23411", "5.24")]
    [InlineData("-5.23411", "-5.24")]
    [InlineData("0", "0")]
    public void RoundUpMatchesExcelAtTwoDecimalPlaces(string input, string expected)
    {
        decimal Parse(string s) => decimal.Parse(s, System.Globalization.CultureInfo.InvariantCulture);
        Assert.Equal(Parse(expected), ExcelRounding.RoundUp(Parse(input), 2));
    }
    [Fact] public void RoundUpDoesNotOverflowForAlreadyIntegralLargeDecimals()
        => Assert.Equal(decimal.MaxValue, ExcelRounding.RoundUp(decimal.MaxValue));

    [Fact] public void MultiplierTwoAndHalfUsesRoundedSaleForGrossMargin()
    {
        var result = new MakerTallyCalculationService().Calculate(new(DefaultData.Filaments().Single(f => f.Name == "ASA eSun"), 141, 6, 2.5m, new()));
        Assert.Equal(8.039725m, result.RawSalePrice); Assert.Equal(8.04m, result.SuggestedSalePrice);
        Assert.Equal(3.92411m, result.RawGrossMargin); Assert.Equal(3.93m, result.GrossMargin);
    }
    [Fact] public void StepperUpdatesGlobalMultiplierAndCalculatorWithHalfStepsAndMinimumOne()
    {
        using var vm = new MainViewModel(Store, Store.Load(), new Dialogs(), _language);
        Assert.False(vm.DetailsExpanded); vm.ToggleDetailsCommand.Execute(null); Assert.True(vm.DetailsExpanded);
        vm.DecreaseMultiplierCommand.Execute(null); Assert.Equal(2.5m, vm.SaleMultiplier); Assert.Equal(8.04m, vm.Result?.SuggestedSalePrice);
        vm.IncreaseMultiplierCommand.Execute(null); Assert.Equal(3m, vm.SaleMultiplier);
        for (int i = 0; i < 20; i++) if (vm.DecreaseMultiplierCommand.CanExecute(null)) vm.DecreaseMultiplierCommand.Execute(null);
        Assert.Equal(1m, vm.SaleMultiplier); Assert.False(vm.DecreaseMultiplierCommand.CanExecute(null));
        vm.FlushSettings(); Assert.Equal(1m, Store.Load().Settings.DefaultSaleMultiplier);
    }
    [Fact] public void LegacyZeroMultiplierLoadsWithoutResetAndFirstStepAppliesMinimum()
    {
        var data = Store.Load(); Store.SaveSettings(data.Settings with { DefaultSaleMultiplier = 0, MachineCostPerHour = 0.77m });
        using var vm = new MainViewModel(Store, Store.Load(), new Dialogs(), _language);
        Assert.Equal(0m, vm.SaleMultiplier); Assert.True(vm.HasLegacyMultiplier);
        vm.IncreaseMultiplierCommand.Execute(null); Assert.Equal(1m, vm.SaleMultiplier);
        vm.FlushSettings(); Assert.Equal(0.77m, Store.Load().Settings.MachineCostPerHour);
    }
    [Theory] [InlineData("", "PLA · Bambu Lab")] [InlineData(null, "PLA · Bambu Lab")] [InlineData(" Matte ", "PLA Matte · Bambu Lab")]
    public void DisplayNameUsesTypeVariantAndBrand(string? variant, string expected)
    {
        var filament = new FilamentProfile { Name = "Legacy name", MaterialType = " PLA ", Brand = "Bambu Lab", Variant = variant };
        Assert.Equal(expected, filament.DisplayName); Assert.Equal("Legacy name", filament.Name);
        Assert.DoesNotContain("DisplayName", JsonSerializer.Serialize(filament));
    }
    [Theory] [InlineData("PLA BambuLab Spool", "Spool")] [InlineData("PLA 850 BambuLab", "850")] [InlineData("PLA BambuLab", "")] [InlineData("My special spool", "")]
    public void LegacyMigrationInfersOnlyUnambiguousVariantsAndRetainsName(string name, string variant)
    {
        var original = new FilamentProfile { Name = name, MaterialType = "PLA", Brand = "BambuLab", Notes = "Original notes", IsActive = false };
        var migrated = LegacyFilamentMigration.AddVariant(original);
        Assert.Equal(variant, migrated.Variant); Assert.Equal(name, migrated.Name);
        Assert.Equal(original.Id, migrated.Id); Assert.Equal(original.Notes, migrated.Notes); Assert.False(migrated.IsActive);
    }
    [Fact] public void MigrationIsIdempotentAndExplicitEmptyVariantWins()
    {
        var loaded = Store.Load(); var f = loaded.Filaments[0] with { Variant = "" };
        string legacy = JsonSerializer.Serialize(new[] { f }).Replace("\"Variant\":\"\",", "");
        File.WriteAllText(Path.Combine(_directory, "filaments.json"), legacy);
        for (int i = 0; i < 2; i++)
        {
            Assert.Equal("Spool", Assert.Single(Store.Load().Filaments).Variant);
            Assert.Equal(legacy, File.ReadAllText(Path.Combine(_directory, "filaments.json")));
        }
        Store.SaveFilaments([f]); Assert.Equal("", Assert.Single(Store.Load().Filaments).Variant);
    }
    [Theory] [InlineData("System")] [InlineData("Light")] [InlineData("Dark")]
    public void ThemePersistsAndNotifiesPresentationWithoutChangingOtherSettings(string theme)
    {
        using var vm = new MainViewModel(Store, Store.Load(), new Dialogs(), _language);
        int changes = 0; vm.ThemeChanged += (_, _) => changes++;
        vm.SelectedTheme = theme; vm.FlushSettings();
        Assert.Equal(theme, Store.Load().Settings.Theme); Assert.Equal(0.1349m, Store.Load().Settings.ElectricityPricePerKWh);
        Assert.Equal(theme == "System" ? 0 : 1, changes);
    }
    [Fact] public void EditorDefaultsAndEditsPreserveLegacyNotesNameActivationAndId()
    {
        var editor = new FilamentEditorViewModel(null, _language);
        Assert.Equal(1000m, editor.Weight.Value); Assert.Equal(120m, editor.Power.Value); Assert.Equal("", editor.Price.Text);
        Assert.Equal("", editor.Variant); Assert.False(editor.IsValid);
        editor.MaterialType = "PLA"; editor.Brand = "Bambu Lab"; editor.Variant = "Matte"; editor.Price.Text = "12,5";
        Assert.True(editor.IsValid); var created = editor.Build(); Assert.True(created.IsActive); Assert.Equal("PLA Matte · Bambu Lab", created.DisplayName);
        var legacy = created with { Name = "Old custom name", Notes = "Preserve exactly  ", IsActive = false };
        var edited = new FilamentEditorViewModel(legacy, _language) { Variant = "HS" };
        var saved = edited.Build(); Assert.Equal(legacy.Id, saved.Id); Assert.Equal(legacy.Name, saved.Name); Assert.Equal(legacy.Notes, saved.Notes); Assert.False(saved.IsActive);
        Store.SaveFilaments([saved]); Assert.Equal("HS", Assert.Single(Store.Load().Filaments).Variant);
        edited.Brand = " "; Assert.False(edited.SaveCommand.CanExecute(null));
    }
    [Fact] public void TimeFieldsTolerateIncompleteInputAndRejectFractionalOrOutOfRangeMinutes()
    {
        using var vm = new MainViewModel(Store, Store.Load(), new Dialogs(), _language);
        foreach (string invalid in new[] { "", ",", ".", "-1", "60", "1.5", "1,5" })
        {
            vm.Minutes.Text = invalid; Assert.True(vm.Minutes.HasErrors); Assert.False(vm.HasCalculation);
        }
        vm.Minutes.Text = "30"; Assert.True(vm.HasCalculation); Assert.Equal(1.17m, vm.Result?.PrintingEnergyKWh);
        vm.Hours.Text = "6.5"; Assert.False(vm.HasCalculation);
        vm.Hours.Text = "0"; vm.Minutes.Text = "0"; Assert.True(vm.HasCalculation); Assert.Equal(0m, vm.Result?.MachineCost);
    }
    public void Dispose() { if (Directory.Exists(_directory)) Directory.Delete(_directory, true); }
    private sealed class Dialogs : IUserDialogs
    {
        public Task<FilamentProfile?> EditFilamentAsync(FilamentProfile? original) => Task.FromResult<FilamentProfile?>(null);
        public Task<bool> ConfirmDeleteAsync(string name) => Task.FromResult(false);
        public Task<FilamentAction> ChooseFilamentActionAsync(FilamentProfile filament) => Task.FromResult(FilamentAction.Cancel);
    }
}
