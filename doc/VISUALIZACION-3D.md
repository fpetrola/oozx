# Ver un juego de ZX Spectrum en 3D

Este documento cuenta **qué estamos haciendo y con qué algoritmos**. Es el mapa: cada sección
resume una pieza y apunta al documento donde está el detalle (`DETECCION-SPRITES-3D.md` para la
detección, `SPRITE3D.md` para el render y la config).

---

## 1. El problema

Un juego de Spectrum no tiene objetos: tiene **6144 bytes de pantalla**. No hay una lista de
sprites, no hay coordenadas, no hay "el personaje". Hay bits que alguien puso ahí, y el juego
sabe qué significan porque su código lo sabe — el archivo no.

Para dibujar la escena en 3D hace falta responder tres preguntas que la pantalla no contesta:

1. **¿Qué bytes son un objeto y cuáles son decorado?** Un personaje parado sobre una plataforma
   es un solo parche de píxeles encendidos.
2. **¿Dónde empieza y dónde termina cada objeto?** Dos cosas que se tocan son un parche.
3. **¿Qué forma 3D le corresponde?** Una plataforma quiere ser un bloque, un planeta una esfera,
   un personaje un volumen que lo siga cuadro a cuadro.

Todo lo que sigue existe para contestar esas tres, sin parchear cada juego a mano.

---

## 2. El pipeline, de punta a punta

```
   RZX (partida grabada)
         │
         ▼
   ┌───────────────────┐   emulador Z80 instrucción por instrucción, determinista
   │   TaintReplay     │   + OriginTaint (de qué direcciones está hecho cada byte)
   │                   │   + árbol de llamadas (qué invocación escribió cada byte)
   └───────────────────┘
         │                        │
   OFFLINE (una vez por juego)    EN VIVO (cada frame, mientras se juega)
         │                        │
         ▼                        ▼
   ┌───────────────────┐    ┌──────────────────────┐
   │  TaintDiscover    │    │  FrameSnapshot        │  pixels, attrs, owner, tile,
   │  SpriteComposites │    │  (inmutable)          │  spriteBits, group
   └───────────────────┘    └──────────────────────┘
         │                        │
         ▼                        ▼
   catalogo-<juego>.md      ┌──────────────────────┐
   objetos-auto-<j>.png     │      JSW3D           │  relieve, sprites, objetos
   games.json               │  Sprite3DPipeline    │  2D → malla 3D
                            └──────────────────────┘
                                   │
                                   ▼
                              escena libGDX
```

Dos tiempos, y la separación importa: **lo caro y lo estadístico va offline** (mirar miles de
frames, medir, decidir qué es sprite), **lo barato y local va en vivo** (¿este parche es tal
objeto? ¿qué malla le toca?). Lo offline produce archivos que son *conocimiento del juego* y
viven en el repo; lo de runtime no guarda nada.

---

## 3. La taint de origen: de qué está hecho cada byte

`OriginTaint` es la base de todo lo demás. Cada byte de memoria (y cada registro) lleva un **id
de nodo** que describe de qué **direcciones originales** salió su valor.

- `0` = sin taint, `addr+1` = ORIGIN(addr), ids más altos = nodos UNION.
- Los nodos se internan (*hash-consing*): "byte de sprite OR byte de fondo" se crea una vez y
  todas las veces siguientes pegan en el memo. El costo por operación es un lookup y la tabla
  no crece.
- Cada nodo cachea la respuesta a la única pregunta que hace el visor —"¿alguna hoja cae en el
  catálogo de sprites, y en cuál?"— así que clasificar un byte de pantalla es O(1).
- Los términos crecen solo en cadenas *read-modify-write* (el OR/XOR de un blit sobre el mismo
  byte); un store **reemplaza** el término.
- Hay un tope de profundidad, y **cortar por el lado profundo es deliberado**: en una cadena de
  compositing lo viejo es historia rancia y lo fresco es el sprite que se está dibujando. En
  runtime el tope es 64; en descubrimiento es 512 (`-Dtaint.depth`), porque ahí las hojas viejas
  son justamente lo que se busca.
- **Refinamiento por BIT** (`spriteBits`): para motores que componen con máscara
  (`fondo AND mascara OR sprite`), el grafo de nodos contesta "de qué direcciones", pero no
  "qué píxeles de este byte son del sprite". Eso se lleva aparte, un bit por píxel.

Consecuencia práctica: **da igual cómo dibujó el juego**. Blit directo, copias pre-shifteadas,
compositing con máscara, ensamblado por pedazos, rebotes por buffers intermedios: si el valor
vino de esa dirección, la taint lo sabe.

---

## 4. Descubrir el catálogo: qué bytes de la RAM son gráficos

`TaintDiscover` replaya el RZX con la taint vacía y, para cada byte encendido de los frames
sampleados, pregunta de qué direcciones se armó. **Esas direcciones son los gráficos del juego.**

La unidad de observación es el **blob conexo** de bytes encendidos. Su conjunto de hojas se
parte en **piezas por hueco de direcciones** (`discover.gap`): un sprite parado en una plataforma
da su pieza de 32 bytes más la pieza del tile de la plataforma, ya separadas.

Después hay que decidir **sprite o fondo**, y para eso no alcanza una señal sola. Se miden seis
y tienen que coincidir:

| señal | qué dice | por qué |
|---|---|---|
| **bpo** | bytes pintados por *stamp* | un sprite pinta 8-40, un fondo cientos |
| **fresh** | fracción de bytes repintados hace poco | los sprites se redibujan, un fondo pintado una vez no |
| **stamps** | apariciones por frame | las entidades son pocas, un patrón de relleno son docenas |
| **reuse** | bytes de pantalla por byte de gráfico | un blit es ~1:1, un tile estampado en 10 celdas es 10:1 |
| **mobility** | celdas distintas jamás pintadas vs una huella | las plataformas nunca se mueven |
| **drift** | movimiento del conjunto de celdas entre frames | el par de dirty-regions (ver abajo) |

**Motores de dirty-regions** (Exolon, Dynamite Dan reescriben 2-10% de la pantalla por frame)
necesitan el par `-Ddiscover.gate` + `-Ddiscover.drift`: los rastros que esos motores dejan
encendidos llevan taint vieja que **invierte todas las demás señales a la vez**. Los motores que
re-blitean la pantalla entera (JSW, Manic Miner) quieren los dos apagados.

Salida: la tabla `sprites_found` (mismo formato que escribía el tracker viejo, así que
`SpriteCatalog` la carga sin cambios) y **`doc/catalogo-<juego>.md`**, que no es documentación:
es la fuente que el visor carga.

---

## 5. El árbol de llamadas: quién escribió cada byte

Esta es la pieza que cambió el enfoque. La pantalla no dice dónde termina un objeto, pero **el
código sí**: la rutina que decidió dibujar una cosa es una sola.

`TaintReplay` mantiene una pila de llamadas en la sombra y, por cada byte de pantalla, guarda
**en qué invocación se escribió** (`writeNode`), además de `lastWrite` (en qué frame) y
`writeOrder`.

Dos decisiones que costaron corridas y valen para cualquiera que reimplemente esto:

- **La pila se sigue por el SP, no contando CALL y RET.** El código Z80 vuelve de maneras que
  un contador no sobrevive: `PUSH addr` + `RET` como salto calculado, una rutina que se come su
  propio frame, una interrupción cayendo en cualquier lado. Cada una de esas drenaba la pila
  sombra hasta que **todas las escrituras parecían pasar en la raíz**, que fue exactamente lo
  que mostró la primera medición. Ahora: si el PC saltó (no es la instrucción siguiente) y el SP
  bajó, es una llamada; si subió, se sale de tantos niveles como haga falta.
- **Un nodo por INVOCACIÓN, no por ruta.** Internar por ruta colapsaba los frames: tres misiles
  dibujados por la misma rutina eran un solo nodo y salían como un objeto de tres cabezas.
- **El árbol se reinicia cada frame**, así que un id de un frame viejo no significa nada. Todo
  lo que quiera usar el árbol tiene que hacerlo *mientras sigue vigente* — por eso el
  agrupamiento se calcula en el hilo del replay (`CallGroups`) y viaja adentro del snapshot: el
  visor dibuja cuando puede, y para cuando mira, los ids ya son de otro frame.

---

## 6. Sprites compuestos: la hipótesis y el algoritmo

### La hipótesis

Cuando un juego muestra un objeto armado de varias piezas, **hay un lugar del código donde se
decide dibujarlo**, y de ahí para abajo se construye todo. Sería raro que un programador
dibujara media cosa en un momento y la otra media en otro, desde lugares distintos. Entonces:
**el objeto es lo que cuelga de una llamada.**

Eso reemplaza a todo lo que habíamos probado antes, que miraba la PANTALLA: adyacencia, color,
orden de escritura, ventana de tiempo. Los cuatro se midieron y los cuatro juntan o parten el
par equivocado, porque la pantalla no dice dónde termina un objeto — una cápsula con el jugador
parado adelante es un parche conexo dibujado en un frame, y no hay un píxel que diga otra cosa.

### El algoritmo (`SpriteComposites`)

1. **Agrupar por nodo**: cada byte pintado en el frame cuelga de su invocación y de todos sus
   ancestros.
2. **Trepar hasta el objeto**: el objeto es la llamada **más alta que todavía parece una cosa**
   — caja dentro de un tope (8 celdas x 48 filas) y densidad mínima (20% de su propia caja
   pintada). Un nivel fijo no puede funcionar: uno arriba del píxel es el objeto para un misil y
   la sala entera para la rutina que repinta la pantalla (medido: niveles fijos daban "objetos"
   de 128x96 px, que es media pantalla).
3. **Frenar la trepada en los bucles**: si un nodo llama **a la misma rutina** dos o más veces,
   no es un objeto — cada invocación suya lo es. Es la diferencia entre
   `for each sprite: drawSprite` y `drawPlayer: drawHead; drawTorso; drawLegs`, y está escrita
   en el árbol.
4. **Partir en dibujos, por PÍXEL**: lo que pintó una llamada se parte en piezas conexas con 2
   px de holgura (una máscara deja huecos). Por byte no funciona y fue la causa de media hoja
   sucia: un byte toca su columna vecina haya o no píxeles cerca del borde, así que dos cosas
   separadas por **ocho píxeles** salían como un dibujo.
5. **El fondo nunca se une al sprite que tiene encima**: en un motor de dirty-regions la rutina
   que dibuja un sprite primero repinta el pedazo de decorado que ensució, así que un nodo
   legítimamente tiene los dos. La taint ya los distingue (un gráfico de sprite no es un tile).
6. **Crecer entre frames, solo para rebanadas** (opcional, `discover.objects.grow`): un motor
   que repinta una cosa grande y estática por mitades nunca la pinta entera en un frame. Se une
   una caja con otra reciente **solo si** se tocan sin solaparse casi (un sprite que camina da
   casi la misma caja cada frame, y unir esas fue cómo el astronauta se llevaba puesta cada
   plataforma que tocaba) **y** la unión sigue siendo un dibujo conexo. Apagado, cada objeto es
   exactamente lo que pintó una llamada.
7. **Identidad**: el objeto es *qué gráficos lo componen, a qué tamaño*. Hashear los píxeles
   hacía que cada offset sub-byte fuera una entrada distinta y el ranking se llenaba de veinte
   copias de la misma tetera.
8. **Una entrada por objeto**, en dos pasos:
   - entradas cuyos gráficos son mayormente los mismos (70% del conjunto más chico);
   - **copias pre-shifteadas**: un juego que no puede rotar píxeles guarda una copia del sprite
     por offset de X, una atrás de la otra en memoria, y cada copia es un conjunto de
     direcciones *distinto*. El astronauta de Exolon vive en `$efe3`, `$f043`, `$f0a3`… y se
     llevaba doce entradas de la hoja hasta que se juntaron por banco.
9. **La cara del objeto** es el avistaje más lleno, no el más frecuente: en un motor de
   dirty-regions lo más frecuente es la rebanada de dos bytes que se repinta todos los frames, y
   la hoja sale llena de migas.

### Lo que este algoritmo NO resuelve

Cuando **una sola llamada dibuja dos cosas que se tocan** (el personaje agarrado de una escalera
que la misma rutina repinta), no hay nada en ese frame que las separe. Quedan como una entrada y
se borran a mano en el editor. Separarlas necesitaría identidad a través del tiempo —una se
mueve respecto de la otra—, que es trabajo pendiente.

Medición en Exolon: 225 entradas sucias → **28 definiciones**, casi todas una figura sola
(astronauta, torre, cohete, cañón, planetas, aro de teleporte, cápsulas, explosiones, cajas).

---

## 7. Qué se guarda, y en qué formato

| archivo | qué es | quién lo escribe / lee |
|---|---|---|
| `analysis/<juego>-taint.db` | tabla `sprites_found`: rangos de memoria que son gráficos | `TaintDiscover` / `SpriteCatalog` |
| `doc/catalogo-<juego>.md` | el catálogo legible **que además es la fuente** | `SpriteReport` / `SpriteCatalog` |
| `doc/objetos-auto-<juego>.png` | la hoja de objetos compuestos | `TaintDiscover` / `JSW3D`, editor |
| `doc/objetos-<juego>.png` | la misma hoja, marcada a mano | editor (Ctrl+E) |
| `minizx3d/src/main/resources/games.json` | **toda** la config: perfiles, presets, objetos | `GameProfile` |

**La hoja de objetos es un PNG que además es su definición** (`ObjectSheet`): en los tres bits
bajos de R/G/B de cada píxel va el índice (9 bits) del gráfico que hay detrás, y la fila 0 lleva
la leyenda que traduce índice → dirección. Se ve como una imagen normal —se puede mirar, cortar
y pegar—, y al leerla se recupera, píxel por píxel, de qué gráfico salió. Así el trabajo a mano
y el automático producen **el mismo artefacto**, y el editor puede abrir lo que generó la pasada
offline, corregirlo y quedarse solo con lo que sirve.

`games.json` guarda las definiciones automáticas en `objetosAuto`, separadas de las marcadas a
mano (`objetos`): un re-catalogado pisa las primeras y jamás las segundas. Al cargar entran
primero las de mano y las automáticas saltean cualquiera cuyo conjunto de gráficos ya esté.

---

## 8. En tiempo de ejecución

Cada frame el replay publica un `FrameSnapshot` inmutable: `pixels`, `attrs`, `owner` (base del
sprite por byte), `tile` (hoja del tile), `spriteBits` (máscara por bit) y `group` (a qué dibujo
pertenece cada byte según el árbol de llamadas). El visor lo consume así:

- **`updateBackdrop`** — el fondo plano, lo que nadie más reclama.
- **`updateTiles` / relieve de pantalla** (`tiles=screen`) — cada celda recibe un **rol**:
  losa (`T`), decorado flotante (`d`), móvil (`D`), fantasma (`G`), sobra (`f`), solo-sprite
  (`X`), plano (`F`), ítem (`I`). Los roles son el corazón del relieve y cada uno se ganó una
  regla y una trampa medida.
- **`updateSprites`** — inunda por adyacencia los bytes que la taint da por sprite, pero exige
  además que sean **el mismo dibujo** (`group`, del árbol de llamadas). Los bytes de los que el
  árbol no dice nada —no se repintaron ese frame— conservan el comportamiento viejo, para que un
  sprite quieto no se desarme.
- **`updateObjects`** — busca las definiciones de la hoja en la pantalla. La definición es un
  **conjunto de gráficos**, así que buscar el objeto es buscar esos gráficos: no hay template
  matching ni posiciones. Sobrevive a que el objeto se mueva, se anime o aparezca en otra sala.
  - **Difuso a propósito** (`render.objects.match`, la mitad por default): un objeto está
    rutinariamente medio tapado.
  - **Acotado por tamaño**: los gráficos se comparten (la textura de roca es del planeta y de
    toda la banda de terreno), así que una definición sin tope encadena celdas hasta comerse la
    sala.
  - **Por región y no por byte**: primero se aísla el parche conexo, después se pregunta qué
    definición lo explica mejor.

---

## 9. De 2D a 3D

`Sprite3DPipeline` convierte un bitmap de pantalla en una malla. Las técnicas:

| técnica | qué hace | para qué sirve |
|---|---|---|
| `BILLBOARD` | quad que mira a la cámara | HUD, fallback barato |
| `SLAB` | silueta extruida a profundidad fija | arquitectura, plataformas |
| `VOXELS` | un cubo por píxel encendido | look de bloques, ítems |
| `PRIMITIVE` | perfil de profundidad desde una superficie (esfera, ovoide, cilindro, cono, almohada) | planetas, cápsulas |
| `INFLATE` | profundidad desde la transformada de distancia de la silueta | se adapta a cualquier forma |
| `SURFACE_NETS` | isosuperficie sobre un campo de ocupación difuminado | piel suave, personajes |

- **Selección automática por reglas** (`TechniqueSelector`, reglas en JSON sobre *features* del
  bitmap: tamaño, densidad, simetría, agujeros…), con **override por sprite** que le gana a las
  reglas, y con la config del objeto **forzada** (`modelForced`) cuando alguien la eligió a mano:
  el selector automático no tiene voto sobre un objeto que una persona nombró y configuró.
- **Cache de mallas** por (hash del bitmap, hash de la config): un juego repite sus dibujos, y
  sin cache el visor rehace todo cada frame.
- Extras: `fillHoles`, profundidad exacta en voxels (`maxDepth`), losas sobre el objeto entero
  (`TileSlabBuilder` con la cara de PAPER), suavizado, efectos ambientales.

---

## 10. El editor (Ctrl+E)

Con el juego corriendo: se recortan rectángulos sobre la pantalla, se agrupan en objetos, se ve
la lista de lo encontrado con la imagen de cada uno (no una lista de nombres: lo que hace falta
saber de un vistazo es si el objeto 3 es la cápsula o la cápsula más medio cohete), se elige
técnica/primitiva/redondez por objeto, y se graba en la hoja PNG + `games.json`. Al abrirlo de
nuevo se relee todo y se sigue donde quedó.

El lazo con lo automático: **la pasada offline escribe exactamente lo que escribiría el editor**,
así que el flujo es "generar automático → abrir el editor → borrar lo que salió mal → ajustar el
render de lo que importa".

---

## 11. Trampas medidas (las que más costaron)

- **Verificar 3D con md5 de capturas no sirve**: el render no es determinista. Se verifica
  mirando, o con diagnósticos headless que cuenten eventos.
- **La ventana de tiempo no identifica un sprite**: se probó y falla en los dos sentidos.
- **El bit BRIGHT no es color**: incluirlo en el corte de tinta partía planetas por la mitad.
- **Un lazo de realimentación**: el personaje aplanado se cacheaba como decorado y ya no volvía
  a levantarse. Cualquier cache de "esto es fondo" tiene que saltear lo que está retenido.
- **Escrituras rotas de config**: `games.json` se escribió una vez con un `,,`. Ahora se valida,
  se hace `.bak` y se mueve atómicamente.
- **La ventana del emulador roba las teclas** cuando se corre en el display del usuario: varias
  verificaciones headless salieron corrompidas por eso.

---

## 12. Cómo se corre

```bash
# 1) catálogo + hoja de objetos automáticos (una sola pasada, ~15-30 s)
mvn -o -q -pl minizx3d exec:java \
  -Dexec.mainClass=com.fpetrola.z80.minizx3d.TaintDiscover \
  -Dexec.args="<rzx> analysis/<juego>-taint.db 1500" -Dgame=<juego>

#    perillas: -Ddiscover.objects.frames / .sample / .from   (cobertura y densidad)
#              -Ddiscover.objects.grow=false                 (solo lo que pintó la llamada)
#              -Ddiscover.objects.minPiezas / .minVeces      (filtros de escritura)
#              -Ddiscover.gate=8 -Ddiscover.drift=0.25       (motores de dirty-regions)
#              -Dobjects.dump=<frame>                        (el árbol de ese frame, nodo a nodo)

# 2) el visor
mvn -o -q -pl minizx3d exec:java \
  -Dexec.mainClass=com.fpetrola.z80.minizx3d.JSW3D -Dexec.args="<rzx>" -Dgame=<juego>

#    -Dlog=true -Dobjects.log=true    qué objeto matcheó dónde, y por qué se descartó un parche
#    -Dshot=<png> -Dshot.frame=<n>    captura para verificar
```

Config: **un solo archivo**, `minizx3d/src/main/resources/games.json`, anidado (global y por
juego), con menú TAB en vivo.

---

## 13. Qué queda abierto

- Separar dos cosas que **una misma llamada** dibuja pegadas: hace falta identidad a través del
  tiempo (una se mueve respecto de la otra).
- Los nombres automáticos son `auto1..autoN`; ponerles nombre es trabajo a mano.
- El `grow` entre frames está para motores que repintan por rebanadas, pero en Exolon medimos
  que no compra nada: falta un juego donde se justifique, o se saca.
- Aplicar el mismo agrupamiento por llamadas al **relieve** (hoy usa adyacencia + roles).

---

### Índice de clases

| clase | qué hace |
|---|---|
| `OriginTaint` | de qué direcciones está hecho cada byte, con nodos internados |
| `TaintReplay` | corre el RZX, mantiene la taint, el árbol de llamadas y publica snapshots |
| `TaintDiscover` | pasada offline: qué bytes son gráficos, sprite vs fondo, escribe catálogo y hoja |
| `SpriteCatalog` | lee el catálogo (`.md` o `.db`) y contesta "¿esta dirección es sprite?" |
| `SpriteComposites` | los objetos compuestos, agrupados por el árbol de llamadas |
| `CallGroups` | el mismo agrupamiento, calculado en vivo para el snapshot |
| `CallTreeProbe` | herramienta de medición del árbol contra objetos marcados a mano |
| `ObjectSheet` | el PNG que además es la definición (direcciones esteganográficas) |
| `SpriteReport` | escribe `catalogo-<juego>.md` y sus imágenes |
| `JSW3D` | el visor: relieve, sprites, objetos, editor, config |
| `Sprite3DPipeline` + builders | bitmap 2D → malla 3D, con cache |
| `TechniqueSelector` | qué técnica le toca a cada sprite, por reglas sobre sus features |
| `GameProfile` | `games.json`: leer, escribir validado, atómico |
