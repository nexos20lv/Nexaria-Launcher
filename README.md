# 🚀 NEXORA Launcher

## 📋 Vue d'ensemble

**NEXORA** est un launcher Minecraft entièrement standalone avec:
- ✅ Auto-update via GitHub Releases
- ✅ Configuration simple via YAML (`config.yml`)
- ✅ Authentification Azuriom (AzAuth)
- ✅ Dossier `data/` pour mods/configs locaux
- ✅ Zéro serveur externe nécessaire

---

## 📦 Prérequis

- Java 11+ recommandé (Temurin)
- Maven 3.6+ (ou Maven Wrapper)
- Git (pour CI/CD et releases)

## 🎯 Installation Rapide

### 1) Configurer `config.yml`

Copiez `config.example.yml` → `config.yml` et modifiez:

```yaml
# Votre serveur Azuriom
azuriomUrl: https://my-server.azuriom.com

# Votre repo GitHub pour auto-update
githubRepo: username/nexora

# Version Minecraft & Loader
minecraftVersion: 1.20.1
loader: forge
loaderVersion: 47.2.0

# RAM à allouer
minMemory: 1024
maxMemory: 4096

# Options
autoUpdate: true
debugMode: false
language: fr
```

### 2) Ajouter des mods/configs localement

Mettez des fichiers `.jar` dans le dossier `data/mods/` :

```
data/
├── mods/
│   ├── jade-11.9.3.jar
│   └── jei-15.2.0.jar
└── configs/
  └── jade.cfg
```

Les fichiers locaux sont **automatiquement synchronisés** au démarrage.

### 3) Compiler et lancer

```bash
mvn clean package
java -jar target/nexora-launcher.jar
```

---

## 🔄 CI/CD et Auto-Update via GitHub

### 1) Créer une Release

```bash
# Tagger une version
git tag v1.0.1
git push origin v1.0.1

# GitHub Actions build automatiquement le JAR
```

### 2) Le launcher s'update tout seul

- Au démarrage, vérifie la dernière release
- Télécharge si nouvelle version disponible
- Redémarre automatiquement

---

## 🧱 Architecture & Dossiers

```
config.yml                 # Configuration principale
config.example.yml         # Exemple de config
data/
├── mods/                 # Mods locaux (optionnel)
└── configs/              # Configs locales (optionnel)
src/main/java/...
pom.xml

~/.nexora/ (home user)    # Auto-créé
├── mods/                 # Mods synchronisés
├── configs/              # Configs synchronisées
├── launcher/             # Versions du launcher
├── cache/                # Cache des téléchargements
└── versions/             # Info des versions
```

---

## 🔐 Générer des SHA256

Pour chaque fichier, générez le hash:

**Windows PowerShell:**
```powershell
(Get-FileHash "chemin\vers\fichier.jar" -Algorithm SHA256).Hash
```

**Linux/Mac:**
```bash
sha256sum chemin/vers/fichier.jar
```

---

## ⚙️ Configuration complète (`config.yml`)

```yaml
# ===== Authentification =====
azuriomUrl: https://your-azuriom.com

# ===== GitHub (Auto-update) =====
githubRepo: username/nexora

# ===== Minecraft =====
minecraftVersion: 1.20.1
loader: forge              # forge, fabric, quilt, neoforge
loaderVersion: 47.2.0

# ===== Mémoire =====
minMemory: 512
maxMemory: 2048
jvmArgs: -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# ===== Launcher =====
launcherVersion: 1.0.0
autoUpdate: true
debugMode: false           # Logs détaillés
language: fr               # fr, en

# ===== Dossiers =====
dataFolder: data           # Dossier local des fichiers
modsSubfolder: mods
configsSubfolder: configs

# ===== Vérifications =====
verifyIntegrity: true      # Vérifier SHA256
cleanupOldMods: true       # Supprimer anciens mods
autoLaunchGame: false      # Auto-lancer le jeu

# ===== Téléchargement =====
downloadTimeout: 30        # Secondes
```

---

## ✨ Fonctionnalités

### Auto-Update
- Vérifie les releases GitHub au démarrage
- Télécharge automatiquement les nouvelles versions
- Redémarre pour appliquer la mise à jour

### Gestion des Mods/Configs locaux
- Copie depuis `data/mods` et `data/configs`
- Option de nettoyage des mods obsolètes
- Vérification d'intégrité (SHA-256) optionnelle

### Configuration Flexible
- Fichier YAML simple à modifier
- Valeurs par défaut intelligentes
- Support de tous les loaders populaires

### Dossier Local `data/`
- Mods/configs du dossier `data/` copiés automatiquement
- Pratique pour tester localement
- Synchronisation avec GitHub

---

## 🛠️ Compilation et Exécution

### Build
```bash
mvn clean package
```

### Run
```bash
# Assurez-vous que config.yml est présent
java -jar target/nexora-launcher.jar
```

---

## 🐛 Dépannage

### Config non trouvée
→ Copier `config.example.yml` vers `config.yml` et configurer

### Mods ne se téléchargent pas
→ Vérifier que vos fichiers locaux sont bien dans `data/`

### Hash mismatch
→ Recalculer le SHA256 du fichier si nécessaire

### Launcher ne s'update pas
→ Vérifier que la Release a un asset `.jar`
→ Vérifier le tag suit le format `v1.0.0`

---

## 📞 Support

Consultez les logs pour plus de détails:
```
debugMode: true
```

Les logs sont affichés dans la console lors du lancement.

---

**Bon gaming!** 🎮🚀
