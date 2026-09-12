# Sillage Android 1.2 — installation et mise à jour

Cette APK contient réellement l’interface Sillage et son catalogue. Elle fonctionne sur Android 8 ou plus récent, même lorsque le PC est éteint.

## Installer

1. Ouvre **Sillage-Android-1.2.apk** sur le téléphone, puis accepte sa mise à jour. **Ne désinstalle pas Sillage** : la mise à jour conserve la bibliothèque, les réglages et la progression. Une sauvegarde complète dans Plus → Paramètres → Données et stockage reste conseillée avant toute mise à jour.
2. Sillage s’installe à côté de TachiyomiSY. Garde l’ancienne application pour transférer et vérifier ta progression.
3. Au premier lancement, termine les étapes d’accueil (dossier de stockage et autorisations utiles). L’application s’ouvre ensuite sur **Catalogue**. Il est déjà inclus : aucun fichier « catalogue » à restaurer.
4. Les extensions déjà installées sur le téléphone peuvent être réutilisées. Ouvre **Catalogue → Sources et résultats → Gérer les extensions**, puis vérifie qu’elles sont disponibles et approuvées. Pour une première installation, les cinq APK d’extensions restent disponibles dans le paquet de la version 1.0 sur GitHub : MangaFire, Vortex Scans, King of Shojo, ManhuaTop et Rolia Scan.

## Mettre à jour Sillage depuis l’application

Dans **Plus → À propos**, touche **Vérifier les mises à jour de Sillage**. Si une version plus récente est publiée sur notre dépôt GitHub, l’application affiche ses nouveautés et propose son téléchargement. Les préversions publiques Sillage sont incluses. Aucun compte GitHub ni abonnement n’est nécessaire.

Une vérification a aussi lieu au lancement, au plus une fois par jour. Rien n’est téléchargé sans acceptation. Après téléchargement, touche **Installer** dans la notification, ou **Installer la mise à jour téléchargée** dans À propos si les notifications sont désactivées. Android demande ta confirmation et peut demander d’autoriser Sillage à installer des applications. Le fichier est contrôlé : il doit avoir le même identifiant et la même signature, et un numéro de version supérieur.

La version 1.2 doit être installée une première fois depuis le lien GitHub, car les versions précédentes ne disposent pas de ce bouton.

## Explorer et lire

- Recherche dans le catalogue commun ; utilise **Trier et filtrer** pour les genres, la note et le nombre minimum de chapitres.
- Une fiche regroupée propose les différentes sources, classées par nombre de chapitres connu. Les notes disponibles sont moyennées. Les données absentes restent inconnues : aucune note n’est inventée.
- Ajoute tes séries avec le cœur. **Mes lectures** contient tes favoris ; **Historique** permet de reprendre la lecture.
- À l’ouverture d’une série, ses chapitres sont actualisés. Le bouton **Vérifier les nouveaux chapitres** permet de relancer cette seule série ; un message affiche le résultat avec l’heure. Les favoris sont aussi vérifiés tous les 7 jours, selon les contraintes réseau et batterie d’Android. En cas d’échec réseau, les chapitres déjà présents et la progression sont conservés.
- **Reprendre** revient au dernier chapitre consulté s’il est en cours, ou au chapitre non lu suivant s’il est terminé. Les chapitres non lus avant ton point de départ ne te renvoient plus au début.
- Les couvertures sont récupérées auprès des sources installées. Elles nécessitent du réseau la première fois. Une source indisponible ou non approuvée peut encore empêcher leur affichage.

## Ajouter une source et actualiser

Dans **Gérer les extensions**, installe une extension puis approuve-la si demandé. Si aucun dépôt n’est configuré, ajoute `https://github.com/keiyoushi/extensions/raw/repo/index.pb` dans les réglages des dépôts d’extensions.

Une nouvelle source installée déclenche son premier import. Ses pages de catalogue sont ajoutées progressivement avec les genres et les chapitres lorsque l’extension fournit ces informations. Les langues françaises, anglaises et celles activées dans tes réglages sont prises en compte. Un gros catalogue peut prendre longtemps ; Android peut différer sa reprise en arrière-plan. L’import conserve sa progression et les fiches déjà enregistrées.

Le bouton **Actualiser les nouveautés** lance une vérification manuelle du flux des publications récentes de chaque source. Il ne recommence pas le catalogue complet. Les séries favorites trouvées dans ces publications reçoivent aussi leurs nouveaux chapitres. **Sources et nouveautés** affiche l’état et le bouton **Actualiser / Réessayer** par source.

Pour une source préchargée, la première vérification établit son repère à partir des cinq premières pages récentes. Les suivantes recherchent tous les éléments du repère précédent, en consultant au moins deux pages et au maximum dix pages récentes. Une limite atteinte, une fiche incomplète ou une source sans flux est signalée. Ce suivi dépend du site : un changement absent du flux reste à vérifier sur la fiche. Le catalogue global s’actualise manuellement ; la vérification automatique tous les 7 jours concerne les favoris.

## Récupérer ta progression

Dans l’ancienne application, crée une **sauvegarde complète** avec bibliothèque, chapitres, historique et catégories. Dans Sillage, utilise **Plus → Paramètres → Données et stockage → Restaurer une sauvegarde** et choisis cette sauvegarde personnelle.

L’ancien import massif est reconnu : les séries commencées et leurs données de lecture sont conservées dans Mes lectures. Les anciennes fiches jamais utilisées restent accessibles dans Catalogue. Un ancien favori jamais lu, sans catégorie personnelle, peut être impossible à distinguer d’une fiche ajoutée par l’import massif ; ajoute-le à nouveau avec le cœur.

Changer de source ouvre une autre édition. Pour transférer une progression entre éditions, utilise la fonction **Migrer** de la bibliothèque. Les téléchargements Windows ne sont pas transférés : télécharge les chapitres depuis le téléphone pour le hors connexion.

## Contrôles et limites de cette livraison

Les résultats de compilation, de tests et de vérification de la signature sont indiqués dans les notes de publication GitHub. Le démarrage sur un téléphone réel et le fonctionnement des cinq sources sur son réseau restent à confirmer : aucun téléphone n’était connecté et l’émulateur local n’a pas démarré correctement.

Le catalogue de départ contient 81 527 fiches regroupées provenant de Windows. Certaines métadonnées n’étaient pas renseignées dans cet export et restent inconnues jusqu’à leur récupération. L’application est indépendante de Windows ; elle ne synchronise pas automatiquement sa progression avec le PC.

Les anciens fichiers, incompatibles avec ce fonctionnement, ont été rangés dans **Ancienne-version-ne-pas-installer**.
