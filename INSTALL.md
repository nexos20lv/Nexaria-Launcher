# 📥 Comment Installer Nexaria Launcher

Choisissez votre plateforme ci-dessous pour les instructions détaillées.

---

## 🪟 Windows
1. Téléchargez le fichier `Nexaria.Launcher.Setup.X.X.X.exe` depuis les [Releases](https://github.com/nexos20lv/Nexaria-Launcher/releases).
2. Lancez l'exécutable. 
3. Si Windows SmartScreen affiche un avertissement (car le launcher n'est pas signé) :
   - Cliquez sur **"Informations complémentaires"**.
   - Cliquez sur **"Exécuter quand même"**.
4. Le launcher s'installera et créera un raccourci sur votre bureau.

---

## 🍎 macOS
> [!IMPORTANT]
> Apple bloque par défaut les applications non signées téléchargées sur internet. Voici comment passer outre la sécurité Gatekeeper.

1. Téléchargez le fichier `Nexaria.Launcher-X.X.X-arm64.dmg` (pour puces Apple M1/M2/M3) ou `x64.dmg` (pour Intel).
2. Ouvrez le `.dmg` et faites glisser le launcher dans votre dossier **Applications**.
3. **Étape Cruciale :** Ouvrez votre **Terminal** et tapez la commande suivante :
   ```bash
   xattr -cr "/Applications/NexariaLauncher.app"
   ```
4. Vous pouvez maintenant lancer l'application normalement depuis votre Launchpad ou dossier Applications.

---

## 🐧 Linux
1. Téléchargez le fichier `Nexaria.Launcher-X.X.X.AppImage`.
2. Faites un clic droit sur le fichier -> **Propriétés** -> **Permissions**.
3. Cochez **"Autoriser l'exécution du fichier comme un programme"**.
4. Alternativement, dans un terminal :
   ```bash
   chmod +x Nexaria.Launcher-X.X.X.AppImage
   ./Nexaria.Launcher-X.X.X.AppImage
   ```

---

## ☕ Prérequis
- **Aucun** : Le launcher installe maintenant automatiquement **Java 21** s'il est manquant ou incompatible. Plus besoin de s'en soucier ! 🎉
