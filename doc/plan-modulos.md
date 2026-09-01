# Partir machine/core en módulos

Plan para que un periférico esté encapsulado de verdad: que el compilador impida que el emulador
lo conozca, en vez de que sea una convención que se respeta hasta que alguien no la respeta.

Medido sobre `machine/core` el 1 de septiembre de 2026, con el gate en 320 tests verdes.

## El principio

Un periférico real conocía el hardware: quien diseñó la ZX Printer tenía los esquemáticos de la
Spectrum, sabía del bus y del timing. Lo que no podía hacer era que la Spectrum supiera de la
impresora.

**La pregunta no es cuánto conoce el periférico, sino quién conoce a quién.** Un periférico que
usa la ULA, el mixer y el reloj está bien. El emulador que nombra a la impresora está mal. La
dirección de dependencia entre módulos hace cumplir exactamente eso, con el compilador y sin
tests.

De ahí sale el resto del plan: **no** hay que angostar lo que un periférico ve (esa fue una
propuesta anterior, descartada), hay que impedir que el emulador vea al periférico.

## Lo que hay hoy

`machine/core` tiene 263 archivos java, de los cuales 51 usan Swing. Es un solo jar con el
emulador, los dispositivos, el escritorio, el launcher y la pantalla adentro.

| dónde | archivos | qué es |
|---|---|---|
| `speccy/peripherals/t` | 32 | el escritorio: ventanas, menús, browser de juegos, docking |
| `speccy/screen` + `SpeccyScreen`, `TvScreen`, `SpectrumPanel`, `ZXScreenComponent` | 13 | la pantalla dibujada |
| `speccy/devices/*` | 33 | los once dispositivos y sus módulos de Guice |
| `speccy/machine`, `modules`, `peripherals`, `ports`, `sound`, `Memory`, `Z80` | ~150 | la emulación |
| `api/`, `speccy/bridge`, `rzx`, `config`, `pokes` | ~35 | servicios y formatos |

Lo bueno: **la emulación casi no conoce al escritorio.** Sólo hay cuatro aristas hacia adentro de
`peripherals/t`, y tres son del launcher, que es parte de la aplicación.

## Los tres nudos

Hay que desatarlos antes de mover nada, porque son ciclos que ningún orden de módulos resuelve.

1. **`Z80.createScreen(...)`** abre un `JFrame` — el camino "noTest" del arranque. Un emulador que
   construye una ventana no puede vivir en un módulo sin Swing. Va a la aplicación, que es quien
   sabe dónde se muestra una máquina.

2. **`Tape.TapeTableModel`** es un `AbstractTableModel` de Swing adentro de la casetera. El modelo
   de la tabla es de la ventana que la muestra, no de la cinta. (Zona de la sesión paralela:
   coordinar antes de tocarlo.)

3. **`api/ZxInfoApiHandler` → `GameBrowserInternalFrame`**: un cliente HTTP que importa una
   ventana. Es una inversión: la ventana escucha al handler, no al revés.

## El destino

```
machine/base       emulación, contratos de dispositivo, roles de máquina, el bus,
                   y el hardware propio de cada máquina (pagers, FDC, ULA, el AY de una 128).
                   Sin Swing: corre headless, que es lo que quieren los tests y el core libretro.

machine/ui         infraestructura de ventanas: AttachedFrame, los helpers de iconos,
                   los componentes de pantalla.  → depende de base

machine/devices    los periféricos opcionales: impresora, mouse, Melodik, Kempston…
                   cada uno con su modelo y su vista.  → depende de base y de ui

machine/app        el escritorio, los menús, el launcher, main().
                   → depende de base + ui + devices, y los descubre por ServiceLoader
```

### Por qué existe `machine/ui`

Es la pieza que permite que un periférico traiga su propia ventana. Si `AttachedFrame` se queda en
la aplicación, la ventana de la impresora tiene que vivir ahí, y entonces el periférico no es
autocontenido; si el periférico la trae, depende de la aplicación, que depende del periférico:
ciclo. La salida es que la infraestructura de ventanas acoplables esté **debajo** de los dos.

`AttachedFrame` no es la aplicación: es un mecanismo — engancharse a una máquina, plegarse, y que
el enganche sea la conexión eléctrica. Eso es tan del emulador como el bus de puertos.

### Qué es un periférico y qué no

**Se queda en base** el hardware propio de una máquina: el pager de la 128 y el del +3, la FDC del
+3, la memoria SE, los dos ULA, el AY que una 128 trae de fábrica. Eso no se enchufa, **es la
computadora**.

**Va a devices** lo que se enchufa: impresora, mouse, Melodik, los dos Kempston, y todo lo que
venga (Interface 1, Beta 128, +D, SpecDrum, teclado de Currah…).

Hoy eso son cuatro o cinco periféricos, no once. Ahí muere la preocupación por la verbosidad.

## Los pasos

Cada uno deja el gate verde y se commitea solo.

**1. Desatar los tres nudos.** Sin mover ningún archivo de módulo todavía: sacar el `JFrame` del
Z80, el `TableModel` de la casetera, e invertir el handler de la api. Al terminar, ninguna clase de
emulación importa Swing ni el escritorio. Es el paso que dice si el corte es posible, y se puede
verificar con un grep antes de escribir un pom.

**2. `machine/ui`.** Mover `AttachedFrame`, los cargadores de iconos, `ScrollPane`, los componentes
de pantalla. Es el módulo más chico y el que menos riesgo tiene.

**3. `machine/app`.** Mover `peripherals/t` (menos lo que se fue a ui), `OOSpectrumLauncher`,
`SwingUserInterface`, la UI de pokes y de config. `machine/base` queda siendo lo que sobra, y hay
que verificar que compila **sin** el classpath de Swing.

**4. `machine/devices`.** Mover los periféricos opcionales con sus vistas y sus service files. Cada
módulo trae su propio `META-INF/services/…DeviceModule`: el `ServiceLoader` lee todos los que
encuentra en el classpath, así que la lista deja de ser un archivo compartido y pasa a ser una
línea que cada módulo trae consigo. Eso es la historia de plugins funcionando de verdad.

**5. La prueba.** Un test en `app` que arranque el emulador y verifique que la impresora está —
sin importar ninguna clase de la impresora, sólo por el bus. Ya existe en espíritu
(`DiscoveredDeviceTest`); ahí pasa a ser real, porque el dispositivo estará en otro jar.

**6. (Opcional, después.)** Promover la impresora a `machine/device-printer`. Es mover una carpeta
y escribir un pom con **una** dependencia. Vale la pena el día que alguien quiera publicar un
periférico sin compilar el emulador; antes de eso, no paga.

## Decisiones abiertas

- **Dónde viven los tests de integración.** `PrinterOnItsMachineTest` y `RomPrintsToThePrinterTest`
  construyen un `Speccy` entero, así que no pueden vivir en el módulo del periférico sin que ese
  módulo dependa de la aplicación. O se van a `app`, o el periférico se prueba sólo por su modelo
  —la correa y el papel, que ya están aislados— y la integración vive arriba. Hay que decidirlo
  antes del paso 4, no cuando el build se queje.

- **El nombre de `machine/base`.** Hoy `machine/core` es todo; si `core` pasa a ser la base, los
  tres módulos nuevos son los que se mueven. Si en cambio `core` queda como la aplicación, se mueve
  la emulación. Lo primero toca menos.

- **`machine/spectrum`** ya existe como módulo hermano. Ver si `base` va ahí o al lado.

## Riesgos

- **El IDE y el build.** Cuatro módulos donde había uno: reload, y unos minutos de compilación
  cruzada la primera vez. No es riesgo técnico, es fricción.

- **Guice.** No cambia nada: los módulos de dispositivo ya se instalan por `ServiceLoader`, que es
  indiferente a en qué jar está cada uno.

- **Mover archivos rompe imports en masa.** Es mecánico, pero conviene un módulo por commit y el
  gate entre medio.

- **La sesión paralela trabaja en `peripherals/t`.** El paso 3 mueve ese paquete entero: hay que
  coordinarlo, o hacerlo cuando esa zona esté quieta.

## Lo que ya está hecho y hace esto posible

- Los dispositivos se descubren por classpath (`DeviceModule` + `ServiceLoader`), no por una lista.
- Cada dispositivo declara en qué máquina encaja (`fitsOn`) y si lo quieren (`isWanted`).
- Un dispositivo pide roles a la máquina (`Paging128`, `PagingPlus3`, `FloppyDrive`), nunca clases.
- Ningún código fuera de `speccy/devices` nombra un dispositivo, salvo las dos ventanas — y ésas
  son justo las que el paso 4 se lleva con su periférico.
