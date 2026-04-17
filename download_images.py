import os
import json
import urllib.request

# Configuración
JSON_FILE = 'datos.json'
DEST_DIR = 'img'

def download_images():
    if not os.path.exists(DEST_DIR):
        os.makedirs(DEST_DIR)
        print(f"Directorio creado: {os.path.abspath(DEST_DIR)}")

    if not os.path.exists(JSON_FILE):
        print(f"Error: No se encuentra el archivo {JSON_FILE}.")
        return

    try:
        with open(JSON_FILE, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except Exception as e:
        print(f"Error al leer {JSON_FILE}: {e}")
        return

    print(f"Iniciando descarga de {len(data)} imágenes...")

    success_count = 0
    for item in data:
        name = item.get('name', 'imagen')
        url = item.get('url')
        
        if not url: continue

        clean_name = "".join([c for c in name if c.isalnum() or c in (' ', '-', '_')]).strip()
        filename = f"{clean_name}.webp"
        filepath = os.path.join(DEST_DIR, filename)

        try:
            print(f"Descargando: {filename}...", end='\r')
            # Usar urllib.request en lugar de requests
            with urllib.request.urlopen(url, timeout=15) as response:
                with open(filepath, 'wb') as out_file:
                    out_file.write(response.read())
                success_count += 1
        except Exception as e:
            print(f"\nError con {name}: {str(e)}")

    print(f"\n¡Listo! {success_count} imágenes guardadas en la carpeta 'img'.")

if __name__ == "__main__":
    download_images()
