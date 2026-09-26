package com.mrtdk.liquid_glass.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class ReleaseItem(
    val title: String,
    val description: String,
    val tag: String? = null
)

data class ReleaseCategory(
    val categoryName: String,
    val icon: ImageVector,
    val items: List<ReleaseItem>
)

object ReleaseNotes {
    const val VERSION_NAME = "0.6.5"
    const val VERSION_TAG = "v0.6.5"
    const val RELEASE_TITLE = "RayMusic v0.6.5 - Rendimiento Supremo, Animaciones Fluidas & Nueva Experiencia"

    val categories: List<ReleaseCategory> = listOf(
        ReleaseCategory(
            categoryName = "Rendimiento & Optimización Extrema",
            icon = Icons.Default.Speed,
            items = listOf(
                ReleaseItem(
                    title = "Optimización general del sistema",
                    description = "Reducción drástica del uso de CPU, GPU, memoria RAM y consumo de batería para una autonomía prolongada.",
                    tag = "Core"
                ),
                ReleaseItem(
                    title = "Cero cuellos de botella",
                    description = "Eliminación completa de cuellos de botella en la interfaz y procesos en segundo plano, alcanzando 120 FPS estables.",
                    tag = "Fluidez"
                ),
                ReleaseItem(
                    title = "Desenfoque por carga real",
                    description = "Nuevo motor de desenfoque inteligente ultra liviano por carga real que sustituye los efectos de blur pesados.",
                    tag = "Motor Gráfico"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Diseño, Animaciones & Gestos",
            icon = Icons.Default.AutoAwesome,
            items = listOf(
                ReleaseItem(
                    title = "Transición fluida a mini-reproductor",
                    description = "Animación suave y natural al deslizar el reproductor hacia abajo, revelando el mini-reproductor nativo.",
                    tag = "Gestos"
                ),
                ReleaseItem(
                    title = "Menú contextual estilo Apple Music",
                    description = "Mantén presionado sobre canciones o álbumes en tu biblioteca para abrir el menú flotante con efecto bloom y tarjeta previa.",
                    tag = "Biblioteca"
                ),
                ReleaseItem(
                    title = "Navegación inferior optimizada",
                    description = "Animación del menú de navegación rediseñada para una respuesta instantánea y fluidez táctil.",
                    tag = "UI"
                ),
                ReleaseItem(
                    title = "Iconos modernizados & salida de audio",
                    description = "Iconos renovados en toda la app y nuevo icono personalizado para la selección de bocinas y dispositivos de sonido.",
                    tag = "Audio"
                ),
                ReleaseItem(
                    title = "Imágenes dinámicas de alta calidad",
                    description = "Fondos dinámicos actualizados con mayor nitidez, colores vivos y carga reactiva instantánea.",
                    tag = "Visual"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Búsqueda & Descubrimiento",
            icon = Icons.Default.Search,
            items = listOf(
                ReleaseItem(
                    title = "Historial de búsquedas",
                    description = "Guarda tus búsquedas recientes interactivas para volver a tus canciones, artistas y álbumes favoritos con un solo toque.",
                    tag = "Búsqueda"
                ),
                ReleaseItem(
                    title = "Catálogo de novedades renovado",
                    description = "Explora las últimas tendencias y lanzamientos musicales con un diseño moderno y dinámico.",
                    tag = "Novedades"
                ),
                ReleaseItem(
                    title = "Replay rediseñado",
                    description = "Revive lo mejor de tu año musical con una experiencia visual totalmente fresca.",
                    tag = "Estadísticas"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Biblioteca, Álbumes & Spotify",
            icon = Icons.Default.Album,
            items = listOf(
                ReleaseItem(
                    title = "Vistas de colección renovadas",
                    description = "Rediseño completo en las pantallas de Álbumes, Playlists y Canciones Favoritas para una navegación más limpia.",
                    tag = "Colección"
                ),
                ReleaseItem(
                    title = "Acceso directo 'Ir al álbum'",
                    description = "Navega directamente al álbum correspondiente desde el menú de opciones del reproductor.",
                    tag = "Navegación"
                ),
                ReleaseItem(
                    title = "Sincronización total con Spotify",
                    description = "Reconstrucción completa de la integración de cuentas Spotify para sincronizar playlists, favoritos y álbumes sin interrupciones.",
                    tag = "Spotify"
                )
            )
        ),
        ReleaseCategory(
            categoryName = "Reproducción, Audio & Letras",
            icon = Icons.Default.MusicNote,
            items = listOf(
                ReleaseItem(
                    title = "Letras en tiempo real sin retrasos",
                    description = "Sistema de letras reconstruido desde cero: sincronización ultra rápida, fluida y con cero retrasos mientras cantas.",
                    tag = "Karaoke"
                ),
                ReleaseItem(
                    title = "Reordenar canciones en la cola",
                    description = "Arrastra y organiza libremente el orden de reproducción a tu gusto personal.",
                    tag = "Cola"
                ),
                ReleaseItem(
                    title = "Automix inteligente",
                    description = "Transiciones continuas y mezclas armónicas entre pistas para una sesión musical ininterrumpida.",
                    tag = "Mezcla"
                ),
                ReleaseItem(
                    title = "Escuchar juntos",
                    description = "Disfruta de la música en tiempo real y comparte sesiones simultáneas con amigos.",
                    tag = "Social"
                ),
                ReleaseItem(
                    title = "Videos dinámicos de artistas",
                    description = "Fondos visuales en movimiento y videos dinámicos integrados en la vista de artistas.",
                    tag = "Artistas"
                )
            )
        )
    )

    val markdownChangelog: String = """
# 🚀 RayMusic v0.6.5 — Rendimiento Supremo & Nueva Experiencia Visual

¡Llegó la actualización más importante de **RayMusic**! En esta versión **v0.6.5**, hemos reconstruido los cimientos de la aplicación para ofrecer una fluidez inigualable, reducir el consumo de recursos al mínimo y presentar una experiencia estética y sonora de primer nivel.

---

### ⚡ Rendimiento & Optimización Extrema
* **Optimización integral del sistema**: Reducción sustancial del consumo de **CPU**, **GPU**, memoria **RAM** y ahorro inteligente de **batería**.
* **Eliminación de cuellos de botella**: Desaparición total de retrasos y bloqueos en el hilo principal de la interfaz; la aplicación ahora corre a 120 FPS consistentes.
* **Desenfoque por carga real ultra liviano**: Sustitución del desenfoque pesado tradicional por un nuevo algoritmo de desenfoque dinámico por carga real, mucho más rápido y ligero en cualquier dispositivo.

### 🎨 Diseño, Animaciones & Gestos
* **Transición fluida a mini-reproductor**: Nueva animación continua al deslizar el reproductor a pantalla completa hacia abajo, conectando de forma orgánica con el mini-reproductor nativo.
* **Menú contextual estilo Apple Music**: Mantén presionado cualquier elemento en tu biblioteca para abrir una previsualización flotante con difuminado dinámico y efecto bloom.
* **Navegación inferior optimizada**: Animaciones perfeccionadas en la barra de navegación flotante para una respuesta táctil inmediata.
* **Iconos renovados & selector de bocinas**: Rediseño de iconos en toda la aplicación y nuevo icono personalizado para la selección de salidas de audio y bocinas.
* **Imágenes dinámicas de alta resolución**: Carátulas y fondos visuales dinámicos actualizados con mayor fidelidad de color y nitidez.

### 🔍 Búsqueda & Exploración Musical
* **Historial de búsquedas inteligente**: Accede de inmediato a tus búsquedas previas para volver a tus canciones, artistas y discos con solo un toque.
* **Catálogo de Novedades renovado**: Nueva vista enriquecida para descubrir lanzamientos recientes, tendencias y recomendaciones exclusivas.
* **Replay interactivo rediseñado**: Explora tus estadísticas y momentos musicales más escuchados con una interfaz revitalizada.

### 📚 Biblioteca, Álbumes & Spotify
* **Vistas de colección renovadas**: Rediseño visual en Álbumes, Playlists y Canciones Favoritas.
* **Acceso directo al Álbum**: Nueva opción *"Ir al álbum"* directamente desde el reproductor para explorar la obra completa al instante.
* **Integración completa de cuentas Spotify**: Reimplementación total de la conexión con Spotify: sincronización fluida de listas, álbumes y pistas guardadas sin fallos.

### 🎵 Reproducción, Audio & Letras
* **Sistema de letras en tiempo real ultra rápido**: Motor de sincronización reconstruido desde cero. Cero retrasos y perfecta sincronización palabra por palabra al cantar.
* **Reordenamiento libre de la cola**: Arrastra y mueve canciones en la cola de reproducción exactamente en el orden que prefieras.
* **Automix inteligente**: Transiciones suaves y sin silencios entre canciones para una experiencia auditiva continua.
* **Escuchar juntos**: Conéctate y sincroniza tu música en tiempo real con amigos donde sea que estén.
* **Videos dinámicos en vista de artistas**: Fondos visuales animados y videos dinámicos integrados en las pantallas de artistas.

---

### 📦 Instalación & Actualización
1. Descarga el archivo `RayMusic-v0.6.5.apk` adjunto a este release.
2. Instala la actualización sobre tu versión actual (conservarás todas tus canciones descargadas, ajustes y listas).
3. ¡Disfruta de la mejor música con RayMusic!
""".trimIndent()
}
