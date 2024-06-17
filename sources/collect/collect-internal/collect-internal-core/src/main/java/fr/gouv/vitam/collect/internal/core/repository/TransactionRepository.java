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
package fr.gouv.vitam.collect.internal.core.repository;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.apache.commons.collections.CollectionUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

/**
 * repository for collect entities  management in mongo.
 */
public class TransactionRepository {

    public static final String TRANSACTION_COLLECTION = "Transaction";
    public static final String ID = "_id";
    public static final String VERSION = "_v";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionRepository.class);
    public static final String TENANT_ID = "_tenant";
    public static final String CREATION_DATE = "context.CreationDate";
    public static final String SET = "$set";
    public static final String STATUS = "Status";
    public static final String LAST_UPDATE = "LastUpdate";

    private final MongoCollection<Document> transactionCollection;

    @VisibleForTesting
    public TransactionRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        transactionCollection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
    }

    public TransactionRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, TRANSACTION_COLLECTION);
    }

    /**
     * create a transaction model
     *
     * @param transactionModel transaction model to create
     * @throws CollectInternalException exception thrown in case of error
     */
    public void createTransaction(TransactionModel transactionModel) throws CollectInternalException {
        LOGGER.debug("Transaction to create: {}", transactionModel);
        try {
            transactionModel.setVersion(0);
            String transactionModelAsString = JsonHandler.writeAsString(transactionModel);
            transactionCollection.insertOne(Document.parse(transactionModelAsString));
        } catch (InvalidParseOperationException e) {
            throw new CollectInternalException("Error when creating transaction: " + e);
        }
    }

    /**
     * replace a transaction model
     *
     * @param transactionModel transaction model to replace
     * @throws CollectInternalException exception thrown in case of error
     */
    public void replaceTransaction(TransactionModel transactionModel) throws CollectInternalException {
        LOGGER.debug("Transaction to replace: {}", transactionModel);
        try {
            int atomicVersion = transactionModel.getVersion();
            transactionModel.setVersion(atomicVersion + 1);
            String transactionModelAsString = JsonHandler.writeAsString(transactionModel);
            final Bson condition = and(eq(ID, transactionModel.getId()), eq(VERSION, atomicVersion));
            UpdateResult result = transactionCollection.replaceOne(condition, Document.parse(transactionModelAsString));

            if (result.getModifiedCount() == 0) {
                throw new CollectInternalException(
                    "concurrency problem: the transaction was modified by another service"
                );
            }
        } catch (InvalidParseOperationException e) {
            throw new CollectInternalException("Error when replacing transaction: ", e);
        }
    }

    private UpdateOneModel<Document> getUpdateOneModel(TransactionModel transactionModel) {
        transactionModel.setLastUpdate(LocalDateUtil.nowFormatted());
        Document documentToUpdate = new Document()
            .append(
                SET,
                new BasicDBObject()
                    .append(STATUS, transactionModel.getStatus().name())
                    .append(VERSION, transactionModel.getVersion() + 1)
            );

        return new UpdateOneModel<>(
            and(eq(ID, transactionModel.getId()), eq(VERSION, transactionModel.getVersion())),
            documentToUpdate,
            new UpdateOptions()
        );
    }

    /**
     * replace a transaction model
     *
     * @param transactionsModel list des transactions model to replace
     * @throws CollectInternalException exception thrown in case of error
     * @deprecated : FIXME : Update only if "version = version - 1";
     */
    public void replaceTransactions(List<TransactionModel> transactionsModel) throws CollectInternalException {
        BulkWriteOptions options = new BulkWriteOptions().ordered(false);

        List<UpdateOneModel<Document>> listUpdate = new ArrayList<>();
        for (TransactionModel item : transactionsModel) {
            listUpdate.add(getUpdateOneModel(item));
        }
        transactionCollection.bulkWrite(listUpdate, options);
    }

    /**
     * return transaction according to id
     *
     * @param id transaction id to find
     * @return Optional<TransactionModel>
     * @throws CollectInternalException exception thrown in case of error
     */
    public Optional<TransactionModel> findTransaction(String id) throws CollectInternalException {
        LOGGER.debug("Transaction id to find : {}", id);
        try {
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            Bson query = and(eq(ID, id), eq(TENANT_ID, tenantId));
            Document first = transactionCollection.find(query).first();
            if (first == null) {
                return Optional.empty();
            }
            return Optional.of(BsonHelper.fromDocumentToObject(first, TransactionModel.class));
        } catch (InvalidParseOperationException e) {
            throw new CollectInternalException("Error when searching transaction by id: " + e);
        }
    }

    /**
     * return transaction according to query
     *
     * @param query transaction query to find
     * @return Optional<TransactionModel>
     * @throws CollectInternalException exception thrown in case of error
     */
    public Optional<TransactionModel> findTransactionByQuery(Bson query) throws CollectInternalException {
        try {
            Document first = getIterableTransactionsByQuery(query).sort(new BasicDBObject(CREATION_DATE, -1)).first();
            if (first == null) {
                return Optional.empty();
            }
            return Optional.of(BsonHelper.fromDocumentToObject(first, TransactionModel.class));
        } catch (InvalidParseOperationException e) {
            throw new CollectInternalException("Error when searching transaction by project id: " + e);
        }
    }

    private FindIterable<Document> getIterableTransactionsByQuery(Bson query) {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        Bson finalQuery = and(query, eq(TENANT_ID, tenantId));
        return transactionCollection.find(finalQuery);
    }

    public List<TransactionModel> findTransactionsByQuery(Bson query) throws CollectInternalException {
        List<TransactionModel> listTransactions = new ArrayList<>();
        try (MongoCursor<Document> transactions = this.getIterableTransactionsByQuery(query).cursor()) {
            while (transactions.hasNext()) {
                Document doc = transactions.next();
                listTransactions.add(BsonHelper.fromDocumentToObject(doc, TransactionModel.class));
            }
            return listTransactions;
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when fetching transactions: ", e);
            throw new CollectInternalException("Error when fetching transactions : " + e);
        }
    }

    public List<TransactionModel> findTransactionsByQueryWithoutTenant(Bson query) throws CollectInternalException {
        List<TransactionModel> listTransactions = new ArrayList<>();
        try (MongoCursor<Document> transactions = transactionCollection.find(query).cursor()) {
            while (transactions.hasNext()) {
                Document doc = transactions.next();
                listTransactions.add(BsonHelper.fromDocumentToObject(doc, TransactionModel.class));
            }
            return listTransactions;
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when fetching transactions: ", e);
            throw new CollectInternalException("Error when fetching transactions : " + e);
        }
    }

    /**
     * delete a transaction model
     *
     * @param id transaction to delete
     */
    public void deleteTransaction(String id) {
        LOGGER.debug("Transaction to delete Id: {}", id);
        transactionCollection.deleteOne(eq(ID, id));
        LOGGER.debug("Transaction deleted Id: {}", id);
    }

    /**
     * delete Transaction according to tenant and delay and status
     *
     * @param tenantId tenant id to find
     * @return Optional<ProjectModel>
     * @throws CollectInternalException exception thrown in case of error
     */
    public List<TransactionModel> getListTransactionToDeleteByTenant(Integer tenantId) throws CollectInternalException {
        LOGGER.debug("Transactions to delete : {}");
        try {
            Bson query = and(eq(TENANT_ID, tenantId), in("Status", "ACK_OK", "ACK_WARNING", "ABORTED"));
            List<TransactionModel> listTransactionToDelete = new ArrayList<>();
            MongoCursor<Document> transactionCursor = transactionCollection.find(query).cursor();
            while (transactionCursor.hasNext()) {
                Document doc = transactionCursor.next();
                listTransactionToDelete.add(BsonHelper.fromDocumentToObject(doc, TransactionModel.class));
            }
            return listTransactionToDelete;
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when fetching transaction to delete: ", e);
            throw new CollectInternalException("Error when fetching transaction to delete : " + e);
        }
    }

    public boolean findOneAndReplace(TransactionStatus transactionStatus, TransactionModel transactionModel)
        throws InvalidParseOperationException {
        return findOneAndReplace(Collections.singletonList(eq(STATUS, transactionStatus.toString())), transactionModel);
    }

    public boolean findOneAndReplace(TransactionModel transactionModel) throws InvalidParseOperationException {
        return findOneAndReplace(new ArrayList<>(), transactionModel);
    }

    public boolean findOneAndReplace(List<Bson> additionalFilters, TransactionModel transactionModel)
        throws InvalidParseOperationException {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        int atomicVersion = transactionModel.getVersion();
        transactionModel.setVersion(atomicVersion + 1);

        final List<Bson> filters = new ArrayList<>();
        filters.add(eq(ID, transactionModel.getId()));
        filters.add(eq(TENANT_ID, tenantId));
        filters.add(eq(VERSION, atomicVersion));

        if (CollectionUtils.isNotEmpty(additionalFilters)) {
            filters.addAll(additionalFilters);
        }

        Bson filter = Filters.and(filters);

        String transactionModelAsString = JsonHandler.writeAsString(transactionModel);
        Document documentToUpdate = Document.parse(transactionModelAsString);

        Document updatedDocument = transactionCollection.findOneAndReplace(filter, documentToUpdate);
        return (updatedDocument != null);
    }
}
