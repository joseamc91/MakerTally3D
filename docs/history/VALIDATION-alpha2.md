# Validación — PrintCost v0.1-alpha2

Fecha: 2026-10-04. Entorno: Windows x64, SDK .NET 10.0.401 / runtime 10.0.12, MAUI 10.0.20; Android SDK API 36, build-tools 36.0.0, Microsoft OpenJDK 17.0.14.

## Recuperación y pruebas

- Auditoría inicial de solución, fuentes, versiones, Git y artefactos: migración parcial, sin Git. Informe detallado en `RECOVERY.md`.
- Pruebas intentadas antes de cambios: no ejecutables por error de importación de `Path` en una nueva clase de almacenamiento.
- Tras la corrección mínima: 48/48 aprobadas, incluidos los 39 casos originales.
- Tras corregir la traducción dinámica y añadir regresión: 49/49 aprobadas, aún incluyendo el test WPF.
- Tras archivar WPF: **48/48 pruebas activas aprobadas**, cero fallidas/omitidas. Se conservaron las 38 pruebas originales no visuales y el código del test WPF en `legacy`.
- Compatibilidad alpha1: ruta Windows idéntica, ruta Android privada, apertura repetida de JSON existente sin reescritura, idioma, ajustes personalizados, identificadores, notas e inactivos conservados.
- Persistencia: valores iniciales, archivo faltante, JSON corrupto preservado, inventario vacío, errores de lectura/escritura, guardado de ajustes y operaciones de inventario.
- Presentación: validaciones, coma/punto, desbordamientos, traducciones, multiplicador independiente, CRUD y confirmación/cancelación de borrado, activar/inactivar y resultados.

## Compilación y artefactos

- `dotnet build PrintCost.sln -c Debug` con rutas SDK/JDK explícitas: Core, Tests y App para **Windows y Android**, cero errores y advertencias.
- Windows Release publicado sin empaquetar, `win-x64`, .NET y Windows App SDK incluidos, sin trimming. Se entrega la carpeta completa dentro del ZIP.
- Android Debug: APK firmado con la clave debug del SDK y ensamblados embebidos; no requiere archivos de fast deployment externos. Metadatos: `com.printcost.app`, versionCode 2, versionName `0.1.0-alpha.2`, API mínima 21 y target 36.
- Android no se ejecutó en dispositivo/emulador. La consulta de dispositivos conectados no devolvió dispositivos. El permiso INTERNET que puede añadir el SDK a Debug se usa para herramientas de depuración; PrintCost no contiene llamadas de red.
- Textos visibles de las vistas centralizados en recursos; idioma inicial persistido y cambio dinámico revisados. No hay DataGrid ni UI web.

## Caso ASA eSun

Sin cambios en las fórmulas ni redondeos intermedios:

| Concepto | Resultado interno |
|---|---:|
| Precio por gramo | 0,0175 |
| Material | 2,4675 |
| Calentamiento | 0,020 kWh |
| Impresión | 1,080 kWh |
| Consumo total | 1,100 kWh |
| Electricidad | 0,14839 |
| Coste impresión | 2,61589 |
| Máquina | 1,50 |
| Coste completo | 4,11589 |
| Venta | 9,34767 |
| Beneficio bruto | 5,23178 |
| Margen | 55,9688136188… % |

En UI española: **9,35 €**, **4,12 €**, **5,23 €**, **55,97 %**. En inglés: **€9.35**, **€4.12**, **€5.23**, **55.97 %**.

## Revisión nativa Windows

| Tamaño de ventana configurado | Calculadora | Ajustes | Inventario |
|---|---|---|---|
| 1100 × 760 | Tres columnas; contenido completo sin desplazamiento vertical | Tarjetas compactas, contenido completo | Dos columnas |
| 1024 × 700 | Tres columnas; contenido y técnicos completos sin desplazamiento vertical con entradas válidas | Contenido completo, sin desplazamiento vertical | Dos columnas, cabecera y Añadir visibles |
| 800 × 700 | Dos columnas y desplazamiento vertical accesible | Contenido completo sin desplazamiento vertical | Una columna; solo la colección se desplaza |

No se observó desplazamiento horizontal ni controles cortados en estos tamaños. La captura de ventana excluye los bordes DWM y por ello puede mostrar unos píxeles menos que las dimensiones configuradas.

Comprobado en pantalla: arranque, navegación, valores ASA, editor de alta/edición, validación de precio incompleto, Guardar deshabilitado mientras hay errores, aceptación de punto en español, guardado de perfil, alta cancelada, idioma en vivo y persistido tras reiniciar, ocultación del resultado con entrada inválida y recuperación usando coma, cabecera fija de inventario y desplazamiento solo de tarjetas.

El CRUD completo, activación y eliminación aceptada/cancelada se verificaron mediante pruebas automatizadas con almacenamiento temporal. No se borraron perfiles personales durante la revisión visual.

Las huellas de `settings.json` y `filaments.json` personales coinciden exactamente con las anteriores a las pruebas. No se requirió una restauración de datos.

## Límites observados

- Android todavía necesita revisión visual y funcional en hardware/emulador, especialmente teclado táctil, rotación y márgenes del sistema.
- A 800 × 700 la calculadora necesita desplazamiento vertical. Errores visibles, fuentes grandes o textos largos también pueden necesitarlo; los controles siguen accesibles.
- Se detectó un bloqueo de Control de aplicaciones sobre una compilación Windows intermedia. La publicación oficial sí abrió y se revisó, sin cambiar políticas de seguridad.
- Los paquetes carecen de firma comercial/producción. Windows incluye una carpeta de dependencias y tiene mayor tamaño que la distribución WPF.
