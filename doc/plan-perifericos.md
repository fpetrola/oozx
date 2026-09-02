# Migrar los periféricos que faltan de Fuse

Plan para traer a este emulador todo lo que Fuse ofrece para enchufar y todavía no está acá, cada
uno en un submódulo maven propio, con la misma funcionalidad que tiene en Fuse y con su ventana en
el escritorio: la que se clipea a una máquina por el borde pegajoso, que es el cable.

Medido el 2 de septiembre de 2026 sobre `fuse-emulator-fuse/peripherals` (23.572 líneas de C) y
sobre `machine/devices` (33 tests, 2 de ellos piden un display y no corren sin X).

## Lo que hay y lo que falta

Fuse registra 47 tipos de periférico. Se reparten así:

| estado | cuáles |
|---|---|
| **lo que una máquina trae, ya en `machine/core`** | paging 128/+3/SE, ULA (las dos), AY (128, +3), uPD765 del +3 |
| **ya migrados, en `machine/devices`** | ZX Printer (las dos decodificaciones), Kempston (estricto y laxo), Kempston Mouse, Melodik |
| **faltan, y son de este plan** | Interface 2 · Multiface 1/128/3 · +D · DISCiPLE · Beta 128 · Opus Discovery · Didaktik 40/80 · impresora paralela (+2A/+3) · Covox · SpecDrum · Fuller Box · Interface 1 (microdrives, RS232, ZX Net) · DivIDE · DivMMC · ZXMMC · Simple 8-bit IDE · ZXATASP · ZXCF · Currah uSpeech · Currah uSource · SpeccyBoot · Spectranet · TTX2000S |
| **fuera del plan, con razón** | SCLD, AY Timex, dock de cartuchos (`dck`): son hardware de los Timex, y acá no hay máquina Timex todavía. Beta128 *del Pentagon* y paging Pentagon 1024: son la computadora, pero el Beta del Pentagon se vuelve real de paso cuando se migre el Beta 128 (mismo chip, misma clase, distinto `fitsOn`, como AY y Melodik) |

## Lo que tienen en común, y dónde vive cada cosa

Antes del primer periférico hay que dejar cuatro piezas, porque los 23 las usan y ninguna es de
un periférico en particular.

### 1. `machine/devices` pasa a ser padre, y cada periférico un submódulo — HECHO

```
machine/devices/                 pom padre (packaging pom)
  kit/                           lo que todo periférico enchufable tiene: ver abajo
  printer/ mouse/ joystick/ melodik/      los cuatro que ya están, movidos tal cual
  interface2/ multiface/ plusd/ ...       uno por periférico de este plan
  all/                           jar vacío que depende de todos; la app depende de éste
```

Cada submódulo depende de `core`, `ui` y `kit`, y de otro periférico sólo cuando se conecta a él
(el +D y el DISCiPLE tienen puerto de impresora: dependen de `parallel-printer` y le mandan lo que
el programa imprime). La app depende de `devices-all`, así sigue sin nombrar ningún periférico:
los encuentra por `ServiceLoader`, que es la frase de `plan-modulos.md`. `devices-all` es también
donde viven los tests que necesitan a todos (`PeripheralPresenceTest`).

### 2. `kit`: la parte de periférico que todos repiten — HECHO

Hoy `PrinterInternalFrame` y `MouseInternalFrame` tienen el mismo código para lo mismo: al
clipearse, buscar el periférico en la máquina de esa ventana, `plugIn(true)` en el hilo del
emulador y `peripherals.update()`; al soltarse, lo inverso; al cerrarse la máquina, soltar. Son 25
líneas por ventana, y con 23 ventanas más serían 600. Van a `kit` una vez:

- **`PluggablePeripheral`** (en `core`, porque no tiene Swing): un `Peripheral` que se enchufa
  desde afuera, con `plugIn(boolean)`; `isWanted()` contesta desde ahí. Es lo que
  `ZxPrinterPeripheral` y `KempstonMousePeripheral` hacían cada uno por su cuenta.
- **`DeviceFrame<P extends PluggablePeripheral>`** extiende `AttachedFrame`: sabe encontrar `P` en
  la máquina a la que está clipeada, enchufarlo y desenchufarlo, y desenchufarlo al cerrarse. La
  subclase sólo dice qué mostrar y qué hacer cuando cambia de máquina (`plugged(P)`, con null al
  quedar suelta).
- **`EmulatorWindow extends MachineWindow`** con `Speccy machine()`: la ventana de una máquina dice
  qué máquina muestra, y el `Function<JInternalFrame, Speccy> machineOf` que hoy se pasa a cada
  ventana desaparece. Es la misma inversión que `MachineWindow.picture()`.
- **`Equipment`**: lo que el escritorio lista en su menú — un nombre y cómo abrir la ventana. Cada
  submódulo registra el suyo en `META-INF/services`; el menú "Equipment" se arma con lo que hay en
  el classpath, y `showPrinter()`/`showMouse()` se vuelven un `show(Equipment)`: una ventana por
  máquina, reusar la que está abierta, clipear a la máquina de adelante.
- **`MediaSlot`**: la ranura — insertar, expulsar, y el nombre de lo que hay adentro — que usan el
  cartucho, cada disquetera, cada microdrive y la tarjeta SD. Es un widget de `kit` porque su
  forma es la misma y lo que cambia es lo que se inserta. Crece (LED, proteger, nuevo, guardar)
  cuando la primera disquetera lo pida, no antes.

Para que las ventanas vivan en el paquete de su periférico y no en `speccy.peripherals.t`,
`AttachedFrame.setCompact` pasa a `protected` (hoy es package-private y la impresora lo llama por
estar en el mismo paquete).

### 3. Lo que le falta al núcleo, y que mejora el modelo por sí mismo

Ninguna de estas cosas es "para un periférico": son cosas que un Z80 y un Spectrum tienen y acá
faltaban. Se hacen antes del periférico que las necesita, cada una con su test, y se commitean solas.

| pieza | quién la necesita | qué es |
|---|---|---|
| **NMI de verdad** en `OOZ80.execute()` | Multiface, +D, DISCiPLE, Beta, Spectranet | hoy `state.activeNMI` se limpia y no pasa nada; y `Z80.z80_nmi` imprime "Z80 NMI". Un NMI es: sacar de HALT, IFF1=0, push PC, PC=0x0066, 11 t-states. Va en el modelo OOP del emulador y el núcleo generado lo hereda porque `execute()` es de `OOZ80`. El módulo de la máquina expone `Cpu.nmi()`. |
| **Trampas de PC** (`PcTraps`, un aspecto en `speccy/modules/z80`) | IF1, +D, DISCiPLE, Opus, Didaktik, Beta, Multiface, uSource, uSpeech, DivIDE, DivMMC, Spectranet | Fuse pagina ROMs cuando el PC pasa por direcciones fijas (0x0008, 0x0066, 0x1708…), antes o después del fetch. Acá no hay ningún hook de ese tipo. Es un aspecto lateral, como `PhaseProcessor`: una tabla de 64K bits, una consulta por instrucción, sin tocar las instrucciones. `Z80.step()` = `traps.before(pc)` → `execute()` → `traps.after(pc)`, y lo usan los dos loops (el de `doOpcodes` y el de RZX, que hoy llama a `ooz80` directo). "Después del fetch" en Fuse es después del byte de opcode; acá es después de la instrucción, y se verifica ROM por ROM que la instrucción en cada dirección de trampa no lee memoria baja (`xxd` sobre la ROM, en el test). |
| **`Module.unregister`** y **`RomcsDevice` por periférico** | todo lo que tiene ROM | el mecanismo existe (`ramInfo.romcs` → `memory.romcsMap` → `Module.romcs()` → `RomcsDevice.mapRom()`) y nadie lo implementa aún. Falta que un periférico pueda irse: `Module` registra y nunca borra. |
| **ROM desde un archivo** en `Utils.readAuxiliaryFile` | Multiface, IF1, Beta, Opus, Didaktik, DivIDE, uSpeech, uSource, Spectranet, TTX2000S | hoy sólo busca `roms/<nombre>` en el classpath. Fuse busca también en el disco. Las ROMs de estos periféricos no se pueden distribuir (`mf1.rom`, `if1.rom`, `trdos.rom` están en `~/detodo/spectrum/Roms`, no en el repo), así que la ventana las elige y el nombre queda en el setting que ya existe (`romMultiface1`, `romInterface1`, `romBeta128`…). |
| **`Dac`**, un `AudioSource` con un nivel de 8 bits | Covox, SpecDrum, uSpeech | es el `Beeper` sin la cinta: `synth.update(tstates, nivel × volumen)`. Un solo dueño del concepto "un DAC que va al mixer". |
| **La pila de disquete** en `core`: `Disk` (formatos), `Fdd`, `WdFdc`, `Crc` | +D, DISCiPLE, Beta, Opus, Didaktik — y el uPD765 del +3 | `Fdd` está portado pero `Disk` es un stub de 40 líneas, así que la disquetera del +3 tampoco lee nada. Van a `core` porque el Beta del Pentagon es la computadora, y porque el uPD765 ya vive ahí. `disk.c` son 3.114 líneas; se portan por formato, en el orden en que los periféricos los necesitan (MGT/IMG para +D, TRD/SCL para Beta, OPD para Opus, D40/D80 para Didaktik, DSK/EDSK para el +3; UDI, FDI, TD0 y SAD al final). |

### 4. La ventana de cada uno

La regla es la de la impresora: **clipear es enchufar**. La ventana compacta muestra los controles y
el estado (LEDs, qué cartucho, qué pista); expandida muestra lo que el periférico tiene para
mirar (el papel, el catálogo, la terminal). Iconos SVG en `/icons/` del jar del periférico, como
los de la impresora. Lo que se elige (ROM, imagen de disco) queda en el setting que Fuse tiene para
eso, así una máquina guardada vuelve con lo mismo enchufado.

## El orden

Primero lo que se ve al enchufarlo. Cada uno se termina entero — modelo, puertos, ventana, tests,
gate verde, commit — antes de empezar el siguiente.

| # | periférico | por qué en este lugar | qué se ve |
|---|---|---|---|
| 0 | infraestructura (padre, `kit`, mover los 4 que hay, menú por `ServiceLoader`) — HECHO | todos la usan | el menú Equipment se arma solo |
| 1 | **Interface 2** — HECHO | 314 líneas en Fuse, sin chip ni trampas: sólo ROMCS. Deja hecha la ranura (`MediaSlot`) | se inserta un cartucho y la máquina arranca en él, sin cinta |
| 2 | **NMI + trampas de PC**, luego **Multiface One / 128 / 3** | primera necesidad de las dos piezas del núcleo; el usuario tiene las tres ROMs | se aprieta el botón rojo y aparece el menú del Multiface sobre el juego |
| 3 | **pila de disquete + +D** | primer usuario de `Disk`/`WdFdc`; `plusd.rom` viene con Fuse | se inserta un disco MGT, `CAT 1` lista el catálogo, `LOAD d1"juego"` carga |
| 4 | **impresora paralela** | el +D y el +3 tienen el puerto; 8 puertos, sin chip | `LPRINT` en un +3 y sale texto en la ventana |
| 5 | **DISCiPLE** | +D con más puertos; `disciple.rom` viene con Fuse | igual que el +D, con joystick y red |
| 6 | **Beta 128** | TRD/SCL es el formato con más software; `trdos.rom` está en `~/detodo/spectrum/Roms`; de paso el Pentagon arranca en TR-DOS | `RUN "boot"` en un Pentagon y en un 128 con la interfaz |
| 7 | **Covox** y **SpecDrum** | dos DACs de 134 y 117 líneas sobre el mismo `Dac` | un vúmetro que se mueve con la música |
| 8 | **Fuller Box** | AY en otros puertos + joystick; reusa `AyPeripheral` y `Joystick.fullerRead`, que ya existe | música AY en un 48K, y el joystick del Fuller |
| 9 | **Opus Discovery** | misma pila; `success.opd` de Fuse como medio de prueba | catálogo de un disco Opus |
| 10 | **Didaktik 40/80** | misma pila, con un 8255 | idem |
| 11 | **Interface 1** | 1.410 líneas: microdrives, RS232, ZX Net; `if1.rom` está en `~/detodo/spectrum/Roms`, `success.mdr` en Fuse | `CAT 1` de un cartucho; la terminal RS232 muestra lo que el programa escribe |
| 12 | **DivIDE** | primera IDE; `FATware-0-12.rom` y `demfir.rom` están en `~/detodo/spectrum/Roms` | arranca el firmware desde un HDF |
| 13 | **Simple 8-bit IDE**, **ZXATASP**, **ZXCF** | sobre la misma `IdeChannel` | idem, con sus paginados |
| 14 | **DivMMC** y **ZXMMC** | tarjeta SD por SPI (`MmcCard`), que no existe en el árbol y se escribe desde el protocolo | igual, desde una imagen de tarjeta |
| 15 | **Currah uSpeech** | SP0256 (1.545 líneas); ROM no disponible acá | el Spectrum habla |
| 16 | **Currah uSource** | ROM no disponible; 305 líneas | el ensamblador en pantalla |
| 17 | **SpeccyBoot** | ENC28J60 por SPI; el TAP es de Linux y pide root; ROM viene con Fuse | el chip contesta; la red sólo si hay TAP |
| 18 | **Spectranet** | W5100 sobre sockets Java, flash AM29F010, paginado de 4K; firmware descargable | sockets reales desde BASIC |
| 19 | **TTX2000S** | teletexto por red; ROM no disponible | páginas de teletexto |

## Cada uno en detalle

Lo que sigue está sacado del C de Fuse (resumido a partir de `peripherals/*.c` y `z80/z80_ops.c`),
y es lo que cada migración tiene que cumplir. Las direcciones y máscaras están en hexadecimal.

### 1. Interface 2 (`if2.c`, 314 líneas) — `machine/devices/interface2` — HECHO

- Un cartucho de exactamente 16K que reemplaza a la ROM en 0x0000-0x3fff por `mapRomcsFull`. Sin
  puertos, sin trampas: insertar es `machine_reset(0)`, y el reset es el que lee el cartucho
  (`machineWasReset` llena las páginas y pone `ramInfo.romcs`); expulsar es otro reset.
- **Encaja** en 48, 128, +2 y los Timex; no en +2A/+3 (sin /ROMCS en el conector) ni en el
  Pentagon. Los dos joysticks Sinclair son teclas y ya estaban en `Joystick.press`.
- Snapshot: `interface2_active` y la imagen de 16K (queda para la fase de snapshots).
- **Ventana**: la ranura (`MediaSlot`: insertar, expulsar, el nombre del cartucho) y los dos
  zócalos de joystick, donde se enchufa el joystick del teclado (tecla 6-0 o 1-5); expandida,
  qué lee cada zócalo.
- **Test**: un cartucho de 7 bytes (`DI; LD A,2; OUT (0xFE),A; JR $`) arranca y la máquina queda
  girando en él; expulsarlo o desenchufar la interfaz devuelve la ROM; en qué máquinas encaja;
  clipear la ventana lo enchufa.

### 2. Multiface One / 128 / 3 (`multiface.c`, 815 líneas) — `machine/devices/multiface`

- **ROM** 8K en 0x0000 y **RAM** 8K en 0x2000 por modelo (RAM persistente, se borra en reset
  duro). ROMs `mf1.rom`, `MF128.ROM`, `MF3.rom` (8K cada una) en `~/detodo/spectrum/Roms`.
- **Puertos** (máscara 0x0072): One valor 0x0012 (`IN 0x9f` pagina si J2, `IN 0x1f` despagina);
  128 valor 0x0032 (`IN 0xbf` pagina, `IN 0x3f` despagina); 3 el mismo 0x0032 con A7 invertido
  (`IN 0x3f` pagina, `IN 0xbf` despagina). En 128/3, un `OUT` al puerto estando paginado pone
  `J2 = A7` (bloqueo por software) y rearma el botón. El 3 además espía escrituras a
  0x1ffd/0x3ffd/0x5ffd/0x7ffd (máscara 0x90ff, valor 0x10fd) y devuelve `xfdd[(port & 0x6000) >> 13] | 0xf0`
  en sus lecturas; el 128 devuelve bit 7 = bit 3 del último byte de 0x7ffd (`ramInfo.lastByte`).
- **Sigilo** (One): `multiface1Stealth` → J2 = 0 al reset: la lectura de 0x9f no pagina, la de
  0x1f sí despagina.
- **Botón rojo**: elige el primer modelo con `IC8b_Q == 1` (el One sólo si J2), lo baja, marca
  `activated` y pide un **NMI**. Con el PC en 0x0066 y `activated`, `setic8` pagina ese modelo
  (trampa antes del fetch). El menú de Fuse también ofrece un NMI pelado.
- Encaja: One en 48; 128 en 48/128/+2; 3 en +2A/+3.
- Snapshot (SZX `MFCE`): modelo, paginado, J2, botón, la RAM, `xfdd[0]` y `[3]`.
- **Ventana**: el botón rojo (grande, es lo que es un Multiface), qué modelo (según la máquina),
  la ROM a elegir, el interruptor de sigilo, un LED "paginado"; expandida, la RAM de 8K en hexa.
- **Test**: con la ROM del usuario si está: botón → NMI → el menú del Multiface escribe en la
  pantalla; sin ella: una ROM sintética en 0x0066 que marca la RAM y `RETN`, y los puertos de
  paginado de cada modelo.

### 3. +D (`plusd.c`, 588 líneas) — `machine/devices/plusd`

- **ROM** 8K en 0x0000-0x1fff; **RAM** 8K en 0x2000-0x3fff (persistente, se borra sólo en reset duro).
  Se pagina leyendo el puerto 0xe7 y se despagina escribiéndolo. Trampas de PC, antes del fetch:
  `PC ∈ {0x0008, 0x003a, 0x0066, 0x028e}` → pagina. No hay despaginado por PC.
- **Puertos** (máscara 0x00ff): 0xe3 estado/comando del FDC, 0xeb pista, 0xf3 sector, 0xfb datos;
  0xef control (escritura): bits 0-1 unidad (sólo `2` elige la segunda), bit 7 lado, bit 6 strobe de
  la impresora; 0xe7 parche (ver paginado); 0xf7 datos de la impresora (escribe al puerto paralelo;
  lee 0xff sin impresora, 0x7f con ella: "nunca ocupada").
- **FDC** WD1770 (`WD_FLAG_NONE`), MFM; **dos unidades** Shugart con geometría automática, la
  primera seleccionada al reset. Sin NMI desde el FDC.
- **Medios**: MGT/IMG (y todo lo que `Disk` sepa leer). Operaciones: insertar, expulsar, nueva,
  guardar, dar vuelta, proteger. Botón NMI: el del +D (snapshot/imprimir) es un NMI común, que la
  ROM atiende porque 0x0066 es una trampa.
- **Snapshot** (SZX `PLSD`+`PDSK`): activo, paginado, dirección, registros del FDC, control, ROM
  custom, RAM, cantidad de unidades. `SnapshotSZX` en `machine/spectrum` ya conoce estos bloques.
- **Ventana**: la bahía de dos disqueteras: por unidad una `MediaSlot` (LED de motor, nombre del
  disco, insertar/expulsar/nueva/guardar/dar vuelta/proteger), la pista y el sector del FDC, el
  botón NMI, y el conector de impresora (depende de `parallel-printer`).
- **Test**: un disco MGT en blanco formateado en Java (`Disk.preformat` + catálogo vacío de G+DOS),
  `CAT 1` en un 48K con la ROM `plusd.rom` de Fuse, y leer del RAM de pantalla el catálogo.

### 4. Impresora paralela (+2A/+3; `printer.c`, la mitad no portada) — `machine/devices/parallel-printer`

- **Puerto** máscara 0xf002, valor 0x0000: escritura guarda el byte; lectura 0xfe con impresora
  (bit 0 = BUSY en bajo, "nunca ocupada"), 0xff sin ella. El **strobe** no es un puerto: lo
  escribe la máquina (+3: bit 4 de 0x1ffd) o la interfaz (+D/DISCiPLE: bit 6 del control; Opus:
  pulso 0→1→0). Fuse empareja dos flancos a menos de 10.000 t-states porque la ROM del +3 lo
  escribe al revés y otros programas al derecho.
- **Texto**: todo va a `printer_text_output_char` (archivo de texto en modo append), que también
  recibe el RS232 del AY (registro 14, UART por bits) y el OCR de la ZX Printer. Sin hook de
  reset ni de snapshot en Fuse.
- Acá: `ParallelPrinter` (el byte, el strobe emparejado, la salida) es el dueño del concepto; el
  +3 le da el strobe desde `SpecPlus3` (que ya escribe 0x1ffd) por un rol de máquina, y el +D y
  el DISCiPLE se lo dan como dependencia de módulo.
- **Ventana**: la impresora de matriz de puntos: el papel continuo con el texto que llega
  (monoespaciado, avance de línea), "cortar", "guardar como .txt"; el archivo de Fuse se
  mantiene como opción.
- **Test**: en un +3, `LPRINT "hola"` desde BASIC (tecleado por el `PhantomTypist`) y leer "hola"
  del papel.

### 5. DISCiPLE (`disciple.c`, 755 líneas) — `machine/devices/disciple`

- Un +D con más puertos y una diferencia: **ROM y RAM se pueden intercambiar** (`memswap`: normal
  ROM en 0x0000/RAM en 0x2000; intercambiado al revés), por el puerto 0x7b (leer → normal,
  escribir → intercambiado).
- Pagina leyendo 0xbb, despagina escribiéndolo. Trampas antes del fetch: `PC ∈ {0x0001, 0x0008,
  0x0066, 0x028e}` → pagina.
- **Puertos** (0x00ff): 0x1b estado/comando, 0x5b pista, 0x9b sector, 0xdb datos; 0x1f lectura
  "joystick": en Fuse sólo bit 6 = impresora ocupada (0xbf sin impresora, 0xff con) — el joystick
  real no está emulado; 0x1f escritura = control: bit 0 unidad (`1` → primera), bit 1 lado, bit 3
  banco de ROM (no implementado), bit 4 inhibir (sólo se guarda), bit 6 strobe; 0x3b red (stub
  vacío); 0xfb datos de impresora.
- **FDC** WD1770; dos unidades. Snapshot como el +D más `inhibit`. ROM `disciple.rom` viene con Fuse
  (8K; los dumps de 16K son GDOS en RAM y no sirven).
- **Ventana**: la misma bahía del +D (se reusa la vista; cambia el modelo), con el conector de
  joystick y el de red dibujados y marcados como no emulados, que es lo que Fuse hace.

### 6. Beta 128 (`beta.c`, 676 líneas) — `machine/devices/beta128`, y el Pentagon en `core`

- **ROM** 16K completa en 0x0000-0x3fff (`trdos.rom`, no viene con Fuse; está en
  `~/detodo/spectrum/Roms`). Sin RAM propia.
- **Paginado sólo por PC**, antes del fetch, y sólo cuando la ROM del 48K está visible
  (`NOT_128_TYPE_OR_IS_48_TYPE` = máquina sin 128 o `current_rom == 1`): pagina si
  `(PC & 0xff00) == 0x3d00` (en un 48K puro la máscara es 0xfe00/0x3c00); despagina si está
  activo y `PC >= 0x4000`. Con `beta128_48boot` en un 48K, pagina al reset.
- **Puertos** (0x00ff): 0x1f estado/comando, 0x3f pista, 0x5f sector, 0x7f datos; 0xff sistema:
  escritura bits 0-1 unidad (4), bit 3 HLT, bit 4 lado (invertido), bit 5 densidad (0=FM, 1=MFM);
  lectura bit 7 INTRQ, bit 6 DATARQ.
- **FDC** FD1793 con `WD_FLAG_BETA128` (HLD atado a READY y al motor). **Cuatro unidades**.
- **Medios**: TRD y SCL (`trdos.c`: FAT de 8 sectores intercalados, sector de sistema, `boot`).
  `Disk` sintetiza las pistas con `GAP_TRDOS`.
- **El Pentagon**: hoy `Beta.java` en `speccy/machine` es un stub con métodos vacíos y arranca con
  las ROMs de 128. El chip es el mismo; el `fitsOn` es `TRDOS_DISK`, y el ROM es la tercera ROM del
  Pentagon (`pentagon.rom`, 32K, en `~/detodo/spectrum/Roms`). `Beta.java` desaparece.
- **Ventana**: la bahía, con cuatro unidades, y el botón "autoload" de Fuse (reset, `PC=0`, ROM 1,
  paginar) que arranca TR-DOS en una 128.
- **Test**: un TRD en blanco formateado en Java, `LIST` del catálogo desde TR-DOS en una 128 con la
  ROM del usuario si está, y sin ella el test de paginado y de puertos solamente.

### 7. Covox (`covox.c`, 134) y SpecDrum (`specdrum.c`, 117) — `machine/devices/covox`, `machine/devices/specdrum`

- **Covox**: dos puertos de escritura, 0xfb y 0xdd (máscara 0xff), un solo flag: los dos a la
  vez. Nivel `val × 128` en el instante `tstates`, volumen `volumeCovox`. Encaja en todo lo que
  no sea +2A/+3 según la tabla base… en Fuse está en `base_peripherals` — es decir, todas.
  Ojo: en un +3 el puerto 0xfb choca con nada, pero en un 48 con ZX Printer, 0xfb es la impresora
  (decodificación 0x0004): Fuse los deja convivir (ambos escuchan).
- **SpecDrum**: un puerto, 0xdf, escritura; nivel `(val − 128) × 128`; volumen `volumeSpecdrum`.
  Sólo 48/128 (no +3).
- Los dos guardan el último byte (`covox_dac`, `specdrum_dac`) para el snapshot; reset lo pone
  en 0; `hard_reset = 1`.
- Acá: `Dac` (un `AudioSource` sobre `BlipSynth` con ecualización plana, como los tres synths de
  Fuse) en `core`; cada periférico lo instancia con su fórmula y su volumen.
- **Ventana**: un vúmetro (el nivel del último byte, con caída) y la perilla de volumen; para el
  SpecDrum, la caja con sus ocho pads que se iluminan por nivel. Expandida, un osciloscopio del
  frame.
- **Test**: escribir una onda al puerto durante un frame y medir en el mixer (como `MelodikTest`).

### 8. Fuller Box (`fuller.c`, 101 líneas) — `machine/devices/fuller`

- AY en 0x3f (registro, lectura/escritura) y 0x5f (datos, escritura), máscara 0xff; joystick en
  0x7f (lectura): activo en bajo, `{izq 0x04, der 0x08, arriba 0x01, abajo 0x02, fuego 0x80}` —
  `Joystick.fullerRead` ya existe. Sólo 48. Snapshot: el estado del AY.
- Acá: `AyPeripheral` ya sabe ser un chip en otros puertos (el Melodik lo demuestra); el Fuller
  es ese chip en 0x3f/0x5f más el puerto del joystick.
- **Ventana**: la caja con el conector de joystick (dónde va el joystick del teclado, como en la
  Interface 2) y tres barras de nivel, una por canal.

### 9. Opus Discovery (`opus.c`, 675 líneas) — `machine/devices/opus`

- **ROM** 8K en 0x0000-0x1fff; **RAM** 2K en 0x2000-0x27ff. **No tiene puertos de E/S**: el FDC y
  la PIA 6821 están mapeados en memoria — 0x2800-0x2fff el WD1770 (`addr & 3`: estado, pista,
  sector, datos), 0x3000-0x37ff la 6821 (`addr & 3`). Eso pide al núcleo una cosa que hoy no
  tiene: **un dispositivo mapeado en memoria** que `Memory` consulte en un rango cuando está
  activo (en Fuse es un `if (opus_active)` dentro de `readbyte`/`writebyte`).
- Trampas **después del fetch**: activo y `PC == 0x1748` → despagina; inactivo y `PC ∈ {0x0008,
  0x0048, 0x1708}` → pagina.
- **6821**: registro 0 (con `CRA & 4`) bit 1 unidad, bit 4 lado, bit 6 "impresora nunca ocupada";
  registro 2 datos hacia la impresora con strobe automático 0→1→0; CRA se lee con bit 6 puesto.
- **FDC** WD1770 con `WD_FLAG_DRQ`; **cada DRQ dispara un NMI**. Dos unidades.
- **Medios**: OPD/OPU (`success.opd` de Fuse como prueba). Snapshot: registros de la PIA además.
- **Ventana**: la bahía de dos unidades, más el conector de impresora.

### 10. Didaktik 40/80 (`didaktik.c`, 644 líneas) — `machine/devices/didaktik`

- **ROM** 14K en tres trozos (8K en 0x0000, 4K en 0x2000, 2K en 0x3000); **RAM** 2K en 0x3800.
  Trampas antes del fetch: `PC ∈ {0x0000, 0x0008}` pagina, `PC == 0x1700` despagina. Botón SNAP:
  con el PC en 0x0066 y la interfaz despaginada, el opcode recién leído se reemplaza por `RST 0`
  — es el único caso en que una trampa cambia el opcode, y pide que el aspecto pueda decir "ejecutá
  esto en vez de lo que hay".
- **Puertos**: 0x81 estado/comando, 0x83 pista, 0x85 sector, 0x87 datos (máscara 0xff); AUX
  (máscara 0xf9, valor 0x89, escritura): bit 0 unidad 0, bit 1 unidad 1, bit 2 motor 0, bit 3
  motor 1, bit 6 DATARQ habilita NMI, bit 7 INTRQ habilita NMI; 8255 (máscara 0x80, valor 0x00):
  stub, lee 0xff.
- **FDC** WD2797 con `WD_FLAG_DRQ | WD_FLAG_RDY` y READY siempre en alto (Fuse lo deja así). NMI
  cuando INTRQ/DATARQ están habilitados en AUX. Dos unidades. ROM `MDOS3.bin` (8K) está en
  `~/detodo/spectrum/Roms`; Fuse espera `didaktik80.rom` de 14K — verificar cuál sirve.
- **Medios**: D40/D80. **Ventana**: la bahía, con el botón SNAP.

### La pila de disquete que estos cinco comparten (en `core`, junto al uPD765)

| pieza | Fuse | qué hay hoy | qué hay que hacer |
|---|---|---|---|
| `Crc` | `crc.c`, 87 | nada | CRC-16 CCITT por byte (para marcas y datos) y CRC-32 (UDI) |
| `Disk` | `disk.c`, 3.114 | stub de 40 líneas | la pista como flujo de bytes con tres planos de bits (marca de reloj, FM/MFM, débil); `trackgen` con la tabla de gaps; formatos por etapas: MGT/IMG → TRD/SCL (+`trdos`) → OPD → D40/D80 → DSK/EDSK (para el +3) → UDI, FDI, SAD, TD0 (sólo sin compresión), LOG |
| `Fdd` | `fdd.c`, 476 | 447 líneas portadas, con `readData`/`writeData` vacíos porque `Disk` no existe | terminar contra `Disk`: índice cada 200 ms (10 ms en alto), motor 400 ms para READY, cabezal, paso, jitter de posición, bits débiles al leer |
| `WdFdc` | `wd_fdc.c`, 1.251 | nada | los cinco chips (WD1770/1772/1773/FD1793/WD2797), tipos I/II/III/IV, `read_id`/`read_datamark` sobre el flujo de la pista, tres eventos (`fdc`, `motor_off`, `timeout`), flags BETA128/DRQ/RDY/NOHLT |

Los eventos van por `EventManager.eventRegister`; el ticket de un periférico se saca al
activarse y se limpia al desactivarse (`eventRemoveType`). El uPD765 del +3 (`UPDFdc`, 219 de
1.537 líneas) queda para después: no es un periférico, pero con `Disk` y `Fdd` reales deja de
estar bloqueado.

### 11. Interface 1 (`if1.c`, 1.410 líneas) — `machine/devices/interface1`

- **ROM** 8K, mapeada dos veces (0x0000 y 0x2000). Trampas: antes del fetch `PC ∈ {0x0008,
  0x1708}` pagina; después del fetch `PC == 0x0700` despagina. ROM `if1.rom`/`if1v2.rom` en
  `~/detodo/spectrum/Roms` (Fuse espera `if1-2.rom`).
- **Puertos**, decodificados sólo por los bits 3-4 (máscara 0x0018): 0xe7 datos del microdrive,
  0xef control/estado, 0xf7 RS232/red.
  - 0xef lectura: bit 0 protegido, bits 1-2 SYN/GAP (15 lecturas en alto, 15 en bajo por bloque),
    bit 3 DTR, bit 4 BUSY (siempre activo en Fuse). Escritura: bit 0 DTA (flanco de subida
    reinicia los contadores serie), bit 1 CLK (flanco de bajada rota el registro de 8 motores con
    `!DTA`), bit 4 CTS, bit 5 WAIT.
  - 0xe7: lectura AND de los drives con motor, un byte por acceso desde `head_pos`; escritura
    clasificada por posición (preámbulo 10×0x00 + 2×0xff, luego 15 o 528 bytes).
  - 0xf7: bit 7 TX (recepción RS232), bit 0 NET/RX; dos máquinas de estado (serializador de
    recepción y deserializador de transmisión por bits, y la ZX Net cruda o interpretada).
- **Microdrive**: 8 unidades; cartucho de N sectores de 543 bytes (15 cabecera + 15 cabecera de
  registro + 512 datos + 1 checksum), `pream[512]`, cabezal que se realinea a un borde de bloque
  en cada acceso a 0xef/0xf7. Formato **MDR** (`Microdrive`: sectores, protección; longitud desde el
  archivo; cartucho nuevo de `mdrLen` sectores o aleatorio ~171-254, relleno con 0xff).
- **RS232**: archivos/FIFOs para RX y TX, con escapes `0x00 0x00..0x03` para DTR/CTS y `0x00 '*'`
  para un cero literal; sin handshake DTR=1. **ZX Net**: un archivo compartido, modo crudo (un
  byte de "estado del cable" reescrito en offset 0) o interpretado (bytes con número de estación).
- **Sin eventos**: todo avanza con los IN/OUT.
- **Snapshot** (SZX `IF1`): activo, paginado, ROM custom; los cartuchos no se guardan.
- **Ventana**: la Interface 1 con sus 8 ranuras (`MediaSlot` chicas: cartucho, LED de motor,
  proteger, nuevo, guardar), y expandida una **terminal** para el RS232 (lo que el Spectrum
  transmite aparece como texto; lo que se escribe se le manda) y el conector de red. Acá se
  aparta de Fuse a propósito: Fuse sólo ofrece archivos; la terminal es la misma funcionalidad
  con la cara de este escritorio, y los archivos siguen estando.
- **Test**: `success.mdr` de Fuse: `CAT 1` desde la ROM del usuario si está; sin ella, el registro
  de motores, el preámbulo y la lectura de un sector contra el archivo.

### 12-13. DivIDE, Simple 8-bit IDE, ZXATASP, ZXCF (`ide/`, 2.912 líneas con los MMC)

Lo que comparten, y no está en el árbol porque es de libspectrum: **`IdeChannel`** (una interfaz
ATA con maestro y esclavo sobre archivos HDF: cabecera HDF, sectores de 512, LBA/CHS, IDENTIFY,
READ/WRITE SECTORS, bus de 8 o 16 bits) — se escribe desde la especificación de HDF y ATA, con
los tests de `*_unittest()` de Fuse como oráculo del lado de los puertos.

- **DivIDE** (`divide.c` 371 + `divxxx.c` 335) — `machine/devices/divide`. EPROM 8K en 0x0000,
  RAM 4×8K en 0x2000 (`control & 3`). Puertos: 0x00e3 (máscara 0xff, escritura) control: bit 7
  CONMEM, bit 6 MAPRAM (pegajoso: un `OUT` sólo puede ponerlo, lo saca el reset duro), bits 0-1
  banco; IDE en 0xa3/0xa7/0xab/0xaf/0xb3/0xb7/0xbb/0xbf (máscara 0x00e3, valor 0x00a3). Automap:
  antes del fetch `(PC & 0xff00) == 0x3d00` → activo; después del fetch `(PC & 0xfff8) == 0x1ff8`
  → inactivo, `PC ∈ {0x0000, 0x0008, 0x0038, 0x0066, 0x04c6, 0x0562}` → activo. Mapa: CONMEM →
  EPROM (escribible si no hay wp) + RAM[banco]; MAPRAM → RAM[3] fija abajo (sólo lectura) + RAM[banco];
  ninguno → EPROM sólo lectura + RAM[banco]; y sin CONMEM el automap sólo manda si hay wp o MAPRAM.
  `divxxx_unittest` da los vectores. Firmware: `FATware-0-12.rom`, `demfir.rom` en
  `~/detodo/spectrum/Roms`. Ventana: la EPROM (elegir, proteger), dos `MediaSlot` (maestro y
  esclavo, archivos HDF: insertar, guardar, expulsar), el estado CONMEM/MAPRAM/banco.
- **Simple 8-bit IDE** (187) — `machine/devices/simpleide`. Un solo puerto: máscara 0x0010, valor
  0x0000; registro = bits 8, 12 y 13 del puerto. Sin memoria. Ventana: dos `MediaSlot`.
- **ZXATASP** (604) — `machine/devices/zxatasp`. 8255 en 0x009f/0x019f/0x029f/0x039f (máscara
  0x039f); puerto C: bits 0-2 registro IDE, 3 WR, 4 RD, 5 primario, 6 latch de RAM, 7 secundario
  o RAM off; 32 bancos × 16K de RAM sobre 0x0000-0x3fff, sólo los impares protegidos con wp;
  `upload` mapea escritura sin lectura. Vectores en `zxatasp_unittest`. Ventana: dos `MediaSlot`,
  el banco y los flags.
- **ZXCF** (377) — `machine/devices/zxcf`. `memctl` en 0x10b4 (máscara 0x10f4): bit 7 memoria
  apagada, bit 6 escribible, bits 0-5 banco de 64×16K; IDE en 0x00b4 (registro en bits 8-10).
  Sólo maestro. Ventana: una `MediaSlot`, banco, protección.

### 14. DivMMC y ZXMMC — `machine/devices/divmmc`, `machine/devices/zxmmc`

**`MmcCard`**: una tarjeta SD/MMC por SPI sobre una imagen de archivo (CMD0, CMD1, CMD8, CMD16,
CMD17, CMD24, ACMD41, CMD58, respuestas R1/R3/R7, tokens de datos, CRC ignorado), que tampoco
está en el árbol y se escribe desde el protocolo.

- **DivMMC** (424): el mismo motor `divxxx` que DivIDE con 16×8K de RAM (`control & 0x0f`),
  mismas trampas; puertos (máscara 0xff): 0xe3 control, 0xe7 selección de tarjeta (`data & 3`:
  `2` → tarjeta, `1`/otro → ninguna), 0xeb datos SPI. Ventana: EPROM + una `MediaSlot` con la
  imagen de la tarjeta.
- **ZXMMC** (243): sin memoria; 0x1f selección, 0x3f datos SPI. Ventana: una `MediaSlot`.

### 15. Currah uSpeech (`uspeech.c` 444 + `sp0256.c` 1.545) — `machine/devices/uspeech`

- **ROM** 2K (`uspeech.rom`, no disponible acá) espejada en 0x0000 y 0x0800; el resto del bloque
  de 16K es una página de 0xff. **Conmuta** con cualquier acceso a memoria a 0x0038 (¡el vector
  de IM1!) — es una trampa de dirección en `readbyte`/`writebyte`, no de PC — y también por el
  puerto exacto 0x0038. Escribir en 0x1000 (memoria, `& 0xf000`) o en el puerto 0x1000 manda un
  alófono (`b & 0x3f`); leerlo da BUSY; 0x3000/0x3001 cambian la entonación (cristal 3.05 / 3.26 MHz).
- **SP0256-AL2**: microsecuenciador + filtro LPC de 12 polos (`qtbl[128]`, `datafmt[189]`,
  `df_idx[128]`), ROM de 2K duplicada a 4K como página 1; produce muestras a su propio ritmo
  (~350 t-states) y las manda al mixer con marca de tiempo propia (`sound_sp0256_write`). Corre
  cada frame aunque nadie escriba.
- Pide al núcleo la trampa de **acceso a memoria** (la misma que Opus necesita para 0x2800-0x37ff).
- **Ventana**: la boca (el nivel de salida), el alófono en curso, la entonación; expandida, la
  cola de alófonos con sus nombres.

### 16. Currah uSource (`usource.c`, 305 líneas) — `machine/devices/usource`

- **ROM** 8K espejada en 0x0000 y 0x2000. **Conmuta** con lectura o escritura del puerto exacto
  0x2bae (devuelve 0xff). Sin RAM, sin trampas de PC, sin NMI. Arranca despaginado. Snapshot:
  activo, paginado, ROM custom. ROM no disponible acá.
- La plantilla mínima de "ROM que se conmuta por puerto".
- **Ventana**: la ROM a elegir y un LED de paginado.

### 17. SpeccyBoot (`speccyboot.c` 295 + `enc28j60.c` 330) — `machine/devices/speccyboot`

- ROM 8K en 0x0000 (`speccyboot-1.4.rom` viene con Fuse), paginada por el bit 5 del puerto
  (bajada pagina, subida despagina). Sin trampas de PC.
- **Un puerto** (máscara 0x00e0, valor 0x0080; canónico 0x9f): bit 0 SCK (flanco de subida
  desplaza un bit SPI), bit 3 /CS del ENC28J60, bit 5 /ROMCS, bit 6 /RST, bit 7 MOSI; lectura
  bit 0 MISO y el resto en 1.
- **ENC28J60**: comandos RCR/RBM/WCR/WBM/BFS/BFC/SRC, 4 bancos × 32 registros, 8K de buffer,
  ERDPT con vuelta en ERXND; TXRTS manda la trama; PKTDEC decrementa EPKTCNT; RX se sondea en
  cada OUT. Trama ↔ host por **TAP** (Linux, `/dev/net/tun`, `TUNSETIFF`; pide permisos). Acá:
  el chip completo, y el TAP por JNA cuando el sistema lo da; si no, la interfaz sin cable.
- Sin snapshot ni menú en Fuse. Ventana: LEDs de link/RX/TX, el nombre del TAP, contador de
  tramas.

### 18. Spectranet (`spectranet.c` 603 + `w5100.c` 418 + `w5100_socket.c` 792 + `am29f010.c` 161)
— `machine/devices/spectranet`

- **Memoria**: flash AM29F010 de 128K (32 páginas de 4K), RAM de 128K (32 páginas, 0xc0-0xdf),
  ventana del W5100 (páginas 0x40-0x47). Los 16K bajos son cuatro ranuras de 4K: 0x0000 fija en
  flash 0, 0x1000 "página A" (puerto 0x003b), 0x2000 "página B" (puerto 0x013b), 0x3000 fija en
  RAM 0xc0. Todo el mapa entra por `mapRomcsFull`. La flash se programa sólo por la página B
  (secuencia 0x555/0x2aa: program, chip erase, sector erase, autoselect).
- **Puertos** (máscara 0xffff): 0x003b, 0x013b paginado; 0x023b lectura versión CPLD (3),
  escritura trampa programable (dos bytes alternados); 0x033b control: bit 0 pagina/despagina
  (sólo si se paginó por E/S), bit 3 activa la trampa; lectura bits 0-2 último byte de la ULA, bit
  4 la ROM 1 de una 128.
- **Trampas**: antes del fetch `PC == 0x0008 || (PC & 0xfff8) == 0x3ff8` pagina; `PC == trampa`
  → NMI; después del fetch `PC == 0x007c` despagina. NMI: flip-flop, que `RETN` limpia (pide un
  hook de RETN en el núcleo). En el NMI del Z80 de Fuse, si hay Spectranet se pagina.
- **W5100**: registros comunes (MR, GWR, SUBR, SHAR, SIPR, IMR, RMSR/TMSR fijos en 0x55), 4
  sockets TCP/UDP con buffers de 2K, comandos OPEN/LISTEN/CONNECT/DISCON/CLOSE/SEND/RECV, un hilo
  de E/S con `select` y un self-pipe. Acá: `java.nio` con un selector en un hilo, mismos estados
  (0x13, 0x14, 0x17, 0x1c, 0x22).
- **Snapshot**: todo, incluidas las dos imágenes de 128K. Sin menú en Fuse.
- Firmware: no está en el árbol ni en `~/detodo/spectrum/Roms`; es software libre y se elige desde
  la ventana. Ventana: el botón NMI, las cuatro conexiones con su estado e IP, la página A/B, y
  expandida un volcado del W5100.

### 19. TTX2000S (`ttx2000s.c`, 544 líneas) — `machine/devices/ttx2000s`

- **ROM** 8K en 0x0000; **RAM** 1K real (2K asignada), espejada cuatro veces en 0x2000-0x3fff;
  cada acceso a la RAM latchea `line_counter = (addr >> 6) & 0xf`. **Puerto** máscara 0x0080,
  valor 0x0000 (A7 = 0): bits 0-1 canal (1-4, direcciones y puertos en settings), bit 3 = 1
  despagina. Arranca **paginado** al reset (única de la lista).
- **Red**: TCP sin handshake al servidor del canal; se reciben campos de 672 bytes (16 líneas de
  42) y se vuelcan a la RAM fila por fila (paso 0x40, byte 0x27 delante de una línea no vacía);
  un evento cada 1/50 s dispara un **NMI** cuando llegó un campo y está paginado. Sin snapshot
  real. ROM no disponible acá; el servidor tampoco (hay uno público en el proyecto original).
- **Ventana**: el selector de canal, LED de conexión, y expandida la página de teletexto
  dibujada desde la RAM.

## Lo que el núcleo gana de paso, y que no era de este plan

- El uPD765 del +3 deja de estar bloqueado cuando `Disk` y `Fdd` sean reales (paso 3).
- `Peripherals.clear()` no avisaba a los periféricos que apagaba: el Melodik quedaba con el chip
  en el mixer, la ULA con dos parlantes. Ahora avisa (hecho en el paso 1).
- Un `Module` no se podía ir (hecho en el paso 1); un banco de ROM lo llena `Memory` y no la
  máquina (hecho en el paso 1).
- `EventManager.eventNextEvent` arranca en −1 y "menor que −1" nunca es verdad: el primer
  `doOpcodes` de cada máquina no corre hasta que `eventDoEvents` lo corrige. Funciona de casualidad.

## Lo que queda fuera, y por qué

- **Snapshots por periférico** (los bloques SZX de cada uno): `machine/spectrum` ya lee algunos
  (`IF1`, `MFCE`, `PLSD`); escribirlos y conectarlos con estos periféricos es una fase aparte, al
  final, cuando los periféricos existan.
- **El diálogo de Settings** tiene una solapa "Peripherals" con checkboxes que llaman a un
  `setPeripheralOption` que imprime y no hace nada. En este escritorio enchufar es clipear la
  ventana; esa solapa se va cuando cada periférico tenga la suya.
- **Timex** (SCLD, AY Timex, dock): son máquinas que no hay.

## Cómo se prueba cada uno

Cada módulo tiene sus tests headless (el modelo y los puertos sobre un `Speccy` entero, como
`MelodikTest`) y, cuando hay un medio o una ROM de prueba libre, uno que arranca el programa de
verdad y lee el resultado de la pantalla o del medio. Lo que necesita una ROM que no se puede
distribuir se prueba con una ROM sintética de pocos bytes que ejercita el mecanismo (paginar en
0x0066, contestar un puerto) y, si la ROM real está en `~/detodo/spectrum/Roms`, el test la usa
y si no la saltea diciendo por qué.

El gate: `mvn -o -q -f machine/devices/pom.xml install` (cada módulo), `mvn -o -q -pl machine/app
test`, y `mvn -o -q -pl machine/core test` cuando se toca el núcleo. Los dos tests de la impresora
que abren un display fallan sin X y no cuentan.
