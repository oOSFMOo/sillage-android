# Sillage Android 1.0 — installation

Cette APK contient réellement l’interface Sillage et son catalogue. Elle fonctionne sur Android 8 ou plus récent, même lorsque le PC est éteint.

## Installer

1. Ouvre **Sillage-Android-1.0.apk** sur le téléphone, puis accepte son installation. Le nom affiché est **Sillage**, avec une icône de livre turquoise.
2. Sillage s’installe à côté de TachiyomiSY. Garde l’ancienne application pour transférer et vérifier ta progression.
3. Au premier lancement, termine les étapes d’accueil (dossier de stockage et autorisations utiles). L’application s’ouvre ensuite sur **Catalogue**. Il est déjà inclus : aucun fichier « catalogue » à restaurer.
4. Les extensions déjà installées sur le téléphone peuvent être réutilisées. Ouvre **Catalogue → Sources et nouveautés → Gérer les extensions**, puis vérifie qu’elles sont disponibles et approuvées. Sinon installe les cinq APK du dossier **extensions** : MangaFire, Vortex Scans, King of Shojo, ManhuaTop et Rolia Scan.

## Explorer et lire

- Recherche dans le catalogue commun ; utilise **Trier et filtrer** pour les genres, la note et le nombre minimum de chapitres.
- Une fiche regroupée propose les différentes sources, classées par nombre de chapitres connu. Les notes disponibles sont moyennées. Les données absentes restent inconnues : aucune note n’est inventée.
- Ajoute tes séries avec le cœur. **Mes lectures** contient tes favoris ; **Historique** permet de reprendre la lecture.
- À l’ouverture d’une série, ses chapitres sont actualisés. Tu peux aussi tirer la fiche vers le bas pour relancer l’actualisation de cette seule série. En cas d’échec réseau, les chapitres déjà présents et la progression sont conservés.
- Les couvertures sont récupérées auprès des sources installées. Elles nécessitent du réseau la première fois. Une source indisponible ou non approuvée peut encore empêcher leur affichage.

## Ajouter une source et actualiser

Dans **Gérer les extensions**, installe une extension puis approuve-la si demandé. Si aucun dépôt n’est configuré, ajoute `https://github.com/keiyoushi/extensions/raw/repo/index.pb` dans les réglages des dépôts d’extensions.

Une nouvelle source installée déclenche son premier import. Ses pages de catalogue sont ajoutées progressivement avec les genres et les chapitres lorsque l’extension fournit ces informations. Les langues françaises, anglaises et celles activées dans tes réglages sont prises en compte. Un gros catalogue peut prendre longtemps ; Android peut différer sa reprise en arrière-plan. L’import conserve sa progression et les fiches déjà enregistrées.

Le bouton **Actualiser les nouveautés** lance une vérification manuelle du flux des publications récentes de chaque source. Il ne recommence pas le catalogue complet. Les séries favorites trouvées dans ces publications reçoivent aussi leurs nouveaux chapitres. **Sources et nouveautés** affiche l’état et le bouton **Actualiser / Réessayer** par source.

Pour une source préchargée, la première vérification établit son repère à partir des cinq premières pages récentes. Les suivantes parcourent le flux jusqu’au repère précédent. Ce suivi dépend de ce que le site expose : un changement ancien absent du flux ne peut pas être détecté par cette méthode. Une source sans flux de nouveautés est signalée et reste actualisable fiche par fiche. Aucune mise à jour globale périodique n’est activée par défaut.

## Récupérer ta progression

Dans l’ancienne application, crée une **sauvegarde complète** avec bibliothèque, chapitres, historique et catégories. Dans Sillage, utilise **Plus → Paramètres → Données et stockage → Restaurer une sauvegarde** et choisis cette sauvegarde personnelle.

L’ancien import massif est reconnu : les séries commencées et leurs données de lecture sont conservées dans Mes lectures. Les anciennes fiches jamais utilisées restent accessibles dans Catalogue. Un ancien favori jamais lu, sans catégorie personnelle, peut être impossible à distinguer d’une fiche ajoutée par l’import massif ; ajoute-le à nouveau avec le cœur.

Changer de source ouvre une autre édition. Pour transférer une progression entre éditions, utilise la fonction **Migrer** de la bibliothèque. Les téléchargements Windows ne sont pas transférés : télécharge les chapitres depuis le téléphone pour le hors connexion.

## Contrôles et limites de cette livraison

L’APK a été compilée avec succès ; 16 tests automatisés couvrent la migration des sauvegardes, la réparation des couvertures et le regroupement/filtrage du catalogue. Le démarrage sur un téléphone réel et le fonctionnement des cinq sources sur son réseau restent à confirmer : aucun téléphone n’était connecté et l’émulateur local n’a pas démarré correctement.

Le catalogue de départ contient 81 527 fiches regroupées provenant de Windows. Certaines métadonnées n’étaient pas renseignées dans cet export et restent inconnues jusqu’à leur récupération. L’application est indépendante de Windows ; elle ne synchronise pas automatiquement sa progression avec le PC.

Les anciens fichiers, incompatibles avec ce fonctionnement, ont été rangés dans **Ancienne-version-ne-pas-installer**.
