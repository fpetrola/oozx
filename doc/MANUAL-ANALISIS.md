# Manual del sistema de análisis: cómo funciona, qué guarda y para qué sirve

> Documentos hermanos: `GUIA-ANALISIS-ECUACIONES.md` es el plan original;
> `COMO-FUNCIONA-ANALISIS.md` es la crónica de implementación (fases F1..F5, desvíos,
> hallazgos). **Este documento explica el sistema conceptualmente**: la idea, el modelo
> de datos, qué es precalculado y qué es consulta, por qué las deducciones funcionan,
> y qué más se puede construir encima.

---

## 1. La idea en un párrafo

Tenemos un juego de ZX Spectrum convertido automáticamente a Java (cada instrucción
Z80 es una línea de Java precedida por su dirección original, `pc(37974, ...)`) y una
partida completa grabada (RZX: el snapshot inicial + todos los inputs). Al reproducir
la partida, cada instrucción se ejecuta millones de veces con datos reales. El sistema
**instrumenta esa reproducción para acumular evidencia por instrucción** — qué calcula,
con qué valores, de dónde vinieron — y después corre **análisis offline sobre la
evidencia** que deducen la estructura del juego (sprites, coordenadas, buffers,
entidades) sin saber nada del juego. Cada deducción es un teorema sobre datos
observados, no una suposición.

## 2. Por qué funciona: los cuatro pilares

**P1 — Una instrucción = un "site" = una ecuación.** El site ID es la dirección Z80
real (0..65535). La ecuación (`mem[HL] = A`, `E = mem[IX+3]`) es texto estático que
dice *cómo* se transforma el dato; la evidencia dinámica dice *cuánto* y *con qué*.
Componer ecuaciones a través del grafo de dependencias reconstruye cualquier cálculo.

**P2 — El universo es diminuto y la ejecución masivamente repetitiva.** 64K de
memoria, ~2600 instrucciones activas, y una partida de 73K frames ejecuta 35M+ de
operaciones: cada instrucción repite su patrón miles de veces. Por eso la agregación
por site pierde poco (los rangos min/max y las máscaras de bits convergen a la verdad)
y por eso los "episodios" (invocaciones enteras) se deduplican solos: pocos caminos de
ejecución distintos explican casi todo.

**P3 — Provenance total.** Cada byte que se mueve arrastra "qué site lo escribió", a
través de TODOS los canales de transporte: memoria (`lastWriterMem[64K]`), registros
(`regProv[23]`), stack Z80 (snapshot por push/pop), y copias en bloque (el ldir
muestrea el origen). Resultado: un grafo de edges `(origen, destino, canal, rol)`
donde el rol dice si la dependencia alimentó la **DIRECCIÓN**, el **VALOR** o la
**CONDICIÓN** del consumidor. Las raíces del grafo son solo dos: **INIT** (byte cargado
del cassette) e **IO** (tecla del jugador). Todo lo demás es cadena intermedia.

**P4 — Verificación de identidad.** La instrumentación no puede alterar la semántica:
se compara un hash de los 64K de memoria en cada frame contra una corrida sin capturar.
`IDENTICAL: 72929 frames match` es la red de seguridad de todo lo demás; se re-verifica
en cada corrida de track automáticamente.

## 3. Arquitectura: dos pasadas + análisis

```
                    (offline, una vez)
  JetSetWilly2.java ──EquationExtractor──> JetSetWilly2Instrumented.java + sites.json
                                                      │
       ┌──────────────────────────────────────────────┤
       ▼ PASADA 1: agregados (~15s)                   ▼ PASADA 2: track dirigido (~25s)
  RZXAnalysisRunner reproduce el RZX             SpriteTracker relee la DB de la pasada 1,
  acumulando POR SITE: conteos, rangos,          decide solo QUÉ loguear POR INSTANCIA
  máscaras, edges, cfg, bulks                    (draw-sites, entries, celdas, lecturas gfx)
       │                                         y re-reproduce el RZX
       ▼                                              │
  analysis.db (tablas de agregados)              analysis.db (tablas por instancia/derivadas)
       │                                              │
       └──────────────┬───────────────────────────────┘
                      ▼
        AnalysisCLI: queries en vivo (slice, explain, map, positions, ...)
```

La pasada 2 existe porque la agregación pierde el "cuándo" y el "cuál": guarda rangos,
no valores por momento. Pero loguear TODO por instancia es imposible (miles de millones
de eventos). La solución es **dirigida**: la pasada 1 descubre qué ~40 sites y ~170
celdas importan, y la pasada 2 loguea solo eso (~35M eventos, manejable).

## 4. El modelo de datos, tabla por tabla

Todo vive en `analysis/analysis.db` (SQLite). Tres categorías:
**[C] capturado crudo** durante la reproducción, **[D] derivado** automáticamente al
final de la corrida, **[E] estático** del extractor.

### Pasada 1 — agregados por site

| Tabla | Cat | Contenido | Por qué sirve |
|---|---|---|---|
| `sites(pc, method, kind, stmt, equation)` | E | la ecuación normalizada de cada instrucción (`mem[HL] = A`, `A = (A-8)&255`) | es el texto que se compone en cadenas; `kind` distingue MEM_READ/WRITE, BRANCH, BULK |
| `site_roles(pc, ch, role)` | E | para cada site, qué rol juega cada canal de entrada (H=ADDR, MEM=VAL, F=COND) | permite separar "cómo se construyó la dirección" de "cómo se construyó el byte" |
| `site_stats(pc, op, count, addr_min/max, addr_and/or, val_min/max, val_and/or, first/last_frame)` | C | por site y operación (W/R): conteo, rangos de dirección y valor, máscaras de bits, ventana de frames | los rangos clasifican regiones (¿escribe pantalla? ¿lee la tabla de sprites?); las máscaras revelan strides y alineación; los frames dicen cuándo estuvo activo |
| `edges(src, dst, ch, role, count)` | C | el grafo de dataflow: "el valor que produjo `src` entró a `dst` por el canal `ch` (registro/MEM/STK) con rol ADDR/VAL/COND, `count` veces" | es EL corazón: caminar los edges hacia atrás desde cualquier escritura reconstruye toda la tubería hasta INIT/IO |
| `cfg(src, dst, count)` | C | transiciones de control pc→pc con conteo | conteos de ejecución exactos por instrucción; detecta loops y branches nunca tomados |
| `bulk_stats(pc, count, src_min/max, dst_min/max, len_min/max)` | C | cada ldir: rangos de origen/destino/longitud | las cadenas de copias revelan los buffers (28672→24576→16384) y las cargas de tablas |
| `flags(pc, reads_f, io)` | C | qué sites leen el flag F (branches) y cuáles leen input externo | los IO son raíces de slicing (input del jugador) |

### Pasada 2 — por instancia (dirigido) y derivados

| Tabla | Cat | Contenido | Por qué sirve |
|---|---|---|---|
| `sprite_draws(frame, method, kind, x, y, w, h, nwrites, buffer, gfx, gfx_hi, path)` | C | **una fila por invocación de rutina de dibujado**: dónde dibujó (posición decodificada del layout ZX), qué gráfico leyó (`gfx..gfx_hi` = identidad del sprite) y por qué camino de ejecución (`path` = hash de los pc recorridos) | responde "qué se dibujó, dónde, cuándo y por qué" para los 73K frames; es la materialización del concepto "episodio" |
| `frame_cells(frame, addr, val)` | C | serie temporal (por deltas) de las ~170 celdas descubiertas como relevantes | valores exactos de coordenadas/estado en cualquier frame; reconstruye trayectorias |
| `coord_cells(addr, axis, transform, off, matched, frames, rate)` | D | celdas que la votación afín identificó como coordenada X o Y, con su fórmula (`pos = (v&31)*8+0`) y soporte | mapea variables del juego a significado |
| `coord_pairs(x_addr, ..., y_addr, ..., joint, rate)` | D | pares (X,Y) validados conjuntamente: EL MISMO dibujo satisface ambas celdas a la vez | la validación decisiva (azar ~1%/frame); un par = una entidad posicionable |
| `coord_tables(stride, slots, x0, y0, ...)` | D | pares agrupados en tablas de registros con stride | la estructura de entidades (base, tamaño de registro, offsets de X e Y) |
| `episodes(method, path, count, gfx_lo/hi, cond)` | D | los caminos de ejecución deduplicados por rutina, con la condición del registro de entidad que selecciona cada camino (`rec+0&7=2`) | "el if que identifica guardián vertical", deducido: qué campo del registro elige la variante de dibujado |
| `sprites_found(base, last, size, veces, frame_first/last, methods)` | D | las direcciones de cada sprite agrupadas: base, extensión (moda), usos, ventana de frames, rutinas que lo usan | el catálogo de assets gráficos del juego |

## 5. Qué es precalculado y qué es query directa

**Precalculado** (queda en la DB al terminar cada corrida): todo lo de la sección 4.
Las tablas [D] son análisis que corren una vez al final del track porque necesitan
cruzar millones de filas (correlación, clustering, deduplicación de caminos).

**Query directa** (se computa al momento, en segundos, sobre las tablas de arriba):

| Comando | Qué compone | Pregunta que responde |
|---|---|---|
| `slice lo hi [depth] [fanout] [addr\|val\|cond]` | BFS hacia atrás por `edges` desde los writers (ecuaciones Y copias) del rango | "¿de dónde vino lo que hay acá?" |
| `explain lo hi` | slice + clasificación al vuelo de cada lectura (ESTÁTICA/DINÁMICA/MIXTA) + roles agrupados + anotaciones de track | la narrativa completa: dirección/valor/condición hasta INIT/IO |
| `map` | todas las recetas juntas (§6) | el inventario del juego en 6 secciones |
| `positions f1 [f2]` | `sprite_draws` + `coord_pairs` aplicados a `frame_cells` | "¿qué había en pantalla en el frame N y dónde estaba cada entidad?" |
| `sprites`, `regions`, `copychains`, `equations`, `site pc` | vistas directas de agregados | exploración de bajo nivel |
| SQL libre (python3/sqlite) | cualquier corte | trayectorias, apariciones de un sprite, joins píxel↔atributo... |

La regla de diseño: **capturar es caro (re-correr el RZX), consultar es gratis**. Por
eso la captura guarda evidencia cruda generosa y las preguntas se responden después,
las veces que haga falta, sin re-correr.

## 6. Las recetas: por qué cada deducción es válida

Cada "descubrimiento automático" es una regla simple sobre la evidencia. Las claves:

- **Buffers**: la pantalla es [16384..23295] (hardware, no juego). Todo rango que se
  copia en bloque hacia una región tipo-pantalla es también tipo-pantalla, con el
  delta de direcciones compuesto (fixpoint sobre `bulk_stats`). Así aparecen solos el
  doble buffer y los buffers de atributos, y las posiciones dibujadas en un buffer se
  decodifican como si fueran pantalla.
- **Rutinas de dibujado**: sites cuyo `site_stats` de escritura intersecta una región
  tipo-pantalla. Sus métodos son "draw methods".
- **Celdas-coordenada**: BFS hacia atrás desde los draw-sites siguiendo SOLO edges de
  rol **ADDR** (cómo se construyó la dirección) → rangos de RAM chicos y mutables.
  Después, votación: una celda es X si `transform(valor) + constante` coincide con la
  posición de algún dibujo, frame tras frame. La coincidencia por eje suelto es barata
  (~30% de azar con 20 dibujos/frame y 32 columnas), así que la validación decisiva es
  **conjunta**: un par de celdas vecinas es (X,Y) real solo si EL MISMO dibujo satisface
  ambas a la vez (azar ~1%). Exclusividad greedy: cada celda pertenece a su mejor par.
- **Gráficos (sprites)**: BFS por edges de rol **VAL** → lecturas cuyo origen es INIT
  (nunca escritas por el juego = vinieron del cassette) y que alimentan los bytes que
  llegan a pantalla. La geometría sale de los datos: el rango leído por invocación es
  el sprite; la moda de las extensiones es su tamaño real (los recortes de borde de
  pantalla son minoría y se absorben).
- **Estructura de entidades**: los pares (X,Y) fuertes caen en direcciones con stride
  uniforme (33026, 33034, 33042... = stride 8) → tabla de registros; la base y el
  total de slots salen de las copias que la cargan; los offsets de cada campo, de la
  aritmética (`mem[IX+2]`, `mem[IX+5]`).
- **Episodios y condiciones**: el hash del camino de ejecución por invocación se repite
  masivamente (P2); para cada camino frecuente se busca qué byte/máscara del registro
  de la entidad dibujada es constante dentro del camino y distinto entre caminos → ese
  campo es el selector (el "tipo" de entidad).

## 7. Usos hoy (qué se puede preguntar ya)

- **El mapa del juego** en un comando (`map`): buffers, rutinas, gráficos con su
  selector dinámico, tablas de consulta, estructura de entidades, variables.
- **Cualquier "por qué" hacia atrás** (`explain`): quién escribe una celda, de dónde
  vienen valor y posición, a través de qué buffers, hasta el cassette o la tecla.
- **La partida entera como datos semánticos**: `sprite_draws` + `frame_cells` son la
  película completa — qué sprite, en qué posición, en qué frame, con qué valores de
  las variables del juego. Ejemplos de SQL de una línea:
  - trayectoria de un enemigo: `SELECT frame,x,y FROM sprite_draws WHERE gfx=46080`
  - habitaciones visitadas y cuándo: `SELECT DISTINCT val FROM frame_cells WHERE addr=33824`
  - catálogo de assets: `SELECT * FROM sprites_found ORDER BY veces DESC`
  - variantes de comportamiento: `SELECT * FROM episodes ORDER BY count DESC`
- **Depuración de la conversión Z80→Java**: IDENTICAL por frame localiza cualquier
  divergencia semántica al frame exacto donde aparece.

## 8. Qué se podría construir con esta información

- **Extractor de assets**: `sprites_found` + el snapshot → volcar cada sprite como
  imagen (base, 32 bytes, 2 bytes/fila → PNG de 16×16). Lo mismo para la fuente y los
  ítems. El catálogo gráfico completo del juego, etiquetado por uso real.
- **Visor/replay semántico**: reproducir la partida SIN emulador, dibujando desde
  `sprite_draws`/`frame_cells` — o un mini-mapa de todo el juego con las trayectorias
  de cada entidad superpuestas por habitación.
- **Modelo del juego a alto nivel**: entidades (registro, campos, tipos), variables
  (vidas, habitación, ítems), reglas (condiciones por camino) — la base para un
  **port o remake** generado, o para documentación estilo "ROM hacking" automática.
- **Detección de contenido muerto**: sprites nunca dibujados (están en la zona de
  gráficos pero no en `sprites_found`), branches nunca tomados (`cfg`), celdas nunca
  leídas — lo que el juego trae pero la partida no ejercitó.
- **Comparación entre partidas**: correr dos RZX distintos y comparar las DBs (mismas
  tablas): qué habitaciones/sprites/caminos aparecen en una y no en otra.
- **Búsqueda de secretos/cheats**: variables que alimentan condiciones críticas
  (vidas, energía) rastreadas con `slice cond` hasta el input.

## 9. Análisis futuros incorporables (en orden de esfuerzo)

1. **Split de rangos por instancia**: los `site_stats` agregan min/max y mezclan
   zonas cuando un site lee dos tablas (la fuente ROM quedó oculta por esto). Un
   histograma grueso de direcciones por site (buckets de 256) los separaría.
2. **Firma de camino estructural**: hashear solo los branches cuya condición NO
   depende de datos de píxel (colisiones) reduciría los 1159 caminos de `$37974` a
   ~una docena de variantes semánticas.
3. **Sonido**: los `OUT` al beeper son el gemelo de las escrituras a pantalla — el
   mismo esquema (sites OUT + slice de valores) encontraría las tablas de melodías y
   efectos.
4. **F5 completo**: trace exacto por ventana de frames (log por instancia de TODOS los
   sites en un rango de frames elegido) para depurar un momento puntual píxel a píxel.
5. **Forward slicing**: "¿qué cosas dependen de esta celda?" — es transponer los
   edges; la DB ya lo permite (`edgesOut`), falta el comando.
6. **Detección de lógica**: colisiones (branches COND alimentados por AND de sprite
   contra pantalla — ya visible en las ecuaciones), RNG (celdas leídas y realimentadas
   sin INIT), timers (contadores por frame).
7. **Generalización multi-juego**: parametrizar el glue (clase convertida, entry
   point, bootstrap del RZX) en una config; el resto del pipeline no conoce JSW.
8. **UI**: un visor web sobre `analysis.db` (grafo de edges navegable + timeline de
   `sprite_draws` + memoria coloreada por regiones).

## 10. Límites conocidos (honestos)

- La agregación es la **unión** de toda la corrida: un site que hace dos cosas mezcla
  su evidencia (min/max, ver §9.1). El canal MEM es único por instrucción.
- Solo se ve **lo que la partida ejecutó**: contenido no visitado no existe para el
  sistema (es una propiedad, no un bug: todo lo afirmado fue observado).
- Los transforms de coordenadas son una lista finita de idiomas ZX (`(v&31)*8`,
  `v>>1`...); un juego con codificación exótica requeriría agregar el suyo (la
  votación hace el resto).
- Las corridas de análisis deben ser headless (`minizx.headless=true`, automático):
  la ventana del juego usa EXIT_ON_CLOSE y cerrarla mata la JVM en medio del análisis.
- Los conteos de `sprite_draws` son por invocación de rutina: Willy aparece como 16
  blits de 16×1 por frame (así lo dibuja el juego realmente); `sprites_found` los
  re-ensambla para el catálogo.
