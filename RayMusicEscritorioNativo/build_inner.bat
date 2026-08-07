@echo off
cd /d "%~dp0"

:: 1. Setup the MSVC Compiler Environment (64-bit) in a FRESH process
echo [1/4] Configurando el entorno de compilacion de Visual Studio...
call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" x64
if %ERRORLEVEL% NEQ 0 (
    echo [ERR] No se pudo inicializar el entorno de Visual Studio.
    exit /b 1
)

:: 2. Verify WebView2 SDK is present
if not exist "webview2_sdk\build\native\include\WebView2.h" (
    echo [ERR] El SDK de WebView2 no se encontro o esta incompleto.
    echo Intentando volver a descargar el SDK...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://www.nuget.org/api/v2/package/Microsoft.Web.WebView2/1.0.2592.51' -OutFile 'webview2.zip'; Expand-Archive -Path 'webview2.zip' -DestinationPath 'webview2_sdk' -Force"
    if not exist "webview2_sdk\build\native\include\WebView2.h" (
        echo [ERR] No se pudo instalar el SDK de WebView2. Verifica tu conexion a internet.
        exit /b 1
    )
)
echo [2/4] SDK de WebView2 verificado y listo.

:: 3. Compile the C++ program using cl.exe
echo [3/4] Compilando codigo fuente C++...

:: Kill any running instance gracefully before compiling
taskkill /f /im RayMusicApp.exe >nul 2>&1
timeout /t 1 /nobreak >nul

rc.exe /fo src\resources.res src\resources.rc

cl.exe /EHsc /std:c++17 /O2 /Fe:RayMusicApp.exe src\main.cpp src\resources.res ^
    /I "webview2_sdk\build\native\include" ^
    /link "webview2_sdk\build\native\x64\WebView2Loader.dll.lib" ^
    User32.lib Shell32.lib Ole32.lib Gdi32.lib Dwmapi.lib ^
    /SUBSYSTEM:WINDOWS

if %ERRORLEVEL% NEQ 0 (
    echo [ERR] Error durante la compilacion de cl.exe.
    exit /b 1
)

:: 4. Copy the WebView2Loader.dll dependency alongside the executable
echo [4/4] Copiando dependencias...
copy /y "webview2_sdk\build\native\x64\WebView2Loader.dll" "WebView2Loader.dll" > nul
copy /y "..\app\src\main\assets\novedades_apple.json" "src\novedades_apple.json" > nul 2>&1

echo ===================================================
echo   COMPILACION EXITOSA: RayMusicApp.exe listo.
echo ===================================================
echo Ejecuta RayMusicApp.exe para abrir la app.
echo ===================================================
