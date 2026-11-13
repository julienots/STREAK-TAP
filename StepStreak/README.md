# StepStreak 2.0 🔥

Application Android de comptage de pas avec séries (streaks), grades, niveaux, défis et succès.
Elle tourne en arrière-plan (service en premier plan), même quand l'app est fermée.

## Nouveautés de la 2.0
- **Nouveau design** : fond « aurora » animé, cartes en verre, navigation flottante à 3 onglets
- **Anneau de progression** : graduations, halo lumineux, point qui pulse, 2e tour quand l'objectif est dépassé, étincelles à 100 %
- **Flamme de série animée** avec braises, qui s'éteint si la série tombe à 0
- **Confettis** quand l'objectif est atteint (et un bouton pour les relancer dans les réglages)
- **Stats** : histogramme interactif sur 7 jours, 30 jours ou 3 mois, calendrier d'activité sur 18 semaines, records personnels
- **Trophées** : 8 grades (Bronze → Mythique), niveau et XP, 26 succès répartis en 5 catégories
- **Gels de série 🧊** : un gel gagné tous les 7 jours de série (2 max), utilisé automatiquement si tu rates un jour
- **Défi du jour** : un nouveau défi chaque jour, avec de l'XP en bonus
- **Distance, calories et minutes actives**, calculées à partir de ta taille et de ton poids
- **3 widgets** : Journée (4×2, avec la semaine), Anneau (2×2) et Flamme (2×2)
- **Notifications** : suivi en direct avec mise en page perso (anneau, flamme, barre de progression, stats), célébrations illustrées, paliers 50 % / 75 %, alerte « série en danger » à 20 h, résumé de la semaine le dimanche, rappels d'inactivité
- **Réglages** : curseur d'objectif avec préréglages, taille et poids, activation de chaque type de notification, sons, vibrations
- Les pas déjà faits sont conservés après un redémarrage du téléphone

## Compiler l'APK
Chaque push lance le workflow GitHub **Build APK**. Une fois terminé, ouvre le run et télécharge l'artifact
`StepStreak-debug-apk`, dézippe-le et installe le `.apk` sur ton téléphone.

## Fiabilité en arrière-plan
Après l'installation, appuie sur « Autoriser l'exécution en arrière-plan ». Sur Xiaomi, Huawei, Samsung ou Oppo,
va aussi dans Paramètres > Batterie > StepStreak et choisis « Sans restriction ».

## Structure
- `StepPrefs.kt` : stockage, historique, série, gels, records
- `Progression.kt` : grades, succès, niveaux/XP, défi du jour, distance/calories, messages
- `Views.kt` / `Painters.kt` : anneau, flamme, aurora, confettis, graphiques, calendrier
- `BitmapRenderer.kt` : rendus pour les widgets et les notifications
- `NotificationHelper.kt` : toutes les notifications
- `Widgets.kt` : les 3 widgets
- `Alarms.kt` : rappels, alerte du soir, redémarrage
- `StepService.kt` : service podomètre
- `MainActivity.kt` : écran principal et réglages
