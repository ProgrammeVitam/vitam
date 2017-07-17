#######################################
Lancement dans Eclipse des VITAM Itests
#######################################


.. section-numbering::

.. image:: LogoVitamGrand2.png
        :alt: Logo Vitam (Bêta)
        :align: center


Fichier de configuration pour lancement en local
================================================

Pour pouvoir lancer depuis un poste local les différents fichiers features, il convient d'avoir une configuration. 
Il faut créer un fichier **tnr-config.conf** contenant :

- homeItest: le chemin vers votre copie de vitam-itests, exemple : /home/vitam/workspace2/vitam-itests/
- specificItest: si non vide, un test en particulier, exemple : /home/vitam/workspace2/vitam-itests/cnotracts.feature
- vitamData: le chemin vers votre /vitam/data, exemple : /vitam/data/
- siegfriedPath: le chemin vers Siegfried installé sur votre machine (version 1.6.5), exemple : /usr/bin/sf

   - Ubuntu/debian : http://dl.bintray.com/siegfried/debian/
   
- dataSiegfried: l'emplacement du -home pour siegfried où vous avez dézippé le data de siegfried, exemple : /vitam/data/siegfried

   - https://github.com/richardlehane/siegfried/releases fichier data-1.6.5.zip
   
- launchSiegfried: Si true, indique que le programme Java va lancé Siegfried lui-même, false Siegfried est déjà lancé sur le port 8102

  - Un bug peut empêcher l'arrêt, il faut alors faire un "killall sf" à la fin du Junit

 
Lancement en local (Eclipse)
============================

Pour pouvoir lancer les TNR en local, il faut disposer des sources VITAM et notamment du module functional-test vitam-itest.
Ensuite il s'agit d'ajouter une nouvelle configuration de lancement (Junit run Configuration) et d'y indiquer les informations suivantes : 
Dans Main :
 - Project : tnr-test
 - Test class : fr.gouv.vitam.functional.tnr.test.TnrLaunchAllApplication
Dans Arguments : (adapter le chemin à votre repo vitam)
 - VM arguments : -ea -Djava.security.properties=/home/vitam/workspace2/vitam/sources/functional-tnr/src/test/resources/java-security -DtnrJunitConf="le path complet de votre fichier **tnr-config.conf**
Dans Classpath : Dans les Advanced Options > Add External Folders > Séléctionner l'endroit de sa configuration locale (ex : /home/vitam/workspace2/vitam-itests/)

Le rapport est généré à la racine du module tnr-test.


**NB** : l'antivirus est un fake, mais parfois les droits d'exécutions sont mal positionnés. Avant de lancer les tests, une fois le build fait, il faut parfois lancer le script "change_exec_right.sh" ou le faire à la mains :

chmod a+rx ./target/test-classes/functional-tnr/scanfake.sh   