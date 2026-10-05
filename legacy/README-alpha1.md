# PrintCost

Versión actual: **v0.1-alpha1**. Metadatos: **0.1.0-alpha.1**. Estado: **Alpha**.

Aplicación de escritorio para Windows que estima el coste de una impresión 3D y su precio de venta. Funciona completamente offline: sin servidor, cuentas, base de datos, telemetría ni actualizaciones automáticas.

## Usar la aplicación

El paquete portable `PrintCost-v0.1-alpha1-win-x64.zip` contiene la aplicación y el runtime: descomprimir y abrir `PrintCost.exe`. No requiere instalación ni .NET preinstalado. Está preparado para Windows x64; los datos se guardan por usuario en LocalAppData, no junto al ejecutable.

La calculadora se abre con ASA eSun, 141 g y 6 h para facilitar la comprobación del ejemplo. Los resultados se actualizan al escribir. El multiplicador de la calculadora sirve para simulaciones; no modifica el predeterminado. El predeterminado de Ajustes se aplica al abrir la aplicación.

Inventario permite añadir, editar y eliminar perfiles con confirmación. Los inactivos permanecen guardados y no aparecen en la calculadora. Precio/kg se calcula usando el peso real de la bobina. No se descuenta material ni se mantienen historiales.

En Ajustes se puede cambiar entre español e inglés sin reiniciar. La moneda de esta alpha es EUR en ambos idiomas. Los campos aceptan coma y punto decimal, sin separadores de miles ni notación científica. Durante una entrada inválida se muestra un borde discreto y se ocultan los resultados; el último valor válido se conserva internamente. Los números que exceden la capacidad de `decimal` tampoco provocan el cierre de la aplicación.

La ventana comienza en 1100 × 760 y admite un mínimo de 820 × 560. Las vistas permiten desplazamiento cuando no cabe todo el contenido; Inventario tiene desplazamiento horizontal para sus columnas.

## Tecnologías y estructura

- C#, .NET 10, WPF estándar, MVVM sencillo y Nullable Reference Types.
- `src/PrintCost.Core`: modelos, fórmulas, parser decimal y persistencia JSON.
- `src/PrintCost.Desktop`: vistas WPF, ViewModels, comandos y recursos `es-ES` / `en-US`.
- `tests/PrintCost.Tests`: xUnit; pruebas de cálculos, entradas, JSON y una prueba WPF integrada en un hilo STA.

La aplicación no depende de paquetes NuGet de terceros. Solo las pruebas utilizan Microsoft.NET.Test.Sdk, xUnit y su adaptador. No se usa contenedor de dependencias ni framework MVVM.

## Compilar y ejecutar desde el código

Requisitos: Windows y SDK de .NET 10. La primera restauración de paquetes de pruebas requiere acceso a NuGet; el uso normal de la aplicación es offline.

Desde esta carpeta:

```powershell
dotnet restore PrintCost.sln
dotnet build PrintCost.sln -c Release
dotnet test PrintCost.sln -c Release
dotnet run --project src/PrintCost.Desktop -c Release
```

Compilación normal: `src/PrintCost.Desktop/bin/Release/net10.0-windows/PrintCost.exe`. Esta variante sí requiere el runtime **.NET Desktop 10**.

Crear una distribución portable con runtime incluido:

```powershell
dotnet publish src/PrintCost.Desktop/PrintCost.Desktop.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true -p:DebugType=None -o artifacts/portable
```

WPF no se recorta con trimming. El ejecutable incluye el runtime y extrae bibliotecas nativas en una carpeta temporal al ejecutarse. Esto aumenta el tamaño del paquete respecto a la compilación que usa un runtime preinstalado.

## Datos

Ubicación: **`%LocalAppData%\PrintCost\`**.

- `settings.json`: idioma, electricidad, calentamiento, coste máquina/hora y multiplicador predeterminado.
- `filaments.json`: perfiles, identificadores, estado activo y notas.

Se crean automáticamente los archivos que falten. Una colección vacía se mantiene vacía; no se repuebla el inventario si el usuario elimina todos los perfiles. Los ajustes válidos se guardan tras una pausa de 400 ms y al cerrar. Las operaciones de Inventario se guardan inmediatamente, antes de confirmar el cambio en memoria. Se escribe primero un archivo temporal y se reemplaza el destino para reducir el riesgo de archivos incompletos.

Si el JSON está dañado o contiene datos inválidos, se usan valores iniciales y se muestra un aviso traducido. Cuando sea posible, el original se conserva con sufijo `.invalid`. Los errores de lectura/escritura se muestran sin cerrar la aplicación. Los archivos contienen números JSON independientes del idioma elegido.

## Fórmulas

Todos los cálculos usan `decimal`, sin redondeo intermedio. Los importes principales se presentan con dos decimales; los técnicos con hasta cuatro y el margen con dos.

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

El calentamiento se suma una vez, incluso si se introducen cero horas, según la fórmula solicitada. El beneficio es bruto: esta versión no aplica impuestos, IVA ni costes adicionales.

Ejemplo ASA eSun: material 2,4675 €, electricidad 0,14839 €, impresión 2,61589 €, máquina 1,50 €, completo 4,11589 €, venta 9,34767 €, beneficio 5,23178 € y margen 55,9688136188… %. La UI muestra **9,35 €** de venta y **55,97 %** de margen.

## Decisiones y límites de la alpha

- Recursos WPF `DynamicResource` y un servicio de cultura para traducción inmediata, incluidos errores, unidades y diálogos propios. Los datos escritos por el usuario (nombre, material, marca, notas) no se traducen.
- Persistencia síncrona de archivos pequeños; temporizador solo para evitar escribir en cada pulsación de Ajustes. No se añade `async` artificialmente.
- La lógica económica está en `PrintCostCalculationService`; las vistas no contienen fórmulas.
- EUR fijo, horas decimales, una configuración global de máquina y modo claro.
- Pensada para una instancia abierta por usuario. No hay coordinación entre varias instancias que editen los mismos archivos.
- El estado de una simulación (peso, horas, selección y multiplicador) no se guarda entre ejecuciones.
- Sin instalador ni firma digital. La distribución publicada corresponde a Windows x64.

## Planned / Future ideas

Posibles pasos posteriores: lectura de peso/tiempo desde 3MF, varias impresoras y bobinas con stock. Son ideas para evaluar después de probar esta alpha; no están implementadas.
