# PrintCost

**v0.1-alpha3 · 0.1.0-alpha.3 · Alpha**

Calcula el coste aproximado y el precio sugerido de una impresión 3D. Funciona completamente offline, sin servidor, cuentas, SQL ni telemetría.

Alpha1 utilizaba WPF. Alpha2 migró la interfaz a .NET MAUI nativo. Alpha3 conserva esa base y simplifica la experiencia para acercar los resultados a las fórmulas oficiales del Excel original.

## Ejecutar en Windows

Descomprime `PrintCost-v0.1-alpha3-ux-win-x64.zip` y abre `PrintCost.exe`. El sufijo `ux` identifica este pulido visual; la versión funcional sigue siendo v0.1-alpha3. Mantén todos los archivos de la carpeta: la publicación sin empaquetar incluye .NET y Windows App SDK. No necesita instalador ni .NET preinstalado. No es un ejecutable único; los datos se guardan en el perfil del usuario, fuera de la carpeta de la aplicación.

Windows x64; validado en Windows 11. El proyecto mantiene Windows 10 1809 como mínimo declarado. El paquete de desarrollo no tiene firma Authenticode comercial. No se han cambiado políticas de seguridad del equipo.

## Uso

- **Calculadora:** selecciona filamento activo, peso en gramos y tiempo en horas/minutos. Horas enteras no negativas; minutos enteros entre 0 y 59. Recalcula al escribir, sin botón Calcular. Solo esos datos son editables aquí.
- **Resultados:** tarjeta principal solo con precio sugerido. La tarjeta de costes comienza con coste de la pieza destacado (material + electricidad), seguido de material, electricidad y máquina (adicional) en texto secundario. La máquina no forma parte del coste de pieza. El margen bruto queda únicamente en Detalles.
- **Detalles del cálculo:** sección cerrada al iniciar. Se despliega para ver precio por gramo, consumos, precisión económica, tarifa de máquina, multiplicador y margen bruto. Los precios previos/posteriores a ROUNDUP ya no aparecen como detalles de la UI; las fórmulas internas siguen intactas.
- **Filamentos:** biblioteca de perfiles y precios, sin stock físico. Tarjetas compactas en `CollectionView`, dos columnas cuando hay espacio y una en ventanas compactas. Peso y potencia se combinan en una línea. El botón de tres puntos tiene fondo transparente y conserva un área táctil de 48 × 48. El FAB `+ Añadir` / `+ Add` de 56 px de alto queda fijo abajo a la derecha, con sombra suave y tooltip localizado, en una fila reservada fuera de la colección para no cubrir perfiles ni navegación. Las acciones permiten editar, activar/inactivar y eliminar con confirmación.
- **Formulario:** tipo y marca obligatorios y libres, variante opcional, peso, precio pagado y potencia. Nuevos perfiles: 1000 g, 120 W, activos y precio inicialmente vacío. Notas e IsActive antiguos se conservan aunque no ocupan el formulario.
- **Ajustes:** dos bloques: Aplicación y Parámetros de cálculo. Aquí viven idioma, tema, electricidad, calentamiento, máquina y multiplicador global. Los botones −/+ cambian el multiplicador en pasos de 0,5, con mínimo 1; la calculadora lo utiliza inmediatamente.

La ventana inicia en 1100 × 760 y puede redimensionarse. Calculadora y Ajustes caben sin scroll en uso normal a 1100 × 760, 1024 × 700 y 800 × 700 con escala habitual. Los detalles expandidos, errores extensos o ventanas menores pueden necesitar desplazamiento vertical. No se utiliza scroll horizontal. La navegación lateral cambia a inferior por debajo de 760 unidades de la vista. El cuadrado P sigue siendo el icono provisional.

## Idiomas y temas

Español `es-ES` e inglés `en-US`, con cambio en ejecución y formato cultural EUR. Los campos decimales admiten coma o punto, sin separadores de miles; horas/minutos exigen valores enteros. Los estados temporales vacíos, `,` y `.` muestran validación discreta y ocultan resultados hasta tener entradas válidas.

**Sistema / Claro / Oscuro** se guardan en JSON y se aplican en ejecución. Sistema es el valor inicial y delega en el tema del SO mediante MAUI. Paletas claras/oscuras centralizadas en `Resources/Styles/Theme.xaml`, con `AppThemeBinding`. Windows adapta también los controles nativos WinUI y la barra de título; ese código está exclusivamente en `Platforms/Windows`.

## Datos y compatibilidad alpha1/alpha2

Windows conserva exactamente:

```text
%LocalAppData%\PrintCost\settings.json
%LocalAppData%\PrintCost\filaments.json
```

Solo se crean valores iniciales cuando falta el archivo correspondiente; una lista vacía es válida. JSON corrupto se captura y conserva como `.invalid` si es posible antes de recuperar valores iniciales. El guardado escribe un temporal y lo reemplaza al finalizar. Los valores válidos de Ajustes se guardan automáticamente tras 400 ms y al detener/cerrar la aplicación.

Los JSON antiguos sin `Theme` cargan `System`. Sin `Variant`, esta queda vacía salvo inferencia segura de un nombre legacy: `PLA BambuLab Spool` → variante `Spool`; `PLA 850 Sakata` → `850`. Solo se reconoce un tipo y marca exactos presentes al principio/final; nombres ambiguos no se reinterpretan. Una variante guardada explícitamente, incluso vacía o nula, tiene prioridad.

La adaptación se hace en memoria, es idempotente y no reescribe archivos al abrir. Al guardar un perfil o su activación se persisten las variantes inferidas. **Name, Id, notas, precios, pesos, potencias y estados anteriores se conservan.** Name permanece como dato legacy; los nombres personalizados no inferibles siguen en el JSON. No se sustituyen perfiles del usuario por los siete iniciales.

El nombre visible es calculado: `MaterialType · Brand` o `MaterialType Variant · Brand`; si faltan tipo/marca en un perfil legacy se utiliza Name como respaldo. El editor pide completar esos campos para guardar.

Por conservación de datos, un antiguo multiplicador menor que 1 carga intacto, con aviso; el primer ajuste mediante + adopta el mínimo 1. No se cambia automáticamente el valor ni el resto de sus ajustes. Los valores predeterminados de instalación nueva siguen siendo electricidad 0,1349, calentamiento 1200 W/1 min, máquina 0,25/h y multiplicador 3.

## Fórmulas oficiales

Todo utiliza `decimal`. No se redondean material, consumos, electricidad, coste de pieza ni máquina durante los cálculos.

```text
TimeHours = Hours + Minutes / 60m
PricePerGram = PurchasePrice / SpoolWeightGrams
MaterialCost = PieceWeightGrams × PricePerGram
HeatingEnergyKWh = (HeatingPowerWatts / 1000) × (HeatingTimeMinutes / 60)
PrintingEnergyKWh = (PrintPowerWatts / 1000) × TimeHours
ElectricityCost = (HeatingEnergyKWh + PrintingEnergyKWh) × ElectricityPricePerKWh
PieceCost = MaterialCost + ElectricityCost
MachineCost = MachineCostPerHour × TimeHours
RawSalePrice = PieceCost × SaleMultiplier + MachineCost
SuggestedSalePrice = ROUNDUP(RawSalePrice, 2)
RawGrossMargin = SuggestedSalePrice - PieceCost - MachineCost
GrossMargin = ROUNDUP(RawGrossMargin, 2)
```

`ExcelRounding.RoundUp` aplica redondeo dirigido mediante `decimal.Round`: `ToPositiveInfinity` para valores no negativos y `ToNegativeInfinity` para negativos. **No utiliza `MidpointRounding.AwayFromZero`**, que redondea al valor más cercano y solo resuelve los puntos medios alejándose de cero. Aquí, a dos decimales, cualquier fracción adicional positiva sube al siguiente céntimo: `9.34101 → 9.35` y `5.23411 → 5.24`; un importe exacto como `9.34000` permanece `9.34`. No se convierte a `double` ni se multiplica por 100. Admite 0..28 decimales; esta aplicación usa 2. Solo SuggestedSalePrice y GrossMargin reciben este redondeo de negocio; GrossMargin parte de SuggestedSalePrice ya redondeado. [Definición oficial de Microsoft](https://support.microsoft.com/en-us/excel/functions/roundup-function).

Caso ASA eSun: 1000 g, 17,50 €, 180 W; pieza 141 g; 6 h 00 min; ajustes iniciales:

| Concepto | Resultado exacto (€) |
| --- | ---: |
| Material | 2,4675 |
| Electricidad | 0,14839 |
| Coste pieza | 2,61589 |
| Máquina | 1,50 |
| Precio sin ROUNDUP | 9,34767 |
| Precio oficial | **9,35** |
| Margen antes de ROUNDUP | 5,23411 |
| Margen bruto oficial | **5,24** |

## Arquitectura y compilación

```text
PrintCost.sln
src/PrintCost.Core/   Modelos, duración, cálculos, ROUNDUP y persistencia JSON
src/PrintCost.App/    .NET MAUI, XAML, MVVM sencillo, recursos y plataformas
tests/PrintCost.Tests/   Pruebas compartidas .NET 10 y presentación no visual
legacy/              Referencia histórica WPF, fuera de la solución activa
```

C#/.NET 10, Nullable Reference Types, MAUI 10.0.20. Sin frameworks UI de terceros. La presentación pura se prueba desde el proyecto de tests sin necesitar host gráfico MAUI.

En Windows instala el SDK .NET 10 y el workload MAUI de Windows compatible; `global.json` permite versiones feature posteriores de .NET 10. Los targets reales se mantienen en `net10.0-windows10.0.19041.0` y `net10.0-android`. **En alpha3 Android NO se ha compilado, ejecutado, validado visualmente ni publicado deliberadamente.** La UI, cálculos y persistencia siguen compartidos; Android mantiene su ruta privada mediante `FileSystem.AppDataDirectory`. Los únicos cambios de UI nativa Windows están aislados en su plataforma. La validación Android queda para otro hito.

```powershell
dotnet test tests/PrintCost.Tests/PrintCost.Tests.csproj -c Release
dotnet build src/PrintCost.App/PrintCost.App.csproj -c Release -f net10.0-windows10.0.19041.0
```

Para ejecutar en desarrollo, abre el EXE de la salida Windows. Para distribuir la carpeta autocontenida:

```powershell
dotnet publish src/PrintCost.App/PrintCost.App.csproj -c Release -f net10.0-windows10.0.19041.0 -p:RuntimeIdentifierOverride=win-x64 --self-contained true -p:WindowsPackageType=None -p:WindowsAppSDKSelfContained=true -p:PublishTrimmed=false -o artifacts/windows-x64-alpha3
```

Se utiliza `RuntimeIdentifierOverride` para limitar el RID a Windows dentro del proyecto multitarget. No publicar la solución completa ni usar Android como parte de esta iteración. WinUI utiliza versión numérica `0.1.0`/revisión `3`; UI y metadatos informativos muestran la versión alpha completa.

## Pruebas, estado y límites

**84 tests activos, 84 aprobados**, ninguno omitido. Se mantienen los 48 casos existentes y se actualizan únicamente las expectativas afectadas por las reglas alpha3; se añaden 31 casos para tiempo, ROUNDUP, margen, multiplicador, variantes, edición, migración y temas, y otros 5 casos límite en la revisión explícita de ROUNDUP. La cobertura anterior de JSON corrupto, archivos faltantes, errores de IO, cálculos y cultura permanece.

Se ejecutó el paquete Windows y se revisaron los tres tamaños solicitados, idiomas y temas. La última iteración solo cambia XAML, estilos específicos y textos visuales: Core, tests, modelos, persistencia, ViewModels, navegación y Ajustes están intactos. Informe de este pulido en `VALIDATION-UX.md`, con auditoría de fuentes/datos en `VALIDATION-UX-audit.json`; la validación inicial de alpha3 y revisión ROUNDUP se conservan en `VALIDATION.md` y `VALIDATION-audit.json`. La documentación alpha1/alpha2 se conserva como historia.

Alpha pequeña: sin stock, historial, proyectos, pedidos, impuestos, importación 3MF, clientes, nube o actualizaciones. No tiene persistencia de la sesión de impresión, firma comercial ni instalador. Tema Sistema revisado con el SO actual oscuro; no se modificó la configuración del SO para probar sus cambios. La revisión de tamaños se hizo con escala habitual; no cubre todas las escalas de accesibilidad. Los detalles expandidos pueden desplazarse en ventanas compactas. WPF solo se conserva como referencia histórica y no es una segunda app activa.

## Planned / Future ideas

Validar Android en un hito posterior y decidir las próximas mejoras después de probar esta alpha. Alpha4 no forma parte de esta entrega.
