# Sillage Android

Une bibliothèque de manhua et webtoons pour lire sur Android, sans dépendre d’un PC allumé. Interface française et catalogue commun, personnalisés à partir de TachiyomiSY 1.13.2.

## Télécharger

**[Télécharger la version Android 1.4](https://github.com/oOSFMOo/sillage-android/releases/tag/v1.4.0)**

Dans les fichiers de la version, prends **Sillage-Android-1.4.apk**. Ouvre-le pour mettre à jour Sillage sans désinstaller l’application. Les extensions déjà installées restent utilisables. Depuis la version 1.2, **Plus → À propos → Vérifier les mises à jour de Sillage** permet de récupérer les prochaines versions depuis GitHub.

[Guide d’installation et de transfert de progression](INSTALLATION.md)

## Fonctionnalités

- Préchargement temporaire glissant de la série lue : chapitre courant puis dix chapitres non lus en avance, en ordre, avec priorité aux demandes du lecteur. Réglage dans **Plus → Paramètres → Lecteur → Chapitres à précharger en avance** : désactivé, 1, 3, 5 ou 10. Fonctionne aussi sur les données mobiles.
- Indicateur de disponibilité dans les commandes du lecteur. Réserve limitée à 1 Go et à l’espace disponible ; les dix chapitres constituent un objectif dépendant du réseau et du site.
- Nettoyage du cache des chapitres terminés lorsque tu passes au suivant ou fermes le lecteur. Expiration après deux heures hors de l’application, exécutée selon la planification Android et vérifiée au retour. Les téléchargements conservés ne sont pas supprimés.
- **Ignorer les chapitres en double** activé automatiquement à la mise à jour et désactivable dans les réglages du lecteur. Les variantes restent disponibles dans la fiche ; les numéros inconnus et fractionnaires restent distincts.
- Asura Scans en anglais intégré, avec import initial automatique des séries exposées par le site. Aucune APK d’extension supplémentaire pour cette source ; seuls les chapitres accessibles publiquement sont proposés.
- Accès aux dernières lectures depuis le catalogue, recommandations par genres communs et accès aux autres sources depuis une série.
- Téléchargement pour les trajets : 5 ou 10 chapitres, choix 25/50/100 ou quantité personnalisée de 1 à 500 depuis la reprise de lecture. Vérifier la fin de la file avant de partir hors connexion.
- Onglet **Chapitres** pour les nouveaux chapitres des favoris, regroupés par série, avec explication et vérification manuelle si la liste est vide.
- État détaillé des imports, avertissement sur leur durée et boutons pour mettre en pause ou reprendre une source. La navigation et la lecture restent accessibles pendant l’import, avec un ralentissement possible.
- Vérification des nouvelles versions de Sillage au lancement (au plus une fois par jour) et bouton manuel dans À propos. Téléchargement uniquement après acceptation, contrôle de l’identifiant et de la signature, puis confirmation de l’installation par Android.

- Catalogue initial de 81 527 fiches regroupées ; les favoris sont choisis séparément.
- Recherche, filtres par genre et minimum de chapitres ; classement par chapitres, note ou titre.
- Différentes éditions accessibles depuis une même fiche, avec la plus complète en premier selon les nombres connus ; moyenne des notes disponibles.
- Récupération des couvertures depuis les sources Android.
- Import initial automatique d’une nouvelle extension installée et approuvée, avec reprise en cas d’interruption.
- Bouton global de recherche des nouveautés dans les flux récents, sans recommencer le catalogue complet.
- Actualisation des chapitres à l’ouverture de chaque série, favoris, historique et lecture hors connexion après téléchargement.
- Reprise de lecture basée sur le dernier chapitre réellement consulté : un chapitre 30 en cours ne revient plus au chapitre 1 non lu.
- Vérification automatique des favoris tous les 7 jours, avec bouton manuel et heure/résultat détaillés.
- Tous les genres disponibles sont listés par importance, avec leur nombre de séries.

## État de la version

Cette version est destinée à être testée. Les tests automatisés couvrent le décodage du catalogue complet, la conservation des métadonnées lors d’une panne, le regroupement/filtrage, la migration des lectures, la reprise au dernier chapitre consulté et le repère des nouveautés. Les résultats de validation de l’APK figurent dans les notes de publication. Le test sur téléphone réel n’a pas encore été réalisé.

Les sites et extensions peuvent changer ou être indisponibles. Certaines métadonnées du catalogue initial sont encore inconnues. Les nouvelles notes ne peuvent être renseignées que si elles sont fournies ; aucune note n’est inventée. L’import initial parcourt le catalogue exposé par l’extension (pages populaires) : son exhaustivité dépend de la source. La première vérification d’une source préchargée établit un repère sur ses cinq pages récentes ; les suivantes recherchent tous les éléments de ce repère, avec au moins deux pages et une limite de dix pages récentes. Atteindre cette limite sans retrouver le repère est signalé comme partiel. Un changement absent du flux reste à vérifier sur la fiche.

L’application et le catalogue ne contiennent pas les images des chapitres. Les sources sont nécessaires pour les consulter. La progression Android n’est pas synchronisée avec Windows.

L’anti-doublons agit sur l’enchaînement du lecteur selon les numéros fournis par la source. Il conserve la variante ouverte et privilégie ensuite le même groupe de traduction. Il ne fusionne ni ne supprime les variantes dans la base de données.

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
