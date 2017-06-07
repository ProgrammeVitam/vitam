Rangement des objets
####################

Algorithme
**********

1. Mise à jour du journal de cycle de vie du groupe d'objet
2. Récupération des informations d'objet technique :
  1. Récupération du groupe d'objet dans le workspace
  2. Parsing du SEDA pour identifier les chemins dans le workspace des objets technique contenus dans le groupe d'objets (à terme il faudra éviter de refaire un parsing SEDA)
3. Pour chaque objet technique :
  1. Mise à jour du journal de cycle de vie du groupe d'objet avec le stockage de l'objet
  2. Stockage de l'objet
  3. Commit du journal de cycle de vie du groupe d'objet avec le stockage de l'objet
4. Commit du journal de cycle de vie du groupe d'objet

