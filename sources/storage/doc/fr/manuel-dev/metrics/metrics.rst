Métriques
##########

Introduction
************
Dans ce qui suit la liste de métriques développées pour ce composant.

.. note::
    Pour avoir plus d'informations sur la partie développement des métriques prometheus, veuillez vous référer à la documentation du composant **Common Cf. vitam-mertics.rst**

.. warning::
    La classe fr.gouv.vitam.common.metrics.VitamMetricsNames liste toutes les métriques prometheus. Si vous rajoutez une nouvelle métrique, pensez à mettre à jour cette classe.

Liste des métriques
*******************
* vitam_storage_download_size_bytes : Données en octets téléchargées par le composant `vitam-storage-engine` depuis les offres de stockages.
    > Cette métrique est de type `Summary`
    > Cette métrique dispose des labels (tenant, strategy, offer_id, data_category, origin)
        - "tenant": Le tenant depuis lequel la demande de télécharegement a était faite
        - "strategy" : La stratégie de stockage utilisée lors de ce téléchargement/lecture
        - "offer_id": L'identifiant de l'offre depuis laquelle les données sont téléchargées
        - "data_category": La catégorie des données téléchargées (objet, unit, ...)
        - "origin": L'origin de l'action de téléchargement (offer_sync, normal, bulk)

    > Total des opérations de téléchargement tout type confondu:
        TODO

    > Total des opérations de téléchargement par tenant et par stratégie. Cette requête peut être utilisée pour determiner la moyenne des téléchargemets par tenant et par stratégie:
        TODO


* vitam_storage_upload_size_bytes : Données en octets téléversées par le composant `vitam-storage-engine` vers les offres de stockages.
    > Cette métrique est de type `Summary`
    > Cette métrique dispose des labels (tenant, strategy, offer_id, data_category, origin, attempt)
        - "tenant": Le tenant depuis lequel la demande a était faite
        - "strategy" : La stratégie de stockage utilisée lors de ce téléversement
        - "offer_id": L'identifiant de l'offre vers laquelle les données sont téléversées
        - "data_category": La catégorie des données téléversées (OBJECT, UNIT, ...)
        - "origin": L'origin de l'action de téléversement (normal, traceability, offer_sync)
        - "attempt": Le numéro d'essai pour le téléversement. Dans le cas d'absence d'erreurs technique, la valeur généralement sera de 1.

    > Total des opérations de téléversement tout type confondu:
        TODO

    > Total des opérations de téléversement par tenant et par stratégie. Cette requête peut être utilisée pour determiner la moyenne des téléversements par tenant et par stratégie:
        TODO

    > Total des opérations de téléversement par tenant et par stratégie et par data_category = OBJECT. Cette requête peut être utilisée pour determiner la moyenne des téléversements des binaires uniquement:
        TODO