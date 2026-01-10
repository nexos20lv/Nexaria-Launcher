wget https://github.com/nexos20lv/Nexaria-Launcher/releases/latest/download/nexaria-launcher.jar
# Guide d'installation

## Prérequis
- Java 21 (Temurin recommandé)
- Connexion internet pour le premier lancement

## Windows 10/11
1. Installer Java 21 : https://adoptium.net (MSI, cochez JAVA_HOME et PATH)
2. Télécharger le JAR : https://github.com/nexos20lv/nexaria-launcher/releases/latest
3. Lancer : double-clic ou `java -jar nexaria-launcher.jar`
4. Si SmartScreen bloque : "Informations complémentaires" > Exécuter quand même.

## macOS (Apple Silicon ou Intel)
1. Installer Java 21 avec Homebrew :
   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   brew install openjdk@21
   sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
   ```
   ou via PKG Temurin depuis adoptium.net
2. Télécharger le JAR : https://github.com/nexos20lv/nexaria-launcher/releases/latest
3. Lancer : double-clic (sinon clic droit > Ouvrir) ou `java -jar nexaria-launcher.jar`

## Linux
1. Installer Java 21 (exemples) :
   - Debian/Ubuntu : `sudo apt install openjdk-21-jre`
   - Fedora : `sudo dnf install java-21-openjdk`
   - Arch : `sudo pacman -S jre-openjdk`
2. Télécharger le JAR :
   ```bash
   wget https://github.com/nexos20lv/nexaria-launcher/releases/latest/download/nexaria-launcher.jar
   ```
3. Lancer : `java -jar nexaria-launcher.jar`

## Où sont stockés les fichiers du jeu ?
- Windows : `%APPDATA%/NexariaLauncher/game`
- macOS : `~/Library/Application Support/NexariaLauncher/game`
- Linux : `~/.local/share/NexariaLauncher/game`

## Problèmes fréquents
- "java n'est pas reconnu" : ajoutez le dossier Java 21 dans PATH ou relancez la session.
- macOS bloque l'ouverture : clic droit > Ouvrir (une fois), ou `chmod +x` inutile pour un JAR.
- UnsupportedClassVersionError : installez Java 21 et relancez.

## Mise à jour
Le launcher vérifie automatiquement les releases GitHub. Téléchargez la nouvelle version et remplacez l'ancien JAR si nécessaire.
