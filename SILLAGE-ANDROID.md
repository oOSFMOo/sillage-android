# Sillage Android

Ce dossier est une personnalisation de TachiyomiSY 1.13.2 sous licence Apache 2.0. Les ajouts Sillage comprennent le catalogue regroupé, les variantes par source, les filtres, la migration des lectures, la récupération des couvertures, l’import initial des extensions et les mises à jour manuelles par flux de nouveautés. Chaque série est actualisée à l’ouverture.

Le catalogue est intégré à `app/src/main/assets/sillage-catalogue.bin` au format JSON compressé par gzip. Ne pas renommer son extension en `.gz` : AAPT décompresse et renomme automatiquement ces fichiers. Le contrôle de livraison doit ouvrir `assets/sillage-catalogue.bin` dans l’APK et comparer son SHA-256 à la source. Le modèle Kotlin est testé avec le catalogue complet.

Les modifications du catalogue sont conservées dans `sillage-discovery.db`, séparément des favoris. Un import est repris grâce à ses pages et positions enregistrées. Les mises à jour suivantes recherchent tous les éléments du repère précédent, sur deux à dix pages récentes. La première vérification d’une source préchargée établit un repère à partir de cinq pages. Les sources sans flux, fiches incomplètes et limites atteintes sont signalées. Les genres et notes non fournis restent inconnus. Les favoris sont vérifiés automatiquement tous les sept jours ; le catalogue global reste manuel.

Pour compiler, configure `sdk.dir` dans `local.properties`, utilise Java 17 et lance `gradlew assembleFoss`. La version FOSS est signée avec la clé de développement pour une installation personnelle. Sa signature sera différente de celle de l’APK TachiyomiSY officielle ; installe-la comme une application séparée et restaure une sauvegarde complète pour reprendre la progression. Conserve l’ancienne application jusqu’à vérification de la migration.

Android SDK 37 et Build Tools 36.0.0 sont nécessaires. FlexibleAdapter est intégré dans `vendor/flexible-adapter` depuis le commit officiel arkon/FlexibleAdapter c8013533 car son artefact JitPack ne se compilait plus. Sa licence est incluse.

Tests : `gradlew :app:testDebugUnitTest --tests '*Sillage*Test' --tests '*CoverRepairCoordinatorTest'`. Consulter les notes de publication pour les résultats de chaque livraison. La version 1.1 doit conserver l’identifiant `fr.sillage.reader` et la signature de la version 1.0 pour s’installer comme mise à jour, sans désinstallation. Le test sur appareil n’a pas été mené à terme, faute de téléphone connecté et d’émulateur fonctionnel.
