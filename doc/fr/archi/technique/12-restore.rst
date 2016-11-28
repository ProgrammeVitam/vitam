Restoration
###########

.. caution:: actuellement, la procédure de restauration s'applique "à froid" durant la phase de bêta.

Restauration des point de sauvegarde VITAM
------------------------------------------

#. S'arrurer que le système VITAM est à l'arret complet :
##. Arrêter les services résiduels.
##. Arreter tous les serveurs MongoDB
#. Décomprésser les archives vitam/conf et vitam/data:
##. Restaurer vitam/data dans tous les serveur MongoDB
# Démarrer Vitam suivant l'ordre de démarrage.

.. figure:: /technique/images/vitam-restore-manual-process.*
    :align: center
    :height: 20 cm

    Procédure de restauration Vitam complète
