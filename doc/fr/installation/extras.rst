Elements extras de l'installation
#################################

Les élements décrits dans cette section sont des élements "extras" non inclus dans l'installation de base mais pouvant être utile

ClamAV en mode démon
====================

Cette procédure permet de passer ClamAV en mode démon et non plus en mode fork à chaque SIP envoyé dans ingest. En cas d'envoi de SIP en parallèle, cela permet de limiter fortement la consommation mémoire.

Cette configuration est actuellement en extra mais devrait être réintégré dans le standard Vitam d'ici les prochaines itérations


Pour chacune des machines disposant du rôle ingest-external (entre parenthèses, les commandes sous Centos) :

  - (en root) S'assurer que le package clamav-server est installé (fourni par l'EPEL) (yum install clamav-server,clamav-scanner,clamav-server-systemd)
  - (en root) Editer le fichier ``/etc/clamav.d/scan.conf`` en modifiant les points suivants

    + Supprimer la ligne commencant par Example
    + Décommenter la ligne ``TCPSocket 3310``
    + Décommenter la ligne ``TCPAddr 127.0.0.1``
    + Décommenter la ligne ``DetectPUA yes``
    + Décommenter la ligne ``ArchiveBlockEncrypted yes``
    + Modifier la ligne ``User clamscan`` en ``User vitam``

  - (en root) Démarrer et activer le démarrage automatique du service clamd (systemctl start clamd@scan && systemctl enable clamd@scan)
  - (avec l'utilisateur vitam) Editer le fichier ``/vitam/conf/ingest-external/scan-clamav.sh``

    + Remplacer ``clamscan -z --detect-pua=yes --block-encrypted=yes`` par ``clamdscan -z  --config-file=/etc/clamd.d/scan.conf``

A noter qu'en cas de relance du job ansible, le fichier ``/vitam/conf/ingest-external/scan-clamav.sh`` sera ecrasé
