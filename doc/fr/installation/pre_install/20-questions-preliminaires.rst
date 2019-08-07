Questions préparatoires
########################

La solution logicielle :term:`VITAM` permet de répondre à différents besoins. 

Afin d'y répondre de la façon la plus adéquate et afin de configurer correctement le déploiement :term:`VITAM`, il est nécessaire de se poser en amont les questions suivantes : 


- Questions techniques : 
    - Topologie de déploiement et dimensionnement de l'environnement ? 
    - Espace de stockage (volumétrie métier cible, techologie d'offre de stockage, etc.) ? 
    - Sécurisation des flux http (récupération des clés publiques des servcies versants, sécurisation des flux d'accès aux offres, etc.) ? 

- Questions liées au métier : 
    - Nombre de tenants souhaités (hormis les tenant 0 et 1 qui font respectivement office de tenant "blanc" et de tenant d'administration) ?
    - Niveau de classification (la plate-forme est-elle "Secret Défense" ?) 
    - Modalités d’indexation des règles de gestion des unités archivistiques (autrement dit, sur quels tenant le recalcul des inheritedRules doit-il être fait complètement / partiellement) ? 
    - Greffons de préservations (`griffins`) nécessaires ? 
    - Fréquence de calcul de l’état des fonds symboliques souhaitée ? 
    - Définition des habilitations (profil de sécurité, contextes applicatifs, ...) ? 
    - Modalités de gestion des données de référence (maître/esclave) pour chaque tenant ?

..    - Faut-il y historiser les mises à jour du classifié ? 

Par la suite, les réponses apportées vous permettront de configurer le déploiement par la définition des paramètres ansible. 