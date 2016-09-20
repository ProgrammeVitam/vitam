Schéma de certificats et d'authentification 
***********

Présentation
************

Pour sécuriser les échanges, les services externes (ingest-external et access-external) seront exposés en HTTPS avec une authentification TLS mutuelle (authentification des clients par certificats x509).
Pour permettre la consultation des URLs de status sans disposer de certificat (par exemple, pour la supervision), au niveau TLS, l'usage d'un certificat client sera proposé mais non obligatoire (WANT et non NEED clientCertificate)
Si un certificat est présenté,
- Jetty fait la poignée de main TLS et refuse si le certificat n'est pas "valide" à ses yeux. Un certificat valide est un certificat signé par une autorité présente dans la liste des autorités de confiance du serveur (truststore), qui n'est pas expiré (champs Not Before, Not After), qui, s'il implémente les extensions x509 keyUsage et extendedKeyUsage, dispose des bons droits pour être un certificat client. Si le client présente un certificat client invalide, jetty ferme la session TCP
- Shiro vérifie si le certificat présenté et bien autorisé par Vitam. Dans l'implémentation actuelle (itération 8), cela


1. Configuration serveur jetty : le serveur sera lancé avec 2 magasins de clé suivants
 - keystore.jks : contient le certificat le la clé privé du serveur 
 - truststore.jks : contient la chaînes des CAs qui génère ce certificats de clients & serveurs
 
2.Configuration de Shiro  
- granted_certs.jks : list de certificats du client qui sont autorisés à faire des requêtes vers 
le serveur
- truststore.jks : contient la chaînes des CAs qui génère ce certificats de clients & serveurs

3. Configuration client : 
le client qui doit présenter sa clé privé & le certificat (format certificat PEM ou PKCS12 contenant 
clé privé ou publique) pour l'authentification lors de la requête. 
