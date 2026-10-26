# Desarrollo y pruebas

**Español** · [English](DEVELOPMENT.md)

## Herramientas

Utiliza Android Studio estable compatible y **JDK 21 para Gradle**, como CI; el JBR incluido sirve cuando sea compatible. El código Java tiene compatibilidad fuente/destino 17. Usa el Gradle Wrapper del repositorio, no un Gradle instalado por separado.

| Componente | Configuración |
| --- | --- |
| AGP / Gradle | 9.4.0 / 9.6.0 |
| Kotlin / plugin Compose | 2.4.20 |
| Compose BOM / Material 3 | 2026.06.01 / 1.4.0 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 24 |
| SDK | Platform 36, Build Tools 36.0.0, Platform Tools |

Configura `ANDROID_HOME` o tu propio `local.properties` ignorado. Rutas JDK/SDK, overrides de AAPT2 y firma deben permanecer locales. No se necesita ninguna carpeta personal concreta. Utiliza herramientas compatibles sin debilitar políticas de seguridad; CI debe seguir siendo portable.

## Comandos

```sh
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug

# Con emulador/dispositivo de pruebas dedicado y arrancado:
./gradlew connectedDebugAndroidTest

adb devices
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.joseamc91.makertally/.MainActivity
```

En PowerShell/Windows usa `gradlew.bat`. Sustituye el serial por el de tu emulador. Utiliza un entorno aislado: los tests instrumentados de UI ejercitan temporalmente configuración y preferencias. Un debug con otra firma no puede actualizar una instalación de producción.

## Tests y CI

La suite actual contiene 90 tests JVM y 9 instrumentados. Se prueban aritmética/impuestos, entradas, JSON/DataStore, migración, onboarding, CRUD/ordenación, ViewModel y recursos. La instrumentación cubre AtomicFile real, IO, accesibilidad Compose, validación, giro, ayuda inline y conservación del borrador del editor.

Android CI valida wrapper, tests JVM, lint y `assembleDebug` en Linux. Solo sube informes JUnit XML con retención de 14 días. No ejecuta emulador, firma producción, genera AAB de publicación, ofrece binarios instalables ni publica en Play. Los APK Debug locales son para desarrollo, no un canal oficial de descarga.

## Estructura

`app/src/main/java/.../{domain,data,ui}` contiene la app; `app/src/test` tests JVM; `app/src/androidTest` instrumentados; `app/src/main/res` recursos ES/EN; `.github` workflows; `branding/` identidad oficial y `docs/assets` presentación pública. `store/` conserva materiales de la ficha 1.0; los informes históricos indican su hito original.

Véanse [arquitectura](ARCHITECTURE.es.md), [cálculos](CALCULATIONS.es.md), [contribuciones](../CONTRIBUTING.md) y [publicación](RELEASE_PROCESS.es.md). La licencia y avisos de terceros rigen la redistribución; los binarios instalables oficiales solo se distribuyen por Google Play.
