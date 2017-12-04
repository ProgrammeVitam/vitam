Certificats
###########

Utilisation
-----------

Plusieurs opérations sont supportées pour la gestion des certificats SIA et personnels.
Les certificats sont stockées dans la base Identity dans les collections Certificate et PersonalCertificate
respectivement.

Les certificats SIA sont rattachés à un SIA donné (IHM, ou autre client d'appel à Vitam) et sont utilisés pour appeler
Vitam via une connexion TLS. Ils sont récupérés par les couches "*-external" de vitam afin de les valider auprès du
security-internal. Le certificat SIA est attaché à un contexte auquel il donne accès.

Les certificats personnels sont utilisés pour les endpoints externes de Vitam qui nécessitent une authentification forte
(aussi appelée authentification personae). Le module security-internal doit être interrogé pour vérifier si telle
permission du endpoint nécessite ou pas l'authentification personnel. Le cas échéant, il convient de les valider auprès
du security-internal.

La factory
**********

Afin de récupérer le client ainsi que la bonne classe de paramètre, une factory a été mise en place.
Actuellement, elle ne fonctionne que pour le journal des opérations.

.. code-block:: java

    // Récupération du client
    InternalSecurityClientFactory.changeMode(ClientConfiguration configuration)
    InternalSecurityClient client = InternalSecurityClientFactory.getInstance().getClient();


Le Mock
=======

Par défaut, le client est en mode Mock. Il est possible de récupérer directement le mock :

.. code-block:: java

    // Changer la configuration du Factory
    InternalSecurityClientFactory.changeMode(null)
    // Récupération explicite du client mock
    InternalSecurityClient client = InternalSecurityClientFactory.getInstance().getClient();

Pour instancier son client en mode Production :

.. code-block:: java

    // Changer la configuration du Factory
    InternalSecurityClientFactory.changeMode(ClientConfiguration configuration);
    // Récupération explicite du client
    InternalSecurityClient client = InternalSecurityClientFactory.getInstance().getClient();

Le client
*********

Le client propose plusieurs méthodes pour la vérification des certificats SIA et personnels.

.. code-block:: java

    // Récupération du client
    InternalSecurityClient client = InternalSecurityClientFactory.getInstance().getClient();

    // Vérifier un certificat SIA
    byte[] certificate = ...;
    Optional<IdentityModel> identity = client.findIdentity(certificate);

    // Vérifier si un endpoint donné nécessite un authentification personae
    String permission = "...";
    IsPersonalCertificateRequiredModel isPersonalCertificateRequired
        = client.isPersonalCertificateRequiredByPermission(permission);

    // Vérifier un certificat personae
    byte[] personalCertificate = ...;
    client.checkPersonalCertificate(certificate, permission);

