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
package fr.gouv.vitam.batch.report.rest.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ExtractedMetadata;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class ExtractedMetadataRepository {

    public static final String COLLECTION_NAME = "ExtractedMetadata";

    private final MongoCollection<Document> extractedMetadataForAuCollection;

    public ExtractedMetadataRepository(MongoCollection<Document> extractedMetadataForAuCollection) {
        this.extractedMetadataForAuCollection = extractedMetadataForAuCollection;
    }

    public void addExtractedMetadataForAu(List<ExtractedMetadata> extractedMetadatas) {
        List<UpdateOneModel<Document>> extractedMetadataDocuments = extractedMetadatas
            .stream()
            .map(this::toUpdateModel)
            .collect(Collectors.toList());

        BulkWriteOptions insertWithoutOrder = new BulkWriteOptions().ordered(false);
        extractedMetadataForAuCollection.bulkWrite(extractedMetadataDocuments, insertWithoutOrder);
    }

    private UpdateOneModel<Document> toUpdateModel(ExtractedMetadata metadata) {
        Bson filter = and(
            eq(ExtractedMetadata.ID, metadata.getId()),
            eq(ExtractedMetadata.PROCESS_ID, metadata.getProcessId()),
            eq(ExtractedMetadata.TENANT, metadata.getTenant())
        );

        return new UpdateOneModel<>(
            filter,
            new Document("$set", pojoToBson(metadata)),
            new UpdateOptions().upsert(true)
        );
    }

    public MongoCursor<ExtractedMetadata> getExtractedMetadataByProcessId(String processId, int tenant) {
        return extractedMetadataForAuCollection
            .find(and(eq(ExtractedMetadata.PROCESS_ID, processId), eq(ExtractedMetadata.TENANT, tenant)))
            .map(this::bsonToPojo)
            .cursor();
    }

    public void deleteExtractedMetadataByProcessId(String processId, int tenant) {
        extractedMetadataForAuCollection.deleteMany(
            and(eq(ExtractedMetadata.PROCESS_ID, processId), eq(ExtractedMetadata.TENANT, tenant))
        );
    }

    private Document pojoToBson(ExtractedMetadata metadata) {
        return Document.parse(JsonHandler.unprettyPrint(metadata));
    }

    private ExtractedMetadata bsonToPojo(Document document) {
        try {
            return BsonHelper.fromDocumentToObject(document, ExtractedMetadata.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }
}
