# 🚀 Nexaria Launcher

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/nexos20lv/nexaria-launcher)](https://github.com/nexos20lv/nexaria-launcher/releases/latest)

**Launcher Minecraft moderne** avec interface élégante, auto-update, authentification Azuriom et gestion automatique des mods via GitHub.

![Nexaria Launcher](https://via.placeholder.com/800x400/6B46C1/FFFFFF?text=Nexaria+Launcher)

---

## ✨ Fonctionnalités

### 🎮 **Gestion du jeu**
- ✅ Support **Forge, Fabric, NeoForge** et Vanilla
- ✅ Installation automatique de Minecraft et des loaders
- ✅ Synchronisation des **mods** et **configs** depuis GitHub
- ✅ Auto-update du launcher via GitHub Releases
- ✅ Minimisation automatique au lancement du jeu
- ✅ Restauration du launcher à la fermeture du jeu

### 🔐 **Authentification**
- ✅ Intégration **Azuriom** (AzAuth)
- ✅ Système "Se souvenir de moi"
- ✅ Upload de skin Minecraft
- ✅ Profil utilisateur avec avatar

### 🎨 **Interface moderne**
- ✅ Design **Glassmorphism** avec particules animées
- ✅ Thème violet élégant avec dégradés
- ✅ Icônes **FontAwesome 5** (2000+ icônes)
- ✅ **FlatLaf** Look & Feel moderne
- ✅ Actualités RSS du serveur
- ✅ Statut serveur en temps réel
- ✅ Responsive et fluide

### ⚙️ **Configuration**
- ✅ Allocation RAM dynamique (slider)
- ✅ Arguments JVM personnalisables
- ✅ Choix du dossier de jeu
- ✅ Mode debug avec logs détaillés
- ✅ Limite de débit de téléchargement
- ✅ Export des logs pour support

---

## 📥 Installation

### **Pour les utilisateurs**
Consultez le [**Guide d'installation complet**](INSTALLATION.md) pour Windows, macOS et Linux.

**Résumé rapide :**
1. **Installer Java 21** : https://adoptium.net
2. **Télécharger** le launcher : [Dernière version](https://github.com/nexos20lv/nexaria-launcher/releases/latest)
3. **Lancer** : `java -jar nexaria-launcher.jar`

---

## 🛠️ Développement

### **Prérequis**
- **JDK 21** (Temurin recommandé)
- **Maven 3.6+**
- **Git**

### **Compilation**

```bash
# Cloner le projet
git clone https://github.com/nexos20lv/nexaria-launcher.git
cd nexaria-launcher

# Compiler
mvn clean package

# Lancer
java -jar target/nexaria-launcher.jar
```

### **Structure du projet**

```
nexaria-launcher/
├── src/main/java/com/nexaria/launcher/
│   ├── auth/              # Authentification Azuriom
│   ├── config/            # Configuration YAML
│   ├── downloader/        # Gestion mods/configs GitHub
│   ├── minecraft/         # Lancement Minecraft (OpenLauncherLib)
│   ├── model/             # Modèles de données
│   ├── news/              # Parser RSS et affichage news
│   ├── ui/                # Interface graphique Swing
│   └── updater/           # Auto-update GitHub Releases
├── src/main/resources/
│   ├── logback.xml        # Configuration des logs
│   └── logo.png           # Logo du launcher
├── config.yml             # Configuration principale
├── pom.xml                # Dépendances Maven
└── README.md
```

### **Technologies utilisées**

| Catégorie | Bibliothèque | Version | Usage |
|-----------|--------------|---------|-------|
| **UI** | FlatLaf | 3.2.1 | Look & Feel moderne |
| **Icônes** | Ikonli FontAwesome 5 | 12.3.1 | 2000+ icônes vectorielles |
| **Minecraft** | OpenLauncherLib | 3.2.11 | Lancement du jeu |
| **Minecraft** | FlowUpdater | 1.9.3 | Installation Forge/Fabric |
| **Auth** | AzAuth | 1.1.0 | Authentification Azuriom |
| **HTTP** | Apache HttpClient | 5.2.1 | Requêtes API |
| **JSON** | Gson | 2.10.1 | Parsing JSON |
| **Logs** | SLF4J + Logback | 2.0.5 | Journalisation |

---

## ⚙️ Configuration

### **config.yml**

```yaml
# URL Azuriom pour l'authentification
azuriomUrl: https://eclozionmc.ovh

# Repository GitHub pour auto-update et mods
githubRepo: nexos20lv/nexaria-launcher
githubBranch: main

# Version Minecraft et loader
minecraftVersion: 1.20.1
loader: forge
loaderVersion: 47.2.0

# Mémoire RAM
minMemory: 1024
maxMemory: 4096

# Arguments JVM
jvmArgs: -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# Serveur Minecraft (statut affiché en haut)
serverHost: eclozionmc.ovh
serverPort: 25565
serverName: EclozionMC

# Options
autoUpdate: true
debugMode: false
minimizeOnLaunch: true
```

### **Système de mods**

Placez vos mods dans le dossier `data/` du projet :

```
data/
├── mods/           # Mods à synchroniser
│   ├── JEI.jar
│   └── OptiFine.jar
└── configs/        # Configurations
    └── jei/
```

Au lancement, le launcher synchronise automatiquement ces fichiers vers le dossier du jeu.

---

## 🔄 Auto-Update

Le launcher vérifie automatiquement les nouvelles versions sur GitHub Releases.

### **Créer une release**

1. **Tagger une version** :
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```

2. **Créer la release sur GitHub** :
   - Aller dans **Releases** > **Draft a new release**
   - Tag : `v1.0.1`
   - Titre : `Version 1.0.1`
   - Uploader le fichier : `nexaria-launcher.jar`

3. **Le launcher détecte automatiquement** la nouvelle version et propose la mise à jour.

---

## 🎨 Personnalisation

### **Changer le thème**

Modifiez `DesignConstants.java` :

```java
public class DesignConstants {
    // Couleurs principales
    public static final Color PURPLE_ACCENT = new Color(107, 70, 193);
    public static final Color PURPLE_ACCENT_DARK = new Color(85, 55, 155);
    
    // Gradient
    public static final Color GRADIENT_MAIN_START = new Color(88, 28, 135);
    public static final Color GRADIENT_MAIN_END = new Color(29, 78, 216);
    
    // Texte
    public static final Color TEXT_PRIMARY = new Color(255, 255, 255, 240);
    public static final Color TEXT_SECONDARY = new Color(200, 200, 200, 200);
}
```

### **Changer le logo**

Remplacez `src/main/resources/logo.png` (128x128 recommandé).

### **Ajouter des actualités**

Le launcher récupère automatiquement le flux RSS Azuriom :
```
https://votre-site.azuriom.com/api/rss
```

Les news s'affichent automatiquement sur la page d'accueil.

---

## 📦 Distribution

### **JAR simple (actuel)**
```bash
mvn clean package
# Fichier : target/nexaria-launcher.jar
```

### **Installateur natif (avancé)**

Pour créer des installateurs `.dmg` (macOS) et `.exe` (Windows), voir :
- [Guide jpackage](https://docs.oracle.com/en/java/javase/21/jpackage/)
- [Certificats de signature](INSTALLATION.md#éviter-éditeur-inconnu)

---

## 🐛 Résolution de problèmes

### **Erreur : ClassNotFoundException: cpw.mods.bootstraplauncher.BootstrapLauncher**

**Solution** : Le launcher installe automatiquement Forge. Si l'erreur persiste :
1. Supprimez le dossier de jeu (voir Paramètres > Dossiers)
2. Relancez le jeu

### **Le launcher ne se met pas à jour**

**Vérifications** :
- Le repository GitHub est-il correct dans `config.yml` ?
- La release contient-elle un fichier `.jar` ?
- Le tag suit-il le format `vX.Y.Z` ?

### **Erreur réseau / Timeout**

**Solutions** :
- Vérifiez votre connexion internet
- Désactivez temporairement le pare-feu/antivirus
- Augmentez le timeout dans Paramètres > Réseau

### **Mode debug**

Activez le mode debug dans **Paramètres > Mémoire** pour obtenir des logs détaillés dans :
- **Windows** : `C:\Users\VotreNom\.nexaria\launcher.log`
- **macOS** : `~/.nexaria/launcher.log`
- **Linux** : `~/.nexaria/launcher.log`

---

## 🤝 Contribution

Les contributions sont les bienvenues ! 

1. Fork le projet
2. Créez une branche : `git checkout -b feature/ma-feature`
3. Commit : `git commit -m "Ajout de ma feature"`
4. Push : `git push origin feature/ma-feature`
5. Ouvrez une Pull Request

---

## 📄 Licence

Ce projet est sous licence **MIT**. Voir [LICENSE](LICENSE) pour plus de détails.

---

## 🙏 Remerciements

- **[FlowArg](https://github.com/FlowArg)** - OpenLauncherLib & FlowUpdater
- **[Azuriom](https://azuriom.com)** - AzAuth
- **[FlatLaf](https://www.formdev.com/flatlaf/)** - Look & Feel moderne
- **[Ikonli](https://github.com/kordamp/ikonli)** - Intégration FontAwesome

---

## 📞 Support

- 🌐 **Site web** : https://eclozionmc.ovh
- 💬 **Discord** : [Rejoindre le Discord](https://discord.gg/votre-serveur)
- 🐛 **Issues** : [GitHub Issues](https://github.com/nexos20lv/nexaria-launcher/issues)
- 📧 **Email** : support@eclozionmc.ovh

---

<div align="center">

**Fait avec ❤️ pour la communauté Minecraft**

[![GitHub Stars](https://img.shields.io/github/stars/nexos20lv/nexaria-launcher?style=social)](https://github.com/nexos20lv/nexaria-launcher)
[![GitHub Forks](https://img.shields.io/github/forks/nexos20lv/nexaria-launcher?style=social)](https://github.com/nexos20lv/nexaria-launcher/fork)

</div>
