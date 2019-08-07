Implémentation de l'authentification
################################################

Implémentation de l'authentification (MongoDbAccess)
---------------------------------------------------------------
L'authentification est le processus de vérification de l'identité du client, donc vous avez besoin d'utiliser quatre paramètres dans la fichier de configuration

	"dbAuthentication", "dbUserName", "dbName", "dbPassword"

La gestion de l'authentification doit être débrayable --
Si "dbAuthentication" est égal à "false", il doit être possible de continuer à 
utiliser des bases de données Mongo sans authentification. 

Si "dbAuthentication" est égal à "true", il faut créer le MongoClient contenant MongoCredential 
qui représente les informations d'identification pour l'authentification auprès d'un serveur mongo, 
ainsi que la source des informations d'identification et le mécanisme d'authentification à utiliser.

Ici, Les utilisateurs "dbUserName" se lient à une base de données spécifique "dbName".
Il a besoin du mot de passe "dbPassword" pour entrer dans la base et :term:`CRUD`.

.. sourcecode:: java

    public static MongoClient createMongoClient(DbConfiguration configuration, MongoClientOptions options) {
        List<MongoDbNode> nodes = configuration.getMongoDbNodes();
        List<ServerAddress> serverAddress = new ArrayList<ServerAddress>();
        for (MongoDbNode node : nodes){
            serverAddress.add(new ServerAddress(node.getDbHost(), node.getDbPort()));
        }
        if (configuration.isDbAuthentication()) {
            // create user with username, password and specify the database name
            MongoCredential credential = MongoCredential.createCredential(
                configuration.getDbUserName(), configuration.getDbName(), configuration.getDbPassword().toCharArray());
            
            // create an instance of mongoclient
            return new MongoClient(serverAddress, Arrays.asList(credential), options);
        } else {
            return new MongoClient(serverAddress, options);
        }
    }



--	List<ServerAddress> serverAddress:
	La liste des adresses du serveur qui permet la base de données mongodb de connecter plusieurs nœuds
--	Arrays.asList(credential):
	La liste des informations d'identification que ce client authentifie toutes les connexions avec

