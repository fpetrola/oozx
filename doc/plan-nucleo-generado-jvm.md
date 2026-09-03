# El núcleo generado, más rápido en la JVM: la máquina adentro del `case`

Segunda fase del núcleo generado. La primera (`plan-nucleo-generado.md`) dejó las instrucciones,
MEMPTR y la contención aplanadas en `GeneratedZ80.java`; ésta mira qué le sigue costando a la JVM
adentro de esa clase y, sobre todo, qué pide todavía afuera —la memoria con sus bancos, la
contención de la ULA, el reloj, el loop de la máquina— y cómo meterlo adentro con el mismo
mecanismo: especializar el código que ya existe, no escribir otro.

Medido el 2 de septiembre de 2026 sobre la rama `nucleo-generado`, JDK 21.0.2 (C2), con
`-XX:+PrintCompilation -XX:+PrintInlining` sobre `CoreBenchmark` corrido directo, y con el perfil
JFR de la reproducción RZX de JSW de la sesión anterior.

**El principio no cambia.** El modelo no se adapta a la herramienta. La memoria de la Spectrum, la
ULA y el reloj son el modelo de la máquina como las instrucciones son el modelo del Z80: el
generador los lee y los especializa; no se reescriben "versiones rápidas" a mano. Lo único que se
toca son mejoras de OOP que valen solas, y se hacen antes.

## Lo que cuesta una instrucción hoy

### Adentro: la forma del generado

| qué | cuánto |
|---|---|
| líneas / `case` / métodos | 27.421 / 1.616 / 112 (7 tablas × 16 grupos, 7 despachos, `step`) |
| método más grande | 3.547 bytes de bytecode (`decodeDDCB_1`); ninguno pasa los 8.000 de HotSpot |
| `memory.read` / `memory.write` / `contend` | 1.035 / 603 / 1.321 sitios, todos llamadas |
| `io.*` / `state.*` | 30 / 30 sitios |
| `& 0xFFFF` / `& 0xFF` | 4.472 / 3.052 |
| `"BC".equals("BC")` y parecidos | 57 |
| `InterruptionMode.values()[n]` | 8 |
| `Integer` (boxing) | 4 |
| `default: throw new IllegalStateException(...)` | 109 |

Lo que el JIT hace con eso, leído de `PrintInlining` sobre `CoreBenchmark` (memoria simulada,
`contend` vacío):

- **Todo lo caliente llega a C2.** `step`, `decode`, los `decode_N` del programa, `decodeCB_1`,
  `decodeDD_7`, `decodeED_11`: tier 4. Nada corre interpretado; el límite de 8.000 bytes está
  bien resuelto.
- **`decode_N` nunca se inlinea.** `GeneratedZ80Cpu.execute` → `step` → `decode` se inlinean
  entre sí ("inline (hot)"); cada `decode_N` sale "too big" / "hot method too big" (1.100–3.500
  bytes contra `FreqInlineSize=325`). Una instrucción es dos `tableswitch` y una llamada real.
- **`memory.read` es una llamada de interfaz por sitio.** En el benchmark cae en
  `AbstractMemory.read`, que crea una lambda por lectura para avisar a los listeners
  (`AbstractMemory$$Lambda::<init>` inlineado más de 120 veces, "callee uses too much stack" 174). El
  benchmark mide la memoria simulada tanto como el núcleo.
- **Corrido directo da 184–197 M instr/s**, contra los 133–148 de la tabla de la fase 1 y los
  167–173 (con una vuelta en 95) del mismo test bajo surefire. La explicación más probable es que
  el mismo JVM corre antes el núcleo OOP sobre la misma `AbstractMemory.read` y los perfiles de
  esos sitios se comparten. El benchmark tiene que correr cada núcleo en su propio JVM y sobre un
  `int[]`; hasta entonces, sus números tienen ese margen.

### Afuera: lo que la máquina hace por cada acceso

El `memory` del generado en la máquina es el `Memory` anónimo de `Z80.initNoTest`. Una lectura:

1. `memory.readByteInternal(address)`: `mapRead[address >>> 11]`, `page.get(address & 0x7FF)`
   (`page[offset + index]`, con bounds check).
2. `if (!disabled)`: el flag del debugger.
3. `memory.readByte(address, ula)`: **la misma búsqueda de página otra vez**, y si es contendida
   `zxClock.addTStates(ula.contention[zxClock.getTStates()], "ula readbyte")`.
4. `zxClock.addTStates(fetching == 1 ? 4 : 3)`.
5. Cada `addTStates` es `tStates += n; if (timeoutProcessor != null) timeoutProcessor.accept(n)`:
   un `Consumer<Integer>`, o sea `Integer.valueOf(n)` más una llamada de interfaz, en cada suma,
   desde el primer `setTimeout` de la cinta en adelante.

Una escritura hace todo eso y además **escribe dos veces**: `memory.writeByte(...)` busca la
página en `mapWrite`, aplica la contención, decide `isWritable() || (source != sourceNone &&
settings.current.writableRoms)`, pasa por `displayDirtySinclair` (cuatro comparaciones y una
lectura de la página) y guarda; después `memory.writeByteInternal2(address, b)` busca la página
otra vez, decide `writable` otra vez y guarda otra vez. La segunda existe para el caso
`disabled`, y corre siempre.

Una contención (`FusePhaseProcessor.contend`): búsqueda de página, y `times` veces
`ula.addUlaStates(tstates)` → `contentionNoMreq[tStates & 0xFFFFF] + tstates` → `addTStates` →
`Consumer<Integer>`. El parámetro `Kind` no se usa.

Y alrededor de cada instrucción, `Z80.doOpcodes` → `step` → `OOZ80.execute`:
`zxClock.getTStates() < eventNextEvent`, la lectura volátil de `emulatorPaused`,
`bridgeCommand.invoke(0, null)` (la lambda del conector: dos campos, un `instanceof` y la cola de
comandos), el `try`, `beforeFetch.armed() || afterInstruction.armed()`, la llamada virtual
`ooz80.execute()`, `isActiveNMI`, `isIntLine`, la llamada virtual `execute(1)`, `step()`, y a la
vuelta `isPendingEI` con `null instanceof EI`. Unas diez cargas y cuatro o cinco llamadas por
instrucción, fuera del núcleo.

**Una iteración de LDIR en JSW** (código en 0x8000, sin contención; destino en pantalla):

| acceso | búsquedas de página | sumas al reloj (cada una por el `Consumer`) |
|---|---|---|
| fetch ED, fetch B0 | 4 | 2 |
| read (HL) | 2 | 1 |
| write (DE), contendida | 2 (+ dos `writable`, dos `set`, `displayDirty`) | 2 |
| `contend(DE-1, 2×1)` | 1 | 2 |
| `contend(DE-1, 5×1)` mientras BC ≠ 0 | 1 | 5 |
| **total** | **10** | **12** |

Con el perfil de la sesión anterior (760 fps, 1,3 ms por frame): LDIR 31 % ≈ 400 µs para unas
3.000 iteraciones, **~130 ns por iteración**; la síntesis del AY 15 %; el avance del RZX 9 %;
`addTStates(int, String)` 5 %; el marcado de pantalla 4 %. Sin llamadas opacas, una iteración de
LDIR es siete lecturas de tabla y unas sumas: 15–25 ns. Eso es lo que hay para ganar en la CPU de
JSW; el resto del frame es AY, pantalla y RZX, y no es de este plan.

## Lo que el JIT hace y no hace

Los hechos de HotSpot en los que se apoya el plan, para verificarlos con `PrintInlining` y no
adivinar:

- **Presupuestos de inlining** (JDK 21, x86_64): `MaxInlineSize=35` bytes para un sitio frío,
  `FreqInlineSize=325` para uno caliente, `InlineSmallCode=2500` bytes de código *compilado* para
  un callee que ya tiene versión C2 ("already compiled into a medium method"),
  `DesiredMethodLimit=8000` bytes acumulados por compilación, `MaxInlineLevel=15`,
  `HugeMethodLimit=8000` (no compila). Un helper de 30–60 bytes se inlinea en todos los sitios
  calientes; un `decode_N` de 3.000 no se inlinea nunca.
- **Una llamada opaca recarga todos los campos.** C2 no puede saber que `memory.read` no toca `H`
  ni `L`, así que después de cada llamada vuelve a leer los registros de memoria. Mientras haya
  una llamada en el `case`, A..L, PC, SP y F viven en el heap, no en registros de máquina. Es la
  razón de fondo para inlinear la memoria: no el costo de la llamada, sino lo que la llamada le
  hace al resto del `case`.
- **Interfaz monomórfica = guard de tipo + inline; polimórfica = llamada real.** Los perfiles son
  por sitio de bytecode: los sitios del generado son suyos, pero los de `AbstractMemory.read` se
  comparten con el OOP cuando los dos corren en el mismo JVM (tests, benchmark).
- **`& 0xFF` justo antes de indexar una tabla de 256 saca el bounds check.** Ésos se quedan. En
  cualquier otro lugar es una instrucción de más, porque C2 no conoce el rango de un campo.
- **Boxing a través de una interfaz no se elimina.** `Consumer<Integer>.accept(n)` es
  `Integer.valueOf` (cacheado en -128..127, alocado afuera) y una llamada de interfaz; el escape
  analysis no la ve.
- **Un `switch` denso es un `tableswitch`**; dos niveles son dos saltos indirectos. Un método por
  opcode inlineado en un `switch` de 256 no cabe en `DesiredMethodLimit`, así que termina siendo
  un salto y una llamada igual. La forma actual no es el problema; se mide, pero se espera poco.
- **Una excepción cuesta microsegundos** (construir el stack trace, y acá además imprimirlo). Un
  opcode que cae en `default: throw` cuesta lo que mil instrucciones.

## Los puntos, adentro de la clase

Cada uno dice qué se ve, por qué le cuesta a la JVM, qué se hace y dónde vive el cambio. Ninguno
toca el modelo: son reglas del generador o pasadas del simplificador.

### A1. Los terminales opacos

Todo `memory.read`, `memory.write` y `contend` es una llamada de interfaz o abstracta. Por lo de
arriba, cada una vale su costo más la recarga de los registros. Es el punto principal y se
resuelve afuera (B1–B3) especializando también la máquina; adentro, lo que hace falta es que el
generador pueda emitir esos accesos como **helpers privados de la clase generada** en vez de
llamadas a un campo: `read3(address)`, `fetch4(address)`, `write3(address, value)`,
`contendN(address)`, con el cuerpo que salga de especializar el `Memory` de la máquina. Chicos
(30–60 bytes), no virtuales, C2 los inlinea en cada sitio caliente sin guard, y los `decode_N` no
crecen. Dónde: `CoreGenerator` (emite helpers en vez de `MethodCallExpr` sobre `memory`),
`Specializer.terminalName` (los terminales pasan a ser un parámetro: ganchos, o los objetos de
una máquina).

### A2. Rangos que el generador sabe y C2 no

```java
int value1_9 = B & 0xFF;          // B siempre está en 0..255
F = _F6 & 0xFF;                   // _F6 ya está enmascarado
PC = _nextPC300 == -1 ? (PC + 3) & 0xFFFF : _nextPC300;   // JP: nunca es -1
MEMPTR = nextPC_620 == -1 ? 0 : nextPC_620;               // ídem
```

Los campos de registro son de 8 o 16 bits por construcción (toda escritura sale enmascarada o de
`memory.read`, que devuelve 0..255 por contrato) y los `nextPC` de los saltos incondicionales
salen de `(operand << 8) | operand`. C2 no conoce ninguno de esos rangos: son campos no finales y
el resultado de una llamada. El generador sí. Se agrega al especializador un hecho de rango por
expresión (registro de 8 bits, de 16, lectura de memoria, `(byte)` → -128..127, `x & m`, `x >>> n`,
`a | b`), y `Folder` pliega `x & 0xFF` cuando el rango ya cabe, `x == -1` cuando el rango no lo
incluye, y con eso el ternario. Se conservan las máscaras que preceden a un índice de tabla
(`SZ53[x & 0xff]`), que son las que le sirven a C2. Son 3.052 + 4.472 sitios; por instrucción,
tres a seis instrucciones de máquina y dos saltos menos en los saltos incondicionales. Dónde:
`Folder` (rangos), `Specializer` (declarar los rangos de los terminales y de los slots).

### A3. Lo que el simplificador deja

```java
int _address3 = 0;  _address3 = (B << 8) | C;               // inicializador muerto
_F14 = value2_28;  _F14 = value2_28;                        // asignación repetida
int jumpAddress2_614 = (_jumpAddress300 = ...); _jumpAddress300 = jumpAddress2_614;   // tres veces lo mismo
int wordNumber_652 = SP;  SP = (wordNumber_652 + 2) & 0xFFFF;                         // alias de un campo
if (("BC".equals("BC") || "BC".equals("DE"))) MEMPTR = ...;  // 57 veces
state.setIntMode(InterruptionMode.values()[1]);              // clona el array de la enum
Integer wordNumber_4099 = MEMPTR;                            // boxing en el final de LDIR/LDDR
```

C2 elimina los stores muertos y las copias solo; lo que no elimina es el `values()` (alocación) ni
el boxing (alocación fuera de -128..127), y el `String.equals` depende de que pliegue un
intrínseco con dos constantes, que no está garantizado. Pero el argumento principal es otro: **el
bytecode es el presupuesto**. Cada byte de más en un `case` es un byte menos de helper que C2
acepta inlinear en ese método, y `DesiredMethodLimit` es acumulado. Se agregan a `Simplifier` la
eliminación de stores muertos (asignación sobreescrita sin lectura entre medio), la propagación de
copias entre asignaciones puras, y a `Folder` el `equals` entre literales y `Enum.values()[n]` con
`n` literal. Los `Integer` salen del especializador cuando el tipo del local del modelo es
`Integer` (`MemptrUpdater`): declara `int`. Dónde: `Simplifier`, `Folder`, `Specializer.declare`.

### A4. La contención condicional, fusionada al `if` de la instrucción

```java
if ((B != 0)) { ...; _nextPC26 = jumpAddress2_49; } else { _nextPC26 = -1; }
MEMPTR = nextPC_51 == -1 ? 0 : nextPC_51;
if (_nextPC26 != -1) contend((PC + 1) & 0xFFFF, 5, 1, READ_NO_MREQ);
if (_nextPC26 == -1) contend((PC + 1) & 0xFFFF, 1, 3, READ);
PC = _nextPC26 == -1 ? (PC + 2) & 0xFFFF : _nextPC26;
```

Cuatro decisiones sobre el mismo bit. En fuente es poco —JR, JR cc y DJNZ en las tres tablas (18
pares) y las ocho instrucciones de bloque repetitivas de ED— pero es de lo que más ejecuta un
programa. El tejido (`CoreGenerator.weave`) pone `IF_JUMPED` / `IF_NOT_JUMPED`
después de la ejecución porque no mira adentro; pero cuando la ejecución termina en un
`if (cond) { nextPC = X } else { nextPC = -1 }`, "saltó" es la rama. Se teje adentro de cada rama,
y con el rango de A2 el `MEMPTR` y el `PC` finales también se meten en las ramas: queda un solo
`if`. Dónde: `CoreGenerator.weave` (reconocer el patrón de asignación de `nextPC` en las ramas).

### A5. Direcciones recalculadas alrededor de llamadas

```java
_address84 = (H << 8) | L;
int value1_201 = memory.read(_address84, 0) & 0xFF;
...
_address84 = (H << 8) | L;                // otra vez, después de la llamada
contend(((H << 8) | L), 1, 1, READ_NO_MREQ);   // y otra
```

C2 no puede unificarlas: entre medio hay llamadas y `H` y `L` son campos. El generador sabe que
el `case` no escribe `H` ni `L` entre las dos, y puede reusar el local. Es una CSE de expresiones
puras sobre campos que el `case` no asigna en el tramo. Con A1 hecho desaparece por sí sola (sin
llamadas, C2 la hace), así que va después y sólo si queda algo. Dónde: `Simplifier`.

**Lo que esta CSE nunca puede tocar** es nada que salga de la tabla de páginas: `mapRead[...]`,
`mapWrite[...]`, los campos de un `MemoryPage`, ni el `int[]` de una página. Un `OUT` pagina en
medio de la instrucción y el que sigue tiene que ver la página nueva. La regla, en el generador,
es la de la frontera de más abajo: eso vive adentro de un helper y no sale de ahí.

### A6. La forma del despacho

Hoy: `execute()` → `decode` (inlineado, `switch (opcode >> 4)`) → `decode_N` (llamada real,
`switch (opcode)`). Dos saltos indirectos y una llamada por instrucción. Las alternativas —un
`switch` de 256 con un método por opcode, o grupos de 8— terminan también en un salto y una
llamada, porque el conjunto no entra en el presupuesto de C2. Se mide con `CoreBenchmark` sobre
`int[]` (paso 0) probando `GROUP_SHIFT` 3, 4 y 5 y un método por opcode; se queda el mejor y no se
espera más de unos puntos. Lo que sí importa del tamaño: con los helpers de A1 adentro, los
`decode_N` crecen; `GROUP_SHIFT` es el regulador. Dónde: `CoreGenerator.GROUP_SHIFT`.

### A7. El marco por instrucción

`OOZ80.execute()` hace por instrucción cuatro lecturas de `state` y dos llamadas virtuales para
llegar a `core.step()`, y a la vuelta chequea `isPendingEI` sobre un `instruction` que para el
generado es siempre `null`. El núcleo generado ya tiene `state`; el chequeo de NMI e INT es suyo
también. Se genera un `run(int untilTStates)` en la clase (o en `GeneratedZ80Cpu`) que hace el
loop `while (tStates < until) { nmi/int; step(); }` con los chequeos adentro, sin pasar por
`OOZ80.execute` ni por `execute(int)`. Es la contraparte de B5. Dónde: `CoreGenerator` (el marco
es a mano; agrega `run`), `GeneratedZ80Cpu`.

### A8. Los opcodes indefinidos tiran

`decodeED` sólo tiene los grupos 4, 5, 6, 7, 10 y 11; ED 00–3F, 80–9F y C0–FF caen en
`default: throw new IllegalStateException(...)`, y `Z80.doOpcodes` lo atrapa e imprime el stack
trace. Un programa que ejecute uno (hay protecciones que lo hacen; en el hardware es un NOP de
8 T) paga microsegundos por instrucción y llena la consola. **No es del generador**: el núcleo OOP
hace lo mismo con un `NullPointerException` sobre `wrappers[opcode]`. El lugar es el modelo:
`EDPrefixTableOpCodeGenerator` devuelve para esos opcodes una instrucción de 2 bytes y 8 T, y el
generado la hereda. Es la decisión abierta "ED indefinidos" de la fase 1, ahora con un costo
puesto.

### A9. `read(x, 1)` contra `read(x, 0)`

El segundo argumento distingue fetch de operando y vale 4 o 3 T-states en la máquina, decidido
con un `if` por lectura. Con A1 son dos helpers (`fetch4`, `read3`) y el literal queda en el
sitio. Sin A1 no vale nada.

### Lo que no se toca

Las tablas DD y FD duplican la base (3.400 líneas cada una) y DDCB/FDCB (5.500). Es tamaño de
fuente, no de camino caliente: cada método se compila cuando se calienta y el code cache sobra.
Queda como estaba en la fase 1.

## Los puntos, afuera de la clase

### B1. La memoria de la máquina, especializada

El `Memory` anónimo de `Z80.initNoTest` es el candidato a especializar: su `read` y su `write`
llaman a `com.fpetrola.oozx.Memory` (páginas de 2 K en `mapRead`/`mapWrite`, cada una un
`MemoryPage` con `int[] page`, `offset`, `contended`, `writable`, `source`), a `Ula` (las tablas
`contention` y `contentionNoMreq`, `byte[]` por máquina, rellenadas en `machineChanged`) y a
`SpectrumZ80Clock`. Todo eso es código plano sobre campos: el especializador lo inlinea como
inlineó las instrucciones, con los objetos vivos en la mano. El paginado sigue funcionando sin
invalidar nada, porque el helper lee `mapRead[address >>> 11]` en cada acceso y lo que cambia al
paginar son las entradas del array, no el array. Lo que hay que sacar antes, como mejora de OOP
(M2, M3): la segunda búsqueda de página en `readByte`, la segunda escritura de `write`, y el
`disabled` del debugger, que no tiene por qué pasar por el camino del núcleo.

### B2. El reloj

`addTStates` es `tStates += n` más un `Consumer<Integer>` desde que la cinta pidió un timeout, y
tiene tres sobrecargas (`int`, `int + String`, `int + Supplier<String>`) de las que dos ignoran la
descripción. Doce llamadas por iteración de LDIR pasan por ahí. La mejora de OOP (M1) lo deja en
`tStates += n` y una comparación, y entonces el especializador lo inlinea a una suma sobre un
campo `int` que, sin llamadas en el `case`, C2 mantiene en un registro.

### B3. La contención de la ULA

`FusePhaseProcessor.contend` es exactamente lo que hay que inlinear: `if (page.contended) times ×
(tStates += contentionNoMreq[tStates] + tstates) else tStates += times * tstates`. Con `times`
literal en cada sitio, el loop se desenrolla (1 a 7 lecturas de tabla). El helper `contendN` sale
de especializar ese método, no de escribirlo; el `Kind` que no se usa desaparece solo.

### B4. El marcado de pantalla en cada escritura

`displayDirtySinclair` decide con `source == sourceRam && pageNum == currentScreen &&
(offset & mask) < 0x1b00 && page.get(a) != value`, y `writeByteInternal` decide `writable ||
(source != sourceNone && settings.current.writableRoms)`, en cada escritura, en dos llamadas. Las
dos son propiedades de la página que cambian sólo al paginar, al cambiar de pantalla o al cambiar
la configuración (M3): un `screen` y un `writable` efectivos por `MemoryPage`, y la escritura
queda `if (p.writable) { if (p.screen && p.page[i] != v) display.dirty(i); p.page[i] = v; }`. La
llamada a `display` queda como llamada, en la rama que casi nunca corre.

### B5. El loop de la máquina

Lo listado arriba: por instrucción, el chequeo de eventos, el volátil de pausa, el bridge, los
traps, el `try`, y las dos llamadas virtuales hasta el núcleo. Con A7 el núcleo ofrece
`run(until)`; `Z80.doOpcodes` lo llama cuando no hay traps armados ni debugger, y conserva el
camino actual cuando los hay (`beforeFetch.armed() || afterInstruction.armed()`, que ya es un
booleano). La pausa y el bridge se chequean cada N instrucciones con un contador en el loop, no en
cada una: los eventos registrados son el fin de frame, la interrupción, el timer y los del FDC, así
que "entre eventos" puede ser un frame entero (20 ms a 100 %), demasiado para una pausa. Con
N = 256 la latencia es de unos 100 µs a velocidad máxima. Es una decisión de la máquina: la
granularidad del debugger pasa de instrucción a tramo salvo cuando está conectado.

### B6. Lo que no es la CPU

El AY se sintetiza aunque el sonido esté apagado (15 % del frame de JSW), la pantalla se
renderiza en `presentFrame`, y el RZX avanza contando fetches por R (9 %). Después de A y B, eso
es lo que queda en JSW, y es de la máquina, no del núcleo. Se anota para que la tabla final no
sorprenda.

## El destino

El núcleo puro (`GeneratedZ80`, ganchos abstractos) sigue existiendo: es lo que Fuse verifica
evento por evento y lo que el benchmark mide. Al lado aparece **el núcleo de la máquina**
(`GeneratedSpectrumZ80`, en `machine/core`), generado por el mismo generador con los terminales
apuntando a los objetos de la máquina en vez de a ganchos. Lo que cambia se ve en un `case`:

```java
// hoy
case 0x77: {
    int _address84 = 0;
    _address84 = (H << 8) | L;
    memory.write(_address84, A);
    if (("HL".equals("BC") || "HL".equals("DE"))) { MEMPTR = ...; }
    PC = (PC + 1) & 0xFFFF;
    break;
}

// después
case 0x77: {
    write3((H << 8) | L, A);
    PC = (PC + 1) & 0xFFFF;
    break;
}
```

y en los helpers que el generador saca de `Memory`, `Ula` y `SpectrumZ80Clock` (los nombres son
ilustrativos; el cuerpo es lo que el especializador produzca de esas clases):

```java
private int read3(int address) {
    MemoryPage p = mapRead[address >>> 11];
    if (p.contended) clock.tStates += contention[clock.tStates & 0xFFFFF];
    clock.tStates += 3;
    return p.page[p.offset + (address & 0x7FF)];
}

private void write3(int address, int value) {
    MemoryPage p = mapWrite[address >>> 11];
    if (p.contended) clock.tStates += contention[clock.tStates & 0xFFFFF];
    clock.tStates += 3;
    if (p.writable) {
        int i = p.offset + (address & 0x7FF);
        if (p.screen && p.page[i] != value) display.dirtySinclair(i);
        p.page[i] = value;
    }
}

private void contend2x1(int address) {
    if (mapRead[address >>> 11].contended) {
        clock.tStates += contentionNoMreq[clock.tStates & 0xFFFFF] + 1;
        clock.tStates += contentionNoMreq[clock.tStates & 0xFFFFF] + 1;
    } else clock.tStates += 2;
}
```

`mapRead`, `mapWrite`, `contention`, `contentionNoMreq`, `clock` y `display` son campos `final`
de la clase generada, recibidos en el constructor: son los objetos vivos de la máquina, no
literales. Con eso, LDIR en pantalla contendida pasa de diez búsquedas de página y doce llamadas
de interfaz a siete lecturas de tabla y ninguna llamada; y como no hay llamadas, `H`, `L`, `D`,
`E`, `B`, `C` y `tStates` viven en registros durante todo el `case`.

El loop:

```java
public void run(int until) {           // generado; reemplaza a doOpcodes cuando no hay traps
    while (clock.tStates < until) {
        if (state.isActiveNMI()) { ... }
        if (state.isIntLine() && state.isIff1()) interruption();
        R = (R + 1 & 0x7f) | regRBit7;
        decode(fetch4(PC));
    }
}
```

## La frontera: qué se congela y qué se lee

Es la pregunta que decide si esto funciona. El especializador de la fase 1 **evalúa el grafo vivo
en tiempo de generación**: un campo primitivo `final` de un objeto vivo se vuelve un literal, uno
no final se vuelve un *slot*, o sea una copia que a partir de ahí vive en la clase generada, y un
objeto se vuelve otro `Obj` en el que sigue entrando. Para una instrucción eso es exactamente lo
que se quiere, porque su grafo no cambia nunca después de construida la tabla. Para la máquina es
al revés: casi todo lo que la memoria toca cambia mientras el emulador corre.

Si se apuntara el especializador de hoy a la memoria de la máquina sin más, saldría esto:

| lo que lee el camino de acceso | qué es | qué haría hoy el especializador | qué tiene que hacer |
|---|---|---|---|
| `PAGE_SIZE_LOGARITHM` | `final int` = 11 | literal `11` | **literal**, está bien |
| `mapRead`, `mapWrite` | `final MemoryPage[]` | `UnsupportedOperationException("instance array")` | campo `final` inyectado, acceso a array |
| `page.contended`, `.offset`, `.writable`, `.source` | campos mutables de `MemoryPage` | *copia* con el valor que tenían al generar | **lectura de campo** en cada acceso |
| `page.page` | `int[]` reemplazable con `setPage` | copia de la referencia | **lectura de campo**, después índice |
| `ula.contention`, `contentionNoMreq` | `final byte[]`, se rellenan al resetear la máquina | excepción | campo `final` inyectado, acceso a array |
| `memory.currentScreen`, `screenMask`, `sourceRam`, `sourceNone` | `int` mutables | copia | **lectura de campo** |
| `clock.tStates` | `int` mutable que leen la ULA, la cinta, los eventos | copia, y el reloj de la máquina se queda atrás | **el contador vive en el reloj**, nunca se copia |
| `settings.current.writableRoms` | objeto reemplazable | congela el `Settings.current` de hoy | lectura, o M3 lo saca del camino |

O sea: **la regla de hoy es exactamente la equivocada para la máquina**, y por eso la frontera hay
que declararla, no dejarla salir sola. La regla que la define:

> Se congela lo que no puede cambiar mientras esta instancia del núcleo exista: la forma del
> algoritmo y las constantes de forma. Se lee en cada acceso todo lo que la máquina muta: la tabla
> de páginas, los campos de una página, las tablas de contención y el contador de T-states. Un
> objeto que la máquina pudiera *reemplazar* no se congela nunca: entra como campo `final` al
> constructor, y quien lo reemplace tiene que reconstruir el núcleo.

### Por qué alcanza: la máquina muta, no reemplaza

Lo verifiqué en el código, porque de esto depende todo lo demás.

**Cambio de máquina.** `Machine.selectMachine` no reconstruye nada: pone `current`, avisa a los
`MachineChangeListener`, pone el reloj en cero y resetea el `EventManager`. `Memory`, `Ula`,
`Display`, `SpectrumZ80Clock` y `Z80` son singletons de Guice, creados una vez; `Z80.start()`, que
es donde se arma el envoltorio de memoria y el núcleo, corre una sola vez desde `Speccy`. El reset
de la máquina rellena `ula.contention[i]` y `contentionNoMreq[i]` **en el mismo array**, y llama a
`current.memoryMap()`, que remapea entradas de `mapRead` y `mapWrite`; `ramSet16kContention` prende
y apaga `contended` **en los mismos `MemoryPage`**. Un núcleo con referencias `final` a esos
objetos ve el cambio de 48K a 128K sin enterarse de que hubo uno.

**Paginado desde el asm.** `OUT (0x7FFD),A` entra por `io.out`, que sigue siendo una llamada opaca
al bus de periféricos, y termina en `Spec128.memoryPortWrite` → `memoryMap()` → `memory.map16k`,
que reemplaza entradas de `mapRead` y `mapWrite`. El núcleo lo ve porque cada acceso vuelve a
hacer `mapRead[address >>> 11]`; no hay nada cacheado entre accesos. Lo mismo vale para el
`/ROMCS` de los periféricos: DivIDE, ZXCF y la TR-DOS paginan con `mapRomcs8k` y compañía, que es
la misma mutación de la misma tabla.

Ese es el hallazgo de fondo, y es lo que hace que el enfoque cierre: **todo el modelo de bancos de
esta máquina es "mutar la tabla de páginas"**, y una tabla que se lee en cada acceso sobrevive
intacta al inlining. Lo que no sobreviviría es un modelo que reemplazara el objeto memoria al
paginar; no es el caso.

### Lo que hay que proteger

- **Invariante**: ningún cambio de máquina, de configuración o de periférico puede reemplazar el
  `Memory`, la `Ula`, el `Display` ni el reloj. Si alguna vez hace falta, quien lo reemplace
  reconstruye el núcleo, como reconstruye hoy el `OOZ80`. Un test lo fija: con el núcleo generado,
  arrancar en 48K, cambiar a 128K, paginar RAM con un `OUT` y verificar que una lectura ve la
  página nueva y la contención nueva. Hoy no existe ese test para ningún núcleo.
- **Nada de estado de páginas fuera de un helper.** El helper busca la página, la usa y la
  descarta; no hay locals de página que crucen dos accesos, ni CSE que los cruce (A5).
- **`clock.tStates` no se copia.** Es del reloj y lo leen la ULA, la cinta y los eventos. El
  generador lo trata como terminal compartido, no como slot. Nota práctica: hoy es `protected` en
  `DefaultZ80Clock`, así que una clase generada en otro paquete no puede tocarlo; o se hace
  público, o —más simple— el reloj se deja como llamada, que después de M1 son seis bytes que C2
  inlinea siempre.

## Quién es el dueño del código que queda adentro

La tercera pregunta, y la más cara: **sí, los helpers de `GeneratedSpectrumZ80` son código de
`machine/core` inlineado, y si ese código cambia, el generado cambia.**

En volumen es poco: el archivo sigue siendo 95 % instrucciones, y los helpers son la
especialización de `Memory.readByte`, `writeByte`, `writeByteInternal`, `displayDirtySinclair`,
`FusePhaseProcessor.contend`, `Ula.addUlaStates` y `SpectrumZ80Clock.addTStates`, unas cien líneas
de máquina repartidas en diez o veinte métodos. En consecuencias es mucho:

1. **`machine/core` pasa a ser entrada del generador.** Es el mismo contrato que ya rige para las
   instrucciones —el código OOP es el único dueño, el archivo generado es un artefacto que se
   regenera— pero ahora alcanza al módulo de la máquina. Y necesita su propio candado: un
   `GeneratedSpectrumZ80IsCurrentTest` que corra en el build de `machine/core`, porque si alguien
   toca `Memory.writeByte` y no regenera, los dos núcleos divergen en silencio y el generado sigue
   pasando Fuse, que no conoce la máquina.
2. **Ese código deja de poder escribirse de cualquier forma.** Tiene que seguir siendo
   especializable: campos y aritmética, sin reflection, sin polimorfismo que el generador no pueda
   resolver desde el grafo vivo. Hoy `Memory` califica; el punto es que a partir de acá es una
   restricción, y las restricciones que no están escritas se rompen sin querer.
3. **Hay dos envoltorios de memoria**, el de `initNoTest` y el de `initTest`, y el generado se
   ataría a uno. Es una razón más para M2: que haya un solo camino de acceso.

### La alternativa más barata, que hay que medir antes de decidir

El documento arranca dando por hecho que hay que generar el código de la máquina. No es obvio, y
el orden de los pasos permite decidirlo con números en la mano:

**B0. La máquina expone su acceso, sin generar nada.** Hoy `memory.read` no se inlinea por dos
razones sumadas: el sitio es una interfaz, y el cuerpo es grande y llama a tres objetos más, uno
con boxing. **M1 y M2 sacan la segunda razón**: una lectura queda en unos cuarenta bytes de
bytecode sin llamadas adentro, muy por debajo de los 325 de `FreqInlineSize`, y entonces C2 la
inlinea en cada sitio caliente y ve los campos directamente. El "no hay llamada opaca" se consigue
sin que el generador sepa nada de la máquina.

Lo que quedaría afuera de B0, y es lo único que justifica G1–G3:

- **La contención con `times` literal.** `contend(addr, 5, 1, READ_NO_MREQ)` con `times` en una
  variable es un loop; con el literal en el sitio se desenrolla. Son cinco lecturas de tabla
  contra un loop de cinco vueltas.
- **El sitio de `contend` es polimórfico.** El campo es `PhaseProcessor` y hay tres
  implementaciones cargadas entre la máquina y los tests; ahí C2 no inlinea nada. Se arregla
  generando, o haciendo el campo del tipo concreto.
- **El `Kind` que nadie usa** y el `if (fetching == 1)` de cada lectura.

Mi estimación, con la tabla de LDIR de arriba: **B0 con M1–M3 se lleva más de la mitad** de lo que
hay para ganar en la CPU, sin acoplar nada. G1–G3 se lleva el resto y cuesta el acoplamiento del
punto anterior. Por eso el paso 4 va antes del 5 y **entre los dos hay una decisión, no un
trámite**: se mide `RzxCoreMeasurement` después de M1–M3 y recién ahí se decide si vale generar
código de la máquina.

## Mejoras de OOP en la máquina, antes de todo

Como en la fase 1: valen solas, se hacen primero con el núcleo OOP, y las verifican los tests de
la máquina (260, más los de memoria contendida y los hashes de boot).

- **M1. El timeout del reloj deja de ser un `Consumer`.** `SpectrumZ80Clock.setTimeout` instala
  `timeoutProcessor = this::timeOutProcess`, y desde ahí cada suma de T-states pasa por una
  interfaz con boxing. Lo que hace es una cuenta regresiva: se expresa como un `int` de vencimiento
  y `if (tStates >= deadline) fire()` dentro de `addTStates`, o —mejor, porque es el dueño de
  "algo pasa en el T-state X"— como un evento de `EventManager`, que ya despacha el fin de frame
  y la interrupción entre instrucciones, igual que Fuse. Lo segundo cambia la granularidad del
  vencimiento (de mitad de instrucción a entre instrucciones); lo deciden los tests de cinta de
  la máquina (`TapeHardwareTest` y el de carga real). Las tres sobrecargas de `addTStates` quedan
  en una.
- **M2. Una escritura es una escritura.** El `Memory` anónimo de `Z80` escribe dos veces por
  el `disabled` del debugger. El debugger lee y escribe por `readByteInternal` /
  `writeByteInternal2` directamente, y el camino del núcleo queda con `writeByte` solo. Lo mismo
  para `readByte(address, ula)`, que vuelve a buscar la página que `readByteInternal` acaba de
  buscar: una sola búsqueda, contención y valor juntos.
- **M3. Las propiedades de la página son de la página.** `screen` (esta página es la pantalla
  actual) y `writable` efectivo (`writable || writableRoms` para las ROM) se calculan al paginar,
  al cambiar `currentScreen` y al cambiar la configuración, no en cada escritura.
- **M4. ED indefinidos** (A8): instrucción de 2 bytes y 8 T en la tabla del modelo.
- **M5. El loop** (B5): `Z80.doOpcodes` distingue "hay traps o debugger" de "no hay", y el
  chequeo de pausa y bridge va por tramo. Vale para el OOP también.

M1 y M2 son las que más pesan: son las doce llamadas y la doble escritura de la tabla de LDIR.

## Cómo se genera

- **G1. Los terminales son un parámetro.** Hoy `Specializer.terminalName` decide que `Memory`,
  `IO` y `State` se emiten como llamadas a un campo. Pasa a recibir un mapa: para el núcleo puro,
  los mismos ganchos de hoy; para el de la máquina, los objetos vivos (`Memory` de `oozx`, `Ula`,
  `SpectrumZ80Clock`, `Display`) que se especializan como cualquier otro `Obj`. Lo que queda
  opaco de verdad (`io`, `display.dirtySinclair`, el `State`) sigue siendo llamada.
- **G2. Objetos externos como campos `final` del constructor, y la frontera invertida.** Un slot
  del especializador es hoy literal, local o campo con inicial. Un objeto de la máquina no tiene
  inicial: es una referencia que llega al construir. Nuevo tipo de slot: `final`, inyectado en el
  constructor, con el nombre del campo de origen (`mapRead`, `contention`). Y adentro de esos
  objetos la regla se invierte respecto de la fase 1: los campos mutables se emiten como lecturas,
  no como copias, y los arrays de instancia como accesos a array —hoy `fieldValue` los rechaza con
  `UnsupportedOperationException("instance array")`, así que es código nuevo, no un ajuste. Es la
  frontera de la sección de arriba, y es la parte del generador donde un error no lo ve ningún
  test que exista hoy.
- **G3. Helpers en vez de llamadas.** `CoreGenerator` emite, por cada terminal especializado y
  por cada combinación de argumentos literales que aparece (`read(x, 0)`, `read(x, 1)`,
  `contend(x, 2, 1)`, `contend(x, 5, 1)`), un método privado cuyo cuerpo es la especialización;
  el `case` llama al helper. Es lo que mantiene los `decode_N` en tamaño y deja que C2 decida el
  inlining con su presupuesto.
- **G4. Rangos** (A2) en `Folder`; **G5. las pasadas de `Simplifier`** (A3, A5); **G6. el tejido
  por rama** (A4) en `CoreGenerator.weave`; **G7. `run`** (A7) en el marco.
- **Dónde vive.** El generador está en `emulator/src/test` y `machine/core` no ve los tests de
  `emulator`; para especializar la memoria de la máquina tiene que ver sus clases. Iría en
  `emulator/src/test`, y no va ahí porque el que genera el núcleo de la máquina tiene que compilar
  contra `machine/core`. Propuesta: módulo `generator` (depende de `emulator` y JavaParser);
  `emulator` lo usa en tests para `GeneratedZ80`, `machine/core` lo usa en tests para
  `GeneratedSpectrumZ80`, cada uno con su `IsCurrentTest`. Es una decisión abierta.

## Los pasos

Cada paso termina verde en su gate y con su número anotado; el siguiente no empieza sin eso.

0. **Medir bien.** *Hecho.* `CoreBenchmark` sobre un `int[]` sin listeners, un núcleo por JVM (la
   contaminación de perfiles ya le costó un 30 % a la tabla de la fase 1). Un script que corra
   con `PrintInlining` y cuente, para los `decode_N` calientes, cuántos sitios de `memory.*` y
   `contend` quedaron como llamada. `LoopCoreMeasurement` (ROM) y `RzxCoreMeasurement` (JSW)
   como están, más el bloque final de `RzxCoreMeasurement` arreglado (hoy tira
   `IndexOutOfBounds` en `EventManager.eventDoEvents`). Son las tres líneas base.
1. **A3 y A2** en `Folder` y `Simplifier`: sin riesgo semántico, cambian el bytecode. Gate: Fuse
   1355, ALU, `IsCurrentTest` regenerado; el tamaño de bytecode por método, antes y después.
   *Hecho.*
2. **A4**: el tejido por rama. Gate: Fuse evento por evento, que es exactamente lo que cambia.
   *Hecho.*
3. **A6**: `GROUP_SHIFT` y un método por opcode, con el benchmark del paso 0. Se queda uno.
   *Hecho: se queda el 4, que era el que estaba.*
4. **M1–M5** en la máquina, con el núcleo OOP. Gate: `machine/core` 260, memoria contendida,
   hashes de boot, los tests de cinta; `RzxCoreMeasurement` para ver cuánto movió M1 y M2
   solas (se espera bastante: es la mitad de la tabla de LDIR).
   *Hecho M1, M2 y M4. No movieron nada y el perfil dice por qué; M3 y M5 quedan sin hacer, con
   razón escrita.*
5. **Decisión**, con el número del paso 4 en la mano: si B0 ya inlineó los accesos
   (`PrintInlining` sin `Memory::read` como llamada en los `decode_N` calientes), G1–G3 sólo
   agrega la contención desenrollada, y quizás no vale el acoplamiento. Si se hace: los terminales
   como parámetro, arrays de instancia en el especializador, la frontera declarada,
   `GeneratedSpectrumZ80` y su candado en `machine/core`. Gate: los 260 con
   `-Doozx.cpu=generated` y los mismos hashes, el test de cambio de máquina y paginado,
   `PrintInlining`, `RzxCoreMeasurement`.
6. **A7 y B5**: `run(until)` y el loop de la máquina por tramos. Gate: los mismos, más los
   tests de traps (cinta, TR-DOS, debugger).
7. **A5** si `PrintInlining` o el fuente muestran que quedó algo.
8. La tabla de "Lo que quedó" con las tres líneas base al lado.

## Lo hecho

Medido el 2 de septiembre de 2026, rama `nucleo-generado`, JDK 21.0.2, en esta máquina.

### Paso 0: las líneas base

`CoreBenchmark` se rehizo: cada núcleo en su propio JVM, sobre un `int[]` pelado sin listeners, y
un segundo test que corre con `PrintCompilation` y `PrintInlining` y dice qué se compiló y qué
quedó como llamada. Correr los dos núcleos en el mismo JVM le costaba un 30 % al generado: los
dos comparten los sitios de llamada de la memoria y el primero decide cómo se perfilan.

| línea base | OOP | generado |
|---|---|---|
| CPU pura, `CoreBenchmark` sobre `int[]` | 44–48 M instr/s | 195–215 M instr/s (**4,5×**) |
| loop propio, ROM ociosa (`LoopCoreMeasurement`) | 1191–1398 fps | 9076–10478 fps |
| RZX de JSW, 6000 frames (`RzxCoreMeasurement`) | 555 fps | 847 fps |
| ídem con `presentFrame`, sin sonido | 559–667 fps | 784–803 fps |

El ruido del micro-benchmark es de ±8 % entre corridas: una diferencia menor a esa no dice nada.
El gate de inlining del núcleo puro da 16 métodos compilados a C2 y **ninguna** llamada a memoria,
contención o boxing sin inlinear: el techo del núcleo puro ya está.

**Las dos líneas base de la máquina no son reproducibles todavía.** Repitiendo la del RZX el mismo
día sin tocar nada de ese camino, el núcleo OOP dio 458 fps donde antes había dado 555, y la del
generado cayó más todavía cuando corrió detrás de dos suites completas. Con 7 GB en la máquina,
un `mvn test` previo deja el equipo en otro estado. Antes de que esos números puedan decidir algo
—y el paso 5 depende de ellos— hay que tomarlos con la máquina quieta, alternando núcleos y
quedándose con la mejor de varias corridas, como hace `CoreBenchmark`. Los números confiables
hoy son el del núcleo puro y el conteo de bytecode.

`RzxCoreMeasurement` ya no tira la excepción, pero ahora se pasa del límite de 60 segundos que
`machine/app` le pone a un test: el bloque del loop propio corre de verdad y no entra. O el
bloque baja de 3000 frames, o la medición sale del suite.

El bloque final de `RzxCoreMeasurement` tiraba `IndexOutOfBounds`, y no era del test:
`RzxSession.release` reponía el evento de fin de frame leyendo el campo `spectrumFrameEvent`, que
vale -1 mientras nadie lo registró, y `eventDoEvents` indexaba la tabla de descriptores con -1.
Va por `frameEvent()`, que registra si hace falta, como hace el cambio de máquina. Y
`eventDoEvents` ahora pide que la cola no esté vacía: sin eventos no hay ninguno vencido, y el
centinela `EVENT_NO_EVENTS` vale -1 justamente para que `doOpcodes` no corra.

### Paso 1: A3 y A2

| qué | antes | el generador | + el banco |
|---|---|---|---|
| líneas de `GeneratedZ80.java` | 27.421 | 26.391 | **26.061** |
| bytecode total | 215.069 | 199.689 | **195.310** (-9,2 %) |
| método más grande | 3.547 | 3.355 | **3.291** |
| `& 0xFF` | 3.052 | 2.034 | **1.295** |
| `& 0xFFFF` | 4.472 | 4.134 | 4.068 |
| `== -1` | 273 | 226 | 203 |
| `A & 0xFF` | 302 | 302 | **0** |
| bloques etiquetados (`done_N:`) | 28 | 28 | **0** |
| `"BC".equals("BC")` | 57 | 0 | 0 |
| `InterruptionMode.values()[n]` | 8 | 0 | 0 |
| boxing (`Integer`) | 4 | 0 | 0 |

Gate: `emulator` 3003 tests verdes, Fuse evento por evento sobre los dos núcleos, la ALU
exhaustiva y el candado de frescura; `machine/core` y `machine/app` verdes con los dos núcleos.

Lo que salió de hacerlo:

- **La regla del `equals` era código muerto.** `Folder` ya sabía plegar `"BC".equals("BC")`, pero
  `MethodCallExpr` retornaba en la primera rama de `foldInPlace` y nunca llegaba a la de abajo.
- **Los rangos van derivados, no supuestos.** El primer intento leyó los anchos de registro del
  propio `case` y sacó los `A & 0xFF`: mal, porque un registro es un campo que sobrevive a la
  instrucción y que las vistas del banco escriben desde afuera. Ahora el generador calcula qué
  puede tener cada nombre mirando **todas** las escrituras que hay, las de los 1.616 `case` y las
  del fuente de `UnrolledRegisterBank`, y repitiendo hasta que dejan de crecer. Con eso `A & 0xFF`
  vuelve solo: `ARegister.write` es `A = value` sin enmascarar, así que el modelo no garantiza ese
  ancho y el generado no puede asumirlo. Los 1.018 masks que sí se fueron son los que el propio
  `case` prueba: lo que sale de `memory.read`, lo ya enmascarado, lo que se corrió a la derecha.
- **El banco de registros ahora mantiene el ancho, y el generador lo derive.**
  `UnrolledRegisterBank` era inconsistente: `IXRegister` enmascaraba al escribir, incrementar y
  decrementar; los de 8 bits enmascaraban al decrementar pero no al incrementar (`A++` dejaba
  0x100) ni al escribir; `PC`, `SP` y `MEMPTR` no enmascaraban nunca; y los pares hacían
  `if (++C < 0x100) return; C = 0;`, que deja el campo fuera de rango a mitad de camino. Ahora
  todos hacen lo que hacía `IXRegister`. Vale por sí solo —una escritura de afuera, cargar un
  snapshot por ejemplo, podía dejar un registro de 8 bits con 0x1FF y corromper la lectura del
  par— y de paso el generador puede derivar el invariante: los 302 `A & 0xFF` se fueron, y con
  ellos otros 739 masks. El banco quedó 11 líneas más corto.
- **Y sacó los bloques etiquetados.** El `if (++C < 0x100) return;` de los pares se especializaba
  como un `done_N: { ... break done_N; ... }` de ocho líneas, uno por cada par que la instrucción
  mueve. Los 28 que había son ahora una asignación y un `if` de dos líneas: el `case` de LDIR, que
  tenía tres, pasó de 24 líneas de eso a 9.
- **`IRRegister.decrement()` incrementaba.** Cuatro de sus cinco líneas eran inalcanzables, porque
  `RRegister` ya enmascara y el `if` que las guardaba nunca es falso, y la única que corría llamaba
  a `low.increment()`. Como R no acarrea a I en un Z80, el par se mueve cuando se mueve R: eso es
  lo que dicen ahora las dos. Nadie las llamaba, así que el bug nunca se vio.

### Paso 2: A4, la decisión se toma una vez

| qué | tras el paso 1 | ahora |
|---|---|---|
| líneas de `GeneratedZ80.java` | 26.061 | **25.443** |
| bytecode total | 195.310 | **191.253** |
| `== -1` | 203 | **14** |
| `!= -1` | 27 | **12** |
| método más grande | 3.291 | 3.295 |

Sobre los dos pasos: 215.069 → 191.253 bytes, **-11,1 %**. DJNZ, por ejemplo:

```java
// antes: la rama decide y después le preguntan cuatro veces
if ((B != 0)) { ...; _nextPC26 = jumpAddress2_49; } else { _nextPC26 = -1; }
int nextPC_51 = _nextPC26;
MEMPTR = (nextPC_51 == -1 ? 0 : nextPC_51) & 0xFFFF;
if (_nextPC26 != -1) contend((PC + 1) & 0xFFFF, 5, 1, READ_NO_MREQ);
if (_nextPC26 == -1) contend((PC + 1) & 0xFFFF, 1, 3, READ);
PC = _nextPC26 == -1 ? (PC + 2) & 0xFFFF : _nextPC26;

// ahora
if ((B != 0)) {
    int operand_50 = memory.read((PC + 1) & 0xFFFF, 0);
    int jumpAddress2_49 = ((PC + 2 + (byte) operand_50) & 0xFFFF);
    MEMPTR = jumpAddress2_49;
    contend((PC + 1) & 0xFFFF, 5, 1, READ_NO_MREQ);
    PC = jumpAddress2_49;
    break;
} else {
    MEMPTR = 0;
    contend((PC + 1) & 0xFFFF, 1, 3, READ);
    PC = (PC + 2) & 0xFFFF;
    break;
}
```

**No es una regla del tejido, es una del simplificador.** El plan proponía que `CoreGenerator.weave`
reconociera el patrón del `nextPC`. Salió mejor una regla que no nombra ninguna instrucción: *la
cola de un `case` que sólo pregunta por dónde fue la rama se copia adentro de la rama*. Con la
respuesta en la mano, cada copia pliega sus preguntas. Alcanza para las cuatro —la contención, el
MEMPTR y el PC— y también para la forma `n = cond ? a : b`, que es la de las ocho instrucciones de
bloque de ED. Las condiciones para que dispare, que son las que la hacen segura:

- las dos ramas dejan el nombre decidido, cada una con un literal o con otro nombre;
- la cola lo lee y nunca lo escribe;
- lo que va a ocupar su lugar no cambia antes de la última lectura del nombre. La sentencia que lo
  cambia puede ser esa última lectura —`PC = nextPC`— porque una asignación evalúa primero la
  derecha. Sin esa precisión, LDIR no entraba.

Lo que salió de hacerlo:

- **Los masks hay que volver a preguntarlos.** Mientras a `PC` se le asignaba `saltó ? allá : acá`,
  su ancho era desconocido, y con eso no se plegaba nada adentro de las ramas nuevas. Ahora
  `classify` recalcula los masks después de simplificar y vuelve a simplificar, hasta que la
  respuesta deja de mejorar. Son dos vueltas y la generación pasó de 8 a 15 segundos.
- **La propagación de alias era de más conservadora.** Se negaba a reemplazar `x` por `y` si `y` se
  asignaba en cualquier parte del cuerpo; lo que importa es si se asigna *después* de la
  declaración, porque lo de antes es justamente el valor que `x` tomó. Con eso caen los
  `int nextPC_601 = jumpAddress2_596;` que quedaban.
- **`Node.remove()` no saca una sentencia de primer nivel.** Las del cuerpo de un `case` están
  sueltas en una lista y no tienen padre, así que `remove()` devuelve false y no hace nada. El
  primer intento sacaba la declaración de un local y dejaba sus asignaciones, y el generado no
  compilaba. Lo agarró el build, no un test: el candado compara texto y no le importa que el texto
  compile.

Quedan 14 `== -1`, y se sabe por qué: seis son de HALT, que guarda su `nextPC` en un campo con -1
inicial —y eso es fiel al modelo, `AbstractInstruction.nextPC` tampoco se resetea entre
ejecuciones—, y ese campo le contagia a `PC` un ancho desconocido, que deja dos preguntas adentro
de la rama tomada de las ocho instrucciones de bloque. Para sacarlas habría que distinguir "cuántos
bits tiene" de "puede ser -1", que son dos hechos y hoy el análisis lleva uno solo. Es un frente
abierto chico y bien delimitado.

### Paso 3: A6, la forma del despacho

Medido con el benchmark del paso 0 sobre un `int[]`, cada variante compilada aparte y las cuatro
corridas **intercaladas** doce veces, en los dos órdenes, porque la diferencia esperada era del
tamaño del ruido:

| corte | métodos | bytecode | mejor | media |
|---|---|---|---|---|
| 0, un método por opcode | 1.626 | 262.899 | 196,5 | 190,9 |
| 3, ocho casos por método | 214 | 196.654 | 193,2 | 185,6 |
| **4, dieciséis casos** | **112** | **191.253** | **222,8** | **216,5** |
| 5, treinta y dos casos | 61 | 187.042 | 219,9 | 213,4 |

**Gana el que ya estaba, y no hay nada que cambiar.** 4 y 5 empatan dentro del ruido; 3 y 0 son un
13 % más lentos, reproducible en los dos órdenes.

Lo que enseñó el experimento es que no se podía extrapolar. De 4 a 3 empeora, así que la tendencia
decía que un método por opcode iba a ser peor todavía; medido, es *mejor* que 3. Son dos regímenes
distintos: con un caso por método los cuerpos son chicos y C2 puede inlinearlos en el despacho, lo
que recupera parte de lo que se pierde. Recupera parte, no todo, pero el argumento de la tendencia
era inválido y sólo se ve midiendo.

`GROUP_SHIFT` queda configurable con `-Doozx.groupshift`, con 4 por default, porque el paso 5 lo
va a necesitar: cuando los accesos se inlineen los métodos crecen y esto es lo que los regula.
Cambiarlo cambia el archivo generado, y eso lo dice el candado.

Nota de medición: corrido directo, sin maven en el medio, el mismo núcleo da 216 M instr/s de
media contra los 194-205 que salían por `mvn test`. Para comparar variantes hay que sacar a maven
del lazo.

### Paso 4: M1, M2 y M4, y lo que la medición dice del paso 5

**M2. Un acceso hace una sola cosa.** Una lectura buscaba la página dos veces, una para el byte y
otra para lo que la página cuesta, y una escritura escribía dos veces —la segunda estaba para que
el camino del debugger apagado igual guardara—. `Memory.readByte` contesta las dos cosas de una
sola mirada a la tabla, y el envoltorio de `Z80` resuelve primero el caso apagado y después hace
cada cosa una vez.

**M4. Un ED indefinido ya no explota.** La tabla devolvía "no hay instrucción", el fetcher se
encontraba un null y el generado tiraba. En el hardware son dos bytes y ocho T-states de nada, que
es lo que ya genera el `Nop` de la propia tabla ED. Son 160 opcodes y 794 líneas más en el
generado.

**M1. El reloj no boxea más, y no cambia nada.** La cuenta regresiva esperaba detrás de un
`Consumer<Integer>`. Ahora espera en un campo. Medido, **no mueve nada**, y la razón hay que
escribirla: `setTimeout` lo llama solamente la cinta, así que sin cinta cargando el `Consumer` era
null y nunca se boxeó nada. Este documento decía "doce llamadas boxeadas por iteración de LDIR";
eso vale sólo mientras carga una cinta, y ni JSW por RZX ni el loop de la ROM cargan ninguna. Las
tres sobrecargas de `addTStates` se quedan, porque el reloj de test las sobreescribe para grabar
para qué fue cada suma.

**Medido, A/B en la misma sesión**, mejor de tres corridas de 6000 frames de JSW, con los tres
archivos de la máquina puestos y sacados con `git checkout`:

| | antes | después |
|---|---|---|
| núcleo generado | 851 fps | 798 fps |
| núcleo OOP | 708 fps | 697 fps |

Dentro del ruido, y con el "antes" nominalmente adelante. **M1 y M2 no mueven JSW.**

#### El perfil, que es lo que había que hacer antes de estimar

JFR sobre la reproducción de JSW con el núcleo generado, 227 muestras:

| | |
|---|---|
| `Z80$1.write` | **31,3 %** |
| `Z80$1.read` | **17,6 %** |
| `SpectrumZ80Clock.addTStates` | 10,6 % |
| `Ay.synthesise` + `BlipSynth` | 10,5 % |
| avance del RZX | 7,9 % |
| **todo el núcleo generado junto** | **8,8 %** |

`Memory.readByte` y `writeByte` no aparecen como frames porque están inlineados adentro del
envoltorio `Z80$1.read`/`write`. Pero ésa es la pregunta equivocada. La que importa es si **el
envoltorio** está inlineado adentro de los `decode_N`, o sea si `memory.write(_address84, ...)` en
el `case` es una llamada de verdad. Medido con `PrintInlining` sobre la misma reproducción:

| sitio | decisiones | inlineadas |
|---|---|---|
| `memory.read` | 1.040 | **89 (8,6 %)** |
| `memory.write` | 433 | **4 (0,9 %)** |
| `contend` | 583 | **40 (6,9 %)** |

**Más del 90 % de los accesos son llamadas reales**, que es exactamente lo que A1 dice. Las razones
que da C2, y son instructivas:

- `callee is too large`: el envoltorio son 78 y 82 bytes, por encima de los 35 de `MaxInlineSize`,
  y C2 no consideró esos sitios lo bastante calientes para usar el presupuesto de 325.
- `already compiled into a medium method`: una vez que compiló `Z80$1.read`/`write` por separado,
  su tamaño **compilado** pasa los 2.500 de `InlineSmallCode` y deja de inlinearlo en todos lados.
- `callee uses too much stack` para `contend`: el `decode_N` que llama ya usa mucha pila.

Y el envoltorio es grande justamente **porque** `Memory.writeByte`, `writeByteInternal` y
`displayDirtySinclair` sí están inlineados adentro de él. Se comió su propio presupuesto.

Parecía abrir un camino más barato que G1–G3 —achicar el envoltorio hasta que entre— y se probó
**sin escribir código**, que es la forma más barata de probarlo: subiendo los umbrales del JIT por
línea de comandos. Cuatro corridas de cada configuración:

| | mejor | media |
|---|---|---|
| como está | 878 | 813 |
| `-XX:MaxInlineSize=120` | 848 | 823 |
| `+ -XX:InlineSmallCode=10000` | 904 | 836 |
| `+ -XX:FreqInlineSize=1000` | 847 | 800 |

Los flags **sí** cambian el inlining —los sitios inlineados pasan de 89 a 132 en lectura y de 4 a
14 en escritura, y las 263 negativas por `medium method` desaparecen— y la velocidad **no se
mueve**: todo cae dentro de un ruido de ±3 %.

O sea: **las llamadas están, y sacarlas no sirve.** El 31 % y el 17,6 % que el perfil le atribuye
al envoltorio son el trabajo, no la llamada.

Se midió también sacando cada pieza del camino, del todo, para ver cuál era:

| | mejor de 3 |
|---|---|
| como está | 826 |
| sin ensuciar la pantalla | 841 |
| sin contención | 854 |
| restaurado | 803 |

Ninguna pasa el ruido. **El costo de la máquina está repartido**: no hay una pieza que sacar.

Lo que esto le hace al plan:

- **A1 y G1–G3 valen alrededor del 3 % en JSW**, no lo que este documento estimaba. Su premisa
  —que la llamada opaca cuesta por lo que le hace al resto del `case`— es cierta en el papel y no
  se ve en la medición.
- **M3 no se justifica** por esta vía: sacar `displayDirtySinclair` del camino da 2 %, dentro del
  ruido.
- **El piso de ruido de esta medición, ±3 %, es del tamaño de los efectos que quedan.** Antes de
  seguir optimizando la máquina hay que medir mejor, o aceptar que lo que queda es difuso.
- Con el núcleo generado en 8,8 % del frame, **hacer la CPU infinitamente rápida da 10 %**. Lo que
  queda para JSW no es una optimización, es una decisión de diseño: la contención se calcula por
  acceso, y lo único que observa el T-state exacto adentro del frame son el borde y el bus
  flotante. Eso es de la máquina y es tuyo decidirlo.

#### M3 y M5

- **M3 no se hizo**, y la medición de arriba dice por qué: sacar `displayDirtySinclair` del camino
  de toda escritura da 2 %, dentro del ruido, y forzar el inlining del envoltorio entero tampoco
  mueve nada.
- **M5** no aparece en este perfil: `Z80.doOpcodes` no está entre las muestras porque
  `RzxPlayback` avanza las instrucciones por su cuenta y no pasa por ahí. Para la app de escritorio
  sí importa —ahí el bridge se consulta por instrucción— pero eso no se mide con esta herramienta,
  así que no hay con qué decidirlo.

Lo que el perfil sí señala, y no es CPU: el AY se sintetiza con el sonido apagado y se lleva el
10,5 %, y el camino de escritura lee el byte viejo de la pantalla para decidir si ensuciarla.

## Cómo se verifica

- **Semántica**: Fuse 1355 evento por evento sobre `GeneratedZ80`; `GeneratedAluReferenceTest`;
  `machine/core` 260 con el generado y los mismos hashes de boot; el RZX de JSW con los mismos
  hashes de frame que el OOP (el RZX es determinista: cualquier diferencia de T-states se ve).
- **JIT**: `PrintInlining` como test de humo del paso 0: para los `decode_N` que se calientan en
  el ROM y en JSW, cero líneas con `Memory::read`, `::contend` o `Integer::valueOf`; ningún
  `default` alcanzado (`PrintCompilation` sin "made not entrant" repetidos sobre el mismo
  método).
- **Velocidad**: las tres líneas base, en el mismo hardware, tres corridas cada una.

## Riesgos y decisiones abiertas

- **Dos artefactos generados.** El núcleo de la máquina acopla el generado a `oozx.Memory`,
  `Ula` y el reloj, y convierte a `machine/core` en entrada del generador con su propio candado de
  frescura. Es el precio de inlinear la máquina, y por eso B0 existe: puede que no haga falta
  pagarlo. Si se paga, está contenido: el puro sigue siendo el que Fuse verifica, y el de la
  máquina se verifica contra el puro con los mismos hashes.
- **La frontera mal puesta es un bug silencioso.** Congelar `contended` o copiar `tStates` da un
  núcleo que pasa Fuse —que no pagina— y falla en un juego de 128K media hora después. El test de
  cambio de máquina y paginado del punto anterior es obligatorio antes del paso 5, y tiene que
  correr con los dos núcleos.
- **El presupuesto de bytecode.** Con helpers de 30–60 bytes y hasta cuatro accesos por `case`,
  un `decode_N` de 16 opcodes sigue bajo 8.000; si A1 se hiciera inlineando en el sitio, no. Se
  mide en el paso 5 y `GROUP_SHIFT` regula.
- **El timeout de la cinta** (M1) es el cambio con semántica: la granularidad. Lo deciden los
  tests de carga; si no pasan con el evento, queda el `deadline` en el reloj, que no cambia nada.
- **El debugger y el bridge por tramo** (B5): pausa y comandos con la latencia del contador
  (unos 100 µs con N = 256) cuando no hay debugger conectado. Es una decisión del usuario de la
  máquina, no del núcleo.
- **`Memory.read` devuelve 0..255**: A2 lo vuelve un contrato del que depende la corrección del
  núcleo puro (hoy las máscaras lo tapan). Se escribe en la interfaz y lo cubre la ALU exhaustiva.
- **Módulo `generator`**: es una carpeta más; la alternativa de publicar el test-jar de `emulator`
  es más frágil.

## Lo que se espera

Hipótesis, a confirmar con las líneas base del paso 0. Los porcentajes son sobre el frame de JSW
medido (1,3 ms a 760 fps).

| tramo | hoy | después de | esperado |
|---|---|---|---|
| núcleo puro, `CoreBenchmark` sobre `int[]` | 184–197 M instr/s | A2–A6 | +10–25 %: menos bytecode, menos saltos, menos máscaras |
| ROM ociosa, `LoopCoreMeasurement` | 10.250 fps | M1, M2, G1–G3, A7/B5 | la CPU deja de ser distinguible del núcleo puro |
| JSW, parte CPU (LDIR ≈ 400 µs) | ~130 ns por iteración | M1–M3, o sea B0 | ~50–70 ns: sin `Consumer`, sin doble escritura, y el acceso ya entra en el presupuesto de inlining de C2 |
| ídem | | G1–G3 | 15–25 ns: contención desenrollada y `contend` no polimórfico |
| JSW, fps de reproducción | 760 | todo lo anterior | ~1.000–1.100: el resto es AY (200 µs), pantalla y RZX |

La última fila es la que responde a "3000 % contra 12000 %": el núcleo de la máquina saca de JSW
lo que es de la CPU, y lo que queda después es el AY que se sintetiza sin sonido, la pantalla y el
avance del RZX. Eso es otro plan.
