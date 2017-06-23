
Cas 2: Je dispose d'une PKI
===========================


Procédure générale
------------------

Si vous disposez d'une PKI, il n'est pas nécessaire d'utiliser celle de Vitam.
Il va par contre être nécessaire de déposer les certificats et les autorités de certifications correspondantes dans les bon répertoires.
Il sera aussi nécessaire de renseigner les mots de passe des clés privées des certificats dans le vault ansible environmements/certs/vault-certs.yml
Il faudra alors ensuite utiliser le script Vitam permettant de générer les différents keystores.


Intégration de certificats existants
------------------------------------

Si vous possédez déjà une :term:`PKI`, il convient de positionner les certificats et CA sous ``environmements/certs/....`` en respectant la structure indiquée ci-dessous.

.. _arborescence_certs:
.. figure:: images/arborescence_certs.*
    :align: center
    :target: ../_images/arborescence_certs.svg

    Vue détaillée de l'arborescence des certificats

.. tip::

    Dans le doute, n'hésitez pas à utiliser la PKI de test (étapes de génération de CA et de certificats) pour générer les fichiers requis au bon endroit et ainsi voir la structure exacte attendue ;
    il vous suffira ensuite de remplacer ces certificats "placeholders" par les certificats définitifs avant de lncer le déploiement.

Ne pas oublier de renseigner le vault contenant les passphrases des clés des certificats: ``environmements/certs/vault-certs.yml``

Pour modifier/créer un vault ansible, se référer à la documentation sur `cette url <http://docs.ansible.com/ansible/playbooks_vault.html>`_.

.. include:: keystores.rst
