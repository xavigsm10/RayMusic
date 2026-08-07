@echo off
title Instalador de RayMusic para Windows
color 0A
echo ========================================================
echo         Instalador Oficial de RayMusic para Windows
echo ========================================================
echo.

set "TARGET_DIR=%LOCALAPPDATA%\Programs\RayMusic"
echo [1/4] Creando directorio de instalacion en %TARGET_DIR%...
if not exist "%TARGET_DIR%" mkdir "%TARGET_DIR%"

echo [2/4] Copiando archivos del programa...
xcopy /E /I /Y "%~dp0RayMusic_Windows_Files\*" "%TARGET_DIR%\" > nul

echo [3/4] Creando accesos directos en Escritorio y Menu Inicio...
powershell -Command "$s=(New-Object -COM WScript.Shell).CreateShortcut('%USERPROFILE%\Desktop\RayMusic.lnk');$s.TargetPath='%TARGET_DIR%\RayMusicApp.exe';$s.WorkingDirectory='%TARGET_DIR%';$s.IconLocation='%TARGET_DIR%\src\icon.ico';$s.Save()"
powershell -Command "$s=(New-Object -COM WScript.Shell).CreateShortcut('%APPDATA%\Microsoft\Windows\Start Menu\Programs\RayMusic.lnk');$s.TargetPath='%TARGET_DIR%\RayMusicApp.exe';$s.WorkingDirectory='%TARGET_DIR%';$s.IconLocation='%TARGET_DIR%\src\icon.ico';$s.Save()"

echo [4/4] Registrando aplicacion en Windows...
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\App Paths\RayMusicApp.exe" /ve /t REG_SZ /d "%TARGET_DIR%\RayMusicApp.exe" /f > nul

echo.
echo ========================================================
echo     INSTALACION COMPLETADA EXITOSAMENTE EN WINDOWS
echo ========================================================
echo Acceso directo creado en el Escritorio y Menu Inicio.
echo.
pause
