/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.collect.internal.repository;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.model.CollectModel;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

/**
 * repository for collect entities  management in mongo.
 */
public class CollectRepository {

    public static final String COLLECT_COLLECTION = "Collect";
    public static final String ID = "Id";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectRepository.class);

    private final MongoCollection<Document> collectCollection;

    @VisibleForTesting
    public CollectRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        collectCollection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
    }

    public CollectRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, COLLECT_COLLECTION);
    }

    /**
     * create a collect model
     *
     * @param collectModel collect model to create
     * @throws CollectException exception thrown in case of error
     */
    public void createCollect(CollectModel collectModel) throws CollectException {
        LOGGER.debug("Collect to create: {}", collectModel);
        try {
            String json = JsonHandler.writeAsString(collectModel);
            collectCollection.insertOne(Document.parse(json));
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when creating collect: ", e);
            throw new CollectException("Error when creating collect: " + e);
        }
    }

    /**
     * replace a collect model
     *
     * @param collectModel collect model to replace
     * @throws CollectException exception thrown in case of error
     */
    public void replaceCollect(CollectModel collectModel) throws CollectException {
        LOGGER.debug("Collect to replace: {}", collectModel);
        try {
            String json = JsonHandler.writeAsString(collectModel);
            final Bson condition = and(eq(ID, collectModel.getId()));
            collectCollection.replaceOne(condition, Document.parse(json));
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when replacing collect: ", e);
            throw new CollectException("Error when replacing collect: " + e);
        }
    }

    /**
     * return collection according to id
     *
     * @param id collect id to find
     * @return Optional<CollectModel>
     * @throws CollectException exception thrown in case of error
     */
    public Optional<CollectModel> findCollect(String id) throws CollectException {
        LOGGER.debug("Collect id to find : {}", id);
        try {
            Document first = collectCollection.find(Filters.eq(ID, id)).first();
            if (first == null) {
                return Optional.empty();
            }
            return Optional.of(BsonHelper.fromDocumentToObject(first, CollectModel.class));
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when searching collect by id: ", e);
            throw new CollectException("Error when searching collect by id: " + e);
        }
    }
}
