# El teclado: qué hay hoy y cómo debería quedar

## Las clases que hay

| clase | líneas | qué hace hoy |
|---|---|---|
| `modules/keyboard/Keyboard` | 407 | la matriz, los nombres de las teclas, la tabla host → Spectrum, la tabla de códigos de ventana, y los textos |
| `modules/input/Input` | 555 | el enum de 200 teclas del host, el evento, el joystick por teclado, el Recreated ZX |
| `desktop/SwingKeyboard` | 298 | dos tablas de códigos de Swing y el `KeyListener` |
| `modules/joystick/Joystick` | | tres tablas de cinco teclas cada una: cursor, Sinclair 1 y 2 |
| `modules/tape/TapeAutoLoader` | | teclea `LOAD ""` |
| `modules/ula/Ula` | | lee la matriz en el puerto 0xFE |

## Cómo se usan

- **La máquina lee**: `Ula.read` hace `keyboard.read(port >> 8)`, y eso es una sola indexación en `byHighByte`, una tabla de 256 entradas que se rehace cada vez que una tecla cambia. Esa parte está bien y es la única en el camino caliente.
- **La persona escribe**: `SwingKeyboard` recibe el `KeyEvent`, traduce el código con `keyboard.remap` y el carácter con su propia tabla, arma un `Input.InputEvent` y llama a `input.event`. `Input` decide si es joystick, si es Recreated, y termina llamando `keyboard.press` de una o dos teclas.
- **El emulador escribe solo**: `TapeAutoLoader` y `Joystick` llaman `press` y `release` con teclas del Spectrum, sin pasar por `Input`.

## Lo que está mal

1. **`Keyboard` es cuatro cosas.** La matriz de la máquina, el diccionario de qué tecla del host produce qué teclas del Spectrum, el diccionario de qué código de ventana es qué tecla del host, y los textos para mostrar. Sólo la primera es del Spectrum; las otras tres son de quien tiene un teclado de PC delante.
2. **Nombres que no dicen nada.** `Place`, `Placed`, `Pressed`, `Produces`, `Keysym`, `returnValues`, `defaultValue`, `keyboardData`, `spectrumKeys`, `keysymsHash`. `Pressed` no es "presionada": es el par de teclas que una tecla del host produce. `Placed` es una fila de la tabla de posiciones. `KEYBOARD_a` repite el prefijo en cada uno de los 43 valores.
3. **Cuatro tablas para lo mismo.** `KEYBOARD_DATA_TABLE` dice dónde está cada tecla, `KEY_TEXT_TABLE` cómo se llama, y en `Joystick` hay tres tablas más que nombran teclas por posición. La tecla no sabe nada de sí misma: hay un enum con un número arbitrario y cuatro mapas afuera que le cuelgan atributos.
4. **Estado global mutable.** `Keyboard.KEYSYMS_MAP` es un campo estático que la aplicación pisa antes de arrancar. Dos emuladores comparten teclado.
5. **Tres pasos en vez de uno.** Del `KeyEvent` a la tecla del Spectrum se pasa por código de ventana, tecla del host, par de teclas. Los dos primeros existen porque un emulador con varios toolkits necesita un enum de teclas propio; acá hay uno solo.
6. **Sin polimorfismo donde hace falta.** `Input.event` es un switch de cuatro casos sobre un `Object` casteado, y `Input.doJoystick` decide con un switch qué botón es. `Joystick` elige la tabla con un switch de tipo de joystick.
7. **`simulateKeypress` y `keyText` no los llama nadie.** `releaseAll` tampoco, salvo desde adentro.

## Cómo quedó

Hecho. Las clases nuevas, en `modules/keyboard`:

| clase | líneas | qué es |
|---|---|---|
| `SpectrumKey` | 65 | las 40 teclas, cada una con su media fila, su bit y su etiqueta |
| `KeyMatrix` | 73 | las ocho medias filas y lo que lee un puerto; la tabla de 256 es suya y privada |
| `Combination` | 47 | lo que una pulsación pone abajo, y cómo aplicarlo a la matriz |
| `KeyLayout` | 40 | la interfaz: qué produce una tecla de quien escribe |
| `PcLayout` | 115 | el teclado de PC |
| `RecreatedLayout` | 152 | el Recreated ZX, que lee lo que vino antes y suelta apretando |
| `Keyboard` | 77 | la fachada: la matriz abajo, el layout arriba |

`Input` bajó de 555 a 446 líneas: se fueron las dos tablas del Recreated, su estado y su `if`.
`SwingKeyboard` se quedó con sus dos tablas de códigos de Swing, que son suyas, y `Keyboard.KEYSYMS_MAP`,
el estático global que la aplicación pisaba, desapareció.

## Lo que se planeó

Cinco conceptos, uno por clase, en `modules/keyboard`:

- **`SpectrumKey`**: un enum con las 40 teclas, y cada una sabe su media fila, su bit y lo que dice arriba. `SpectrumKey.Z.row()`, `.bit()`, `.label()`. Se van `Place`, `Placed`, `Label`, `KEYBOARD_DATA_TABLE`, `KEY_TEXT_TABLE` y los tres mapas que las leen.
- **`KeyMatrix`**: las ocho medias filas y qué lee un puerto. `press(SpectrumKey)`, `release(SpectrumKey)`, `releaseAll()`, `read(int high)`. Es lo único que la ULA ve, y conserva la tabla de 256 como optimización encapsulada.
- **`Combination`**: las teclas que una pulsación produce, una o dos. Reemplaza `Pressed` con un nombre que dice qué es, y sabe `pressOn(matrix)` y `releaseOn(matrix)`, que es lo que hoy hace `Input` a mano dos veces.
- **`KeyLayout`**: el mapa de qué produce cada tecla de quien escribe. Una interfaz, con la disposición normal y la del Recreated ZX como dos implementaciones, en vez del `if (config.recreatedSpectrum)` y los dos mapas dentro de `Input`.
- **`Typist`**, en `modules/tape` o donde se use: teclear una secuencia con esperas, que es lo que `TapeAutoLoader` tiene adentro.

Y en `app`, una sola clase que traduzca `KeyEvent` a `SpectrumKey`, sin pasar por un enum intermedio de 200 teclas. El enum `Input.InputKey` sobrevive sólo si el joystick lo necesita para su configuración.

## Lo que hay que cuidar

- `Ula.read` es lo único caliente: ocho lecturas por frame como mínimo, más lo que el juego haga. La tabla de 256 se queda.
- El bridge compara contra el emulador de referencia por `ZXSpectrumULATests`, así que la lectura del puerto tiene que dar los mismos bytes.
- `Input.Config` se guarda en el archivo de configuración: `keyboard.up`, `.down`, etc. son códigos de `InputKey`. Cambiar el enum cambia lo guardado.

# El joystick

## Lo que había

Una clase de 226 líneas, `Joystick`, con todo adentro: dos enums, dos arreglos de nombres, cinco
tablas de bits y de teclas, cuatro campos de estado, y un `switch` de ocho casos donde cada caso
repetía el mismo `if (press) ... else ...` con otra tabla. La lógica de cada tipo de joystick
estaba en una rama, no en un objeto.

## Lo que quedó

| clase | líneas | qué es |
|---|---|---|
| `Direction` | 24 | las cinco formas de empujarlo |
| `JoystickKind` | 34 | la interfaz: qué hace un empujón, y qué lee su puerto |
| `PortedJoystick` | 68 | el que tiene puerto propio: Kempston, Timex y Fuller, que son los mismos bits con el sentido invertido |
| `KeyedJoystick` | 48 | el cableado a teclas de la máquina: Cursor y los dos del Interface 2, que son cinco teclas distintas |
| `NoJoystick` | 26 | nada enchufado; el empujón no es suyo |
| `Joystick` | 127 | los tres zócalos y qué hay en cada uno |

El `switch` de ocho casos desapareció: hay un objeto por tipo, hecho una vez, y `press` le pasa el
empujón al que está en ese zócalo. Un Kempston y un Fuller son la misma clase con el sentido del
bit al revés; un Cursor y un Sinclair 1 son la misma clase con otras cinco teclas.
