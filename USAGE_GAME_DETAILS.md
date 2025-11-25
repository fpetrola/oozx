# Game Details Dialog - Usage Guide

## Overview
El diálogo de detalles del juego muestra información completa y real del API de ZXInfo.dk, incluyendo imágenes, pantallas, información técnica y más.

## Flujo de Uso

### 1. Buscar Juego
1. Abre la aplicación ZX Spectrum Desktop Emulator
2. En la pestaña "Game Browser", busca un juego usando el campo de búsqueda
3. Los resultados aparecerán en la lista

### 2. Ver Detalles
1. Haz clic derecho en un juego de la lista
2. Selecciona "View Details" del menú contextual
3. Se muestra un diálogo de carga mientras se obtienen los datos del API
4. El diálogo se abre con toda la información del juego

## Contenido del Diálogo

### Panel Izquierdo (280px)
**Portada del Juego**
- Carga automática de la imagen de portada del API
- Si no hay portada, muestra un placeholder

**Sistema de Puntuación**
- Estrellas visuales basadas en la puntuación del juego (0-5)
- Puntuación numérica en escala 0-100
- Indicador ⚠ si el juego está marcado como X-Rated

**Información Rápida**
- Año de lanzamiento
- Editorial principal
- Género
- Tipo de máquina
- Memoria requerida
- Estado de disponibilidad

**Checkbox de Favoritos**
- Marca el juego como favorito (futuro: persistencia)

### Panel Derecho (Tabs)

#### Tab 1: General
- Información básica del juego
- Tabla con todas las propiedades principales:
  - Título completo
  - Año de lanzamiento
  - Fecha de lanzamiento (año-mes-día si está disponible)
  - Géneros (tipo y subtipo)
  - Tipo de máquina
  - Disponibilidad
  - ID único del juego
  - Puntuación
  - ISBN (si aplica)

#### Tab 2: Technical
- Especificaciones técnicas
- Máquinas compatibles (si están disponibles)
- Formato y tipo de contenido
- Información de versión

#### Tab 3: Publishers
- Lista de editoriales/productoras
- Pueden ser múltiples si el juego tuvo varias ediciones

#### Tab 4: Authors
- Lista de desarrolladores/autores
- Programadores, diseñadores gráficos, compositores, etc.

#### Tab 5: Description
- Descripción completa del juego
- Texto de carátula o descripción del catálogo
- Área de texto desplazable

#### Tab 6: Screenshots
- Galería de pantallas del juego
- Carga automática desde URLs del API
- Disposición en cuadrícula (ajustada automáticamente)
- Cada pantalla se redimensiona a 200x150px
- Soporte para múltiples pantallas

#### Tab 7: Releases
- Historial de lanzamientos por plataforma
- Editorial y año de cada lanzamiento
- Múltiples versiones si existen

#### Tab 8: Downloads
- Descargas adicionales disponibles
- Manuales, carteles, contenido extra

## Panel Inferior - Opciones de Emulación

### Selección de Modelo
- **Spectrum 48K**: Memoria básica
- **Spectrum 128K**: Memoria extendida
- **Spectrum +3**: Unidad de disco integrada

### Control de Velocidad
- Rango de 0.5x a 4.0x la velocidad normal
- Incrementos de 0.5x
- Útil para juegos que corren demasiado rápido/lento

### Características
- **Mute Sound**: Desactiva el audio
- **Turbo Mode**: Acelera la ejecución
- **Fullscreen**: Pantalla completa (futuro)

### Botones de Acción
- **▶ Play Game**: Inicia el emulador con este juego (futuro)
- **↓ Download**: Descarga el juego (futuro)
- **🌐 Open Link**: Abre la página de ZXInfo en el navegador (futuro)
- **Close**: Cierra el diálogo

## Características Técnicas

### Carga Asincrónica
- Las imágenes se cargan en hilos separados
- La UI permanece responsiva durante la descarga
- Las imágenes se redimensionan automáticamente

### Manejo de Errores
- Si una imagen no puede cargarse, muestra un placeholder
- Si la llamada al API falla, muestra información básica como fallback
- Los campos nulos se muestran como "N/A"

### Fuentes de Datos
- **URL Base del API**: `https://api.zxinfo.dk`
- **Endpoint de Detalles**: `GET /v3/games/{id}`
- **URLs de Media**: `https://media.zxinfo.dk/media/{filename}`

## Notas Importantes

1. **Conexión a Internet Requerida**: El diálogo requiere acceso a internet para cargar datos del API
2. **Carga Asincrónica**: No esperes que todo cargue instantáneamente - las imágenes se cargan en segundo plano
3. **Información en Tiempo Real**: Los datos mostrados son siempre los más recientes del API
4. **Campos Opcionales**: Algunos campos pueden estar vacíos si no están disponibles en el API

## Troubleshooting

### El diálogo tarda mucho en abrirse
- Verifica tu conexión a internet
- Es normal que tarde 1-2 segundos en obtener los datos del API

### Las imágenes no se cargan
- Verifica que tengas acceso a `https://media.zxinfo.dk`
- Algunos juegos pueden no tener imágenes disponibles

### Error: "Error loading game details"
- Asegúrate de tener conexión a internet
- El ID del juego puede ser inválido
- El API de ZXInfo puede estar temporalmente no disponible

### Las tablas muestran "N/A"
- Significa que el API no tiene datos para ese campo
- Es completamente normal para juegos antiguos o menos documentados

## Funcionalidad Futura

- [ ] Persistencia de favoritos
- [ ] Jugar directamente desde el diálogo
- [ ] Descargar juegos
- [ ] Abrir página web de ZXInfo
- [ ] Búsqueda avanzada con filtros
- [ ] Caché local de datos
- [ ] Historial de visualizaciones
- [ ] Reseñas y puntuaciones de usuarios
