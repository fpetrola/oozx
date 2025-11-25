# Estado del Refactoring del Módulo Joystick

## Resumen Ejecutivo

Se ha completado la primera fase del refactoring del módulo joystick para separarlo en un módulo Maven independiente. Se ha extraído un núcleo de interfaces base a `machine-core` y se ha creado el módulo `machine-peripheral-joystick` para contener la implementación del joystick.

## Cambios Realizados

### 1. Creación de machine-core
Se creó un nuevo módulo Maven `machine-core` que contiene todas las interfaces y clases base necesarias para que los periféricos se registren e implementen:

**Interfaces base:**
- `ZxModule` - interfaz base para módulos de la máquina
- `ZXModuleInfo` - interfaz para información de módulos (snapshots, reset, etc.)
- `PortHandler` - interfaz para manejadores de puertos I/O
- `DefaultPortHandler` - clase abstracta base para manejadores de puertos
- `StartupModule` - interfaz para módulos de inicio
- `AbstractStartupModule` - clase abstracta base para módulos de inicio
- `MachineChangeListener` - interfaz para escuchar cambios de máquina
- `ZxPeripheral` - interfaz para periféricos
- `Periph` - enums para tipos y presencia de periféricos
- `IPeriph` - interfaz principal para gestión de periféricos
- `JoystickProvider` - interfaz para joystick
- `StartupModuleProvider` - interfaz para proveedores de módulos de inicio

### 2. machine-core pom.xml
Se configuró el pom.xml de machine-core sin dependencias internas (solo hereda del proyecto padre).

### 3. Actualización de machine
- Se actualizó `machine/pom.xml` para depender de `machine-core`
- Se copiaron las clases de joystick a `machine/src/main/java/com/fpetrola/oozx/joystick/` manteniendo la estructura de paquetes
- Se actualiza

ron los imports en archivos que usan Joystick:
  - `Fuse.java` - ahora importa de `com.fpetrola.oozx.joystick.modules`
  - `Input.java` - ahora importa de `com.fpetrola.oozx.joystick.modules`
  - `Settings.java` - actualizado
  - `Ui.java` - actualizado
  - `Libspectrum.java` - actualizado
- Se resolvió ambigüedad de `Module` usando fully qualified name `com.fpetrola.oozx.Module`

### 4. Creación de machine-peripheral-joystick
Se creó el módulo `machine-peripheral-joystick` con:
- Dependencias en `machine-core` para las interfaces base
- Dependencias en `machine` para acceso a clases específicas del Spectrum
- Misma estructura de paquetes que en machine para facilitar migración futura

**Archivos en machine-peripheral-joystick:**
- `joystick/modules/Joystick.java` - implementación del joystick que implementa `JoystickProvider`
- `joystick/modules/JoystickModuleInfo.java` - información del módulo
- `joystick/handlers/JoystickPortHandler.java` - manejador de puertos
- `joystick/startup/JoystickStartupModule.java` - módulo de inicio
- `joystick/UiJoystick.java` - interfaz de usuario para joystick

### 5. Estructura Maven Final
```
pom.xml (raíz)
├── machine-core/ (interfaces base)
├── machine/ (implementación de máquina, ahora incluye joystick)
└── machine-peripheral-joystick/ (periféricos opcionales)
```

## Estado de Compilación

✅ `mvn clean compile -DskipTests` ejecuta exitosamente

Todos los módulos compilan sin errores:
- machine-core: ✅
- machine: ✅
- machine-peripheral-joystick: ✅

## Próximos Pasos

1. **Separación Completa del Joystick**: Remover las clases de joystick de machine/src y hacer que machine importe desde machine-peripheral-joystick usando inyección de dependencias o carga dinámica.

2. **Otros Periféricos**: Aplicar el mismo patrón a otros periféricos (Keyboard, Tape, etc.) para crear módulos independientes.

3. **Gestión de Dependencias**: Implementar un sistema de carga de módulos dinámicos para que machine no tenga que depender explícitamente de periféricos específicos.

4. **Tests**: Crear tests unitarios para verificar que cada módulo se puede compilar y usar independientemente.

## Notas Técnicas

- Se mantienen dos copias de las clases joystick (una en machine, otra en machine-peripheral-joystick) para mantener compatibilidad durante la migración gradual.
- La clase `Joystick` en machine implementa `JoystickProvider` (interfaz base) para permitir polimorfismo.
- Se usó fully qualified name para resolver ambigüedad entre `com.fpetrola.oozx.Module` y `java.lang.Module` (Java 9+).

## Archivos Modificados

- `/pom.xml` - orden de módulos
- `/machine-core/pom.xml` - creado
- `/machine-core/src/main/java/` - interfaces base
- `/machine/pom.xml` - dependencia a machine-core
- `/machine/src/main/java/com/fpetrola/oozx/Fuse.java` - imports
- `/machine/src/main/java/com/fpetrola/oozx/Input.java` - imports
- `/machine/src/main/java/com/fpetrola/oozx/Settings.java` - imports
- `/machine/src/main/java/com/fpetrola/oozx/Ui.java` - imports
- `/machine/src/main/java/com/fpetrola/oozx/Libspectrum.java` - imports
- `/machine/src/main/java/com/fpetrola/oozx/joystick/` - copias de joystick
- `/machine-peripheral-joystick/pom.xml` - actualizado
- `/machine-peripheral-joystick/src/main/java/com/fpetrola/oozx/joystick/` - clases joystick
