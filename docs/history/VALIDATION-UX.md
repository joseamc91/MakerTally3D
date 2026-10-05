# Pulido visual — PrintCost v0.1-alpha3

Entrega identificada como `alpha3-ux`, sin cambio de versión funcional ni metadatos: 0.1.0-alpha.3. Fecha: 2026-10-04.

## Punto de partida y alcance

Antes de editar se ejecutaron las 84 pruebas compartidas, todas aprobadas, y se abrió el paquete Windows anterior para revisar Calculadora y Filamentos. Se conservaron una copia de las fuentes y los dos archivos de datos actuales. No hay repositorio Git; la comparación se realiza contra esa copia de fuentes.

Solo cambian CalculatorView.xaml, InventoryView.xaml, recursos visuales adicionales en Theme.xaml y tres textos en cada idioma. No hay cambios de C#, Core, fórmulas, ROUNDUP, modelos, persistencia, JSON, ViewModels, comandos, navegación, mecanismo de idioma/tema, Ajustes o proyectos. Los tests permanecen intactos.

## Cambios comprobados

- Tarjeta azul: únicamente título y precio, con tamaño 50 frente a 46 anterior.
- Costes: coste de la pieza destacado a 17 px seminegrita, material y electricidad a 14 px, máquina (adicional) secundaria a 12 px. Sin divisoria, tarjeta extra ni margen grande.
- Margen bruto: solamente en Detalles, cerrado al iniciar. Nueve datos técnicos; sin los precios antes/después de ROUNDUP. Lógica interna intacta.
- Filamentos: padding vertical 10 frente a 16 anterior; separación entre tarjetas 8 frente a 12. Peso y potencia en una línea. En los siete perfiles de prueba, altura aproximada 96 px frente a 136. Nombre compuesto, precio pagado y precio/kg permanecen visibles.
- Acciones: botón transparente con glifo de tres puntos; área 48 × 48, botón nativo accesible, focus de teclado y tooltip. El menú sigue mostrando Editar, Activar/Desactivar y Eliminar.
- Extended FAB: Button MAUI de 56 px, radio 16, colores primarios del tema, Shadow de radio 8/opacidad 0,18 y tooltip localizado mediante ToolTipProperties.Text. Sin nuevas dependencias ni código de plataforma. Reutiliza AddCommand. Su fila inferior, fuera de CollectionView, evita solapamiento con perfiles y navegación. No se necesita variante circular a los tamaños probados.

Se comprobó abrir el formulario desde el FAB y cancelarlo sin guardar. Se comprobó abrir el menú de acciones, sin modificar perfiles. El tooltip `Add filament` se observó realmente en Windows; Español/English cambian las etiquetas nuevas en ejecución.

## Validación

- Antes: 84/84 tests, 0 fallidos/omitidos.
- Después: 84/84 tests, 0 fallidos/omitidos; no se añadieron pruebas artificiales para cambios XAML.
- Build Windows Release: 0 advertencias, 0 errores.
- Publicación Windows x64 autocontenida sin instalador: iniciada y revisada.
- ASA eSun, 141 g, 6 h, multiplicador 3: precio 9,35 €, coste pieza 2,62 €, margen 5,24 € en Detalles. Precisión interna y reglas Excel intactas.

| Ventana | Resultado |
| --- | --- |
| 1100 × 760 | Precio y costes compactos; Detalles visibles al expandir. Filamentos en dos columnas: los siete perfiles completos, frente a seis y parte del séptimo antes del pulido. Ajustes intacto. |
| 1024 × 700 | Calculadora y Ajustes completos en uso normal; dos columnas de Filamentos y siete perfiles visibles. FAB separado del contenido; sin scroll horizontal. |
| 800 × 700 | Calculadora/Ajustes completos en uso normal; una columna de Filamentos. Solo colección desplazable: cabecera y FAB permanecen fijos. Última tarjeta totalmente accesible por encima del FAB. Detalles abiertos pueden requerir un leve scroll vertical. |
| 740 × 700 adicional | Navegación inferior intacta y FAB situado encima, sin solapamiento. |

La herramienta de captura omite parte de los bordes del SO: los tamaños capturados fueron 1086×753, 1010×693, 786×693 y 726×693 respectivamente. Redimensionado con controles normales de Windows, sin alterar la configuración de la ventana en código.

Sistema se revisó con el SO actual oscuro; Claro y Oscuro se aplicaron durante la ejecución. Calculadora, detalles, Filamentos, FAB, menú y Ajustes mantienen contraste. No se cambió el tema del SO ni se probó cada combinación de DPI/escala. Se revisaron ambos idiomas, sin cambiar el mecanismo existente.

Los cambios temporales de idioma/tema se restauraron tras cerrar la aplicación. Se comprobó que `settings.json` y `filaments.json` coinciden byte por byte con sus copias de inicio también después de reabrir. No se dejan perfiles temporales ni cambios de precios/parámetros.

Android no fue compilado, ejecutado ni publicado. Sigue siendo un target real, sin dependencias Windows nuevas en código compartido.

## Entrega

`PrintCost-v0.1-alpha3-ux-win-x64.zip` y `PrintCost-v0.1-alpha3-ux-source.zip`. Los artefactos anteriores se conservan. Descomprimir la carpeta completa y abrir PrintCost.exe. Auditoría de archivos, recursos y datos en `VALIDATION-UX-audit.json`; hashes en `PrintCost-v0.1-alpha3-ux-SHA256SUMS.txt` junto a los ZIP.

La distribución continúa como carpeta sin instalador/firma comercial. Los detalles expandidos pueden desplazarse en pantallas compactas y no se cubren todas las escalas de accesibilidad. No se inicia alpha4.
