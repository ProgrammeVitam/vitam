/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.cold.server.simulator.repository;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.cold.server.simulator.exception.InaTapeProxyRepositoryException;
import fr.gouv.vitam.storage.cold.server.simulator.model.TapeDriveModel;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TapeDriveModel collection.
 * Manages CRUD operations for drive state persistence in the "tape_drive" collection.
 */
public class TapeDriveRepository {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeDriveRepository.class);

    private static final String VERSION_FIELD = "_v";

    private final MongoCollection<Document> collection;

    public TapeDriveRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    /**
     * Save or update a drive model entry (upsert by _id).
     */
    public void save(TapeDriveModel drive) throws InaTapeProxyRepositoryException {
        try {
            drive.setUpdateDate(LocalDateTime.now().toString());

            long currentVersion = drive.getVersion();
            drive.setVersion(currentVersion + 1);

            Document doc = toBson(drive);

            Bson filter = Filters.and(
                Filters.eq(TapeDriveModel.ID, drive.getId()),
                Filters.eq(VERSION_FIELD, currentVersion)
            );

            FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().upsert(currentVersion == 0);
            Document updated = collection.findOneAndReplace(filter, doc, options);

            // When inserting a new document (version 0), findOneAndReplace returns null (no old document)
            // Only throw error if we expected to update an existing document but got null
            if (updated == null && currentVersion > 0) {
                throw new InaTapeProxyRepositoryException("Optimistic lock conflict for drive id: " + drive.getId());
            }
        } catch (MongoException e) {
            throw new InaTapeProxyRepositoryException("Failed to save drive: " + drive.getId(), e);
        }
    }

    /**
     * Find drive by its ID (e.g. \"drive_0\").
     */
    public Optional<TapeDriveModel> findById(String id) throws InaTapeProxyRepositoryException {
        try {
            Document doc = collection.find(Filters.eq(TapeDriveModel.ID, id)).first();
            if (doc == null) {
                LOGGER.warn("Drive not found for id: {}", id);
                return Optional.empty();
            }
            return Optional.of(toModel(doc));
        } catch (MongoException | InvalidParseOperationException e) {
            throw new InaTapeProxyRepositoryException("Failed to read drive: " + id, e);
        }
    }

    /**
     * Find all drives, ordered by index.
     */
    public List<TapeDriveModel> findAll() throws InaTapeProxyRepositoryException {
        try {
            List<TapeDriveModel> results = new ArrayList<>();
            for (Document doc : collection.find()) {
                results.add(toModel(doc));
            }

            results.sort(Comparator.comparingInt(TapeDriveModel::getIndex));
            return results;
        } catch (MongoException | InvalidParseOperationException e) {
            throw new InaTapeProxyRepositoryException("Failed to read all tape drives", e);
        }
    }

    private TapeDriveModel toModel(Document doc) throws InvalidParseOperationException {
        return JsonHandler.getFromString(doc.toJson(), TapeDriveModel.class);
    }

    private Document toBson(TapeDriveModel model) {
        return Document.parse(JsonHandler.unprettyPrint(model));
    }
}
