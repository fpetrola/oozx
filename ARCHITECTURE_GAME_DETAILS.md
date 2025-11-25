# Game Details Architecture

## Diagrama de Flujo

```
Usuario
   ↓
┌──────────────────────────────────────────┐
│   ZXSpectrumDesktopApp.onViewDetails()   │ Clic en "View Details"
└──────────────────────────────────┬───────┘
   ↓
┌──────────────────────────────────────────┐
│   Mostrar Diálogo de Carga               │ "Fetching from API..."
└──────────────────────────────┬───────────┘
   ↓
┌──────────────────────────────────────────┐
│   SwingWorker (Hilo Separado)            │ No bloquea UI
│   → ZxInfoApiHandler.fetchGameDetails()  │
│   → GET /v3/games/{id}                   │
└──────────────────────────┬────────────────┘
   ↓
┌──────────────────────────────────────────┐
│   API: api.zxinfo.dk                     │ Conexión HTTP
│   Respuesta: GameEntry JSON              │
└──────────────────────────────┬────────────┘
   ↓
┌──────────────────────────────────────────┐
│   convertGameEntryToDetail()              │ Mapeo de datos
│   - Procesa listas                       │
│   - Construye URLs                       │
│   - Maneja nulos                         │
└──────────────────────────────┬────────────┘
   ↓
┌──────────────────────────────────────────┐
│   GameDetail (Objeto Completo)           │ Objeto poblado
└──────────────────────────────┬────────────┘
   ↓
┌──────────────────────────────────────────┐
│   GameDetailsDialog.initializeComponents()│ Construir UI
│   - Panel Izquierdo                      │
│   - Panel Derecho (8 Tabs)               │
│   - Panel Inferior                       │
└──────────────────────────────┬────────────┘
   ↓
┌──────────────────────────────────────────┐
│   loadImageAsync()                       │ Carga en hilos
│   SwingWorker para cada imagen           │
└──────────────────────────────┬────────────┘
   ↓
┌──────────────────────────────────────────┐
│   GameDetailsDialog Visible              │ Mostrar al usuario
└──────────────────────────────────────────┘
```

## Componentes Principales

### 1. GameDetail (Modelo de Datos)
```
GameDetail
├── id: String
├── title: String
├── yearOfRelease: String
├── originalMonthOfRelease: Integer
├── originalDayOfRelease: Integer
├── genre: String
├── genreType: String
├── genreSubType: String
├── machineType: String
├── machines: List<String>
├── memoryRequired: String
├── availability: String
├── score: Double
├── xrated: Integer
├── isbn: String
├── publisher: String
├── publishers: List<String>
├── authors: List<String>
├── screenshots: List<String> (URLs)
├── description: String
├── releases: List<Map<String, String>>
├── additionalDownloads: List<String>
└── coverImageUrl: String
```

### 2. ZxInfoApiHandler (Controlador API)
```
ZxInfoApiHandler
├── search(String query): List<Hit>
│   └── Llama a: GET /v3/search
├── fetchGameDetails(String gameId): GameDetail
│   ├── Llama a: GET /v3/games/{id}
│   └── Convierte respuesta a GameDetail
└── convertGameEntryToDetail(GameEntry, String): GameDetail
    ├── Mapea campos simples
    ├── Procesa listas
    ├── Construye URLs de media
    └── Maneja valores nulos
```

### 3. GameDetailsDialog (Vista)
```
GameDetailsDialog
├── createLeftPanel(): JPanel
│   ├── coverLabel (carga async)
│   ├── ratingPanel
│   ├── infoSummaryPanel
│   └── favoriteCheckBox
├── createRightPanel(): JPanel
│   └── JTabbedPane (8 tabs)
│       ├── General Tab
│       ├── Technical Tab
│       ├── Publishers Tab
│       ├── Authors Tab
│       ├── Description Tab
│       ├── Screenshots Tab
│       ├── Releases Tab
│       └── Downloads Tab
├── createButtonsPanel(): JPanel
│   ├── Model selector
│   ├── Speed control
│   ├── Feature checkboxes
│   └── Action buttons
└── loadImageAsync(String url, JLabel, int, int, String)
    └── SwingWorker para carga de imágenes
```

## Flujo de Datos

### Búsqueda → Detalles
```
1. Usuario busca juego
2. API devuelve lista de GameEntry (búsqueda)
3. Usuario selecciona "View Details"
4. Se extrae ID del juego
5. Se llama a fetchGameDetails(id)
6. Se retorna GameDetail completamente poblado
7. Se crea GameDetailsDialog con GameDetail
8. UI se popula con datos reales
```

### Carga de Imágenes
```
Para cada URL de imagen:
1. SwingWorker.doInBackground()
   - Descarga imagen de URL
   - La redimensiona
   - Retorna ImageIcon
2. SwingWorker.done()
   - Establece ImageIcon en JLabel
   - Si falla, muestra placeholder
3. UI se actualiza sin bloqueo
```

## Patrones Utilizados

### 1. Model-View-Controller (MVC)
- **Model**: `GameDetail`, `GameEntry`
- **View**: `GameDetailsDialog`
- **Controller**: `ZxInfoApiHandler`

### 2. SwingWorker (Threading)
```java
SwingWorker<GameDetail, Void> worker = 
    new SwingWorker<GameDetail, Void>() {
        protected GameDetail doInBackground() {
            // Ejecuta en hilo separado
            return apiHandler.fetchGameDetails(id);
        }
        protected void done() {
            // Ejecuta en EDT (Event Dispatch Thread)
            GameDetail detail = get();
            showDialog(detail);
        }
    };
worker.execute();
```

### 3. Builder Pattern (UI Construction)
```java
// Cada método crea un panel específico
createLeftPanel()
createRightPanel()
  - createGeneralInfoPanel()
  - createTechnicalSpecsPanel()
  - ... (6 más)
createButtonsPanel()
```

### 4. Async Image Loading
```java
loadImageAsync(url, label, width, height, fallback)
// No bloquea UI, carga en segundo plano
```

## Manejo de Errores

### Niveles de Fallback
```
Nivel 1: API Call Success
  → Todos los datos reales

Nivel 2: API Call Fails
  → Información básica del juego
  → Imágenes de búsqueda disponibles

Nivel 3: Network Error
  → Diálogo de error
  → No se congela la aplicación

Nivel 4: UI Error
  → Placeholder para imágenes
  → "N/A" para campos faltantes
```

### Exception Handling
```java
try {
    // API call
    GameEntry entry = zxClient.getGameDetails(id);
    return convertGameEntryToDetail(entry, id);
} catch (Exception e) {
    System.err.println("Error fetching game details: " + e.getMessage());
    return null;  // Fallback
} finally {
    if (client != null) {
        client.close();  // Cleanup
    }
}
```

## Optimizaciones

### 1. Threading
- API calls en `SwingWorker` (no bloquea EDT)
- Image loading asincrónico
- UI responsiva mientras carga

### 2. URL Handling
```java
// Intentos múltiples de URL
if (screen.url != null) {
    use screen.url;
} else if (screen.scrUrl != null) {
    use screen.scrUrl;
} else if (screen.filename != null) {
    construct URL from filename;
}
```

### 3. Null Safety
```java
// Verificación de null en cada paso
if (publishers != null && !publishers.isEmpty()) {
    for (Publisher pub : publishers) {
        if (pub.name != null) {
            // procesar
        }
    }
}
```

## Dependencias

### Jakarta REST Client
```xml
<dependency>
    <groupId>jakarta.ws.rs</groupId>
    <artifactId>jakarta.ws.rs-api</artifactId>
</dependency>
<dependency>
    <groupId>org.jboss.resteasy</groupId>
    <artifactId>resteasy-client</artifactId>
</dependency>
```

### Swing (Estándar de Java)
```java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultTreeModel;
```

## Escalabilidad

### Posibles Mejoras
1. **Caché Local**
   - Guardar respuestas de API
   - Reducir llamadas de red
   - Mostrar datos cached mientras se actualiza

2. **Búsqueda Avanzada**
   - Filtros por género, año, máquina
   - Uso de parámetros del API

3. **Descarga de Juegos**
   - Integración con servicio de descarga
   - Progreso de descarga

4. **Persistencia**
   - Base de datos local para favoritos
   - Historial de visualizaciones
   - Configuraciones por usuario

5. **Multimedia**
   - Soporte para video si está disponible
   - Audio de demostración

## Seguridad

### Consideraciones
1. **Validación de URL**
   - Verificar que sea HTTPS
   - Validar formato de URL

2. **Sanitización**
   - No ejecutar código del API
   - Mostrar como texto plain

3. **Rate Limiting**
   - No hacer múltiples llamadas
   - Implementar caché

## Testing

### Unit Tests
```java
// Testear convertGameEntryToDetail()
// Testear manejo de nulos
// Testear construcción de URLs
```

### Integration Tests
```java
// Testear llamada real a API
// Testear threading
// Testear carga de imágenes
```

### UI Tests
```java
// Testear apertura de dialogo
// Testear población de campos
// Testear navegación de tabs
```

## Documentación Externa

- [API ZXInfo Swagger](https://api.zxinfo.dk/v3/swagger_v3.yaml)
- [Jakarta REST](https://jakarta.ee/specifications/restful-ws/)
- [Java Swing Threading](https://docs.oracle.com/javase/tutorial/uiswing/concurrency/)
