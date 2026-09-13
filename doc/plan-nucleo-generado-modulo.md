# El núcleo generado como módulo aparte, generado en tiempo de ejecución

El generador funciona. Lo que quedó mal es dónde vive y cómo se elige: el generador es código de
test del emulador, el generado es un fuente de 27 000 líneas commiteado en `machine/core`, la
máquina lo elige con un `if` sobre `-Doozx.cpu=generated` y otro sobre `Emulation.noTest`, y el
`Z80` importa las dos clases generadas. Este plan lo saca de la base, lo hace transparente para
quien lo usa, y lo genera al arrancar con un cache afuera del jar.

## Lo que se midió, y lo que decide

| paso | tiempo | tamaño |
|---|---|---|
| generar `GeneratedSpectrumZ80` (especializador sobre el modelo vivo) | 11,1 s | 956 KB de fuente |
| compilarlo en memoria con `javax.tools` | 1,8 s | 322 KB de clases |
| cargar las clases ya compiladas | milisegundos | |

Trece segundos por arranque es demasiado: **va cache**. Y como recompilar desde el fuente cuesta
menos de dos segundos, el cache guarda las dos cosas: el `.java`, que es lo que uno quiere mirar,
y las clases, que es lo que hace el arranque en frío instantáneo. La clave del cache es el hash de
lo que entra al generador: los fuentes del modelo y el generador mismo. Si se toca una
instrucción, la próxima corrida regenera sola.

## El módulo: `machine/generated`, artefacto `generated-core`

Depende de `core`, de `emulator` y de su test-jar (por `FuseTestParser`), y de `javaparser`, que
deja de ser dependencia de test de dos módulos y pasa a ser dependencia normal de uno. La base
del emulador no depende de él y no sabe que existe.

| qué | hoy | después |
|---|---|---|
| `Specializer`, `CoreGenerator`, `Simplifier`, `Folder`, `SourceIndex` | test de `emulator` | `generated/src/main` |
| `GenerateZ80`, `GenerateSpectrumZ80` | tests que escriben en `src/main` de otro módulo | entradas del módulo, opt-in con `-Doozx.generate=true`, que escriben la copia de referencia |
| `GeneratedZ80.java`, `GeneratedSpectrumZ80.java` | `src/main` de `emulator` y `core` | `generated/src/test`: copias de referencia, para leer y verificar; no viajan en el jar |
| `GeneratedFuseTests`, `GeneratedAluReferenceTest`, `CoreBenchmark`, `SpecializerTest`, `SpecializeMemoryTest`, los dos `IsCurrent` | tests de `emulator` y `core` | tests de `generated` |
| los fuentes del modelo | en disco, leídos por ruta relativa | copiados al jar en `META-INF/model-sources` al construir, pasando por `target/model-sources` para que el IDE no tome `emulator/src/main/java` como root de este módulo; sin índice: el jar (o el directorio de clases) se recorre como árbol y `SourceIndex` lo lee igual que un directorio; un build sin Maven que no los empaquete corre el OOP y lo dice (HECHO) |
| `FuseTestParser(…, boolean generated)` | elige el núcleo con un booleano e importa `GeneratedZ80` | recibe un `Core`; el booleano se va (HECHO) |

## La costura: quien usa el núcleo no sabe cuál es

En `core`, una interfaz con lo que el `Z80` le pide a un núcleo, y nada más:

```java
public interface Core {
  RegisterBank bank(...);          // el banco de registros: el generado ES el banco
  OOZ80 cpu(State state);          // el procesador sobre ese estado
  boolean countsItsOwnContention(); // la memoria envuelta no le avisa al aspecto de contención
}
```

La interfaz quedó en `emulator` (`com.fpetrola.z80.cpu.Core`) y no en `core`, porque el arnés de los vectores
le pide lo mismo a un núcleo que la máquina: `bank(memory, io)`, `cpu(state, contention)` y
`countsItsOwnContention()`. Un solo dueño para las dos costuras. `GeneratedZ80Core`, en los tests del
módulo, es el núcleo puro como `Core` y es lo que corre los 1355 vectores.

`OopCore` la implementa en `core` y es el default por `OptionalBinder` en `EmulatorModule`.
`GeneratedMachineCore`, en el módulo nuevo, la reemplaza con `setBinding`, y el módulo se descubre
por `ServiceLoader` como ya se descubren los dispositivos. `Z80` pierde `generatedCore()`, el `if`
de `Emulation.noTest`, el `!generatedCore()` de `ContendedMemory` y los dos imports. El núcleo
puro `GeneratedZ80` sobre la memoria instrumentada, que hoy sale del `if` de `noTest`, pasa a ser
una tercera implementación que ata el arnés de tests que lo necesita, no la máquina.

El `contend` que hoy se sobreescribe en una subclase anónima no se puede escribir sobre una clase
cargada en tiempo de ejecución: el generado pasa a recibir en el constructor a quién contarle la
contención. Misma llamada virtual que hoy, un campo `final` en vez de una subclase. Medido en el
paso 1: `GeneratedSpectrumZ80` no llama nunca a ese hook (la contención está inlineada en
`contendNxM`); solo lo declara abstracto. Así que el generado contra la máquina puede salir concreto
y sin hook, y la inyección por constructor queda solo para el núcleo puro, que sigue compilado en
los tests.

## La canilla: `GeneratedCores`, en el módulo

1. Calcula la clave: SHA-256 de `META-INF/model-sources` más los fuentes del generador.
2. Busca `~/.cache/oozx/generated/<clave>/`. Si están las clases, las carga con un `URLClassLoader`
   hijo del de la aplicación y las instancia contra los objetos vivos de la máquina. Milisegundos.
3. Si no: genera con el `CoreGenerator` sobre una máquina propia, compila con `javax.tools`, escribe
   `GeneratedSpectrumZ80.java` y las clases en ese directorio, y carga. Trece segundos, una vez,
   con un aviso. Si no hay compilador porque corre sobre un JRE, se queda con el `OopCore` y lo
   dice. Ese es el único `if` del feature, y vive adentro de la canilla.
4. Un setting `fastCore`, prendido por defecto, para elegir el OOP desde la ventana cuando se
   quiera depurar. Lo lee la canilla, nadie más.

## El orden, cada paso un commit con los gates en verde

| | paso | qué prueba que quedó bien |
|---|---|---|
| 1 | HECHO 2026-09-05: el módulo, las mudanzas de la tabla, `javaparser` fuera de la base, `FuseTestParser` invertido sobre `Core` | los tests generados corren en el módulo nuevo; `emulator` y `core` no mencionan `Generated*` |
| 2 | HECHO 2026-09-05: `OopCore` en `emulator` (uno solo, para el arnés de los vectores y la máquina), el default por `OptionalBinder`, `EmulatorModule.core(clase)` para atar otro, `Z80` sin ifs; `ModelCore` en el módulo es la máquina que lee el generador; el generado contra la máquina sale concreto y sin hook | suites enteras con el OOP; `GeneratedMachineCoreTest` arranca la ROM 300 cuadros en el OOP y en el generado y compara RAM, registros y reloj, en los dos modos de la máquina |
| 3 | HECHO 2026-09-05: `GeneratedCores` es el `Extension` descubierto por `ServiceLoader` y la canilla (clave, cache, generar, compilar con `javax.tools`, cargar); `GeneratedMachineCore` instancia la clase cargada sobre los objetos de la máquina; la contención inyectada no hizo falta (el hook no existe en el generado de la máquina) | `GeneratedCoresTest`: la primera máquina genera y guarda fuente y clases, la segunda carga sin regenerar, y el fuente guardado es la copia de referencia; `GeneratedMachineCoreTest` sobre el núcleo descubierto; `jsw-full.rzx` se mide en el paso 4, cuando `app` lo tenga |
| 4 | HECHO 2026-09-05: `app` depende de `generated-core`; `-Doozx.cpu` se fue en el paso 1; el setting es `Emulation.fastCore`, persistido en la config (`OOZxConfiguration.fastCore`), leído por la canilla, puesto por el escritorio al arrancar y por la casilla "Fast Core" del diálogo de settings para las máquinas que se abran de ahí en más | los tests de `app` corren en el generado sin flags (39, 0 fallos); `RzxCoreMeasurement` mide `jsw-full.rzx` en los dos núcleos en una corrida: A/B contra el commit anterior al plan (`c83c514a3` con `-Doozx.cpu=generated`), tres rondas intercaladas, una JVM por núcleo, mejor corrida: playback generado 21 186 % antes / 20 773 % después, sesión 26 942 % / 26 253 %; OOP playback 3 819 % / 3 841 %, sesión 4 472 % / 4 380 %. Paridad dentro del ruido, que en esta notebook es de ±15 % entre corridas iguales. La tercera parte de la medición, el loop propio tras `release()`, nunca funcionó: también en el commit viejo se cuelga a los 60 s |
| 5 | HECHO 2026-09-05: README (sección "The generated Z80 core"), memoria del proyecto | |

## Decisiones abiertas

- El cache en `~/.cache/oozx/generated` (o `$XDG_CACHE_HOME`) y no en config: es regenerable. Fallback a `java.io.tmpdir`. `-Doozx.cache=<dir>` lo cambia; los tests del módulo usan `target/generated-cache`. La clave lleva también la versión de Java: clases compiladas en una JVM más nueva no cargan en una más vieja.
- En modo test (`Emulation.noTest == false`) la canilla da el `OopCore`: la memoria instrumentada del arnés es otra contención, y el generado contra la máquina no pasa por ella.
- Sobre un JRE sin `javac` corre el OOP. Empaquetar ECJ para compilar sin JDK se puede después.
- ~~El núcleo puro `GeneratedZ80` sigue existiendo solo como artefacto de test y benchmark.~~ Eliminado
  el 2026-09-05: fue un paso intermedio y `GeneratedSpectrumZ80` lo superó. Con él se fueron
  `GeneratedFuseTests`, `GeneratedAluReferenceTest` y `CoreBenchmark`, que eran las baterías del modelo
  copiadas para pasarles otro procesador. La regla que quedó: los tests no cambian por tener otra
  implementación del procesador; se la enchufa y se corren los mismos. `MachineOnTheGeneratedCoreTests`,
  en el módulo, es la suite de `model.tests` de `core` tal cual, corrida con el módulo en el classpath.
  Y las baterías del emulador (los vectores de CPU 1355, ALU, emustudio 226) piden su procesador a
  `ProcessorUnderTest`, que es un `Core` más una memoria para leer y escribir y la lista de eventos que
  reporta, descubierto por `ServiceLoader` con el OOP como default; el módulo registra
  `GeneratedUnderTest`, que arma el generado sobre 64K propios del arnés (páginas escribibles, sin
  contención, sin pantalla) con los objetos de una máquina silenciosa que nunca toca, y
  `EmulatorOnTheGeneratedCoreTests` corre esas baterías tal cual. Lo único que el generado no reporta
  son los eventos de memoria (MR/MW/MC), porque llega a la memoria sin llamada: el emulador de referencia compara sobre él
  registros, memoria, T-states y eventos de puerto. Las 1592 pasan.
