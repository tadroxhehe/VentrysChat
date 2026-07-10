# Guide d'Installation - VentrysChat

Ce guide vous accompagne dans l'installation et la configuration du mod VentrysChat pour Minecraft 1.18.2.

## 📋 Prérequis

### Système
- **Système d'exploitation** : Windows 10/11, macOS 10.15+, ou Linux
- **Mémoire RAM** : Minimum 4 GB, recommandé 8 GB
- **Espace disque** : 2 GB minimum pour Minecraft + mods

### Logiciels
- **Java** : Version 17 ou supérieure (JDK ou JRE)
- **Minecraft** : Version 1.18.2 (client officiel)
- **Forge** : Version 40.2.0 ou supérieure

## 🚀 Installation Étape par Étape

### Étape 1 : Vérifier Java
1. **Ouvrez un terminal/invite de commande**
2. **Vérifiez la version Java** :
   ```bash
   java -version
   ```
3. **Assurez-vous d'avoir Java 17+** :
   ```
   openjdk version "17.0.12" 2024-10-22
   OpenJDK Runtime Environment (build 17.0.12+8-LTS-286)
   OpenJDK 64-Bit Server VM (build 17.0.12+8-LTS-286, mixed mode, sharing)
   ```

### Étape 2 : Installer Minecraft 1.18.2
1. **Lancez le launcher Minecraft officiel**
2. **Allez dans l'onglet "Installations"**
3. **Cliquez sur "Nouvelle installation"**
4. **Configurez l'installation** :
   - **Nom** : `VentrysChat 1.18.2`
   - **Version** : `1.18.2`
   - **Type** : `Release`
5. **Cliquez sur "Créer"**

### Étape 3 : Installer Forge
1. **Téléchargez Forge 40.2.0+** depuis [files.minecraftforge.net](https://files.minecraftforge.net/)
2. **Lancez le fichier JAR téléchargé**
3. **Sélectionnez "Install client"**
4. **Attendez la fin de l'installation**
5. **Vérifiez que Forge apparaît dans vos installations**

### Étape 4 : Installer VentrysChat
1. **Téléchargez le fichier JAR** `ventryschat-2.2.0.jar`
2. **Localisez le dossier mods** :
   - **Windows** : `%APPDATA%\.minecraft\mods\`
   - **macOS** : `~/Library/Application Support/minecraft/mods/`
   - **Linux** : `~/.minecraft/mods/`
3. **Placez le fichier JAR dans le dossier mods**
4. **Vérifiez la structure** :
   ```
   .minecraft/
   ├── mods/
   │   └── ventryschat-2.2.0.jar
   ├── saves/
   └── logs/
   ```

### Étape 5 : Première Lancement
1. **Lancez Minecraft avec l'installation Forge**
2. **Attendez le chargement complet**
3. **Vérifiez dans les logs** que VentrysChat se charge :
   ```
   [INFO] Ventrys Chat RP mod initialisé !
   [INFO] Ventrys Chat RP mod configuré !
   ```

## 🔧 Configuration

### Configuration Automatique
Le mod se configure automatiquement. Aucun fichier de configuration n'est requis.

### Fichiers de Données
Les données RP sont automatiquement sauvegardées dans :
```
.minecraft/saves/[nom_du_monde]/ventryschat_rp_data.json
```

### Première Utilisation
1. **Rejoignez un monde ou serveur**
2. **Testez les commandes de base** :
   ```
   /setname VotrePrénom
   /setsurname VotreNom
   /rpstatus
   ```
3. **Testez le chat RP** :
   ```
   * se lève et s'étire
   - Chut, c'est secret
   + Bonjour tout le monde !
   ```

## 🎮 Utilisation

### Commandes Disponibles
| Commande | Description | Permission |
|----------|-------------|------------|
| `/setname <prénom>` | Définir son prénom RP | Joueur |
| `/setsurname <nom>` | Définir son nom RP | Joueur |
| `/rpstatus` | Afficher son statut RP | Joueur |
| `/narration <message>` | Envoyer une narration | Joueur |
| `/setnameother <joueur> <prénom>` | Changer le prénom RP d'un autre joueur | OP (niveau 2) |
| `/setsurnameother <joueur> <nom>` | Changer le nom RP d'un autre joueur | OP (niveau 2) |

### Préfixes de Chat
| Préfixe | Type | Distance | Exemple |
|---------|------|----------|---------|
| (aucun) | Message normal | 15 blocs | `Bonjour` → `[RP] VotreNom : Bonjour` |
| `*` | Action | 15 blocs | `* se lève` → `VotreNom se lève` |
| `[` | HRP | 15 blocs | `[ Ceci est HRP` → `[HRP] VotreNom : Ceci est HRP` |
| `-` | Chuchotement | 4 blocs | `- Secret` → `[Chuchotement] VotreNom : Secret` |
| `--` | Chuchot très privé | 2 blocs | `-- Ultra secret` → `[Chuchot] VotreNom : Ultra secret` |
| `+` | Cri | 30 blocs | `+ ATTENTION !` → `[CRI] VotreNom : ATTENTION !` |
| `!` | Hurlement | 60 blocs | `! AU SECOURS !` → `[HURLEMENT] VotreNom : AU SECOURS !` |

## 🚨 Dépannage

### Problèmes Courants

#### Le mod ne se charge pas
- **Vérifiez la version de Forge** : Doit être 40.2.0+
- **Vérifiez Java** : Doit être version 17+
- **Vérifiez les logs** : Regardez dans `.minecraft/logs/latest.log`

#### Les commandes ne fonctionnent pas
- **Vérifiez les permissions** : Certaines commandes nécessitent le niveau OP
- **Vérifiez la syntaxe** : Utilisez `/rpstatus` pour vérifier votre statut
- **Redémarrez le serveur** : En cas de problème persistant

#### Les données ne se sauvegardent pas
- **Vérifiez les permissions d'écriture** : Le dossier doit être accessible en écriture
- **Vérifiez l'espace disque** : Assurez-vous d'avoir suffisamment d'espace
- **Vérifiez les logs** : Recherchez les erreurs de sauvegarde

### Logs Importants
- **Client** : `.minecraft/logs/latest.log`
- **Serveur** : `logs/latest.log` (dans le dossier du serveur)

### Messages d'Erreur Courants
```
[ERROR] Mod ventryschat requires Forge 40.2.0 or higher
→ Mettez à jour Forge vers la version 40.2.0+

[ERROR] Java version must be 17 or higher
→ Mettez à jour Java vers la version 17+

[WARN] Failed to save RP data
→ Vérifiez les permissions d'écriture du dossier
```

## 🔄 Mise à Jour

### Depuis une Version Antérieure
1. **Sauvegardez votre monde** : Copiez le dossier `saves/[nom_du_monde]`
2. **Remplacez l'ancien JAR** par le nouveau dans le dossier `mods`
3. **Redémarrez Minecraft**
4. **Vérifiez la compatibilité** des données existantes

### Sauvegarde des Données
Les données RP sont automatiquement sauvegardées. Pour une sauvegarde manuelle :
1. **Arrêtez le serveur/client**
2. **Copiez le fichier** `ventryschat_rp_data.json`
3. **Stockez-le en lieu sûr**

## 📞 Support

### Ressources d'Aide
- **README.md** : Documentation complète du mod
- **CHANGELOG.md** : Historique des versions et modifications
- **Logs** : Informations détaillées dans les fichiers de log

### Problèmes Non Résolus
Si vous rencontrez un problème non résolu :
1. **Vérifiez les logs** pour les erreurs
2. **Vérifiez la compatibilité** avec votre version de Forge
3. **Vérifiez les prérequis** (Java, Minecraft, Forge)
4. **Redémarrez complètement** Minecraft et votre ordinateur

---

**Note** : Ce guide couvre l'installation standard. Pour des configurations spéciales ou des serveurs dédiés, consultez la documentation avancée.
