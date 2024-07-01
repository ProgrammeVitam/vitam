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
package fr.gouv.vitam.metadata.core.database.collections;

import com.mongodb.BasicDBObject;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoException;
import com.mongodb.bulk.BulkWriteError;
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
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.TENANT_ID;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.VERSION;
import static fr.gouv.vitam.common.parameter.ParameterHelper.getTenantParameter;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.ATOMIC_VERSION;

public class MongoDbMetadataRepository<T extends VitamDocument<T>> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MongoDbMetadataRepository.class);

    private final Supplier<MongoCollection<T>> mongoCollectionSupplier;

    public MongoDbMetadataRepository(Supplier<MongoCollection<T>> mongoCollectionSupplier) {
        this.mongoCollectionSupplier = mongoCollectionSupplier;
    }

    public List<T> selectByIds(Iterable<? extends String> ids, BasicDBObject projection) {
        Bson condition = in(ID, ids);

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
        BulkWriteOptions options = new BulkWriteOptions().ordered(false);
        try {
            List<InsertOneModel<T>> collect = metadataDocuments
                .stream()
                .map(metadataDocument -> {
                    metadataDocument.append(VERSION, 0);
                    metadataDocument.append(ATOMIC_VERSION, 0);
                    metadataDocument.append(TENANT_ID, getTenantParameter());
                    return new InsertOneModel<>(metadataDocument);
                })
                .collect(Collectors.toList());
            mongoCollectionSupplier.get().bulkWrite(collect, options);
        } catch (MongoBulkWriteException e) {
            boolean hasBlockerErrors = false;
            for (BulkWriteError bulkWriteError : e.getWriteErrors()) {
                if (bulkWriteError.getCategory() == ErrorCategory.DUPLICATE_KEY) {
                    LOGGER.info(
                        "Document already exists " +
                        metadataDocuments.get(bulkWriteError.getIndex()).getId() +
                        ". Ignoring quietly (idempotency)"
                    );
                } else {
                    hasBlockerErrors = true;
                    LOGGER.error("An error occurred during metadata insert " + bulkWriteError);
                }
            }
            if (hasBlockerErrors) {
                throw new MetaDataExecutionException(e);
            }
        } catch (MongoException | IllegalArgumentException e) {
            throw new MetaDataExecutionException(e);
        }
    }

    public void delete(List<T> metadataDocuments) throws MetaDataExecutionException {
        BulkWriteOptions options = new BulkWriteOptions().ordered(false);
        try {
            List<DeleteOneModel<T>> toDeleteModels = metadataDocuments
                .stream()
                .map((Function<T, DeleteOneModel<T>>) DeleteOneModel::new)
                .collect(Collectors.toList());
            mongoCollectionSupplier.get().bulkWrite(toDeleteModels, options);
        } catch (MongoException | IllegalArgumentException e) {
            throw new MetaDataExecutionException(e);
        }
    }

    public void update(Map<String, Bson> updates) throws MetaDataExecutionException {
        BulkWriteOptions options = new BulkWriteOptions().ordered(false);
        try {
            List<UpdateOneModel<T>> collect = updates
                .entrySet()
                .stream()
                .map(
                    item ->
                        new UpdateOneModel<T>(
                            and(eq(ID, item.getKey()), eq(TENANT_ID, getTenantParameter())),
                            item.getValue()
                        )
                )
                .collect(Collectors.toList());
            mongoCollectionSupplier.get().bulkWrite(collect, options);
        } catch (MongoException | IllegalArgumentException e) {
            throw new MetaDataExecutionException(e);
        }
    }
}
