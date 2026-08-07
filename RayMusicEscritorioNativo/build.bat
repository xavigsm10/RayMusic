@echo off
cd /d "%~dp0"
title Compilador RayMusic Escritorio (WebView2)

echo ===================================================
echo   Compilando RayMusicApp.exe (WebView2 Container)
echo ===================================================

call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" x64
if %ERRORLEVEL% NEQ 0 (
    echo [ERR] No se pudo inicializar el entorno de Visual Studio.
    exit /b 1
)

taskkill /f /im RayMusicApp.exe >nul 2>&1
taskkill /f /im RayMusicDesktop.exe >nul 2>&1
timeout /t 1 /nobreak >nul

rc.exe /fo src\resource.res src\resource.rc

cl.exe /EHsc /std:c++17 /O2 /Fe:RayMusicApp.exe src\main.cpp src\resource.res ^
    /I "webview2_sdk\build\native\include" ^
    /link "webview2_sdk\build\native\x64\WebView2Loader.dll.lib" ^
    User32.lib Gdi32.lib Shell32.lib Ole32.lib Dwmapi.lib ^
    /SUBSYSTEM:WINDOWS

if %ERRORLEVEL% NEQ 0 (
    echo [ERR] Error durante la compilacion.
    exit /b 1
)

copy /y "RayMusicApp.exe" "RayMusicDesktop.exe" > nul 2>&1
copy /y "RayMusicApp.exe" "RayMusicNativoApp.exe" > nul 2>&1

echo ===================================================
echo   COMPILACION EXITOSA: RayMusicApp.exe listo.
echo ===================================================
echo Ejecuta RayMusicApp.exe para abrir la app.
echo ===================================================
