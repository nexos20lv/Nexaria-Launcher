@echo off
REM Script de setup NEXORA Launcher pour Windows

echo 🚀 Setup NEXORA Launcher
echo ========================

REM Vérifier Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Java n'est pas installé!
    echo Installez Java 11+ depuis https://adoptium.net
    pause
    exit /b 1
)

for /f "tokens=*" %%i in ('java -version 2^>^&1 ^| findstr /r "version"') do echo ✓ %%i

REM Créer config.yml s'il n'existe pas
if not exist "config.yml" (
    echo.
    echo 📝 Création de config.yml...
    copy config.example.yml config.yml
    echo ✓ config.yml créé - modifiez-le avec vos paramètres
) else (
    echo ✓ config.yml trouvé
)

REM Créer les dossiers
if not exist "data\mods" mkdir data\mods
if not exist "data\configs" mkdir data\configs
echo ✓ Dossiers créés

REM Compiler le launcher
echo.
echo 🔨 Compilation du launcher...
call mvn clean package -q
if errorlevel 1 (
    echo ❌ Erreur lors de la compilation
    pause
    exit /b 1
)
echo ✓ Compilation réussie!

echo.
echo ✅ Setup terminé!
echo.
echo Prochaines étapes:
echo 1. Éditez config.yml avec vos paramètres
echo 2. Ajoutez des mods dans data\mods\ (optionnel)
echo 3. Lancez: java -jar target\nexora-launcher.jar
echo.
pause
