# Referencia histórica de alpha1

Este árbol WPF está congelado bajo la antigua identidad PrintCost. Queda fuera de MakerTally.sln y de CI; sus namespaces y rutas antiguas son referencias históricas deliberadas. No se compila contra MakerTally.Core ni se mantiene como otra aplicación activa.

`PrintCost.Desktop` contiene la UI WPF retirada de la solución activa después de compilar y revisar MAUI en Windows. No se mantiene como una segunda aplicación. Se conserva porque esta carpeta no tenía Git y era importante no descartar trabajo útil durante la recuperación.

`WpfBindingsTests.cs` conserva la antigua prueba de interfaz WPF. Pasó junto con todas las demás antes de archivarse: 49/49. Sus escenarios de cálculos, entradas, idioma, persistencia e inventario están cubiertos en `SharedPresentationPreservesTheAlpha1Scenarios`, además de la revisión nativa de MAUI. Las 38 pruebas originales de lógica, entradas y almacenamiento permanecen sin cambios en el proyecto activo.

Los ZIP originales de alpha1 también se conservan en la carpeta `outputs`, fuera de este proyecto. No se han sobrescrito. El ZIP de código alpha1 permite recuperar la solución y su proyecto de pruebas originales completos.
