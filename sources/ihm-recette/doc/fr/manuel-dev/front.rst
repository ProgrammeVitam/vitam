IHM Front
#########

Cette documentation décrit la partie front/Angular de l'IHM et en particulier sa configuration et ses idéologies architecturales
--------------------------------------------------------------------------------------------------------------------------------

Utils et général / Composition du projet Angular
************************************************

Voici l'architecture des dossiers et fichiers composant l'application front IHM-recette (à partir de ihm-recette/ihm-recette-web-front)

- **e2e**           Pour le moment inutilisé, pourra être utilisé pour les tests d'intégration end 2 end
- **src**           Dossier contenant les sources du projet

    - **app**           Dossier contenant les sources typescript des composants du projet
    - **assets**        Dossier contenant des fichiers statiques utilisés dans l'IHM recette (images, polices, ...)
    - **deb**           Dossier contenant des fichiers spécifiques utiles à la génération des packages debian
    - **environments**  Dossier contenant des règles spécifiques utilisés pour les builds en dev ou en prod
    - *styles.css*      Feuille de styles globale des composants de l'application.
      Les modifications sur ce fichiers doivent EXCLUSIVEMENT être faites à partir du build de theme.scss
      (Voir Build CSS)
    - *main.ts*         Point d'entrée de l'application
    - *test.ts*         Fichier de configuration des tests unitaires définissant entre autre les fichiers à inclure dans les tests.
- **themes**        Dossier contenant les pattern du thème vitam
    - **vitam-red**     Dossier contenant le thème rouge pour l'IHM recette
        - *theme.scss*      Fichier définissant des couleurs et des règles spécifiques au thème rouge pour l'IHM recette
    - *_theme.scss*     Fichier définissant un template commun à tous les thèmes pour générer les thèmes vitam.
- *karma.conf.js*   Fichier de configuration du framework de lancement des tests unitaires.
- *package.json*    Fichier de configuration des dépendances npm et des scripts utilisés pour les builds (dev/prod/tsts/...)
- *pom.xml*         Fichier de configuration du build maven
- *proxy.conf.json* Fichier de configuration du proxy utilisé sur poste de dev. Utilisé dans un des scripts du package.json.
- *tslint.json*     Fichier de configuration du formatage des fichiers typeScript.
- *zip-conf.xml*    Fichier de configuration du packaging du build front. Utilisé dans le pom.xml.

Voici l'architecture *théorique* des composants de l'application (à partir de ihm-recette/ihm-recette-web-front/src/app)

- **common** Dossier contenant les composants globaux réutilisables pour l'ensemble des pages.

  Cela peut par exemple être le bandeau du menu ou un service de gestion des requêtes HTTP.

    - **componant-name**        Dossier contenant un composant global tel que le menu, le fil d'ariane ou encore un composant d'affichage des données.
    - *service-name.service.ts* Fichier contenant un service utilitaire
    - *class-name.ts*           Fichier contenant une classe utilisée dans plusieurs composants
- **theme1** Dossier contenant des composant sur un même theme (Pour l'ihm recette, nous aurons le theme d'administration, de sécurisation et de tests).
    - **page1**             Dossier contenant le composant d'une des pages de l'application.

      Ce composant à des particularités spécifiques (Voir "Composant de Page")

        - **component1**            Dossier contenant un des sous-composant utilisé dans la page1.
          Ce composant à des particularités spécifiques (Voir "Sous Composant")
        - *page1.component.css*     Fichier contenant le style spécifique au composant page1. Le style définit ici n'est ni utilisable par les autres pages, ni par les sous-composants de la page1.
        - *page1.component.html*    Fichier contenant le template HTML du composant page1. Les sous-composants peuvent êtres appelés ici grace à la balise <vitam-composant-name />
        - *page1.component.spec.ts* Fichier contenant les tests unitaires du composant page1.
        - *page1.component.ts*      Fichier contenant la logique du composant page1. Les appels au(x) services sont à faire ici.
        - *page1.service.ts*        Fichier contenant un service d'appels HTTP et/ou d'utilitaire pour le composant page1.
          Ce service à des particularités spécifiques (Voir "Service Composant")
        - *class-name.ts*           Fichier contenant une classe utilisée seulement dans la page1.

          Il peut s'agir d'une classe définissant les propriétés de la structure utilisée pour les appels HTTP du service.

    - *theme1.service.ts*   Fichier contenant un service utilitaire global aux pages de ce thème.
    - *class-name.ts*       Fichier contenant une classe utilisée dans plusieurs composants du thème.

Builds et lancement des tests
=============================

NPM: npm est utilisé pour gérer les dépendances de l'application. Le fichier *package.json* définit deux types de dépendances:

- devDependencies: Dépendances utilisées pour les tests unitaire, le build ou la vérification du code. Ces dépendances ne sont pas utilisés par l'application finale en prod.
- dependencies: Dépendances utilisées par l'application finale en prod. Ils peuvent être des composants, des classes ou des utilitaires de l'application.

*Important*: Lors de la récupération de la branche, il est important de télécharger une première fois toutes les dépendances grâce à la commande *npm install*

Des scripts ont étés définis dans le fichier *package.json*. Ces scripts sont utilisables via la commande *npm run <scriptName>*.

Scripts pour le developpement (A executer, sans erreurs avant toute demande de MR):

- *start*: Lance la commande *ng serve --proxy-config proxy.conf.json* qui permet de déployer l'application à chaud en utilisant un proxy pour les appels vers le backoffice.

  Un watch est fait sur les fichiers sous le dossier src. Tout fichiers modifiers sous src mettra à jour, à chaud, l'application front.

- *test*: Lance la commande *ng test* qui lance les tests unitaire aussi bien sur Chrome que sur PhantomJS. Un watch est également activé pour relancer les tests su un fichier est modifié.
- *lint*: Lance la commande *ng lint* qui permet de vérifier les fichiers

Les scripts suivantes sont utilisés par le build maven:

- *prod*: Lance la commande *ng build --env=prod* qui permet de builder l'application pour une cible de production (Actuelement similaire au script *build* qui lance *ng build --env=dev*
- *inttest*: Lance la commande *ng test --single-run=true --browsers PhantomJS --watch=false* qui permet de lancer les tests unitaire une seule fois sur PhantomJS.

La configuration du proxy se fait dans le fichier *proxy.conf.json*.
Pour le moment, aucun fichier de surcharge sur poste de dev n'est prévue pour avec des modifications locales ignorées par le git.

Build du css:
Pour mettre à jour le CSS (styles.css) il faut:

- Update theme in *themes/vitam-red/theme.scss* or template in *themes/_theme.scss*
- Generate css with the command line *sass themes/vitam-red/theme.scss:src/styles.css*
- Remove src/styles.css.map before commit

Le build maven lance le script inttest dans la phase test (-DskipTests permet donc d'ignorer les TU front)
Les commandes suivantes peuvent être lancés pour faire des packages rpm/debian:

- ``mvn clean install rpm:rpm``
- ``mvn clean install jdeb:jdeb``

Composant de Page
=================

Les composant de page ont pour but de:

- Initialiser les composant communs (fil d'ariane, titre, ...) à toute les pages (Héritage de PageComponent)
- Récupérer les données utiles en faisant appel au service (HTTP GET)
- Traiter les données si besoin (Utiliser des services utilitaires pour les gros traitement des données)
- Appeler des sous-composants pour afficher les donénes sur la page (Injecter des donénes / fonctions dans les sous-composants appelés)

Le composant de page doit hériter du composant PageComponent (app/common/page/page-component) et utiliser pageOnInit().
Cela permet d'initialiser le fil d'ariane et la titre sur toute les pages.

Service de Composant
====================

Les service doit utiliser le resourcesService qui ajoute de lui même les headers importants (tenant) et qui connais le path vers les api vitam.
Les service doit définir l'api sur laquelle taper dans url. Elle sera concatenée avec le base URL (qui termine par un /)
Enfin, pour les requêtes GET, le service doit appliquer un plainToClass() pour transformer l'objet en une classe définie

Exemple: resourcesService.get('contrats').map((x)=>x.json()).map((json) => plainToClass(Contract, json.$results))

Les services du composant ont dont principalement un rôle de gestion des requêtes HTTP et du format de la réponse.
Ils peuvent aussi avoir quelques fonctions utilitaires pour parser/préparer la réponse pour le composant.

Sous Composant
==============

Les sous-composant ont pour but de:

- Être initialisés grâce à des objets injectés par le composant parent,
- Faire le rendement graphique d'une partie de la page (partie potentiellement répétée plusieurs fois sur la page),
- Utiliser des services d'affichage des données ou des composants graphiques,
- Faire des actions (potentielle utilisation du service du composant pour les appels PUT/POST/DELETE).

Le sous-composant ne devrait pas:

- Avoir de logique ni de traitement (Il doit se contenter d'afficher ce que le composant de page et ses services ont calculés pour lui),
- Utiliser le service pour des appels HTTP GET (C'est le rôle du composant de page).
