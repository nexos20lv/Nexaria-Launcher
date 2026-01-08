# 🚀 NEXARIA Launcher

## 📋 Vue d'ensemble

**NEXARIA** est un launcher Minecraft moderne avec une interface graphique élégante et des icônes FontAwesome 5:
- ✅ Auto-update via GitHub Releases
- ✅ Configuration simple via YAML (`config.yml`)
- ✅ Authentification Azuriom (AzAuth)
- ✅ Dossier `data/` pour mods/configs locaux
- ✅ Zéro serveur externe nécessaire
- 🎨 Interface moderne avec icônes FontAwesome
- 🖼️ Design glassmorphism avec effets visuels

---

## 📦 Prérequis

- **Java 11+** recommandé (Temurin, Adoptium)
- **Maven 3.6+** (ou Maven Wrapper)
- **Git** (pour CI/CD et releases)

## 🎨 Bibliothèques UI

Le launcher utilise des bibliothèques modernes pour une interface élégante:
- **FlatLaf 3.2.1** - Look & Feel moderne pour Swing
- **Ikonli 12.3.1** - Intégration FontAwesome 5 (2000+ icônes)
- **FontAwesome 5** - Icônes vectorielles dans toute l'interface

## 🎯 Installation Rapide

### 1) Configurer `config.yml`

Copiez `config.example.yml` → `config.yml` et modifiez:

```yaml
# Votre serveur Azuriom
azuriomUrl: https://my-server.azuriom.com

# Votre repo GitHub pour auto-update
githubRepo: username/nexaria

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
java -jar target/nexaria-launcher-1.0.0.jar
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

~/.nexaria/ (home user)    # Auto-créé
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
githubRepo: username/nexaria

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

### 🎨 Interface Moderne
- **Design glassmorphism** avec effets de transparence
- **Icônes FontAwesome 5** partout dans l'interface:
  - 🏠 Navigation avec icônes home/settings
  - 🎮 Bouton jouer avec icône play
  - ⚙️ Paramètres avec icônes contextuelles
  - 🔒 Authentification 2FA avec icône shield
  - 📊 Statut serveur avec icône server
- **Effets visuels** avec particules et dégradés
- **Thème sombre élégant** optimisé pour le gaming

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
- Support de tous les loaders populaires (Forge, Fabric, NeoForge, Quilt)

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
java -jar target/nexaria-launcher-1.0.0.jar
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

### ❌ Erreur: ClassNotFoundException: cpw.mods.bootstraplauncher.BootstrapLauncher

Cette erreur survient avec **Forge 1.20.1+** et indique un problème de bibliothèques de lancement.

**Causes possibles:**
1. **Bibliothèques Forge manquantes** - Les librairies Forge ne sont pas correctement téléchargées
2. **Profil installer.json corrompu** - Le profil d'installation Forge est incomplet
3. **Cache corrompu** - Les fichiers en cache sont invalides
4. **FlowUpdater incomplet** - L'installation de Forge n'a pas terminé correctement

**✅ Solutions (dans l'ordre):**

#### Solution 1: Vider le cache et réinstaller (RECOMMANDÉ)
```bash
# Supprimer le cache launcher et les versions
rm -rf ~/.nexaria/cache
rm -rf ~/.nexaria/versions
rm -rf ~/.nexaria/libraries

# Relancer le launcher (il retéléchargera tout)
java -jar nexaria-launcher-1.0.0.jar
```

#### Solution 2: Vérifier l'installation Forge manuellement
```bash
# Vérifier que les librairies Forge Bootstrap sont présentes
ls -la ~/.nexaria/libraries/cpw/mods/bootstraplauncher/
ls -la ~/.nexaria/libraries/cpw/mods/securejarhandler/

# Si elles manquent, c'est que FlowUpdater n'a pas terminé l'installation
# → Retour à Solution 1
```

#### Solution 3: Passer à une version Forge compatible
Dans `config.yml`, essayez une version Forge plus récente ou plus ancienne:
```yaml
# Forge 1.20.1 - Versions testées
loaderVersion: 47.3.0  # Plus récent
# ou
loaderVersion: 47.1.0  # Plus ancien
```

#### Solution 4: Utiliser un loader alternatif
Si le problème persiste avec Forge, essayez **Fabric** ou **NeoForge**:
```yaml
# Fabric (plus léger et stable)
loader: fabric
loaderVersion: 0.15.11

# OU NeoForge (fork moderne de Forge)
loader: neoforge
loaderVersion: 20.1.0
```

#### Solution 5: Mode débogage pour diagnostic complet
Activez les logs détaillés dans `config.yml`:
```yaml
debugMode: true
```
Puis relancez et consultez les logs dans la console. Recherchez:
- ⚠️ Erreurs de téléchargement de bibliothèques
- ⚠️ Timeouts réseau
- ⚠️ Permissions fichiers

**🔍 Informations techniques:**

L'erreur `cpw.mods.bootstraplauncher.BootstrapLauncher` signifie que Forge moderne (1.17+) utilise un nouveau système de chargement qui nécessite:
1. **bootstraplauncher** - Lance le bootstrap Forge
2. **securejarhandler** - Gère les JARs signés et sécurisés
3. **Profil Forge complet** - Généré par l'installateur Forge

Le launcher utilise **FlowUpdater** qui automatise ce processus. Si l'installation ne termine pas correctement (réseau lent, interruption), ces fichiers critiques peuvent manquer.

**📝 Note importante:** Cette erreur n'est **PAS** un bug du launcher, mais un problème d'installation incomplète de Forge. FlowUpdater doit terminer complètement l'étape `MOD_LOADER` avant le lancement.

---

## 📞 Support

Consultez les logs pour plus de détails:
```
debugMode: true
```

Les logs sont affichés dans la console lors du lancement.

---

**Bon gaming!** 🎮🚀
