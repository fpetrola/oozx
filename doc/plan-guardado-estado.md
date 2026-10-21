# Guardar el entorno y que vuelva exactamente igual

Paso 3 de `plan-version-1.md`. Cerrar OOZX y abrirlo tiene que dejar cada máquina, cada ventana y
cada aparato enchufado exactamente como estaban. Cada máquina se guarda en un SZX con todos los
bloques del formato que conoce más bloques propios del entorno; lo que no es de una máquina va al
JSON del entorno. Ningún plugin sabe cómo se escribe nada: cada uno dice qué guarda y cómo se
restaura, y el entorno base es el único que conoce el formato de lo suyo.

**Estado (21 de octubre de 2026):** hechos el paso 1 y lo principal del 0 y del 2 (ver cada uno).
Relevado sobre `traduccion` (oozx, `68d400341`) y `prueba-apuntar` (oozx-plugins, `0e3c6b2`).

## Cómo está hoy

Todo vive en `~/.oozx/config.json`, en la sección `desktop` (`OOZxConfiguration`):

- `openWindows` es una lista de `WindowState`: por cada ventana de máquina, x, y, ancho, alto,
  `zOrder`, el archivo de origen, turbo, pausa, mudo, `snapshotId` y los pokes aplicados.
- `snapshots` guarda cada máquina como texto: `"szx:"` más el SZX comprimido con gzip y pasado a
  caracteres (`Snapshots.packed()`, `SnapshotSaver.packed`).
- El SZX (`SzxFormat`, en `devices/snapshots`) lleva Z80R, SPCR, KEYB, AY, PLTT, RAMP y CRTR. De
  cualquier otro periférico escribe "not carried" y lo pierde. Los bloques que no conoce, al leer,
  los saltea con una nota y no los conserva.
- Las ventanas de escritorio que vuelven son las que implementan `KeepsItsPlace` (`GamesFrame`,
  `HistoryFrame`), escribiendo en el mismo `WindowState`. `MachineTool.remember/restore` es el
  único gancho de un plugin en la ventana de una máquina, y solo se usa para los pokes.
- No se guarda: la posición, el tamaño, el lado ni el modo compacto de ninguna ventana de equipo;
  qué aparatos estaban enchufados; el aspecto de cada máquina (escalador, filtros, perfiles: solo
  los valores por defecto globales); el volumen y la velocidad de cada una; la cinta, los
  discos, cartuchos y tarjetas que tenían adentro.
- Los valores de `Settings.mirror` son globales: al guardar, cada máquina escribe los suyos encima
  y queda el de la última creada.

Lo que está roto (leído en el código; ninguna prueba lo cubre):

1. **Al arrancar, ninguna máquina vuelve.** `restoreOpenWindows()` corre dentro del constructor de
   `ZXSpectrumDesktopApp`. Llega a `appHolder[0].registerMachine(...)` (`OOSpectrumLauncher`), y
   `appHolder[0]` se asigna recién cuando el constructor termina: NPE, tragado por el `catch` de
   `reopen` ("Error restaurando snapshot desde configuración: null"), con el hilo de la máquina
   ya arrancado. Después `config.getSnapshots().clear()` borra lo guardado.
2. **Archivo > Salir no guarda:** es `System.exit(0)` y no pasa por `windowClosing`.
3. **Un guardado a mitad de sesión deja `openWindows` apuntando a snapshots que ya se borraron.**
4. **Las máquinas sin `snapshotModel()`** (Pentagon, Scorpion, TC2048, SE, NTSC, +3e y los clones)
   no se pueden escribir en SZX: `Shape.banksOf(null)` revienta, y la ventana no vuelve.
5. **Se escriben y nunca se leen:** `muted`, `snapshotName`, `zOrder`. La ventana principal se
   restaura y después `showOnScreen` la vuelve a centrar.

## El principio

- **Una máquina es un archivo SZX** que cualquier otro emulador puede abrir. Lleva los bloques
  estándar de todo lo que el formato conoce, de la máquina y de cada periférico. Lo que el estándar
  no tiene lugar para guardar (las ventanas de esa máquina, su aspecto, su volumen, los aparatos sin
  bloque propio, lo que un aparato tiene de más) va en un bloque propio del entorno, que los demás
  emuladores saltean.
- **El entorno es un JSON**, el `config.json` de hoy: la ventana principal, la lista de máquinas
  abiertas y en qué orden, las ventanas de escritorio, y lo de cada plugin que no es de una
  máquina.
- **Nadie conoce lo que guardan los demás.** Lo que quiere volver igual implementa un único rol y
  escribe y lee sus valores por una API neutra. No sabe si terminan en JSON, en un bloque SZX o en
  otra cosa.
- **Cada formato tiene un solo dueño.** El JSON y el bloque propio son del entorno base; el SZX es
  del plugin de snapshots. Ningún aparato tiene bytes de un formato.

## El contrato

En `machine/core`, junto a `Configuration`. No va en `KeepsItsPlace` ni en `MachineTool` porque
están atados a `WindowState`, una clase con campos fijos del escritorio. Lo nuevo los reemplaza a
los dos, que quedan deprecados mientras haya plugins publicados que los usen (`plugin-binary-api`).

`Keeps` no es un rol (`@RoleInterface`): el procesador de crystal indexaría como plugin a todo el
que lo implemente, y los periféricos y las ventanas se construyen con Guice, sin constructor vacío.
Se encuentra con `instanceof` en lo que el entorno ya tiene. Si un plugin necesita guardar algo que
no es una ventana ni una parte de una máquina, se le da un rol aparte.

```java
/** Lo que vuelve como estaba: dice qué es ahora, y después se lo dicen de vuelta. */
public interface Keeps {
  /** Bajo qué nombre, estable y único entre los que se guardan juntos. */
  String keptAs();
  void keep(Kept into);
  void restore(Kept from);
}

/** Valores con nombre, sin formato: lo que se pone se lee igual, y lo que no está da lo que se pide. */
public interface Kept {
  Kept put(String name, String value);   // y long, double, boolean, byte[]
  String text(String name, String otherwise);   // y number, real, flag, bytes
  Kept in(String name);                  // un grupo dentro de este
  List<Kept> all(String name);           // varios del mismo tipo: discos, ventanas
  Kept add(String name);
  boolean has(String name);
}
```

`Kept` es una interfaz porque cada formato la implementa sobre lo suyo. El entorno base la
implementa sobre el árbol de Jackson que ya usa `Configuration`, adentro de esa clase, que es la
dueña del JSON: `Configuration.fresh()`, `bytes(kept)` y `kept(bytes)` para llevar valores dentro
de otro archivo, y `configuration.kept(nombre)` para lo del entorno, que se guarda con el resto. El plugin de snapshots la lee y la
escribe cuando un bloque estándar tiene lugar para esos valores.

## Quién guarda qué

| Dónde | Quién | Ejemplos |
|---|---|---|
| Bloques estándar del SZX | el plugin de snapshots, desde las partes del recorrido (`Speccy.accept`) y desde los `Kept` de los aparatos | CPU, memoria, paginado, borde, AY, ULAplus; MFCE, IF1 y MDRV, B128 y BDSK, PLSD, OPUS, +3 y DSK, DIDE, DMMC, ZXAT, ZXCF, SIDE, COVX, DRUM, AMXM, ZXPR, JOY, TAPE, SCLD, DOCK, USPE, ROM |
| Bloque propio del entorno en el SZX | el entorno base, con los `Kept` de todo lo que es de esa máquina y el estándar no lleva | la ventana de la máquina (lugar, tamaño, orden), su aspecto, volumen, velocidad, turbo, pausa; qué aparatos están enchufados; cada ventana de equipo enganchada (qué equipo, de qué lado, tamaño, compacta) con lo suyo; lo que un aparato tiene de más que su bloque estándar; los aparatos sin bloque estándar (µSource, impresora paralela, Melodik, Fuller) |
| JSON del entorno | el entorno base, con los `Kept` de lo que no es de ninguna máquina | la ventana principal; las máquinas abiertas, en orden, y su archivo; las ventanas de escritorio (catálogo, historial, favoritos, ajustes, plugins); lo de cada plugin que no es de una máquina |

Se juntan por dónde están, no por un registro. Lo de una máquina es lo que aparece al recorrerla
(las partes y los periféricos activos), su ventana con sus herramientas, y las ventanas de equipo
enganchadas a ella. Lo del entorno son las ventanas del escritorio que no son de una máquina. En
cada caso el entorno pregunta `instanceof Keeps` y nada más.

## El bloque propio

El SZX admite bloques con un identificador de cuatro letras que los lectores que no lo conocen
saltean. El entorno usa uno, `OOZX`, con todos los `Kept` de la máquina que no fueron a un bloque
estándar: un árbol por nombre, codificado por el entorno base (`Configuration.bytes`; puede cambiar
sin tocar ningún plugin). El plugin de snapshots no sabe qué hay adentro: recibe bytes para
escribir con ese nombre, y al leer devuelve los bloques que no conoce en vez de descartarlos.

Para leer se reusa `SnapshotFormat.carried(bytes)`, que ya devolvía lo que un EZX lleva además de
la máquina (los `gfx` y `cfg` de Spec256): en un SZX son los bloques que no lee. Para escribir,
`SnapshotFormat` gana el simétrico, con una versión por defecto que avisa y escribe solo la máquina
en los formatos sin lugar (Z80, SNA, SP):

```java
default byte[] write(Speccy machine, Map<String, byte[]> carrying, Consumer<String> notes)
```

En el motor (`libs/formats`), `SnapshotFile.carry/carried` guarda lo que se lleva; `DeclaredFormat`
lo pasa al escribir y lo devuelve en `carried`; `SzxBlocks` escribe esos bloques al final y guarda
los que no conoce al leer.

## Los archivos

- `~/.oozx/session/<n>.szx`: una por ventana de máquina abierta. Se puede abrir con Fuse.
- `~/.oozx/config.json`: lo de hoy; `openWindows` nombra el archivo de cada máquina en vez de un
  `snapshotId`, y desaparece el mapa `snapshots`. Al leer una configuración vieja se migra una vez:
  cada snapshot empaquetado se escribe como archivo.
- `config.json` sigue teniendo los valores por defecto de las máquinas nuevas (aspecto, volumen,
  joystick). Lo de cada máquina abierta va en su SZX y ya no se pisa entre máquinas.

## El orden al volver

1. El entorno lee su JSON: la ventana principal, sin volver a centrarla.
2. Por cada máquina, en el orden en que estaban: el formato lee el SZX; elige el modelo;
   enchufa los aparatos que nombran los bloques estándar y el bloque propio; recorre la máquina,
   y cada parte toma lo suyo de los bloques estándar; después, cada `Keeps` toma lo suyo del bloque
   propio, que se agrega a lo que el estándar ya trajo.
3. Se abre su ventana con su lugar, tamaño, aspecto, volumen, velocidad y pausa; se reabren sus
   ventanas de equipo, cada una enganchada del lado y del tamaño que tenía, y cada una restaura lo
   suyo.
4. Se reabren las ventanas de escritorio y se respeta el orden de todas.

Al guardar, lo mismo al revés. Se guarda al cerrar por cualquier camino (la ventana o Archivo >
Salir) y no hay un guardado a medias que deje referencias sueltas.

## Decisiones

1. **Dónde va el mapeo de los bloques estándar de los aparatos.** Recomendado: en el plugin de
   snapshots, que es el dueño del SZX, leyendo y escribiendo el `Kept` de cada aparato por su
   nombre (`multiface`, `interface1`...), sin depender de las clases de los aparatos. Así un SZX con
   un bloque de un aparato que no está instalado se puede leer y volver a escribir sin perderlo. La
   alternativa, que cada plugin traiga su bloque con sus bytes, pone formato dentro de los
   aparatos, que `plan-snapshots.md` descartó. El AY y ULAplus, hoy atados por clase, pasan a lo
   mismo al final, y `devices/snapshots` deja de depender de los plugins de aparatos.
2. **Ajustes por máquina o globales.** Recomendado: en la sesión van los de cada máquina; en
   `config.json` quedan los valores para las máquinas nuevas.
3. **Los medios.** Recomendado: si el archivo está igual que cuando se abrió, se guarda su ruta;
   si la máquina escribió en él (un disco formateado, un microdrive), se guarda la imagen adentro,
   como prevén los bloques DSK, BDSK, PDSK, ODSK y MDRV.

## Los pasos

Uno por commit. Cada uno nace con su prueba en rojo.

0. **Lo que está roto.** La sesión se restaura después de que el lanzador registró la aplicación;
   Archivo > Salir guarda como cerrar la ventana; un guardado a mitad no deja referencias sueltas;
   la ventana principal no se recentra. Prueba: un escritorio que guarda dos máquinas y vuelve a
   levantarlas.
   **Hecho en el código** (`ZXSpectrumDesktopApp.comeBack()` y `keepTheSession()`; ya no se borran
   los snapshots al arrancar, los huérfanos los limpia `cleanOrphanSnapshots` al guardar). La prueba
   no se pudo escribir: ninguna prueba arma el escritorio (es un `JFrame`) y no hay Xvfb. Llega con
   el paso 3, cuando la sesión sea una unidad que se prueba sin Swing.
1. **El contrato.** `Keeps` y `Kept` en `core`; el `Kept` sobre Jackson del entorno base. Pruebas:
   ida y vuelta de cada tipo, grupos, listas, bytes; lo que no se pidió vuelve igual.
   **Hecho** (`KeptTest`, 4 pruebas).
2. **El SZX lleva bloques ajenos.** Los dos métodos de `SnapshotFormat`; `SzxFormat` escribe los
   que le dan y devuelve los que no conoce. Más los identificadores de máquina del SZX que faltan
   (6 a 16: +3e, Pentagon, TC2048, TC2068, Scorpion, SE, TS2068, Pentagon 512 y 1024, NTSC, 128Ke)
   para que toda máquina se pueda guardar. Pruebas: ida y vuelta; libspectrum lee el archivo igual,
   salteando el bloque propio.
   **Hecho lo de llevar bloques** (`ASessionIsKeptAsAnSzxTest`, 2 pruebas nuevas, una contra
   libspectrum). Faltan los identificadores de máquina 6 a 16.
3. **La sesión en archivos.** Un SZX por máquina en `~/.oozx/session/`, con el bloque `OOZX`;
   `config.json` los nombra; la migración de los snapshots empaquetados. Pruebas: ida y vuelta de
   una sesión; migración de un `config.json` viejo.
4. **Lo de cada máquina en el entorno.** La ventana de la máquina, su aspecto (`ScreenSettings`),
   volumen, velocidad, turbo, pausa y mudo; las herramientas (`MachineTool` pasa a `Keeps`); las
   ventanas de equipo enganchadas (`AttachedFrame` guarda lado, tamaño y compacto; `MachineFrame`
   dice qué equipo es); qué aparatos están enchufados. Prueba: guardar, volver y comparar cada valor.
5. **El motor del SZX para los bloques que faltan.** Bloques repetidos (un BDSK por disquetera),
   largos variables, páginas de 8K y 32K, enchufar los aparatos que nombra un bloque. Prueba: los
   fixtures de libspectrum que ya están en `szx-chunks/`.
6. **Cada aparato, uno por commit**, por lo que más se usa: +3 (disco), Beta 128, +D, DISCiPLE,
   Opus, Multiface, Interface 1 y microdrives, Interface 2, DivIDE, DivMMC, ZXATASP, ZXCF, Simple
   IDE, joystick, mouse, Covox, SpecDrum, ZX Printer, cinta (TAPE), Timex (SCLD, DOCK), µSpeech,
   ROM propia. Cada uno implementa `Keeps`, el plugin de snapshots mapea su bloque, y se prueba
   contra libspectrum y con ida y vuelta.
7. **Lo del entorno.** Las ventanas de escritorio pasan de `KeepsItsPlace` a `Keeps`; los `Keeps`
   de plugins que no son de una máquina, encontrados como rol. Prueba: guardar y volver el
   escritorio entero.
8. **"Exactamente igual".** Una prueba que arma varias máquinas con aparatos, medios y ventanas,
   cierra, levanta un escritorio nuevo y compara: el SZX que escribe cada máquina, byte a byte, y el
   lugar, tamaño y estado de cada ventana.

## Lo que no entra

- Guardar varias sesiones con nombre o elegir cuál abrir: hay una sola, la última.
- Rebobinar: es otra cosa, aunque va a usar este mismo SZX.
- Los bloques de aparatos que el entorno no emula (General Sound, Spectranet, LEC).
