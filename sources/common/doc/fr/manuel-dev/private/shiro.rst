Configuration de apache shiro
******************************

TODO: présentation de apache shiro, configuration, ...

Présentation authentification via certificats
**********************************************

Afin de pouvoir authentifier des clients via des certificats valides il suffit de bien configurer shiro.
Pour ce faire vitam utilise le fichier shiro.ini qui a la forme suivante.

.. code-block:: yaml

    [main]
    x509 = fr.gouv.vitam.common.auth.web.filter.X509AuthenticationFilter
    x509.useHeader = false
    x509credentialsMatcher = fr.gouv.vitam.common.auth.core.authc.X509CredentialsSha256Matcher
    x509Realm = fr.gouv.vitam.common.auth.core.realm.X509KeystoreFileRealm
    x509Realm.grantedKeyStoreName = path/granted_certs.jks
    x509Realm.grantedKeyStorePassphrase = password
    x509Realm.trustedKeyStoreName = path/truststore.jks
    x509Realm.trustedKeyStorePassphrase = password
    x509Realm.credentialsMatcher = $x509credentialsMatcher
    securityManager.realm = $x509Realm
    securityManager.subjectDAO.sessionStorageEvaluator.sessionStorageEnabled = false
    [urls]
    /ingest-ext/v1/**= x509



Décryptage de shiro.ini
***********************

[main]
Contient les déclaration de filters et classes comme par exemple X509AuthenticationFilter, X509CredentialsSha256Matcher, X509KeystoreFileReal, ...
La clé (x509, x509Realm) sont custom et on peut donner le nom qu'on veut, par contre securityManager est un mot clé shiro.
La ligne securityManager.realm = $x509Realm passe à shiro le Realm qu'on veut utiliser, ceci dit, les clé custom peut être passé à shiro de la même façon.

[urls]
Pour une url donnée on dit quel filter utiliser, exemple: /ingest-ext/v1/**= x509 signifie que l'on veut utiliser le filter x509 pour toutes les urls de type  /ingest-ext/v1/**


Utilisation des certificats
****************************

Vitam a une implémentation de filter pour utiliser des certificats x509 afin d'authentifier des clients.

X509AuthenticationFilter (filter par defaut)

 - Activation du filter dans le fichier shiro.ini: x509 = fr.gouv.vitam.common.auth.web.filter.X509AuthenticationFilter
 - Ce filter récupère les certificats fournis dans la requête :

    .. sourcecode:: java

        X509Certificate[] clientCertChain = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
    

 - Si des certificats sont trouvé alors un token est crée qui sera passé à la méthode qui s'occupe d'authentifier un client.

    .. sourcecode:: java

        new X509AuthenticationToken(clientCertChain, getHost(request));
    

 - X509AuthenticationFilter peut aussi authentifier via un certificat passé dans le header. La variable "useHeader" est égale à false par défaut. Donc cette option est désactivé par défaut. Si useHeader= true (qu'on peut spécifier dans shiro.ini: x509.useHeader = false dans l'exemple ci-dessus) et qu'aucun cetificat n'est fourni dans l'attribute de la requête javax.servlet.request.X509Certificate alors il bascule vers une authentification via le header. Le nom du header est X-SSL-CLIENT-CERT, et il doit avoir comme valeur un certificat valide au format pem. Le certificat pem est ensuite converti vers un X509Certificate qui sera utilisé pour créer le token d'authentification. Ci-dessous une snipet de code qui permets de récupérer la valeur du certificat depuis le header et le convertir au bon format.
 - Attention, jetty n'accepte pas les retour à la ligne dans le `header`, d'où la nécessité d'encoder le ``pem`` en base 64. X509AuthenticationFilter s'occupe de déterminer si le certificat passé dans le header est encodé ou non en base 64 et fera en sorte d'accepter même les certificats non encodés.

    .. sourcecode:: java

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        String pem = httpRequest.getHeader(X_SSL_CLIENT_CERT);
        byte[] pemByte = null;
        if (null != pem) {
            try {
                try {
                    pemByte = Base64.getDecoder().decode(pem);
                } catch (IllegalArgumentException ex) {
                    // the pem is not base64 encoded
                    pemByte = pem.getBytes();
                }
                final InputStream pemStream = new ByteArrayInputStream(pemByte);
                final CertificateFactory cf = CertificateFactory.getInstance("X.509");
                final X509Certificate cert = (X509Certificate) cf.generateCertificate(pemStream);
                clientCertChain = new X509Certificate[] {cert};
            } catch (Exception ce) {
                throw new ShiroException(ce);
            }
        }


 - Il faut noter que l'authentification via un certificat passé dans le `header` n'est pas sécurisée (moins sécurisée que la solution via l'attribute de la requête). En effet, il peut y avoir une injection lors de l'acheminement de la requête depuis un client vers un serveur jetty. Nous recommendons donc l'utilisation de certificats dans l'attribute de la requête.
