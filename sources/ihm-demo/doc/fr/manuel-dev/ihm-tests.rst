IHM Front - Tests
#################

Cette documentation décrit la partie tests (unitaires et end to end) du front/Angular de l'ihm.
-----------------------------------------------------------------------------------------------

Il est possible de lancer tout les tests via la commande `gulp tests` (Protractor nécessite Chrome).
Un `npm install` est nécessaire.

Tests unitaires
***************

Installation / Lancement des tests unitaires
============================================

Karma est installé automatiquement avec les autres dépendances via la commande `npm install`.
Le lancement des tests s'effectue vià la commande `gulp testKarma`

Informations sur la configuration des tests unitaires
=====================================================

La configuration des tests unitaires se trouve dans webapp/karma.conf.js

En particulier, la partie 'files' définit les fichiers à charger pour les tests unitaires.
Il sera nécessaire d'en ajouter lors de l'ajout de prochaines fonctionnalités et tests unitaires.

Exemples de tests unitaires
===========================

4 samples de tests ont étés implémentés pour montrer ce qu'il est globalement possible de faire:

| **Base**      beforeEach (Charger un service) / Test de retour de valeur en fonction du paramètre
|               Exemples: date-validator.service.js / response-validator.service.js
| **Espion**    SpyOn permettant de vérifier qu'une fonction est bien appelée comme il faut
|               Exemple: load-static-value.service.js (Test nombre appel) / response-validator.service.js (Bon paramètres)
| **HTTPMock**  httpBackend permettant de mocker un appel rest / afterEach permettant de vérifier les appels traités
|               Exemple: accession-register.service.js
| **CallMock**  initialisation d'un controller / mock de l'appel des méthodes d'un service / cohérence des résultats
|               accession-register-details.controller.js

Tests end to end
****************

Initialisation / Lancement des tests e2e
========================================

Pour le moment, il est nécessaire d'avoir un environement lancé dans le serveur d'App pour servir les resources. Un gulp serve devrait régler le problème.

[Inutile si lancé via gulp]Installation de protractor

.. code-block:: bash

	npm install -g protractor@2
	protractor --version

Cette commande devrait avoir installer un 'webdriver-manager' (Sélénium).

[Inutile si lancé via gulp]Il est nécessaire de le mettre à jour et de le lancer pour lancer les tests e2e.

.. code-block:: bash

	node_modules/protractor/bin/webdriver-manager update
	node_modules/protractor/bin/webdriver-manager start

Si une erreur 'info: driver.version: unknown' est remontée, vérifier la compatibilité entre votre navigateur Chrome et son plugin ChromeDriver.
Si besoin, modifiez le fichier webapp/node_modules/protractor/config.json, et mettez à jour la propriété "chromedriver" avec une valeur compatible (2.27 pour les plus récent).
Cette modification 'hardcodded' doit être faite après chaque mise à jour de npm (npm install).

[Inutile si lancé via gulp]Le lancement des tests end to end se font grâce à la commande suivante:

.. code-block:: bash

	protractor protractor.conf.js

Il est également possible de le lancer via gulp via la commande:

.. code-block:: bash

	gulp testProtractor

Il est possible de surcharger divers arguments grâce aux arguments suivants (donnés à titre d'exemple:

- --baseUrl='http://localhost:8082/ihm-demo/#!' Permet de modifier l'URL de base utilisée. Peut par exemple servir a lancer les tests e2e sur le serveur de recette.
- --params.<paramName>='<paramValue>'           Permet de modifier un paramètre de la configuration protractor (params)
- --suite='<maSuite>'                           Permet d'utiliser seulement une ou plusieurs suites de tests plutôt que de lancer toute la baterie.

Ces paramètres sont aussi settables dans le json de configuration gulp de la tache testProtractor.

Informations sur la configuration des tests e2e
===============================================

La configuration définit des batteries de tests (suites). Lors de l'ajout d'un test e2e, il est nécessaire d'ajouter une entrée dans les suites en précisant les fichiers à éxécuter.

La configuration permet aussi de:

- Définir un login/password (Via la surcharge des params userName/password)
- Utiliser ou non le mode mock http (Via la surcharge du param mock)

Exemple d'utilisation des outils e2e
====================================

Création de fonctions réutilisables dans chaque test :

- Création d'un fichier utils/\*.function.js
- Création d'une fonction éxportée via module.exports
- Import des fonctions dans le test via require('./path/to/file');

Sélection des éléments

- Sélection d'une balise a laquelle le modèle associé est variable.name (<input ng-model="variable.name" />)

-- element(by.model('variable.name'))

- Sélection d'une balise grâce à son identifiant (<div id="navbar"></div>)

-- element(by.id('navbar'));

- Sélection d'une balise contenant un attribut 'type' et une valeur 'submit' (<button type="submit" />)

-- element(by.css('[type="submit"]'))

- Sélection d'une balise grâce à son tag (<ul></ul>)

-- element(by.css('ul'));

- Sélection multiple d'éléments (<li></li><li></li>)

-- element.all(by.css('li'));

- Sélection d'un sous élément (<div> <p>xxx</p><p>yyy</p> <button/> </div>)

-- var div = element(by.css('div'));
-- div.element(by.css('button')); / div.all(by.css('p'));

- Sélection d'une partie d'un ensemble d'éléments (<p>xxx</p> <p>yyy</p> <p>zzz</p>)

	- var ps = element.all(by.css('p'));
	- var firstP = ps.first(); // xxx
	- var pNumber1 = ps.get(1); // yyy
	- var lastP = ps.last(); // zzz

Conclusion:

- Selection classique: element(by.xxx());
- Sélection multiple: element.all(by.yyy());
- Sélections Chaînées: element(by.xxx()).all(by.yyy()).get(2).element(by.zzz());

Récupérations des propriétés configurés dans protractor.conf.js:

- browser.baseUrl (L'url configurée)
- browser.params.paramName (Récupère le paramètre paramName)

Actions / promise et Expects:

- Les actions sur un élément (item.click() / item.count() / ...) renvoient une promise qu'il faut traiter dans un then si on veut enchainer une action ou récupérer une valeur.
- Les expects expect(item.count())toBe(2); traitent la promise de la bonne manière pour comparer la valeur.
 
Mock HTTP:

- Exemple simple dans login ou on configure le httpMocker dans beforeEach si le mode mock est activé.
- Exemple plus complexe dans accession-register où on renvoie une réponse en fonction des paramètres.
