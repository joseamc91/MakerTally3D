# Validación de PrintCost v0.1-alpha4

Fecha: 2026-10-05. Estado: implementación compilada, **no cerrada**; ejecución bloqueada por política externa de Windows.

## Estado inicial inspeccionado

Se encontró alpha3 MAUI con Core/App/Tests, 84 tests y el pulido UX anterior. No había repositorio Git. Se preservó un ZIP completo del código previo (`work/source-before-alpha4.zip`) y copia de los datos locales en `work/user-data-before-alpha4`. Se ejecutó la suite inicial: 84 correctas, 0 fallos y 0 omitidas (`work/alpha4-test-results/alpha4-before.trx`). La referencia alpha3 Windows se abrió y se revisó antes de cambiar archivos. Los artefactos y documentos de las alphas anteriores se conservaron.

## Cambios realizados

MainPage: navegación inferior permanente, contenido centrado y máximo 520, sin rail. Calculadora: PVP primero, entradas debajo, costes y detalles transparentes después. Filamentos: una columna y FAB fijo en fila propia. Ajustes: lista de secciones, Pickers compactos, electricidad manual y steppers de 48 unidades con pasos 100 W / 1 min / 0,05 €/h; multiplicador existente 0,5/mínimo 1. Se mantienen recursos de ambos idiomas y tres temas. Versión actualizada en props, proyecto, manifiesto y recursos.

PlatformWindow Windows: 450×760 inicial, mínimo 400×560; guardado separado en window.json; geometría física relativa a área de trabajo, mínimos dependientes de escala, elección de monitor por intersección, corrección/centrado seguro y conservación de última geometría normal. Las APIs WinUI no aparecen en código compartido. [Coordenadas de WorkArea según Microsoft](https://learn.microsoft.com/en-us/windows/windows-app-sdk/api/winrt/microsoft.ui.windowing.displayarea.workarea).

Nueva lógica pura de steppers en MainViewModel.CompactSettings.cs y geometría/storage en Platforms/Windows/WindowPlacement.cs. Se enlaza este último archivo en tests sin cargar MAUI ni WinUI. No se reescribe Core.

## Comprobaciones realizadas

| Comprobación | Resultado |
| --- | --- |
| Suite alpha3 antes de cambios | 84/84 aprobadas |
| Build Windows Release | Correcta, 0 errores, 0 avisos |
| Publicación autocontenida Windows x64 | Correcta |
| Fórmulas/Core/ROUNDUP/persistencia/modelos | Fuentes idénticas al respaldo |
| Tests matemáticos y de persistencia existentes | Fuentes idénticas al respaldo |
| Idiomas y textos XAML | Claves coincidentes, sin literales visibles nuevos |
| Datos reales settings.json / filaments.json | Idénticos al respaldo |
| Compilación proyecto de tests después | Correcta |
| Ejecución suite después | Bloqueada antes de descubrimiento; no hay resultados aprobados de alpha4 |
| Inicio Windows alpha4 | Bloqueado antes de entrar en el programa |
| Revisión 450×760, 400×700, 500×800, 800×800 | Pendiente |
| UI temas Sistema/Claro/Oscuro e idiomas ES/EN | Pendiente en alpha4 |
| Movimiento/redimensión/cierre/restauración de ventana | Pendiente en ejecución real |
| Ventana fuera de monitor / datos corruptos | Casos añadidos, ejecución pendiente |
| Android en esta iteración | Inspección únicamente; 0 builds, 0 despliegues |

## Bloqueo externo exacto

VSTest/xUnit informa `System.IO.FileLoadException`, código `0x800711C7`: «Una directiva de Control de aplicaciones bloqueó este archivo» al cargar `PrintCost.Tests.dll`. Ocurrió en Release, en reintento sin recompilar y en Debug. No es un fallo de aserciones: el adaptador no llegó a descubrir las pruebas.

El inicio de la publicación Windows no expuso una ventana. El registro Application/.NET Runtime confirma el mismo `FileLoadException 0x800711C7` para `artifacts/windows-x64-alpha4/PrintCost.dll`. El programa no llegó a crear UI ni leer/escribir datos.

No se alteraron políticas de seguridad, no se añadieron exclusiones/certificados de confianza ni se intentó eludir el bloqueo. No se dispone de certificado de firma de código en el almacén personal inspeccionado. Para completar la tarea hace falta poder ejecutar los ensamblados en un entorno de desarrollo autorizado o mediante una firma de código válida y confiable.

## Pruebas nuevas preparadas

CompactUtilityTests añade 14 casos reales: pasos exactos y recálculo/persistencia, límites en cero, precisión heredada, estados inválidos y overflow; detalle/chevron; geometría predeterminada, monitor con coordenadas negativas, monitor desconectado/barra inaccesible, intersección parcial, tamaños inválidos/extremos, área menor al mínimo y almacenamiento separado/corrupto/no disponible. Se mantienen los 84 casos previos, con una sola actualización no matemática de versión en PresentationTests. La suite prevista es de 98; **no se declara aprobada**.

Caso ASA eSun esperado y sin cambios de fórmula: material 2,4675; electricidad 0,14839; pieza 2,61589; máquina 1,50; precio 9,35; margen bruto 5,24. El caso estuvo aprobado en la suite inicial; su reejecución alpha4 queda pendiente.

## Preparación Android (solo lectura al final)

Workload maui-android 10.0.20/10.0.100 instalado, pack 36.1.69. SDK local work/android-sdk con API36/build-tools36.0.0/platform-tools36.0.0/CLI latest. JDK local Microsoft17.0.14, y JDK25.0.4.101 en JAVA_HOME/PATH. El log histórico de alpha2 registra build Android correcta con SDK local/JDK17. adb1.0.41 (36.0.0) funciona y devuelve lista vacía; se inició su daemon normal para consultar dispositivos. No Emulator, system-images ni AVD detectados en las ubicaciones inspeccionadas; no dispositivos conectados. No se construye alpha4 Android ni se genera APK. No se descargó ni instaló nada. La build actual de Android no está confirmada; los prerrequisitos de compilación previamente usados están presentes.

## Entrega preservada y trabajo pendiente

ZIP Windows x64 **unvalidated**, ZIP fuente y hashes SHA-256. La integridad del ZIP y correspondencia del DLL publicado se comprueban al empaquetar. No se incluyen datos personales ni settings/filaments del usuario.

No se puede cerrar alpha4 todavía. Pendientes: ejecutar los 98 casos en un entorno autorizado; iniciar Windows y completar la matriz visual; probar cierre/restauración real y fallback fuera de pantalla. No se inicia alpha5. Los artefactos alpha3 funcionales siguen disponibles.
