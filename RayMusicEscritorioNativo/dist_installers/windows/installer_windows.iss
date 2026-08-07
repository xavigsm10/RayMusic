[Setup]
AppName=RayMusic
AppVersion=1.0.0
DefaultDirName={autopf}\RayMusic
DefaultGroupName=RayMusic
UninstallDisplayIcon={app}\src\icon.ico
Compression=lzma2/ultra64
SolidCompression=yes
OutputDir=C:\Users\Xavi\Documents\RayMusic\RayMusicEscritorioNativo\dist_installers\windows
OutputBaseFilename=RayMusic_Setup_Windows
SetupIconFile=C:\Users\Xavi\Documents\RayMusic\RayMusicEscritorioNativo\src\icon.ico

[Files]
Source: "C:\Users\Xavi\Documents\RayMusic\RayMusicEscritorioNativo\dist_installers\windows\RayMusic_Windows_Files\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs

[Icons]
Name: "{group}\RayMusic"; Filename: "{app}\RayMusicApp.exe"; IconFilename: "{app}\src\icon.ico"
Name: "{autodesktop}\RayMusic"; Filename: "{app}\RayMusicApp.exe"; IconFilename: "{app}\src\icon.ico"

[Run]
Filename: "{app}\RayMusicApp.exe"; Description: "Ejecutar RayMusic ahora"; Flags: postinstall nowait skipifsilent
