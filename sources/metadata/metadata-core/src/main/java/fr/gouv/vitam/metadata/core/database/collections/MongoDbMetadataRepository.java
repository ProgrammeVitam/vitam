package fr.gouv.vitam.metadata.core.database.collections;

import com.mongodb.BasicDBObject;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.UpdateOneModel;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.TENANT_ID;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.VERSION;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.ATOMIC_VERSION;

/**
 * Repository to access to metadata collection
 */
public class MongoDbMetadataRepository<T extends VitamDocument> {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(MongoDbMetadataRepository.class);

    private Supplier<MongoCollection<T>> mongoCollectionSupplier;

    public MongoDbMetadataRepository(Supplier<MongoCollection<T>> mongoCollectionSupplier) {
        this.mongoCollectionSupplier = mongoCollectionSupplier;
    }

    /**
     * @param ids list of parents
     * @param projection
     * @return the FindIterable on the find request based on the given collection
     */
    public Collection<T> selectByIds(Iterable<? extends String> ids, BasicDBObject projection) {
        // final Bson condition = and(in(ID, ids), eq(TENANT_ID, ParameterHelper.getTenantParameter()));
        final Bson condition = in(ID, ids);

        FindIterable<T> result = mongoCollectionSupplier.get().find(condition).projection(projection);

        List<T> vitamDocuments = new ArrayList<>();

        try (final MongoCursor<T> cursor = result.iterator()) {
            while (cursor.hasNext()) {
                final T vitamDocument = cursor.next();
                vitamDocuments.add(vitamDocument);
            }
        }

        return vitamDocuments;
    }

    public void insert(List<T> metadataDocuments) throws MetaDataExecutionException {

        BulkWriteOptions options = new BulkWriteOptions();
        options.ordered(false);

        try {
            List<InsertOneModel<T>> collect = new ArrayList<>();
            metadataDocuments.forEach(metadataDocument -> {
                metadataDocument.append(VERSION, 0);
                metadataDocument.append(ATOMIC_VERSION, 0);
                metadataDocument.append(TENANT_ID, ParameterHelper.getTenantParameter());
                InsertOneModel<T> tInsertOneModel = new InsertOneModel<>(metadataDocument);
                collect.add(tInsertOneModel);
            });
            BulkWriteResult bulkWriteResult = mongoCollectionSupplier.get().bulkWrite(collect, options);
            if (bulkWriteResult.getInsertedCount() != metadataDocuments.size()) {
                throw new MetaDataExecutionException(
                    String.format("Error while bulk save document count : %s != size : %s :",
                        bulkWriteResult.getInsertedCount(), metadataDocuments.size()));
            }
        } catch (final MongoBulkWriteException e) {

            boolean hasBlockerErrors = false;
            for (BulkWriteError bulkWriteError : e.getWriteErrors()) {
                if (bulkWriteError.getCategory() == ErrorCategory.DUPLICATE_KEY) {
                    LOGGER.warn("Document already exists " + metadataDocuments.get(bulkWriteError.getIndex()).getId() + ". Ignoring quietly (idempotency)");
                } else {
                    hasBlockerErrors = true;
                    LOGGER.error("An error occurred during metadata insert " + bulkWriteError);
                }
            }
            if(hasBlockerErrors) {
                throw new MetaDataExecutionException(e);
            }
        } catch (final MongoException | IllegalArgumentException e) {
            throw new MetaDataExecutionException(e);
        }
    }

    /**
     * delete bulk documents
     *
     * @param metadataDocuments metadataDocuments
     * @throws MetaDataExecutionException MetaDataExecutionException
     */
    public void delete(List<T> metadataDocuments) throws MetaDataExecutionException {

        BulkWriteOptions options = new BulkWriteOptions();
        options.ordered(false);
        try {
            List<DeleteOneModel<T>> collect = new ArrayList<>();
            metadataDocuments.forEach(metadataDocument -> {
                DeleteOneModel<T> tDeleteOneModel = new DeleteOneModel<>(metadataDocument);
                collect.add(tDeleteOneModel);
            });
            BulkWriteResult bulkWriteResult = mongoCollectionSupplier.get().bulkWrite(collect, options);
            if (bulkWriteResult.getDeletedCount() != metadataDocuments.size()) {
                throw new MetaDataExecutionException(
                    String.format("Error while bulk delete document count : %s != size : %s :",
                        bulkWriteResult.getDeletedCount(), metadataDocuments.size()));
            }
        } catch (final MongoException | IllegalArgumentException e) {
            throw new MetaDataExecutionException(e);
        }
    }

    public void update(Map<String, Bson> updates) throws MetaDataExecutionException {
        BulkWriteOptions options = new BulkWriteOptions();
        options.ordered(false);
        try {
            List<UpdateOneModel<T>> collect = updates.entrySet().stream()
                .map(item -> {
                    Bson query = and(
                        eq(VitamDocument.ID, item.getKey()),
                        eq(TENANT_ID, ParameterHelper.getTenantParameter()));
                    return new UpdateOneModel<T>(query, item.getValue());
                })
                .collect(Collectors.toList());
            BulkWriteResult bulkWriteResult = mongoCollectionSupplier.get().bulkWrite(collect, options);
            if (bulkWriteResult.getMatchedCount() != updates.size()) {
                throw new MetaDataExecutionException(
                    String.format("Error while bulk save document count : %s != size : %s :", bulkWriteResult.getInsertedCount(), updates.size()));
            }
        } catch (final MongoException | IllegalArgumentException e) {
            throw new MetaDataExecutionException(e);
        }

    }

}
