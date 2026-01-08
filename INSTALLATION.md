# 📥 Guide d'installation - Nexaria Launcher

Ce guide vous aidera à installer et lancer le **Nexaria Launcher** sur votre système.

---

## 📋 Table des matières

- [Windows](#-windows-1011)
- [macOS](#-macos)
- [Linux](#-linux)
- [Problèmes courants](#-problèmes-courants)

---

## 🪟 Windows 10/11

### **Étape 1 : Installer Java 21**

1. **Télécharger Java 21** :
   - Rendez-vous sur [Adoptium Temurin](https://adoptium.net/fr/temurin/releases/?version=21)
   - Sélectionnez **Windows x64**
   - Cliquez sur **.msi** (installateur)

2. **Installer Java** :
   - Double-cliquez sur le fichier téléchargé
   - Suivez l'assistant d'installation
   - ✅ **Cochez "Set JAVA_HOME variable"**
   - ✅ **Cochez "Add to PATH"**
   - Cliquez sur **Installer**

3. **Vérifier l'installation** :
   ```cmd
   java -version
   ```
   Vous devriez voir :
   ```
   openjdk version "21.0.x"
   ```

### **Étape 2 : Télécharger le Launcher**

1. Allez sur la page [Releases GitHub](https://github.com/nexos20lv/nexaria-launcher/releases/latest)
2. Téléchargez **`nexaria-launcher-X.X.X.jar`**
3. Placez le fichier dans un dossier dédié :
   ```
   C:\Users\VotreNom\NexariaLauncher\
   ```

### **Étape 3 : Lancer le Launcher**

#### **Option A : Double-clic (Recommandé)**
- Double-cliquez sur `nexaria-launcher-X.X.X.jar`
- Windows devrait automatiquement le lancer avec Java

#### **Option B : Invite de commandes**
```cmd
cd C:\Users\VotreNom\NexariaLauncher
java -jar nexaria-launcher.jar
```

### **Étape 4 : Créer un raccourci (Optionnel)**

1. Créez un fichier `Nexaria Launcher.bat` :
   ```batch
   @echo off
   cd /d "%~dp0"
   start javaw -jar nexaria-launcher.jar
   ```

2. Créez un raccourci vers ce `.bat`
3. Changez l'icône (clic droit > Propriétés > Changer l'icône)

---

## 🍎 macOS

### **Étape 1 : Installer Java 21**

#### **Option A : Homebrew (Recommandé)**
```bash
# Installer Homebrew si nécessaire
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Installer Java 21
brew install openjdk@21

# Lier Java au système
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk \
     /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

#### **Option B : Téléchargement manuel**
1. Télécharger depuis [Adoptium Temurin](https://adoptium.net/fr/temurin/releases/?version=21)
2. Sélectionnez **macOS aarch64** (Apple Silicon) ou **x64** (Intel)
3. Téléchargez le **.pkg** et installez-le

#### **Vérifier l'installation**
```bash
java -version
```

### **Étape 2 : Télécharger le Launcher**

1. Téléchargez **`nexaria-launcher-X.X.X.jar`** depuis [GitHub Releases](https://github.com/nexos20lv/nexaria-launcher/releases/latest)
2. Placez-le dans **Applications** ou un dossier dédié :
   ```bash
   mkdir -p ~/Applications/NexariaLauncher
   mv ~/Downloads/nexaria-launcher-*.jar ~/Applications/NexariaLauncher/
   ```

### **Étape 3 : Lancer le Launcher**

#### **Option A : Double-clic**
- Double-cliquez sur le `.jar`
- **Si macOS bloque** : Clic droit > Ouvrir (la première fois seulement)

#### **Option B : Terminal**
```bash
cd ~/Applications/NexariaLauncher
java -jar nexaria-launcher.jar
```

### **Étape 4 : Créer une app macOS (Optionnel)**

1. Ouvrez **Automator**
2. Créez une nouvelle **Application**
3. Ajoutez l'action **"Exécuter un script Shell"**
4. Collez ce script :
   ```bash
   #!/bin/bash
   cd ~/Applications/NexariaLauncher
   /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java \
     -Xdock:name="Nexaria Launcher" \
     -Xdock:icon=$HOME/Applications/NexariaLauncher/logo.png \
     -jar nexaria-launcher.jar
   ```
5. Enregistrez comme **Nexaria Launcher.app**
6. Placez-la dans **Applications**

---

## 🐧 Linux

### **Étape 1 : Installer Java 21**

#### **Ubuntu/Debian**
```bash
sudo apt update
sudo apt install openjdk-21-jre -y
```

#### **Fedora/RHEL**
```bash
sudo dnf install java-21-openjdk -y
```

#### **Arch Linux**
```bash
sudo pacman -S jre21-openjdk
```

#### **Vérifier l'installation**
```bash
java -version
```

### **Étape 2 : Télécharger le Launcher**

```bash
# Créer un dossier
mkdir -p ~/Applications/NexariaLauncher
cd ~/Applications/NexariaLauncher

# Télécharger depuis GitHub
wget https://github.com/nexos20lv/nexaria-launcher/releases/latest/download/nexaria-launcher.jar
```

### **Étape 3 : Lancer le Launcher**

```bash
java -jar nexaria-launcher.jar
```

### **Étape 4 : Créer un lanceur (Optionnel)**

Créez `~/.local/share/applications/nexaria-launcher.desktop` :

```ini
[Desktop Entry]
Version=1.0
Type=Application
Name=Nexaria Launcher
Comment=Launcher Minecraft pour Nexaria
Exec=java -jar /home/VOTRE_NOM/Applications/NexariaLauncher/nexaria-launcher.jar
Icon=/home/VOTRE_NOM/Applications/NexariaLauncher/logo.png
Terminal=false
Categories=Game;
```

Rendez-le exécutable :
```bash
chmod +x ~/.local/share/applications/nexaria-launcher.desktop
```

---

## 🔧 Problèmes courants

### ❌ "Java n'est pas reconnu"

**Cause** : Java n'est pas dans le PATH

**Solution Windows** :
1. Panneau de configuration > Système > Paramètres système avancés
2. Variables d'environnement
3. Dans "Variables système", éditez **Path**
4. Ajoutez : `C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot\bin`
5. Redémarrez le terminal

**Solution macOS/Linux** :
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH=$JAVA_HOME/bin:$PATH
```

### ❌ "Impossible d'ouvrir le fichier JAR"

**Windows** :
- Clic droit > Ouvrir avec > Java(TM) Platform SE Binary
- OU utiliser la commande : `java -jar nexaria-launcher.jar`

**macOS** :
- Clic droit > Ouvrir (autorise l'ouverture)
- OU via Terminal : `java -jar nexaria-launcher.jar`

### ❌ "UnsupportedClassVersionError"

**Cause** : Version de Java trop ancienne

**Solution** : Installez Java 21 (voir étape 1)

### ❌ Le launcher ne se lance pas en double-clic

**Vérifiez l'association de fichiers** :

**Windows** :
```cmd
assoc .jar=jarfile
ftype jarfile="C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot\bin\javaw.exe" -jar "%1" %*
```

**macOS** :
- Clic droit sur le `.jar` > Obtenir des informations
- "Ouvrir avec" : Sélectionnez **Jar Launcher** ou **Java**
- Cliquez sur "Tout modifier"

### ❌ Erreur "Impossible de trouver les bibliothèques"

**Solution** : Lancez toujours le launcher depuis le dossier où il se trouve :
```bash
cd /chemin/vers/NexariaLauncher
java -jar nexaria-launcher.jar
```

---

## 🚀 Première utilisation

1. **Lancez le launcher**
2. **Connectez-vous** avec vos identifiants Azuriom
3. Le launcher va automatiquement :
   - Télécharger Minecraft 1.20.1
   - Installer Forge 47.2.0
   - Synchroniser les mods depuis GitHub
4. **Cliquez sur JOUER** 🎮

---

## 📞 Besoin d'aide ?

- 🌐 **Site web** : https://eclozionmc.ovh
- 💬 **Discord** : [Rejoindre le Discord](https://discord.gg/votre-serveur)
- 🐛 **Signaler un bug** : [GitHub Issues](https://github.com/nexos20lv/nexaria-launcher/issues)

---

## 🔄 Mises à jour

Le launcher se met à jour automatiquement ! 🎉

Vous serez notifié quand une nouvelle version est disponible.

---

**Bon jeu ! ⚔️**
