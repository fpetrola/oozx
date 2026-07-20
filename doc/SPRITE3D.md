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
- **`vertexEstimate` antes de hornear.** Una malla libGDX indexa con shorts: 32767 vértices
  por modelo. El pipeline pregunta primero y degrada a `VOXELS` si no entra.
- **`dispose()` al desalojar del LRU.** Son recursos GPU; soltar la referencia los filtra.

---

## 7. Pendiente

- **Horneado en worker thread.** Hoy es síncrono en el hilo GL. El riesgo es real y medido:
  ya se vio el visor arrastrarse por churn de caché. El worker debe trabajar sobre el
  `SpriteBitmap` ya copiado — no puede leer memoria del Z80, que la maneja el hilo
  `taint-replay`. La subida a GPU va con `Gdx.app.postRunnable`.
- **`ColorMode.TEXTURE`.** Hoy el color sigue siendo un tint por blob (voto de ink). Subir el
  bitmap como textura `Nearest` con UVs por vértice daría color exacto por píxel y
  conservaría el attribute clash, que es parte de la estética. Es de lo más rendidor que
  queda.
- El `No buffer allocated!` intermitente que aparecía al apretar teclas de render rápido
  quedó atacado en su mecanismo (liberación diferida, §6). No se pudo reproducir headless
  —necesita input real a mayor ritmo que los snapshots—, así que la verificación es por
  lectura del código, no empírica.
