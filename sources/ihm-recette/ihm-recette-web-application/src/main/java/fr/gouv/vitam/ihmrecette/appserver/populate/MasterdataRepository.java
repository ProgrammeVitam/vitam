/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ihmrecette.appserver.populate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mongodb.MongoException;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.model.InsertOneModel;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * insert into metadata in bulk mode
 */
public class MasterdataRepository {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MasterdataRepository.class);
    public static final String GUID = "GUID";
    public static final String TENANT_ID = "\"TENANT_ID\"";
    public static final String AGENCY_NAME = "AGENCY_NAME";
    public static final String CONTRACT_NAME = "CONTRACT_NAME";
    public static final String RULE_ID = "RULE_ID";
    public static final String RULE_CATEGORY = "RULE_CATEGORY";

    private MongoDatabase metadataDb;
    private TransportClient transportClient;

    private Map<VitamDataType, MongoCollection<Document>> mongoCollections = new HashMap<>();

    private static String RULE_TEMPLATE;

    private static String AGENCY_TEMPLATE;

    private static String ACCESS_CONTRACTS_TEMPLATE;

    public MasterdataRepository(MongoDatabase metadataDb, TransportClient transportClient) {
        this.metadataDb = metadataDb;
        this.transportClient = transportClient;
        try {
            AGENCY_TEMPLATE = StringUtils.getStringFromInputStream(
                MasterdataRepository.class.getResourceAsStream("/agency-template.json"));
            RULE_TEMPLATE = StringUtils.getStringFromInputStream(
                MasterdataRepository.class.getResourceAsStream("/rule-template.json"));
            ACCESS_CONTRACTS_TEMPLATE = StringUtils.getStringFromInputStream(
                MasterdataRepository.class.getResourceAsStream("/access-contract-template.json"));
        } catch (IOException e) {
            LOGGER.error("Fail to init referential template");
        }
        // init collections for available metadataTypes
        initCollections();
    }


    /**
     * Find a document by key-value
     *
     * @param  documentType to fetch
     * @param  options to fetch
     * @return Document if found
     */
    public Optional<Document> findDocumentByMap(VitamDataType documentType,
        Map<String, String> options) {

        List<Bson> conditions = new ArrayList<>();
        for (String option : options.keySet()) {
            conditions.add(eq(option, options.get(option)));
        }
        FindIterable<Document> models = this.getCollection(documentType).find(and(conditions));

        Document first = models.first();
        if (first == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(first);
        } catch (final IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * import agency by name
     *
     * @param agencyName identifier to import
     */
    public void importAgency(String agencyName, int tenantId) {
        String agencyToImport = AGENCY_TEMPLATE
            .replace(GUID, GUIDFactory.newEventGUID(tenantId).toString())
            .replace(TENANT_ID, tenantId + "")
            .replace(AGENCY_NAME, agencyName);

        Document agency = Document.parse(agencyToImport);
        try {
            this.getCollection(VitamDataType.AGENCIES).insertOne(agency);
        } catch (MongoException e) {
            LOGGER.error(e);
        }
        List<Document> agencies = new ArrayList<>();
        agencies.add(agency);
        indexDocuments(agencies, VitamDataType.AGENCIES, tenantId);

    }

    /**
     * import rule by id
     *
     * @param ruleId id to import
     */
    public void importRule(String ruleId, int tenantId) {
        String ruleToImport = RULE_TEMPLATE
            .replace(GUID, GUIDFactory.newEventGUID(tenantId).toString())
            .replace(TENANT_ID, tenantId + "")
            .replace(RULE_ID, ruleId)
            .replace(RULE_CATEGORY, getRuleCategoryByRuleId(ruleId));

        Document rule = Document.parse(ruleToImport);
        try {
            this.getCollection(VitamDataType.RULES).insertOne(rule);
        } catch (MongoException e) {
            LOGGER.error(e);
        }
        List<Document> rules = new ArrayList<>();
        rules.add(rule);
        indexDocuments(rules, VitamDataType.RULES, tenantId);
    }

    /**
     * import access contract by id
     *
     * @param contractId id to import
     */
    public void importAccessContract(String contractId, int tenantId) {
        String ruleToImport = ACCESS_CONTRACTS_TEMPLATE
            .replace(GUID, GUIDFactory.newEventGUID(tenantId).toString())
            .replace(TENANT_ID, tenantId + "")
            .replace(CONTRACT_NAME, contractId);

        Document rule = Document.parse(ruleToImport);
        this.getCollection(VitamDataType.ACCESS_CONTRACT).insertOne(rule);
        List<Document> rules = new ArrayList<>();
        rules.add(rule);
        indexDocuments(rules, VitamDataType.ACCESS_CONTRACT, tenantId);
    }

    /**
     * Store and index a document list 
     * 
     * @param tenant        tenant identifier
     * @param documents     documents to store and index
     * @param vitamDataType  dataType of documents
     * @param storeInDb     if true documents will be saved in DB
     * @param indexInEs     if true documents will be indexed in ES
     */
    private void storeAndIndex(int tenant, List<Document> documents, VitamDataType vitamDataType,
                               boolean storeInDb, boolean indexInEs){
        if(storeInDb){
            storeDocuments(documents, vitamDataType);
        }
        if(indexInEs) {
            indexDocuments(documents, vitamDataType, tenant);
        }
    }

    /**
     * store a list of documents in db
     *
     * @param documents    to store
     * @param vitamDataType of the documents to store
     */
    private void storeDocuments(List<Document> documents, VitamDataType vitamDataType) {
        List<InsertOneModel<Document>> collect =
                documents.stream().map(InsertOneModel::new).collect(Collectors.toList());

        BulkWriteResult bulkWriteResult = this.getCollection(vitamDataType).bulkWrite(collect);
        LOGGER.info("{}", bulkWriteResult.getInsertedCount());
    }

    /**
     * index a list of documents
     *
     * @param documents    to index
     * @param vitamDataType of the documents
     * @param tenant       related tenant
     */
    private void indexDocuments(List<Document> documents, VitamDataType vitamDataType, int tenant) {
        BulkRequestBuilder bulkRequestBuilder = transportClient.prepareBulk();

        documents.forEach(document -> {
            String id = (String) document.remove("_id");
            String source = document.toJson();
            bulkRequestBuilder
                    .add(transportClient.prepareIndex(vitamDataType.getIndexName(), VitamCollection.TYPEUNIQUE, id)
                            .setSource(source, XContentType.JSON));
        });

        BulkResponse bulkRes = bulkRequestBuilder.execute().actionGet();

        LOGGER.info("{}", bulkRes.getItems().length);
        if (bulkRes.hasFailures()) {
            LOGGER.error("##### Bulk Request failure with error: " + bulkRes.buildFailureMessage());
        }
    }

    /**
     * init collection's list for available metadataTypes
     */
    private void initCollections() {
        MongoCollection<Document> ruleCollection = metadataDb.getCollection(VitamDataType.RULES.getCollectionName());
        mongoCollections.put(VitamDataType.RULES, ruleCollection);
        MongoCollection<Document> agencyCollection = metadataDb.getCollection(VitamDataType.AGENCIES.getCollectionName());
        mongoCollections.put(VitamDataType.AGENCIES, agencyCollection);
        MongoCollection<Document> contractCollection = metadataDb.getCollection(VitamDataType.ACCESS_CONTRACT.getCollectionName());
        mongoCollections.put(VitamDataType.ACCESS_CONTRACT, contractCollection);
    }

    /**
     * Get a collection
     *
     * @param vitamDataType to fetch a collection for
     * @return MongoCollection
     */
    private MongoCollection<Document> getCollection(VitamDataType vitamDataType) {
        return mongoCollections.getOrDefault(vitamDataType,
                metadataDb.getCollection(VitamDataType.RULES.getCollectionName()));
    }


    public static String getRuleCategoryByRuleId(String ruleId) {
        if (ruleId.startsWith("ACC-")) {
            return "AccessRule";
        }
        if (ruleId.startsWith("APP-")) {
            return "AppraisalRule";
        }
        if (ruleId.startsWith("DIS-")) {
            return "DisseminationRule";
        }
        if (ruleId.startsWith("CLASS-")) {
            return "ClassificationRule";
        }
        if (ruleId.startsWith("REU-")) {
            return "ReuseRule";
        }
        return "StorageRule";
    }

}
