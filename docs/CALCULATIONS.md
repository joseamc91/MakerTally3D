# Calculations and precision

[Español](CALCULATIONS.es.md) · **English**

Currency is EUR. Inputs use grams, watts, minutes, hours, €/kWh and €/hour. The UI accepts comma or dot decimals, nonnegative values, positive spool weight, whole hours and minutes 0–59. Its sale multiplier minimum is 1. Legacy decoding/model compatibility remains separate from first-run setup.

## Calculation contract

```text
TimeHours = Hours + Minutes / 60
PricePerGram = PurchasePrice / SpoolWeightGrams
MaterialCost = PieceWeightGrams × PricePerGram
HeatingEnergy = HeatingPower × HeatingMinutes / 60000
PrintingEnergy = PrintPower × (Hours × 60 + Minutes) / 60000
TotalEnergy = HeatingEnergy + PrintingEnergy
EffectiveElectricityPrice = ElectricityPrice × (1 + ElectricityTaxPercent / 100)
ElectricityCost = TotalEnergy × EffectiveElectricityPrice
PieceCost = MaterialCost + ElectricityCost
MachineCost = MachineRate × TimeHours
RawSalePrice = PieceCost × SaleMultiplier + MachineCost
SuggestedSalePrice = ROUNDUP(RawSalePrice, 2)
RawGrossMargin = SuggestedSalePrice − PieceCost − MachineCost
GrossMargin = ROUNDUP(RawGrossMargin, 2)
```

`PricePerKg = PurchasePrice × 1000 / SpoolWeightGrams` remains a profile/card value. It is not displayed in the editor.

PieceCost excludes machine cost. The multiplier applies to PieceCost; MachineCost is added afterwards. Gross margin here is an estimated absolute EUR amount, not an accounting or tax calculation. Electricity taxes represent the effective increase over the base price, not a sales-tax feature. If a tariff already includes all taxes, enter its final €/kWh price and tax 0%.

## Exact arithmetic and rounding

Monetary calculations use BigDecimal, with no Float/Double conversion. The internal `Ratio` retains exact fractions across divisions and operations; intermediate costs are not rounded. Non-terminating technical values are projected to DECIMAL128 for presentation/returned diagnostics only. The final price and margin are rounded directly from the exact ratios.

`RoundingMode.UP` implements Excel ROUNDUP: away from zero, including negative rounding test cases. It is not CEILING. Both final money values use two decimal places; display formatting does not change stored precision. Heating is an additional fixed energy term; printing energy covers the entered duration, as in the preserved contract.

## Technical regression fixture

The historical ASA/eSun fixture is test data, **not a first-run recommendation**: 1000 g spool at €17.50, 180 W, piece 141 g, duration 6 h, electricity €0.1349/kWh, taxes 0%, warm-up 1200 W/1 min, machine €0.25/h and multiplier 3.

| Value | Expected |
| --- | --- |
| MaterialCost | 2.4675 |
| ElectricityCost | 0.14839 |
| PieceCost | 2.61589 |
| MachineCost | 1.50 |
| RawSalePrice | 9.34767 |
| SuggestedSalePrice | 9.35 |
| RawGrossMargin | 5.23411 |
| GrossMargin | 5.24 |

Tests also cover zero values, invalid/incomplete input, range limits, repeating fractions, decimal separators, printing power, effective taxes and stored precision. Reference wattages are explanatory only; measurement improves the inputs, without changing the engine.
