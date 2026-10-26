# Proceso de publicación

**Español** · [English](RELEASE_PROCESS.md)

## Código y distribución

`main` contiene el código actual. **Google Play es el único canal oficial de binarios Android instalables de MakerTally.** GitHub no aloja APK, AAB ni instaladores en Releases, adjuntos o artifacts de Actions. Compilar el código sigue siendo posible bajo Apache-2.0.

Si en el futuro se utilizan GitHub Releases, pueden incluir notas y documentación asociadas a tags, sin binarios instalables. No se crean releases vacías por estética. Los informes de tests sí pueden ser artifacts. CI valida wrapper, tests JVM, lint y Debug; los instrumentados se ejecutan localmente antes de publicar.

La línea 1.0 se utiliza en el test cerrado de Google Play. Este documento es un procedimiento recurrente, no una afirmación sobre pasos concretos pendientes o completados en Play Console. El mantenimiento de documentación no cambia pistas existentes de Play.

## Antes de publicar en producción

1. Revisar versionName e incrementar versionCode por encima de cualquier subida previa. Código actual: 1.1.0 / code 6; cambiar versión en Git no sube nada.
2. Actualizar ambos changelogs y documentación pública cuando cambie comportamiento.
3. Ejecutar todos los tests JVM, lint, Debug e instrumentados en un entorno de pruebas dedicado. Conservar evidencia y revisar avisos conocidos.
4. Realizar un **real-device smoke test**: configuración limpia, actualización/conservación de datos, calculadora, editor, idioma/tema y ambas orientaciones. Revisar compatibilidad y requisitos nativos/16 KB del bundle real.
5. Generar el AAB final firmado mediante el proceso seguro de producción existente, fuera del CI público. Verificar package, versión, identidad de firma y Play App Signing/upload key. Claves, credenciales y configuración privada quedan completamente fuera de Git y de logs.
6. Validar build firmada, copy, capturas, icono/feature graphic, privacidad y avisos de terceros. Los materiales 1.0 existentes no son una entrega automática de 1.1.
7. Revisar requisitos de cuenta/testing, declaraciones, ficha, regiones y pista en Play Console. Enviar solo con autorización explícita para esa publicación.
8. Crear tag únicamente después de aprobar código y build finales. Esta preparación del repositorio no crea tag ni GitHub Release.

## Contrato de privacidad / Data Safety

- App/desarrollador: MakerTally 3D / joseamc91; package `com.joseamc91.makertally`.
- Contacto público: joseamc91.dev@gmail.com; soporte/privacidad: https://github.com/joseamc91/MakerTally3D/issues.
- Política canónica pública: https://github.com/joseamc91/MakerTally3D/blob/main/PRIVACY.md; [español](../PRIVACY.es.md). Ambos textos completos están incluidos offline.
- Datos recogidos fuera del dispositivo por la app: ninguno. Datos compartidos por la app: ninguno.
- Cuentas, anuncios, tracking, analytics, telemetría y reporting remoto: ninguno.
- Sin INTERNET, backend/nube/sincronización propios ni Google Play Services.
- Perfiles, ajustes, progreso/valores de configuración y reconocimientos se guardan localmente. No se afirma cifrado propio. Backup Android desactivado, sin garantizar transferencias OEM/sistema.
- El navegador y el contacto voluntario por GitHub/email dependen de esos servicios; MakerTally no adjunta datos locales.

Las declaraciones Play deben reflejar la app distribuida. Revisar este contrato si cambian dependencias, permisos o flujos. Completar Ads, App access, Content rating y Target audience cuando corresponda; no deducirlas solo del README.

## Ficha pública y assets

Cuando la ficha esté disponible para usuarios normales, añadir a ambos READMEs el badge oficial de **Google Play**, con destino `https://play.google.com/store/apps/details?id=com.joseamc91.makertally`. Será el único botón oficial de descarga. No activarlo durante testing cerrado.

`store/` conserva los materiales 1.0; `docs/assets` contiene presentación del repositorio. Ninguna carpeta distribuye instaladores. Para Social Preview de GitHub, cargar `docs/assets/SocialPreview.png` desde Settings → General → Social preview; no se utiliza una API de subida no soportada.
