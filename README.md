# MakerTally 3D

**v0.1-alpha4 · 0.1.0-alpha.4 · Alpha**

Lightweight 3D printing cost and pricing calculator for Windows, with Android support planned through .NET MAUI.

Aplicación completamente offline, sin backend, cuentas, SQL ni telemetría. Windows es la plataforma principal: alpha3 fue ejecutada y revisada en Windows; alpha4 compila, pero su validación de ejecución/visual sigue pendiente por Windows Application Control. Android permanece como target real y comparte la UI, sin build ni despliegue Android en esta tarea.

Esta entrega cambia la identidad anterior PrintCost a **MakerTally 3D** y prepara Git/CI. Mantiene alpha4: no es alpha5, no cambia fórmulas ni rediseña la interfaz.

## Estructura y tecnología

```text
MakerTally.sln
src/MakerTally.Core/       Modelos, cálculos decimal, ROUNDUP, JSON y copia de datos legacy
src/MakerTally.App/        .NET MAUI 10.0.20, XAML, MVVM, recursos, Windows y Android
tests/MakerTally.Tests/    Tests compartidos sin host gráfico MAUI
.github/workflows/        CI Windows
docs/history/             Documentación histórica del producto anterior
legacy/                   WPF alpha1 congelado, fuera de solución y CI
```

C#/.NET 10, Nullable Reference Types y controles MAUI estándar. No hay UI web ni librerías visuales adicionales. Namespaces activos `MakerTally.Core`, `MakerTally.App` y `MakerTally.Tests`; assembly de la app `MakerTally`, ejecutable `MakerTally.exe`.

Nombre visible **MakerTally 3D**, marca corta **MakerTally**, descripción **3D Printing Cost Calculator**. ApplicationId Android `com.joseamc91.makertally`, basado en la cuenta personal, sin atribuir un dominio empresarial. El manifiesto Windows utiliza esa misma identidad, `Publisher="CN=MakerTally"` y versión numérica `0.1.0.4`; es una identidad de desarrollo sin empaquetar ni certificado instalado.

## Comportamiento conservado

Ventana inicial aproximada 450×760, mínimo 400×560, una columna centrada con máximo 520 unidades. Navegación inferior fija; Calculadora ordenada como PVP, Nueva impresión, costes y Detalles plegables. Filamentos en una columna con FAB fijo. Ajustes compactos, electricidad manual y steppers de 100 W / 1 minuto / 0,05 €/h / multiplicador 0,5. Temas Sistema/Claro/Oscuro, idiomas Español/English y validación cultural existentes. La memoria de tamaño/posición está aislada en Windows.

El icono provisional existente se conserva: este renombrado no incluye diseño gráfico nuevo. WPF y sus antiguos nombres solo permanecen como referencia histórica; no constituyen una segunda app activa ni se compilan contra el Core actual.

## Datos y migración automática

El código puede vivir en `D:\MakerTally 3D`; los datos personales **no** se guardan allí:

```text
%LocalAppData%\MakerTally\settings.json
%LocalAppData%\MakerTally\filaments.json
%LocalAppData%\MakerTally\window.json
```

En el primer arranque Windows, antes de cargar JSON y restaurar ventana, se buscan los tres archivos en `%LocalAppData%\PrintCost`. Cada archivo antiguo se copia únicamente si falta su equivalente en MakerTally, en orden settings → filaments → window. La existencia de la carpeta nueva por sí sola no impide completar archivos ausentes.

La copia mantiene exactamente los bytes originales, usa un temporal único y lo renombra sin reemplazar archivos ya existentes, incluso si otra instancia los crea simultáneamente. Repetirla es seguro. La carpeta antigua permanece como respaldo y no se borran, mezclan ni vuelven a sembrar perfiles. No se cambia el contrato JSON ni se redondean sus valores.

Si una copia falla por IO/permisos, la inicialización se detiene antes de crear valores predeterminados que ocultarían datos antiguos. El origen y cualquier archivo completo previamente copiado se conservan. La recuperación de JSON corrupto y adaptación de variantes sigue siendo la de alpha4.

Android mantiene almacenamiento privado `FileSystem.AppDataDirectory`. El nuevo package id representa una identidad nueva; no se implementa migración entre sandboxes Android de la antigua app.

## Fórmulas sin cambios

```text
TimeHours = Hours + Minutes / 60m
PricePerGram = PurchasePrice / SpoolWeightGrams
MaterialCost = PieceWeightGrams × PricePerGram
HeatingEnergyKWh = (HeatingPowerWatts / 1000m) × (HeatingTimeMinutes / 60m)
PrintingEnergyKWh = (PrintPowerWatts / 1000m) × TimeHours
ElectricityCost = (HeatingEnergyKWh + PrintingEnergyKWh) × ElectricityPricePerKWh
PieceCost = MaterialCost + ElectricityCost
MachineCost = MachineCostPerHour × TimeHours
RawSalePrice = PieceCost × SaleMultiplier + MachineCost
SuggestedSalePrice = ROUNDUP(RawSalePrice, 2)
RawGrossMargin = SuggestedSalePrice - PieceCost - MachineCost
GrossMargin = ROUNDUP(RawGrossMargin, 2)
```

Se utiliza `decimal`, sin redondeos intermedios. `ExcelRounding` aplica redondeo dirigido, equivalente a Excel ROUNDUP; permanece intacto. Solo se renombra la clase de marca a `MakerTallyCalculationService`; su método conserva el código matemático anterior. Caso ASA eSun de referencia: material 2,4675 €, electricidad 0,14839 €, pieza 2,61589 €, máquina 1,50 €, precio 9,35 € y margen bruto 5,24 €.

## Compilar en Windows

SDK .NET 10 y workload `maui-windows`. `global.json` conserva la configuración original: 10.0.100 mínimo, `latestFeature`, sin prereleases; permite las feature compatibles de .NET 10. El SDK seleccionado localmente para esta validación fue 10.0.401.

```powershell
dotnet workload install maui-windows
dotnet restore tests/MakerTally.Tests/MakerTally.Tests.csproj
dotnet test tests/MakerTally.Tests/MakerTally.Tests.csproj -c Release
dotnet restore src/MakerTally.App/MakerTally.App.csproj -p:Configuration=Release -p:WindowsOnly=true -r win-x64 -p:RuntimeIdentifierOverride=win-x64 -p:SelfContained=true -p:WindowsAppSDKSelfContained=true -p:WindowsPackageType=None -p:PublishTrimmed=false
dotnet build src/MakerTally.App/MakerTally.App.csproj -c Release -f net10.0-windows10.0.19041.0 -p:WindowsOnly=true -r win-x64 -p:RuntimeIdentifierOverride=win-x64 --self-contained true -p:WindowsAppSDKSelfContained=true -p:WindowsPackageType=None -p:PublishTrimmed=false --no-restore
dotnet publish src/MakerTally.App/MakerTally.App.csproj -c Release -f net10.0-windows10.0.19041.0 -p:WindowsOnly=true -r win-x64 -p:RuntimeIdentifierOverride=win-x64 --self-contained true -p:WindowsPackageType=None -p:WindowsAppSDKSelfContained=true -p:PublishTrimmed=false --no-restore -o artifacts/windows-x64
```

`WindowsOnly=true` selecciona solo el target Windows **dentro del proyecto App**, sin propagar un TargetFrameworks Windows a Core. Sin este selector se mantienen Windows y Android reales. No ejecutar restore de la solución multitarget completa para una tarea Windows.

La variante por carpeta, publicada oficialmente sin empaquetar, incluye .NET y Windows App SDK: conservar todos sus archivos y abrir `MakerTally.exe`. Esta variante no es un ejecutable único, no crea instalador y no tiene firma Authenticode comercial. [Publicación oficial de MAUI Windows](https://learn.microsoft.com/en-us/dotnet/maui/windows/deployment/publish-unpackaged-cli?view=net-maui-10.0).

## Tests y estado real

Se conservan los **98 casos** anteriores y se añaden **9 casos de migración**, total previsto **107**. Se prueban archivos individuales, bytes intactos, origen ausente, destino existente, no sobrescritura, repetición, exclusión de otros archivos, fallo seguro y carga de JSON antiguo con filamento inactivo y ventana.

La build Windows Release desde la ubicación nueva termina con **0 errores y 0 avisos**. Los tests compilan; su único intento local de ejecución fue bloqueado por Windows Application Control al cargar `MakerTally.Tests.dll`, código `0x800711C7`, antes de descubrir los casos. No se declaran 107 tests aprobados; no se realizan reintentos, excepciones de seguridad ni cambios de políticas. Las 84 pruebas aprobadas corresponden a la referencia anterior a alpha4.

La revisión visual alpha4 y la migración real al arrancar en este PC siguen pendientes de poder ejecutar la app. No se compila Android ni se modifica su infraestructura. La auditoría de fuentes comprueba que UI/cálculos/ROUNDUP y tests matemáticos solo difieren en identificadores de marca necesarios; los datos del perfil del usuario no se han tocado. El primer CI detectó además un fallo preexistente de alpha4: el stepper de máquina seguía habilitado en decimal.MaxValue porque restar 0,05 a ese extremo puede redondear al mismo valor. Se añade únicamente una comprobación explícita del límite; se conserva el test original, sin modificar fórmulas, pasos ni uso normal.

## GitHub Actions Windows

`.github/workflows/windows-ci.yml` permite `workflow_dispatch`, push a `main` y pull requests a `main`, con `windows-latest`. Instala .NET según `global.json` y exclusivamente `maui-windows`. Restaura tests y App Windows, exige que todos los casos se ejecuten y pasen, compila/publica Release x64, comprime la carpeta completa y sube el ZIP con SHA-256. No hay Android, Release, tag ni firma automática.

Artifacts temporales para comparación A/B: `MakerTally3D-v0.1-alpha4-win-x64-folder-ci-<run_number>` y `MakerTally3D-v0.1-alpha4-win-x64-single-ci-<run_number>`. También se conservan los resultados TRX. Los artefactos duran 14 días. Solo se produce el artifact Windows si los tests, build y publish pasan. [Configuración oficial de setup-dotnet](https://github.com/actions/setup-dotnet), [upload-artifact](https://github.com/actions/upload-artifact).

Repositorio privado: [joseamc91/MakerTally3D](https://github.com/joseamc91/MakerTally3D), creado con autorización explícita. Las ejecuciones y sus resultados reales se consultan en [Actions](https://github.com/joseamc91/MakerTally3D/actions/workflows/windows-ci.yml). Tras un CI correcto, el usuario descargará y ejecutará el ZIP manualmente; producirlo en GitHub no garantiza que Application Control lo acepte. No se desactiva seguridad ni se descarga/ejecuta automáticamente el artifact en el PC.

### Variante single-file de Windows (prueba de distribución Alpha)

Windows CI conserva la publicación por carpeta y produce además un EXE único mediante el soporte oficial de .NET y Windows App SDK **1.7.250909003**, sin actualizar dependencias. El artifact single-file contiene únicamente `MakerTally.exe` y `MakerTally.exe.sha256`; el hash corresponde al EXE, no al ZIP de descarga de GitHub.

La opción `WindowsSingleFile=true`, limitada al target Windows, activa `PublishSingleFile`, `IncludeAllContentForSelfExtract`, `EnableMsixTooling` y `SelfContained`; mantiene `WindowsPackageType=None`, `WindowsAppSDKSelfContained=true` y `PublishTrimmed=false`. Los símbolos de App/Core se embeben mediante `DebugType=embedded` al publicar. Los intermediarios de ambas variantes están separados; el CI falla si quedan archivos externos, sin borrarlos para fingir un EXE único. Android no recibe estas opciones y no se compila en este workflow.

Es una distribución **unpackaged y self-contained**, sin necesidad de instalar .NET. Puede extraer internamente las dependencias al arrancar; ese comportamiento oficial se conserva. No está firmada comercialmente y continúa siendo v0.1-alpha4, no una Release estable. La aceptación por Windows Application Control y la ejecución en este PC deben probarse manualmente; el CI no descarga ni ejecuta el artifact aquí. [Single-file oficial de Windows App SDK](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/unpackage-winui-app#single-file-exe), [símbolos embebidos de .NET](https://learn.microsoft.com/en-us/dotnet/core/deploying/single-file/overview#include-pdb-files-inside-the-bundle).

## Privacidad y archivos históricos

`.gitignore` excluye bin/obj, Visual Studio, resultados, builds, logs, ZIP/APK y materiales de firma/credenciales. Excluye expresamente settings.json, filaments.json y window.json en cualquier carpeta. Los siete perfiles de `DefaultData` y los JSON sintéticos de tests son ejemplos deliberados, no datos personales.

`docs/history` y `legacy` mantienen nombres del producto anterior de manera intencionada. Los informes históricos se limpian de rutas absolutas de usuario y fingerprints de sus datos; no sirven como instrucciones de la app actual. El árbol original anterior se conserva fuera de este repositorio como respaldo.

Más detalle en [docs/RENAMING.md](docs/RENAMING.md). No se inicia alpha5 ni se añaden nuevas funciones de negocio.
