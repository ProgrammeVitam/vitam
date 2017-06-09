Génération de certificats et de keystore  
#########################################

Présentation
************

Nous avons besoins de certificats & keystore pour la procédure d'authentification client-serveur. 
Ce document présente comment nous les crééons 
 
1. Pour rappel, nous avons besoins de  différents keystore: 

- keystore.jks : contient le certificat de la clé privé du serveur 
- truststore.jks : contient la chaîne des CAs qui génère ce certificat de clients & serveurs  
- granted_certs.jks : list de certificats du client qui sont autorisés à faire des requêtes vers 
le serveur
- le client qui doit présenter sa clé privée & le certificat,lors d'une requête d'authentification. 


2.Création des certificats 
Comme il n'y a pas de PKI, nous utilisons le xca  pour générer des certificats et pour les tests. 
Nous créons l'ensemble des certificats suivants en utilisant le xca.  

- VitamRootCA : certificat auto-signé, modèle de certificat : CA, X509v3 Basic Constraints Extensions :  Autorité de Certification
- VitamIntermediateCA : certificat signé par VitamRootCA, modèle de certificat : CA, X509v3 Basic Constraints Extensions :  Autorité de Certification
- IngestExtServer : certificat signé par VitamIntermediateCA , modèle de certificat : https_server, X509v3 Basic Constraints Extensions :  Entité Finale
- client : certificat signé par VitamIntermediateCA , modèle de certificat : https_client, X509v3 Basic Constraints Extensions :  Entité Finale
- client_expired : certificat signé par VitamIntermediateCA , modèle de certificat : https_client, X509v3 Basic Constraints Extensions :  Entité Finale
- client_notgranted : certificat signé par VitamIntermediateCA , modèle de certificat : https_client, X509v3 Basic Constraints Extensions :  Entité Finale

Une fois qu'on a créé ces certificats, nous exportons ces certificats soit en format crt, pem ou p12 pour des utilisations différentes  

3. Création des keystores vides
Nous utilisons le keytool pour créer les keystores  

keytool -genkey -alias mydomain -keystore keystore.jks
keytool -delete -alias mydomain -keystore keystore.jks 

keytool -genkey -alias mydomain -keystore truststore.jks
keytool -delete -alias mydomain -keystore truststore.jks 

keytool -genkey -alias mydomain -keystore granted_certs.jks
keytool -delete -alias mydomain -keystore granted_certs.jks 


4. Import des certificats 

- truststore.jks : importer VitamIntermediateCA.crt, VitamRootCA.crt 
	keytool -import -trustcacerts -alias VitamRootCA -file VitamRootCA.crt -keystore truststore.jks
	keytool -import -trustcacerts -alias VitamIntermediateCA -file VitamIntermediateCA.crt -keystore truststore.jks
	
- keystore.jks
 	importer la clé privée et le certificat du serveur
	keytool -v -importkeystore -srckeystore IngestExtServer.p12 -srcstoretype PKCS12 -destkeystore keystore.jks -deststoretype JKS
	keytool -import -trustcacerts -alias IngestExtServer -file IngestExtServer.crt -keystore truststore.jks
	
- granted_certs.jks 
	importer des certificats client.crt et client_expired.crt
	
5. Utilisation des certificats client.	
   exporter en format p12 ou pem selon des buts d'utilisations. 
	
	
	
	
