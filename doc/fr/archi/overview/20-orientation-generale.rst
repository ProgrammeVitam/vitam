Orientations générales
######################

.. warning:: Ces orientations générales donnent la direction vers laquelle tend la solution logicielle  :term:`VITAM` ; il contient donc des références à des fonctionnalités qui ne sont pas forcément présentes dans cette version du système :term:`VITAM`.

Open Source
===========

Les logiciels utilisés et le résultat sont *Open Source* afin de faciliter la réutilisation et d'éviter les contraintes de marchés publics pour la réutilisation au sein des différentes entités publiques.

Le logiciel produit est un logiciel de *Back-office*, supposant qu'il y a donc des *Front-offices* développés par ailleurs.

Le *back-office* se veut être mutualisé entre plusieurs *Front-offices*, pour :

* Permettre la réutilisation des données (objets numériques et métadonnées) dans plusieurs contextes (mémoire de l'entité publique)
* Permettre la réduction des coûts en centralisant les investissements.

Chaque *front-office* aura des conditions particulières d'usage du *back-office*. Ces conditions particulières pourront varier selon :

* La nature des grandes opérations qui pourront être effectuées (versement, accès, gestion, …)
* La nature des variantes d'opérations qui pourront être effectuées (ajout d'une entrée, modification d'une entrée ou de métadonnées, …) : en résumé lecture seule, écriture simple (insertion), écriture riche (mise à jour), effacement
* Le domaine d'application de ces opérations (quelles filières, quels objets numériques, quels périmètres)
* Le filtrage sur ces domaines (2 niveaux : habilité / non habilité, utilisables en fonction de règles de gestion : communicabilité, diffusion, données publiques/privées, …)

Même si :term:`VITAM` est un *back-office*, certaines :term:`IHM` sont prévues pour différentes fonctions :

* :term:`IHM` d'administration : pour les opérations d'administration (métier) à accès restreint. Selon le *front-office* utilisé, cette :term:`IHM` peut ne pas être nécessaire ;
* :term:`IHM` minimale : elle assure un socle minimal d':term:`IHM` pour assurer un usage rapide de la solution logicielle :term:`VITAM`. Cette :term:`IHM` est prévue pour être utilisée dans les cas simples, et donc, selon le front-office utilisé, elle peut ne pas être nécessaire ;
* :term:`IHM` de démonstration : elle porte des exemples d'implémentations limitatives tant en fonctionnalité qu'en garantie de fonctionnement. Ces :term:`IHM` ne doivent pas être mises en production mais sont des exemples dont peuvent s'inspirer les concepteurs d'applications *front-offices*.

	- Cette :term:`IHM` porte notamment des codes de démonstration, des cas particulier d'exemples pour de futures implémentations de front-offices, mais uniquement sur un aspect codage (requêtes et réponses) pour illustrer des cas d'usages.


:term:`API` :term:`REST`
========================

Pour assurer l'interconnexion entre le *back-office* et les *front-offices*, il est proposé d'utiliser des interfaces HTTPS :term:`REST` (hors protocoles spécifiques additionnels de transferts de fichiers). Ainsi, toutes les fonctionnalités accessibles aux *front-offices* seront offertes via ces :term:`API`. Les :term:`IHM` minimales et de démonstration utiliseront ces :term:`API`. Les :term:`IHM` d'administration pourront utiliser des API spécifiques si nécessaire (mais ce n'est pas une obligation, ces API pouvant elles aussi être exposées in fine).

Une analogie peut être faite entre :term:`VITAM` et une base de données :

* Une base de données peut héberger une ou plusieurs tables communes à de multiples applications clientes ;
* Les applications clientes utilisent des :term:`API` (SQL) pour échanger avec le moteur de la base de données ;
* La base de données dispose d'une :term:`IHM` spécifique d'administration pouvant utiliser les mêmes :term:`API` (SQL) ou des API spécifiques du moteur.


Big Data et Cloud computing
===========================

Les contraintes de volumétrie (plusieurs dizaines de milliards d'objets) conduisent à une volumétrie (en nombre) dépassant les capacités des logiciels usuels (type :term:`SGBDR`). Les technologies :term:`NoSQL` ou Cloud computing à forte distribution permettent de pallier ce problème.

Pour chaque objet numérique, les métadonnées associées sont variables ([Nom, Prénom, ...] pour un dossier RH, [Projet, Domaine, ...] pour un dossier projet, [Action, Plan comptable, … ] pour de la comptabilité, …). Cette variabilité peut être assumée par des technologies NoSQL dites *schemaless*.

Chaque objet numérique peut être d'un format différent (Word, PDF, JPG, AVI, …). La lisibilité dans le temps d'un objet numérique est un enjeu majeur. Si on ne peut plus le lire (le consulter par exemple), il n'est plus compréhensible par l'humain. Les transformations de format pour en assurer la lisibilité sont donc indispensables dans le temps. Du fait de la masse (dizaines de milliards d'objets numériques), cette contrainte impose de gérer une vélocité de grande masse.

Du fait de la prolifération des formats et des usages (usage dit de master pour la conservation, usage dit de diffusion dans une qualité moindre, voire d'autres usages comme une qualité de type vignette ou contenu textuel (TEXTE)), ces formats induisent eux aussi une grande variabilité qui doit être gérée de manière efficiente (vélocité).

De plus, l'accès aux métadonnées ou aux objets numériques doit pouvoir se faire dans des temps acceptables (de la seconde à quelques dizaines de secondes pour certains éléments massifs). Là aussi, la vélocité est donc un point important.

Ces 3 V (Volume, Variété, Vélocité) imposent une vision "Big Data" mais non analytique (*Big Data* de traitements). Il n'est pas prévu par exemple de pouvoir effectuer des traitements de masse sur le contenu des archives et d'en déduire des analyses statistiques ou d'utiliser des mécanismes d'intelligence artificielle. Ainsi le modèle Hadoop ne correspond pas à notre usage.


Cloud storage
=============

La particularité de l'accès aux objets numériques est un accès unitaire à minima (l'accès à un lot se résumant à faire des accès unitaires pour chacun des éléments de ce lot). Ainsi, on accède à un courriel et non uniquement à une boîte aux lettres. De ce fait, chaque objet étant accédé unitairement, la logique retenue pour le stockage est une logique Objet (:term:`CAS`) et non une logique systèmes de fichiers. L'implémentation réelle peut s'appuyer sur une logique de systèmes de fichiers, mais l'interface visible sera bien objet. Le modèle de référence (ce qui ne veut pas dire l'implémentation réelle ni l'interface exacte) s'inspire de la NF Z 42-020 et du modèle :term:`Swift` ou *CEPH*. L'avantage des deux dernières technologies est qu'elles permettent d'envisager un modèle qui peut croître en taille sans avoir à tout changer à chaque fois. Il s'agit donc du modèle *Cloud Storage*.

.. note:: Les notions de *Cloud computing* ou *Cloud Storage* ne sont pas à prendre au sens hébergement chez Amazon, Google ou Azure, mais au sens des technologies sous-jacentes.

Par contre, il doit être possible de regrouper logiquement des unités en lots (des courriels d'une boîte aux lettres) afin d'en faciliter l'accès. Comme il s'agit de regroupement logique, et que pour une même unité, plusieurs regroupements peuvent être envisagés (un courriel classé dans une boîte, et ce même courriel classé dans un dossier d'affaire), c'est une vision arborescente (dossiers, sous-dossiers, tout comme une arborescence de répertoires contenant des fichiers) disjointe des objets numériques qui est mise en oeuvre. Celle-ci s'inspire du modèle :term:`IsaDG`, :term:`EAD`, :term:`SEDA` mais aussi du modèle :term:`MoReq` 2010. Il a conduit à la notion d' "unités d'archives" (ou Units) structurés dans une arborescence (plan de classement).

Cette façon de distinguer ce qui est porté dans l'arbre de métadonnées (le classement) et dans le stockage (les objets unitaires) permet de faciliter le développement différencié des deux en en réduisant la complexité pour chacun, ce qui permet d'envisager le remplacement plus facilement de telle ou telle partie, et en particulier pour le stockage, d'autoriser d'autres implémentations.


PCA/PRA et répartitions des travaux
===================================

La solution logicielle  :term:`VITAM` est prévue pour être installée sur un nombre de sites suffisant pour assurer la sécurité des données. Selon les volumétries, le nombre de sites peut être variable :

* 1 site : Ce cas ne peut concerner que des volumes de très petite taille dont la sauvegarde journalière et complète ("Full daily backup") est permise et réaliste, ainsi que l'acceptation d'un délai de remise en oeuvre de quelques jours. Ceci n'empêche pas la mise en oeuvre de sauvegarde différentielle, mais donne une limite raisonnable d'application du modèle. Une version particulière de Vitam nommée mini-Vitam devrait permettre une telle mise en oeuvre mais avec des fonctionnalités amoindries pour tenir sur un ensemble limité de serveurs ;
* 2 sites : Ce cas peut être acceptable tant qu'un plan de sauvegarde traditionnel des volumétries est applicable (moins d'un To a priori) via, par exemple, un schéma de sauvegarde de type "Full backup" hebdomadaire et "Incremental backup" journalier. Il s'agit de la réplication des architectures usuelles pour les applications informatiques. Le second site est considéré comme le site de Plan de Reprise d'Activité (PRA) ;
* 3 sites : Ce cas devrait être le plus général, car il permet de couvrir les volumes les plus importants (plusieurs centaines de To ou plus) où les moyens de sauvegarde usuels ne fonctionnent plus, tout en assurant la sécurité. En cas de sinistre sur un site, le deuxième site "chaud" permet de redémarrer rapidement le service. En cas d'incident après un sinistre, le 3 :sup:`ème` site assure la sécurité des données, comme une sauvegarde classique le ferait.


Sécurité des données additionnelle
==================================

Chaque offre de stockage doit répondre aux enjeux définis dans la norme "NF Z 42-020" (:term:`CCFN`).

La recommandation en termes de sécurité est d'avoir au moins 3 copies d'une même archive, réparties sur au moins 3 sites pour des raisons de sécurités géographiques (en limitant l'impact de sinistres impliquant la disparition d'un site de production) et sur au moins 2 types de stockage de natures distinctes.

* Le recours à plusieurs offres de stockage permet d'assurer une meilleure résilience : une attaque, une faille de sécurité ou un défaut d'usure sont liés à la technologie utilisée ; varier les technologies tend à diminuer ce risque (comme il est d'usage de le faire par exemple avec les solutions de sécurité) ;
* Plusieurs offres de stockage doivent être supportées simultanément par le logiciel Vitam afin de permettre les migrations dans le temps entre les offres (tous les 5 à 10 ans selon les technologies utilisées) ;
* Des implémentations d'offres de stockages réalisées par exemple par des acteurs privés en dehors du Programme Vitam (constructeur, éditeur, etc.) pourront venir compléter ou remplacer les solutions proposées par le programme Vitam, ceci permettant d'offrir à Vitam une meilleure capacité à résister dans le temps par la multiplicité des choix proposés. Une illustration de l'architecture de stockage est présentée ci-après.
* Plusieurs offres de stockage permettent de servir plusieurs niveaux de services :

   - Par exemple des accès rapides pour les accès aux versions de diffusion des archives, et à l'inverse des accès lents pour les accès aux originaux (masters) potentiellement plus volumineux ; à l'instar de la vidéo en mode HDV pouvant être considérée comme le format "master" mais non diffusable du fait de sa taille – 3 Mo/s environ, soit plus de 11 Go/h – qui serait stockée sur des supports lents, tandis que le format Xvid – 500 Mo/h – serait utilisé pour la diffusion et servi par des supports rapides ;
   - Ces niveaux de services différents permettent aussi de répondre à des exigences de sécurité (résilience par rapport à une autre offre). Il est proposé ainsi la mise en oeuvre de deux niveaux de services majeurs pour offrir un délai complémentaire de réactivité et éviter ainsi des destructions d'archives (suite à un incident, une attaque ou un défaut) :

      + Via une offre dénommée "stockage primaire" (ou secondaire en secours immédiat ou "chaud") servant aux accès rapides mais pouvant subir des éliminations tout aussi rapides (et donc dangereuses en termes de sécurité) ;
      + Et l'autre dénommée "stockage de sécurité", lent par nature (et même si possible "offline" ou "froid") dont les propriétés d'accès rendent lentes les opérations d'écriture et d'élimination.


Architecture multi-tenants
==========================

La solution logicielle  :term:`VITAM` doit pouvoir être instanciée sur une infrastructure mutualisée, avec une administration centralisée et unique des composants, mais en autorisant une séparation virtuelle et sécurisée des informations (archives et métadonnées) pour chaque client (client = "tenant" en anglais) ainsi que la gestion séparée de ces informations par chaque client.

L'objectif est de permettre, par exemple, le regroupement d'acteurs publics au sein d'une même infrastructure pour diminuer les coûts d'infrastructure et d'exploitation, tout en assurant une étanchéité entre ces environnements logiques pour chaque client.


Solution exploitable
====================

Du fait de la complexité des composants à mettre en oeuvre et donc de l'exploitation associée, tant par composant que dans des visions de suivi d'opérations, la solution logicielle :term:`VITAM` doit apporter un maximum d'aides et de facilités aux administrateurs techniques et exploitants, sans forcément se substituer aux outils d'administration propres à chacun des composants.

Ainsi, il est nécessaire de disposer d'un outillage permettant la configuration, l'installation et la mise à jour des composants et des services pour une plate-forme :term:`VITAM`.

Il est également nécessaire de disposer d'un outillage permettant de suivre l'activité du système global :

* Gestion des logs centralisée
* Suivi des opérations ou d'une opération en cours
* Planification

Il n'est pas obligatoire de substituer des outils d'administration d'un composant lorsqu'ils existent déjà :

* Administration d'une base MongoDB ou d'une base ElasticSearch
* Supervision technique des :term:`VM` et :term:`OS`, du réseau,...

Par contre, certaines informations utiles (soit pour le déroulement d'une opération comme la charge CPU d'un serveur, soit pour une vision globale de l'activité comme la charge CPU ou réseau de la plate-forme) pourraient être captées par la solution logicielle :term:`VITAM` pour ses propres usages (et donner de l'information à l'administrateur technique).
