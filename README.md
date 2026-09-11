# Sillage Android

Une bibliothèque de manhua et webtoons pour lire sur Android, sans dépendre d’un PC allumé. Interface française et catalogue commun, personnalisés à partir de TachiyomiSY 1.13.2.

## Télécharger

**[Télécharger la version Android](https://github.com/oOSFMOo/sillage-android/releases/tag/v1.0.0)**

Dans les fichiers de la version, prends **Sillage-Android-1.0.apk**. Le paquet **Sillage-Android-et-extensions.zip** contient aussi les cinq extensions préparées et le guide.

[Guide d’installation et de transfert de progression](INSTALLATION.md)

## Fonctionnalités

- Catalogue initial de 81 527 fiches regroupées ; les favoris sont choisis séparément.
- Recherche, filtres par genre et minimum de chapitres ; classement par chapitres, note ou titre.
- Différentes éditions accessibles depuis une même fiche, avec la plus complète en premier selon les nombres connus ; moyenne des notes disponibles.
- Récupération des couvertures depuis les sources Android.
- Import initial automatique d’une nouvelle extension installée et approuvée, avec reprise en cas d’interruption.
- Bouton global de recherche des nouveautés dans les flux récents, sans recommencer le catalogue complet.
- Actualisation des chapitres à l’ouverture de chaque série, favoris, historique et lecture hors connexion après téléchargement.

## État de la version

Cette première version compilée est destinée à être testée. **16 tests automatisés passent**, dont le décodage du catalogue complet, la conservation des métadonnées lors d’une panne, le regroupement et la migration des lectures. Le contenu du catalogue dans l’APK et sa signature ont été vérifiés. Le test sur téléphone réel n’a pas encore été réalisé ; l’émulateur de préparation n’a pas démarré correctement.

Les sites et extensions peuvent changer ou être indisponibles. Certaines métadonnées du catalogue initial sont encore inconnues. Les nouvelles notes ne peuvent être renseignées que si elles sont fournies ; aucune note n’est inventée. La première vérification d’une source préchargée établit un repère sur ses cinq pages récentes ; les suivantes suivent ce repère. Un changement absent du flux reste à vérifier sur la fiche.

L’application et le catalogue ne contiennent pas les images des chapitres. Les sources sont nécessaires pour les consulter. La progression Android n’est pas synchronisée avec Windows.

## Construire

Java 17, Android SDK 37 et Build Tools 36.0.0. Définir `sdk.dir` dans `local.properties`, puis :

```sh
./gradlew :app:assembleFoss
./gradlew :app:testDebugUnitTest --tests '*Sillage*Test' --tests '*CoverRepairCoordinatorTest'
```

Le catalogue compressé doit rester nommé `app/src/main/assets/sillage-catalogue.bin` : Android transforme les fichiers terminant par `.gz` pendant l’assemblage.

La version personnelle FOSS utilise une clé de développement. Conserver la même clé pour les futures mises à jour ; une compilation effectuée ailleurs utilisera une autre signature.

## Origine et licence

Personnalisation de [TachiyomiSY](https://github.com/jobobby04/TachiyomiSY), sous [licence Apache 2.0](LICENSE). Les notices de copyright originales sont conservées dans le code. FlexibleAdapter est intégré depuis le commit `c8013533` du [dépôt arkon/FlexibleAdapter](https://github.com/arkon/FlexibleAdapter), avec sa licence dans `vendor/flexible-adapter/LICENSE`, car son artefact JitPack était indisponible.

Les extensions sont distribuées par [Keiyoushi](https://github.com/keiyoushi/extensions). Les workflows de publication du projet d’origine ne sont pas activés dans ce dépôt.
