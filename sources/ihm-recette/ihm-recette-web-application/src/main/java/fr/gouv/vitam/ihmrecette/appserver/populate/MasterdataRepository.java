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

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.BsonHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

/**
 * insert into metadata in bulk mode
 */
public class MasterdataRepository {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MasterdataRepository.class);
    private static final String GUID = "GUID";
    private static final String TENANT_ID = "\"TENANT_ID\"";
    private static final String AGENCY_NAME = "AGENCY_NAME";
    private static final String TOTAL_DOCUMENTS = "TOTAL_DOCUMENTS";
    private static final String TOTAL_OBJECTS_SIZE = "TOTAL_OBJECTS_SIZE";
    private static final String CONTRACT_NAME = "CONTRACT_NAME";
    private static final String RULE_ID = "RULE_ID";
    private static final String RULE_CATEGORY = "RULE_CATEGORY";

    private MongoDatabase metadataDb;
    private Client transportClient;

    private Map<VitamDataType, MongoCollection<Document>> mongoCollections = new HashMap<>();

    private static String RULE_TEMPLATE;

    private static String AGENCY_TEMPLATE;
    private static String ACCESSION_REGISTER_SUMMARY_TEMPLATE;
    
    private static String ACCESS_CONTRACTS_TEMPLATE;

    public MasterdataRepository(MongoDatabase metadataDb, Client transportClient) {
        this.metadataDb = metadataDb;
        this.transportClient = transportClient;
        try {
            AGENCY_TEMPLATE = StringUtils.getStringFromInputStream(
                    MasterdataRepository.class.getResourceAsStream("/agency-template.json"));
            ACCESSION_REGISTER_SUMMARY_TEMPLATE = StringUtils.getStringFromInputStream(
                    MasterdataRepository.class.getResourceAsStream("/accession-register-summary.json"));
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
     * @param tenant
     * @param identifier
     * @return Document if found
     */
    public Optional<Document> findAgency(int tenant, String identifier) {

        List<Bson> conditions = new ArrayList<>();
        conditions.add(eq(PopulateService.TENANT, tenant));
        conditions.add(eq("Identifier", identifier));

        FindIterable<Document> models = this.getCollection(VitamDataType.AGENCIES).find(and(conditions));

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
     * Find a document by key-value
     *
     * @param tenant
     * @param identifier
     * @return Document if found
     */
    public Optional<Document> findAccessionRegitserSummary(int tenant, String identifier) {

        List<Bson> conditions = new ArrayList<>();
        conditions.add(eq(PopulateService.TENANT, tenant));
        conditions.add(eq("OriginatingAgency", identifier));

        FindIterable<Document> models = this.getCollection(VitamDataType.ACCESSION_REGISTER_SUMMARY).find(and(conditions));

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
     * Find a document by key-value
     * 
     * @param tenant
     * @param ruleId
     * @return Document if found
     */
    public Optional<Document> findRule(int tenant, String ruleId) {

        List<Bson> conditions = new ArrayList<>();
        conditions.add(eq(PopulateService.TENANT, tenant));
        conditions.add(eq("RuleId", ruleId));

        FindIterable<Document> models = this.getCollection(VitamDataType.RULES).find(and(conditions));

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
     * Find a document by key-value
     *
     * @param contract
     * @return Document if found
     */
    public Optional<Document> findAccessContract(String contract) {

        List<Bson> conditions = new ArrayList<>();
        conditions.add(eq("Name", contract));

        FindIterable<Document> models = this.getCollection(VitamDataType.ACCESS_CONTRACT).find(and(conditions));

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
     * @param agencyName
     * @param tenantId
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

    public void createAccessionRegisterSummary(int tenantId, String agencyName, int objectCount, int totalObjectSize) {
        String accessionRegisterSummaryToInsert = ACCESSION_REGISTER_SUMMARY_TEMPLATE
                .replace(GUID, GUIDFactory.newEventGUID(tenantId).toString())
                .replace(TENANT_ID, Integer.toString(tenantId))
                .replace(AGENCY_NAME, agencyName)
                .replace(TOTAL_DOCUMENTS, Integer.toString(objectCount))
                .replace(TOTAL_OBJECTS_SIZE, Integer.toString(totalObjectSize));

        Document accessionRegisterSummary = Document.parse(accessionRegisterSummaryToInsert);
        try {
            this.getCollection(VitamDataType.ACCESSION_REGISTER_SUMMARY).insertOne(accessionRegisterSummary);
        } catch (MongoException e) {
            LOGGER.error(e);
        }
        List<Document> accessionRegisterSummaries = new ArrayList<>();
        accessionRegisterSummaries.add(accessionRegisterSummary);
        indexDocuments(accessionRegisterSummaries, VitamDataType.ACCESSION_REGISTER_SUMMARY, tenantId);
    }

    /**
     * import rule by id
     * 
     * @param ruleId
     * @param tenantId
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
     * @param contractId
     * @param tenantId
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
     * index a list of documents
     *
     * @param documents     to index
     * @param vitamDataType of the documents
     * @param tenant        related tenant
     */
    private void indexDocuments(List<Document> documents, VitamDataType vitamDataType, int tenant) {
        BulkRequestBuilder bulkRequestBuilder = transportClient.prepareBulk();

        documents.forEach(document -> {
            String id = (String) document.remove("_id");
            String source = BsonHelper.stringify(document);
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
        MongoCollection<Document> AccessionRegisterSummary = metadataDb.getCollection(
                VitamDataType.ACCESSION_REGISTER_SUMMARY.getCollectionName());
        mongoCollections.put(VitamDataType.ACCESSION_REGISTER_SUMMARY, AccessionRegisterSummary);
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
