package fr.gouv.vitam.core;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.core.database.collections.MongoDbAccess;

public class MongoDbAccessFactory {
    public MongoDbAccess create(MetaDataConfiguration configuration) {
        return new MongoDbAccess(
            new MongoClient(new ServerAddress(
                configuration.getHost(),
                configuration.getPort()),
                MongoDbAccess.getMongoClientOptions()),
            configuration.getDbName(), false);
    }
}
