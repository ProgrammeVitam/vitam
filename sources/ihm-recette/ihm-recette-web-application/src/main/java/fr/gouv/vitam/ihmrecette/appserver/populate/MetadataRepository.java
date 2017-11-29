/**
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
 */
package fr.gouv.vitam.ihmrecette.appserver.populate;

import static com.mongodb.client.model.Filters.eq;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.InsertOneModel;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * insert into metadata in bulk mode
 */
public class MetadataRepository {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataRepository.class);

    private MongoCollection<Document> mongoCollection;

    private TransportClient transportClient;

    public MetadataRepository(MongoCollection<Document> mongoCollection, TransportClient transportClient) {
        this.mongoCollection = mongoCollection;
        this.transportClient = transportClient;
    }

    public void store(int tenant, List<Document> units) {
        List<InsertOneModel<Document>> collect =
            units.stream().map(InsertOneModel::new).collect(Collectors.toList());

        BulkWriteResult bulkWriteResult = mongoCollection.bulkWrite(collect);
        LOGGER.info("{}", bulkWriteResult.getInsertedCount());

        BulkRequestBuilder bulkRequestBuilder = transportClient.prepareBulk();

        units.forEach(unit -> {
            String id = (String) unit.remove("_id");
            String source = unit.toJson();
            bulkRequestBuilder
                .add(transportClient.prepareIndex(String.format("%d_unit", tenant), "type_unique", id)
                    .setSource(source, XContentType.JSON));
        });

        BulkResponse bulkRes = bulkRequestBuilder.execute().actionGet();

        LOGGER.info("{}", bulkRes.getItems().length);
        if (bulkRes.hasFailures()) {
            LOGGER.error("##### Bulk Request failure with error: " + bulkRes.buildFailureMessage());
        }
    }

    public Optional<UnitModel> findUnitById(String rootId) {
        FindIterable<Document> models = mongoCollection.find(eq("_id", rootId));

        Document first = models.first();
        if (first == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(JsonHandler.getFromString(first.toJson(), UnitModel.class));
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }

}
