package fr.gouv.vitam.core;

import org.apache.log4j.Logger;

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoWriteException;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.api.MetaData;
import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.core.database.collections.translator.mongodb.MongoDbClient;
import fr.gouv.vitam.core.database.collections.translator.mongodb.InsertToMongodb;
import fr.gouv.vitam.parser.request.parser.InsertParser;

public class MetaDataImpl implements MetaData {

	private static final Logger LOGGER = Logger.getLogger(MetaDataImpl.class);
	private MongoDbClient mongoDbClient;

	public MetaDataImpl(MetaDataConfiguration configuration) {
		super();

		mongoDbClient = new MongoDbClient(configuration.host, configuration.port, configuration.dbName,
				configuration.collectionName);
	}

	@Override
	public void insertUnit(String insertRequest) throws InvalidParseOperationException, MetaDataNotFoundException,
			MetaDataAlreadyExistException, MetaDataExecutionException {

		final InsertParser request = new InsertParser();
		try {
			request.parse(insertRequest);
		} catch (InvalidParseOperationException e) {
			LOGGER.error("Invalid parse operation exception");
			throw e;
		}
		InsertToMongodb requestToMongo = new InsertToMongodb(request);

		try {
			mongoDbClient.createDocument(requestToMongo);
		} catch (DuplicateKeyException e) {
			throw new MetaDataAlreadyExistException(e);
		} catch (IllegalAccessError | IllegalAccessException | MongoWriteException e) {
			throw new MetaDataExecutionException(e);
		}

	}

}
