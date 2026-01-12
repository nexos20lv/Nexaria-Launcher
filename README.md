# Nexaria Launcher

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Launcher Minecraft avec authentification Azuriom, synchronisation automatique des mods/configs et contrôle d'intégrité renforcé.

---

## Fonctionnalités principales
- Support Forge/Fabric/NeoForge/Vanilla (build actuel ciblé : **Forge 1.21.5-55.1.4**)
- Synchronisation locale : tout le contenu `data/` (mods + config) est copié dans le dossier du jeu
- Manifest unifié `data-manifest.json` (SHA-256) généré au build
- Double vérification d'intégrité (après synchro et juste avant lancement), politique strict/warn
- Quarantaine automatique des fichiers inattendus/modifiés
- Authentification Azuriom + auto-login, support 2FA (double facteur)
- Anti-Cheat client (détection processus interdits, hardening JVM)
- Galerie de screenshots intégrée avec visualisation et gestion
- Console de débogage avec export de logs (.zip)
- UI Swing avec FlatLaf, news RSS, statut serveur, minimisation automatique pendant le jeu

---

## Installation rapide (utilisateurs)
1. Installez **Java 21** (Temurin recommandé) : https://adoptium.net
2. Téléchargez la [dernière release](https://github.com/nexos20lv/nexaria-launcher/releases/latest)
3. Lancez : `java -jar nexaria-launcher.jar`

Le guide détaillé est dans [INSTALLATION.md](INSTALLATION.md).

---

## Données et intégrité
- Placez vos fichiers dans `data/` :
    - `data/mods/*.jar`
    - `data/config/...` (configs, assets, layouts)
- Le build (`mvn clean package`) génère automatiquement `data/data-manifest.json` via `ManifestGeneratorMojo`.
- Au lancement :
    1. Synchronisation `data/` -> `game/` (`mods`, `config`, manifest)
    2. Vérification d'intégrité immédiatement après la copie
    3. Vérification d'intégrité juste avant de démarrer le jeu
- Politique d'application : `strict` si `enforceModPolicy=true`, sinon simple avertissement.

Chemin du jeu par défaut : macOS `~/Library/Application Support/NexariaLauncher/game` (similaire Windows/Linux via `LauncherConfig`).

---

## Développement

### Prérequis
- JDK 21
- Maven 3.6+
- Git

### Build & run
```bash
git clone https://github.com/nexos20lv/nexaria-launcher.git
cd nexaria-launcher
mvn clean package -DskipTests
java -jar target/nexaria-launcher.jar
```

### Structure
```
src/main/java/com/nexaria/launcher/
├── auth/        # Authentification Azuriom
├── config/      # Lecture config.yml et paths
├── downloader/  # Sync data -> game, quarantaine
├── minecraft/   # Lancement via OpenLauncherLib
├── security/    # Vérification d'intégrité (data-manifest)
├── ui/          # UI Swing
└── updater/     # Auto-update GitHub Releases
data/            # Mods + configs à synchroniser
```

---

## Configuration (exemple `config.yml`)
```yaml
azuriomUrl: https://eclozionmc.ovh
githubRepo: nexos20lv/nexaria-launcher
githubBranch: main

minecraftVersion: 1.21.5
loader: forge
loaderVersion: 55.1.4

minMemory: 1024
maxMemory: 4096
jvmArgs: -XX:+UseG1GC -XX:MaxGCPauseMillis=200

serverHost: 151.240.30.3
serverPort: 25545
serverName: Nexaria

autoUpdate: true
debugMode: false
minimizeOnLaunch: true
enforceModPolicy: true
bannedProcessesCsv: cheatengine.exe, processhacker.exe, x64dbg.exe, x32dbg.exe, ida64.exe, ida.exe, ollydbg.exe, wireshark.exe, fiddler.exe
```

---

## Support
- Site : https://eclozionmc.ovh
- Discord : https://discord.gg/votre-serveur
- Bugs : https://github.com/nexos20lv/nexaria-launcher/issues

---

Fait avec ❤️ pour la communauté Minecraft.
