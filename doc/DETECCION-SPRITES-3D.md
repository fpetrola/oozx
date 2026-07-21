# Detección de sprites para el render 3D (minizx3d): estado, límites y plan

> **Propósito**: documento de handoff para retomar en otra sesión el problema de detectar
> correctamente los sprites/gráficos de un juego para renderizarlos en 3D. Explica cómo
> funciona hoy, por qué anda bien con unos juegos y mal con otros, y el plan concreto para
> generalizarlo. Complementa `doc/COMO-FUNCIONA-ANALISIS.md` (análisis) — este se enfoca en
> el camino gráfico → pantalla → 3D del módulo `minizx3d`.

---

## 1. El objetivo

`minizx3d` (clase principal `JSW3D`) reproduce un RZX del juego original en el emulador OOZ80
y, en tiempo real, **lifta los gráficos del plano 2D y los reemplaza por modelos voxel/suaves
en 3D**. Para eso necesita saber, por cada byte de la pantalla, si ese byte pertenece a un
sprite (y a cuál) o al fondo. Los sprites se inflan en 3D; el fondo/tiles se tratan aparte
(losas 3D o backdrop 2D).

El corazón del problema es entonces: **¿qué direcciones de memoria del juego son gráficos, y
cuando un byte llega a la pantalla, de qué gráfico vino?**

---

## 2. Los dos mecanismos que ya existen

### 2.1. `SpriteTracker` (offline) → produce el catálogo `sprites_found`

Vive en `translator/.../analysis/SpriteTracker.java`. Se corre offline con
`AnalysisCLI z80track <rzx>` y genera la tabla `sprites_found(base, last, size, veces,
frame_first, frame_last, methods)` dentro de la db de análisis.

**Cómo razona (clave para entender los fallos)**: NO mira los píxeles/formas en la pantalla.
Espía la rutina de dibujo mientras corre — cada vez que el juego dibuja, anota **qué
direcciones de memoria de gráficos se leyeron durante esa invocación** y en qué posición X de
pantalla. Después infiere: "las direcciones que se leen juntas al dibujar en una posición son
un sprite". El tamaño de un sprite es el `min..max` de las direcciones leídas, y el extent
final se toma como la MODA de los finales observados (para que una fusión rara no contamine).

Razona por **invocación de dibujo** (grano grueso).

### 2.2. `OriginTaint` + `TaintReplay` (runtime) → clasifica cada byte de pantalla

Viven en `minizx3d/.../OriginTaint.java` y `TaintReplay.java`. Corren durante la reproducción
en el visor 3D.

**Cómo razona**: cada byte de memoria y cada registro llevan un *node id* que describe de qué
direcciones ORIGINALES de memoria se construyó su valor. `union(a,b)` combina dos conjuntos de
orígenes cuando el juego combina dos valores (OR/XOR/copia). Cuando un byte llega a la
pantalla, su nodo tiene acumuladas todas las direcciones que intervinieron. Se pregunta:
"¿alguna de esas direcciones cae dentro del catálogo `sprites_found`?" → si sí, ese píxel ES
ese sprite.

Detalles de implementación importantes:
- **Hash-consing**: los union nodes se internan/deduplican por par de operandos (`memo`). Un
  juego se repite, así que el nodo "sprite OR fondo" se crea una vez y después pega en el memo.
  Por eso corre a 50fps, no solo offline. Un medio juego acumula ~150K nodos, no millones.
- **Arrays paralelos** (`mem[0x10000]`, `reg[32]`) indexados por dirección/registro, NO una
  subclase de `WordNumber`. Es más eficiente (evita boxing en el hot path). Existe
  `TraceableWordNumber` pero `OriginTaint` deliberadamente no la usa.
- **`MAX_DEPTH = 64`**: en cadenas largas de compositing, corta el lado "viejo/profundo" y
  conserva el "fresco". Para runtime está bien; para descubrimiento habría que subirlo.
- **`leaves(node, budget)`**: recupera las direcciones origen de cualquier nodo (para debug).

Razona por **píxel** (grano fino).

**Relación entre los dos**: hoy el taint DEPENDE del catálogo del tracker. El tracker dice qué
direcciones son sprites; el taint clasifica cada píxel contra esas direcciones. Si el catálogo
es malo, el taint no puede clasificar bien.

---

## 3. Por qué anda bien con JSW y Manic Miner

Comparten estilo de motor: **pantallas fijas (flip-screen, sin scroll), tiles 8×8 en celdas
fijas, y sprites 16×16 dibujados directo de memoria a pantalla**. La relación gráfico→sprite es
uno-a-uno y contigua: para dibujar a Willy, la rutina lee sus 32 bytes contiguos y los vuelca.
Una invocación = un sprite = un rango chico y contiguo. El tracker lo captura sin ambigüedad, y
el taint clasifica limpio.

MM salió casi gratis por eso (mismo molde). Ver estado en §6.

---

## 4. Los casos difíciles y qué aprendimos

### 4.1. Dynamite Dan — RESUELTO a medias

Problemas y arreglos (todos ya en el código):
- **Dirty-regions**: DD solo redibuja lo que cambió y deja *trails* de taint viejo (sprites que
  ya no están) → confetti de fantasmas. **Fix**: *freshness-gate* — `TaintReplay` registra en
  qué frame se escribió cada byte de pantalla (`lastWrite[]`); un byte cuya taint no se refrescó
  en N frames no se clasifica. `-Dfresh.frames=N`, **default 0 (off)** porque rompe JSW/MM (sus
  sprites animan más lento que cada frame y parpadearían). Solo el perfil de DD lo activa (=10).
- **Copias pre-shifteadas / gráficos relocados**: Dan se descomprime en runtime a slots
  ($5E3A…) distintos de su fuente comprimida ($633A). El tracker cataloga la fuente, no la
  copia. **Fix**: hallar los slots con `-Ddebug.mem` y agregarlos al catálogo a mano.
- **Ancho variable**: DD tiene sprites de 1..3 bytes de ancho (16/24 px). El header de sprite
  del juego (1 byte: bits 0-1 ancho, bits 2-7 filas/8) lo dice. **Fix**: columna `methods`
  con `"w=N"` → `SpriteCatalog.strideOf` → builders con stride.
- **Tiles de fondo multi-columna (UDGs)**: header 2 bytes, data en orden INVERSO si >1 bloque.
  **Fix**: `SpriteCatalog.tileStride()` (stride negativo para bottom-up) +
  `TileSlabBuilder.build(leaf, mem, depth, stride)`.
- **Escenario que flashea** (lámparas) dispara falsos "items". **Fix**: `-Ditems=false`.

Fuente autoritativa usada: el disassembly hecho a mano de Ritchie Swann,
https://ritchie333.github.io/dan/ — headers de sprites, tabla de UDGs, buffers y rutinas.

Estado: Dan, guardianes y plataformas en 3D reconocible. Pendiente: sprites con máscara
(punteados), pilares/cúpulas flojos. Detalle completo en la memoria del proyecto.

### 4.2. Exolon — catálogo RESUELTO, clasificación a medias (ver §5.1 para el estado nuevo)

> Lo que sigue es el diagnóstico original con el catálogo del *tracker*. El taint-discovery
> (§5 paso 1, ya implementado) lo resolvió: medido sobre los mismos frames, la cobertura de
> bytes clasificados pasó de **3% a 51%** y de **1,0 a 12,5 gráficos distintos por frame**.
> Lo que queda abierto es separar sprite de fondo, no separar los gráficos entre sí.


Exolon ES flip-screen (no scroll), pero aun así el catálogo salió mal: 1 sprite chico + 12
tiles, y los gráficos reales agrupados en **bloques gigantes de ~25.000 bytes**. Resultado en
el visor: casi todo 2D plano, solo algunas plataformas en 3D, sprites no separados.

**Por qué** (el corazón de este documento):

Exolon **compone sprites grandes a partir de piezas más chicas** dispersas en memoria, y dibuja
con **máscara** (AND-máscara / OR-sprite). El tracker, que razona por invocación con `min..max`,
al dibujar un objeto compuesto lee la pieza A ($76cc), la B ($8000), la C ($9500)… y concluye
"un sprite de $76cc a $9500" = 25.000 bytes, huecos incluidos. Peor: si el mismo código de
dibujo se reutiliza para todos los objetos y las piezas se comparten, el span colapsa **casi
toda la biblioteca de gráficos en un solo bloque**. No tiene cómo cortar.

En cambio, el **taint separa las piezas por píxel**: el píxel que vino de A "sabe" que su origen
es $76cc; el de al lado que vino de B "sabe" que es $8000 — sin importar que se dibujaran en la
misma llamada, compartieran código o pasaran por un buffer con máscara.

**Lección general de las 4 corridas**: lo que importa NO es el scroll, es **cómo dibuja el
juego**. "Flip-screen como JSW" es necesario pero no suficiente.

| Juego | Motor de dibujo | Resultado |
|---|---|---|
| JSW / Manic Miner | sprites 16×16 directo, tiles 8×8, celdas fijas | limpio, casi todo 3D |
| Dynamite Dan | dirty-regions + copias pre-shifteadas + ancho variable | ok tras arreglos + disassembly |
| Exolon | composición de piezas + buffers grandes + máscara | mayormente 2D, sprites no separados |

Cada paso que un juego se aleja del molde JSW (dirty-regions → máscaras → composición) es
trabajo real y no garantizado.

---

## 5. El plan para generalizar

La raíz del problema con Exolon (y en parte DD) es que **el catálogo lo produce el tracker, que
razona por invocación y fusiona**. El taint razona por píxel y NO fusiona. La idea es **dar
vuelta la dependencia**: usar el taint mismo para DESCUBRIR qué direcciones son gráficos, en vez
de que el tracker lo infiera del código de dibujo. Esto es genérico: no depende de cómo se
dibuja.

Orden recomendado (de mayor a menor retorno, menor a mayor riesgo):

### 5.1. Paso 1 y 2 — HECHOS. Estado y lo que falta

`TaintDiscover` (`minizx3d/.../TaintDiscover.java`) implementa el paso 1: corre el RZX con un
taint vacío, y por cada **blob conexo** de bytes encendidos parte el conjunto de hojas en
**piezas contiguas** (corte por hueco de direcciones), una observación por pieza por frame.
Emite `sprites_found` con la misma forma que el tracker, así que `SpriteCatalog` lo carga sin
cambios. Se corre solo: `TaintDiscover <rzx> <db> [maxFrames]`.

**Resultado en Exolon** (30.000 frames, `-Ddiscover.rows=20 -Ddiscover.gate=8
-Ddiscover.drift=0.25` → `analysis/exolon-taint.db`): 240 entradas de 4..255 bytes donde el
tracker daba bloques de ~25.000. El banco de sprites cae en fronteras alineadas a 16
(`$edc0, $edd0, $edf0, $ee00, $ee10, $ee40`), que es la señal de que está encontrando
límites reales y no spans arbitrarios. Medido con el mismo RZX y rango de frames:

| catálogo | cobertura de bytes encendidos | gráficos distintos por frame |
|---|---|---|
| `exolon.db` (tracker) | 3% | 1,0 |
| `exolon-taint.db` (taint) | 51% | 12,5 |

**La granularidad quedó resuelta; lo que falla ahora es sprite vs fondo.** En el visor 3D se
ve geometría real (antes era casi todo plano), pero se **infla de más**: la banda de terreno
y el texto del marcador salen como sprites.

**Por qué** — y esto es lo importante para retomar. Exolon reescribe solo el **2-10% de los
bytes encendidos por frame** (medido): es un motor de dirty-regions duro. Los rastros que
deja encendidos conservan el taint viejo del sprite que ya se fue, y eso **invierte todos los
discriminadores a la vez**: el gráfico se ve viejo (`fresh`), se ve como que nunca se mueve
(`drift`) y se ve desparramado (`mob`). Por eso `-Ddiscover.gate=N` descarta los bytes no
reescritos: cada métrica vuelve a medir lo que el juego pinta AHORA.

Pero el gate tiene un costo que es la causa del over-inflate: **fragmenta** la banda de
terreno (que es un solo blob grande) en astillas de lo poco que se repintó ese frame. Y
`reuse` y `stamps` — los dos discriminadores diseñados justamente para cazar tiles estampados
— se miden DENTRO del blob. Fragmentado el blob, quedan ciegos. Caso concreto: `$edc0` (16
bytes, la textura punteada de roca) es la entrada más observada del juego y sale `sprite` con
`reuse=0,9 stamps=3`; sin gate, esa misma banda sería un blob con `reuse≈50`. Se verificó que
NO es un buffer: tiene 10 escrituras en 30.000 frames (carga de nivel), es gráfico estático de
verdad.

**HECHO — vista partida**: `TaintDiscover.onFrame` hace ahora DOS barridos por frame
muestreado (`scan(..., gated)`). El **sin gatear** alimenta las métricas de cobertura
(`reuse`, `stamps`, `bpo`, `mob`, `fresh`), porque un blob entero es la única forma de ver
"cuánta área cubre este gráfico a la vez"; el **gateado** alimenta solo `drift`, porque los
rastros encendidos son lo único que le impide ver el movimiento. `-Ddiscover.gate` aplica
únicamente al segundo, y el segundo ni corre si `drift` está apagado.

Funcionó sobre el caso que lo motivó: **`$edc0` (la textura de roca) pasó de `sprite` a
`FONDO`**, y lo cazó `fresh` (26% < 35%) — justo la métrica que los rastros invertían, que
sobre la vista sin gatear volvió a ser válida. El catálogo pasó de 98 sprites + 142 fondo a
**56 sprites + 150 fondo**.

**Pero el resultado en pantalla obligó a resolver el fondo aparte.** Al dejar de ser sprites,
esas zonas pasaron a ser zonas-tile y `updateTiles` las convirtió en losas: quedó un ruido
peor que antes. La causa es concreta: `updateTiles` extruye cada celda desde su **template
de tile en memoria**, y eso exige que el catálogo haya encontrado bitmaps de tile reales de 8
filas. Exolon no tiene: su decorado es una textura punteada, así que leer 8 bytes en la hoja
descubierta da un bitmap arbitrario que no se parece a lo que hay en pantalla.

Primero se probó **`-Dtiles=false`** (fondo plano en el backdrop 2D). Correcto pero **inútil
como imagen**: el juego quedaba casi todo plano, que es de donde habíamos partido — apagar el
3D no es arreglarlo.

La solución es **`-Dtiles=screen`** (`JSW3D.updateScreenRelief`, activado en el perfil de
Exolon): el fondo se construye **desde los píxeles de la pantalla**, una celda 8×8 por vez,
en vez del template de memoria. Es el mismo atajo que ya usan los sprites compuestos (§5,
atajo de render) y no necesita que el template sea válido, porque la pantalla ya tiene la
imagen compuesta. Cacheado por hash de contenido, así que una banda de roca punteada colapsa
a un puñado de modelos. Los bytes del playfield se sacan del backdrop 2D, si no se ve doble.

**Y tiene que ser una LOSA, no un modelo de sprite** (`pixSlab` → `TileSlabBuilder`, no
`pixModel`). Se probó primero con los builders de sprite y el fondo salía como una lámina
inflada tipo globo, finita: no se estiraba hacia el fondo como una plataforma, y —el mismo
síntoma visto de otra forma— los sprites móviles no se leían "en el medio" de ninguna
profundidad, porque no había espesor donde estarlo. Los personajes se colocan en `midZ()`,
que es **la mitad de `slabDepth()`**: es el espesor de la losa el que les da un adentro donde
pararse. Con `relief.depth=1` el fondo usa la misma profundidad que los tiles de JSW y las
entidades quedan embebidas en la escena igual que Willy entre sus plataformas.

**Cuidado con el cache** (costó una corrida muerta): los modelos por píxeles de pantalla los
comparten los blobs de sprite y el relieve, y la expulsión estaba adentro de `updateSprites`
con tope 512. Las ~640 celdas por frame del relieve lo pasaban siempre, así que se destruía y
reconstruía el juego de modelos entero **cada frame** y el visor se arrastraba (ni llegaba al
frame pedido). Ahora se expulsa una vez por frame, antes de que nada reconstruya instancias,
con tope 4096.

Resultado verificado: estructuras y terreno extruidos hacia el fondo con caras laterales
visibles, entidades en 3D en el medio de ese espesor, y **el personaje como una sola figura
sin estela**. JSW/MM siguen en `tiles=slab` (default) y quedaron sin cambios.

**Lo que flota se infla por OBJETO, no por celda** (`JSW3D.floatBlobs`, salió de Monty: "los
ítems tienen que tener volumen y no tratarlos como tile"). En modo `screen` hay dos destinos:
losa (arquitectura) o flotar a media profundidad (ítem, cosa que se mueve, decorado). Lo que
flota se inflaba **una celda 8×8 por vez**, así que un ítem de 16×16 salía como cuatro bolitas
en anillo y una repisa como una fila de almohadones: adentro de una celda la transformada de
distancia no ve más de 8 píxeles en ninguna dirección, o sea la silueta que hace falta para
leer un volumen no está ahí. Ahora las celdas flotantes contiguas se juntan en un blob y
reciben **un solo modelo**, igual que un sprite compuesto en modo `adjacent`.

Tres reglas que costaron una corrida cada una:

- **El blob corta donde cambia el COLOR.** Un guardián magenta parado sobre una repisa cian
  está 8-conexo con ella; fusionarlos daba un modelo con el voto de tinta mayoritario y la
  repisa salía magenta con un bulto encima. Mismo ink, mismo objeto.
- **Se rellena la silueta hueca** (`SpriteFx.fillHoles`, `-Drelief.fill=false` para apagarlo):
  estos juegos dibujan un objeto sólido como su CONTORNO, y inflar el contorno literal da una
  rosquilla — el ítem de Monty salía como un aro. Lo que un flood desde el borde no alcanza es
  el adentro del objeto.
- **La tinta sobrante en las celdas del personaje sigue yendo celda por celda.** Es lo que su
  máscara no reclamó (casi todo, píxeles suyos que la taint no marcó); fusionarla y rellenarla
  le construía una burbuja lisa alrededor que tapaba el modelo real del sprite.

**Una celda reescrita CON LO MISMO QUE YA TENÍA es decorado, no algo que se mueve** (la causa
principal de "cuando un sprite se acerca, los tiles de la plataforma desaparecen", medida con
`-Drelief.audit`). El motor borra y repinta la plataforma que el personaje acaba de tapar, así
que sus celdas quedan *frescas* mientras él pasa y la regla de `relief.dyn` las mandaba a
flotar: la losa profunda se cambiaba por un bulto fino a media profundidad, o sea un mordisco
que corría junto al personaje. Con `relief.dyn=17` y `tile.depth=17` (config real de un
usuario) es imposible no verlo. Ahora se compara el bitmap de la celda contra `cellBmp` (los
últimos píxeles limpios): si son **exactamente** los mismos, no se movió nada y sigue siendo
losa. Medido en Monty: hasta 17 celdas por frame salvadas, en 411 de ~950 frames.

**Un planeta no es una pared: la arquitectura LLEGA AL PISO.** El corte decorado/arquitectura
era solo el TAMAÑO de la componente conexa, y los planetas grandes de Exolon (16-18 celdas) caen
del lado de la arquitectura con cualquier umbral que no se trague también un muro: salían
extruidos hasta el fondo en vez de volumétricos. Lo que los separa de verdad es que la
arquitectura está anclada —la banda de terreno, un pilar, un muro llegan a la fila de abajo—
mientras el planeta cuelga en el cielo, y además es compacto donde una repisa es una barra fina.
`JSW3D.island`: no toca el piso + bbox ≤ `relief.island` (8 celdas) + relación de lados ≤ 2,5.
**Sin test de llenado**: se probó exigir una masa sólida (≥ 0,5 del bbox) y tiraba a losa el
cielo disperso —una nube de 5 celdas en un bbox de 4x4 llena 0,31— que salía extruido hasta el
fondo como peinetas ("puntitos que van del frente hasta el fondo y parecen plataforma").
**Tocar el borde de PANTALLA no es anclaje**: los planetas de Exolon aparecen
recortados por el borde izquierdo, y probar contra cualquier borde dejaba media esfera
volumétrica y la mitad recortada extruida como un rayo.

**Un gráfico se puede declarar FONDO a mano, por su hoja** (`relief.flat`, y las teclas **I/W**
en el visor). Es la salida para lo que ninguna regla de forma acierta: las motas de estrella de
Exolon son 8x8, están quietas y están encendidas, o sea que para cualquier test geométrico son
un pedazo de decorado, y extruidas se leen como plaquitas colgadas en el cielo. Nombrar el
gráfico es más simple y más honesto que una regla que finge inferirlo.

Cómo se usa, sin saber direcciones de antemano: **I** cicla por los gráficos que el relieve está
modelando en pantalla (Shift+I para atrás), pinta de BLANCO las celdas del elegido —así se ve
cuál es— e imprime su hoja y cuántas celdas cubre; **W** lo pasa a plano (o lo devuelve a
modelado) y lo guarda en `~/.jsw3d-relief-<juego>.json`. También se puede sembrar desde
games.json o por `-D` con una lista separada por comas: `render.relief.flat: "$ede0,$ede8"`.
Una celda de un gráfico marcado no recibe modelo NI fantasma: la pinta el backdrop 2D.

**Una mota del cielo no se modela: se deja plana** (`relief.dot`, 0 = apagado; Exolon usa 2).
Toda regla acá decide losa contra prop, y para una estrella de una celda las dos respuestas son
malas: modelada como prop queda una bolita flotando adelante del backdrop, y un cielo lleno de
esas se lee como mugre en el vidrio ("puntitos de 8x8, de distintos colores, que aparecen de
golpe y van del frente al fondo"). Una isla estática de hasta `relief.dot` celdas que no llega
al piso se deja **sin modelar**: la pinta el backdrop 2D, que es donde va una estrella. Y la
misma idea para el arte que **no es un gráfico**, `relief.dotpx` (0 = apagado; Exolon usa 2):
una celda que **ninguna hoja reclama** (`tile`=0) y que tiene hasta N píxeles encendidos es
fondo. Las estrellas de Exolon no son un sprite ni un tile: una rutina prende píxeles sueltos,
así que la taint no tiene dirección para señalar, no hay nada que nombrar en `relief.flat`, y
tampoco sirve medir islas —la conectividad de 8 vecinos pega la dispersión en componentes de
47 a 112 celdas, que por tamaño y bbox parecen arquitectura. Medido: 10.446 celdas de cielo
salían losa sin hoja, **8.365 de ellas con exactamente 1 píxel encendido**, mientras que las
celdas de TERRENO sin clasificar traen 30-46. Un píxel y sin origen es una mota de cielo; con
el umbral en 2 quedaron 0.

**Un fantasma de casi nada no es una pared.** El fantasma existe para que un personaje no
abra un agujero en una plataforma, pero un caché de uno o dos píxeles es una chispa o una
estrella, y reconstruirlo como losa dejaba un bloque de 8x8 corriendo toda la profundidad hasta
el fondo. Aparecen **al lado de otros sprites** justamente porque ahí es cuando la taint reclama
la celda y el camino del fantasma se hace cargo — el usuario lo diagnosticó así y la medición lo
confirmó: 94 de 300 fantasmas losa de Exolon eran celdas de 1-2 píxeles. Con `relief.dotpx` en 2
quedaron 0, y los 97 fantasmas que sobreviven son paredes de verdad.

**Un fantasma solo es una losa si la celda lo era.** El fantasma reconstruye desde el caché el
decorado que un sprite está tapando, y lo hacía SIEMPRE como losa: cada vez que algo cruzaba
una estrella o una nube aparecía de la nada un bloque de 8x8 corriendo toda la profundidad de la
losa hacia el fondo. Ahora vuelve como lo que era (`sceneryRole`): un prop flota, y una celda
que estaba plana sigue plana.

**Las tiras finas del cielo son una perilla, no un algoritmo** (`relief.bar`, medido: barras de
8x1, 9x1, 6x1 celdas que caían a losa). Son chicas como un adorno pero alargadas como una
repisa, así que `island` las rechaza y terminaban extruidas: una fila de peinetas cruzando el
cielo, "puntitos que van del frente hasta el fondo y parecen plataforma". No se puede decidir
por la forma, porque **es la misma forma en los dos casos**: en Exolon son destello de estrellas
y estelas (fondo), en Monty on the Run es la repisa donde se para. Por eso es una opción por
juego: `slab` (como siempre, default), `flat` (no se modela: la pinta el backdrop 2D, que es lo
que hace el perfil de Exolon) o `float` (volumétrica como un adorno).

**Una entidad que se detuvo sigue siendo una entidad.** Un motor de dirty-regions no repinta lo
que no se mueve, así que el personaje quieto pierde la taint de sprite (`fresh.frames`) *y* la
frescura de escritura, se une como componente estática al terreno que pisa y lo extruyen contra
la pared: "hay frames en que deja de ser volumétrico". Lo que lo retiene NO es una ventana de
tiempo —medido, se aplana entre 40 y 110 frames después de que se suelta la propiedad, y una
ventana tan larga congela también lo que el catálogo reclama de más— sino la POSE: se guarda el
bitmap de la celda en el frame en que la taint la soltó, y mientras siga mostrando exactamente
eso (y no coincida con el decorado limpio cacheado) sigue flotando, sin límite de tiempo. Si el
personaje se va, el motor repinta el fondo, la pose deja de coincidir y la celda vuelve a ser
losa al instante: no deja estela. Lo que acota el riesgo es el TAMAÑO: una isla retenida de más
de `relief.hold` celdas (12) es decorado mal catalogado y se asienta igual (un personaje de
16x16 son 4 celdas). Y hubo que cortar un lazo: mientras una celda está retenida **no se cachea
como decorado** — el frame en que el personaje se aplanaba, sus propios píxeles pasaban a ser el
"limpio" de la celda y desde ahí todo lo daba por parte de la sala.

**Un cambio de pantalla no es movimiento.** La frescura de escritura solo significa "esto se
mueve" cuando es LOCAL: cuando el juego repinta la pantalla entera, todas las celdas quedan
frescas, la sala pasaba la ventana de movimiento entera como bultos flotantes y la profundidad
llegaba visiblemente tarde ("tarda unos milisegundos en activarse"). Si más del 40% de lo
encendido acaba de reescribirse, el frame se clasifica como si todo fuera estático y las losas
aparecen de una.

**El grafo de componentes es "dónde hay decorado", no "qué está quieto"** — y esa es la causa del
parpadeo de las puntas de las plataformas. Dejar afuera las celdas móviles hacía que la
pertenencia dependiera de lo que pasara caminando: un sprite cruzando una plataforma la parte en
dos, el pedazo sobrante cae bajo el umbral de decorado, y sus celdas se cambiaban de losa a
bulto flotante y de vuelta **con los píxeles intactos** (medido: 853 flips `T<->d`). Ahora una
celda entra al grafo si tiene píxeles limpios cacheados o tinta que no es algo que se mueve: el
caché puentea por debajo del personaje y el misil que pasa cerca de un planeta no se le pega.
Encima, **disparador de Schmitt** en el umbral: una celda que ya flota necesita que su componente
llegue al DOBLE del límite para pasar a arquitectura. Medido en Exolon: 853 → 286 flips, y los
que quedan son nubes de humo pasajeras.

**Blobs de sprite demasiado chicos**: `updateSprites` ignora los blobs de menos de 4 bytes (una
mota no es un personaje) pero `updateBackdrop` ya les había borrado los píxeles → hueco negro.
Es el mismo caso que los blobs gigantes, del otro extremo, así que se arregla en el mismo lugar:
`demoteOversizedBlobs` (modo `adjacent`) ahora también les revoca la propiedad **antes** del
backdrop y quedan planos en 2D. Medido en Monty: 746 eventos en una corrida, ahora 0.

**El volumen de la losa lleva el color del tile, la cara no.** El decorado de Exolon es tinta
punteada sobre papel negro, así que el bloque extruido es casi todo papel y desde cualquier
ángulo la plataforma se leía como una masa negra. Pintar TODO el papel con el color de la tinta
se probó dos veces y se revirtió las dos: la sala se vuelve ladrillos de un solo color y se
pierde el punteado (con tinta blanca y un tinte de 0,35, un pilar quedaba un bloque gris liso).
La cara frontal es donde vive esa textura, y la masa de atrás es donde corresponde el color de
la plataforma: `TileSlabBuilder` ahora emite las columnas de papel en dos partes —una piel fina
al frente (material `paper`, color de papel real) y el resto de la profundidad (`paperSide`)—
y el visor tiñe solo la segunda hacia la tinta, `relief.paper` (0,35 por default, en el menú).

**Nada puede irse de la pantalla sin que algo lo dibuje** (la otra mitad del mismo síntoma). `updateBackdrop` borraba del plano 2D **todo**
byte con dueño o con tile, aunque el relieve no lo hubiera podido dibujar — y hay tres formas de
no poder: el caché de decorado de la celda vacío, la máscara del sprite encima, o un modelo que
salió null. Esas celdas quedaban negras justo donde estaba el personaje. Ahora el backdrop mira
**solo `cellClaimed`**, y lo no reclamado vuelve a 2D menos la tinta propia del sprite (que
`updateSprites` ya modela), que es la misma regla que el modo no-`screen` ya usaba. Además el
decorado chico (`decor`) ahora también cachea sus píxeles limpios en `cellBmp`, así que una
repisa pisada tiene fantasma como cualquier losa.

**Sigue abierto**: quedan bases catalogadas como sprite con bbox de pantalla completa
(`$6400`, `$643e`, `$fbe6` — parecen gráficos de marcador/fuente), que son las que todavía
ensucian; y evaluar un camino intermedio para el fondo de Exolon (losas donde el fondo es
estructura, plano donde es textura).

Otras dos cosas aprendidas, que conviene no volver a descubrir:
- **`drift` aliasea**. Mide cuánto cambia el conjunto de celdas de un frame muestreado al
  siguiente. Una entidad con recorrido cíclico corto puede volver a las mismas celdas en
  exactamente `discover.sample` frames y leerse como estática: en JSW los bitmaps de 32 bytes
  de los guardianes dan `drift=0,00`. Por eso `drift` y `gate` vienen **apagados por default**
  (JSW/MM/DD no cambian en nada) y son el par para motores de dirty-regions.
- **`drift` y `gate` se necesitan mutuamente.** Sin gate, los rastros clavan el conjunto de
  celdas de un sprite que sí se mueve y `drift` colapsa a 0. Con gate, `mob` pierde filo en un
  juego flip-screen (el decorado solo se observa justo después de cambiar de pantalla, en un
  lugar distinto por pantalla, así que acumula mucha movilidad), y `drift` queda como la única
  señal local en el tiempo.

**La estela del personaje en Exolon: por qué NO se arregla con el gate** (medido). El gate de
frescura funciona (con `fresh.frames=8` solo sobreviven bytes escritos hace ≤8 frames), y aun
así el personaje deja estela. La causa está medida: en un frame de juego, el **97-100% de los
bytes con dueño tienen origen MEZCLADO** — todos tienen más de una hoja, y todos combinan el
gráfico que los reclama con algo de afuera. Exolon dibuja con máscara (`fondo AND máscara OR
sprite`), así que la taint de cada byte pintado es la UNIÓN de fondo y sprite, y `spriteOf`
se queda con el sprite. Consecuencia: cuando el personaje se va y el juego pinta fondo encima
con la misma rutina enmascarada, el byte nuevo **sigue conteniendo las direcciones del
sprite** → se le sigue atribuyendo al personaje. Es un byte recién escrito, así que ninguna
ventana de frescura lo distingue del personaje real: por eso no es cuestión de bajar el
umbral. Y hay una tensión de fondo que impide bajarlo igual: en un motor de dirty-regions un
sprite quieto NO se repinta, así que una ventana corta lo haría parpadear (es exactamente la
regresión que rompió JSW cuando el default fue 3). Encima, agrupar por adyacencia **suelda**
la estela al personaje en un solo blob.

Esto es el **paso 3 (taint por bit)**, no una perilla: el taint es por byte y Exolon compone
por bit.

**IMPLEMENTADO** (variante liviana, `-Dsprite.bits=true`, apagado por default; el perfil de
Exolon lo activa): `OriginTaint.bits[0x10000]` + `regBits[32]` llevan, por byte y por
registro, **qué bits vinieron de un bitmap de sprite**. Se propaga en paralelo al flujo de
`srcTaint`/`pendingRead` que ya existía, y la regla no necesita decodificar el opcode:

```
bitsResultado = valorEscrito & (bits de todos los operandos leídos)
```

Enmascarar por el valor es lo que la hace servir para cualquier op: una copia conserva la
máscara, un OR agrega los bits que puso el sprite, un AND/XOR tira los que la máscara apagó,
y **fondo pintado encima de una posición abandonada deja la máscara en 0** — que es justo la
estela que la unión sola no podía soltar. La máscara se siembra en `readBits`: leer un bitmap
del catálogo marca todos sus bits encendidos. En `publish`, un byte con máscara 0 pierde el
dueño; `updateBackdrop` borra **solo los bits del sprite** (antes borraba el byte entero) y el
modelo 3D se infla solo con esos bits. Con la pasada apagada la máscara vale el byte entero,
así que el comportamiento previo queda idéntico — verificado: JSW sale **byte-idéntico**.

**Lo que arregló y lo que no** (medido sobre frames 6000-6400 de Exolon): sprites que se
estiraban colapsan a su ancho real — `$ef40` pasó de 240px de bbox a **16px**. Pero el
artefacto dominante de Exolon NO es la estela sino el catálogo: la máscara se siembra desde
lo que el catálogo llama sprite, así que mientras `$edc0` (terreno) y `$fbe8` (fuente) estén
catalogados como sprites, el terreno se sigue inflando y sus bbox siguen ocupando la pantalla
entera. **La taint por bit no puede ser mejor que el catálogo**: conviene arreglar primero el
clasificador (§5.1, medir `reuse`/`stamps` sin gatear) y ahí esta pasada rinde del todo.

**Tope de malla (`Too many vertices`)**: una malla de libGDX indexa con shorts → 32767
vértices por modelo. Agrupar SOLO por adyacencia (que es lo que hace que un objeto compuesto
salga entero) implica que un byte de fondo mal reclamado encadena una banda entera de pantalla
en un solo blob: el terreno de Exolon es una tirada conexa de cientos de celdas y al inflarla
reventaba. `JSW3D.demoteOversizedBlobs` (solo en modo `adjacent`) le revoca la propiedad a todo
blob cuyo modelo no entre en el presupuesto, **antes** de `updateBackdrop` — ese orden es el
punto: `updateBackdrop` borra todo byte con dueño, así que saltear el blob después dejaría un
agujero negro en vez de dejarlo plano. El presupuesto lo calcula cada builder
(`VoxelSpriteBuilder.vertexCount` = 24 por píxel encendido; `SmoothSpriteBuilder.vertexCount` =
la retícula entera, frente y dorso, independiente de cuántos píxeles estén encendidos), así que
el umbral vive donde se arman las mallas. Es red de seguridad Y mejor imagen: un blob así de
grande es decorado que el catálogo reclamó de más. Verificado en los dos modos.

**Límite práctico**: el `leafMemo` crece con los nodos union (10,4M nodos a 30.000 frames,
3,7 GB de RSS). En una máquina de 7 GB, 30.000 frames es el techo; Exolon tiene 112.474.

El paso 2 (blobs por adyacencia + modelo desde píxeles de pantalla) está en `JSW3D`
detrás de `-Dblobs=adjacent`, activado en el perfil de Exolon.

### Paso 1 — Taint-discovery (formalizar `debug.scan`)

Ya existe un prototipo: `-Ddebug.scan=true` en el main de `TaintReplay` recorre bytes de
pantalla encendidos, mira sus hojas de taint (`OriginTaint.leaves`), y arma un histograma de
"qué direcciones de memoria contribuyen a píxeles". **Con eso encontré las zonas de Dan** que el
tracker no veía. Formalizarlo en una pasada de análisis (`taint-discover <rzx>`) que emita
`sprites_found` directamente. Ajustes necesarios:
- **Subir `MAX_DEPTH`** (hoy 64) a 256–1024 o sin cap en la pasada offline, para no perder hojas
  viejas en cadenas largas de compositing.
- **Filtrar buffers** de los gráficos estáticos: un gráfico estático se LEE mucho y se ESCRIBE
  poco (solo al cargar); un buffer se reescribe cada frame. Reusar `lastWrite[]` (ya existe para
  el freshness-gate) como discriminador.
- **Discriminar sprite vs fondo** con bytes-por-frame (un sprite mueve ~14 B/f, un fondo >75).
  Ya se usó a mano para DD.

Bajo riesgo, alto valor, resuelve la raíz. **Empezar por acá.**

### Paso 2 — Flood-fill por adyacencia, no por base

Hoy `updateSprites` (en `JSW3D`) agrupa píxeles owned en blobs por conectividad **+ misma base**.
Para sprites compuestos de piezas con bases distintas, eso los separa. Cambiar el criterio a
"cualquier píxel-sprite adyacente en pantalla, sin importar la base" agruparía el objeto
compuesto **gratis**, porque el juego ya puso las piezas pegadas. Probable que la mayoría de
Exolon se separe bien acá, sin declarar composición.

### Paso 3 — Taint por bit (para máscaras)

Es el cuello de botella real de Exolon y lo que dejó punteados a los guardianes de DD. Hoy el
taint es por byte: un byte compuesto con máscara (fondo AND máscara OR sprite) mezcla el origen
de fondo y sprite. Trackear la taint a nivel de bit (8 nodos por byte, o un nodo por byte con
máscara de qué bits son sprite) separaría los píxeles de sprite de los de fondo dentro del mismo
byte. Es el cambio más pesado; dejarlo para cuando 1 y 2 estén.

### Paso 4 — Config de composición (último recurso)

Solo para lo que la conectividad (paso 2) no separe bien: **objetos con huecos** (piezas no
adyacentes) u **objetos solapados** (dos enemigos que se tocan, que la conectividad fusionaría).
Una config declarativa "objeto = pieza A en (0,0) + pieza B en (8,0)…" resolvería esos casos,
pero es curación por-juego (como los headers de DD, sale del disassembly si existe). No empezar
por acá: probablemente la conectividad ya resuelva la mayoría.

**Atajo de render**: para el modelo 3D de un sprite compuesto, en vez de reconstruirlo desde
memoria ensamblando piezas, se puede inflar directamente **los píxeles que ya están en la
pantalla** (el juego ya hizo la composición). El taint solo separa sprite/fondo. Evita necesitar
la config de composición para renderizar. Contra: el sprite en pantalla puede estar parcialmente
tapado; el bitmap de memoria es la forma "limpia". Trade-off a evaluar.

---

## 6. Estado actual por juego

| Juego | `-Dgame=` | db | Estado |
|---|---|---|---|
| Jet Set Willy | `jsw` | `analysis/jsw-catalog.db` | ✅ completo |
| Manic Miner | `mm` | `analysis/mm.db` | ✅ completo |
| Dynamite Dan | `dd` | `analysis/dd.db` | 🟡 sprites+tiles ok; máscaras pendientes |
| Exolon | `exolon` | `analysis/exolon-taint.db` | 🟢 catálogo por taint-discovery (30k frames), fondo por relieve de pantalla, sin estela (§5.1). Pendiente: bases de marcador/fuente aún como sprite |
| Monty on the Run | `monty` | `analysis/monty-taint.db` | 🟢 catálogo por taint-discovery (4.889 frames, 5s), fondo por relieve de pantalla. Es el primer juego con el marcador ARRIBA: `playfield.top=2` + `rows=20`. Las piezas del catálogo son tiras de animación enteras: el tamaño se capa por el blob (§ arriba). Ítems y decorado con volumen por blob + relleno de silueta, y sin agujeros negros donde pisa Monty (§5.1). Pendiente: la planta donde se para queda clasificada como sprite, así que se ve por fantasma/sobrante y no como losa |

Perfiles en `minizx3d/src/main/resources/games.json`. Cada uno trae rzx + db + tweaks del juego.

---

## 7. Herramientas de debug disponibles

Todas en el `main` de `TaintReplay` (modo validación headless) salvo la última:

- **`-Ddebug.scan=true`** — histograma de zonas huérfanas (direcciones origen de píxeles
  encendidos sin clasificar) en un rango de frames. Prototipo del taint-discovery.
- **`-Ddebug.mem=addr[,addr...]`** — hojas de taint de bytes de MEMORIA (ver si una copia staged
  se trazó y de dónde viene). Así se hallaron los slots de Dan.
- **`-Ddebug.rect=x0,y0,x1,y1`** — hojas de los bytes ENCENDIDOS sin clasificar en un rectángulo
  de pantalla.
- **`-Dmax.frames=N`** (en `Z80AnalysisRunner`, o sea afecta `z80run`/`z80track`) — analizar solo
  los primeros N frames. Exolon tiene 112.474 frames; con esto se cataloga una porción en
  segundos para probar.
- **`-Dseek=N`** (visor 3D) — reproducir a fondo hasta el frame N para llegar rápido a una
  pantalla.
- **`-Dshot=/path.png -Dshot.frame=N -Dcam.pos=x,y,z`** — captura del framebuffer para chequeo
  visual.
- **`-Drelief.audit=N`** (visor, modo `tiles=screen`) — en el primer frame ≥ N imprime **qué
  decidió el relieve en cada celda** del playfield, como un mapa de 32 columnas: `.` aire, `G`
  celda con sprite reconstruida del caché, `f` sobrante del personaje, `X` celda con sprite que
  no dibujó nada, `D` flotando por reescritura, `d` isla chica (decorado), `T` losa, `I` ítem,
  `?` el modelo salió null. Además lista las celdas con sprite con su edad de escritura y el
  histograma hoja → celdas. Es determinista, al revés que la captura.
- **`-Drelief.holes=true`** (visor) — avisa, frame a frame, de **píxeles encendidos que no
  dibuja nadie**: blobs de sprite descartados (chicos o sin modelo) cuyos bytes el backdrop ya
  borró. Debe dar 0; cualquier línea es un agujero negro en la sala. Informa también cuántas
  celdas salvó por frame la regla de "repintada con lo mismo".
- **`-Drelief.paint=true`** (visor) — pinta cada modelo según **quién lo dibujó**, ignorando los
  colores del juego: rojo = losa (arquitectura), verde = prop flotante (decorado/móvil), amarillo
  = tinta sobrante alrededor de un personaje, azul = fantasma losa, cyan = fantasma de prop,
  magenta = ítem detectado, blanco = modelo de sprite, **gris = el backdrop 2D plano** (donde va
  una estrella). Cualquier otra cosa que conserve su color es un efecto ambiental (basura,
  nieve, globos). El backdrop se pinta gris a propósito: con los colores del juego, un píxel rojo
  del arte era indistinguible de una losa pintada de rojo — me confundió a mí primero. Es la
  forma rápida de contestar "¿qué está dibujando esto?" sin adivinar mirando la escena.
- **`-Drelief.flips=true`** (visor) — lista las celdas que **cambian de rol con los píxeles
  idénticos**, marcando con `*` las que la taint de sprite tuvo hace poco (o sea: el personaje): el mundo no se movió, el render sí. Es la medida del parpadeo (`T<->d` = losa
  contra bulto flotante) y la que sirve para comparar reglas: en Exolon bajó de 853 a 286.
  Ignora `.<->X`, que no dibujan nada en ninguno de los dos estados.
- **`-Dlog=true`** — habilita todos los prints (por default la consola está muda).

### El catálogo legible (y por qué el visor lo carga a él)

`TaintDiscover` escribe además **`doc/catalogo-<juego>.md` y `.png`** (`SpriteReport`), y eso
no es un reporte al costado: **`SpriteCatalog` lee ese .md**, así que lo que corre el visor es
el mismo texto que estás mirando. Un catálogo que vive solo como filas de SQLite es invisible
—nadie se entera cuando una recatalogación cambia el tamaño de un sprite o se come un
personaje— y como texto se diffea en git, se grepea y se corrige a mano.

- **Arriba de todo, los OBJETOS COMPUESTOS: lo que se ve en pantalla.** Una entrada del
  catálogo es un rango de memoria que se leyó junto, que en un motor que compone —Exolon arma
  un personaje con varias piezas compartidas— es un fragmento: medio torso, una franja de una
  pierna. Listar eso contesta "qué bytes son gráficos", no "qué muestra este juego".
  `SpriteComposites` reproduce el RZX **con el catálogo ya construido** y agrupa lo que cae en
  pantalla. **No sobre los bytes con dueño de sprite**: en Exolon los planetas, las cápsulas,
  los cañones y las columnas están clasificados como FONDO, así que agrupar solo lo que la
  taint llama sprite mostraba los bichitos y se perdía todo lo grande. Agrupa lo que está
  ENCENDIDO y **corta donde cambia la tinta** —el color es lo único que dice dónde termina un
  objeto en este hardware, y es lo que separa un cañón verde del piso amarillo donde se
  apoya—, ignorando el bit BRIGHT para el corte (un planeta mitad brillante y mitad no es un
  planeta, y cortando ahí salían dos medias lunas). La banda de terreno cruza la pantalla y la
  descarta el tope de tamaño (`discover.objects.cols`/`.rows`); un planeta, una columna o una
  cápsula entran y salen enteros. El marcador queda afuera por el playfield del perfil. Los objetos iguales
  colapsan por hash de contenido, así que un ciclo de animación queda en un puñado de entradas
  ordenadas por cuántas veces el juego las dibujó. **Y se dice de qué está hecho cada uno**:
  `compuesto por: $fbee 1307 B · $fbec 24 B · $6400 21 B · ...` con enlace a la sección de cada
  pieza, con los bytes **por aparición** y agrupado por ENTRADA del catálogo: la taint corta
  por byte, así que sin eso un planeta punteado cita 140 hojas (`$dd40`, `$dd41`, `$dd42`…)
  con totales de cinco cifras sumados sobre cada vez que se dibujó, que no dice nada. Así
  queda `compuesto por 4 gráficos: $dd38 95.8 B · $edc0 12.5 B · ...`.
- **Dos hojas de contacto**: `catalogo-<juego>.png` son los **objetos, en color**, como se ven
  en el juego —es la página que uno mira— y `catalogo-<juego>-piezas.png` son las piezas del
  catálogo en blanco y negro, que contesta la otra pregunta ("¿qué catalogó?").
- **Una línea de datos por gráfico**, entre acentos graves, que es lo único que se parsea:
  `` `base=$edc0 last=$eddf size=32 stride=2 tipo=sprite veces=12480` ``. Corregir ahí un
  `stride` o pasar un `tipo=sprite` a `fondo` cambia lo que se renderiza, sin tocar código ni
  la db. El parser es deliberadamente tolerante con el orden y los espacios, y acepta `$edc0`,
  `0xedc0` o decimal: si un archivo editado a mano no carga, la herramienta no sirve.
- **El dibujo en ASCII**, dos caracteres por píxel (`██`/`··`) porque la celda monoespaciada es
  el doble de alta que de ancha y un carácter por píxel deja los sprites irreconocibles. Los
  cuadros de una tira de animación van **uno al lado del otro**, que es como se lee un ciclo de
  caminata; apilados verticalmente parecen un bicho imposiblemente alto.
- Las direcciones van abajo de cada gráfico en una tipografía de 3x5 píxeles. Es para la
  primera pregunta de un juego nuevo —¿el catálogo agarró personajes o agarró basura?—, que se
  contesta mirando, no leyendo una tabla.
- **`-Ddiscover.report.only=true`** rehace el reporte desde la `sprites_found` que YA está en
  la db, sin recatalogar: re-renderizar el archivo o recapturar los objetos con otros límites
  es cosa de minutos sobre una tabla que ya existe, y volver a derivarla sería media hora de
  replay al pedo. Los objetos se capturan con `discover.objects.frames` (6000),
  `discover.objects.sample` (4) y `discover.objects.max` (64).

Precedencia: si existe el `.md` (o el que liste `"md"` en el perfil), **gana sobre la `.db`**;
si no, se sigue cargando la db como siempre. Ojo con esto: **el .md sale de la corrida que lo
generó**, así que generarlo con `-Dmax.frames` chico deja un catálogo peor que la db que había.
Generar siempre con la corrida completa que uno quiera usar.

Flujo para un juego nuevo (el catálogo por taint NO necesita las pasadas del tracker: se
basta solo, y en Exolon dio 17x más cobertura — ver §5.1):
```
TaintDiscover <rzx> analysis/<g>-taint.db [maxFrames]     # -Dgame=<g> tambien escribe doc/catalogo-<g>.md
# motor de dirty-regions (Exolon, DD): agregar -Ddiscover.gate=8 -Ddiscover.drift=0.25
# re-blitter de pantalla entera (JSW, MM): dejar los dos apagados (default)
JSW3D -Dgame=<g>   (agregar el perfil a games.json)
```

Dos cosas que conviene mirar en la PRIMERA corrida de un juego nuevo, porque son las que más
ensucian y las dos se arreglan desde el perfil:

- **¿El decorado sale como ruido blanco?** Es el síntoma de §5.1: `updateTiles` extruye cada
  celda desde su template de memoria, y si el catálogo dio spans grandes (bancos de sprites
  enteros, no tiles de 8 filas) el bitmap no se parece a lo que hay en pantalla. `tiles=screen`
  lo resuelve sin tocar el catálogo. Le pasó a Exolon y a Monty.
- **¿Dónde está el marcador?** El playfield son las filas `[playfield.top, +playfield.rows)`.
  JSW/MM lo tienen ABAJO (`top=0, rows=16`, el default); Monty lo tiene ARRIBA y además el
  nombre de sala abajo, así que va `top=2, rows=20`. Sin el offset el texto del SCORE se
  extruye en relieve junto con la sala.

### El personaje sale al doble de alto, con otro cuadro mezclado adentro

Síntoma: algunos cuadros del protagonista salen bien y otros son el doble de altos con una
forma cualquiera pegada abajo; al caminar parece dejar una estela de esos bultos (no es
estela: son los bultos de más, uno por posición).

Causa: **taint-discovery corta las piezas por HUECO de direcciones**, así que una tira de
animación guardada con los cuadros pegados no tiene dónde cortarse y sale como UNA pieza.
`SpriteCatalog.sizeOf` toma ese tamaño y `ofMemory` lee la tira entera: varios cuadros
apilados en un modelo. En Monty las piezas eran de 85..113 bytes para un personaje de 16
filas (2-3 cuadros por modelo).

`updateSprites` lo corrige solo, sin tocar el catálogo: **el blob en pantalla sabe la altura
real**, así que el tamaño se capa por él. Solo cuando el exceso es un cuadro entero
(`bytes >= blobBytes * 2`), para que un blob apenas recortado por el borde o tapado por otro
sprite siga leyendo su bitmap completo, y para que un catálogo ya bien dimensionado (los 32
bytes de JSW sobre un blob de 16 filas) quede igual que siempre. Verificado sin cambios en
JSW, MM y DD.

**Lo que NO hay que probar acá: `fresh.frames`.** Es tentador porque el síntoma se ve como
estela, pero en Monty borra decorado REAL: las formaciones de roca están quietas muchos
frames, el gate las da por rastro viejo y desaparecen de la pantalla. `fresh.frames` es para
motores de dirty-regions que dejan taint viejo encendido (DD, Exolon), no para esto. La forma
de darse cuenta es comparar contra el 2D plano (`-Dtiles=false`) antes de decidir qué sobra:
sin esa referencia se corrige contra otra render 3D y se termina borrando lo que sí estaba.
El camino viejo por tracker sigue disponible (`AnalysisCLI z80run` + `z80track`), y es el que
usan JSW/MM/DD hoy.

Perillas de `TaintDiscover` (todas `-Ddiscover.*`): `rows` (filas de playfield muestreadas),
`gate`/`drift` (el par para dirty-regions, §5.1), `sample`/`from` (muestreo), `gap` (corte
entre piezas), `bg`/`reuse`/`mobility`/`stamps`/`freshfrac` (umbrales del clasificador),
`maxwrites` (filtro de buffers), `cap` (tope de hojas por byte), `minsize`/`min`. Todas las
métricas van a la columna `methods` de cada fila, así que se puede re-clasificar offline
leyendo el db sin volver a correr la pasada.

---

## 8. Archivos clave

- `minizx3d/.../JSW3D.java` — visor 3D; `updateSprites` (blobs, §5 paso 2), `updateTiles`,
  `updateBackdrop`, catálogo de params/presets, perfiles de juego.
- `minizx3d/.../OriginTaint.java` — el taint de orígenes (hash-consing, `union`, `leaves`,
  `spriteOf`/`tileOf`, `MAX_DEPTH`).
- `minizx3d/.../TaintReplay.java` — reproducción + propagación de taint en runtime; `lastWrite`
  (freshness); herramientas de debug; modo validación headless.
- `minizx3d/.../SpriteCatalog.java` — carga `sprites_found`; `strideOf`, `tileStride`, exclusión
  de ROM.
- `minizx3d/.../SmoothSpriteBuilder.java` / `VoxelSpriteBuilder.java` / `TileSlabBuilder.java` —
  construcción de los modelos 3D (leen el bitmap de memoria con stride).
- `minizx3d/.../TaintDiscover.java` — el taint-discovery (§5.1): blob → piezas → observación,
  discriminadores y emisión de `sprites_found`. Es el reemplazo del tracker.
- `translator/.../analysis/SpriteTracker.java` — el tracker por-invocación (§2.1); ya
  reemplazado por taint-discovery en Exolon, todavía en uso en JSW/MM/DD.
- `translator/.../analysis/Z80AnalysisRunner.java` — productor de la captura; `-Dmax.frames`.

---

## 9. Resumen en una frase

El tracker fusiona los gráficos porque razona por invocación de dibujo; el taint los separa
porque razona por píxel. Usar el taint para descubrir el catálogo (pasos 1 y 2) ya está hecho
y **resolvió la granularidad**: en Exolon, 3% → 51% de cobertura y 1,0 → 12,5 gráficos por
frame. Lo que queda no es separar gráficos entre sí sino decidir **cuál es sprite y cuál es
fondo**, y ahí el obstáculo está identificado: en un motor de dirty-regions hay que gatear los
rastros para que las métricas midan el presente, pero el gate fragmenta los blobs y deja
ciegos a `reuse`/`stamps`, que son los que cazan tiles estampados. El próximo paso es medir
cada señal en la vista donde es válida (§5.1), y recién después atacar máscaras (taint por
bit, paso 3).
