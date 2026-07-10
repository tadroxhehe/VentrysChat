# VentrysChat - Mod de Chat Roleplay pour Minecraft 1.18.2

## Description
VentrysChat est un mod Minecraft Forge qui transforme le système de chat en un système de roleplay avancé avec des préfixes, des distances de diffusion et une gestion des identités RP.

## Version
**2.2.0** - Version stable avec toutes les fonctionnalités implémentées

## Fonctionnalités

### 🎭 Système de Chat RP
- **Messages normaux** : Sans préfixe → formatés automatiquement avec `[RP]` (15 blocs)
- **Actions RP** : `*` pour les actions (15 blocs)
- **Messages HRP** : `[` pour les messages hors-roleplay (15 blocs)
- **Chuchotements** : `-` pour les conversations privées (4 blocs)
- **Chuchot très privé** : `--` pour les conversations confidentielles (2 blocs)
- **Cris** : `+` pour attirer l'attention (30 blocs)
- **Hurlements** : `!` pour les situations d'urgence (60 blocs)
- **Narration** : `/narration` pour la narration immersive (100 blocs)

### 👤 Gestion des Identités RP
- **Commandes personnelles** :
  - `/setname <prénom>` - Définir son prénom RP
  - `/setsurname <nom>` - Définir son nom de famille RP
  - `/rpstatus` - Afficher son statut RP actuel
  - `/narration <message>` - Envoyer une narration

- **Commandes administrateur** (niveau OP 2) :
  - `/setnameother <joueur> <prénom>` - Changer le prénom RP d'un autre joueur
  - `/setsurnameother <joueur> <nom>` - Changer le nom RP d'un autre joueur

### 🔧 Caractéristiques Techniques
- **Système de sauvegarde automatique** : Toutes les 5 minutes
- **Synchronisation multijoueur** : Données RP partagées entre tous les joueurs
- **Gestion d'erreurs robuste** : Le chat continue de fonctionner même en cas de problème
- **Logs détaillés** : Suivi complet des actions et erreurs
- **Performance optimisée** : Calculs de distance optimisés

## Installation

### Prérequis
- Minecraft 1.18.2
- Forge 40.2.0 ou supérieur
- Java 17

### Installation
1. Téléchargez le fichier JAR depuis la section Releases
2. Placez-le dans le dossier `mods` de votre installation Minecraft
3. Redémarrez Minecraft

## Configuration
Le mod se configure automatiquement. Les données RP sont sauvegardées dans :
```
.minecraft/saves/[nom_du_monde]/ventryschat_rp_data.json
```

## Utilisation

### Messages de Base
- Tapez simplement votre message → il sera automatiquement formaté avec `[RP]` et diffusé sur 15 blocs
- Utilisez les préfixes pour des types de messages spécifiques

### Exemples d'Utilisation
```
Bonjour tout le monde          → [RP] VotreNom : Bonjour tout le monde (15 blocs)
* se lève et s'étire          → VotreNom se lève et s'étire (15 blocs)
- Chut, c'est secret          → [Chuchotement] VotreNom : Chut, c'est secret (4 blocs)
-- Message ultra secret        → [Chuchot] VotreNom : Message ultra secret (2 blocs)
+ ATTENTION !                 → [CRI] VotreNom : ATTENTION ! (30 blocs)
! AU SECOURS !                → [HURLEMENT] VotreNom : AU SECOURS ! (60 blocs)
[ Ceci est un message HRP     → [HRP] VotreNom : Ceci est un message HRP (15 blocs)
/narration Le vent souffle     → [narration] + Le vent souffle (100 blocs)
```

### Commandes
```
/setname Jean                  → Définit votre prénom RP à "Jean"
/setsurname Dupont            → Définit votre nom RP à "Dupont"
/rpstatus                     → Affiche votre statut RP actuel
/narration Le soleil se couche → Envoie une narration
```

## Structure du Code

### Fichiers Principaux
- **`VentrysChatMod.java`** - Point d'entrée principal du mod
- **`RPConstants.java`** - Toutes les constantes (préfixes, couleurs, distances)
- **`RPMessageHandler.java`** - Gestion du formatage et des distances des messages
- **`RPDataManager.java`** - Gestion des données RP et sauvegarde
- **`RPCommands.java`** - Implémentation de toutes les commandes
- **`ServerChatHandler.java`** - Gestion des événements de chat côté serveur
- **`ClientChatHandler.java`** - Gestion des événements de chat côté client

### Architecture
- **Système d'événements** : Utilise les événements Forge pour intercepter le chat
- **Gestion des données** : Sauvegarde JSON avec vérification d'intégrité
- **Réseau** : Synchronisation automatique des données entre client et serveur
- **Commandes** : Système Brigadier pour une intégration parfaite avec Minecraft

## Développement

### Compilation
```bash
./gradlew build
```

### Tests
```bash
./gradlew test
```

### Structure des Tests
- Tests unitaires pour toutes les méthodes utilitaires
- Vérification des distances et préfixes
- Tests de formatage des messages

## Historique des Versions

### v2.2.0 (Actuelle)
- ✅ Suppression de la commande `/chuchot` redondante
- ✅ Distances optimisées selon les spécifications utilisateur
- ✅ Correction de tous les bugs identifiés
- ✅ Code nettoyé et optimisé
- ✅ Tests mis à jour

### v2.1.0
- Ajout des commandes administrateur
- Amélioration de la gestion d'erreurs
- Optimisation des performances

### v2.0.0
- Refonte complète du système de chat
- Nouveau système de préfixes
- Gestion des distances de diffusion

## Support et Contribution
Ce mod est développé pour la communauté Minecraft. Pour toute question ou suggestion, n'hésitez pas à ouvrir une issue sur le repository.

## Licence
Ce mod est distribué sous licence MIT. Voir le fichier LICENSE pour plus de détails.

---

**Note** : Ce mod est optimisé pour Minecraft 1.18.2 avec Forge. Assurez-vous d'utiliser la bonne version de Forge pour éviter les problèmes de compatibilité.
