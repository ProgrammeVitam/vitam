/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.database.api.impl;


import com.mongodb.BasicDBObject;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.api.VitamRepository;
import fr.gouv.vitam.common.database.api.VitamRepositoryStatus;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;

/**
 * Implementation for MongoDB
 */
public class VitamMongoRepository implements VitamRepository {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamMongoRepository.class);
    public static final String NAME = "Name";
    private static final String ALL_PARAMS_REQUIRED = "All params are required";
    private MongoCollection<Document> collection;

    /**
     * Default constructor
     *
     * @param collection the collection on which to perform operation
     */
    public VitamMongoRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public void save(Document document) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, collection, document);
        try {
            collection.insertOne(document);
        } catch (Exception e) {
            LOGGER.error("Insert Document Exception: ", e);
            throw new DatabaseException(e);
        }
    }

    @Override
    public VitamRepositoryStatus saveOrUpdate(Document document) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, collection, document);
        try {
            ReplaceOneModel<Document> replaceOneModel =
                new ReplaceOneModel<>(eq("_id", document.get("_id")), document,
                    new UpdateOptions().upsert(true));
            UpdateResult result = collection.replaceOne(replaceOneModel.getFilter(), replaceOneModel.getReplacement(),
                replaceOneModel.getOptions());
            if (result.getModifiedCount() > 0) {
                return VitamRepositoryStatus.UPDATED;
            } else {
                return VitamRepositoryStatus.CREATED;
            }
        } catch (Exception e) {
            LOGGER.error("Insert or Update Document Exception: ", e);
            throw new DatabaseException(e);
        }
    }

    @Override
    public void save(List<Document> documents) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, collection, documents);
        List<InsertOneModel<Document>> collect =
            documents.stream().map(InsertOneModel::new).collect(Collectors.toList());

        BulkWriteResult bulkWriteResult = collection.bulkWrite(collect);

        int count = bulkWriteResult.getInsertedCount();
        if (count != documents.size()) {
            LOGGER.error(
                String.format("Error while bulk save document count : %s != size : %s :", count, documents.size()));

            throw new DatabaseException(
                String.format("Error while bulk save document count : %s != size : %s :", count, documents.size()));
        }
    }

    @Override
    public void saveOrUpdate(List<Document> documents) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, collection, documents);
        List<ReplaceOneModel<Document>> collect =
            documents.stream().map(document -> new ReplaceOneModel<>(eq("_id", document.get("_id")), document,
                new UpdateOptions().upsert(true))).collect(Collectors.toList());

        BulkWriteResult bulkWriteResult = collection.bulkWrite(collect);

        int inserted = bulkWriteResult.getUpserts().size();
        int matched = bulkWriteResult.getMatchedCount();
        if ((inserted + matched) != documents.size()) {
            LOGGER.error(
                String.format("Error while bulk save or update document count : %d != size : %d :", inserted + matched,
                    documents.size()));
            throw new DatabaseException(
                String.format("Error while bulk save or update document count : %d != size : %d :", inserted + matched,
                    documents.size()));
        }
    }

    @Override
    public void update(List<WriteModel<Document>> queries) throws DatabaseException {
        try {
            collection.bulkWrite(queries, new BulkWriteOptions().ordered(false));
        } catch (Exception e) {
            LOGGER.error("Bulk update documents exception: ", e);
            throw new DatabaseException(e);
        }
    }

    @Override
    public void remove(String id, Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, id);
        DeleteResult delete = collection.deleteOne(new BasicDBObject(ID, id));
        long count = delete.getDeletedCount();
        if (count == 0) {
            LOGGER.error(String.format("Document %s is not deleted", id));
            throw new DatabaseException(String.format("Document %s is not deleted", id));
        }
    }

    @Override
    public long remove(Bson query) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, query);
        try {
            return collection.deleteMany(query).getDeletedCount();
        } catch (Exception e) {
            LOGGER.error("Remove documents exception: ", e);
            throw new DatabaseException(e);
        }
    }

    @Override
    public void removeByNameAndTenant(String name, Integer tenant) throws DatabaseException {

        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, name, tenant);
        Bson query = and(eq(VitamDocument.TENANT_ID, tenant), eq(NAME, name));
        DeleteResult delete = collection.deleteOne(query);
        long count = delete.getDeletedCount();
        if (count == 0) {
            LOGGER.error(String
                .format("Error while removeByNameAndTenant> Name : %s and tenant: %s",
                    name, tenant));
            throw new DatabaseException(String
                .format("Error while removeByNameAndTenant> Name : %s and tenant: %s",
                    name, tenant));
        }

    }

    @Override
    public long purge(Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tenant);

        try {
            DeleteResult response = collection.deleteMany(new BasicDBObject(VitamDocument.TENANT_ID, tenant));
            return response.getDeletedCount();
        } catch (Exception e) {
            LOGGER.error(String.format("Error while delete documents for tenant %s", tenant), e);
            throw new DatabaseException(String.format("Error while delete documents for tenant %s", tenant, e));
        }

    }

    @Override
    public long purge() throws DatabaseException {
        try {
            DeleteResult response = collection.deleteMany(new BasicDBObject());
            return response.getDeletedCount();
        } catch (Exception e) {
            LOGGER.error(String.format("Error while delete documents"), e);
            throw new DatabaseException(String.format("Error while delete documents", e));
        }
    }

    @Override
    public Optional<Document> getByID(String id, Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, id);
        try {
            FindIterable<Document> result = collection.find(new BasicDBObject(ID, id));
            if (result.iterator().hasNext()) {
                return Optional.of(result.first());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            LOGGER.error(String.format("DatabaseException while getting document by id : %s", id), e);
            throw new DatabaseException(String.format("DatabaseException while getting document by id : %s", id), e);
        }
    }


    @Override
    public Optional<Document> findByIdentifierAndTenant(String identifier, Integer tenant)
        throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, identifier, tenant);
        Bson query = and(eq("Identifier", identifier), eq(VitamDocument.TENANT_ID, tenant));
        try {
            FindIterable<Document> result = collection.find(query);
            if (result.iterator().hasNext()) {
                return Optional.of(result.first());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            LOGGER.error(String
                    .format("Error while findByIdentifierAndTenant > identifier : %s and tenant: %s",
                        identifier, tenant),
                e);
            throw new DatabaseException(String
                .format("Error while findByIdentifierAndTenant > identifier : %s and tenant: %s",
                    identifier, tenant),
                e);
        }
    }

    @Override
    public Optional<Document> findByIdentifier(String identifier) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, identifier);
        try {
            FindIterable<Document> result = collection.find(eq("Identifier", identifier));
            if (result.iterator().hasNext()) {
                return Optional.of(result.first());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            LOGGER.error(String
                    .format("Error while findByIdentifierAndTenant > identifier : %s",
                        identifier),
                e);
            throw new DatabaseException(String
                .format("Error while findByIdentifierAndTenant > identifier : %s",
                    identifier),
                e);
        }
    }

    @Override
    public FindIterable<Document> findByFieldsDocuments(Map<String, String> fields, int mongoBatchSize,
        Integer tenant) {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tenant);
        if (fields == null || fields.isEmpty()) {
            return findDocuments(mongoBatchSize, tenant);
        }
        BasicDBObject filter = new BasicDBObject();
        fields.forEach((key, value) -> filter.append(key, value));
        filter.put(VitamDocument.TENANT_ID, tenant);
        return collection.find(filter).batchSize(mongoBatchSize);
    }


    @Override
    public FindIterable<Document> findDocuments(Collection<String> ids, Bson projection) {
        ParametersChecker.checkParameter("Id list is required ", ids);
        if (null == projection) {
            return collection.find(in(ID, ids));
        } else {
            return collection.find(in(ID, ids)).projection(projection);
        }
    }


    @Override
    public FindIterable<Document> findDocuments(int mongoBatchSize, Integer tenant) {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tenant);
        return collection.find(new BasicDBObject(VitamDocument.TENANT_ID, tenant)).batchSize(mongoBatchSize);
    }

    @Override
    public FindIterable<Document> findDocuments(int mongoBatchSize) {
        return collection.find().batchSize(mongoBatchSize);
    }

    @Override
    public FindIterable<Document> findDocuments(Bson query, int mongoBatchSize) {
        return collection.find(query).batchSize(mongoBatchSize);
    }

    @Override
    public void delete(List<String> ids, int tenant) {
        collection.deleteMany(
            Filters.and(
                Filters.in(VitamDocument.ID, ids),
                Filters.eq(VitamDocument.TENANT_ID, tenant)
            ));
    }
}
