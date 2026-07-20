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

**Siguiente paso concreto**: medir cada señal en la vista donde es válida — `reuse`/`stamps`
sobre la pantalla SIN gatear (ahí "cuánta área cubre este gráfico simultáneamente" se mide
bien) y `drift`/`fresh` sobre la gateada (ahí "se está repintando / se mueve" se mide bien).
Es un cambio chico y ataca la causa, en vez de mover umbrales.

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
por bit. La variante liviana que alcanza — y que conviene sobre el taint por bit completo —
es llevar por cada byte de pantalla una **máscara de qué bits vinieron de un sprite**: en un
`AND máscara / OR sprite` esos bits se calculan exactos, y un byte cuya máscara quedó en 0 ya
no le pertenece al sprite aunque su unión de orígenes lo siga nombrando.

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
| Exolon | `exolon` | `analysis/exolon-taint.db` | 🟡 catálogo por taint-discovery (30k frames): granularidad ok (§5.1), se infla de más el terreno y el marcador |

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
- **`-Dlog=true`** — habilita todos los prints (por default la consola está muda).

Flujo para un juego nuevo (el catálogo por taint NO necesita las pasadas del tracker: se
basta solo, y en Exolon dio 17x más cobertura — ver §5.1):
```
TaintDiscover <rzx> analysis/<g>-taint.db [maxFrames]
# motor de dirty-regions (Exolon, DD): agregar -Ddiscover.gate=8 -Ddiscover.drift=0.25
# re-blitter de pantalla entera (JSW, MM): dejar los dos apagados (default)
JSW3D -Dgame=<g>   (agregar el perfil a games.json)
```
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
