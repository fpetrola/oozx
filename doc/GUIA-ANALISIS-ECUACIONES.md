# Guía de implementación: Análisis por ecuaciones algebraicas de la ejecución RZX

**Objetivo**: instrumentar `JetSetWilly2Converted.java` (conversión automática Z80→Java de Jet Set Willy)
para capturar, durante la reproducción de un RZX completo, ecuaciones algebraicas de cada operación
de memoria/registro con sus rangos de datos y dependencias, y con eso deducir automáticamente:
zonas de memoria, tipos de datos, tablas de sprites, flujos sprite→pantalla, trace hacia atrás
desde un pixel hasta su dato de origen.

Esta guía es autocontenida: contiene el diseño completo ya acordado con el usuario. Implementar
en el orden de las fases del final.

---

## 1. Contexto del repo (verificado)

- Repo: `/home/fernando/detodo/spectrum/oozx`, módulo Maven `translator`.
- **Código convertido**: `translator/src/main/java/com/fpetrola/z80/bytecode/tests/minimal/JetSetWilly2Converted.java` (~3413 líneas).
  - `public static int[] mem = new int[0x10000]` — memoria como campo estático.
  - Los **registros Z80 son variables locales** de cada método (`int A; int F; int HL;`...), NO campos.
    Fluyen entre rutinas como parámetros y retornos `int[]` (ej: `int[] r1 = $37974(C, DE, HL); F = r1[0]; DE = r1[1];`).
  - Rutinas = métodos `$NNNN()` donde NNNN es la dirección Z80 original. Entry point del juego: `$34463()`.
  - Helpers estáticos que encapsulan semántica Z80: `_rlc`, `cp`, `inc`, `inc16`, `dec`, `dec16`, `add`, `add16`, `flagZ`, `sra`, `carry`, más `push`/`pop` (stack Java `Stack<Integer>`).
  - Los LDIR ya están convertidos a: `while ((BC--) != 0) mem[DE++] = mem[HL++];`
  - Los flags se modelan con `F` como int y branches tipo `F = cp(A, 211); if (F != 0) {...}`.
- **Ejecución del RZX**: `translator/src/main/java/com/fpetrola/z80/bytecode/tests/rzzx/GameRZXInvokerConverted.java`
  usa `RZXPlayerIO` (`translator/src/main/java/com/fpetrola/z80/minizx/RZXPlayerIO.java`) que alimenta
  los `in()` desde la grabación. `RZXPlayerIO.getCurrentFrameIndex()` da el frame actual;
  `changeFrame()` (privado) avanza de frame. RZX usado en pruebas previas:
  `/home/fernando/detodo/desarrollo/m/zx/roms/recordings/jsw/Jet Set Willy - Mildly Patched.rzx`.
- **Spoon ya es dependencia** (`spoon-core 10.4.3-beta-21` en `translator/pom.xml`). Patrón de uso de
  referencia: `bytecode/tests/minimal/SpoonZ80Transformer.java` (Launcher + processors + TypeFilter).
- Rutina clave verificada: `$37974(int C, int DE, int HL)` (línea ~2815) **es la rutina de dibujado de
  sprites**: loop B=16, lee `mem[DE]`, testea colisión `mem[DE] & mem[HL]`, escribe `mem[HL] = mem[DE] | mem[HL]`,
  con la aritmética clásica de layout de pantalla ZX sobre HL. Caller ejemplo: `$35211` (línea ~1301)
  computa `DE = (157<<8) | (rlc³(mem[34273]) & 96)` antes de llamarla.

## 2. Arquitectura (3 capas, paquete nuevo `com.fpetrola.z80.analysis`)

```
equations/EquationExtractor      Spoon processor: genera JetSetWilly2Instrumented.java + sites.json
runtime/Tracer                   runtime estático de captura: shadow state + agregados por site
runtime/AnalysisDump             vuelca los agregados a SQLite al terminar el RZX
RZXAnalysisRunner                main: corre el RZX completo con la clase instrumentada
db/AnalysisDB                    carga SQLite y expone queries
analysis/BackwardSlicer          backwardSlice(addrRange) → cadenas de ecuaciones
analysis/RegionClassifier        classifyRegions() → mapa de 64K clasificado
analysis/CopyChainFinder         findCopyChains() → pipelines de bulkCopy encadenados
analysis/SpriteFinder            findSprites() → tablas de sprites con geometría deducida
```

**Principio central**: agregación **por site estático** (un site = una operación en el fuente,
~miles), nunca log por instancia dinámica (miles de millones en un RZX completo). Cada site
acumula conteos, rangos min/max, máscaras de bits y edges hacia otros sites. Las ecuaciones son
plantillas estáticas; las cadenas dinámicas se reconstruyen componiendo plantillas a través de los
edges. (Un modo "trace exacto por ventana de frames" queda para fase posterior; la infraestructura
de sites lo permite sin rediseño.)

## 3. Capa 1 — EquationExtractor (Spoon)

Entrada: `JetSetWilly2Converted.java`. Salidas:
1. `JetSetWilly2Instrumented.java` (mismo paquete o `com.fpetrola.z80.analysis.generated`) —
   semánticamente idéntico al original, con llamadas a `Tracer` insertadas.
2. `sites.json` — tabla estática de ecuaciones: `{id, method, line, kind, equation, notes}` donde
   `equation` es el pretty-print Spoon de la expresión original.

Site IDs secuenciales. **Site 0 = SITE_INIT** (reservado: representa la carga inicial de memoria
en `init()`).

### Reglas de transformación (por patrón, usando TypeFilter sobre CtArrayWrite/CtArrayRead/CtAssignment/CtIf/CtDo/CtWhile)

| # | Patrón fuente | Transformación | kind |
|---|---|---|---|
| R1 | `mem[expr] = valor` | `Tracer.wr(SID, expr, valor, srcSite)` inmediatamente antes del store. `srcSite` = provenance del valor (ver shadow locals abajo) | `MEM_WRITE` |
| R2 | lectura `mem[expr]` dentro de una expresión | extraer a local: `int tN = mem[expr]; Tracer.rd(SID, expr);` y sustituir la lectura por `tN`. La provenance del resultado = `Tracer.lastRead` (el tracer resuelve `lastWriterMem[expr]`) | `MEM_READ` |
| R3 | asignación a local-registro `A = expr` | tras la asignación: shadow local `A$s = Tracer.assign(SID, <sites de operandos>)` | `REG_ASSIGN` |
| R4 | `if (F ...)` / condición de `while`/`do-while` sobre F | `Tracer.branch(SID, F != 0, F$s)` antes del branch — liga el branch al site que produjo F | `BRANCH` |
| R5 | `while ((BC--) != 0) mem[DE++] = mem[HL++];` (detectar el patrón exacto) | reemplazar por `Tracer.bulkCopy(SID, HL, DE, BC+1);` seguido del loop original (o un `System.arraycopy` + actualización de HL/DE/BC equivalente). Un solo registro por ejecución del loop, no por byte | `BULK_COPY` |
| R6 | llamada `$NNNN(args)` | `Tracer.call(SID, NNNN, <shadow de args>)` antes; `Tracer.ret()` captura provenance de retorno | `CALL` |
| R7 | `push(v)` / `pop()` | `Tracer.push(v$s)` / `x$s = Tracer.pop()` — provenance a través del stack | `STACK` |
| R8 | `in(BC, ...)` | `Tracer.in(SID, port)` — marca origen externo (input del RZX) | `IO_IN` |

### Provenance de locales (shadow locals)

Como los registros son locales, por cada local `X` usada el processor declara `int X$s = -1;` al
inicio del método y la actualiza en cada asignación con el site que produjo el valor. Para el paso
inter-método (R6/R7): `Tracer` mantiene arrays thread-static `argProv[]` / `retProv[]` que el caller
llena y el callee lee (el processor inserta la lectura al inicio del método instrumentado y antes
de cada `return new int[]{...}`).

Los helpers (`cp`, `add`, `_rlc`, `flagZ`...) NO se instrumentan por dentro: la ecuación del site
que los invoca ya captura la operación completa.

**No instrumentar**: `init()`, `getProgramBytes()`, helpers estáticos, `createScreen`.
Al final de `init()` insertar `Tracer.initDone()` (marca toda la memoria con SITE_INIT).

## 4. Capa 2 — Tracer (runtime)

Todo con arrays planos y primitivas; cero allocation en el hot path.

```java
public final class Tracer {
  static int[] lastWriterMem = new int[0x10000]; // site que escribió cada dirección
  // por site (indexado por SID, dimensionado desde sites.json):
  static long[] execCount;
  static int[] addrMin, addrMax, valMin, valMax;
  static int[] addrBitsOr, addrBitsAnd, valBitsOr, valBitsAnd; // para stride/alineación/máscaras
  static int[] firstFrame, lastFrame;
  // edges (src→dst) con conteo: mapa long( (src<<32)|dst ) → count.
  //   Usar HashMap<Long,long[]> o fastutil Long2LongOpenHashMap si está disponible.
  // branches: taken[], notTaken[] por SID + rangos de operandos de la comparación asociada.
  // bulk copies: por SID acumular srcMin/srcMax/dstMin/dstMax/lenMin/lenMax/count.
  static int currentFrame; // actualizado desde RZXPlayerIO (exponer hook público o polling de getCurrentFrameIndex())
}
```

Operaciones:
- `wr(sid, addr, val, srcSite)`: actualizar agregados del sid, edge `srcSite→sid`, `lastWriterMem[addr]=sid`.
- `rd(sid, addr)`: agregados + edge `lastWriterMem[addr] → sid`; devuelve/expone ese writer como provenance.
- `assign(sid, srcs...)`: edges de cada src al sid; retorna sid (nueva provenance del local).
- `branch(sid, taken, condSrcSite)`: conteos taken/notTaken + edge condSrcSite→sid.
- `bulkCopy(sid, src, dst, len)`: agregados de rangos; `Arrays.fill(lastWriterMem, dst, dst+len, sid)`;
  edge agregado con el writer *predominante* del rango fuente (muestrear origen: basta lastWriterMem[src]).
- Al terminar el RZX (RZXPlayerIO tiene flag `stop` / fin de frames): `AnalysisDump.dump("analysis.db")`.

## 5. Esquema SQLite

```sql
CREATE TABLE sites(id INTEGER PRIMARY KEY, method TEXT, line INT, kind TEXT,
                   equation TEXT, branch_context TEXT);
CREATE TABLE site_stats(site_id INT, exec_count INT, addr_min INT, addr_max INT,
                        addr_bits_or INT, addr_bits_and INT, val_min INT, val_max INT,
                        val_bits_or INT, val_bits_and INT, first_frame INT, last_frame INT);
CREATE TABLE edges(src_site INT, dst_site INT, count INT);
CREATE TABLE branches(site_id INT, taken INT, not_taken INT,
                      op1_min INT, op1_max INT, op2_min INT, op2_max INT);
CREATE TABLE bulk_copies(site_id INT, count INT, src_min INT, src_max INT,
                         dst_min INT, dst_max INT, len_min INT, len_max INT);
CREATE TABLE calls(caller_site INT, callee_addr INT, count INT);
```

Driver: `org.xerial:sqlite-jdbc` (agregar a `translator/pom.xml`).

## 6. Capa 3 — Análisis (Java sobre la DB)

### BackwardSlicer
`slice(addrLo, addrHi)`: sites MEM_WRITE con rango ⊆ [lo,hi] → BFS hacia atrás por `edges`
componiendo `sites.equation` en cada paso → lista de cadenas tipo:
`mem[16384..22527] = mem[DE] | mem[HL]  ←  DE = 40192+{0,32,64,96}  ←  mem[34273] = inc(mem[34273])`.
Cortar en SITE_INIT (dato original del juego) o IO_IN (input del jugador).

### SpriteFinder (receta automática, sin conocimiento de JSW)
1. Sites MEM_WRITE con addr ⊆ [16384, 22527] (pantalla ZX) ordenados por exec_count.
2. Para cada uno, slice hacia atrás hasta lecturas cuyo `lastWriter=SITE_INIT` → **zonas de datos gráficos**.
3. Geometría desde la estructura: iteraciones del loop contenedor × lecturas por iteración =
   bytes por entrada (ej: B=16 × 2 = 32 bytes = sprite 16×16). Máscaras del índice en el caller
   (`& 96` → 4 frames de 32 bytes) = cantidad de frames de animación.
4. Emitir: `{base, entrySize, width, height, indexedBy: <ecuación del índice>, callSite}` por cada caller.
5. Los branches tipo `(mem[DE] & mem[HL]) != 0` en la misma rutina = detección de colisión por pixel.

### RegionClassifier (reglas componibles, cada una una query; diseñar para agregar reglas sin tocar la captura)
| Evidencia | Clasificación |
|---|---|
| lastWriter=SITE_INIT toda la ejecución, leída por draws/copies | datos estáticos (gráficos/niveles) |
| escrita ~1 vez por frame | estado por frame |
| leída por site cuya ecuación alimenta cálculo de dirección | puntero/índice |
| leída de a pares consecutivos hacia registro de 16 bits (stride 2 en addr_bits) | tabla de punteros 16-bit |
| rango de valores {0,1} y solo usada en branches | flag |
| ecuación de escritura `mem[x]=inc(mem[x])` | contador |
| leída con `& 31` alimentando byte bajo de dirección de pantalla | coordenada X |
| destino de bulkCopy dependiente de una variable (ej. `H = mem[33824] \| 192`) | buffer conmutado; la variable = selector (nº de habitación) |

### CopyChainFinder
Encadenar `bulk_copies` donde dst de uno solapa src de otro → pipelines
(en JSW: datos de nivel → backbuffer → pantalla; atributos 38912 → 22528).

## 7. Verificación (obligatoria antes de confiar en los datos)

1. El instrumentado debe ser **semánticamente idéntico**: correr `GameRZXInvokerConverted`
   (original) y `RZXAnalysisRunner` (instrumentado) sobre el mismo RZX y comparar un hash de
   `mem[]` cada N frames (ej. N=100). Cualquier divergencia = bug de transformación; arreglar antes de seguir.
2. Sanity checks de los datos: S de `$37974` debe mostrar addr de escritura ⊆ pantalla;
   `SpriteFinder` debe encontrar la tabla en página 157 (40192) que usa `$35211`.
3. Presupuesto de performance: overhead aceptable 5–20×; si excede, revisar allocation en hot path.

## 8. Fases de implementación (commits separados)

1. **F1 — Extractor mínimo + verificación**: EquationExtractor solo con R1/R2 (memoria),
   Tracer con wr/rd + lastWriterMem, RZXAnalysisRunner, verificación de identidad semántica (7.1).
2. **F2 — Provenance completa**: R3–R8 (shadow locals, branches, bulkCopy, calls, stack, io),
   edges, frames.
3. **F3 — Dump SQLite + AnalysisDB**.
4. **F4 — Análisis**: BackwardSlicer, SpriteFinder, RegionClassifier, CopyChainFinder +
   sanity checks (7.2).
5. **F5 (opcional)** — modo trace exacto por ventana de frames para trace pixel-a-pixel
   instancia por instancia.

## 9. Decisiones ya tomadas con el usuario (no re-preguntar)

- Vehículo: transformación Spoon (no wrappers simbólicos, no edición manual).
- Granularidad: agregado por site + edges (trace exacto por ventanas = fase 5).
- Salida: SQLite; análisis en Java en el mismo repo.
- Los edges guardan conteo; bulkCopy guarda rangos src/dst.
- push/pop sí se instrumentan (R7) para no cortar provenance entre rutinas.
