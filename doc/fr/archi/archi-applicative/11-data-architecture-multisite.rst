Architecture des données & multisite
####################################


Inventaire des données
======================

.. seealso:: Le modèle de données complet est explicité dans la documentation externe dédiée ("Modèle de données").

Le tableau ci-dessous représente l'inventaire des données gérées par VITAM, avec leur localisation et le composant responsable du cycle de vie de la donnée (i.e. règles de création / modification / suppression) :

.. tabularcolumns:: |p{1cm}|p{4cm}|p{2cm}|p{1.5cm}|p{1.5cm}|p{1cm}|p{1cm}|p{1cm}|p{1cm}|

.. csv-table:: Inventaire des données VITAM
	:header-rows: 1
	:file: data/vitam-data-inventory.csv
	:class: longtable
	:delim: ,

Quelques remarques :

* si une donnée est persistée à plusieurs endroits, l'emplacement de référence (i.e. faisant foi en cas de désynchronisation entre les emplacements) est indiqué par le préfixe ``REF:``. Les processus de reconstruction ou de remise en cohérence de la solution logicielle s'appuient sur cet emplacement référentiel pour alimenter les autres emplacements de stockage. En particulier, les offres de stockage VITAM portent la référence des données concernant les archives hébergées par le système VITAM : leur contenu binaire (BDO), mais également les métadonnées associées au sens large (AU, GOT, journaux) et les référentiels métier
* les données de référence à l'origine du registre des fonds sont les journaux opération (JOP)
* il existe 2 types de journaux d'écriture :

    - le premier au niveau du moteur de stockage, qui permet de s'assurer de la bonne prise en compte des écritures par le système VITAM. Il s'agit d'un journal métier, participant à la preuve systémique (il est donc sécurisé comme les journaux d'opération et de cycle de vie des archives) ;
    - le deuxième au niveau de l'offre de stockage, qui permet de conserver l'ordre d'écriture des éléments stockés pour permettre leur rejeu lors d'une reconstruction (totale ou partielle). Il s'agit donc d'un journal technique, s'inspirant fortement du concept des archivelog des bases de données.


Multisite
=========

.. figure:: images/dualsite-architecture.*
    :align: center

    Architecture des données d'archives ; fonctionnement multisite.


Le fonctionnement multisite s'appuie fortement sur les capacités de reconstruction de VITAM :

* VITAM doit être déployé avec une stratégie de stockage comportant une offre de stockage sur chaque site ;
* Le fonctionnement de VITAM sur plusieurs sites fonctionne sur un principe actif / passif : 

    - le site principal fonctionne en mode nominal,
    - le site secondaire fonctionne en mode "reconstruction au fil de l'eau" (les tâches planifiées de sécurisation et d'audit sont arrêtées, les composants frontaux et de traitement de données sont arrêtés (en gris dans le schéma précédent), les tâches planifiées de reconstruction au fil de l'eau sont activées)

* Toute donnée liées aux archives est systématiquement écrite dans les offre de stockage (le cas échéant, en même temps que dans les bases de données), donc sur les 2 sites en même temps ;
* Sur le site secondaire, des processus viennent régulièrement récupérer les données écrites en dernier dans l'offre de stockage de ce site (en se basant sur le contenu des logs d'écriture de l'offre) pour alimenter en update le contenu des bases de données "secondaires" :

    - Référentiels : reconstruction régulière et totale
    - AU/GOT/BDO/Journaux/Graphe : reconstruction au fil de l’eau

En cas de perte du site primaire, l'intégralité des données est donc présente dans le stockage sur le site secondaire, et est presque entièrement reconstruit dans les bases de données du même site. Une fois la reconstruction complètement terminée, le site secondaire est donc accessible ; le niveau d'accessibilité dépendra de la stratégie de stockage sur le site secondaire :

* Soit la dégradation du niveau de résilience des offres est acceptée, et la stratégie de stockage devra être modifiée pour limiter les écritures à une seule offre.
* Soit cette stratégie continue à requérir l'écriture sur 2 offres de stockage, et le système ne sera accessible qu'en lecture seule ; seule une recréation de l'offre de stockage sur le site principal permettra le retour à un fonctionnement nominal (Cf. admonition ci-dessous). Ce scénario est délicat à implémenter, et nécessite notamment la mise en place d'un contrat d'accès spécifique permettant de bloquer les accès en modification.

.. caution:: La version courante de la solution logicielle VITAM ne permet pas la resynchronisation d'une offre de stockage à partir d'une autre offre de stockage. Ainsi la réplication de l'offre de stockage du site secondaire vers une nouvelle offre de stockage du site principal,  indispensable pour un retour à la normale une fois le PRA terminé, doit être réalisé par des moyens d'infrastructure uniquement (recopie de systèmes de fichiers, duplication d'un object storage, dump + restoration de la base MongoDB des offres de stockage, ...)

.. caution:: En cas de bascule de site, les traitements en cours sur le site 1 sont perdus ; en particulier, les ingests non terminés doivent être renvoyés à VITAM et les autres batchs en cours doivent être relancés. L’incohérence des données sera réglée dans une version ultérieure du système VITAM.



