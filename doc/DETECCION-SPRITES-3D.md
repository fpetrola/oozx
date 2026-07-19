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

### 4.2. Exolon — NO anda bien (diagnóstico)

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
| Exolon | `exolon` | `analysis/exolon.db` | 🔴 catálogo malo (solo primeros 4000 frames); sirve para experimentar |

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

Flujo para un juego nuevo:
```
AnalysisCLI z80run  <rzx>  -Danalysis.db=analysis/<g>.db [-Dmax.frames=N]
AnalysisCLI z80track <rzx> -Danalysis.db=analysis/<g>.db [-Dmax.frames=N]
# curación del db si hace falta (mirar sprites_found con el juego corriendo)
JSW3D -Dgame=<g>   (agregar el perfil a games.json)
```

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
- `translator/.../analysis/SpriteTracker.java` — el tracker por-invocación (§2.1); a
  complementar/reemplazar con taint-discovery.
- `translator/.../analysis/Z80AnalysisRunner.java` — productor de la captura; `-Dmax.frames`.

---

## 9. Resumen en una frase

El tracker actual fusiona los gráficos porque razona por invocación de dibujo; el taint los
separa porque razona por píxel. El plan es usar el taint para descubrir el catálogo
(genérico, independiente del método de dibujo), agrupar por adyacencia en pantalla, y recién
después atacar máscaras (taint por bit) y composición (config, solo si hace falta). El primer
paso concreto y de bajo riesgo es formalizar `-Ddebug.scan` en un productor de catálogo.
