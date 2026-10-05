# PrintCost

Versión actual: **v0.1-alpha2** · Metadatos: **0.1.0-alpha.2** · Estado: **Alpha**.

Calcula el coste aproximado de una impresión 3D y su precio de venta sugerido. Funciona offline, sin servidor, SQL, cuentas, telemetría ni actualizaciones automáticas.

Alpha1 utilizaba WPF. Alpha2 migra la interfaz a **.NET MAUI nativo**, compartiendo XAML, estilos y presentación entre **Windows y Android**. Conserva las fórmulas, los modelos, el formato JSON y los perfiles existentes.

## Ejecutar los paquetes

**Windows x64:** descomprimir `PrintCost-v0.1-alpha2-win-x64.zip` y abrir `PrintCost.exe`. Conservar toda la carpeta: incluye .NET y Windows App SDK. No requiere instalador ni .NET preinstalado. Es una distribución oficial MAUI/WinUI sin empaquetar, no un único ejecutable portable como WPF. Los datos pertenecen al usuario de Windows y se guardan fuera de esa carpeta. Windows 10 1809 o posterior / Windows 11, x64.

El paquete no tiene firma Authenticode comercial. Las políticas de Control de aplicaciones de cada equipo pueden afectar a su ejecución; no se han cambiado políticas del sistema para validarlo.

**Android:** `PrintCost-v0.1-alpha2-android-debug.apk` es un APK de prueba firmado con la clave de depuración del SDK, con los ensamblados incluidos. No depende del despliegue rápido de un IDE. Android mínimo 5.0 / API 21; target API 36. El APK incluye ARM64 y x64. No hay firma de producción, AAB ni configuración de Play Store. Se ha compilado y verificado su estructura, pero no se ha ejecutado en un dispositivo o emulador durante esta entrega.

## Uso y diseño

- Calculadora: filamento activo, peso en gramos, horas decimales y multiplicador de simulación. Recalcula al escribir y destaca el precio sugerido. El coste completo, beneficio y margen se distinguen de los costes parciales y consumos técnicos.
- Inventario: tarjetas en `CollectionView`, dos columnas cuando hay espacio y una en tamaños menores. Añadir, editar, activar/inactivar y eliminar con confirmación. La cabecera permanece visible y solo se desplaza la colección.
- Ajustes: idioma, electricidad, calentamiento, coste de máquina y multiplicador predeterminado. Guardado automático de valores válidos tras 400 ms y al detener/cerrar la aplicación.

La ventana inicia en 1100 × 760. Calculadora y Ajustes caben sin desplazamiento vertical a 1024 × 700 con datos válidos y escala habitual. A 800 × 700 la calculadora pasa a dos columnas y permite desplazamiento vertical; Ajustes sigue cabiendo. No hay desplazamiento horizontal. En anchuras inferiores a 760 unidades de la vista, la navegación lateral se convierte en navegación inferior; las tarjetas pasan a una columna cuando hace falta. Los formularios y avisos de validación pueden necesitar desplazamiento para conservar su accesibilidad.

Tema claro, tarjetas suaves, azul discreto, recursos centralizados y controles con altura mínima de 48 unidades. No hay tema oscuro.

Español (`es-ES`) e inglés (`en-US`) se aplican sin reiniciar. Los textos se centralizan en dos recursos JSON incrustados; no se necesita descargar traducciones. Ambos idiomas usan EUR: `17,50 €` / `€17.50`. Los campos aceptan coma o punto decimal, sin miles ni notación científica. Vacío, `,`, `.` y otros estados incompletos muestran validación discreta y ocultan los resultados mientras no se pueda calcular. No aparecen diálogos durante la escritura.

El ejemplo inicial de la calculadora utiliza ASA eSun, 141 g y 6 h cuando ese perfil está activo. Las simulaciones no cambian el multiplicador predeterminado; el predeterminado se aplica al abrir la aplicación.

## Estructura y tecnología

```text
PrintCost.sln
src/
  PrintCost.Core/          Modelos, decimal, cálculos, parser y persistencia JSON
  PrintCost.App/           .NET MAUI: Windows + Android
    Controls/             Controles pequeños y distribución adaptable
    Views/                Calculadora, Inventario, Ajustes y editor modal
    Presentation/         ViewModels, comandos, traducción y contratos sin MAUI
    Services/             Adaptación de diálogos nativos
    Resources/            Idiomas, estilos, fuentes, icono y splash
    Platforms/            Rutas y detalles nativos mínimos
tests/PrintCost.Tests/   xUnit, lógica, presentación y compatibilidad
legacy/                 Referencia WPF de alpha1 fuera de la solución activa
```

C#, .NET 10, Nullable Reference Types, MVVM sencillo y `System.Text.Json`. La única dependencia directa de la aplicación es `Microsoft.Maui.Controls` 10.0.20 y sus dependencias oficiales. No se usan Blazor, WebView, HTML, CSS, JavaScript, DataGrid ni toolkit UI de terceros. Los diálogos de inventario usan las APIs nativas de MAUI. `async` se limita a esas interacciones; las pequeñas lecturas/escrituras JSON siguen siendo síncronas.

Las pruebas compilan el mismo código de `Presentation` sin un host MAUI. Esto permite comprobar comportamiento y almacenamiento sin mantener otro proyecto de lógica UI. Los controles visuales se validan ejecutando la aplicación.

## Compilar

Requisitos: SDK **.NET 10**, workloads `maui-windows` y `maui-android`. `global.json` selecciona un SDK estable de la familia 10. La restauración y la instalación inicial de herramientas requieren conexión; el uso de PrintCost no.

```powershell
dotnet workload install maui-windows maui-android
dotnet restore PrintCost.sln
dotnet build src/PrintCost.App/PrintCost.App.csproj -c Release -f net10.0-windows10.0.19041.0
dotnet testtests/PrintCost.Tests/PrintCost.Tests.csproj -c Release
```

La compilación Windows genera `src/PrintCost.App/bin/Release/net10.0-windows10.0.19041.0/win-x64/PrintCost.exe`. Esta compilación normal utiliza el runtime .NET instalado; el paquete publicado incluye su propio runtime.

Para publicar Windows, compilar **el proyecto App**, no toda la solución:

```powershell
dotnet publish src/PrintCost.App/PrintCost.App.csproj -c Release -f net10.0-windows10.0.19041.0 -p:RuntimeIdentifierOverride=win-x64 --self-contained true -p:WindowsPackageType=None -p:WindowsAppSDKSelfContained=true -p:PublishTrimmed=false -o artifacts/windows-x64
```

Se sigue el [procedimiento oficial de distribución sin empaquetar de MAUI](https://learn.microsoft.com/en-us/dotnet/maui/windows/deployment/publish-unpackaged-cli?view=net-maui-10.0). `RuntimeIdentifierOverride` se aplica solo a Windows; evita pasar un RID Windows al target Android durante la restauración del proyecto compartido.

Para Android hacen falta además Android SDK, platform-tools, build-tools 36.0.0, plataforma API 36 y un JDK compatible. La validación se realizó con Microsoft OpenJDK 17.0.14. No se incluyeron SDK/JDK en el código ni en los paquetes de entrega.

```powershell
# Sustituir por las ubicaciones de las herramientas de tu equipo.
$androidSdk = 'C:/Android/Sdk'
$javaSdk = 'C:/Java/jdk-17'
dotnet build PrintCost.sln -c Debug -p:AndroidSdkDirectory=$androidSdk -p:JavaSdkDirectory=$javaSdk
```

Ese comando compila la solución completa para Windows y Android. El APK firmado de depuración queda en `src/PrintCost.App/bin/Debug/net10.0-android/com.printcost.app-Signed.apk`. También se puede compilar solo Android con `dotnet build src/PrintCost.App/PrintCost.App.csproj -c Debug -f net10.0-android` y esos mismos parámetros SDK/JDK. El SDK puede añadir el permiso INTERNET al manifiesto de depuración para sus herramientas; PrintCost no realiza conexiones ni necesita red. La copia de seguridad automática de Android está deshabilitada en el manifiesto de la aplicación.

El paquete utiliza metadatos informativos `0.1.0-alpha.2` y texto UI `v0.1-alpha2`. Windows exige una versión numérica para sus metadatos de paquete (`0.1.0`, revisión 2); Android admite `0.1.0-alpha.2` como versionName.

## Datos y compatibilidad alpha1 → alpha2

**Windows:** se reutiliza exactamente `%LocalAppData%/PrintCost/`, con `settings.json` y `filaments.json`. No se copian a otra ruta, no se renombraron claves, no se borran originales y no se recrean perfiles encima de archivos válidos. La migración es una apertura compatible y repetible de los mismos datos. Mantiene identificadores, ajustes, idioma, precios, notas y perfiles inactivos. El directorio lo proporciona la adaptación Windows; la presentación no conoce rutas Windows.

**Android:** ambos JSON se guardan en `FileSystem.AppDataDirectory`, el almacenamiento privado de la aplicación. No existe transferencia automática de datos entre Windows y Android. Desinstalar la aplicación Android elimina su almacenamiento privado.

Si falta un archivo, se crea solo el que falte con valores iniciales. Sin datos previos se crean los siete perfiles originales. Un inventario vacío válido permanece vacío. El JSON es independiente de la cultura de visualización. Las operaciones de inventario se guardan antes de actualizar la colección; los fallos de escritura conservan el inventario anterior en memoria.

Si un JSON está corrupto o contiene valores inválidos, se utiliza un valor seguro y se muestra un aviso traducido. Cuando es posible, el original se conserva con sufijo `.invalid` antes de reemplazarlo. Se escribe primero un temporal y después se reemplaza el destino. Se mantiene el servicio de almacenamiento de alpha1 detrás de `IAppDataStorage`; no se añadió otro mecanismo de persistencia.

## Fórmulas

Todos los cálculos usan `decimal`, sin redondear valores intermedios. Importes principales: 2 decimales. Valores técnicos: hasta 4. Margen: 2.

```text
PrecioPorGramo = PrecioBobina / PesoNetoBobinaGramos
PrecioKg = PrecioPorGramo × 1000
CosteMaterial = PesoPiezaGramos × PrecioPorGramo
ConsumoCalentamientoKWh = (PotenciaCalentamientoW / 1000) × (TiempoCalentamientoMin / 60)
ConsumoImpresionKWh = (PotenciaImpresionW / 1000) × TiempoImpresionHoras
ConsumoTotalKWh = ConsumoCalentamientoKWh + ConsumoImpresionKWh
CosteElectricidad = ConsumoTotalKWh × PrecioElectricidadKWh
CosteImpresion = CosteMaterial + CosteElectricidad
CosteMaquina = TiempoImpresionHoras × CosteMaquinaHora
CosteCompleto = CosteImpresion + CosteMaquina
PrecioVenta = (CosteImpresion × MultiplicadorVenta) + CosteMaquina
BeneficioBruto = PrecioVenta − CosteCompleto
MargenPorcentaje = PrecioVenta > 0 ? (BeneficioBruto / PrecioVenta) × 100 : 0
```

El calentamiento se suma una vez incluso con cero horas, según la fórmula original. No se calculan impuestos ni beneficio neto.

ASA eSun: material **2,4675 €**, electricidad **0,14839 €**, impresión **2,61589 €**, máquina **1,50 €**, completo **4,11589 €**, venta **9,34767 €**, beneficio **5,23178 €**, margen **55,9688136188… %**. En pantalla: **9,35 €**, **4,12 €**, **5,23 €**, **55,97 %**.

## Pruebas y recuperación

La sesión interrumpida había creado una migración parcial; no había una entrega alpha2 validada. Se aprovecharon sus vistas, estilos, presentación y pruebas. Los detalles están en `RECOVERY.md` y `VALIDATION.md`.

**48 pruebas activas aprobadas:** las 38 pruebas originales de lógica, parser y JSON sin cambios, más 10 pruebas de presentación, traducción y compatibilidad. La prueba WPF original también pasó antes de archivarse; sus escenarios se trasladaron a la presentación compartida. No se eliminó una prueba que fallaba para obtener una compilación correcta.

Se cubren fórmulas y caso ASA, cero y divisiones inválidas, coma/punto e incompletos, JSON corrupto/faltante, valores iniciales, lecturas repetidas de JSON alpha1 sin sobrescritura, rutas por plataforma, guardado de idioma, inventario inactivo, edición, confirmación/cancelación de eliminación, fallos de escritura y notificación de cambio de idioma.

## Límites de esta alpha

- Windows fue ejecutado y revisado visualmente; Android se compiló, pero falta revisión en hardware/emulador y con teclado táctil.
- Distribución Windows con carpeta de dependencias: mayor tamaño que WPF. Sin instalador ni firma comercial.
- EUR fijo; tiempo en horas decimales; perfiles de filamento, sin stock ni historiales.
- No persiste la simulación ni su filamento seleccionado: al abrir se usa el ejemplo y el multiplicador predeterminado guardado.
- El diseño se ha revisado a escala habitual. Escalas grandes, textos extremos y avisos pueden requerir desplazamiento vertical.

## Planned / Future ideas

Después de probar esta alpha: mejorar la entrada de tiempo, valorar lectura de 3MF y estudiar bobinas individuales. Estas funciones no están implementadas.
