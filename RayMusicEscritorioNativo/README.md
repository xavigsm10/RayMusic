# RayMusic Escritorio (Versión C++)

Esta es la reconstrucción de la versión de escritorio de **RayMusic** desde cero utilizando **C++ y WebView2**.

## Estructura del Proyecto

*   **`src/main.cpp`**: Contiene la lógica del contenedor nativo Windows (Win32) y la inicialización de Edge WebView2, además de la comunicación por mensajes para maximizar, minimizar, arrastrar y cerrar la ventana borderless.
*   **`src/index.html`**: Estructura visual de la interfaz inspirada en Apple Music de macOS.
*   **`src/index.css`**: Estilos visuales con un diseño oscuro premium, bordes redondeados y efectos glassmorphism.
*   **`src/index.js`**: Gestor de reproducción de audio, interacción con la API de YouTube Music (InnerTube), cola de reproducción, sugerencias y búsquedas.
*   **`build.bat`**: Script automatizado para configurar el compilador MSVC y generar el archivo ejecutable (`RayMusicApp.exe`).

## Compilación

Para compilar la aplicación, abre una consola de PowerShell o CMD en la carpeta `RayMusicEscritorio` y ejecuta:

```cmd
build.bat
```

Una vez finalizada la compilación, se generará `RayMusicApp.exe` en esta carpeta. Ejecútalo para abrir la aplicación.
