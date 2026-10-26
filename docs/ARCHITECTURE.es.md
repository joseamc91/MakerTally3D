# Arquitectura

**Español** · [English](ARCHITECTURE.md)

## Android nativo y almacenamiento local

MakerTally utiliza Kotlin, Jetpack Compose y Material 3. Tiene un módulo Android y no dispone de backend, cuentas, permiso de red, analytics ni Google Play Services. Application ID y namespace: `com.joseamc91.makertally`.

| Capa | Responsabilidad |
| --- | --- |
| `domain/` | Modelos decimales validados, motor exacto, ratios, formato y reglas de entrada/editor. |
| `data/` | JSON, Preferences DataStore, AtomicFile, recuperación y estado inicial. |
| `ui/` | Pantallas Compose, shell adaptativo y estado/acciones de `MakerTallyViewModel`. |
| `MainActivity` | Host Compose, edge-to-edge e integración del idioma por aplicación. |

## Estado y persistencia

`MakerTallyViewModel` expone `UiState` mediante StateFlow. Compose observa el estado con el ciclo de vida y envía acciones explícitas al modelo. SavedStateHandle conserva datos de calculadora/navegación durante recreaciones normales de Activity; no es una base de datos de historial.

Preferences DataStore contiene `settings_json` y el registro separado `onboarding_json`. Los ajustes guardan idioma, tema, electricidad/impuestos, calentamiento, máquina, multiplicador y ordenación. El onboarding guarda texto sin procesar, paso, finalización, origen de instalación, ID estable de Generic y reconocimiento del aviso. El texto conserva intencionadamente entradas decimales incompletas.

Los filamentos se guardan como JSON con decimales en texto en `files/filaments.json`, privado de la app. AtomicFile gestiona escrituras y lecturas recuperables. Se diferencia ausencia de error de IO; el contenido dañado se conserva localmente como `.invalid` antes de recuperarlo. El fallback de biblioteca es vacío, sin reintroducir perfiles antiguos de desarrollo.

## Primer arranque y migración 1.0

1. Un registro de onboarding existente decide reanudación o arranque normal.
2. Sin ese registro, almacenamiento físico previo de ajustes, un payload de ajustes o un archivo recuperable de filamentos identifica una instalación existente. Una biblioteca vacía también cuenta; nunca se comparan precios o marcas.
3. Se conservan ajustes y biblioteca válidos y se marca la configuración completada silenciosamente. No se añade Generic.
4. Solo la ausencia real inicia Bienvenida, con idioma del sistema compatible y tema Sistema. No se escribe un payload de ajustes personales ni los siete perfiles antiguos.

La acción final guarda ajustes/finalización y crea el único ejemplo Generic si todavía no existe biblioteca. La biblioteca se escribe atómicamente; ajustes y finalización comparten una edición de DataStore. El ID estable y la conservación de contenido existente permiten reintentar tras una interrupción. El aviso permanece hasta reconocerlo y borrar Generic no lo vuelve a crear.

Los defaults de deserialización de `AppSettings` siguen siendo el contrato de compatibilidad legacy; los valores neutrales iniciales son explícitos y separados. Véase el [contrato detallado](FIRST-RUN-1.1.md).

## UI y principios

Portrait mantiene cabecera y navegación inferior. Landscape reutiliza la lógica de pantallas en un shell con rail; los insets de navegación del sistema orientan el lateral cuando están disponibles. Calculadora, grid, editor y Ajustes utilizan ancho disponible, no DPI físico. Anchuras estrechas y texto ampliado permiten una columna y scroll.

En portrait normal el editor agrupa Material/Activo y Marca/Variante, con campos numéricos completos. Landscape ancho mantiene dos columnas. Ayudas, navegación, contraste y áreas táctiles siguen Material. Insets e IME permanecen en la capa de interfaz.

El alcance sigue siendo cálculo de costes, sin inventario, nube, importación de slicer ni pedidos. Las reglas monetarias están fuera de Compose: [cálculos](CALCULATIONS.es.md). La [privacidad](../PRIVACY.es.md) explica el tratamiento local.
