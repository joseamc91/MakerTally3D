using MakerTally.App;
using MakerTally.App.Presentation.Services;
using MakerTally.App.Presentation.ViewModels;
using MakerTally.Core;

namespace MakerTally.Tests;

public sealed class CompactUtilityTests : IDisposable
{
    private readonly string _directory = Path.Combine(Path.GetTempPath(), "MakerTally.Alpha4.Tests", Guid.NewGuid().ToString());
    private static readonly WindowBounds[] Areas = [new(0, 0, 1920, 1040), new(-1280, 0, 1280, 984)];

    [Fact]
    public void SettingsStepsAreExactPersistAndRecalculateWithoutChangingFormulas()
    {
        var store = new JsonDataStore(_directory);
        using var vm = new MainViewModel(store, store.Load(), new NoDialogs(), new LocalizationService());
        vm.IncreaseHeatingPowerCommand.Execute(null);
        Assert.Equal(1300m, vm.HeatingPower.Value);
        Assert.InRange(vm.Result!.HeatingEnergyKWh, 0.0216666666666666666666666666m, 0.0216666666666666666666666668m);
        vm.DecreaseHeatingPowerCommand.Execute(null);
        vm.IncreaseHeatingTimeCommand.Execute(null);
        Assert.Equal(2m, vm.HeatingMinutes.Value);
        vm.DecreaseHeatingTimeCommand.Execute(null);
        vm.IncreaseMachineRateCommand.Execute(null);
        Assert.Equal(0.30m, vm.MachineRate.Value);
        Assert.Equal(1.80m, vm.Result!.MachineCost);
        vm.DecreaseMachineRateCommand.Execute(null);
        vm.IncreaseMultiplierCommand.Execute(null);
        Assert.Equal(3.5m, vm.SaleMultiplier);
        vm.DecreaseMultiplierCommand.Execute(null);
        Assert.Equal(9.35m, vm.Result!.SuggestedSalePrice);
        Assert.Equal(5.24m, vm.Result.GrossMargin);
        vm.FlushSettings();
        var saved = store.Load().Settings;
        Assert.Equal(1200m, saved.HeatingPowerWatts);
        Assert.Equal(1m, saved.HeatingTimeMinutes);
        Assert.Equal(0.25m, saved.MachineCostPerHour);
        Assert.Equal(3m, saved.DefaultSaleMultiplier);
    }

    [Fact]
    public void StepsClampToZeroPreserveLegacyPrecisionAndRejectInvalidOrOverflowingInput()
    {
        var store = new JsonDataStore(_directory);
        using var vm = new MainViewModel(store, store.Load(), new NoDialogs(), new LocalizationService());
        var controls = new[]
        {
            (vm.HeatingPower, vm.IncreaseHeatingPowerCommand, vm.DecreaseHeatingPowerCommand, 100m),
            (vm.HeatingMinutes, vm.IncreaseHeatingTimeCommand, vm.DecreaseHeatingTimeCommand, 1m),
            (vm.MachineRate, vm.IncreaseMachineRateCommand, vm.DecreaseMachineRateCommand, 0.05m)
        };
        foreach (var (field, increase, decrease, step) in controls)
        {
            field.Text = "0.0123";
            increase.Execute(null); Assert.Equal(0.0123m + step, field.Value);
            decrease.Execute(null); Assert.Equal(0.0123m, field.Value);
            decrease.Execute(null); Assert.Equal(0m, field.Value);
            Assert.False(decrease.CanExecute(null));
            decrease.Execute(null); Assert.Equal(0m, field.Value);
            field.Text = ",";
            Assert.False(increase.CanExecute(null)); Assert.False(decrease.CanExecute(null));
            increase.Execute(null); Assert.Equal(",", field.Text);
            field.Text = decimal.MaxValue.ToString(System.Globalization.CultureInfo.InvariantCulture);
            Assert.False(increase.CanExecute(null));
            increase.Execute(null); Assert.Equal(decimal.MaxValue, field.Value);
            field.Text = "0";
        }
    }

    [Fact]
    public void DetailsStartClosedAndChevronTracksExpansion()
    {
        var store = new JsonDataStore(_directory);
        using var vm = new MainViewModel(store, store.Load(), new NoDialogs(), new LocalizationService());
        Assert.False(vm.DetailsExpanded); Assert.Equal(vm.L["ChevronRight"], vm.DetailsChevron);
        var notifications = new List<string?>();
        vm.PropertyChanged += (_, e) => notifications.Add(e.PropertyName);
        vm.ToggleDetailsCommand.Execute(null);
        Assert.True(vm.DetailsExpanded); Assert.Equal(vm.L["ChevronDown"], vm.DetailsChevron);
        Assert.Contains(nameof(vm.DetailsChevron), notifications);
    }

    [Fact]
    public void FirstWindowIsCompactCenteredAndReachable()
        => Assert.Equal(new WindowBounds(735, 140, 450, 760), WindowPlacementPolicy.Restore(null, Areas));

    [Fact]
    public void ValidWindowOnNegativeCoordinateMonitorIsPreserved()
    {
        var saved = new WindowBounds(-1100, 80, 500, 800);
        Assert.Equal(saved, WindowPlacementPolicy.Restore(saved, Areas));
    }

    [Theory]
    [InlineData(100000, 100000, 450, 760)]
    [InlineData(0, -740, 450, 760)]
    [InlineData(1800, 1000, 450, 760)]
    public void MissingMonitorOrUnreachableTitleBarRecenters(int x, int y, int width, int height)
        => Assert.Equal(new WindowBounds(735, 140, 450, 760), WindowPlacementPolicy.Restore(new(x, y, width, height), [Areas[0]]));

    [Fact]
    public void PartialVisibleWindowIsClampedEntirelyInsideWorkArea()
        => Assert.Equal(new WindowBounds(1470, 140, 450, 760), WindowPlacementPolicy.Restore(new(1700, 140, 450, 760), Areas));

    [Theory]
    [InlineData(0, -1, 450, 760)]
    [InlineData(50, 10, 400, 560)]
    [InlineData(int.MaxValue, int.MaxValue, 1920, 1040)]
    public void InvalidOrExtremeWindowSizesHaveSafeBounds(int width, int height, int expectedWidth, int expectedHeight)
    {
        var result = WindowPlacementPolicy.Restore(new(int.MaxValue, int.MaxValue, width, height), [Areas[0]]);
        Assert.Equal(expectedWidth, result.Width); Assert.Equal(expectedHeight, result.Height);
        Assert.InRange(result.X, 0, 1920 - result.Width); Assert.InRange(result.Y, 0, 1040 - result.Height);
    }

    [Fact]
    public void SmallWorkAreaTakesPriorityOverNominalMinimum()
        => Assert.Equal(new WindowBounds(10, 20, 320, 480), WindowPlacementPolicy.Restore(null, [new(10, 20, 320, 480)]));

    [Fact]
    public void WindowPlacementRoundTripsSeparatelyAndToleratesCorruptOrUnavailableFiles()
    {
        var path = Path.Combine(_directory, "window.json");
        var store = new WindowPlacementStore(path);
        Assert.Null(store.Load());
        var bounds = new WindowBounds(-1100, 40, 400, 700);
        store.Save(bounds); Assert.Equal(bounds, store.Load());
        Assert.False(File.Exists(Path.Combine(_directory, "settings.json")));
        File.WriteAllText(path, "{broken"); Assert.Null(store.Load());
        store.Save(bounds); Assert.Equal(bounds, store.Load());
        Directory.CreateDirectory(Path.Combine(_directory, "blocked"));
        var unavailable = new WindowPlacementStore(Path.Combine(_directory, "blocked"));
        unavailable.Save(bounds); Assert.Null(unavailable.Load());
    }

    public void Dispose() { if (Directory.Exists(_directory)) Directory.Delete(_directory, true); }

    private sealed class NoDialogs : IUserDialogs
    {
        public Task<FilamentProfile?> EditFilamentAsync(FilamentProfile? original) => Task.FromResult<FilamentProfile?>(null);
        public Task<bool> ConfirmDeleteAsync(string name) => Task.FromResult(false);
        public Task<FilamentAction> ChooseFilamentActionAsync(FilamentProfile filament) => Task.FromResult(FilamentAction.Cancel);
    }
}
