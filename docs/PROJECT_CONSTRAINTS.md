# Contraintes projet NFT

## Donnees et securite

- Ne pas afficher de mocks ou de fausses donnees comme si elles venaient du backend.
- Filtrer les souvenirs cote backend par utilisateur connecte.
- Autoriser l'acces a un souvenir uniquement au proprietaire ou a un collaborateur explicite.
- Creer les changements de base de donnees via migrations, jamais a la main.

## Creation de souvenir

- La creation doit survivre a la navigation et au refresh avec un statut persistant.
- Statuts attendus : `uploading`, `preferences`, `generating`, `done`, `error`.
- Le bouton principal doit revenir vers l'etape active et afficher un loader pendant une creation.

## Imports et contribution

- Aucun bouton decoratif : chaque import doit fonctionner ou afficher une erreur claire de configuration.
- Google Drive et Google Photos utilisent OAuth lorsque les variables Google sont configurees.
- iCloud utilise un fallback fichier explicite, car il n'existe pas de picker web public complet.
- Les liens contributeurs sont publics, tokenises, expirables et limites aux uploads photo.

## Internationalisation

- Langues supportees : francais, anglais, espagnol, portugais.
- Francais par defaut.
- La langue choisie doit etre sauvegardee dans le profil et appliquee apres refresh/reconnexion.

## Validation

- Verifier build front et compilation backend quand l'environnement local le permet.
- Verifier responsive mobile, loading/error/success states, orthographe visible et coherence UI avant presentation.
- Verifier qu'aucun bouton visible n'est decoratif : chaque action doit fonctionner ou afficher une erreur de configuration claire.
- Valider en priorite mobile first : tailles de police, espacements, alignements, cards, boutons et micro-interactions sobres.
- Francais par defaut pour l'interface multilingue, avec libelles visibles accentues et formulations naturelles.
- Ne jamais modifier la base de donnees a la main : toute evolution passe par un fichier de migration.
