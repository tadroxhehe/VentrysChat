# Changelog - VentrysChat

Toutes les modifications notables de ce projet seront documentées dans ce fichier.

## [2.2.0] - 2025-01-14

### ✨ Nouvelles Fonctionnalités
- **Commandes administrateur étendues** : Ajout de `/setnameother` et `/setsurnameother` pour les OP
- **Système de permissions amélioré** : Niveau 2 requis pour les commandes administratives
- **Gestion des erreurs robuste** : Meilleure stabilité et récupération en cas de problème

### 🔧 Améliorations
- **Architecture des commandes** : Refactorisation complète pour éviter les conflits
- **Gestion des événements** : Optimisation du traitement des messages côté client et serveur
- **Système de logging** : Logs plus détaillés et niveaux appropriés (DEBUG/INFO/WARN/ERROR)
- **Performance** : Optimisation des calculs de distance et de la recherche de joueurs

### 🐛 Corrections de Bugs
- **Préfixe `--`** : Correction du préfixe chuchot qui ne fonctionnait pas correctement
- **Conflits de commandes** : Résolution des doublons dans l'enregistrement des commandes
- **Détection des préfixes** : Amélioration de l'ordre de priorité pour éviter les conflits
- **Gestion des erreurs** : Le chat continue de fonctionner même en cas de problème dans le traitement RP

### 🗑️ Suppressions
- **Commande `/chuchot`** : Suppression de la commande redondante avec le préfixe `--`
- **Code obsolète** : Nettoyage des méthodes et constantes non utilisées

### 📚 Documentation
- **README complet** : Documentation détaillée de toutes les fonctionnalités
- **CHANGELOG** : Historique complet des modifications
- **Commentaires de code** : Amélioration de la lisibilité et de la maintenance

### 🧪 Tests
- **Tests unitaires** : Mise à jour et correction des tests existants
- **Tests de compilation** : Vérification que tous les changements compilent correctement

## [2.1.0] - 2025-01-13

### ✨ Nouvelles Fonctionnalités
- **Commandes administrateur** : Possibilité de changer les noms RP d'autres joueurs
- **Système de permissions** : Niveaux de permission pour les commandes sensibles
- **Validation des arguments** : Meilleure gestion des erreurs de saisie

### 🔧 Améliorations
- **Gestion des données** : Amélioration de la sauvegarde et synchronisation
- **Interface utilisateur** : Messages de confirmation et d'erreur plus clairs
- **Logs** : Meilleur suivi des actions administratives

### 🐛 Corrections de Bugs
- **Synchronisation** : Correction des problèmes de freeze lors de la synchronisation
- **Validation** : Meilleure vérification des noms de joueurs

## [2.0.0] - 2025-01-12

### ✨ Nouvelles Fonctionnalités
- **Système de chat RP complet** : Refonte totale du système de chat
- **Préfixes multiples** : Support de tous les types de messages RP
- **Système de distances** : Rayons de chat réalistes selon le type de message
- **Gestion des identités RP** : Système de noms RP personnalisables

### 🔧 Améliorations
- **Architecture modulaire** : Code restructuré pour une meilleure maintenance
- **Gestion des événements** : Utilisation des événements Forge pour une meilleure intégration
- **Sauvegarde automatique** : Système de sauvegarde des données RP
- **Synchronisation multijoueur** : Partage automatique des données entre joueurs

### 🐛 Corrections de Bugs
- **Compatibilité** : Correction des problèmes de compatibilité avec Forge 1.18.2
- **Performance** : Optimisation des calculs de distance et de la diffusion des messages

## [1.0.0] - 2025-01-11

### ✨ Première Version
- **Système de chat basique** : Fonctionnalités de base du chat RP
- **Commandes simples** : `/setname` et `/setsurname` pour les noms RP
- **Sauvegarde locale** : Sauvegarde des données RP en JSON

---

## Format du Changelog

Ce projet adhère au [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

### Types de Modifications
- **✨ Nouvelles Fonctionnalités** : Ajout de nouvelles fonctionnalités
- **🔧 Améliorations** : Amélioration des fonctionnalités existantes
- **🐛 Corrections de Bugs** : Correction de bugs identifiés
- **🗑️ Suppressions** : Suppression de fonctionnalités ou de code
- **📚 Documentation** : Amélioration de la documentation
- **🧪 Tests** : Ajout ou modification de tests
- **⚡ Performance** : Amélioration des performances
- **🔒 Sécurité** : Correction de vulnérabilités de sécurité

### Structure des Entrées
Chaque entrée suit le format :
```
## [Version] - Date

### Type de Modification
- **Description** : Détails de la modification
```

---

*Ce changelog est maintenu manuellement et mis à jour à chaque version.*
