# La configuración: un archivo, una sección por parte, cada sección una clase

`Settings` es una struct de configuración de C transliterada: 253 campos planos en una sola clase, más
seis métodos que tiran `UnsupportedOperationException`. No persiste nada — lo que se guarda en
disco es `OOZxConfiguration`, que es del escritorio. Este plan lo reemplaza por un solo archivo
JSON dividido en secciones, donde **cada sección es la serialización de una clase Java y nadie lee
el archivo como un mapa**: quien necesita configuración recibe su clase, y solo la suya.

## Lo que se midió, el 6 de septiembre de 2026

| | |
|---|---|
| campos declarados en `Settings` | 253 |
| que alguien lee | 97 |
| que solo se escriben | 2 |
| que no se nombran en ningún lado | 154 |

Los 97 vivos se agrupan solos, y en los tres grupos grandes el dueño ya existe:

- **`Input`: 38.** Los quince disparos de cada joystick, su salida, y el teclado como joystick.
- **Las ROMs: 29, más sus 29 gemelas en `defaults`.** Cada máquina lee las suyas y cada
  periférico la suya; `loadRom(elegida, la_que_viene, …)` es el par que hay que conservar.
- **Los periféricos: unos 21.** Entre uno y cinco cada uno, leídos solo por su propio módulo.
- **Los realmente compartidos: unos diez.** `emulationSpeed`, `sound`, `soundDevice`, `fastload`,
  `lateTimings`, `writableRoms`, `issue2`, `startMachine`, `joyKempston`, `kempstonMouse`.

## El mecanismo

`Configuration` lee un archivo, lo guarda, y entrega secciones. Una sección se pide por su clase y
se nombra con `@Section`, cuyo nombre es un **camino**: `machine.plus3` es la sección `plus3`
adentro de `machine`, no una clave con un punto. Una sección que contiene a otras se escribe sin
llevárselas puestas. El JSON solo lleva lo que difiere: **los valores por defecto son los
inicializadores de campo de la clase**, y lo leído se aplica encima con `readerForUpdating` de
Jackson, que ya es dependencia. Eso hace que agregar un ajuste no toque ningún archivo guardado, y
que una sección de un módulo que este build no tiene se conserve intacta en vez de perderse.

Hubo por un rato una tercera capa, un JSON de defaults empaquetado en el jar que se leía antes que
el de afuera. No hacía falta como capa: los defaults ya están en las clases, y el archivo los
repetía, o sea dos fuentes de verdad sostenidas por un test. Pero **ver toda la configuración en un
solo lugar sí vale**, así que ese archivo dejó de ser una capa y pasó a ser `doc/configuracion.json`,
**generado** desde las clases: lo escribe un test que arma una máquina con todo enchufado, le pide
cada sección que este build tiene y la guarda. No se lee nunca. Un `IsCurrent` falla si alguien
cambia una clase y no lo regenera, como con el núcleo generado.

```java
@Section("sound")
public class SoundConfig {
  public boolean enabled = true;
  public String device = "buffer=8192,frames=4,verbose";
  public boolean whileLoading = true;
}
```

Nadie recibe `Configuration`: cada módulo declara sus secciones y sus partes reciben la suya.

```java
Configuration.section(binder, SoundConfig.class);   // en el módulo de Guice
@Inject Sound(SoundConfig config) { … }             // en la parte
```

## El OOP que estas clases sí tienen

No tienen comportamiento, pero sí forma:

- **Reuso de subinstancias.** `joystick1` y `joystick2` son dos instancias de la misma clase, no
  treinta campos numerados. Una ROM es una instancia de `Rom` y una máquina tiene las suyas.
- **Polimorfismo de estructura.** Un joystick de gamepad y el teclado usado como joystick son dos
  formas del mismo concepto: la salida es común, los controles no. Lo mismo entre periféricos con
  EPROM y protección de escritura — DivIDE y DivMMC son la misma forma dos veces.

## Los pasos, cada uno un commit con los gates en verde

| | paso | qué prueba que quedó bien |
|---|---|---|
| 1 | borrar los 154 campos muertos de `Settings` | compila; 253 campos quedan en 99 |
| 2 | `Configuration`, `@Section`, el binder, el archivo | un archivo que nombra un campo conserva los defaults de los demás, en todo nivel; una sección desconocida sobrevive a una vuelta de lectura y escritura |
| 3 | sonido, velocidad, memoria, máquina | las suites enteras |
| 4 | las ROMs, con `Rom` y el par elegida/la que viene | cada máquina arranca con su ROM y cae a la que trae |
| 5 | `Input`: los 38 en tres instancias de dos clases | el teclado y los joysticks siguen mapeando |
| 6 | un commit por periférico, cada uno con sus tests | los tests de cada dispositivo |
| 7 | el escritorio: `OOZxConfiguration` pasa a ser una sección | favoritos, recientes y ventanas sobreviven a la migración |
| 8 | se van `Settings` y `SettingsInfo` | nadie los nombra |

## Decisiones

- **El archivo sigue siendo `~/.oozx/config.json`.** Lo que hay hoy es la configuración del
  escritorio en la raíz; al leer un archivo sin secciones, esa raíz se toma como la sección del
  escritorio, así que nadie pierde sus favoritos.
- **Una sección que este build no conoce se conserva.** Desenchufar un periférico no borra su
  configuración.
- **`Settings` es `@Singleton` dentro del inyector de cada `Speccy`**, o sea por máquina. Las
  secciones se comportan igual: dos máquinas abiertas tienen configuración independiente, y
  guardar es una acción explícita de quien la cambió.
