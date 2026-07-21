# Sprite3D: subsistema de conversión sprite 2D → 3D

> Convierte un sprite del Spectrum en una malla 3D, eligiendo la técnica a mano, por reglas
> automáticas, o por el default. Complementa `doc/DETECCION-SPRITES-3D.md`, que se ocupa del
> problema anterior: **qué** bytes de pantalla son un sprite. Este doc es el **cómo** se
> renderiza una vez identificado.

---

## 1. El mecanismo por default

**Figura geométrica + smooth después**, eligiendo la figura según el tipo de sprite. Una
primitiva (ovoide, esfera, cilindro, almohadón...) da el VOLUMEN y las pasadas de blur que ya
existían dan el ACABADO. La selección automática está **activa por default**: las reglas de
`sprite3d-rules.json` asignan a cada tipo su figura.

Precedencia: **override a mano > selección automática > `INFLATE`** (distance transform, el
que no falla nunca, para lo que ninguna regla matchea).

Para volver al comportamiento previo —inflado por distancia, sin primitivas—:
`-Dsprite3d.auto=false`.

Implementación: `SmoothSpriteBuilder.build(..., primitive, roundness)`. Conserva intacta la
maquinaria de silueta y el chamfer, y solo reemplaza de dónde sale la altura. El chamfer
sigue haciendo falta para el **taper del borde**: el frente y el dorso tienen que encontrarse
en h=0 o la silueta muestra un acantilado en vez de un canto.

**El slider de profundidad se rebasea en el camino de primitivas** (`SmoothSpriteBuilder.GAIN`).
La inflación vieja picaba en `2*sqrt(radio)`, el sólido geométrico pica en `radio`: un valor
de slider calibrado para la curva vieja (0.41 era un valor guardado real) dejaba cada sprite
como un medallón de 3px de profundidad sobre un cuerpo de 16. El rebase pone ese mismo ajuste
en "tan profundo como ancho", y D/C sigue funcionando desde ahí. **Solo se rebasea el camino
de primitivas**: `INFLATE` a secas sigue significando lo que siempre significó.

**La profundidad se escala con el radio del propio sprite** (`radiusPx = min(rx,ry)`), no con
un factor fijo: una esfera es realmente una esfera —tan profunda como ancha— en vez de un
medallón chato. Se usa el semieje MENOR para que un sprite alargado reciba un cilindro tan
grueso como angosto es, y no uno absurdamente más profundo que su propia altura.

**`roundness` mezcla la primitiva contra la inflación PROPIA del sprite**, no contra una
constante. Con `roundness=1` sale un sólido geométrico limpio pero todos los sprites terminan
siendo el mismo huevo genérico; conservar parte de la distance transform es lo que mantiene el
relieve propio del personaje para que siga pareciéndose a sí mismo. Los defaults de las reglas
están en 0.6-0.75 por eso.

### Una técnica por PERSONAJE, no por cuadro

Cada cuadro de animación es un base de catálogo propio (JSW guarda 8 cuadros de 32 bytes en
una página de 256). Seleccionando por base, **25 de 28 personajes de JSW se partían en 2-4
técnicas distintas** y la figura saltaba al caminar. Ahora la elección es del grupo
(`base & ~(sprite3d.group-1)`, default 256) y se decide por **mayoría entre todos los cuadros
del strip** (`Sprite3DPipeline.vote`): un cuadro suelto puede medir como texto fino o como
forma angular, así que quedarse con el primero que aparecía era arbitrario y además elegía
técnicas chatas para cosas claramente volumétricas. Votar también hace el resultado
independiente del orden en que aparecen los cuadros.

Un override sobre cualquier cuadro configura al personaje entero.

---

## 2. Piezas

| Clase | Qué hace |
|---|---|
| `SpriteBitmap` | El sprite ya desacoplado del Spectrum: silueta 1-bit + color opcional + hash + base de catálogo. Guarda las filas en el layout empaquetado original a propósito, para que `memView()` alimente **sin cambios** a los builders que ya existían |
| `Sprite3DConfig` | Técnica y parámetros. `hash()` entra en la clave del caché |
| `MeshBakingStrategy` | Una técnica. `bake` + `vertexEstimate` |
| `SpriteFx` | EPX/Scale2x, distance transform (chamfer) y los perfiles de primitiva |
| `SurfaceNetsBuilder` | Campo escalar 3D + blur separable + naive surface nets |
| `SpriteAnalyzer` / `SpriteFeatures` | Métricas de silueta, deterministas |
| `TechniqueSelector` | Reglas ordenadas desde `sprite3d-rules.json`, primera que matchea gana |
| `Sprite3DConfigStore` | Overrides por sprite, JSON por juego |
| `Sprite3DPipeline` | Resuelve config → hornea → cachea (LRU con `dispose()`) |

**Reuso**: `VOXELS`, `INFLATE` y `SLAB` delegan en `VoxelSpriteBuilder`, `SmoothSpriteBuilder`
y `TileSlabBuilder`, que ya estaban y ya estaban validados en cuatro juegos. Lo único que se
tocó de ellos fue generalizar `VoxelSpriteBuilder` con `buildWithDepth(mask, depth, fill,
doubleSided)`, para que las primitivas y los mapas de profundidad pintados a mano reusen su
emisión de cajas en vez de duplicarla.

---

## 3. Técnicas

`BILLBOARD` · `SLAB` · `VOXELS` · `PRIMITIVE` (esfera, ovoide, cilindro V/H, cono, almohadón)
· `INFLATE` (distance transform, el default) · `SURFACE_NETS` · `STACK`.

`PRIMITIVE` es el **mecanismo por default** (§1): figura geométrica por tipo de sprite, con
el smooth aplicado después. Con `voxelFill < 1` la misma técnica emite cajas en vez de malla
suave, que es el único motivo para querer la versión chunky.

`INFLATE` quedó como **red de seguridad**: es lo que `SmoothSpriteBuilder` hacía y quedó
validado en JSW, Manic Miner, Dynamite Dan y Exolon. Se adapta a cualquier silueta sin
clasificar nada, así que es donde cae lo que ninguna regla matchea — y a donde se vuelve
entero con `-Dsprite3d.auto=false`.

**EPX quedó opt-in, no default** (a diferencia de lo que pedía el prompt original):
`SmoothSpriteBuilder` ya suaviza el contorno con upsample 4× + threshold 0.45, elegido
explícitamente para conservar rasgos de 1px. Apilar EPX encima suaviza dos veces y en 16×16
—el tamaño típico acá— se pierde detalle. Además EPX compara **colores** y una celda Spectrum
tiene dos: hay que medirlo A/B antes de considerarlo default, y si gana, que reemplace, no
que se sume.

---

## 4. Las dos capas de clasificación

Son preguntas distintas y viven separadas:

- **Capa 1 — `TaintDiscover`**: ¿esto es sprite o es fondo? Métricas de comportamiento
  (`reuse`, `stamps`, `mob`, `drift`).
- **Capa 2 — `SpriteAnalyzer` + `TechniqueSelector`**: dado que es un sprite, ¿cómo lo
  renderizo? Métricas de forma (`aspect`, `fill`, `solidity`, `symmetryV`, `components`,
  `holes`, `thinness`).

Por eso un "sprite" que resulta ser texto y otro que resulta ser humanoide reciben técnicas
distintas sin que ninguno deje de ser sprite.

Reglas en `minizx3d/src/main/resources/sprite3d-rules.json`, reemplazables con
`-Dsprite3d.rules=<path>`. Formato: `when` mapea feature → `[min,max]` inclusive y **todas**
deben cumplirse; `then` pisa campos de la config. Gana la primera; si ninguna matchea, el
sprite se queda con el default (nunca se lo fuerza).

Validado sobre el catálogo real de JSW: los glifos de texto de 6 bytes salen `texto-hud →
SLAB` (una primitiva sobre una letra la deformaría), los humanoides `humanoide →
PRIMITIVE/OVOID`, los irregulares con huecos reales `SURFACE_NETS`.

**Trampa que costó**: `holes` contaba cada hueco de 1 píxel del dithering, así que los
guardianes detallados de JSW reportaban 7-16 "huecos" y casi todo caía en la rama cara de
surface nets. Ahora un hueco cuenta solo con ≥3 píxeles (`SpriteAnalyzer.MIN_HOLE`).

---

## 5. Override por sprite

Keyed por la **base del catálogo, no por el hash del bitmap**. Cada cuadro de animación tiene
su propio hash: hashear obligaría a configurar un personaje una vez por cuadro, y otra vez
por cada cuadro todavía no visto. La base es la misma para toda la animación. El hash del
bitmap sigue siendo lo que debe ser: la clave del **caché de mallas**.

Persiste en `~/.jsw3d-sprite3d-<juego>.json` (`-Dsprite3d.file=` lo redirige), misma
convención que la config viva del visor.

Teclas de ajuste en vivo del RELIEVE: **I** elegir gráfico de fondo (cicla los modelados en
pantalla, se pinta blanco el elegido) · **W** pasarlo a plano / volverlo a modelar, se guarda en
`~/.jsw3d-relief-<juego>.json`.

Teclas de ajuste en vivo: **F7** elegir sprite (cicla los de la pantalla) · **F8** técnica ·
**F9** primitiva · **F10** guardar override · **F11** volver a auto/default. F8/F9 aplican al
instante sin escribir a disco (el caché keyea por `(bitmap, config)`, así que la malla nueva
se hornea sola en el frame siguiente); F10 es lo que persiste.

---

## 6. Cosas que rompen si no se respetan

- **Nunca liberar un modelo que una instancia todavía referencia.** Las teclas que
  reconstruyen (M, S/X, D/C, T/G, presets) disparan ENTRE snapshots, y `updateSprites` /
  `updateTiles` solo corren cuando llega un frame nuevo. Si se hace `dispose()` ahí mismo,
  `render()` sigue dibujando las instancias viejas contra mallas ya liberadas — y explota como
  `No buffer allocated!` en `ModelBatch.end()`, lejísimos de la causa. Por eso todo lo que se
  saca de un caché en runtime va a `pendingDispose` (`retire()`) y se libera en
  `releaseRetired()`, en el único punto donde las listas de instancias están vacías: arriba
  del update por frame. Vale igual para la expulsión del LRU de `Sprite3DPipeline`, que por eso
  retira en vez de destruir (`drainRetired()`). Había un `Thread.sleep(100)` como parche en
  `afterKey()`: no garantizaba nada y encima bloqueaba el hilo GL.
- **Nunca emitir una malla vacía.** Un `Mesh` sin index buffer no falla al construirse: falla
  al **renderizar**, con `No buffer allocated!` y un stack que apunta a `ModelBatch.end()`,
  lejos de la causa. Los builders devuelven `null` y el llamador deja el sprite en 2D.
- **Y si una técnica devuelve `null`, hay que caer a otra, no descartar el sprite.** Surface
  nets no encuentra isosuperficie cuando el blur se come un cuadro flaco: descartarlo hacía
  que cuadros sueltos de un guardián **parpadearan y desaparecieran** mientras los demás se
  veían. `Sprite3DPipeline` cae a `INFLATE` y después a `VOXELS`, que siempre dan geometría.
- **`vertexEstimate` antes de hornear.** Una malla libGDX indexa con shorts: 32767 vértices
  por modelo. El pipeline pregunta primero y degrada a `VOXELS` si no entra.
- **`dispose()` al desalojar del LRU.** Son recursos GPU; soltar la referencia los filtra.

---

## 6.a Config por SPRITE en games.json

Ademas de las `properties` (que valen para todo el juego), cada juego puede traer una seccion
`sprites` con config para sprites puntuales, por **direccion base del catalogo**:

```json
"sprites": {
  "$9d00": { "technique": "PRIMITIVE", "primitive": "OVOID", "roundness": 0.85,
             "smoothing": 0.25, "voxelLook": true },
  "$b940": { "technique": "PRIMITIVE", "primitive": "CYL_V", "roundness": 1.0 }
}
```

Acepta las mismas claves que las reglas y que el archivo de overrides. Una entrada cubre
**todo el strip de animacion** de ese personaje (la pagina de 256), porque la busqueda cae del
base exacto al grupo.

**Precedencia**: lo que guardaste a mano con F10 (`~/.jsw3d-sprite3d-<juego>.json`) le gana a
games.json, para que el ajuste en vivo no quede pisado en silencio por el archivo del
proyecto. Al arrancar se imprime cuantos sprites configuro cada fuente.

**Como averiguar la direccion de un sprite**: F7 cicla los sprites que hay en pantalla e
imprime su base, sus features y si ya tiene override. Esa es la direccion que va como clave.

## 6.b Presets, override por juego y menu

Los parametros de sprites estan registrados como `Param`/`Toggle` igual que cualquier otro
efecto, y eso resuelve tres cosas de una sola vez porque el registro ya alimentaba todo:

- **Presets**: `buildConfigTree` recorre todos los `Param`/`Toggle`, asi que entran solos.
  Un preset guarda ahora tambien la forma de los sprites — se puede tener uno "parecido al
  original" y otro tipo dark-storm y alternar con F4/F5.
- **Override por juego**: cada uno declara su clave *legacy*, que es la misma que se pone en
  `properties` de `games.json` por juego (`sprite3d.smoothing`, `sprite3d.maxdepth`, ...).
- **Ajuste en vivo**: aparecen en el menu de config bajo la seccion `Sprites 3D`.

### Todos los `-D` de render y catalogo, tambien en el menu

Lo mismo se extendio a TODO lo que antes solo entraba por linea de comando (`JSW3D.java`):

- **Seccion `Render`** — perillas que cambian lo que se ve, aplicadas por frame asi que el
  cambio es inmediato: `tiles` (slab/screen/off, un `Choice`), `blobs` (base/adjacent),
  `playfield` fila inicial + alto, `relieve` (profundidad / frames-movil / celdas-decor /
  isla-bbox / celdas-quieto / motas-del-cielo (celdas y px) / barras-del-cielo / tinte del relleno / relleno de siluetas huecas), deteccion de
  items, opacidad de fantasmas, y los dos del **hilo de taint** —
  `sprite bits` y `fresh frames`— tageados `[re-seek]` porque su efecto es hacia adelante
  (`TaintReplay.spriteBitsOn`/`freshFrames`, `volatile`, seteados desde el hilo de render).
- **Seccion `Catalogo (offline)`** — las perillas `discover.*` de `TaintDiscover`, tageadas
  `[recatalogar]`. NO cambian la vista (el catalogo ya esta horneado): se editan, se guardan
  al config del juego, y la **proxima** corrida de `TaintDiscover -Dgame=<este>` las lee
  (`loadDiscoverSettings` lee el mismo archivo). Cierra el lazo calibrar → recatalogar.

Dos tipos nuevos de item de menu lo hacen posible sin romper el resto: `Choice` (opciones con
nombre, se guarda la palabra en el JSON, no un indice) y `addFlag` (si/no). Ambos viajan por el
mismo `Param` de floats — el float es el indice — asi que menu, guardado y carga no se enteran;
solo el DISPLAY y la serializacion ramifican en `labels != null`.

**Ojo con los limites acoplados** (costo un crash intermitente): `playfield.top` y `.rows` se
tunean por separado, asi que su SUMA puede pasarse de las 24 filas y cualquier loop que indexa
`idx(y0+r,col)` se sale del array de 6144 bytes de pantalla. Se clampa una vez en `playEnd()`
(`min(24, top+rows)`) y todos los loops del playfield lo usan.

### games.json: bloque global + override por juego, estructurado

`properties` ahora acepta **objetos anidados** (`GameProfile.applyProps` los aplana a claves
punteadas), asi se escribe estructurado en vez de un muro de claves planas:

```json
"properties": {
  "render":   { "tiles": "screen", "playfield": { "top": 2, "rows": 20 } },
  "sprite3d": { "smoothing": 0.35, "maxdepth": 8 },
  "discover": { "gate": 8 }
}
```

Hay un **bloque `properties` GLOBAL** al tope de games.json (hermano de `games`) que vale para
todos los juegos, y cada juego lo pisa con el suyo. Precedencia, de mayor a menor:
**`-D` explicito > `properties` del juego > `properties` global > default del campo**, y arriba
de todo el archivo de config por juego (lo que tuneaste en vivo). Las claves planas viejas
siguen funcionando (cada campo lee por su clave *legacy* ademas de la anidada). `TaintDiscover`
usa el mismo `GameProfile.applyGamesJson` para leer global+juego en la pasada offline.

**Reglas vs perillas.** Una REGLA elige la FORMA (tecnica + primitiva); las perillas vivas
mandan el ACABADO (redondeo, profundidad, tamano de voxel, EPX). Sin esa division, una regla
que fijara el acabado dejaba la perilla inerte — que es exactamente como el dial de suavizado
llego a parecer roto. Se aplica en `Sprite3DPipeline.finish`, y **fuera** del cache por
personaje: cachear el acabado ya aplicado hace que mover una perilla nunca llegue al sprite.

### Menu tipo TV

Cuatro niveles con breadcrumb: `CONFIG > seccion > grupo > parametro`, y en cada nivel se ven
los hermanos disponibles en vez de tener que recordarlos.

Los niveles "lista de parametros" y "editando un valor" estan separados a proposito. Con el
ajuste atado a izquierda/derecha en la lista **no habia forma de volver arriba**: llegar a un
valor te dejaba atrapado, porque izquierda solo decrementaba. Ahora izquierda es "volver"
mientras elegis, y recien se convierte en "bajar el valor" cuando entraste con derecha. El
breadcrumb muestra el parametro y el cartel cambia a EDITANDO para que se note en que modo
estas.

| tecla | accion |
|---|---|
| arriba / abajo | moverse en el nivel actual (tambien mientras editas: salta a otro valor) |
| derecha | entrar; sobre un parametro, entra a EDITARLO |
| izquierda | volver; **solo ajusta cuando ya entraste al valor** |
| BACKSPACE | volver un nivel, incluso saliendo de un valor |
| TAB / SHIFT+TAB | hermano siguiente / anterior |
| F12 | **guardar sobre el preset activo**, sin preguntar el nombre |
| ESC | cerrar |

## 7. Pendiente

- **Horneado en worker thread.** Hoy es síncrono en el hilo GL. El riesgo es real y medido:
  ya se vio el visor arrastrarse por churn de caché. El worker debe trabajar sobre el
  `SpriteBitmap` ya copiado — no puede leer memoria del Z80, que la maneja el hilo
  `taint-replay`. La subida a GPU va con `Gdx.app.postRunnable`.
- **`SURFACE_NETS` está fuera de la selección automática.** Producía enemigos que se veían
  como cáscara hueca en vez de cuerpo (sospecha: winding/normales invertidas, se ve el
  interior). Sigue disponible a mano con F8. Hay que revisarlo antes de volver a activarlo.
- **Siluetas con el centro hueco** (JSW tiene un guardián que es literalmente un marco vacío)
  salen como anillo, y es fiel al original: ninguna primitiva puede inventarle cuerpo a una
  silueta agujereada. Si se quisiera, haría falta una opción de rellenar huecos antes de
  inflar.
- **`ColorMode.TEXTURE`.** Hoy el color sigue siendo un tint por blob (voto de ink). Subir el
  bitmap como textura `Nearest` con UVs por vértice daría color exacto por píxel y
  conservaría el attribute clash, que es parte de la estética. Es de lo más rendidor que
  queda.
- El `No buffer allocated!` intermitente que aparecía al apretar teclas de render rápido
  quedó atacado en su mecanismo (liberación diferida, §6). No se pudo reproducir headless
  —necesita input real a mayor ritmo que los snapshots—, así que la verificación es por
  lectura del código, no empírica.
