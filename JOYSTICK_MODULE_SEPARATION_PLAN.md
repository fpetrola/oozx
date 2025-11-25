# Plan de Separación del Módulo Joystick

## Objetivo
Separar la funcionalidad de joystick en un submódulo Maven independiente (`machine-peripheral-joystick`) del módulo `machine`.

## Estado Actual
Los archivos de joystick están ubicados en `machine/src`:
- `com/fpetrola/oozx/fuse/modules/Joystick.java`
- `com/fpetrola/oozx/fuse/modules/JoystickModuleInfo.java`
- `com/fpetrola/oozx/fuse/ports/JoystickPortHandler.java`
- `com/fpetrola/oozx/fuse/startup/JoystickStartupModule.java`
- `com/fpetrola/oozx/UiJoystick.java`

## Archivos Creados (incompletos)
Se han creado estructuras iniciales en:
- `/machine-peripheral-joystick/pom.xml`
- `/machine-core/pom.xml`

## Desafío Principal
El módulo joystick tiene dependencias en:
- `com.fpetrola.oozx.fuse.modules.*` (ZxModule, ZXModuleInfo, Keyboard, etc.)
- `com.fpetrola.oozx.fuse.peripherals.*` (IPeriph, KempstonStrictPeripheral, etc.)
- `com.fpetrola.oozx.fuse.startup.*` (AbstractStartupModule, LibspectrumStartupModule)
- `com.fpetrola.oozx.*` (Settings, Libspectrum, Ui, etc.)

**El problema:** Estas clases están en el módulo `machine`, por lo que habría una **dependencia circular** si movemos joystick a un submódulo independiente.

## Soluciones Posibles

### Opción 1: Extraer interfaces a `machine-core` (RECOMENDADO)
1. Crear un módulo `machine-core` con todas las interfaces base:
   - `ZxModule`
   - `ZXModuleInfo`
   - `IPeriph`
   - `DefaultPortHandler`
   - `AbstractStartupModule`
   - Etc.

2. Hacer que `machine` dependa de `machine-core`

3. Hacer que `machine-peripheral-joystick` dependa de `machine-core`

4. Mover las clases de joystick a `machine-peripheral-joystick`

**Ventajas:**
- Arquitectura clara y modular
- Permite múltiples periféricos independientes
- Sin dependencias circulares

**Desventajas:**
- Requiere refactorización significativa
- Cambios en muchos archivos

### Opción 2: Mantener en `machine` con mejor organización
Reorganizar dentro del módulo `machine`:
```
machine/src/main/java/com/fpetrola/oozx/
├── core/              (interfaces base)
├── joystick/          (lógica de joystick)
├── keyboard/          (otros periféricos)
└── ...
```

**Ventajas:**
- Cambios mínimos
- Fácil de implementar

**Desventajas:**
- No es un verdadero submódulo Maven
- Menos encapsulación

### Opción 3: Usar composición de módulos
Crear un POM padre específico para periféricos:
```
machine-peripherals/
├── pom.xml (tipo pom, sin código)
├── machine-peripheral-joystick/
├── machine-peripheral-keyboard/
└── ...
```

## Recomendación
**Opción 1** (extraer interfaces a `machine-core`) es la más limpia para una arquitectura modular.

## Pasos para Implementar Opción 1

1. **Extraer interfaces de `machine` a `machine-core`:**
   - Copiar interfaces base a `machine-core/src`
   - Actualizar `machine/pom.xml` para depender de `machine-core`

2. **Mover código de joystick:**
   - Copiar archivos joystick a `machine-peripheral-joystick/src`
   - Actualizar package statements (fuse.modules -> joystick.modules, etc.)
   - Actualizar imports

3. **Actualizar dependencias:**
   - `machine-core`: sin dependencias internas
   - `machine`: depende de `machine-core`
   - `machine-peripheral-joystick`: depende de `machine-core`
   - Agregar `machine-peripheral-joystick` al pom.xml raíz

4. **Actualizar imports en archivos que usan Joystick**

5. **Eliminar archivos originales de `machine/src`**

6. **Compilar y verificar**

## Próximos Pasos
- Seleccionar la opción preferida
- Implementar la extracción de interfaces a `machine-core`
- Mover y refactorizar el código de joystick
