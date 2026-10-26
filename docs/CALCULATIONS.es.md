# Cálculos y precisión

**Español** · [English](CALCULATIONS.md)

La moneda es EUR. Se utilizan gramos, vatios, minutos, horas, €/kWh y €/h. La UI admite coma o punto, valores no negativos, peso de bobina positivo, horas enteras y minutos 0–59. El multiplicador mínimo de la UI es 1. La compatibilidad de datos legacy permanece separada del primer arranque.

## Contrato matemático

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

`PricePerKg = PurchasePrice × 1000 / SpoolWeightGrams` sigue siendo un dato del perfil/tarjeta, sin mostrarse en el editor.

PieceCost excluye máquina. El multiplicador se aplica a PieceCost y después se añade MachineCost. El margen bruto es una estimación absoluta en EUR, no un cálculo contable o fiscal. Los impuestos eléctricos son el incremento efectivo del precio base, no impuestos de venta. Si la tarifa ya incluye todo, se introduce el precio final por kWh e impuestos 0%.

## Aritmética exacta y redondeo

Los importes utilizan BigDecimal, sin conversión a Float/Double. `Ratio` conserva fracciones exactas durante divisiones y operaciones; no se redondean costes intermedios. Los valores técnicos periódicos solo se proyectan a DECIMAL128 para presentación/diagnóstico. Precio y margen se redondean directamente desde los ratios exactos.

`RoundingMode.UP` equivale a ROUNDUP de Excel: alejándose de cero, incluidos los casos negativos de prueba. No equivale a CEILING. Los dos importes finales tienen dos decimales; el formato visible no modifica la precisión guardada. El calentamiento es un término energético fijo adicional y la impresión cubre la duración introducida, según el contrato preservado.

## Fixture técnico de regresión

El caso histórico ASA/eSun es un test, **no una recomendación inicial**: bobina de 1000 g a 17,50 €, 180 W, pieza de 141 g, 6 h, electricidad 0,1349 €/kWh, impuestos 0%, calentamiento 1200 W/1 min, máquina 0,25 €/h y multiplicador 3.

| Valor | Resultado |
| --- | --- |
| MaterialCost | 2.4675 |
| ElectricityCost | 0.14839 |
| PieceCost | 2.61589 |
| MachineCost | 1.50 |
| RawSalePrice | 9.34767 |
| SuggestedSalePrice | 9.35 |
| RawGrossMargin | 5.23411 |
| GrossMargin | 5.24 |

También se prueban ceros, entradas inválidas/incompletas, límites, fracciones periódicas, separadores, potencia, impuestos efectivos y precisión almacenada. Los vatios de referencia son explicativos: medir mejora los datos de entrada, sin cambiar el motor.
