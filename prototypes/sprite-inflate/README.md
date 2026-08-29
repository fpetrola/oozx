# sprite-inflate

Un sprite del Spectrum convertido en un sólido 3D: xBRZ para la silueta, un campo de distancia
para la panza, y un OBJ del otro lado. Prototipo para el visor 3D, no forma parte del emulador —
no está en los `<modules>` del proyecto de arriba y nada del emulador depende de esto.

## Cómo se corre

Primero hay que tener el proyecto de arriba instalado (`mvn install`), porque `Grab` usa el
emulador.

```bash
# 1. sacar un sprite real de un juego en marcha (opcional: ya hay uno en sprites/willy.png)
mvn -q exec:java -Dexec.mainClass=com.fpetrola.oozx.proto.inflate.Grab \
    -Dexec.args="/ruta/a/jsw-full.rzx 6000 target/screen.png"

# 2. inflarlo: escribe los PNG de diagnóstico y un OBJ por perfil
mvn -q exec:java -Dexec.args="sprites/willy.png target/out 4"
#                              sprite          salida     escala ("4", o "4x2" para 8x)

# 3. mirarlo, girarlo, comparar perfiles (1-4 cambia, espacio gira, arrastrar orbita)
mvn -q exec:java -Dexec.mainClass=com.fpetrola.oozx.proto.inflate.InflateViewer \
    -Dexec.args="target/out"

# ...o pasarle una HOJA de sprites: los muestra todos en un panel al costado, se elige con el
# mouse (o las flechas) y se infla en vivo, y abajo se elige el color.
# Sin argumentos usa sprites/img.png si existe, así que anda con el botón verde del IDE.
mvn -q exec:java -Dexec.mainClass=com.fpetrola.oozx.proto.inflate.InflateViewer \
    -Dexec.args="sprites/img.png"

# y para revisar el corte a ojo: escribe cada sprite y una lámina con todos
mvn -q exec:java -Dexec.mainClass=com.fpetrola.oozx.proto.inflate.SpriteSheet \
    -Dexec.args="sprites/img.png target/sprites"
```

`sprites/img.png` son los 152 guardianes de Jet Set Willy. La grilla no está hardcodeada: se mide
el color de fondo, se buscan las islas del mismo tamaño (eso descarta el título, cuyas letras
también son islas pero cada una de un tamaño distinto) y se detecta a cuántas veces está dibujada
la hoja probando a qué tamaño de bloque todo bloque es de un solo color. Deshacer ese aumento es
obligatorio: darle a xBRZ píxeles que ya vienen de a pares le hace redondear los pares, o sea
suavizar el agrandado en lugar del dibujo, y sale peor que no escalar.

## Qué hace, paso por paso

1. **Un margen transparente.** Willy llena su celda de 16×16: el sombrero está en la fila 0 y los
   pies en la 15. Donde la figura se sale del borde no hay contorno donde cerrar, y el sólido
   queda abierto arriba y abajo.
2. **xBRZ.** Lo que se ve escalonado cuando el sprite está parado en 3D es su contorno.
3. **Rellenar los agujeros chicos, y devolverlos después.** El ojo de Willy es un píxel de fondo
   adentro de la cabeza. Como agujero perfora el sólido y, peor, el campo de distancia mide desde
   ahí y le deja toda la cara flaca. Así que se rellena para medir — y una vez que la geometría
   está resuelta vuelve como **hoyuelo y color**, que es lo que un ojo es de todos modos. El
   hoyuelo es deliberadamente poco profundo: alcanza para agarrar la luz de un lado, nunca para
   tocar la otra superficie y reabrir el agujero que se había tapado. Medido sobre el mesh, la
   altura máxima va de 4.95 en el centro del ojo a 7.49 en la cara lejos.
4. **Campo de distancia** exacto (transformada de Felzenszwalb) hacia adentro del contorno.
5. **Grosor local**: el radio del disco inscrito más grande que *cubre* cada punto. Esto es lo que
   le avisa a una pierna de dos píxeles que es una pierna de dos píxeles y no parte del torso.
6. **La panza**, espejada — lo mismo para adelante que para atrás, así la sección de un miembro es
   una elipse entera. Las dos mitades se juntan en el contorno, donde la profundidad es cero.
7. **Malla soldada** con una normal por vértice.

## Lo que se midió

Profundidad contra ancho local, sobre el Willy real:

| perfil | partes finas | partes gruesas |
|---|---|---|
| lineal | 0.40× | 0.47× |
| esfera, R global | **1.73×** | 0.83× |
| esfera, R local | **0.76×** | **0.79×** |

`sphere-local` da la misma proporción en las dos escalas: cada parte queda tan redonda como
ancha. Con R global los brazos salen a 1.73× su semiancho — aletas.

Dos ideas que **no** funcionaron, anotadas para el próximo que las tenga (están en el código con
su explicación):

- **`dome-local`**, un perfil de pendiente finita en el borde. Saca el peine de facetas del borde
  y mete una cresta por el medio de cada miembro, que es peor: el borde es un anillo de caras y
  la cresta corre a lo largo de toda la figura.
- **Escalar más.** Largo del contorno sobre raíz del área, con Willy:

  | escala | 2x | 3x | 4x | 5x | 6x | 8x | 12x |
  |---|---|---|---|---|---|---|---|
  | contorno | 7.12 | 7.08 | 7.17 | 7.15 | 7.19 | 7.02 | 7.11 |
  | triángulos | 1 616 | 3 588 | 5 964 | 9 380 | 13 060 | 22 824 | 50 628 |

  Sin tendencia: xBRZ decide qué esquinas redondear sobre la grilla **original**, y escalar más
  solo dibuja esas mismas decisiones con paso más fino. El límite es la decisión, no el muestreo.
  Sobre un disco, cuyo perímetro ideal se conoce (2√π = 3.545), se queda en ~3.61 a cualquier
  escala. Ese 2% es la aproximación de xBRZ.

Y una que sí importó menos de lo esperado: **thresholdear después de escalar** cuesta poco.
Escalar cierra el 85% de la brecha contra el contorno ideal y conservar el coverage cierra 5
puntos más. Si thresholdear simplifica el mesh, hacelo — siempre que escales primero.

## Verificación

`writeObj` cuenta las aristas usadas por una sola cara. Un sólido cerrado tiene **cero**, y eso es
todo el "no se ve para adentro" expresado como número. Esa cuenta encontró un error real: los
vértices del contorno tomaban la profundidad interpolando entre una celda interior (que a medio
píxel del borde ya vale ~2) y una exterior (cero), con lo que la figura quedaba abierta a lo largo
de toda su silueta.

## Si esto se lleva a otro proyecto

`xbrz-core` es **GPL-3 con excepción de linkeo** (ver el `NOTICE` en la raíz). Es un motivo más
para mirar Kopf–Lischinski, *Depixelizing Pixel Art* (SIGGRAPH 2011), que además devuelve curvas
en vez de un bitmap más grande: para extruir, un contorno como splines es mejor materia prima, y
permite teselar el borde tan fino como haga falta sin subir la resolución del interior —
justamente donde escalar más no da nada.
