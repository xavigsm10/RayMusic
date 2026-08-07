import os
import shutil
import zipfile

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
NATIVO_DIR = os.path.dirname(BASE_DIR)
OUTPUT_DIR = os.path.join(NATIVO_DIR, "dist_installers")

os.makedirs(OUTPUT_DIR, exist_ok=True)
win_dir = os.path.join(OUTPUT_DIR, "windows")
linux_dir = os.path.join(OUTPUT_DIR, "linux")
mac_dir = os.path.join(OUTPUT_DIR, "macos")

os.makedirs(win_dir, exist_ok=True)
os.makedirs(linux_dir, exist_ok=True)
os.makedirs(mac_dir, exist_ok=True)

# ----------------------------------------------------
# 1. WINDOWS INSTALLER PACKAGE
# ----------------------------------------------------
print("[1/3] Creando paquete instalador para Windows...")
win_payload = os.path.join(win_dir, "RayMusic_Windows_Files")
os.makedirs(win_payload, exist_ok=True)

# Copy EXE, DLL, src folder, icons
shutil.copy2(os.path.join(NATIVO_DIR, "RayMusicApp.exe"), win_payload)
shutil.copy2(os.path.join(NATIVO_DIR, "WebView2Loader.dll"), win_payload)
if os.path.exists(os.path.join(NATIVO_DIR, "src", "icon.ico")):
    shutil.copy2(os.path.join(NATIVO_DIR, "src", "icon.ico"), win_payload)
if os.path.exists(os.path.join(NATIVO_DIR, "src", "app_icon.png")):
    shutil.copy2(os.path.join(NATIVO_DIR, "src", "app_icon.png"), win_payload)

win_src_dest = os.path.join(win_payload, "src")
if os.path.exists(win_src_dest):
    shutil.rmtree(win_src_dest)
shutil.copytree(os.path.join(NATIVO_DIR, "src"), win_src_dest)

# Create Windows Installer Script (RayMusic_Installer_Windows.bat)
win_installer_bat = os.path.join(win_dir, "Instalar_RayMusic_Windows.bat")
with open(win_installer_bat, "w", encoding="utf-8") as f:
    f.write('''@echo off
title Instalador de RayMusic para Windows
color 0A
echo ========================================================
echo         Instalador Oficial de RayMusic para Windows
echo ========================================================
echo.

set "TARGET_DIR=%LOCALAPPDATA%\\Programs\\RayMusic"
echo [1/4] Creando directorio de instalacion en %TARGET_DIR%...
if not exist "%TARGET_DIR%" mkdir "%TARGET_DIR%"

echo [2/4] Copiando archivos del programa...
xcopy /E /I /Y "%~dp0RayMusic_Windows_Files\\*" "%TARGET_DIR%\\" > nul

echo [3/4] Creando accesos directos en Escritorio y Menu Inicio...
powershell -Command "$s=(New-Object -COM WScript.Shell).CreateShortcut('%USERPROFILE%\\Desktop\\RayMusic.lnk');$s.TargetPath='%TARGET_DIR%\\RayMusicApp.exe';$s.WorkingDirectory='%TARGET_DIR%';$s.IconLocation='%TARGET_DIR%\\src\\icon.ico';$s.Save()"
powershell -Command "$s=(New-Object -COM WScript.Shell).CreateShortcut('%APPDATA%\\Microsoft\\Windows\\Start Menu\\Programs\\RayMusic.lnk');$s.TargetPath='%TARGET_DIR%\\RayMusicApp.exe';$s.WorkingDirectory='%TARGET_DIR%';$s.IconLocation='%TARGET_DIR%\\src\\icon.ico';$s.Save()"

echo [4/4] Registrando aplicacion en Windows...
reg add "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\RayMusicApp.exe" /ve /t REG_SZ /d "%TARGET_DIR%\\RayMusicApp.exe" /f > nul

echo.
echo ========================================================
echo     INSTALACION COMPLETADA EXITOSAMENTE EN WINDOWS
echo ========================================================
echo Acceso directo creado en el Escritorio y Menu Inicio.
echo.
pause
''')

# Create Inno Setup script (installer_windows.iss)
iss_file = os.path.join(win_dir, "installer_windows.iss")
with open(iss_file, "w", encoding="utf-8") as f:
    f.write(f'''[Setup]
AppName=RayMusic
AppVersion=1.0.0
DefaultDirName={{autopf}}\\RayMusic
DefaultGroupName=RayMusic
UninstallDisplayIcon={{app}}\\src\\icon.ico
Compression=lzma2/ultra64
SolidCompression=yes
OutputDir={win_dir}
OutputBaseFilename=RayMusic_Setup_Windows
SetupIconFile={os.path.join(NATIVO_DIR, "src", "icon.ico")}

[Files]
Source: "{win_payload}\\*"; DestDir: "{{app}}"; Flags: recursesubdirs createallsubdirs

[Icons]
Name: "{{group}}\\RayMusic"; Filename: "{{app}}\\RayMusicApp.exe"; IconFilename: "{{app}}\\src\\icon.ico"
Name: "{{autodesktop}}\\RayMusic"; Filename: "{{app}}\\RayMusicApp.exe"; IconFilename: "{{app}}\\src\\icon.ico"

[Run]
Filename: "{{app}}\\RayMusicApp.exe"; Description: "Ejecutar RayMusic ahora"; Flags: postinstall nowait skipifsilent
''')

# Zip Windows Installer Bundle
win_zip = os.path.join(OUTPUT_DIR, "RayMusic_Installer_Windows.zip")
with zipfile.ZipFile(win_zip, 'w', zipfile.ZIP_DEFLATED) as zipf:
    for root, dirs, files in os.walk(win_dir):
        for file in files:
            abs_p = os.path.join(root, file)
            rel_p = os.path.relpath(abs_p, win_dir)
            zipf.write(abs_p, os.path.join("RayMusic_Windows_Installer", rel_p))

print(" -> Paquete de Windows listo:", win_zip)


# ----------------------------------------------------
# 2. LINUX INSTALLER PACKAGE
# ----------------------------------------------------
print("[2/3] Creando paquete instalador para Linux...")
linux_payload = os.path.join(linux_dir, "RayMusic_Linux_Files")
os.makedirs(linux_payload, exist_ok=True)

# Copy Web files and Assets
linux_src_dest = os.path.join(linux_payload, "src")
shutil.copytree(os.path.join(NATIVO_DIR, "src"), linux_src_dest, dirs_exist_ok=True)

# Create Linux launcher script (raymusic-launcher.sh)
linux_launcher = os.path.join(linux_payload, "raymusic-launcher")
with open(linux_launcher, "w", encoding="utf-8") as f:
    f.write('''#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
if command -v google-chrome &> /dev/null; then
    google-chrome --app="file://$DIR/src/index.html" --user-data-dir="$HOME/.config/RayMusic" --window-size=1280,800
elif command -v chromium &> /dev/null; then
    chromium --app="file://$DIR/src/index.html" --user-data-dir="$HOME/.config/RayMusic" --window-size=1280,800
elif command -v chromium-browser &> /dev/null; then
    chromium-browser --app="file://$DIR/src/index.html" --user-data-dir="$HOME/.config/RayMusic" --window-size=1280,800
else
    xdg-open "$DIR/src/index.html"
fi
''')
os.chmod(linux_launcher, 0o755)

# Create Desktop Entry file (raymusic.desktop)
desktop_file = os.path.join(linux_payload, "raymusic.desktop")
with open(desktop_file, "w", encoding="utf-8") as f:
    f.write('''[Desktop Entry]
Name=RayMusic
Comment=Reproductor de Música Premium RayMusic
Exec=/opt/raymusic/raymusic-launcher
Icon=/opt/raymusic/src/app_icon.png
Terminal=false
Type=Application
Categories=AudioVideo;Player;Audio;Music;
''')

# Create Linux Installer Script (Instalar_RayMusic_Linux.sh)
linux_installer_sh = os.path.join(linux_dir, "Instalar_RayMusic_Linux.sh")
with open(linux_installer_sh, "w", encoding="utf-8") as f:
    f.write('''#!/bin/bash
echo "========================================================"
echo "         Instalador Oficial de RayMusic para Linux"
echo "========================================================"
echo ""

TARGET_DIR="/opt/raymusic"
echo "[1/3] Instalando archivos del sistema en $TARGET_DIR..."
sudo mkdir -p "$TARGET_DIR"
sudo cp -r "$(dirname "$0")/RayMusic_Linux_Files/"* "$TARGET_DIR/"
sudo chmod +x "$TARGET_DIR/raymusic-launcher"

echo "[2/3] Instalando icono y acceso directo de escritorio..."
sudo cp "$TARGET_DIR/raymusic.desktop" /usr/share/applications/
sudo mkdir -p /usr/share/icons/hicolor/256x256/apps/
sudo cp "$TARGET_DIR/src/app_icon.png" /usr/share/icons/hicolor/256x256/apps/raymusic.png

echo "[3/3] Creando enlace simbolico ejecutable en /usr/local/bin/raymusic..."
sudo ln -sf "$TARGET_DIR/raymusic-launcher" /usr/local/bin/raymusic

echo ""
echo "========================================================"
echo "     INSTALACION COMPLETADA EXITOSAMENTE EN LINUX"
echo "========================================================"
echo "Puedes ejecutar RayMusic desde el menu de aplicaciones o escribiendo 'raymusic'."
''')
os.chmod(linux_installer_sh, 0o755)

# Zip Linux Package
linux_tar = os.path.join(OUTPUT_DIR, "RayMusic_Installer_Linux.tar.gz")
with zipfile.ZipFile(os.path.join(OUTPUT_DIR, "RayMusic_Installer_Linux.zip"), 'w', zipfile.ZIP_DEFLATED) as zipf:
    for root, dirs, files in os.walk(linux_dir):
        for file in files:
            abs_p = os.path.join(root, file)
            rel_p = os.path.relpath(abs_p, linux_dir)
            zipf.write(abs_p, os.path.join("RayMusic_Linux_Installer", rel_p))

print(" -> Paquete de Linux listo:", os.path.join(OUTPUT_DIR, "RayMusic_Installer_Linux.zip"))


# ----------------------------------------------------
# 3. macOS INSTALLER PACKAGE
# ----------------------------------------------------
print("[3/3] Creando paquete instalador para macOS...")
mac_app_bundle = os.path.join(mac_dir, "RayMusic.app")
mac_contents = os.path.join(mac_app_bundle, "Contents")
mac_macos = os.path.join(mac_contents, "MacOS")
mac_resources = os.path.join(mac_contents, "Resources")

os.makedirs(mac_macos, exist_ok=True)
os.makedirs(mac_resources, exist_ok=True)

# Copy Web Assets into Mac Resources
shutil.copytree(os.path.join(NATIVO_DIR, "src"), os.path.join(mac_resources, "src"), dirs_exist_ok=True)
if os.path.exists(os.path.join(NATIVO_DIR, "src", "app_icon.png")):
    shutil.copy2(os.path.join(NATIVO_DIR, "src", "app_icon.png"), os.path.join(mac_resources, "AppIcon.png"))

# Create Info.plist for macOS
plist_file = os.path.join(mac_contents, "Info.plist")
with open(plist_file, "w", encoding="utf-8") as f:
    f.write('''<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>RayMusic</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon.png</string>
    <key>CFBundleIdentifier</key>
    <string>com.mrtdk.raymusic</string>
    <key>CFBundleName</key>
    <string>RayMusic</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0.0</string>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
''')

# Create macOS launcher script inside Contents/MacOS/RayMusic
mac_executable = os.path.join(mac_macos, "RayMusic")
with open(mac_executable, "w", encoding="utf-8") as f:
    f.write('''#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
RESOURCES="$DIR/../Resources"
open -a "Google Chrome" --args --app="file://$RESOURCES/src/index.html" --user-data-dir="$HOME/Library/Application Support/RayMusic" || open -a "Safari" "$RESOURCES/src/index.html"
''')
os.chmod(mac_executable, 0o755)

# Create macOS Installer Command Script (Instalar_RayMusic_macOS.command)
mac_installer_cmd = os.path.join(mac_dir, "Instalar_RayMusic_macOS.command")
with open(mac_installer_cmd, "w", encoding="utf-8") as f:
    f.write('''#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "========================================================"
echo "         Instalador Oficial de RayMusic para macOS"
echo "========================================================"
echo ""
echo "[1/2] Copiando RayMusic.app a /Applications..."
cp -R "$DIR/RayMusic.app" /Applications/

echo "[2/2] Registrando aplicacion en LaunchServices..."
xattr -cr /Applications/RayMusic.app 2>/dev/null

echo ""
echo "========================================================"
echo "     INSTALACION COMPLETADA EXITOSAMENTE EN macOS"
echo "========================================================"
echo "Puedes abrir RayMusic desde la carpeta Aplicaciones o el Launchpad."
''')
os.chmod(mac_installer_cmd, 0o755)

# Zip macOS Package
mac_zip = os.path.join(OUTPUT_DIR, "RayMusic_Installer_macOS.zip")
with zipfile.ZipFile(mac_zip, 'w', zipfile.ZIP_DEFLATED) as zipf:
    for root, dirs, files in os.walk(mac_dir):
        for file in files:
            abs_p = os.path.join(root, file)
            rel_p = os.path.relpath(abs_p, mac_dir)
            zipf.write(abs_p, os.path.join("RayMusic_macOS_Installer", rel_p))

print(" -> Paquete de macOS listo:", mac_zip)

print("\n=======================================================")
print("  TODOS LOS INSTALADORES FUERON GENERADOS EN:")
print(" ", OUTPUT_DIR)
print("=======================================================")
