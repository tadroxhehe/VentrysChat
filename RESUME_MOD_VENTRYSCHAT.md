# VentrysChat - Résumé Complet du Mod

## 📋 Vue d'ensemble

VentrysChat est un mod Minecraft qui transforme complètement l'expérience de jeu en ajoutant un système de roleplay (RP) complet et un système de progression par aptitudes. Le mod permet aux joueurs de créer des identités RP détaillées, de communiquer avec des distances réalistes, et de développer leurs personnages à travers trois types d'aptitudes qui influencent leurs capacités.

---

## 🎭 MODULE 1 : Système de Chat Roleplay

### Description
Le système de chat RP remplace le chat standard par un système immersif où chaque message est formaté selon son type et sa portée. Les joueurs peuvent utiliser différents préfixes pour communiquer à différentes distances, créant une expérience de roleplay réaliste.

### Fonctionnalités principales

#### **Préfixes de Chat**
- **Messages normaux** (sans préfixe) : Automatiquement formatés avec `[RP]`, portée de 15 blocs
- **Actions RP** (`*`) : Pour décrire les actions de votre personnage, portée de 15 blocs
- **Messages HRP** (`[`) : Pour les messages hors-roleplay, portée de 15 blocs
- **Chuchotements** (`-`) : Conversations privées, portée de 4 blocs
- **Chuchot très privé** (`--`) : Conversations confidentielles, portée de 2 blocs
- **Cris** (`+`) : Pour attirer l'attention, portée de 30 blocs
- **Hurlements** (`!`) : Pour les situations d'urgence, portée de 60 blocs
- **Narration** (`/narration`) : Narration immersive, portée de 100 blocs

#### **Narration Ciblée**
- Permet d'envoyer une narration RP directement à un joueur spécifique
- 8 couleurs disponibles pour personnaliser le message
- Permet l'envoi de narrations à soi-même pour des effets personnels

---

## 👤 MODULE 2 : Gestion des Identités RP

### Description
Chaque joueur peut créer une identité RP complète avec des informations personnelles, un métier, et des prestiges (hauts-faits) obtenus au cours de ses aventures.

### Fonctionnalités principales

#### **Informations Personnelles**
- **Prénom RP** : Le prénom de votre personnage
- **Nom de famille RP** : Le nom de famille de votre personnage
- **Date de naissance RP** : La date de naissance de votre personnage dans le monde RP
- **Métier RP** : La profession ou le rôle de votre personnage

#### **Prestiges (Hauts-faits)**
- Les administrateurs peuvent attribuer des prestiges aux joueurs
- Chaque prestige a un titre et une description
- Affichés dans la fiche RP du joueur
- Permet de récompenser les accomplissements RP

#### **Fiche RP Interactive**
- Interface graphique accessible via `/rpprofile`
- Affiche toutes les informations RP du joueur
- Liste tous les prestiges obtenus
- Permet de consulter son profil complet

---

## ⚔️ MODULE 3 : Système d'Aptitudes

### Description
Système de progression multijoueur où chaque joueur développe trois types d'aptitudes : **Martialité**, **Artisanat**, et **Savoir**. Chaque aptitude a 10 niveaux (0 à 10), et les joueurs doivent gérer leurs points stratégiquement car ils sont limités à 15 points totaux répartis sur les trois aptitudes.

### Fonctionnalités principales

#### **Les Trois Aptitudes**

1. **Martialité** (⚔️)
   - Représente les capacités de combat et de résistance physique
   - Confère des effets permanents invisibles selon le niveau :
     - Niveaux 0-2 : Réduction progressive de la vitesse d'attaque (Mining Fatigue)
     - Niveau 3 : Vitesse d'attaque normale (annulation du Mining Fatigue)
     - Niveau 5+ : Effets cumulatifs de Force et Résistance
   - Maximum : 10 points

2. **Artisanat** (🔨)
   - Représente les capacités de création et de fabrication
   - Utile pour les métiers d'artisan
   - Maximum : 10 points

3. **Savoir** (📚)
   - Représente les connaissances et l'intelligence
   - Utile pour les métiers intellectuels
   - Maximum : 10 points

#### **Système de Progression**

**Points Initiaux**
- Chaque nouveau joueur reçoit 5 points à répartir lors de son intégration
- Ces points sont distribués par le staff via une commande dédiée
- Le joueur ne peut pas les répartir lui-même, c'est le staff qui gère la répartition

**Give Global**
- Toutes les 2 semaines (1 dimanche sur 2), le staff effectue un "give global"
- Tous les joueurs éligibles reçoivent automatiquement 1 point dans leur aptitude de focus
- Pour être éligible, un joueur doit :
  - Avoir défini un focus (martialité, artisanat, ou savoir)
  - S'être connecté au moins 1 fois entre les 2 derniers give globaux
  - Ne pas être en cooldown (14 jours après changement de focus)
  - Ne pas avoir atteint les limites (10 points dans le focus ou 15 points totaux)

**Focus**
- Chaque joueur peut définir une aptitude de focus
- Le point du give global va automatiquement dans cette aptitude
- Changer de focus active un cooldown de 14 jours
- Pendant le cooldown, les connexions ne comptent pas pour l'éligibilité

**Limites**
- Maximum 15 points totaux répartis sur les 3 aptitudes
- Maximum 10 points par aptitude individuelle
- Les points à répartir n'ont pas de limite

#### **Effets Permanents de la Martialité**
Les effets sont complètement invisibles (pas de particules, pas d'affichage dans l'inventaire) :
- **Niveau 0** : Mining Fatigue III (70% de vitesse d'attaque)
- **Niveau 1** : Mining Fatigue II (80% de vitesse d'attaque)
- **Niveau 2** : Mining Fatigue I (90% de vitesse d'attaque)
- **Niveau 3** : Vitesse normale (annulation du Mining Fatigue)
- **Niveau 4** : Aucun effet
- **Niveau 5** : Force I (+3 dégâts)
- **Niveau 6** : Résistance I (20% de réduction de dégâts) - s'ajoute à Force I
- **Niveau 7** : Aucun effet supplémentaire
- **Niveau 8** : Force II (+6 dégâts) - remplace Force I
- **Niveau 9** : Aucun effet supplémentaire
- **Niveau 10** : Résistance II (40% de réduction de dégâts) - remplace Résistance I

---

## 🔧 MODULE 4 : Gestion des Données et Synchronisation

### Description
Toutes les données RP et d'aptitudes sont sauvegardées automatiquement et synchronisées entre tous les joueurs en temps réel. Le système est optimisé pour les serveurs massivement multijoueur.

### Fonctionnalités principales

- **Sauvegarde automatique** : Toutes les 30 minutes
- **Synchronisation multijoueur** : Les données sont partagées instantanément entre tous les joueurs
- **Gestion d'erreurs robuste** : Le système continue de fonctionner même en cas de problème
- **Optimisations** : Calculs de distance optimisés, traitement par lots pour les synchronisations

---

## 📝 LISTE COMPLÈTE DES COMMANDES

### 🎭 COMMANDES RP - Pour tous les joueurs

#### `/setname <prénom>`
**Utilité** : Définit le prénom RP de votre personnage.
**Exemple** : `/setname Jean`
**Note** : Ce prénom remplacera votre nom Minecraft dans le chat RP.

#### `/setsurname <nom>`
**Utilité** : Définit le nom de famille RP de votre personnage.
**Exemple** : `/setsurname Dupont`
**Note** : Le nom complet (prénom + nom) apparaîtra dans le chat RP.

#### `/setbirthdate <date>`
**Utilité** : Définit la date de naissance RP de votre personnage.
**Exemple** : `/setbirthdate 15 mars 1452`
**Note** : Cette information apparaîtra dans votre fiche RP.

#### `/lorejob <métier>`
**Utilité** : Définit le métier ou la profession RP de votre personnage.
**Exemple** : `/lorejob Forgeron`
**Note** : Peut contenir plusieurs mots, apparaît dans votre fiche RP.

#### `/rpstatus`
**Utilité** : Affiche toutes vos informations RP actuelles et l'état de vos données.
**Exemple** : `/rpstatus`
**Note** : Utile pour vérifier que vos données sont correctement sauvegardées.

#### `/rpprofile`
**Utilité** : Ouvre votre fiche RP complète dans une interface graphique.
**Exemple** : `/rpprofile`
**Note** : Affiche toutes vos informations RP, aptitudes, et prestiges dans une interface dédiée.

#### `/narration <message>`
**Utilité** : Envoie une narration immersive visible par tous les joueurs dans un rayon de 100 blocs.
**Exemple** : `/narration Le vent souffle doucement à travers les arbres`
**Note** : Format spécial : `[narration]` suivi du texte en gris. Vous pouvez aussi utiliser `d:distance:` pour changer la portée.

#### `/nrp <joueur> <couleur> <texte>`
**Utilité** : Envoie une narration RP directement à un joueur spécifique avec une couleur personnalisée.
**Exemple** : `/nrp PlayerName red Vous entendez un bruit étrange derrière vous`
**Couleurs disponibles** : white, yellow, green, blue, purple, red, orange, gray
**Note** : Permet d'envoyer des narrations personnalisées à un joueur, ou à vous-même pour des effets personnels.

#### `/chathelp`
**Utilité** : Affiche toutes les commandes RP disponibles et les préfixes de chat.
**Exemple** : `/chathelp`
**Note** : Guide complet pour comprendre le système de chat RP.

---

### ⚔️ COMMANDES APTITUDES - Pour tous les joueurs

#### `/setfocus <type>`
**Utilité** : Définit votre aptitude de focus. Le point du give global ira automatiquement dans cette aptitude.
**Types** : `martialité` (ou `martialite`, `martial`), `artisanat` (ou `artisan`), `savoir` (ou `sav`)
**Exemple** : `/setfocus martialité`
**Note** : Changer de focus active un cooldown de 14 jours pendant lequel vos connexions ne comptent pas pour l'éligibilité.

#### `/aptitudes`
**Utilité** : Affiche vos propres aptitudes, points à répartir, focus actuel et statut du cooldown.
**Exemple** : `/aptitudes`
**Informations affichées** :
- Points dans chaque aptitude (Martialité, Artisanat, Savoir)
- Total réparti / 15
- Points à répartir disponibles
- Focus actuel
- Statut du cooldown (si actif)
- Dernière connexion éligible

#### `/aptitudes me`
**Utilité** : Identique à `/aptitudes`, affiche vos propres aptitudes.
**Exemple** : `/aptitudes me`

---

### 👑 COMMANDES ADMINISTRATEUR - RP

#### `/setnameother <joueur> <prénom>`
**Utilité** : Change le prénom RP d'un autre joueur.
**Exemple** : `/setnameother PlayerName Pierre`
**Note** : Utile pour corriger des erreurs ou gérer les identités RP.

#### `/setsurnameother <joueur> <nom>`
**Utilité** : Change le nom de famille RP d'un autre joueur.
**Exemple** : `/setsurnameother PlayerName Martin`
**Note** : Utile pour corriger des erreurs ou gérer les identités RP.

#### `/giveprestige <joueur> "<titre>" <description>`
**Utilité** : Attribue un prestige (haut-fait) à un joueur.
**Exemple** : `/giveprestige PlayerName "Héros de la Bataille" A défendu la ville contre les envahisseurs`
**Note** : Le titre peut être entre guillemets s'il contient plusieurs mots. Le prestige apparaîtra dans la fiche RP du joueur.

#### `/resetrpdata <joueur>`
**Utilité** : Réinitialise complètement toutes les données RP d'un joueur (noms, date de naissance, métier, prestiges).
**Exemple** : `/resetrpdata PlayerName`
**Attention** : Action irréversible, utiliser uniquement en cas de nécessité absolue.

---

### 👑 COMMANDES ADMINISTRATEUR - Aptitudes

#### `/giveaptitudeinitiale <joueur>`
**Utilité** : Donne les 5 points à répartir initiaux à un nouveau joueur. Ne peut être utilisé qu'une seule fois par joueur.
**Exemple** : `/giveaptitudeinitiale PlayerName`
**Note** : À utiliser lors de l'intégration d'un nouveau joueur. Le joueur devra ensuite utiliser `/giveaptitude` pour répartir ces points.

#### `/giveaptitude <joueur> <type> <nombre>`
**Utilité** : Donne directement des points dans une aptitude spécifique. Consomme automatiquement les points à répartir du joueur si disponibles.
**Exemple** : `/giveaptitude PlayerName martialité 3`
**Types** : `martialité`, `artisanat`, `savoir`
**Fonctionnement** :
- Si le joueur a assez de points à répartir : consomme ces points et ajoute les points à l'aptitude
- Si le joueur n'a pas assez de points à répartir : ajoute directement les points (le staff peut bypasser)
- Vérifie automatiquement les limites (15 points totaux max, 10 par aptitude)

#### `/giveaptitudeglobal`
**Utilité** : Donne 1 point directement dans le focus de tous les joueurs éligibles. À utiliser toutes les 2 semaines (1 dimanche sur 2).
**Exemple** : `/giveaptitudeglobal`
**Fonctionnement** :
- Parcourt tous les joueurs avec des données d'aptitudes
- Vérifie l'éligibilité de chaque joueur
- Donne 1 point dans le focus de chaque joueur éligible
- Enregistre la date du give global dans l'historique
**Note** : Aucune prévision automatique n'est affichée, le staff gère le planning.

#### `/aptitudes <joueur>`
**Utilité** : Affiche les aptitudes d'un autre joueur.
**Exemple** : `/aptitudes PlayerName`
**Informations affichées** :
- Points dans chaque aptitude du joueur cible
- Total réparti / 15
- Points à répartir disponibles
- Focus actuel
- Statut du cooldown (si actif)

#### `/aptitudeseligibles`
**Utilité** : Liste tous les joueurs éligibles pour le prochain give global.
**Exemple** : `/aptitudeseligibles`
**Informations affichées** :
- Liste des joueurs éligibles avec leur focus actuel
- Nombre total de joueurs éligibles
**Note** : Utile pour vérifier qui recevra le point avant d'exécuter `/giveaptitudeglobal`.

#### `/aptitudeshistorique`
**Utilité** : Affiche l'historique des give globaux effectués.
**Exemple** : `/aptitudeshistorique`
**Informations affichées** :
- Date du dernier give global (ou "Jamais" si aucun)
- Nombre total de give globaux effectués
**Note** : Utile pour suivre l'historique des distributions.

#### `/resetaptitudes <joueur>`
**Utilité** : Réinitialise complètement toutes les aptitudes d'un joueur.
**Exemple** : `/resetaptitudes PlayerName`
**Fonctionnement** :
- Remet à 0 toutes les aptitudes (Martialité, Artisanat, Savoir)
- Supprime tous les points à répartir
- Supprime le focus
- Supprime le cooldown
- Remet les points initiaux à "non donnés"
**Attention** : Action irréversible, utiliser uniquement en cas de nécessité absolue.

---

## 💡 CONSEILS D'UTILISATION

### Pour les Joueurs
1. **Définissez votre identité RP** dès votre arrivée avec `/setname` et `/setsurname`
2. **Consultez votre fiche RP** régulièrement avec `/rpprofile` pour voir vos prestiges
3. **Utilisez les préfixes de chat** pour créer une expérience RP immersive
4. **Définissez votre focus** avec `/setfocus` pour recevoir automatiquement les points du give global
5. **Consultez vos aptitudes** avec `/aptitudes` pour suivre votre progression

### Pour le Staff
1. **Intégrez les nouveaux joueurs** avec `/giveaptitudeinitiale` puis répartissez leurs points avec `/giveaptitude`
2. **Effectuez les give globaux** toutes les 2 semaines avec `/giveaptitudeglobal`
3. **Vérifiez l'éligibilité** avant chaque give global avec `/aptitudeseligibles`
4. **Récompensez les accomplissements** avec `/giveprestige`
5. **Consultez l'historique** avec `/aptitudeshistorique` pour suivre les distributions

---

## 📊 RÉSUMÉ DES LIMITES ET RÈGLES

### Limites d'Aptitudes
- **15 points totaux maximum** répartis sur les 3 aptitudes
- **10 points maximum** par aptitude individuelle
- **Points à répartir** : pas de limite

### Cooldown de Focus
- **14 jours** après changement de focus
- Pendant le cooldown, les connexions ne comptent pas pour l'éligibilité au give global
- Le cooldown est affiché dans `/aptitudes`

### Éligibilité au Give Global
Pour être éligible, un joueur doit :
1. Avoir un focus défini (`/setfocus`)
2. S'être connecté au moins 1 fois entre les 2 derniers give globaux
3. Ne pas être en cooldown (14 jours après changement de focus)
4. Ne pas avoir déjà 10 points dans son focus
5. Ne pas avoir déjà 15 points totaux répartis

---

## 🎮 EXEMPLES D'UTILISATION

### Scénario 1 : Nouveau Joueur
```
Staff : /giveaptitudeinitiale PlayerName
Staff : /giveaptitude PlayerName martialité 3
Staff : /giveaptitude PlayerName artisanat 2
Joueur : /setfocus martialité
Joueur : /setname Jean
Joueur : /setsurname Dupont
```

### Scénario 2 : Give Global
```
Staff : /aptitudeseligibles
Staff : /giveaptitudeglobal
Staff : /aptitudeshistorique
```

### Scénario 3 : Communication RP
```
Joueur : Bonjour tout le monde !          → [RP] Jean Dupont : Bonjour tout le monde ! (15 blocs)
Joueur : * se lève et s'étire            → Jean Dupont se lève et s'étire (15 blocs)
Joueur : - Chut, c'est secret             → [Chuchotement] Jean Dupont : Chut, c'est secret (4 blocs)
Joueur : + ATTENTION !                    → [CRI] Jean Dupont : ATTENTION ! (30 blocs)
Joueur : /narration Le vent souffle       → [narration] + Le vent souffle (100 blocs)
```

---

**Version du mod** : 2.2.1  
**Compatibilité** : Minecraft 1.18.2 avec Forge 40.2.0+  
**Dernière mise à jour** : 2025

