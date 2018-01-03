/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.common.database.api.impl;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mongodb.BasicDBObject;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.result.DeleteResult;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.api.VitamRepository;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * Implementation for MongoDB
 */
public class VitamMongoRepository implements VitamRepository {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamMongoRepository.class);
    public static final String NAME = "Name";

    private MongoCollection<Document> collection;

    public VitamMongoRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public void save(Document document) throws DatabaseException {
        ParametersChecker.checkParameter("All params are required", collection, document);
        try {
            collection.insertOne(document);
        } catch (Exception e) {
            LOGGER.error("Insert Document Exception: ", e);
            throw new DatabaseException(e);
        }
    }

    @Override
    public void save(List<Document> documents) throws DatabaseException {
        ParametersChecker.checkParameter("All params are required", collection, documents);
        List<InsertOneModel<Document>> collect =
            documents.stream().map(InsertOneModel::new).collect(Collectors.toList());

        BulkWriteResult bulkWriteResult = collection.bulkWrite(collect);

        int count = bulkWriteResult.getInsertedCount();
        if (count != documents.size()) {
            LOGGER.error(
                String.format("Error while bulk save document count : %s != size : %s :", count, documents.size()));

            throw new DatabaseException(
                String.format("Error while bulk save document count : %s != size : %s :", count, documents.size())
            );
        }
    }

    @Override
    public void remove(String id, Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter("All params are required", id);
        DeleteResult delete = collection.deleteOne(new BasicDBObject(VitamDocument.ID, id));
        long count = delete.getDeletedCount();
        if (count == 0) {
            LOGGER.error(String.format("Document %s is not deleted", id));
            throw new DatabaseException(String.format("Document %s is not deleted", id));
        }
    }

    @Override
    public void removeByNameAndTenant(String name, Integer tenant) throws DatabaseException {

        ParametersChecker.checkParameter("All params are required", name, tenant);
        Bson query = and(eq(VitamDocument.TENANT_ID, tenant), eq(NAME, name));
        DeleteResult delete = collection.deleteOne(query);
        long count = delete.getDeletedCount();
        if (count == 0) {
            LOGGER.error(String.format("Documents with name %s and tenant %s are not deleted", name, tenant));
            throw new DatabaseException(
                String.format("Documents with name %s and tenant %s are not deleted", name, tenant));
        }

    }

    @Override
    public long purge(Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter("All params are required", tenant);

        try {
            DeleteResult response = collection.deleteMany(new BasicDBObject(VitamDocument.TENANT_ID, tenant));
            return response.getDeletedCount();
        } catch (Exception e) {
            LOGGER.error(String.format("Error while delete documents for tenant %s", tenant), e);
            throw new DatabaseException(String.format("Error while delete documents for tenant %s", tenant, e));
        }

    }

    @Override
    public Optional<Document> getByID(String id, Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter("All params are required", id);
        try {
            FindIterable<Document> result = collection.find(new BasicDBObject(VitamDocument.ID, id));
            if (result.iterator().hasNext()) {
                return Optional.of(result.first());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            LOGGER.error("Error while gessting document by id :", e);
            throw new DatabaseException(String.format("DatabaseException while calling fetch by id : %s", e));
        }
    }
}
