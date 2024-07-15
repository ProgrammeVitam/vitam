Notes et procédures spécifiques V6RC
####################################

Procédures à exécuter AVANT la montée de version
================================================

Duplication des packages lors de la mise à jour
-----------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration depuis une version 7.0.1- (v7.0.1 ou inférieur) vers une version 7.0.2+ (v7.0.2 ou supérieure).

.. caution:: Cette opération doit être effectuée uniquement pour les systèmes d'exploitation à base de RPM (CentOS 7 & AlmaLinux 9) sur un vitam éteint.

Un bug a été introduit dans la construction des packages RPM à partir de la V6RC. Si vous effectuez une montée de version depuis la V6RC ou de la V6, vous devrez appliquer la commande suivante pour supprimer les précédents packages. Sinon lors de de la mise à jour, le package précédent ne sera pas désinstallé conduisant à la présence de plusieurs versions du même package.

Exemple d'erreur:

.. code-block:: bash

    /var/tmp/rpm-tmp.fGwZMk: ligne 1 : fg: pas de contrôle de tâche
    erreur : %preun(vitam-offer-6.2-1.noarch) scriptlet échoué, état de sortie 1
    Error in PREUN scriptlet in rpm package vitam-offer
    erreur : vitam-offer-6.2-1.noarch: effacer échoué

..

Voici la commande à exécuter pour permettre la désinstallation des packages de la version précédente:

.. code-block:: bash

    ansible vitam --ask-vault-pass -v -a "yum --setopt=tsflags=noscripts remove -y vitam-*.noarch" -i environments/<inventaire>

..
