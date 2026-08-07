#!/bin/bash
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
