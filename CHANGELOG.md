# Historial de cambios

**Español** · [English](CHANGELOG.en.md)

## Unreleased

Sin cambios adicionales registrados.

## 1.1.0 — 2026-10-26

### Experiencia inicial

- Configuración guiada de electricidad e impuestos efectivos, calentamiento opcional, coste de máquina y multiplicador de venta.
- Guardado del paso y de los valores para reanudar la configuración tras cerrar la app.
- Perfil de ejemplo PLA · Generic creado únicamente al finalizar, con aviso inicial hasta reconocerlo.
- Calculadora neutral: Peso y Horas vacíos, Minutos 0, sin resultado inicial ni errores prematuros.

### Interfaz y comprensión

- Ayuda de impuestos inline y referencias de calentamiento en bloques suaves.
- Explicación compacta de potencia, con referencias PLA 120 W, PETG 140 W y ASA/ABS 180 W, sin autofill.
- Editor compacto: Material/Activo y Marca/Variante comparten fila cuando hay ancho; Peso, Precio y Potencia mantienen ancho completo en portrait.
- Mejor equilibrio de Bienvenida y Configuración completada; ajustes para portrait, landscape y fuentes ampliadas.

### Conservación de datos y pruebas

- Migración automática desde 1.0 sin cambiar ajustes, perfiles, estados activos/inactivos ni preferencias.
- Se conservan bibliotecas vacías; no se añade Generic a usuarios existentes ni se reintroducen perfiles eliminados.
- Regresiones de instalación nueva, reanudación, migración e interfaz: 90 tests JVM y 9 instrumentados.
- Documentación bilingüe y CI con lint; GitHub conserva código e informes de tests, sin distribuir APK/AAB.

## 1.0.0 — 2026-10-07

- Primera versión Android estable con Kotlin, Jetpack Compose y Material 3.
- Calculadora de material, electricidad, impuesto efectivo, máquina, precio sugerido y margen; precisión decimal y redondeo explícito.
- Perfiles editables, activación/desactivación y ordenación.
- Ajustes de electricidad, calentamiento, máquina y multiplicador; Ayuda y Privacidad offline.
- Español/English, Sistema/Claro/Oscuro, portrait y landscape adaptativo.
- Almacenamiento privado local, sin cuentas, red, anuncios ni tracking; primera preparación para Google Play.

MakerTally comenzó como un prototipo multiplataforma y se reconstruyó como aplicación Android nativa antes de 1.0. Los hitos alpha y MAUI son históricos, no versiones públicas adicionales.
