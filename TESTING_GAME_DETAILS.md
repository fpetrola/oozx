# Testing Game Details Feature

## Compilación

### Compilar el proyecto completo
```bash
mvn clean compile
```

### Compilar solo el módulo de máquina
```bash
mvn -pl machine clean compile
```

### Compilar y empaquetar
```bash
mvn clean package -DskipTests
```

## Testing Manual

### 1. Iniciar la Aplicación
```bash
mvn exec:java -pl machine
```

O compilar y ejecutar el JAR:
```bash
mvn clean package -DskipTests
java -jar machine/target/machine-1.0-SNAPSHOT.jar
```

### 2. Probar la Búsqueda de Juegos
1. Navega a la pestaña "Game Browser"
2. Busca un juego conocido, por ejemplo:
   - "Jet Pac"
   - "Pac-Man"
   - "Manic Miner"
   - "Dizzy"
   - "Commando"

### 3. Probar el Diálogo de Detalles
1. En los resultados de búsqueda, haz clic derecho en un juego
2. Selecciona "View Details"
3. Espera a que cargue el diálogo (verás un spinner de carga)
4. Verifica que:
   - La portada se carga correctamente
   - Las estrellas de puntuación se muestran
   - Los tabs contienen información
   - Las pantallas se cargan en la galería

### 4. Verificar Cada Tab

#### Tab "General"
- [ ] Se muestra el título del juego
- [ ] Aparece el año de lanzamiento
- [ ] Se muestra la fecha completa si está disponible
- [ ] Editorial y género aparecen correctamente
- [ ] ID del juego es visible

#### Tab "Technical"
- [ ] Se lista el tipo de máquina
- [ ] Se muestran las máquinas compatibles
- [ ] Formato y tipo de contenido son visibles

#### Tab "Publishers"
- [ ] Se listan todas las editoriales
- [ ] Múltiples editoriales se muestran correctamente

#### Tab "Authors"
- [ ] Se listan desarrolladores/autores
- [ ] Si no hay autores, muestra "No author information available"

#### Tab "Description"
- [ ] Se muestra la descripción completa
- [ ] El texto es desplazable si es muy largo

#### Tab "Screenshots"
- [ ] Las imágenes se cargan desde URLs
- [ ] Se muestran en cuadrícula
- [ ] Las imágenes se redimensionan correctamente
- [ ] Las que no carguen muestran un placeholder

#### Tab "Releases"
- [ ] Se listan los lanzamientos por plataforma
- [ ] Editorial y año se muestran correctamente

#### Tab "Downloads"
- [ ] Se listan descargas adicionales disponibles
- [ ] Si no hay, muestra "No additional downloads available"

### 5. Pruebas de Error

#### Prueba sin conexión a internet
1. Desconecta la red
2. Abre el diálogo de detalles
3. Debe mostrar un error y fallback a información básica
4. Verifica que no se congele la UI

#### Prueba con juego sin portada
1. Busca un juego antiguo que no tenga portada
2. Verifica que muestre un placeholder apropiado

#### Prueba con juego sin pantallas
1. Algunos juegos pueden no tener pantallas
2. Verifica que el tab de screenshots muestre "No screenshots available"

## Verificación de Compilación

### Sin Warnings Críticos
```bash
mvn clean compile 2>&1 | grep -i error
# No debe mostrar ningún ERROR
```

### Classes Compiladas
Verifica que existan:
```bash
ls -la machine/target/classes/com/fpetrola/oozx/api/GameDetail.class
ls -la machine/target/classes/com/fpetrola/oozx/fuse/peripherals/t/GameDetailsDialog.class
```

## Casos de Uso Específicos

### Juego Popular (muchos datos)
Busca: "Jet Pac"
- Debe tener muchas pantallas
- Múltiples editoriales
- Información completa en todos los tabs

### Juego Antiguo (pocos datos)
Busca: "3D Monster Maze"
- Puede no tener portada
- Información limitada
- Algunos campos vacíos (N/A)

### Juego Español
Busca: "Batman"
- Busca juegos españoles para probar caracteres especiales

## Performance

### Tiempo de Carga
- El diálogo debe abrirse en 1-2 segundos máximo
- Las imágenes se cargan en segundo plano
- La UI no debe bloquearse

### Consumo de Memoria
- El diálogo debe cerrarse correctamente
- No debe haber memory leaks
- Las imágenes no cargadas no deben consumir memoria

## Diagnóstico

### Ver logs de la aplicación
```bash
tail -f app.log
```

### Depuración de conexión al API
```bash
curl https://api.zxinfo.dk/v3/search?query=jetpac&size=1
# Debe retornar JSON válido
```

### Verificar que la URL de un juego sea válida
El ID se extrae de:
- Full URL: `https://zxinfo.dk/games/12345/jetpac`
- Se extrae: `jetpac`

## Checklist de Testing Completo

- [ ] Compilación sin errores
- [ ] Búsqueda de juegos funciona
- [ ] Diálogo abre correctamente
- [ ] Todos los tabs cargan datos
- [ ] Las imágenes se cargan
- [ ] La UI no se congela
- [ ] Manejo de errores funciona
- [ ] Información se muestra correctamente
- [ ] Fallback funciona sin conexión
- [ ] Rendimiento es aceptable

## Reportar Problemas

Si encuentras problemas:

1. **Compila de nuevo**
   ```bash
   mvn clean compile
   ```

2. **Revisa los logs**
   ```bash
   # En la consola de la aplicación
   ```

3. **Verifica la conexión de red**
   ```bash
   ping api.zxinfo.dk
   ```

4. **Comprueba las clases compiladas**
   ```bash
   javap -cp machine/target/classes com.fpetrola.oozx.api.GameDetail
   ```

## Notas de Testing

- Los datos siempre serán los más actuales del API
- La primera vez que se abre un diálogo, la carga será más lenta (network latency)
- Las imágenes que no carguen desde media.zxinfo.dk mostrarán placeholders
- El comportamiento puede variar según la disponibilidad de datos del API
