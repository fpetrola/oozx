# Base semántica: de detección de sprites a recuperación de estructura

Estado: PARCIALMENTE IMPLEMENTADO (jul 2026). La cadena mínima al esqueleto §3.5 está
hecha y medida contra el oráculo en JSW: arnés (`jsw-oracle.json` + `OracleVerify`),
plano dir (§2, en `TaintReplay`/`OriginTaint` tras `-Ddir.plane=true`), capa 1
(`SemanticCapture` → `read_sets`/`draw_events`/`mem_profile`/`oracle_truth`),
`instances`+`deps` (`SemanticBuild`) y la visualización (`JSW3D`, perilla
`render.skeleton`). Quedan fuera de este corte: `mem_accesses`, §4-§6 (parametricidad,
variantes, COND), capa 3. Ver §2.5 por los hallazgos que CORRIGEN partes del diseño.

Este documento extiende `doc/DETECCION-SPRITES-3D.md`. Aquel describe cómo se
recuperan los sprites; éste describe de qué mecanismo general los sprites son un
caso particular, y qué más se recupera con el mismo instrumental.

---

## 0. La tesis

El detector de sprites no es *el* mecanismo. Es la primera proyección de un
mecanismo más general: instrumentar el sustrato computacional (procedencia de
valores, procedencia de direcciones, árbol de ejecución, identidad temporal) y
consultarlo.

"Qué bytes de pantalla vienen del catálogo, agrupados por estado de origen" es
una consulta sobre esos ejes. "Qué structs hay, qué campos tienen, qué rutinas
son genéricas sobre qué bloques, qué variantes tiene cada tipo, qué rutina es
loadRoom" son otras consultas sobre los mismos ejes.

El objetivo final es una **base semántica única** por juego, de la cual
`sprites_found` es apenas la primera tabla, y que el conversor Z80→Java consume
en vez de bytes crudos.

---

## 1. Los cuatro planos de observación

| Plano | Qué responde | Estado |
|---|---|---|
| **Valor** (value-taint) | de qué direcciones salió este byte | YA EXISTE (`OriginTaint`) |
| **Dirección** (dir-taint) | desde qué estado se calculó dónde escribir | FALTA (ver §2) |
| **Ejecución** (call tree) | qué invocación hizo esto | YA EXISTE (`writeNode`, `nodeParent`) |
| **Condición** (cond-taint) | qué dato eligió esta rama | FALTA, quirúrgico (ver §6) |

### 1.1 Lo que ya está resuelto en `TaintReplay`

No re-implementar; leer antes de tocar:

- **LDIR y familia**: por byte vía flag `bulk`; cada write se aparea con el read
  inmediatamente anterior. Detección de continuación por `pc == prevPc`.
- **Stack blits**: PUSH/POP NO se suprimen; sólo la maquinaria de direcciones de
  retorno de CALL/RET (`info.suppressMem && !push && !pop && !bulk`).
- **EXX / EX DE,HL / EX AF,AF'**: swaps de taint, no uniones. La unión genérica
  rompe los loops de copia por EXX (Dynamite Dan).
- **Poda "cortá lo profundo"**: en `union()`, al exceder `maxDepth` se conserva el
  lado *shallow* (el sprite fresco) y se descarta el acumulado. Discovery invierte
  el régimen (`-Dtaint.depth=512`) porque ahí las hojas viejas SON el catálogo.
- **Hash-consing** con `spriteOf`/`tileOf` cacheados por nodo: clasificación O(1).
- **Shadow stack por SP**, no por conteo de CALL/RET (sobrevive a PUSH addr/RET,
  a rutinas que popean su propio frame, y a interrupciones).
- **`memWrites`**: contador de escrituras por dirección. Discriminador de buffers.

---

## 2. El plano de dirección (prerequisito de todo lo demás)

### 2.1 Por qué está a tres cambios de distancia

En `Listener.beforeExecution` la propagación toma sólo los reads con rol VAL
(`info.roles...indexOf('V') >= 0`). Los reads con rol ADDR se descartan
deliberadamente — correcto para el plano valor, y es exactamente la información
que el plano dir necesita.

La procedencia de la dirección **ya se computa gratis**: mientras el juego arma
HL/DE con la posición de la entidad, esos cálculos son operaciones de valor, así
que `taint.reg[H]`/`taint.reg[L]` ya llevan la cadena hasta `$estado_X.y`. Sólo
se pierde en el momento del uso como dirección.

Núcleo del cambio: en el write listener, cuando `a` cae en pantalla, capturar la
unión de `taint.reg[slot]` para los slots con rol ADDR de esa instrucción, y
guardarla por byte igual que hoy se guarda `writeNode`. Un array más:
`dirTaint[PIXEL_BYTES]`.

Los round-trips por memoria (dirección guardada en variable y recargada) salen
gratis: el value-taint del byte que guarda la dirección ya lleva la procedencia.

### 2.2 Los tres cambios de política que sí hacen falta

1. **Loads de tabla indexada.** Bajo las reglas actuales el taint del valor leído
   es el origen del byte de la tabla; el índice que formó la dirección no entra.
   Para el plano dir hay que subir al índice. Esto exige un **segundo plano** con
   la regla de roles ampliada, no un retoque del existente: mezclar ADDR en el
   plano de valor contamina el catálogo con índices.

2. **Poda invertida.** Para dirección hay que conservar lo viejo/profundo (la
   variable de posición), lo contrario de la política actual. Hoy está hardcodeado
   en `union()` (`da <= db ? a : b`). Parametrizar por instancia de `OriginTaint`.
   Probablemente el plano dir necesite régimen de profundidad alta permanente.

3. **Automodificación.** El read listener descarta operandos inmediatos
   (`a >= curPc && a < curPc + curLen`) y fetches. Correcto para valor; para dir,
   cuando el juego parchea la dirección dentro del opcode, la procedencia vive en
   el `taint.mem` del byte de operando y al saltear esa lectura se corta la cadena.
   JSW casi no lo necesita; motores con sprites compilados o punteros parcheados, sí.

### 2.3 El discriminador se invierte entre planos

`TaintDiscover` filtra hojas con `memWrites` alto (un buffer reescrito no es un
gráfico). Para dir-taint es al revés: la hoja discriminante (`$estado_X.y`) se
escribe todos los frames — `memWrites` ALTO es la firma de un bloque de estado.
Mismo array, signo cambiado.

### 2.4 Decisión de diseño abierta

RESUELTA: una sola clase `OriginTaint` con política por instancia (`maxDepth`,
`keepDeep`). El costo medido en JSW: ~9x los nodos del plano valor, absorbido por un
memo primitivo (ver javadoc de `OriginTaint.memo`) — RZX de 60K frames en <3GB de heap.

### 2.5 Hallazgos de la implementación que corrigen el diseño (medidos en JSW)

1. **La poda invertida (§2.2.2) tal cual se FOSILIZA.** Una variable de estado
   actualizada por RMW gana +1 de profundidad POR FRAME aunque su conjunto de hojas
   converja; a ~500 frames toca el cap y desde ahí `keepDeep` descarta el operando
   fresco de cada unión: gfx, slots y tablas desaparecen de las hojas. La corrección
   es `OriginTaint.flatten`: cada STORE re-interna el nodo por su CONJUNTO de hojas
   (el historial de construcción no converge; el conjunto sí).
2. **La hoja discriminante NO es el buffer runtime** ($8100, memWrites alto, §2.3)
   sino el PAR DE SPEC de la data de sala ($C000+sala·256+240+2·slot): el buffer se
   escribe (initRoom copia spec→slot) antes de leerse jamás, así que su origin nunca
   entra a las cadenas. La identidad de instancia es (sala, par de spec), y la tabla
   de definiciones ($A000) da la identidad de tipo. El discriminador por memWrites
   quedó sin uso en JSW.
3. **La historia de transiciones de sala contamina TODO**: la procedencia del número
   de sala recursa por cada sala atravesada (~6 hojas por sala) y aparece en cada
   byte. Es procedencia verdadera pero no identidad; obliga a `dir.leafcap` alto
   (512 cubre el mapa entero, saturó 0 bytes) y a filtrar por zonas offline.
4. **`deps` vivo vs derivado**: tras soltarse de la soga, la posición de Willy sigue
   DERIVANDO de ella hasta el próximo reinit (arista "histórica", precisión 4%). El
   vínculo VIVO se detecta comparando la ventana deslizante de la tabla de animación
   ($83xx) que el follower arrastra contra la ACTUAL del líder: coinciden colgado,
   divergen al soltarse (la de Willy queda congelada). Precisión 74-96% / recall
   34-68% según sostén (knobs `build.*`); 6/7 episodios de colgada con arista y CERO
   aristas entre guardianes independientes.

   Verificado además: la fila de vidas sale con `dir_core_set = {$85CC}` — el
   contador de vidas documentado — sin buscarlo: la procedencia de dirección
   encuentra la variable sola. Pendientes medidos: las flechas casi no se cubren
   (207/225 fuera; transitorias, su identidad no pasa por el par de spec) y la
   cadena de la soga parpadea en el visor entre ticks de juego.

---

## 3. Resultados visibles (verificación, en orden de costo)

1. **Headless, dos guardianes idénticos → dos cajas.** El `main()` de
   `TaintReplay` ya imprime `[gfx=N x= y= w= h=]`. Hoy dos guardianes con el mismo
   bitmap salen como UNA entrada con bbox que abarca a los dos. Con dir-taint esa
   línea se parte en dos, cada una con su caja. Diff de texto, segundos. Smoke test.

2. **Contra el oráculo** (ver §8): la hoja discriminante debe caer DENTRO del slot
   correcto de la tabla de entidades, no cerca. Tres métricas por frame:
   precisión de atribución, conteo de instancias vs. guardianes activos, pureza
   (contaminación cruzada cuando dos se acercan).

3. **Overlay por instancia.** Colorear `owner[]` por hoja discriminante en vez de
   por base de catálogo. Dos guardianes idénticos, colores distintos, y cada uno
   MANTIENE su color mientras se mueve. La estabilidad temporal del color es la
   verificación visual de la identidad (slot, época). El reciclaje de slot se ve
   como cambio discreto de matiz (época incrementada), no como herencia.

4. **El cruce en 3D.** Dos sprites que se cruzan son en 2D una mancha ambigua.
   Con identidad por instancia van dos mallas completas a la escena y el z-buffer
   resuelve. Pausar en el cruce, mover la cámara: los píxeles que la pantalla
   original perdió están ahí, porque la forma viene de la definición y la pose de
   (slot, época). Imposible de fingir con clasificación por gráfico.

5. **Jerarquía como esqueleto.** Renderizar las aristas de dependencia como líneas
   entre instancias. La soga de JSW como cadena de segmentos colgando de un ancla.
   Links que no saltan entre objetos no relacionados = aristas reales.

---

## 4. Parametricidad (polimorfismo de datos)

**Definición**: mismo código, distinto bloque. `procesarEntidad` recibe IX
apuntando a `$8100` en una invocación y a `$8108` en la siguiente. El análogo Z80
de un método de instancia con `this` implícito en el registro índice.

Esto NO es una extensión del mecanismo: es consecuencia directa del plano dir.
El detector de sprites YA ES detección de parametricidad, restringida al caso
donde el efecto observable es un blit.

### 4.1 Firma formal

> R es paramétrica sobre una región ⟺ el conjunto de accesos de cada invocación
> es un traslado constante del de otra invocación, y la diferencia entre bases
> coincide entre todos los accesos de la misma invocación.

Da tres cosas de un golpe: el código genérico (R), el stride implícito (las
diferencias entre bases), y la población de instancias (las bases observadas).

### 4.2 Dos sabores, por procedencia del parámetro

- **Base desde variable de iteración** (caller hace `LD IX,tabla` + `ADD IX,DE` en
  loop): arreglo de structs procesado por loop. El caso JSW.
- **Base desde punteros distintos en sitios de llamada distintos**: misma rutina,
  estructuras heterogéneas, posiblemente NO contiguas. Éste es el caso que el
  clustering por stride no puede ver, y es la evidencia de tipo más fuerte que
  existe en el binario: si el mismo código lee offset +3 de ambos bloques, el
  campo +3 significa lo mismo en ambos. **El código compartido es la definición
  de tipo.**

### 4.3 Consecuencia: `structs` se deriva, no se infiere

El clustering por stride queda como señal SECUNDARIA. Un tipo es *el conjunto de
bases que fluyen por las mismas rutinas paramétricas*; sus campos son *los offsets
del patrón de acceso*. Dos regiones disjuntas unificadas por código compartido son
el mismo struct. El stride contiguo es caso particular (bases en progresión
aritmética), no definición.

Un arreglo tampoco es "direcciones con stride" (frágil: cualquier tabla lo cumple)
sino "región cuyas bases con stride son procesadas por la misma rutina paramétrica
dentro de un loop": estructura + código + iteración certificándose mutuamente.

---

## 5. Variantes (tipo suma etiquetado)

**El caso JSW**: stride fijo, mismo recorrido, misma rutina — pero un campo dice
el tipo (enemigo / soga / flecha) y las ramas tratan la estructura distinto. Los
mismos offsets SIGNIFICAN cosas distintas según la variante.

Es la composición de §4 (portador uniforme) con dispatch (variantes). Los dos
detectores no compiten: se componen, y la composición detecta lo que ninguno ve solo.

### 5.1 Firma observable

Dentro de la rutina paramétrica R el patrón de accesos por invocación NO es
uniforme, y ahí está la señal. Agrupando invocaciones por camino en el árbol, los
patrones se parten en familias: rama A lee offsets {0,1,2,3}, rama B lee {0,4,5,7}.

> Un campo en offset k es TAG ⟺ se accede en el prefijo común de todas las
> invocaciones, y particionar las invocaciones por el valor leído en k coincide
> con particionarlas por camino de ejecución y por patrón de accesos posterior.

Tres particiones independientes que deben coincidir (valor del tag / subárbol
ejecutado / campos accedidos). Redundancia de señales, como la época.

### 5.2 El slot vacío es una variante más

El terminador de la lista de guardianes (tipo 0 = "nada acá") deja de ser caso
especial: es la variante cuyo patrón de accesos posterior es vacío, y cuya rama es
el early-exit. Sin regla ad hoc.

### 5.3 Traducción a Java

Jerarquía sellada. `abstract class Guardian { int tipo; Position pos; }` con
`class Soga extends Guardian`, etc. — y el byte de tag DESAPARECE, absorbido por el
tipo de la clase, que es lo que un compilador hace con un sum type. Las ramas del
dispatch se vuelven overrides.

Es el punto donde la decompilación deja de producir "código que hace lo mismo" y
produce el diseño que el programador de 1984 aplanó a mano porque el Z80 no tenía
clases.

---

## 6. El plano COND, quirúrgico

La versión causal pura requiere taint de flags. Hay un atajo estadístico casi
gratis: leer el valor real de cada offset accedido en el prefijo y buscar el que
tiene información mutua máxima con el camino ejecutado.

Problema del atajo: correlación vs. causa (el puntero de gráficos también difiere
por tipo, así que también "predice" el camino).

**Desambiguación mínima, sin plano COND global**: instrumentar sólo los compares
DENTRO de rutinas ya identificadas como paramétricas. Cuando ejecuta un CP/BIT/AND
cuyo operando viene de memoria, el `pendingRead` del listener ya tiene las hojas
dir de ese operando; se guarda (rutina, hoja, offset) y cuando el salto condicional
inmediato ejecuta, se atribuye. En Z80 compare y branch son adyacentes, así que la
asociación por proximidad es confiable.

El estadístico encuentra candidatos; el compare-taint certifica cuál es el tag.

**Punto delicado abierto**: cascadas `CP n / JR / CP m / JR` (que es como JSW
despacha de hecho) en vez de jump table. La atribución por adyacencia hay que
diseñarla para eso.

### 6.1 Vocabulario

- **parametricidad**: mismo comportamiento sobre bloques intercambiables.
  → en Java: una clase, muchas instancias.
- **dispatch**: comportamiento distinto elegido por un tag.
  → en Java: jerarquía con override.

Son duales: uno es polimorfismo de *código* elegido por datos, el otro de *datos*
recorridos por el mismo código.

---

## 7. Esquema de la base semántica

Una sola SQLite por juego, extendiendo la existente (para que `SpriteCatalog` y el
pipeline actual carguen sin cambios). Cada tabla es salida de un plano, cada fila
lleva su EVIDENCIA (como ya hace la columna `methods`), cada tabla tiene su
verificador. Nada se infiere en cadena sin registrar de qué se infirió: si un
verificador falla, se sabe qué plano miente.

### Capa 1 — Hechos crudos (durante el replay)

```
read_sets(id, addrs)
  conjuntos de hojas internados. Todo lo demás referencia por id.

draw_events(frame, screen_lo, screen_hi, gfx_base, dir_leaf_set,
            node_addr, write_order)
  el evento de dibujado como registro de primera clase.
  TABLA FUNDACIONAL: todo lo de arriba es agregación de ésta, así que la capa 2
  se puede rediseñar sin re-replay.

mem_accesses(frame, node_id, routine_addr, addr, dir_leaf, is_write)
  necesaria para parametricidad (§4): accesos agrupados por INVOCACIÓN.
  Filtrable a las regiones de estado (memWrites alto) para no explotar.

routine_calls(frame, addr, parent_addr, count)
  árbol agregado por frame. El fingerprint de estados sale de acá.
```

### Capa 2 — Estructura inferida (pasadas offline sobre capa 1)

```
param_routines(addr, access_pattern_id, bases_seen, param_source, evidence)
  param_source: loop | multi_caller

structs(id, derived_from_routine, bases, stride_or_null, evidence)
  derivado de param_routines (§4.3), NO de clustering por stride

fields(struct_id, offset, size, role, variant_id, flow_targets, evidence)
  role: position | anim | type | unknown
  variant_id NULL = campo del prefijo común (header compartido)

variants(struct_id, tag_offset, tag_value, path_fingerprint, evidence)

instances(slot, epoch, struct_id, frame_first, frame_last, gfx_bases)
  evidence incluye qué señal disparó la época:
  reinit / discontinuidad / frontera de pantalla

deps(frame_range, instance_a, instance_b, kind, evidence)
  kind: follower | chase | compose
```

### Capa 3 — Semántica

```
routines(addr, role, evidence)
  role: blitter | loadRoom | death | init | decompressor

game_states(id, fingerprint, label)
state_transitions(from, to, trigger_event, frames)

events(frame, kind, signals, affected)
  kind: room_change | life_lost | init
```

### 7.1 Detectores de eventos (firmas)

- **Cambio de sala**: ráfaga de `memWrites` sobre zona de tiles + reinit masivo de
  value-taint en slots de entidades + salto en la fracción de pantalla redibujada.
  El ancestro común en el call tree de todas esas escrituras ES loadRoom.
- **Pérdida de vida**: reinit del slot del jugador SIN frontera de sala + posición
  que salta al punto de entrada + decremento de un byte. Ese byte se auto-identifica
  por convergencia: es el que la rutina de la fila de vidas LEE (su valor fluye a
  pantalla en la zona de status) y que el subárbol de muerte DECREMENTA. Se
  encuentra la variable de vidas y la rutina de muerte simultáneamente, cada una
  certificando a la otra.
- **Init**: rutinas ejecutadas una sola vez al principio cuyas escrituras son el
  origen del taint de todo lo demás. `memWrites` ya discrimina (escrito una vez
  temprano = init/descompresor).
- **Máquina de estados**: el conjunto de `nodeAddr` invocados por frame es un
  fingerprint; clusterizando frames por fingerprint salen menú / gameplay / muerte
  / game over, y las transiciones entre clusters son los eventos de arriba.

### 7.2 Dos decisiones fijadas

1. `draw_events` se escribe DURANTE el replay; capas 2 y 3 son pasadas offline.
   El replay instrumentado corre una vez, la inferencia itera barato. Precio:
   tamaño de `draw_events` (mitigable con interning agresivo y agregación de
   eventos idénticos consecutivos con contador).
2. El plano COND queda fuera del primer corte. Nada más depende de él y es el
   plano con más riesgo de ruido.

### 7.3 Decisión abierta

¿`draw_events.dir_leaf_set` guarda todas las hojas o sólo la discriminante?
Fija cuánto se puede rediseñar sin re-replay.

---

## 8. Verificación

Archivo `jsw-oracle.json` con los hechos documentados del desensamblado
comunitario de JSW, y una clase verificadora por capa que corre las queries y
emite precisión/recall POR TABLA (estilo `CallTreeProbe`: el éxito no es una
impresión, es un número).

Qué debe verificar cada tabla:

| Tabla | Test contra oráculo |
|---|---|
| `draw_events` | atribución vs. tabla de entidades leída del snapshot en el MISMO frame |
| `param_routines` | la rutina de proceso de guardianes sale paramétrica, con parámetro de loop |
| `structs` | aparece el buffer de entidades con su stride exacto |
| `fields` | los offsets de posición y frame de animación reciben el rol correcto |
| `variants` | tag_offset correcto; un variant por cada tipo que el RZX ejercitó; campos comunes con variant_id NULL y reinterpretados particionados |
| `instances` | conteo por sala vs. guardianes definidos en la data de sala |
| `deps` | la soga sale como cadena; ningún guardián independiente tiene aristas |
| `routines` | match EXACTO de dirección contra el desensamblado (la verificación más dura de toda la base) |
| `events` | life_lost decrementa el contador de vidas documentado |

**Test fino de parametricidad**: Willy, que en JSW vive en variables sueltas
separadas del buffer de guardianes, NO debe unificarse con ellos salvo que
efectivamente compartan rutinas. Algunas comparten, otras no; el mecanismo debería
recuperar exactamente esa frontera, ni más ni menos.

**Stress test de variantes**: la soga, que usa los offsets de manera completamente
distinta al resto.

### 8.1 Modo consistencia interna (juegos sin documentación)

¿Los strides dividen la región? ¿Toda instancia tiene struct? ¿Todo evento death
decrementa el mismo byte? Más débil, pero detecta degradación.

### 8.2 Cobertura: el límite que hay que reportar

Todo es tan completo como la partida grabada. Para variantes es más serio que para
sprites: un tipo suma incompleto genera una jerarquía Java incompleta que compila
igual. El verificador debe marcar los valores de tag **observados en memoria pero
nunca ejecutados** (slots cuyo byte de tipo tiene un valor sin variante
registrada): son ramas conocidas no exploradas, y dicen exactamente qué le falta
grabar al RZX.

Mitigación parcial para el catálogo: una vez conocida la zona y el formato, se
pueden enumerar los vecinos que la partida no mostró.

---

## 9. Orden de implementación

0. **Arnés de verificación primero** (`jsw-oracle.json` + runner). Autocontenido,
   no toca el listener, deja el medidor listo. Sin esto no se sabe si la primera
   versión del plano dir anda.
1. **Plano dir** en el listener (§2). Prerequisito de todo lo demás.
2. **`draw_events` + `read_sets`** (capa 1).
3. **`mem_accesses`** filtrada a regiones de estado.
4. **`param_routines` → `structs` → `fields`** con su verificador.
5. **`instances` + `deps`**.
6. **Capa 3**: eventos y máquina de estados.
7. **Plano COND** y `variants`.

---

## 10. Límites honestos

- El mecanismo recupera ESTRUCTURA, no nombres: "struct de 8 bytes, campo en +6
  que selecciona 3 variantes" es lo que da; "guardián.tipo" lo pone un humano.
- Cobertura del RZX (§8.2).
- Ambigüedad entre eventos con firmas parecidas (pausa vs. muerte) — se resuelve
  con señales redundantes, igual que la época.
- El núcleo (blit desde catálogo) cubre la familia mayoritaria de motores; degrada
  con gracia, no catastróficamente, en sprites compilados, gráficos vectoriales y
  composición isométrica con oclusión masiva.
