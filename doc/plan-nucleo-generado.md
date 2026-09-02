# Un núcleo Z80 generado: el emulador OOP desplegado en una sola clase

Plan para obtener, automáticamente y a partir del código OOP que ya existe, una versión del
emulador con la forma de los emuladores rápidos: una clase, un `switch` por tabla de opcodes, un
`case` por variante de instrucción, los prefijos como `switch` en cascada, y ninguna llamada
virtual en el camino caliente salvo memoria, puertos y el gancho de contención de la máquina. La
referencia de forma es `Z80.java` de Z80Core (jsanchezv); la referencia de semántica es el código
OOP de `emulator`, que es el único dueño.

Medido sobre `emulator` el 1 de septiembre de 2026, con la suite Fuse en 1355 verdes.

**Estado (2 de septiembre de 2026, rama `nucleo-generado`): hecho, salvo C.** A, B y D están
hechos y commiteados; el generador existe y `GeneratedZ80.java` pasa los 1355 tests de Fuse
evento por evento, la referencia ALU exhaustiva, y los 260 tests de `machine/core` con los
mismos hashes de boot que el núcleo OOP. La máquina lo usa con `-Doozx.cpu=generated`; el
default sigue siendo el OOP. C (fetch y executor) queda como propuesta a revisar, y el generador
lleva mientras tanto sus dos reglas de deuda. Lo medido está al final, en "Lo que quedó".

## El principio

**El modelo no se adapta a la herramienta.** El proyecto es OOP y sigue siéndolo; el núcleo
generado es un artefacto de optimización que se obtiene *analizando* ese modelo, no una razón
para cambiarlo. Los únicos cambios que este plan propone al modelo son mejoras de OOP que valen
por sí solas —y se hacen **antes** de escribir el generador, para que el modelo quede más prolijo
y la transformación salga sola de esa prolijidad.

Lo que **no** se hace, y por qué:

- **MEMPTR no se mezcla en las instrucciones.** Es un registro no documentado, que puede estar o
  no en el procesador; no es esencial, y por eso está agregado lateralmente (`MemptrUpdater`).
  Se mejora *como aspecto*: un solo dueño, entradas explícitas.
- **La contención no se mezcla en el diseño del Z80.** No es del Z80, es de la Spectrum, y por
  eso es un aspecto (`PhaseProcessor`). Se mejora *como aspecto*: reglas como valores, un solo
  gancho, y el modelo deja de conocerla.
- **Nada se cambia "para que el generador lo entienda"**: tablas ALU, campos lambda,
  `instanceof`, opcodes indefinidos. El generador se hace cargo.

Cada instancia de instrucción que fabrica el decodificador es un grafo de objetos fijo:
`Add(target=A, source=(HL), flag=F, alu=Add8TableAluOperation)`. Lo único que varía cuando corre
son los valores de registros y memoria; todo lo demás quedó decidido al construir la tabla. Ésa es
la situación en la que un **evaluador parcial** produce código especializado: se toma el fuente de
un método, cada llamada a un colaborador se reemplaza por el fuente del método de ESE colaborador
concreto (recursivamente), cada campo de configuración se reemplaza por su valor, y queda el código
directo que esa instancia ejecuta. `target.write(source.read())` con target=A y source=(HL) se
vuelve `A = memory.read(H << 8 | L, 0)`.

Los aspectos laterales entran por el mismo camino. `MemptrUpdater` es un visitor: con la instancia
en la mano, el doble despacho se resuelve estáticamente y el cuerpo de `visitingCall` queda
inlineado después del `case`. La contención, una vez que sea una lista de valores por instancia,
ni siquiera hace falta especializarla: se lee y se imprime en el punto que cada valor dice. El
núcleo generado es, literalmente, **el modelo con sus aspectos tejidos y aplanados**, y qué
aspectos tejer es un parámetro del generador, no una decisión del modelo.

Tres consecuencias:

1. **El dueño de la semántica sigue siendo el código OOP.** El archivo generado es un artefacto,
   como un `.class`: no se edita, se regenera. Un test falla si lo commiteado no coincide con lo
   que el generador produce hoy.
2. **El generador tiene un solo mecanismo** —inlining dirigido por el grafo de instancias— y no
   un template por instrucción. Una regla que nombre una instrucción es la señal de que algo del
   modelo está mal expresado; se mira ahí primero, y si arreglarlo no es una mejora de OOP, la
   regla se escribe igual y se documenta como deuda del generador, no del modelo.
3. **El núcleo generado es exactamente tan correcto como el OOP** con los aspectos que se le
   tejieron. Por eso el gate es el mismo: los 1355 tests de Fuse, que comparan registros,
   memoria, MEMPTR, T-states totales y la lista exacta de eventos de bus (MR/MW/MC/PR/PW/PC)
   con su instante.

## Lo que hay hoy

| qué | dónde | tamaño |
|---|---|---|
| instrucciones | `instructions/impl` | 76 clases; 35 redefinen `execute()`, el resto lo heredan de 16 tipos base |
| referencias a operandos | `opcodes/references` | 25 clases: registro, `(HL)`, `(IX+d)`, `n`, `nn`, `(nn)`, constante, puerto |
| operaciones de flags | `Add.Add8TableAluOperation` y 40 más | 41 clases, cada una envuelta en `CachedTableAluOperation` (tabla precalculada) |
| decodificación | `opcodes/decoder/table` | 7 tablas de 256: base, CB, ED, DD, FD, DDCB, FDCB ≈ 1.792 lugares (ED tiene 80 definidos, el resto `null`) |
| fetch | `cpu/DefaultInstructionFetcher`, `MultiOpcodeFetcher`, `DefaultFetchNextOpcodeInstruction` | prefijos como instrucciones dentro de la tabla, más 4 envoltorios y una caché de lecturas |
| ejecución | `cpu/DefaultInstructionExecutor` | listeners antes/después, `nextPC` o `pc + length` |
| contención (aspecto) | `fuse/tstates/PhaseProcessor` (417) + base, `CachedPhase`, 4 fases, 4 visitors | lambdas registradas por instancia; 8 ganchos abstractos que la máquina (`FusePhaseProcessor`) y el test (`TestFusePhaseProcessor`) implementan |
| MEMPTR (aspecto) | `cpu/MemptrUpdater` (205) + `spy/MemptrUpdateInstructionSpy` | lo instalan los tests de Fuse y el translator; la máquina hoy no, y ahí MEMPTR lo escriben sólo los hooks de `Ld`/`Ex` que viven en `PhaseProcessor`, y `doInt` |
| registros | `registers/*` | `Composed16BitRegister` y `PlainComposed16BitRegister`; `UnrolledRegisterBank` (1.544 líneas, campos `int` planos) existe y **nadie lo usa** |

### Por qué el núcleo OOP es lento

No es "porque es OOP". Son cosas concretas, todas en el camino de cada instrucción, y ninguna
es una razón para cambiar el modelo: son lo que el artefacto generado elimina.

- **Sitios de llamada megamórficos.** `target.write(...)` en `Ld.execute()` ve seis clases de
  receptor distintas; `source.read()` otras tantas; `condition.conditionMet()` tres. El JIT no
  puede desvirtualizar un sitio que ve más de dos tipos, así que cada uno es un `invokeinterface`
  real. Son entre cinco y diez por instrucción. **Esto es lo que el `switch` elimina**, y es la
  razón por la que rinde más que "una clase generada por instancia": en el `case` no hay ningún
  sitio de llamada.
- **La fase se despacha en cada lectura de memoria.** El wrapper de memoria de `Z80.initNoTest`
  llama `processPhase(afterMR)` por byte leído: `CachedPhase.execute` → visitor → lambda →
  `if (readCount == k)`. En el generado la contención ya está intercalada en el punto exacto.
- **Cinco campos lambda** en el camino caliente: `AluOperation.triFunction`,
  `ConditionBase.isConditionMet`, `IndirectMemory16BitReference.memoryWriter`,
  `DefaultFetchNextOpcodeInstruction.inc2Consumer`, `DefaultInstructionExecutor.afterExecutionAction`.
- **`ObservableRegister`**: cada `read()`/`write()` de un registro consulta `listening`.
- **`MemoryForOpcodes`**: un journal de direcciones que se resetea por instrucción, para que un
  operando que el grafo lee dos veces cueste una sola lectura.
- **Memoria**: `CachedTableAluOperation` construye para las operaciones de tres valores (`Ini`,
  `Outi`, `BIT`) tablas de 256³ enteros, 64 MB cada una, 192 MB al arrancar. El generado inlinea
  el `calculate*` y no las necesita; el OOP las conserva, es su decisión.

### Lo que ya existe y sirve

- **JavaParser ya es dependencia** (`javaparser-core`, versión centralizada en el pom raíz,
  scope test en `emulator`).
- **Hay un prototipo** en `emulator/src/test/java/com/fpetrola/oozx/`: `CodeInliner` (317),
  `InstructionAnalyzer` (150), `MethodCodeExtractor` (173), `TestInlinerTest` (331). Extrae con
  JavaParser el cuerpo de `execute()` y de la operación ALU, y para tres formas de `Ld`/`Xor`/`Or`
  arma la clase inlineada. Dos cosas lo descalifican como base: el inlining es un template por
  forma de referencia (`if (target instanceof MemoryPlusRegister8BitReference) code.append(...)`),
  que es la duplicación que este plan evita; y apunta a un checkout viejo
  (`/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/...`), así que hoy no corre. Se reusa la
  parte de JavaParser (`MethodCodeExtractor`); el resto se reemplaza.
- **`UnrolledRegisterBank`** es la forma exacta que el núcleo generado necesita para los
  registros: campos `int` planos con vistas `Register` internas, para que `State` y el resto de la
  máquina sigan viendo registros. Con las tablas construidas sobre ese banco, especializar
  `register.read()` da `A` sin ninguna regla: su `read()` es `return A;`.
- **`TableBasedOpCodeDecoder`** es el catálogo: recorrerlo da todas las instancias, y las
  instancias de `DefaultFetchNextOpcodeInstruction` dan la forma de cada `switch` anidado.
- **Los aspectos ya están escritos como visitors**, que es la forma que un especializador
  resuelve mejor: doble despacho sobre tipos que en la instancia están fijos.
- **`FetchListener`** existe (`instructionFetchedAt(pc, instruction)`), el fetcher lo acepta y
  nunca lo llama. Es exactamente el punto de enganche que un aspecto necesita.
- **Oráculos**, todos existentes: Fuse (1355, evento por evento), `AluReferenceTest` (todas las
  entradas de cada instrucción ALU contra el Z80 documentado), `AllTableAluOperationsCompatibilityTest`
  (MD5 de las tablas), `ZXSpectrumContendedMemoryTests` (contención en la máquina),
  `EmulationRegressionTest` (hashes de pantalla, sysvars y RAM tras el boot de la ROM), y las
  grabaciones RZX (`zx-rzx`), que fijan el conteo de fetches por frame.

### Lo que el generador resuelve sin tocar el modelo

Cada una de estas cosas podría tentar a "arreglar el modelo para que el generador lo lea". No:
son reglas del especializador.

- **Campos lambda y method references.** El objeto de runtime no tiene fuente, pero la
  asignación en el constructor sí: `memoryWriter = target es SP ? memory::write16Bits :
  memory::write16BitsReverse`. El especializador especializa esa asignación contra la instancia,
  pliega la condición y sigue por el método elegido. `AluOperation` elige su función sondeando
  `calculate*(0,0,0) != -1`: la sonda es pura y se evalúa sobre la instancia.
- **`instanceof` en la semántica** (siete: `BIT` 2, `LdOperation` 2, `In`, `Out`,
  `IndirectMemory16BitReference`): se evalúan contra la instancia y la rama muerta se borra.
- **Tablas ALU**: `aluOperation.execute2ValuesAndCarry(a, b, flag)` sobre una
  `CachedTableAluOperation` se resuelve por el `calculate*` que redefine el delegado, con `F` y
  `flag` mapeados al campo `F`. La tabla es una optimización de ese cuerpo, no otra semántica.
- **Opcodes ED indefinidos** (`null` en la tabla; hoy `NullPointerException` al ejecutarlos): el
  generador emite lo que el modelo haga con ellos el día que los defina, y mientras tanto los
  deja fuera con un aviso.

## Las mejoras de OOP, antes de todo

Cuatro, en este orden. Cada una vale por sí sola —achica, desacopla o corrige—, cada una deja
Fuse en verde, y cada una le quita al generador una regla que de otro modo tendría que nombrar
algo del modelo. Son propuestas de forma; el gusto final es del dueño del modelo.

### A. Los aspectos dejan de estar en el modelo

Hoy el modelo conoce a la contención. Cinco archivos del núcleo importan `fuse.tstates`:
`Instruction` tiene `getCachedPhase()` en la interfaz, `AbstractInstruction` tiene un campo
`CachedPhase` y `setPhaseInterceptor`, `DummyInstruction` devuelve `null`, y
`DefaultInstructionFetcher` tiene un campo público `tPhaseProcessor` que **construye por defecto
un `TestFusePhaseProcessor`** —el procesador de contención de los tests de Fuse— y lo aplica en
`setupPhaseInterceptor`; la máquina lo pisa después con el suyo. `PhaseProcessorBase.processPhase`
castea el fetcher a `DefaultInstructionFetcher` para preguntarle la última instrucción. Y `State`
tiene `events`/`addEvent` con `Event`, que es el registro de eventos del harness de Fuse: lo usan
`TestFusePhaseProcessor`, `AddStatesIO` y `FuseResult`, nadie más.

Propuesta: el aspecto se engancha, no se hospeda.

- La contención se instala con los tres listeners que el modelo ya ofrece: `FetchListener`
  (para preparar la instancia que se va a ejecutar), `ExecutionListener` (antes y después) y los
  listeners de memoria (después de cada lectura, antes de cada escritura). Su dato por instancia
  vive en el aspecto (una tabla por identidad de instrucción: las instancias son finitas y son
  las de la tabla), no en la instrucción.
- `Instruction`, `AbstractInstruction` y `DummyInstruction` pierden `CachedPhase`; el fetcher
  pierde `tPhaseProcessor` y `setupPhaseInterceptor`; ningún archivo bajo `cpu`, `instructions`,
  `opcodes` o `registers` importa `fuse.tstates`.
- Los eventos del harness salen de `State`: son un decorador de `Z80Clock` que registra, o un
  listener; `State` se queda con el reloj.

Lo que le da a la transformación: el especializador arranca desde `OOZ80.execute()` y no
encuentra en el camino un procesador de test que ignorar ni un cast que resolver.

### B. La contención como valores, con un solo gancho

`PhaseProcessor` es, en el fondo, una tabla: "ADD HL,rr: 7 MC en IR antes de ejecutar";
"JR tomado: 5 MC en PC+1 al final, no tomado: 1 MC de 3 T en PC+1"; "EX (SP),HL: 1 MC en SP+1
antes de la primera escritura, 2 MC en SP al final". Contadas, son 35 registraciones y todas
tienen la misma forma: **cuándo** (antes de ejecutar; después de la lectura k; antes de la
escritura k; al final; al final si saltó), **dónde** (IR, PC+δ, HL, BC, DE±δ, SP±δ, o la última
dirección accedida) y **cuánto** (1 a 7 veces, de 1 ó 3 T). Hoy están escritas como lambdas que
preguntan `readCount == 1` adentro, con `currentRegister` y `address` como argumentos implícitos
pasados por campos mutables de la base, y despachadas por cuatro clases de fase y cuatro
interfaces de visitor cuyo único trabajo es llegar a la lambda correcta.

Propuesta:

- Una regla es un valor: `cuándo` y `contención(dónde, veces, T)`. El visitor de `PhaseProcessor`
  sigue siendo el único que sabe qué regla tiene cada forma, pero produce una lista de valores por
  instancia en vez de registrar closures. Adiós `CachedPhase`, las cuatro fases, los cuatro
  visitors, `readCount`, `writeCount`, `currentRegister`, `Optional<Boolean>`, y el `instanceof`
  sobre ocho clases de `visitRepeatingInstruction` (un visitor que no usa el visitor).
- Un ejecutor de 30 líneas recorre la lista cuando el punto llega: la k-ésima lectura, la
  k-ésima escritura, el final. Es lo que hoy hacen `AddStatesMemoryReadListener`, el wrapper de
  memoria de `Z80.initNoTest` y `PhaseProcessorExecutionListener` por separado y duplicado.
- **Un solo gancho**, con argumentos explícitos: `contend(address, times, tstates)`. Los ocho
  abstractos de hoy (`addMultipleMCIR`, `addMultipleMCPc2`, `addMultipleMCPC3`, ...) son ese uno
  con la dirección ya calculada; `FusePhaseProcessor` queda en cinco líneas (página contendida →
  `Ula.addUlaStates` por vez; si no, el reloj) y `TestFusePhaseProcessor` en tres (un `Event`
  MC por vez).
- Los hooks de MEMPTR que viven acá (`addAfterExecution` de `visitingLd`, y el de `visitEx`) se
  van a D.

Lo que le da a la transformación: el tejido de contención deja de ser "especializar el código que
registra lambdas y plegar `readCount == k`" y pasa a ser "leer la lista de la instancia e imprimir
cada valor en su punto". Neto estimado: ≈ −700 líneas.

### C. Fetch y executor: construido, no declarado (propuesta a revisar; no se hace todavía)

**Límite fijo: la instrucción no cambia.** Una instrucción es lo mínimo para llamarse instrucción
—`execute()`, `getLength()`, `accept()`— y cómo se hace fetch no forma parte de ella; por eso
no se modela ahí. Todo lo que sigue vive del lado que hoy tiene el `delta`: el fetcher, el
decodificador y las referencias a operando.

El fetch de hoy es correcto pero está construido, y se nota en dos lugares: al leer el código y
en lo que un especializador ve. Un especializador no distingue una compensación de una semántica:
si el fetcher pre-lee un byte para corregir un orden, el generado pre-lee también; si hay una
caché para que una re-lectura no cueste, el generado arrastra el journal.

Lo que está construido:

- **Dónde está cada operando lo saben entre todos.** El `delta` de `Memory8BitReference` /
  `Memory16BitReference` / `MemoryPlusRegister8BitReference` es un offset absoluto desde el PC y
  depende de en qué tabla cae la instrucción: por eso `UnprefixedTableOpCodeGenerator` recibe
  `delta` (1 ó 2), `IndexerRegisterTableOpCodeGenerator.createLd1` elige entre `n(3)` y `n(2)`, y
  `DDCBFDCBPrefixTableOpCodeGenerator` usa `iRRn(ixy, true, 2)`. El segundo argumento de `iRRn`,
  `rewindOnWrite`, no se usa: es lo que quedó de "incrementar el PC y volver".
- **La longitud se parcha desde afuera.** `DefaultFetchNextOpcodeInstruction` hace
  `setLength(getLength() + 1)` sobre cada entrada de su tabla al construirse; el generador de
  DDCB suma otro; `LdOperation` suma uno más en su constructor.
- **El orden de lectura se corrige con envoltorios.** `LdSpecialWrapper` pre-lee `pc+2` para que
  en `LD (IX+d),n` el `d` se lea antes que el `n` (porque `target.write(source.read())` evalúa
  el `n` primero); `inc2Consumer` pre-lee el `d` de DDCB antes del cuarto byte;
  `FetchNextOpcodeInstructionWrapper` existe para llamar `update()` (el R++ del prefijo) antes
  de decodificar. `increment = incPc - 1 + length` es la aritmética que los sostiene.
- **Las re-lecturas se absorben con una caché.** `MemoryForOpcodes` existe porque el grafo lee
  el mismo byte más de una vez: `d` en un read-modify-write sobre `(IX+d)` (`target.read()` y
  `target.write()` llaman `fetchRelative()` cada uno), `BIT (IX+d)` lo lee dos veces,
  `MemptrUpdater.updateBefore` vuelve a leer el `nn` de `CALL`. `MemoryPlusRegister8BitReference`
  ya tiene un campo `fetchedRelative` que sólo el clon `Cached*` usa.
- **Código muerto en el camino.** En `DefaultInstructionFetcher`: `prefetch` (siempre `false`),
  `afterExecute` (nadie lo llama; `OOZ80` lo tiene comentado), `fetchListener` (se guarda, nunca
  se invoca), `rdelta`, `setClone`. En `DefaultInstructionExecutor`: `executingInstructions` e
  `instructions` nunca se llenan, `addTopExecutionListener` está vacío, y `noRepeat` es `false`
  en las siete construcciones que hay (el translator lo maneja en su propio
  `TransformerInstructionExecutor`).

Propuesta, toda del lado del fetch:

1. **El fetcher es dueño del stream de la instrucción.** Lee los bytes de opcode (M1, R++) y
   expone el área de operandos de la instrucción en curso. Una referencia a inmediato lee de esa
   área por posición **relativa al área**, no por offset absoluto desde PC: la posición la asigna
   el decodificador —que es quien sabe cómo se codifica cada opcode—, y la profundidad de prefijo
   la sabe el fetcher, que es quien consumió los prefijos. `OpcodeTargets` y los generadores de
   tabla dejan de recibir `delta`.
2. **El área de operandos se lee una vez por ejecución**: recuerda cada byte leído hasta la
   próxima instrucción. Con eso desaparecen `MemoryForOpcodes`, los tres `Cached*Reference` y el
   `fetchedRelative` a medias; y los aspectos pueden leer "el operando ya leído" sin volver a la
   memoria.
3. **El fetcher lee los desplazamientos de índice al preparar la instrucción; los inmediatos y
   los desplazamientos de salto se leen cuando se usan.** Es cómo lo hace el Z80: el `d` de
   `(IX+d)` se consume justo después del opcode, antes de cualquier inmediato y —en DDCB— antes
   del último byte de opcode. El fetcher ya visita la instrucción recién decodificada (hoy para
   la contención); ahí lee los desplazamientos de índice, lateralmente, sin que la instrucción
   sepa nada. Esa única regla reemplaza a `LdSpecialWrapper` y a `inc2Consumer`, y deja explícito
   lo que Fuse espera y el código ya hace: un salto relativo lee `d` sólo si salta (`20_2`: un MC
   de 3 T en PC+1 y ningún MR), mientras `JP cc` y `CALL cc` leen `nn` siempre (`c2_2`, `c4_2`).
4. **El prefijo declara su fetch.** Sigue siendo una instrucción que decodifica un nivel más
   —ésa es una buena idea y se conserva—, pero dice lo que hace: qué byte lee (M1 o no), si
   incrementa R, y que en DDCB el desplazamiento precede al opcode. Los bytes que un prefijo
   consume los cuenta el fetcher al calcular el próximo PC; nadie parcha la longitud de las
   instrucciones de la tabla. Sin `incPc`, sin `increment`, sin envoltorios.
5. **Lo muerto se borra.**

Y una que se anota para decidir, porque toca la interfaz de la instrucción: hoy la instrucción
deja `nextPC` en un campo de sí misma —una instancia compartida por toda ejecución— con `-1`
como centinela, y el executor lo lee después. Dónde sigue la ejecución sí es semántica de la
instrucción; lo que huele es el campo mutable en un objeto de la tabla. Si se cambia, es a que la
instrucción lo *conteste*; si no, el generador lo convierte en un local del `case` y listo.

Lo que le da a la transformación: el marco de cada `switch` se lee de los prefijos (4); los
offsets de operando se calculan desde posiciones, no se buscan por reflexión en campos privados
(1); no hace falta una regla de eliminación de lecturas repetidas (2); no hay envoltorios que
inlinear ni pre-lecturas que reproducir (3). Mientras C no esté hecho, el generador lleva dos
reglas de deuda: "dos lecturas del mismo byte de operando en una instrucción son una" (la
semántica de `MemoryForOpcodes`) y "los envoltorios se inlinean como parte del grafo".

Neto estimado: ≈ −400 líneas. Gate: Fuse evento por evento más los 338 de `machine`.

### D. MEMPTR: un dueño, entradas explícitas

`MemptrUpdater` tiene dos visitors anónimos —uno antes de ejecutar, otro después— y es el dueño
de MEMPTR salvo por tres casos que viven en `PhaseProcessor` (`LD` con `(BC)`, `(DE)`, `(nn)` y
`(nn)` de 16 bits; `EX (SP),HL`), que están ahí porque necesitan la dirección efectiva que la
referencia calculó al ejecutar. Tres cosas más: el visitor de antes **vuelve a leer memoria**
(`tCall.calculateJumpAddress()` para `CALL`, `getSource().read()` para `IN A,(n)`), y sólo la
caché evita que eso cueste; el de después lee campos públicos `address`/`value` de tres clases de
referencia; y `visitingConditionalInstruction` escribe `0` en MEMPTR en todo salto condicional no
tomado y en `JP (HL)` (`nextPC == -1 ? 0 : nextPC`). Según la documentación de MEMPTR y lo que hace
Z80Core, un salto no tomado no lo toca; Fuse no distingue porque todos sus tests arrancan con
MEMPTR en 0. **A confirmar antes de cambiarlo.**

Propuesta:

- **Un solo dueño**: los tres casos de `PhaseProcessor` vuelven a `MemptrUpdater`, con los
  mismos medios que el visitor de después ya usa (la dirección efectiva que la referencia
  calculó). Cuando C esté hecho, la dirección y el operando se preguntan con nombre.
- **Entradas explícitas**: el visitor lee el operando que la instrucción ya leyó y la dirección
  efectiva que la referencia calculó; no vuelve a la memoria. El de después recibe el próximo PC
  del executor en vez de preguntarle a la instrucción por un campo.
- Los saltos no tomados no escriben 0, una vez confirmado.
- Sigue siendo un visitor con código, no una lista de valores como la contención: sus reglas son
  aritmética sobre valores de ejecución (`(A << 8) | ((addr + 1) & 0xff)`, `BC + 1` con el BC de
  antes), es decir, código. La contención es una tabla; MEMPTR es una función.

Lo que le da a la transformación: MEMPTR se teje especializando un visitor cuyas entradas son
locales del `case` (el operando, la dirección, el próximo PC), sin re-lecturas que deduplicar ni
campos públicos que rastrear. Neto: ≈ −60 líneas, y el aspecto entero en un archivo.

### Y dos que se mencionan pero no se proponen todavía

- **`ObservableRegister`** es la base de `Plain8BitRegister`: el aspecto de espionaje (`listening`,
  listeners de lectura y escritura) está construido dentro del registro. `InstructionSpy` ya tiene
  `wrapRegister` y `wrapBank`, que es la forma en que un spy se engancha sin que el registro lo
  sepa. Es el mismo tema que A, en los registros; toca muchos archivos y no lo necesita ni el
  modelo ni el generador (que usa el banco desenrollado), así que queda anotado.
- **`AbstractInstruction` decide envolver su ALU** en `CachedTableAluOperation`: una decisión de
  optimización (y de 192 MB) tomada dentro del modelo. Quien construye instrucciones —la fábrica—
  es quien debería decidir si la operación va con tabla o sin ella. Anotado; no cambia nada para
  el generador.

## El destino

Un archivo, `GeneratedZ80.java`, con esta forma (nombres a decidir):

```java
public abstract class GeneratedZ80 extends UnrolledRegisterBank {
  protected final Memory memory;   // la de la máquina: cobra 3/4 T y la contención ULA de cada acceso
  protected final IO io;
  protected State state;           // iff1/iff2/IM/halted, como hoy

  public abstract void contend(int address, int times, int tstates, Contention.Kind kind);   // el gancho de B

  public void step() {
      R = (R + 1 & 0x7f) | regRBit7;
      decode(memory.read(PC, 1));
  }

  private void decode(int opcode) {          // un método por tabla, partido de a 16 opcodes por el límite del JIT
    switch (opcode >> 4) { case 0: decode_0(opcode); break; /* ... */ }
  }

  private void decode_8(int opcode) {
    switch (opcode) {
      case 0x86: {                                       // ADD A,(HL), tal como salió
          int _F181 = 0;
          int _address84 = 0;
          _address84 = (H << 8) | L;
          int sourceValue_288 = memory.read(_address84, 0);
          int value1_289 = A & 0xFF;
          int value2_290 = sourceValue_288 & 0xFF;
          _F181 = value2_290;
          int addtemp_292 = value2_290 + value1_289;
          int lookup_293 = ((value2_290 & 0x88) >> 3) | ((value1_289 & 0x88) >> 2) | ((addtemp_292 & 0x88) >> 1);
          value2_290 = addtemp_292 & 0xff;
          _F181 = ((addtemp_292 & 0x100) != 0 ? 1 : 0) | HALF_CARRY_ADD[(lookup_293 & 0x07)] | OVERFLOW_ADD[(lookup_293 >> 4)] | (SZ53[value2_290 & 0xff] | (value2_290 == 0 ? 0x40 : 0));
          int result_294 = value2_290 & 0xFF;
          F = _F181 & 0xFF;
          A = result_294;
          PC = (PC + 1) & 0xFFFF;
          break;
      }
      // ...
    }
  }

  private void decode_2(int opcode) {
    switch (opcode) {
      case 0x20: {                                       // JR NZ,d, tal como salió
          int _nextPC51 = 0;
          if ((!((F & 0x40) == 0x40))) {
              int operand_105 = memory.read((PC + 1) & 0xFFFF, 0);     // se lee sólo si salta
              int jumpAddress2_104 = ((PC + 2 + (byte) operand_105) & 0xFFFF);
              _nextPC51 = jumpAddress2_104;
          } else {
              _nextPC51 = -1;
          }
          int nextPC_106 = _nextPC51;
          MEMPTR = nextPC_106 == -1 ? 0 : nextPC_106;                  // MemptrUpdater, tejido
          if (_nextPC51 != -1)
              contend((PC + 1) & 0xFFFF, 5, 1, Contention.Kind.READ_NO_MREQ);   // los valores de PhaseProcessor, impresos
          if (_nextPC51 == -1)
              contend((PC + 1) & 0xFFFF, 1, 3, Contention.Kind.READ);
          PC = _nextPC51 == -1 ? (PC + 2) & 0xFFFF : _nextPC51;
          break;
      }
      // ...
    }
  }

  private void decode_13(int opcode) {
    switch (opcode) {
      case 0xDD: {                                       // prefijo: R++, segundo opcode con fetch M1
          R = (R + 1 & 0x7f) | regRBit7;
          decodeDD(memory.read((PC + 1) & 0xFFFF, 1));
          break;
      }
      // ...
    }
  }
}
```

Los tres `case` son los que el generador produjo (los nombres con sufijo numérico son los locales
que el especializador renombra; los que empiezan con `_` son campos de instancia del modelo
vueltos variables). Coinciden con lo que Fuse espera: para `20_1` (tomado) un MR en 0001 y cinco
MC en 0001; para `20_2` (no tomado) un único MC en 0001 de 3 T y ningún MR. Y se ve lo que tejer
significa: la línea de MEMPTR es el visitor de `MemptrUpdater` aplanado —incluido el `0` que
escribe cuando no salta—, y las dos de contención son los dos valores que `PhaseProcessor`
produce para `JR` impresos. Si el modelo cambia cualquiera de las tres, el generado cambia con él,
y `GeneratedZ80IsCurrentTest` lo exige.

Restricciones de la forma:

- **HotSpot no compila métodos de más de 8000 bytes de bytecode** (`HugeMethodLimit`,
  `DontCompileHugeMethods=true`): un método con 256 `case` de este tamaño quedaría interpretado
  para siempre, que es peor que el núcleo OOP. Medido: una tabla entera son entre 15 y 47 KB de
  bytecode. Por eso cada tabla se parte en métodos de 16 opcodes (el más grande, 3.562 bytes),
  con un `switch (opcode >> 4)` delante: dos saltos de tabla por instrucción.
- **Lo que queda virtual**: `memory.read/write`, `io.in/out`, `contend` y los accesos a `state`.
  Son de la máquina, no del núcleo, y Z80Core hace lo mismo con `MemIoOps`.
- **Hacia afuera**: `State` recibe este mismo objeto como banco de registros, así snapshots, RZX,
  la UI y `Z80.java` siguen leyendo registros como hoy. Lo que **no** soporta el generado son
  los spies, `DebugEnabledOOZ80`, la caché de instrucciones y el translator: ésos usan el núcleo
  OOP, que es el modelo y no se va a ningún lado.

## Cómo se genera

### El especializador

Una sola operación: `specialize(objeto, método, entorno) → bloque`. Toma el AST (JavaParser) del
método en la clase de runtime del objeto —subiendo por la jerarquía si es heredado, que es el
caso de `JP.execute()` (en `ConditionalInstruction`) o `Add.execute()` (en
`ParameterizedBinaryAluInstruction`)— y lo reescribe con estas reglas:

1. **Campos de configuración** (`int`, `boolean`, `String`: `n`, `p`, `flag`, `negate`, `mode`,
   `notRegister`, las posiciones de operando) → el literal leído por reflexión de la instancia.
2. **Campos que son colaboradores** (referencias, condiciones, registros, `BlockInstruction`,
   `AluOperation`, visitors) → quedan ligados al objeto de runtime. Una llamada sobre uno de
   ellos, `source.read()`, se reemplaza por `specialize(source, read, ...)`: la recursión.
   Parámetros se ligan a las expresiones argumento, locales se renombran para no chocar, y un
   `return e` en cuerpo de una sentencia se vuelve la expresión `e`. El doble despacho de un
   visitor es este mismo caso dos veces: `instruction.accept(v)` se resuelve por el tipo de la
   instrucción, y adentro `v.visitingCall(this)` por el tipo del visitor.
3. **Terminales**, donde la recursión para: los registros del `UnrolledRegisterBank`, que quedan
   campos de la clase generada; `memory`, `io`, `state` y `contend`, que quedan campos y
   llamadas; los estáticos (`Push.doPush`, `sz53Table`, las tablas de `AluOperationBase`), que se
   copian una vez a la clase generada.
4. **Un arreglo indexado por un valor de runtime** (`table[memory.read(...)]`) sobre un
   `Instruction[]` ligado → `switch (memory.read(...)) { case k: specialize(table[k]) }`. Los
   `switch` en cascada salen de acá, no de un template.
5. **Estado de una ejecución** (el operando leído, la dirección efectiva, el próximo PC) → un
   local del `case`.
6. **Condiciones sobre la estructura de la instancia** (`instanceof`, `getName().equals(...)`,
   sondas puras) → se evalúan contra la instancia y la rama muerta se borra.
7. **Asignaciones a campos lambda** → se especializa la asignación del constructor (regla 6
   adentro) y se sigue por el método elegido.
8. **ALU** → el `calculate*` del delegado, con `F` y `flag` mapeados al campo `F`. Única regla
   que nombra una clase; está justificada porque `AluOperation` elige su función por una sonda en
   el constructor.
9. **Contención** → la lista de valores de la instancia (B), impresa: "antes de ejecutar" al
   principio del `case`; "después de la lectura k" tras la k-ésima sentencia `memory.read` de esa
   rama; "antes de la escritura k" igual; "al final" y "al final si saltó" al final, guardado por
   el resultado del salto. Como el cuerpo está aplanado, la posición es sintáctica.

**No hace falta el symbol solver de JavaParser**: los receptores se resuelven por el tipo de
runtime del objeto ligado y los campos por reflexión; lo único que se resuelve por nombre son
locales y parámetros dentro de un método. Si un caso lo pide, se agrega.

### El punto de partida

`OOZ80.execute()`, con el grafo completo: `OOZ80` → fetcher → tablas → instrucción, y el
executor con sus listeners: el aspecto de MEMPTR y el de contención, si se piden. No hay un
"marco" escrito a mano: la aceptación de interrupciones, `doInt`, el R++, la lectura del opcode y
la escritura del PC son el fuente de esas clases especializado. Por eso A, B y C van antes: hoy
ese camino tiene un procesador de test por defecto, un journal, tres envoltorios y código muerto,
y el especializador los arrastraría al generado o necesitaría una regla por cada uno.

### Dónde vive y cómo se corre

En `emulator/src/test/java`, reemplazando el prototipo (`com/fpetrola/oozx/CodeInliner` y
compañía, 971 líneas): es lo que ese código intentaba ser, necesita el JavaParser de scope test,
y no se despacha con el emulador. Se corre como ya se corre la actualización de baselines de las
tablas ALU: `mvn test -pl emulator -Dtest=GenerateZ80` regenera; `GeneratedZ80IsCurrentTest`
regenera en memoria y compara con el archivo commiteado, y falla si difieren. El archivo generado
va a `emulator/src/main/java`, commiteado y legible, como `Z80.java` de Z80Core.

## Los pasos

Cada uno deja los gates verdes y se commitea solo.

**0. Medir. HECHO en parte.** El benchmark existe (`CoreBenchmark`); el catálogo de formas no se
escribió porque el generador recorre las tablas enteras y falla en la primera forma que no cubre,
que resultó ser mejor inventario. Lo que decía: un test que recorre las siete tablas e imprime: instancias por tabla, formas
distintas (clase, clase del target, clase del source, condición) —el catálogo que el
especializador tiene que cubrir— y los lugares `null`. Un benchmark sin JMH: instrucciones por
segundo del núcleo OOP sobre el boot de la ROM de 48K (`EmulationRegressionTest` ya corre 200
frames) y sobre un programa sintético en `MockedMemory`. Baseline hoy: Fuse 1355/1355.

**1. A: los aspectos fuera del modelo. HECHO** (`49de2b1d3`, −73 líneas). Gate: Fuse, `machine`, y un grep: ningún archivo de
`cpu`, `instructions`, `opcodes` ni `registers` importa `fuse.tstates`.

**2. B: la contención como valores. HECHO** (`24775046d`, −537 líneas). Gate: Fuse evento por evento, `ZXSpectrumContendedMemoryTests`,
hashes de `EmulationRegressionTest`. El núcleo OOP sale más rápido de acá aunque no se genere
nada: no hay despacho de fases por lectura.

**3. D: MEMPTR con un dueño. HECHO** (`f395170d5`, −50 líneas). Gate: Fuse verifica MEMPTR en cada test; el translator y los tests
siguen instalando el spy como hoy. La máquina decide si lo instala (ver decisiones abiertas).

**4. C: fetch y executor. PENDIENTE**, después de revisar la propuesta: lo muerto; el área de operandos
leída una vez; los desplazamientos de índice al preparar; el prefijo que declara. Fuse después de
cada uno. Hasta entonces el generador lleva las dos reglas de deuda de C.

**5. El especializador sobre hojas. HECHO** (`16d29ae4d`), reglas 1 a 8; los dorados están como
prueba exploratoria (`SpecializerTest`) y no como texto fijado todavía: con tests dorados sobre diez instancias que
cubren todas las formas: `LD A,(IX+d)`, `LD (IX+d),n`, `ADD A,n`, `JR NZ,d`, `CALL NZ,nn`,
`LDIR`, `BIT 3,(IX+d)` (DDCB con `LdOperation`), `IN A,(n)`, `EX (SP),HL`, `ADC HL,DE`.
Reemplazan los tres dorados de `TestInlinerTest`. Los dorados se revisan a mano una vez: son la
especificación del generador.

**6. Los aspectos. HECHO.** El visitor de MEMPTR (regla 2) y la lista de contención (regla 9). Mismos
diez dorados, ahora con las líneas de MEMPTR y de `contend`, revisadas contra `MemptrUpdater` y
`PhaseProcessor` a ojo y contra Fuse después.

**7. Desde `OOZ80.execute()`. HECHO CON MARCO A MANO.** Hasta que C exista, el marco (R++, la
lectura del opcode, los prefijos con sus parámetros leídos por reflexión de
`DefaultFetchNextOpcodeInstruction`, el próximo PC) lo escribe `CoreGenerator` en unas 60 líneas; lo
que decía este paso: el especializador arranca arriba y produce la clase completa,
con las cascadas por la regla 4. `GenerateZ80` la escribe, la compila en el test (`javax.tools`),
mide los métodos y parte si hace falta. `FuseTests` y `AluReferenceTest` se parametrizan por
núcleo y corren sobre los dos. Gate: 1355 verdes también en el generado, con la misma lista de
eventos. Éste es el paso donde todo se cobra: si un `case` sale mal, Fuse dice cuál y el dorado
dice por qué.

**8. Enchufar y medir. HECHO** (`d6a5fdbc2`), default OOP. `Z80.createOOZ80` elige el núcleo por configuración (propiedad de
sistema; default OOP). Con el generado la máquina no instala el aspecto de contención —lo tiene
adentro— y le implementa `contend` con lo que hoy hace `FusePhaseProcessor`. Con el generado:
`ZXSpectrumContendedMemoryTests`, `EmulationRegressionTest` con los mismos hashes, las
grabaciones RZX, y el benchmark del paso 0. Se cambia el default cuando todo eso está verde y el
número justifica el cambio.

**9. El candado. HECHO.** `GeneratedZ80IsCurrentTest` en la suite normal, más una línea en el README de
cómo regenerar. A partir de acá, tocar una instrucción, un aspecto o el fetcher y no regenerar
rompe el build, que es lo que sostiene la consecuencia 1 del principio.

## Cómo se verifica

| oráculo | qué fija | dónde corre |
|---|---|---|
| Fuse, 1355 tests | registros, memoria, MEMPTR, T-states, **cada evento de bus con su instante** | los dos núcleos, desde el paso 7 |
| `AluReferenceTest` | todas las entradas de cada instrucción ALU contra el Z80 documentado | los dos núcleos |
| `AllTableAluOperationsCompatibilityTest` | que las tablas del OOP no cambien; el generado inlinea el mismo `calculate*` | OOP |
| dorados del especializador | el código exacto de diez formas, sin y con aspectos | generador |
| `GeneratedZ80IsCurrentTest` | que lo commiteado sea lo que el modelo produce hoy | build |
| grep de imports | que el modelo no conozca `fuse.tstates` | build, desde el paso 1 |
| `ZXSpectrumContendedMemoryTests` | contención en la máquina real, T-state por T-state | máquina, los dos núcleos |
| `EmulationRegressionTest` | hashes de pantalla, sysvars, RAM y registros tras 200 frames de boot | máquina, los dos núcleos |
| RZX | conteo de fetches por frame sobre juegos reales | máquina, los dos núcleos |
| benchmark del paso 0 | instrucciones por segundo | cada paso |

## Alternativas consideradas

- **Escribir el `switch` a mano** con Z80Core como guía. Dos dueños de la semántica, y cada
  arreglo hecho dos veces o ninguna.
- **Inlining a nivel de bytecode** con ASM (que ya está en `bytecode`): tomar el `execute()`
  compilado de cada clase y resolver los `invokevirtual` con el grafo de instancias. Misma
  velocidad, pero no da un archivo Java legible, y depurar bytecode generado es depurar a ciegas.
  El fuente es la ventaja de JavaParser.
- **Evaluación parcial en runtime** (Truffle/Graal): el grafo de instrucciones es un AST y Graal
  lo especializaría solo. Es la misma idea ejecutada por la VM; agrega GraalVM y no produce la
  clase que se pide. Vale como marco teórico: lo que se construye acá es un evaluador parcial
  offline con las tablas del decodificador como entrada estática.
- **Una clase generada por instancia** en vez de un `switch`: monomórfica adentro, pero el
  `instruction.execute()` de la tabla sigue siendo un sitio megamórfico, uno por instrucción.
  Mejor que hoy, peor que el `switch`.
- **Sólo A a D, sin generador.** Achica y aclara el modelo, saca la caché, los envoltorios y el
  despacho de fases del camino; pero los sitios megamórficos siguen ahí, así que la ganancia está
  acotada. El benchmark del paso 0 la mide; si alcanza, el generador espera.

## Decisiones abiertas

- **Nombre y paquete** de la clase generada y del generador. `GeneratedZ80` dice lo que es;
  `cpu` es donde vive `OOZ80`.
- **Commitear el generado o generarlo en el build.** El plan lo commitea con el candado del paso 9:
  es legible, diffeable y compila sin correr el generador. Generarlo en el build (`exec-maven`)
  evita el candado a cambio de que nadie pueda leer el núcleo sin construirlo.
- **Qué aspectos tejer, y para quién.** MEMPTR y contención son parámetros del generador. Hoy la
  máquina no instala `MemptrUpdater`; un generado con MEMPTR tejido se lo daría. Es una decisión
  de la máquina, no del modelo, y hay que tomarla antes del paso 8.
- **Gancho abstracto o especialización por máquina.** `contend` queda virtual (una llamada por
  grupo de MC). La alternativa es especializar también la implementación de la máquina y obtener
  un `GeneratedSpectrumZ80` con la contención de la ULA inlineada; es más rápido y acopla el
  artefacto a la máquina. Para después, si el benchmark lo pide.
- **Compartir DD y FD.** El 90 % de esas tablas es la base con HL→IX/IY; Z80Core lo resuelve con
  un parámetro. En v1 se generan enteras.
- **`UnrolledRegisterBank`**: extenderlo (sus campos pasan a `protected`) o que el generador copie
  sus campos y vistas a la clase generada desde el mismo fuente. Lo segundo no toca nada.

## Riesgos

- **Las reglas crecen hacia "un caso por instrucción".** Es el riesgo de diseño. La regla que
  nombra una instrucción se documenta como deuda del generador; el modelo no se toca por eso.
- **B cambia la forma de un aspecto que hoy pasa 1355 tests**: 35 reglas transcriptas a valores.
  Fuse las verifica evento por evento y `ZXSpectrumContendedMemoryTests` en la máquina; se hace
  regla por regla, comparando la lista de eventos antes y después.
- **JavaParser sin resolución de símbolos**: renombres de locales, sombras, `this` implícito. Se
  contiene con los dorados y con Fuse; si un caso lo pide, se agrega el symbol solver.
- **Tamaño de método y JIT.** Se mide en el paso 7 y se verifica con `PrintCompilation`; sin
  medir, el resultado es un núcleo "rápido" que corre interpretado.
- **El orden de lecturas en C.** Es la parte con más trucos acumulados; Fuse los verifica evento
  por evento, y es el único oráculo que hace falta para ese paso.
- **`UnrolledRegisterBank` lleva tiempo sin usarse** y no enmascara al escribir (`A = value`).
  Antes de apoyar el generado en él, correr Fuse con el núcleo OOP sobre ese banco: una línea en
  `FuseTestParser`, y prueba el banco por separado del generador.
- **Java 18** es el nivel del proyecto: alcanza (`switch` clásico, `instanceof` con patrón).

## Lo que quedó

Medido el 2 de septiembre de 2026, en la rama `nucleo-generado`.

| qué | resultado |
|---|---|
| `GeneratedZ80.java` | 27.400 líneas, 1.616 `case`, 7 tablas en 112 métodos de hasta 3.562 bytes de bytecode |
| Fuse sobre el generado | 1355/1355, evento por evento (`GeneratedFuseTests`) |
| ALU exhaustiva sobre el generado | verde (`GeneratedAluReferenceTest`) |
| `machine/core` con `-Doozx.cpu=generated` | 260/260, los mismos hashes de boot que el OOP |
| CPU pura, programa sintético (`CoreBenchmark`) | OOP ≈ 36–43 M instr/s, generado ≈ 133–148 M instr/s: **3,5×** |
| estado persistente que sobrevivió | 3 campos: el `nextPC` de los tres HALT, que el modelo también guarda |
| candado | `GeneratedZ80IsCurrentTest`: la generación es determinista y el commiteado es lo que el modelo produce |

Lo que el generador aprendió del modelo en el camino, y que conviene saber:

- **Los aspectos sí se tejen desde sus visitors.** `MemptrUpdater` se especializa sin cambios:
  el doble despacho se resuelve con la instancia, y los visitors anónimos se tratan como objetos
  "virtuales" cuyos métodos son el cuerpo anónimo y cuyo `this` exterior es el updater. Las
  contenciones, siendo valores, se imprimen.
- **Las cinco lambdas** se resolvieron como decía: `ConditionPredicate` es la identidad,
  `memoryWriter` se elige reevaluando la condición del constructor, `triFunction` no se toca
  porque la ALU va por el `calculate*` del delegado, y las dos del fetch no se especializan
  porque el marco es a mano.
- **Lo que un objeto compartido guarda entre ejecuciones** (`address`, `value`, `nextPC`,
  `jumpAddress`, el `F` de una operación) se vuelve variable del `case` cuando se asigna antes de
  leerse en todo camino, y campo de la clase cuando no; sólo HALT necesitó el campo.
- **Deudas del generador** (reglas que existen porque C no está hecho): las lecturas repetidas de
  un byte de operando son una (`MemoryForOpcodes`), los envoltorios del fetch se inlinean como
  parte del grafo, y el marco es a mano.

Para revisar a la mañana:

- El código generado se lee, pero los nombres (`value2_290`, `_F181`) son los del especializador;
  un pase de nombres bonitos es cosmético y se puede hacer sobre los dorados.
- Decidir cuándo el generado pasa a ser el default de la máquina (hoy: propiedad de sistema).
- C, con la propuesta de arriba.

