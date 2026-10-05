# Validación — PrintCost v0.1-alpha3

Este documento conserva la validación inicial y la revisión de ROUNDUP de alpha3. Para el pulido visual posterior, sin cambio de versión ni lógica, consulta `VALIDATION-UX.md`.

Fecha: 2026-10-04. Windows x64, SDK .NET 10.0.401, MAUI 10.0.20.

## Estado inspeccionado antes de modificar

- Versión 0.1.0-alpha.2, solución MAUI Core/App/Tests; WPF en legacy fuera de la solución.
- 48/48 pruebas de partida aprobadas. Build Windows Release: cero errores y warnings.
- No existe repositorio Git en este workspace, por lo que no se atribuyen modificaciones a commits ni autores anteriores. La fuente recuperada fue preservada antes de los cambios.
- Los datos existentes tenían multiplicador 1. No se reemplazaron por los predeterminados. Se tomó copia exacta antes de realizar pruebas de UI.

## Alpha3: comprobaciones de lógica

84/84 tests activos pasan, ninguno fallido ni omitido. Se conservan los 48 casos de alpha2 y se actualizan las expectativas de venta/beneficio afectadas por Excel. 31 casos añadidos inicialmente: duración 6:30 y 5:45; cero; rangos y fracciones inválidas; estados transitorios; ROUNDUP positivo/negativo/exacto/cero; decimal extremo; multiplicador 2,5 y pasos 0,5; datos antiguos; Variant vacío/nulo/con valor; DisplayName; migración idempotente; temas; notas/ID/estado/nombre preservados al editar. La revisión posterior de ROUNDUP añade otros 5 casos límite.

ASA eSun con multiplicador 3: material 2,4675; electricidad 0,14839; pieza 2,61589; máquina 1,50; precio previo 9,34767; final 9,35; margen previo 5,23411; final 5,24. Afirmaciones exactas en los tests y valores visibles comprobados en la aplicación publicada.

Se mantiene la cobertura de JSON corrupto, defaults, un archivo faltante sin reemplazar el otro, lista vacía, IDs duplicados y errores IO. Los datos legacy se leen repetidamente sin reescritura. Las variantes solo se infieren cuando la clave está ausente; Variant explícita vacía tiene prioridad.

### Revisión explícita de ROUNDUP, sin cambio de versión

Se inspeccionó la implementación antes de modificar archivos. Ya utilizaba `decimal.Round` con `MidpointRounding.ToPositiveInfinity` para valores no negativos y `MidpointRounding.ToNegativeInfinity` para negativos; nunca `MidpointRounding.AwayFromZero`. El informe final anterior omitía esa distinción. Se aclararon comentario y README, sin modificar la lógica ni la UI.

Solo SuggestedSalePrice y GrossMargin se redondean como regla de negocio. GrossMargin parte del precio de venta ya redondeado. No se redondean costes intermedios.

La teoría de ROUNDUP verifica explícitamente los nueve casos solicitados a dos decimales:

| Entrada | Resultado |
| --- | --- |
| 9.34000 | 9.34 |
| 9.34001 | 9.35 |
| 9.34101 | 9.35 |
| 9.34767 | 9.35 |
| 9.34999 | 9.35 |
| 9.35000 | 9.35 |
| 5.23000 | 5.23 |
| 5.23001 | 5.24 |
| 5.23411 | 5.24 |

Los cuatro casos que ya existían se conservaron y se añadieron los cinco restantes. Continúan pasando los casos de cero, negativos, decimal extremo y el cálculo completo ASA eSun. Ejecución Release: 84/84; TRX `work/alpha3-roundup-test-results/alpha3-roundup.trx` en el workspace. No se compiló Android, no se cambió la UI y la versión sigue siendo 0.1.0-alpha.3.

Durante el desarrollo una versión intermedia del ensamblado de tests fue bloqueada por Control de aplicaciones (0x800711C7). No se modificaron políticas ni se desactivó seguridad. Las ejecuciones posteriores normales de Release, tras corregir las pruebas y recompilar, fueron completas y aprobadas.

## Revisión visual y funcional Windows

Se ejecutó el EXE de la publicación oficial x64 autocontenida. La ventana se redimensionó desde sus bordes, sin cambios especiales de código para las dimensiones de prueba. Las capturas de la herramienta omiten parte de los bordes del sistema: dimensiones visibles 1086×753, 1010×693 y 786×693 correspondientes a ventanas 1100×760, 1024×700 y 800×700.

| Tamaño | Resultado |
| --- | --- |
| 1100×760 | Calculadora y Ajustes completos sin scroll en uso normal; filamentos en dos columnas, solo colección desplazable. |
| 1024×700 | Dos bloques de Ajustes y dashboard completos. Detalles compactos accesibles al desplegar. Sin scroll horizontal. |
| 800×700 | Calculadora y Ajustes completos con datos válidos y detalles cerrados; filamentos en una columna, acciones visibles. Sin scroll horizontal; detalles expandidos pueden desplazarse verticalmente. |
| 740×700 adicional | Navegación inferior visible; lista conserva cabecera y Añadir. |

Sistema se aplicó desde JSON legacy sin Theme, siguiendo el SO actual oscuro. Se forzaron Claro y Oscuro durante la ejecución y se confirmó el tema guardado al reabrir. El selector de tema mantiene su índice al cambiar el idioma. No se alteraron los ajustes de tema del sistema operativo.

Se revisaron Calculadora, Filamentos, Ajustes, editor y menú de acciones en oscuro; inputs, texto, tarjetas, botones, selección y detalles mantienen contraste. La implementación WinUI corrige también el tema de los desplegables nativos y sus flechas al forzar Claro sobre un SO oscuro.

Español e inglés cambian en runtime: etiquetas, costes EUR, separación decimal y multiplicador. La calculadora solo presenta filamento, peso y horas/minutos como entradas; sale multiplier reside en Ajustes. Los resultados invalidan a guiones cuando minutos es 60, con indicación localizada sin diálogos; al volver a 0 reaparecen.

Alta real de un perfil temporal con variante Matte, precio escrito 12,5, 1000 g y 120 W. Guardado confirmado en JSON. Edición de Variant de un perfil legacy y estado inactivo comprobados: Name antiguo y resto de valores intactos; aparece Inactive y tarjeta atenuada. Pruebas automatizadas cubren además borrado con confirmación/cancelación y errores de guardado. Los datos originales se restauran exactamente después de cerrar la app y verificar que solo existen los cambios deliberados de prueba; no se deja material temporal en la biblioteca del usuario.

## Compilación, publicación y límites

Build/publicación Windows Release sin errores ni advertencias. Versión 0.1.0-alpha.3 / v0.1-alpha3. Artefactos: ZIP de carpeta Windows x64 sin instalador y ZIP de fuente actualizada, con hashes SHA-256.

Android permanece target real, pero **no se compiló ni validó ni se generó APK en alpha3**, por instrucción explícita. La integración nativa de tema usa un método parcial implementado únicamente bajo Platforms/Windows; no existen referencias WinUI en Core o presentación compartida.

No se han añadido funcionalidades de alpha4. Limitaciones: distribución de carpeta, sin firma comercial; escala/DPI y cambios de tema del SO no exhaustivos; detalles/formularios pueden necesitar scroll en pantallas menores; Android pendiente de validación.
