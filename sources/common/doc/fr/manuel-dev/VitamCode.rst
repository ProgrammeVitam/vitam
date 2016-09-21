Code d'erreur Vitam
###################

Afin d'harmoniser les erreurs un code d'erreur commun aux différents modules Vitam a été défini.
Il est composé de trois éléments aplhanumériques à deux caractères.

Exemple :
0A7E08 où 0A est le code service, 7E est le code domaine et 08 l'item.

Les codes
*********

Code service
============

Le code service identifie le service concerné par l'erreur. Tous les services sont listés dans l'énum ServiceName.
On y retrouve son code et sa description.

Attention, le code 00 est utilisé dans le cas où le service concerné ne se trouve pas dans l'énum. Il sert également
aux différents test, il ne faut pas le supprimer.

L'énum offre également la possibilité de retrouver un service via son code (getFromCode(String code)).

Code domaine
============

Le code domaine identifie le domaine concerné par l'erreur. Tous les domaines actuellement identifiés sont listés
dans l'énum DomainName. On y retrouve son code et sa description.

Attention, le code 00 est uniquement utilisé dans les tests. Il ne doit pas être utilisé dans le code de Vitam. Il ne
 doit pas êre supprimé.

L'énum offre également la possiblité de retrouver un domaine via son code (getFromCode(String code)).

Code Vitam
==========

Le code Vitam est donc composé du service, du domaine et d'un item. On retrouve les erreurs Vitam dans l'énum CodeVitam.
On y voit le service, le domaine, l'item, le status HTTP associé à cette erreur ainsi qu'un message.

A terme, le message sera une clef de traduction afin d'internationaliser les messages d'erreur.

Le code 000000 (service 00, domaine 00, item 00) est un code de test. Il ne faut pas l'utiliser dans le code Vitam ni
 le supprimer.

Ajout d'élement dans les énums
==============================

Au fur et à mesure des développements, chaque développeurs va être amené à ajouter une erreur. Il n'aura
principalement qu'à ajouter une ligne dans VitamCode. Cependant, le triplet service, domain, item est unique.

Pour garantir cette unicitée, un test unitaire se charge de vérifier les trois énums : CodeTest.

Dans un premier temps sont validés les codes (2 caractères alphanumériques en majuscule) pour chaque énum.
Ensuite est vérifié l'unicité des codes pour chacune.

Ces tests n'ont pas à être modifié ! S'il ne passe plus après l'ajout d'une entrée c'est que celle ci est incorrecte,
 le test ne le sera jamais. Dans les logs de CodeTest vous trouverez la raison de l'erreur (code dupliqué et avec
 lequel ou erreur dans le code).

Utilisation
***********

Afin de récupérer un VitamCode, il suffit de passer par l'énum :

.. code-block:: java

    VitamCode vitamCode = VitamCode.TEST;

Il est également possible de le récupérer directement via son code à l'aide du helper VitamCodeHelper :

.. code-block:: java

    VitamCode vitamCode = VitamCodeHelper.getFrom("012AE5");

A partir des getter de l'énum VitamCode, il est possible de récupérer les différentes informations :

.. code-block:: java

    VitamCode vitamCode = VitamCode.TEST;
    ServiceName service = vitamCode.getService();
    DomainName domain = vitamCode.getDomain();
    String item = vitamCode.getItem();
    Status status = vitamCode.getStatus();
    String message = vitamCode.getMessage();

Concernant le message, il est possible de lui mettre des paramètres (String.format()). Ainsi, via le helper, il est
possible de récupérer le message avec les paramètres insérés :

.. code-block:: java

    VitamCode vitamCode = VitamCode.TEST;
    String message = VitamCodeHelper.getParametrizedMessage(vitamCode, "monParametre", "monAutreParametre");

Il est possible de récupérer un "log" formaté et paramétré telque "[codeVitam] message paramétré" :

.. code-block:: java

    String log = VitamCodeHelper.getLogMessage(VitamCode.TEST, param1, param2);

