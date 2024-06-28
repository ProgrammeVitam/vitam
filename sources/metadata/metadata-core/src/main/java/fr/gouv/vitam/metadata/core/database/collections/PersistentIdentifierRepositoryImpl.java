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

import com.google.common.collect.Lists;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertOneModel;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.core.reconstruction.model.PurgedPersistentIdentifier;
import fr.gouv.vitam.metadata.core.reconstruction.repository.PersistentIdentifierRepository;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.TENANT_ID;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.VERSION;
import static fr.gouv.vitam.common.parameter.ParameterHelper.getTenantParameter;

public class PersistentIdentifierRepositoryImpl implements PersistentIdentifierRepository {

    public static final String PURGED_PERSISTENT_IDENTIFIER_COLLECTION = "PurgedPersistentIdentifier";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PersistentIdentifierRepositoryImpl.class);
    private static final String LAST_PERSISTENT_DATE = "lastPersistentDate";
    private static final String ALL_PARAMS_REQUIRED = "All params are required";

    private final MongoCollection<Document> purgedPersistentIdentifierCollection;

    public PersistentIdentifierRepositoryImpl(MongoDbAccessMetadataImpl mongoDbAccess) {
        this.purgedPersistentIdentifierCollection = mongoDbAccess
            .getMongoDatabase()
            .getCollection(PURGED_PERSISTENT_IDENTIFIER_COLLECTION);
    }

    public PersistentIdentifierRepositoryImpl(MongoDbAccessMetadataImpl mongoDbAccess, String prefix) {
        this.purgedPersistentIdentifierCollection = mongoDbAccess
            .getMongoDatabase()
            .getCollection(prefix + PURGED_PERSISTENT_IDENTIFIER_COLLECTION);
    }

    @Override
    public List<PurgedPersistentIdentifier> findByPersistentIdentifierAndTenant(
        String persistentIdentifier,
        Integer tenant,
        @Nullable String type
    ) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, persistentIdentifier, tenant);
        final List<Bson> filters = Lists.newArrayList(
            eq("persistentIdentifier.PersistentIdentifierContent", persistentIdentifier),
            eq(TENANT_ID, tenant)
        );
        if (type != null) {
            filters.add(eq("type", type));
        }
        final Bson query = and(filters);
        try {
            List<PurgedPersistentIdentifier> result = new ArrayList<>();
            final FindIterable<Document> documents = purgedPersistentIdentifierCollection.find(query);
            for (Document doc : documents) {
                result.add(PurgedPersistentIdentifier.fromDocument(doc));
            }
            return result;
        } catch (MongoException | InvalidParseOperationException e) {
            throw new DatabaseException(
                String.format(
                    "Error while findByPersistentIdentifierAndTenant > persistentIdentifier : %s and tenant: %s",
                    persistentIdentifier,
                    tenant
                ),
                e
            );
        }
    }

    @Override
    public void insert(List<Document> purgedPersistentIdentifiers) throws MetaDataExecutionException {
        BulkWriteOptions options = new BulkWriteOptions().ordered(false);
        try {
            List<InsertOneModel<Document>> insertModels = purgedPersistentIdentifiers
                .stream()
                .map(document -> {
                    document.append(VERSION, 0);
                    document.append(TENANT_ID, getTenantParameter());
                    document.append(LAST_PERSISTENT_DATE, LocalDateUtil.nowFormatted());
                    return new InsertOneModel<>(document);
                })
                .collect(Collectors.toList());
            purgedPersistentIdentifierCollection.bulkWrite(insertModels, options);
        } catch (MongoBulkWriteException e) {
            boolean hasBlockerErrors = false;
            for (BulkWriteError bulkWriteError : e.getWriteErrors()) {
                if (bulkWriteError.getCategory() == ErrorCategory.DUPLICATE_KEY) {
                    LOGGER.info(
                        "Document already exists " +
                        purgedPersistentIdentifiers.get(bulkWriteError.getIndex()) +
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
}
