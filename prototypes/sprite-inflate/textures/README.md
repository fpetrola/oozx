Materiales de [ambientCG](https://ambientcg.com/), bajo **Creative Commons CC0 1.0 Universal**:
se pueden copiar, modificar y distribuir, incluso comercialmente, sin pedir permiso. La atribución
no es obligatoria; está acá porque corresponde.

Son los mapas de color cuadrados que el propio sitio usa para previsualizar, reducidos a 512×512.
Para producción conviene bajar los originales (hasta 8K, más normal, rugosidad y desplazamiento)
desde `https://ambientcg.com/a/<nombre>`. Cualquier `.jpg` o `.png` que se agregue a esta carpeta
lo toma el visor solo.

## Cómo se eligieron

La primera tanda se eligió tomando **una de cada categoría, dando vueltas por todas**. Eso
maximiza la variedad de rubros y minimiza la de aspecto: entraron papel, plástico, papel tapiz y
porcelana —colores planos que no aportan nada sobre una figura— y quedó una sola madera y una
sola tierra, que son las que más lucen.

Esta tanda va al revés. El reparto está pesado a mano hacia lo que se ve: tierra y madera 8 cada
una, ladrillo 6, roca y metal 5, adoquín, baldosa, mármol y piso de madera 4, tela 3, hormigón 2.
Y dentro de cada rubro se piden 26 candidatos y se ordenan por **detalle medido**: la miniatura
contra su propia versión desenfocada, en el centro de la esfera, así el degradado del render no
puntúa y sí la veta, el grano y la junta. Se bajan los mapas grandes sólo de las que ganan.

## Lo que salió mal midiendo, por si alguien repite esto

- El CDN de las miniaturas devuelve **403** al agente por defecto de Python. Hay que identificarse.
  Peor que el 403 fue mi `except Exception: pass`, que se comió los 330 errores y reportó "cero
  candidatos" sin decir por qué.
- Medir el detalle sobre la **miniatura esférica** deja pasar mapas planos: el brillo especular del
  render parece detalle. `Metal034` puntuó 5.4 así y su mapa de color es un amarillo liso, con alta
  frecuencia 0.01. Hay que volver a medir sobre la textura real después de bajarla, que es lo que
  la descartó.
- De la tanda anterior: `ChristmasTreeOrnament019` es un **mapa de normales** en el lugar del de
  color (lila y cian en diagonal), y `Porcelain001` tiene desvío estándar cero.
