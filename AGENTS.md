# Validation des mises à jour Sillage

Instruction utilisateur du 14 septembre 2026 : vérifier le fonctionnement de la mise à jour depuis l’application avant de publier.

- Les tests unitaires, la signature de l’APK et un HTTP 200 ne suffisent pas à valider le parcours de mise à jour.
- Avant publication, télécharger intégralement le fichier de livraison, vérifier son empreinte et tester sur Android le parcours depuis une version précédente : détection, téléchargement complet, autorisation d’installation, installation par-dessus et conservation de la bibliothèque/progression.
- Tester également un téléchargement interrompu et une tentative d’installation avant sa fin. Un fichier partiel ne doit jamais être proposé à l’installation.
- Si aucun téléphone ou émulateur fonctionnel n’est disponible, signaler cette limite et garder la livraison non publiée jusqu’à validation de ce parcours. Ne pas annoncer que la mise à jour depuis l’application fonctionne sur la seule base des tests unitaires.
