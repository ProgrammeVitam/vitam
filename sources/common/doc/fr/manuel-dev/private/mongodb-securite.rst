Implémentation de l'authentification
################################################

Implémentation de l'authentification (MongoDbAccess)
---------------------------------------------------------------
L'authentification est le processus de vérification de l'identité du client, donc vous avez
besoin d'utiliser quatre paramètres dans la fichier de configuration -- 
	"dbAuthentication", "dbUserName", "dbName", "dbPassword"

La gestion de l'authentification doit être débrayable --
Si "dbAuthentication" est égal à "false", il doit être possible de continuer à 
utiliser des bases de données Mongo sans authentification. 

Si "dbAuthentication" est égal à "true", il faut créer le MongoClient contenant MongoCredential 
qui représente les informations d'identification pour l'authentification auprès d'un serveur mongo, 
ainsi que la source des informations d'identification et le mécanisme d'authentification à utiliser.

Ici, Les utilisateurs "dbUserName" se lier à une base de données spécifique "dbName".
Il a besoin de mot de passe "dbPassword" pour entrer le base et CRUD.

.. code-block:: java

    public static MongoClient createMongoClient
    	(DbConfiguration configuration, MongoClientOptions options) {
        if (configuration.isDbAuthentication()) {

            // create user with username, password and specify the database name
            MongoCredential credential = MongoCredential.createCredential(
                configuration.getDbUserName(), configuration.getDbName(), configuration.getDbPassword().toCharArray());

            // create an instance of mongoclient
            return new MongoClient(new ServerAddress(
                configuration.getDbHost(),
                configuration.getDbPort()),
                Arrays.asList(credential),
                options);
        } else {
            return new MongoClient(new ServerAddress(
                configuration.getDbHost(),
                configuration.getDbPort()),
                options);
        }
    }
 
 
 
 
 