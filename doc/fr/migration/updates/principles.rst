Principes généraux
##################

Montées de version bugfix
=========================

Au sein d'une même release, la montée de version depuis une version bugfix vers une version bugfix supérieure est réalisée par réinstallation de la solution logicielle :term:`VITAM` grâce aux playbooks ansible fournis, et selon la procédure d’installation classique décrite dans le Document d’INstallation (DIN). 

Les montées de version bugfix ne contiennent à priori pas d'opérations de migration ou de reprises de données particulières. Toutefois, des spécificités propres aux différentes versions bugfixes peuvent s'appliquer, elles sont explicitées dans le chapitre :ref:`bugfixes_updates`. 

.. caution:: Parmi les versions bugfixes publiées au sein d'une même release, seuls les chemins de montées de version d'une version bugfix à la version bugfix suivante sont qualifiés par :term:`VITAM`. 

Montées de version mineure
==========================

La montée de version depuis une version mineure (de type release) vers une version mineure supérieure est réalisée par réinstallation de la solution logicielle :term:`VITAM` grâce aux playbooks ansible fournis, et selon la procédure d’installation classique décrite dans le Document d’INstallation (DIN). 

Ce document décrit les chemins de montées de version depuis une version mineure, vers la version mineure maintenue supérieure. 

Les montées de version mineure doivent être réalisées en s'appuyant sur les dernières versions bugfixes publiées. 

Les opérations de migration ou de reprises de données propres aux différentes versions releases sont explicitées dans le chapitre :ref:`releases_updates`. 

.. caution:: Parmi les versions mineures publiées au sein d'une même version majeure, seuls les chemins de montées de version depuis une version mineure maintenue, vers la version mineure maintenue suivante sont qualifiés par :term:`VITAM`. 

Montées de version majeure
==========================

La montée de version depuis une version majeure vers une version majeure supérieure s'appuie sur les chemins de montées de version mineure décrits au chapitre précédent. 
