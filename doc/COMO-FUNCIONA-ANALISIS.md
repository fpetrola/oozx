# Cómo funciona el análisis de ecuaciones/dataflow de la ejecución RZX

**Propósito de este documento**: que cualquier persona (o IA) pueda retomar el proyecto
entendiendo qué pensamos, cómo lo fuimos construyendo y cuál es la idea general.
`doc/GUIA-ANALISIS-ECUACIONES.md` fue el *plan original*; este documento describe
**cómo quedó construido realmente** (hubo desvíos deliberados respecto de la guía) y
hacia dónde sigue.

---

## 1. La idea general (el "para qué")

Tenemos Jet Set Willy convertido automáticamente de Z80 a Java
(`translator/src/main/java/com/fpetrola/z80/bytecode/tests/JetSetWilly2.java`) y una
grabación RZX de una partida completa (`Jet Set Willy - Mildly Patched.rzx`, en la raíz
del repo). Al reproducir el RZX sobre el juego convertido, cada operación de memoria y
registro ocurre millones de veces con datos reales.

La idea central: **capturar por cada instrucción una "ecuación" (qué calcula) más los
rangos de datos observados (con qué valores) más las dependencias (de dónde vinieron esos
valores)**. Con esas tres cosas se puede deducir automáticamente el comportamiento del
juego sin conocimiento previo:

- La **ecuación** da el *cómo*: `mem[HL] = mem[DE] | mem[HL]` es un OR de sprite contra pantalla.
- Los **rangos** dan el *cuánto*: si ese site escribe addr ∈ [24576..28671], escribe en el backbuffer.
- Los **edges de dataflow** dan el *qué depende de qué*: la dirección HL se rastrea hasta
  la coordenada X del personaje en la tabla de entidades; el valor se rastrea hasta la
  tabla de sprites del snapshot original.

Encadenando edges hacia atrás desde una escritura en pantalla se reconstruye toda la
tubería: coordenada → cálculo de dirección → backbuffer → copia → pantalla. Eso es lo que
permite responder preguntas como "¿qué variable del juego determina dónde se dibuja
Willy?" o "¿por qué al perder una vida se dibuja un sprite menos?".

**Extensión en curso (acordada, ver §8)**: los edges hoy no distinguen si una dependencia
aportó a la **dirección** o al **valor** de un acceso. Tiparlos (ADDR/VAL/COND) separa el
árbol de "cómo se construyó la dirección" del de "cómo se construyó el byte", que es la
consulta que más información da sobre el juego.

## 2. Los tres principios de diseño (no negociables sin buena razón)

1. **Un site = una instrucción Z80 = una ecuación.** El código convertido tiene un
   marcador `pc(NNNNN, len)` antes de cada instrucción; el site ID **es la dirección Z80
   original** (0..65535). Todo el estado per-site vive en arrays planos de 64K — cero
   allocation en el hot path.
2. **Agregación por site, nunca log por instancia.** Un RZX completo son ~20583 frames y
   cientos de millones de operaciones; por site solo se acumulan conteos, min/max de
   addr/val, máscaras de bits AND/OR, primer/último frame, y edges con conteo. Las
   cadenas se reconstruyen componiendo las ecuaciones estáticas a través de los edges.
   (El trace exacto instancia-por-instancia queda para F5, por ventanas de frames.)
3. **El instrumentado debe ser semánticamente idéntico al original.** Se verifica
   comparando hashes de `mem[]` por frame entre la corrida original y la instrumentada
   (resultado actual: IDENTICAL en los 20583 frames, overhead ~12%). Cualquier cambio a
   la captura se re-verifica antes de confiar en los datos.

## 3. Cómo se construyó (crónica y desvíos respecto de la guía)

- **F1** (commit `d948afc2`/previos): la guía proponía instrumentar el "minimal"
  insertando llamadas Tracer por cada patrón con Spoon. **Desvío clave**: el objetivo
  pasó a ser `JetSetWilly2.java` (paquete tests, `extends MiniZX`), que ya tiene
  `pc(NNNNN,len)` por instrucción y métodos overrideables `mem(i,pc)` / `wMem(i,v,pc)`.
  Entonces la transformación Spoon quedó mínima (reescribir `mem[i]=v` → `wMem(i,v,pc)` y
  `mem[i]` → `mem(i,pc)`) y **toda la captura se hace por overrides en una subclase**
  (`RZXAnalysisRunner`), sin tocar más el código del juego. Bootstrap del RZX: replicar
  la lógica de PcInterceptor — forzar F en pc 34629/34637/34720/34726/34732 y cargar el
  snapshot en pc=34738 (está en `RzxBootstrap`).
- **F2** (commit `4d5906cd`): provenance completa. Modelo "las fuentes leídas entre dos
  `pc()` pertenecen a la instrucción actual": se acumulan en `curSrc` y se descargan como
  edges en el boundary. Registros como 23 slots (`regProv`), stack Z80 con
  snapshot de fuentes por push/pop, CFG dinámico, roots de IO (input del jugador).
- **F3+F4** (commit `67bb473a`): volcado a SQLite (`analysis/analysis.db`) y capa de
  análisis en Java (AnalysisDB, BackwardSlicer, SpriteFinder, RegionClassifier,
  CopyChainFinder, AnalysisCLI).

## 4. Componentes (todos en `translator/src/main/java/com/fpetrola/z80/analysis/`)

### Capa estática (se corre una vez, offline)
- **`EquationExtractor`** (Spoon): lee `JetSetWilly2.java`, reescribe los accesos
  `mem[...]` a `mem(i,pc)`/`wMem(i,v,pc)` resolviendo el pc del marcador `pc(N,len)`
  anterior más cercano, renombra la clase a `analysis/generated/JetSetWilly2Instrumented`
  y emite `translator/src/main/resources/analysis/sites.json` (~2575 sites): por site
  `{pc, method, line, kind, index, value, stmt}` con kinds `MEM_READ`, `MEM_WRITE`,
  `BULK_COPY`, `IO_IN`, `INSTR`, `BRANCH` (BRANCH = detección textual de `if (F()...)`).
  El texto `stmt` de cada site son las líneas fuente de esa instrucción: esa es la
  "ecuación" que después se compone en las cadenas.

### Capa de captura (corre junto al RZX)
- **`Tracer`** (estático, hot path): `lastWriterMem[64K]` (qué site escribió cada
  dirección; `SITE_INIT=0` = dato del snapshot), agregados W/R/bulk por site,
  `regProv[23]` (qué site escribió cada registro), `curSrc` (fuentes de la instrucción en
  curso), `edges`/`cfg` (`EdgeMap`: hash open-addressing long→count, sin boxing),
  `readsF[]`, `ioSites[]`, `stackProv`.
- **`RZXAnalysisRunner extends JetSetWilly2Instrumented`**: overrides de `mem`/`wMem`
  (→ `Tracer.rd`/`wr`), `ldir` (→ `Tracer.bulk`: agrega rangos, muestrea el
  `lastWriterMem` del origen para unir cadenas de copias, y marca el destino),
  `pc` (→ `Tracer.boundary`: descarga `curSrc` como edges y registra CFG),
  **todos** los accessors de registros de 8/16 bits y shadow (getter → `regRead`, setter
  → `regWrite`), los helpers que escriben F por adentro (`rlc/rrc/rl/rr/sl/sr` →
  `flagWrite`), `push`/`pop` (provenance por stack), `in` (root de IO), `cpir` (lee mem
  directo, se registra grueso). El `main` corre el RZX completo y al final vuelca
  `analysis/analysis-f1.json`, `analysis/instrumented-hashes.txt` y `analysis/analysis.db`.
- **`RzxBootstrap`** + **`FrameHasher`**: arranque del RZX y hash de `mem[]` por frame.
- **`BaselineRunner`** (corre el original y hashea) + **`VerifyF1`** (compara los dos
  archivos de hashes: debe dar IDENTICAL).

### Capa de análisis (sobre la DB, offline)
- **`AnalysisDump`**: esquema SQLite — `sites`, `site_stats` (op W/R por site),
  `bulk_stats`, `edges(src,dst,count)`, `cfg`, `flags(reads_f, io)`.
- **`AnalysisDB`**: carga todo en memoria; `describe(pc)` imprime site + rangos + ecuación.
- **`BackwardSlicer`**: BFS hacia atrás por edges desde los writers de un rango,
  componiendo ecuaciones; corta en `INIT` (dato original) e `IO` (input del jugador).
- **`SpriteFinder`**: writers de pantalla/backbuffer → slice a lecturas INIT-rooted =
  zonas de datos gráficos.
- **`RegionClassifier`**: reglas componibles sobre stats (estático/por-frame/contador/
  flag...) → mapa de regiones de los 64K. Incluye la regla por roles: lecturas cuyo
  flujo de salida tiene edges ADDR = **punteros/índices/coordenadas** (encuentra sola la
  tabla de entidades [33027..33075] y la tabla de punteros [33281..33489]).
- **`CopyChainFinder`**: encadena `bulk_stats` donde dst de uno solapa src de otro →
  pipelines de buffers.
- **`EquationLister`**: listado tipo "desensamblado algebraico anotado" — una línea por
  site con la **ecuación normalizada** (cadenas `varN` inlineadas por el pase 1.7 del
  extractor: `mem[HL] = A`, `A = (A - 8) & 255`, `B = (B - 1) & 255; while (B != 0)`),
  conteo de ejecuciones (del CFG) y rangos W/R observados. Las ecuaciones normalizadas
  viven en `sites.json` (campo `equation`) y en la columna `sites.equation` de la DB;
  `AnalysisDB` las prefiere sobre el stmt crudo, así que slices y reports las usan.
- **`AnalysisCLI`**: punto de entrada (ver §6).

## 5. Modelo de captura en una imagen

```
   pc(37974,..)  ←──────────── boundary: descarga curSrc→edges, cfg, currentPc=37974
   int var2 = DE();            regRead(D), regRead(E)  → curSrc += regProv[D], regProv[E]
   int var3 = mem(var2, 37978) rd: curSrc += lastWriterMem[var2]; stats R del site 37978
   A(var3);                    regWrite(A) → regProv[A]=37978... (site actual)
   pc(37985,..)  ←──────────── boundary del site anterior
   wMem(var5, var4, 37985)     wr: stats W, lastWriterMem[var5]=37985
```

Los saltos entre rutinas quedan cubiertos porque cada canal de transporte tiene su
mecanismo: **memoria** (`lastWriterMem`), **registros** (`regProv`), **stack Z80**
(`push/pop` con snapshot de fuentes), **copias en bloque** (`bulk` muestrea el origen).

## 6. Cómo correr todo

```bash
# 1. (solo si cambió JetSetWilly2.java o el extractor) regenerar instrumentado + sites.json
mvn -q -pl translator exec:java -Dexec.mainClass=com.fpetrola.z80.analysis.EquationExtractor

# 2. correr el RZX completo instrumentado (~9s) → analysis/analysis.db + hashes + json
mvn -q -pl translator compile
mvn -q -pl translator exec:java -Dexec.mainClass=com.fpetrola.z80.analysis.RZXAnalysisRunner

# 3. verificación de identidad semántica (obligatoria tras tocar captura/extractor)
mvn -q -pl translator exec:java -Dexec.mainClass=com.fpetrola.z80.analysis.BaselineRunner
mvn -q -pl translator exec:java -Dexec.mainClass=com.fpetrola.z80.analysis.VerifyF1

# 4. explorar
mvn -q -pl translator exec:java -Dexec.mainClass=com.fpetrola.z80.analysis.AnalysisCLI \
    -Dexec.args="slice 16384 22527"        # también: sprites | regions | copychains | site <pc>
#   slice lo hi [depth] [fanout] [addr|val|cond]   — slice filtrado por rol
#   equations [metodo | pcLo pcHi]                 — listado de ecuaciones normalizadas
#   track [rzxPath]                                — pipeline completo de posiciones (§9)
#   positions frameLo [frameHi]                    — sprites dibujados + pares coordenada
#   explain lo hi [depth] [fanout]                 — narrativa recursiva de un rango (§9bis)
```

## 7. Qué ya se dedujo del juego (sanity checks vivientes)

Estos hallazgos salieron de las recetas automáticas y sirven para validar cambios
(si después de un cambio dejan de aparecer, algo se rompió):

- `$37974` es la rutina de dibujado de sprites; lee gráficos de **[39680..48382]** y
  escribe contra un **backbuffer [24576..28671]** además de pantalla.
- Doble buffer: **28672→24576→16384** (1× por frame) y atributos **24064→23552→22528**.
- **33824** = habitación actual (5 writers, val 27..56). **34252** = vidas (init 7,
  dec en 35899).
- Tabla de entidades viva en **[33024..33088]** (registros de 8 bytes copiados desde
  [40960..41976]); tabla de punteros 16-bit en **[33280..33489]**; fuente ROM en 15616
  usada por `$38555`.
- El slice de pantalla muestra la cadena dirección ← tabla punteros [33280..33488] ←
  `mem[IX+3]` de la tabla de entidades (coordenada), cruzando `$37974`/`$35211`/`$37310`.

## 8. Edges tipados por rol (ADDR / VAL / COND) — IMPLEMENTADO (2026-07-12)

**Problema que resuelve**: los edges `src→dst` no decían *en qué rol* llegó la
dependencia al consumidor — si alimentó la **dirección** del acceso, el **valor**
escrito/transferido, o la **condición** de un branch. En el slice las ramas salían
entreveradas.

**Diseño** (mitad dinámica + mitad estática):
1. **Canal dinámico**: cada dependencia se registra junto con el canal por el que entró
   al site consumidor: el slot de registro (A,F,B,C,...,R = slots 0..22 de `regProv`),
   `MEM` (valor leído de memoria) o `STK` (llegó por pop). `curSrc` pasa a pares
   (site, canal); el edge pasa a `(src, dst, canal) → count` (en el key long del EdgeMap
   sobran bits: src y dst son PCs de 16 bits).
2. **Rol estático por (site, canal)**: como un site es una instrucción, la ecuación dice
   cómo usa cada canal. El `EquationExtractor` hace un mini def-use dentro del site
   (siguiendo los locals `varN`): canal que fluye al índice de `mem[...]` → **ADDR**;
   al valor escrito / a un setter de registro / a push → **VAL**; a una condición de
   `if`/`while` → **COND**. Un canal puede tener varios roles en un site
   (ej. `mem[HL] = mem[HL] | A`: H,L=ADDR, MEM=VAL, A=VAL). Se emite en `sites.json`
   como campo `roles`.
3. **Join al volcar**: `AnalysisDump` resuelve el rol de cada edge por
   (site destino, canal) → columnas `ch` y `role` en la tabla `edges` (+ tabla
   `site_roles`). El `BackwardSlicer` gana filtro (`slice lo hi ... addr|val|cond`) y
   etiqueta cada edge impreso.

**Qué habilita**: separar "así se construyó la dirección" (→ coordenadas, índices,
punteros) de "así se construyó el byte" (→ sprites, máscaras, buffers); reglas del
RegionClassifier exactas ("leída alimentando cálculos de dirección → puntero/índice");
consultas tipo "¿qué celdas de memoria terminan influyendo en direcciones de pantalla?".

**Sanity check (verificado en la implementación)**: en `$37974` (write site 37985,
`mem[HL]=A`), `slice 16384 22527 3 3 addr` muestra solo H:ADDR/L:ADDR rastreando a la
aritmética de pantalla, `$35211` y la tabla de punteros [33280..33489] de `$37310`;
`slice ... val` muestra A:VAL ← `mem[DE]` con la tabla de sprites [39680..48382] ← INIT,
y el OR contra el backbuffer alimentado por el ldir 28672→24576. Identidad semántica
re-verificada: IDENTICAL 20583 frames. Detalles de implementación: canales en
`Tracer.CH_NAME` (25: 23 slots de registro + MEM + STK); key del `EdgeMap` =
`src<<32|dst<<16|ch`; roles estáticos calculados en el pase 1.6 del `EquationExtractor`
(campo `roles` en sites.json, ej. `"D=A;E=A;MEM=V"`); tablas SQLite `edges(src,dst,ch,
role,count)` y `site_roles(pc,ch,role)`; el filtro de rol del slicer aplica solo al
primer salto (los niveles profundos se recorren completos, etiquetados).

**Límite conocido (aceptado)**: sigue siendo el grafo de relaciones agregado sobre toda
la corrida — la unión de "qué depende de qué" con conteos y rangos — no la reconstrucción
de una instancia puntual (ese es F5). `MEM` es un solo canal: dos lecturas de memoria en
la misma instrucción comparten canal (el rol queda como conjunto).

## 9. F5 dirigido — "track": posiciones de sprites por frame — IMPLEMENTADO (2026-07-13)

**Qué resuelve**: la captura agregada da rangos por site, no valores por instancia; con
esto solo se sabía *dónde viven* las coordenadas, no *dónde está cada sprite en cada
frame*. El comando `track` cierra ese hueco de punta a punta y **sin interpretación
manual**: ningún paso requiere leer un slice a ojo y decidir qué observar.

**Pipeline (una sola invocación, `AnalysisCLI track`)**:
1. **`CoordinateFinder`** deriva de la DB de agregados, automáticamente:
   regiones tipo-pantalla (pantalla + todo buffer bulk-copiado hacia ella, transitivo,
   con el delta que mapea cada buffer sobre la pantalla real), métodos de dibujado
   (dueños de write-sites que tocan esas regiones), **celdas a observar** (rangos de RAM
   chicos y mutables cuyas lecturas alimentan la DIRECCIÓN de esos writes — BFS hacia
   atrás con primer salto por edges ADDR) y tablas de consulta estáticas.
   (Si `analysis/analysis.db` no existe, corre primero la pasada de agregados.)
2. **Re-corrida con `TrackLog`** (`SpriteTracker.TrackRunner extends RZXAnalysisRunner`):
   log por instancia SOLO de lo descubierto — cada write de los draw-sites, cada entrada
   a un método de dibujado y cada cambio de celda observada, en un único stream ordenado
   de longs empaquetados (~3M eventos, +0 s de overhead perceptible). Se auto-verifica:
   hashes por frame IDENTICAL contra la corrida de agregados.
3. **Clustering offline**: un cluster = una invocación de rutina de dibujado (marcada por
   su entry); sus writes se decodifican con el layout ZX (backbuffers incluidos vía los
   deltas de región) → tabla **`sprite_draws(frame, method, kind P/A, x, y, w, h, ...)`**
   con la posición real de TODO lo dibujado en los 20583 frames.
4. **Correlación automática** (la "interpretación" hecha por el sistema): votación afín
   por (celda, eje, transform) con filtro de posiciones distintas ≥10, y la validación
   decisiva **conjunta**: un par de celdas vecinas es (X, Y) real si EL MISMO cluster
   satisface ambas a la vez (azar ≈1%/frame vs ~30% por eje suelto). Se prueban los
   top-3 transforms por eje y se aplica exclusividad greedy (cada celda pertenece a su
   mejor par). Salidas: **`coord_pairs`**, `coord_cells`, `coord_tables`,
   **`frame_cells`** (valores por frame de las celdas observadas).

**`AnalysisCLI positions <frameLo> [frameHi]`**: imprime por frame los draws reales al
lado de las posiciones predichas por los pares (aplicando los transforms a los valores
de `frame_cells` de ese frame) — la coincidencia se ve sola.

**Hallazgos automáticos verificados (JSW2)**:
- Willy: **X=mem[34259]** (`(v&31)*8`), **Y=mem[34255]** (`v>>1`), par validado al 95%;
  lo dibuja `$38504` fila por fila (16 clusters de 16×1 por frame).
- Tabla de entidades: slot 0 = **(mem[33026], mem[33027])** (38% de los frames — solo
  cuando el slot está activo), slot 1 = (33034, 33035), slot 2 = (33042, ...); X con
  `(v&31)*8`, Y con `v>>1`/`(v&240)>>1` (guardianes verticales guardan Y en medios píxeles).
- Sanity vivo: en el frame 12001 el par del slot 0 predice (200,96) y `sprite_draws`
  tiene `$37974 P (200,96) 16x16`; el par de Willy predice x=96 y Willy está en x=96.
- Los pares con confianza baja (≤7%) son ruido de correlación entre guardianes — el
  porcentaje reportado los delata; no se filtran para no ocultar evidencia.

**Límite conocido**: la muestra impresa al final usa un frame temprano que puede caer en
el modo demo (celdas con valores que no corresponden a sprites en pantalla); `positions`
sobre frames de gameplay muestra la correspondencia exacta.

## 9bis. "explain" — la consulta narrativa (2026-07-13)

**Qué resuelve**: las preguntas "¿quién escribe acá? ¿de dónde salen el valor y la
dirección? ¿a través de qué buffers pasó? ¿cuál es el dato original y cuál la variable
que cambia?" requerían encadenar `slice addr` / `slice val` / `copychains` / `site` a
mano. `explain lo hi` hace el recorrido completo en un paso:

- **Roots**: ecuaciones que escriben el rango Y copias en bloque cuyo destino lo toca
  (el `slice` también incluye ahora bulk-roots).
- Primer nivel agrupado por rol: **DIRECCION / VALOR / CONDICION** — cómo se construyó
  cada cosa.
- Cada lectura de memoria cruzada se **clasifica en el momento**:
  `ESTATICA` (nunca escrita = cassette), `DINAMICA` (con cuántas ecuaciones la
  escriben), `MIXTA` (p.ej. la zona de sprites [39680..48382]: `88% nunca escrita,
  dinamica en [40962..41978] [42159..42239]` — JSW2 persiste estado de guardianes y
  flags de ítems dentro de la zona de datos), y si `track` corrió, anota
  "coordenadas de sprite: X=mem[33026] Y=mem[33027]..." y "serie por frame en frame_cells".
- Las ramas cortan en **DATO ORIGINAL DEL CASSETTE (INIT)** o **INPUT DEL JUGADOR (RZX)**.

Demo que responde todo junto (`explain 24576 28671`): backbuffer ← ldir 28672→24576 ←
compositor `$36171` ← layout de habitación [32929..32981] ← copia desde [56064..] ← INIT;
y en VALOR del sprite: tabla [39680..48382] MIXTA ← INIT. `explain 33024 33088` muestra
la tabla de entidades: carga por copias desde [40960..41977] indexadas por
`HL = habitación*8` (tres `add16(HL,HL)`), movimiento en `$37056` con stride 8, y las
celdas-coordenada anotadas.

## 10. Roadmap / ideas pendientes

- **F5 completo — trace exacto por ventana de frames**: log por instancia de TODOS los
  sites en una ventana, para trace pixel-a-pixel puntual (el `TrackLog` de §9 es la
  versión dirigida de esto; la infraestructura queda lista).
- **Forward slicing** ("¿qué cosas dependen de X?"): es transponer los edges, la DB ya
  lo permite (`edgesOut`).
- **Refinar geometría de sprites**: entry size desde la estructura de loops
  (iteraciones × lecturas por iteración), frames de animación desde máscaras del índice.
- **Generalizar a otros juegos**: nada del pipeline depende de JSW salvo el bootstrap
  del RZX (`RzxBootstrap`) y las direcciones de los sanity checks.

## 11. Decisiones ya tomadas (no re-preguntar)

- Vehículo: transformación Spoon mínima + captura por overrides en subclase (no wrappers
  simbólicos, no edición manual, no ByteBuddy).
- Site ID = dirección Z80 real; granularidad = agregado por site + edges con conteo.
- Salida: SQLite (`analysis/analysis.db`); análisis en Java en el mismo repo.
- push/pop se instrumentan para no cortar provenance entre rutinas.
- Edges tipados por rol (§8): canal dinámico + rol estático, join al volcar. El taint
  por valor (transportar pares valor+provenance) se descartó por invasivo; la inferencia
  de rol puramente estática se descartó por ambigua.
- Verificación de identidad semántica obligatoria tras cualquier cambio de captura.