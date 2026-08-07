#!/bin/bash
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
