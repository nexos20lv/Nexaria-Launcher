#!/bin/bash
# Script de setup NEXORA Launcher

echo "🚀 Setup NEXORA Launcher"
echo "========================"

# Vérifier Java
if ! command -v java &> /dev/null; then
    echo "❌ Java n'est pas installé!"
    echo "Installez Java 11+ depuis https://adoptium.net"
    exit 1
fi

echo "✓ Java détecté: $(java -version 2>&1 | head -n 1)"

# Créer config.yml s'il n'existe pas
if [ ! -f "config.yml" ]; then
    echo ""
    echo "📝 Création de config.yml..."
    cp config.example.yml config.yml
    echo "✓ config.yml créé - modifiez-le avec vos paramètres"
else
    echo "✓ config.yml trouvé"
fi

# Créer les dossiers
mkdir -p data/mods
mkdir -p data/configs

echo "✓ Dossiers créés"

# Compiler le launcher
echo ""
echo "🔨 Compilation du launcher..."
mvn clean package -q
if [ $? -eq 0 ]; then
    echo "✓ Compilation réussie!"
else
    echo "❌ Erreur lors de la compilation"
    exit 1
fi

echo ""
echo "✅ Setup terminé!"
echo ""
echo "Prochaines étapes:"
echo "1. Éditez config.yml avec vos paramètres"
echo "2. Ajoutez des mods dans data/mods/ (optionnel)"
echo "3. Lancez: java -jar target/nexora-launcher.jar"
