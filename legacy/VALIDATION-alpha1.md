# Validación de v0.1-alpha1

Comprobado el 4 de octubre de 2026 en Windows x64, SDK .NET 10.0.401 y runtime 10.0.12.

## Compilación y publicación

- `dotnet build PrintCost.sln -c Release`: correcto, 0 errores y 0 advertencias en la compilación final.
- `dotnet test PrintCost.sln -c Release --no-build`: **39 casos aprobados**, 0 fallos y 0 omitidos.
- Publicación `win-x64`, self-contained y single-file: correcta.
- Metadatos del ejecutable publicado: ProductVersion `0.1.0-alpha.1`, FileVersion `0.1.0.0`.

## Cobertura automatizada

- Ejemplo ASA eSun completo, comprobando cada resultado antes del redondeo y el margen con tolerancia.
- Precio/g y precio/kg con una bobina distinta de 1000 g; horas y minutos fraccionarios.
- Costes cero, multiplicador cero/uno, margen con venta cero, calentamiento con cero horas.
- Rechazo de peso de bobina cero/negativo y otras entradas negativas.
- Coma/punto decimal, campo vacío, separadores aislados, texto y números fuera del rango decimal.
- Creación de los siete perfiles y ajustes iniciales; persistencia de idioma, valores, IDs, notas y estado inactivo.
- JSON corrupto, datos semánticamente inválidos, archivos ausentes, inventario vacío, IDs duplicados y ubicación no escribible.
- Prueba WPF en hilo STA: ventana, bindings reales, validación, cambio de idioma, cultura, recálculo y guardado de Ajustes.
- Alta/edición/baja mediante los comandos y ventanas modales reales, con almacenamiento temporal aislado. Confirmación cancelada y aceptada.
- Filamento seleccionado que se inactiva; lista vacía; multiplicador simulado independiente del predeterminado.
- Ventana reducida a 820 × 560, desplazamiento y edición del multiplicador después de traerlo al área visible.

## Comprobación visual y ejecutable final

Se abrió la aplicación compilada y el ejecutable portable publicado usando la habilidad `computer-use`.

- Calculadora: material 2,47 €, electricidad 0,15 €, impresión 2,62 €, máquina 1,50 €, coste completo 4,12 €, venta 9,35 €, beneficio 5,23 €, margen 55,97 %.
- Ajustes: cambio a English sin reiniciar; textos, etiquetas y decimales actualizados.
- Inventario: siete filas iniciales, precios con formato inglés y desplazamiento horizontal.
- Ejecutable portable: arranca y recupera el idioma guardado en la ejecución anterior.

La revisión visual se realizó en este equipo Windows; no se ha realizado una matriz de pruebas en otros equipos, versiones de Windows ni configuraciones de DPI.

## Recursos de idioma

Auditoría estática: **86 claves** en cada diccionario, con las mismas claves en ambos idiomas. Todos los textos visibles definidos en las vistas usan recursos o bindings; se revisaron también los textos de los ViewModels y los diálogos propios. Los nombres y notas del usuario se mantienen como datos, sin traducirlos.
