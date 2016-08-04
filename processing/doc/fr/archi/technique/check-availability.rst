Vérification de la disponibilité
################################

Algorithme
**********

1. Calcul de la taille totale des Objets + manifeste SEDA:
  1. Récupération du manifeste SEDA depuis le workspace.
  2. Parsing du manifeste pour calculer la taille totale des objets techniques contenus.
  3. Récupération depuis le Workspace, des informations sur le fichier manifeste SEDA dont sa taille.
  4. Calcul de la taille total (manifeste SEDA + objets techniques à stocker).

2. Comparaison capacité stockage VS taille totale
  1. Appel au moteur de stockage pour récupérer un Json contenant les informations de capacité pour un couple tenant/stratégie de stockage donné.
  2. Comparaison entre capacité retournée par le moteur de stockage et taille totale calculé précédemment
    2.1 Si capacité supérieure Alors Inscription dans logbook operation d'un OK
    2.2 Si capacité inférieure Alors Inscription dans logbook operation d'un KO, fin du process : "Disponibilité de l'offre de stockage insuffisante"
    2.3 Si un problème est rencontré (Offres non dispos, Server down, etc...) Alors Inscription dans logbook operation d'un KO, fin du process : "Offre de stockage non disponible"
