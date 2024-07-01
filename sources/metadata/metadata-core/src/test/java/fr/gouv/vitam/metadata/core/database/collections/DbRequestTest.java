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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.DeleteMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.InsertMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.DeleteParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.InsertParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserHelper;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.elasticsearch.model.ElasticsearchCollections;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchTestHelper;
import fr.gouv.vitam.common.exception.ArchiveUnitOntologyValidationException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.DurationData;
import fr.gouv.vitam.common.model.FacetBucket;
import fr.gouv.vitam.common.model.FacetResult;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileStatus;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyOrigin;
import fr.gouv.vitam.common.model.administration.OntologyType;
import fr.gouv.vitam.common.model.massupdate.ManagementMetadataAction;
import fr.gouv.vitam.common.model.massupdate.RuleAction;
import fr.gouv.vitam.common.model.massupdate.RuleActions;
import fr.gouv.vitam.common.model.massupdate.RuleCategoryAction;
import fr.gouv.vitam.common.model.massupdate.RuleCategoryActionDeletion;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.ontology.OntologyTestHelper;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.config.ElasticsearchExternalMetadataMapping;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.mapping.MappingLoader;
import fr.gouv.vitam.metadata.core.model.UpdatedDocument;
import fr.gouv.vitam.metadata.core.trigger.FieldHistoryManager;
import fr.gouv.vitam.metadata.core.trigger.History;
import fr.gouv.vitam.metadata.core.validation.CachedArchiveUnitProfileLoader;
import fr.gouv.vitam.metadata.core.validation.CachedSchemaValidatorLoader;
import fr.gouv.vitam.metadata.core.validation.MetadataValidationException;
import fr.gouv.vitam.metadata.core.validation.OntologyValidator;
import fr.gouv.vitam.metadata.core.validation.UnitValidator;
import net.javacrumbs.jsonunit.JsonAssert;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.isNull;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.missing;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.ne;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.nin;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.path;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.range;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.term;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.all;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.tenant;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.add;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.inc;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.min;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.push;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.set;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.unset;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataCollections.OBJECTGROUP;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataCollections.UNIT;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DbRequestTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequestTest.class);
    private static final Integer TENANT_ID_0 = 0;
    private static final Integer TENANT_ID_1 = 1;
    private static final Integer TENANT_ID_2 = 2;
    private static final String MY_INT = "MyInt";
    private static final String CREATED_DATE = "CreatedDate";
    private static final String DESCRIPTION = "Description";
    private static final String TITLE = "Title";
    private static final String MY_BOOLEAN = "MyBoolean";
    private static final String MY_FLOAT = "MyFloat";
    private static final String UNKNOWN_VAR = "unknown";
    private static final String VALUE_MY_TITLE = "MyTitle";
    private static final String ARRAY_VAR = "ArrayVar";
    private static final String ARRAY2_VAR = "Array2Var";
    private static final String EMPTY_VAR = "EmptyVar";
    private static final String _OPS = "_ops";
    private static final String ROOTS = "$roots";
    private static final String _UDS = "_uds";

    private static final String DATA = "$data";
    private static final String AU_TREE_NEGATIVE_DEPTH_LEVEL_ONE = "au_tree_negative_depth_level_one.json";
    private static final String AU_TREE_NEGATIVE_DEPTH_LEVEL_TWO = "au_tree_negative_depth_level_two.json";
    private static final String AU_INCORRECT_OFF_LIMIT = "au_incorrect_offset_limit.json";
    private static final String REQUEST_SELECT_TEST = "{$query: {$eq: {\"id\" : \"id\" }}, $projection : []}";
    private static final String UUID2 = "aebaaaaaaaaaaaabaahbcakzu2stfryaaaaq";
    private static final String REQUEST_INSERT_TEST_1 =
        "{ \"#id\": \"aebaaaaaaaaaaaabaahbcakzu2stfryabbaq\", \"id\": \"id\" }";
    private static final String REQUEST_INSERT_TEST_2 =
        "{ \"#id\": \"aeaqaaaaaaaaaaababid6akzxqwg6qqaaaaq\", \"id\": \"id\" }";
    private static final String REQUEST_SELECT_TEST_ES_1 =
        "{$query: { $match : { 'Description' : 'OK' , '$max_expansions' : 1  } }}";
    private static final String REQUEST_SELECT_TEST_ES_2 =
        "{$query: { $match : { 'Description' : 'dèscription OK' , '$max_expansions' : 1  } }}";
    private static final String REQUEST_SELECT_TEST_ES_3 =
        "{$query: { $match : { 'Description' : 'est OK description' , '$max_expansions' : 1  } }}";
    private static final String REQUEST_SELECT_TEST_ES_4 =
        "{$query: { $or : [ { $match : { 'Title' : 'Vitam' , '$max_expansions' : 1  } }, " +
        "{$match : { 'Description' : 'vitam' , '$max_expansions' : 1  } }" +
        "] } }";
    private static final String REQUEST_SELECT_TEST_ES_5 =
        "{$query: [ { $eq : { 'DescriptionLevel' : 'Item' } } ]," +
        "$facets : [ { $name : 'desc_level_facet' , $terms : { $field : 'DescriptionLevel', $size: 10, $order: 'ASC' } } ] }";
    private static final String REQUEST_SELECT_TEST_ES_6 =
        "{$query: [ { $eq : { 'DescriptionLevel' : 'Item' } } ]," +
        "$facets : [ { $name : 'EndDate' ,  \"$date_range\": { $field : 'EndDate',$format : 'yyyy' , \"$ranges\": [ {\"$from\": \"1800\",\"$to\": \"2080\"}]} } ] }";
    private static final String REQUEST_SELECT_TEST_ES_7 =
        "{$query: [ { $match : { 'Description' : 'OK' } } ]," +
        "$facets : [ { $name : 'filtersFacet',  $filters: { $query_filters: [ {$name:'has_desc_level', $query : { $exists : 'DescriptionLevel' } }]} } ] }";
    private static final String REQUEST_SELECT_TEST_ES_8 =
        "{$query: [ { $match : { 'Description' : 'OK' } } ]," +
        "$facets : [ { $name : 'has_validComputedInheritedRules',  $terms: { $field: '#validComputedInheritedRules', \"$order\" : \"ASC\", \"$size\" : 100 } }] }";

    private static final String REQUEST_INSERT_TEST_ES_1_TENANT_1 =
        "{ \"#id\": \"aebaaaaaaaaaaaabaahbcakzu2stfryaabaq\", " +
        "\"#tenant\": 1, " +
        "\"Title\": \"title vitam\", " +
        "\"Description\": \"description est OK\"," +
        "\"#management\" : {\"ClassificationRule\" : [ {\"Rule\" : \"RuleId\"} ] } }";
    private static final String REQUEST_INSERT_TEST_ES =
        "{ \"#id\": \"" +
        UUID2 +
        "\", " +
        "\"Title\": \"title vitam\", " +
        "\"Description\": \"description est OK\"," +
        "\"DescriptionLevel\": \"Item\"," +
        "\"EndDate\": \"2050-12-30\"," +
        "\"#management\" : {\"ClassificationRule\" : [ {\"Rule\" : \"RuleId\"} ] }," +
        "\"#validComputedInheritedRules\": \"true\"}";
    private static final String REQUEST_INSERT_TEST_ES_2 =
        "{ \"#id\": \"aeaqaaaaaet33ntwablhaaku6z67pzqaaaar\", \"Title\": \"title vitam\", \"Description\": \"description est OK\" , \"DescriptionLevel\": \"Item\" }";
    private static final String REQUEST_INSERT_TEST_ES_3 =
        "{ \"#id\": \"aeaqaaaaaet33ntwablhaaku6z67pzqaaaat\", \"Title\": \"title vitam\", \"Description\": \"description est OK\" , \"DescriptionLevel\": \"Item\" }";
    private static final String REQUEST_INSERT_TEST_ES_4 =
        "{ \"#id\": \"aeaqaaaaaet33ntwablhaaku6z67pzqaaaas\", \"Title\": \"title sociales test_abcd_underscore othervalue france.pdf\", \"Description\": \"description est OK\", \"DescriptionLevel\": \"Item\"  }";
    private static final String UUID1 = "aeaqaaaaaaaaaaabab4roakztdjqziaaaaaq";
    private static final String REQUEST_UPDATE_INDEX_TEST =
        "{$roots:['" +
        UUID1 +
        "'],$query:[],$filter:{},$action:[{$set:{'date':'09/09/2015'}},{$set:{'Title':'ArchiveDoubleTest'}}]}";
    private static final String REQUEST_UPDATE_COMPUTED_FIELD =
        "{$roots:['" +
        UUID1 +
        "'],$query:[],$filter:{},$action:[{$push:{'_elimination': [{'OperationId':'my_guid'}] }}, {'$set': {'_glpd':'2018-01-01T01-34-59'}}]}";
    private static final String REQUEST_SELECT_TEST_ES_UPDATE = "{$query: { $eq : { '#id' : '" + UUID1 + "' } }}";
    private static final String REQUEST_INSERT_TEST_ES_UPDATE = "REQUEST_INSERT_TEST_ES_UPDATE.json";
    private static final String REQUEST_INSERT_TEST_ES_UPDATE_KO =
        "{ \"#id\": \"aeaqaaaaaagbcaacabg44ak45e54criaaaaq\", \"#tenant\": 0, \"Title\": \"Archive3\", " +
        "\"_mgt\": {\"OriginatingAgency\": \"XXXXXXX\"}," +
        " \"DescriptionLevel\": \"toto\" }";
    private static final String REQUEST_UPDATE_INDEX_TEST_KO =
        "{$roots:['aeaqaaaaaagbcaacabg44ak45e54criaaaaq'],$query:[],$filter:{},$action:[{$set:{'date':'09/09/2015'}},{$set:{'title':'Archive2'}}]}";
    private static final String ADDITIONAL_SCHEMA =
        "{ \"$schema\": \"http://vitam-json-germain-schema.org/draft-04/schema#\", \"id\": \"http://example.com/root.json\", \"type\": \"object\", \"additionalProperties\": true, \"anyOf\": [ { \"required\": [ \"specificField\" ] } ], \"properties\": { \"specificField\": { \"description\": \"champ obligatoire - valeur = item\", \"type\": \"array\", \"items\": { \"description\": \"at least 1 element\", \"type\": \"string\" }, \"minItems\": 1 } } }";
    private static final String REQUEST_UPDATE_INDEX_TEST_KO_SECONDARY_SCHEMA =
        "{$roots:['" + UUID1 + "'],$query:[],$filter:{},$action:[{$set:{'ArchiveUnitProfile':'AdditionalSchema'}}]}";
    private static final String REQUEST_UPDATE_INDEX_TEST_OK_SECONDARY_SCHEMA =
        "{$roots:['" +
        UUID1 +
        "'],$query:[],$filter:{},$action:[{$set:{'specificField':['specificField']}}," +
        "{$set:{'ArchiveUnitProfile':'AdditionalSchema'}}]}";

    private static final String MAPPING_FOLDER = "mapping";

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(Unit.class, ObjectGroup.class)
    );

    private static final MongoDbVarNameAdapter mongoDbVarNameAdapter = new MongoDbVarNameAdapter();
    private static final MappingLoader mappingLoader;

    static {
        try {
            List<ElasticsearchExternalMetadataMapping> mappingData = Arrays.asList(
                new ElasticsearchExternalMetadataMapping(
                    MetadataCollections.UNIT.getName(),
                    PropertiesUtils.getResourcePath(
                        MAPPING_FOLDER + ElasticsearchCollections.UNIT.getMapping()
                    ).toString()
                ),
                new ElasticsearchExternalMetadataMapping(
                    MetadataCollections.OBJECTGROUP.getName(),
                    ElasticsearchTestHelper.loadObjectGroupMapping()
                )
            );
            mappingLoader = new MappingLoader(mappingData);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ElasticsearchMetadataIndexManager indexManager =
        MetadataCollectionsTestUtils.createTestIndexManager(
            Arrays.asList(TENANT_ID_0, TENANT_ID_1, TENANT_ID_2),
            emptyMap(),
            mappingLoader
        );

    private ElasticsearchAccess esClientWithoutVitamBehavior;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private FieldHistoryManager fieldHistoryManager;

    @BeforeClass
    public static void beforeClass() throws Exception {
        List<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );
        ElasticsearchAccessMetadata elasticsearchAccessMetadata = new ElasticsearchAccessMetadata(
            ElasticsearchRule.VITAM_CLUSTER,
            esNodes,
            indexManager
        );
        MetadataCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            GUIDFactory.newGUID().getId(),
            elasticsearchAccessMetadata
        );
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        MetadataCollectionsTestUtils.afterTestClass(indexManager, true);
    }

    private static final JsonNode buildQueryJsonWithOptions(String query, String data) throws Exception {
        return JsonHandler.getFromString(
            new StringBuilder()
                .append("{ $roots : [ ], ")
                .append("$query : [ " + query + " ], ")
                .append("$data : " + data + " }")
                .toString()
        );
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        fieldHistoryManager = mock(FieldHistoryManager.class);
        List<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );

        esClientWithoutVitamBehavior = new ElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER, esNodes);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void after() throws Exception {
        MetadataCollectionsTestUtils.afterTest(indexManager);
    }

    @Test
    @RunWithCustomExecutor
    public void testExecRequestWithAllSedaFields() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        final GUID uuid = GUIDFactory.newUnitGUID(TENANT_ID_0);
        try {
            final DbRequest dbRequest = new DbRequest();
            // INSERT
            final JsonNode insertRequest = createInsertRequestWithUUID(uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
            insertParser.parse(insertRequest);
            LOGGER.debug("InsertParser: {}", insertParser);
            // Now execute the request
            dbRequest.execInsertUnitRequest(insertParser);

            // SELECT
            JsonNode selectRequest = createSelectRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
            selectParser.parse(selectRequest);
            LOGGER.debug("SelectParser: {}", selectParser);
            // Now execute the request
            executeRequest(dbRequest, selectParser);

            // UPDATE
            final JsonNode unit = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile("generated_archive_unit.json")
            );
            final JsonNode updateRequest = createUnitUpdateRequestWithUUID(uuid, unit);

            // Now considering update request and parsing it as in Data Server (PATCH command)
            final UpdateParserMultiple updateParser = new UpdateParserMultiple(mongoDbVarNameAdapter);
            updateParser.parse(updateRequest);
            LOGGER.debug("UpdateParser: {}", updateParser);

            // Now execute the request
            OntologyValidator ontologyValidator = mock(OntologyValidator.class);
            doAnswer(args -> args.getArgument(0)).when(ontologyValidator).verifyAndReplaceFields(any());

            dbRequest.execUpdateRequest(
                updateParser,
                uuid.toString(),
                UNIT,
                ontologyValidator,
                mock(UnitValidator.class),
                Collections.emptyList(),
                true
            );

            // SELECT ALL
            final SelectMultiQuery select = new SelectMultiQuery();
            select.addUsedProjection(all()).addQueries(eq(id(), uuid.toString()));
            LOGGER.debug("SelectAllString: " + select.getFinalSelect());
            selectRequest = select.getFinalSelect();
            // Now considering select request and parsing it as in Data Server (GET command)
            selectParser.parse(selectRequest);
            LOGGER.debug("SelectParser: {}", selectParser);
            // Now execute the request
            executeRequest(dbRequest, selectParser);
        } finally {
            // clean
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid.toString()));
        }
    }

    /**
     * Test method for execRequest
     * .
     *
     * @throws MetaDataExecutionException
     */
    @Test
    @RunWithCustomExecutor
    public void testExecRequest() throws Exception {
        // input data
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        final GUID uuid = GUIDFactory.newUnitGUID(TENANT_ID_0);
        try {
            final DbRequest dbRequest = new DbRequest();
            // INSERT
            final JsonNode insertRequest = createInsertRequestWithUUID(uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
            insertParser.parse(insertRequest);
            LOGGER.debug("InsertParser: {}", insertParser);
            // Now execute the request
            dbRequest.execInsertUnitRequest(insertParser);

            // SELECT
            JsonNode selectRequest = createSelectRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
            selectParser.parse(selectRequest);
            LOGGER.debug("SelectParser: {}", selectParser);
            // Now execute the request
            executeRequest(dbRequest, selectParser);

            // UPDATE
            final JsonNode updateRequest = createUpdateRequestWithUUID(uuid);
            // Now considering update request and parsing it as in Data Server (PATCH command)
            final UpdateParserMultiple updateParser = new UpdateParserMultiple(mongoDbVarNameAdapter);
            updateParser.parse(updateRequest);
            LOGGER.debug("UpdateParser: {}", updateParser);

            // Now execute the request
            OntologyValidator ontologyValidator = mock(OntologyValidator.class);
            doAnswer(args -> args.getArgument(0)).when(ontologyValidator).verifyAndReplaceFields(any());

            dbRequest.execUpdateRequest(
                updateParser,
                uuid.toString(),
                UNIT,
                ontologyValidator,
                mock(UnitValidator.class),
                Collections.emptyList(),
                true
            );

            // SELECT ALL
            selectRequest = createSelectAllRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            selectParser.parse(selectRequest);
            LOGGER.debug("SelectParser: {}", selectParser);
            // Now execute the request
            executeRequest(dbRequest, selectParser);

            // DELETE
            final JsonNode deleteRequest = createDeleteRequestWithUUID(uuid);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            final DeleteParserMultiple deleteParser = new DeleteParserMultiple(mongoDbVarNameAdapter);
            deleteParser.parse(deleteRequest);
            LOGGER.debug("DeleteParser: " + deleteParser);
            // Now execute the request
            executeRequest(dbRequest, deleteParser);
        } finally {
            // clean
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid.toString()));
        }
    }

    /**
     * Test negative Depth on unit's graph -> depth < -1.
     */
    @Test
    @RunWithCustomExecutor
    public void testExecRequestWithNegativeDepthLevel_One() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        final String guidParent1 = "aeaqaaaaaaagsoddab2wcak75hnwm6aaaaba";
        final String guidParent2 = "aeaqaaaaaaegexzwab76uak74nta33aaaaba";
        final String guidChild = "aeaqaaaaaadu6tbzablxaak75hfyaoiaaaba";
        final String operation = "aedqaaaaachtcmknab2okak74ntaehqaaaaq";

        try {
            final DbRequest dbRequest = new DbRequest();
            final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
            final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);

            // prepare unit with one parent -> depth_negative_level = -1.
            final ObjectNode insertReq1 = (ObjectNode) createInsertRequestTreeWithParents(
                guidParent1,
                "Fake titre_A",
                operation
            );
            final ObjectNode insertReq2 = (ObjectNode) createInsertRequestTreeWithParents(
                guidParent2,
                "Fake titre_B",
                operation
            );
            final ObjectNode insertReq3 = (ObjectNode) createInsertRequestTreeWithParents(
                guidChild,
                "Fake titre_C",
                operation
            );
            insertReq3.set(ROOTS, JsonHandler.toJsonNode(Arrays.asList(guidParent1, guidParent2)));
            ObjectNode uds = JsonHandler.createObjectNode().put("1", guidParent1).put("2", guidParent2);
            ((ObjectNode) insertReq3.get(DATA)).set(_UDS, uds);

            // Insert data (insertReq1, insertReq2, insertReq3)
            insertParser.parse(insertReq1);
            LOGGER.debug("insertParser: {}", insertParser);
            dbRequest.execInsertUnitRequest(insertParser);

            insertParser.parse(insertReq2);
            LOGGER.debug("insertParser: {}", insertParser);
            dbRequest.execInsertUnitRequest(insertParser);

            insertParser.parse(insertReq3);
            LOGGER.debug("insertParser: {}", insertParser);
            dbRequest.execInsertUnitRequest(insertParser);

            // prepare select dsl query -> Select parents ot the given root corresponding on the depth (depth < -1)
            JsonNode selectQuery = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile(AU_TREE_NEGATIVE_DEPTH_LEVEL_ONE)
            );
            selectParser.parse(selectQuery);
            LOGGER.debug("selectParser: {}", selectParser);

            // get query's results
            final Result<MetadataDocument<?>> result = dbRequest.execRequest(selectParser, Collections.emptyList());

            LOGGER.debug("result size: {}", result.getNbResult());
            assertEquals(1L, result.getNbResult());
            final List<MetadataDocument<?>> docs = result.getFinal();
            assertEquals("Fake titre_A", docs.get(0).getString(TITLE));

            try {
                JsonNode selectIncorrectQuery = JsonHandler.getFromFile(
                    PropertiesUtils.getResourceFile(AU_INCORRECT_OFF_LIMIT)
                );
                selectParser.parse(selectIncorrectQuery);
                LOGGER.debug("selectParser: {}", selectParser);
                dbRequest.execRequest(selectParser, Collections.emptyList());
                fail("Should throw an exception as offset limit are incorrect");
            } catch (BadRequestException e) {
                // do nothing
            }
        } finally {
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, guidParent1));
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, guidParent2));
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, guidChild));
        }
    }

    /**
     * Test negative Depth on unit's graph -> depth < -2.
     */
    @Test
    @RunWithCustomExecutor
    public void testExecRequestWithNegativeDepthLevel_Two() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        final String guidParent = "aeaqaaaaaaagsoddab2wcak75hnwm6aaaaba";
        final String guidChild1 = "aeaqaaaaaadu6tbzablxaak75hfyaoiaaaba";
        final String guidChild2 = "aeaqaaaaaaegexzwab76uak74nta33aaaaba";
        final String operation = "aedqaaaaachtcmknab2okak74ntaehqaaaaq";

        try {
            final DbRequest dbRequest = new DbRequest();
            final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
            final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);

            // prepare unit with one parent ==> depth_negative_level = -2.
            final ObjectNode insertReq1 = (ObjectNode) createInsertRequestTreeWithParents(
                guidParent,
                "Fake titre_A",
                operation
            );

            final ObjectNode insertReq2 = (ObjectNode) createInsertRequestTreeWithParents(
                guidChild1,
                "Fake titre_B",
                operation
            );
            insertReq2.set(ROOTS, JsonHandler.toJsonNode(Arrays.asList(guidParent)));
            ObjectNode uds = JsonHandler.createObjectNode().put("1", guidParent);
            ((ObjectNode) insertReq2.get(DATA)).set(_UDS, uds);

            final ObjectNode insertReq3 = (ObjectNode) createInsertRequestTreeWithParents(
                guidChild2,
                "Fake titre_C",
                operation
            );
            insertReq3.set(ROOTS, JsonHandler.toJsonNode(Arrays.asList(guidParent, guidChild1)));
            ObjectNode uds1 = JsonHandler.createObjectNode().put("2", guidParent).put("1", guidChild1);
            ((ObjectNode) insertReq3.get(DATA)).set(_UDS, uds1);

            // Insert data (insertReq1, insertReq2, insertReq3)
            insertParser.parse(insertReq1);
            LOGGER.debug("insertParser: {}", insertParser);
            dbRequest.execInsertUnitRequest(insertParser);

            insertParser.parse(insertReq2);
            LOGGER.debug("insertParser: {}", insertParser);
            dbRequest.execInsertUnitRequest(insertParser);

            insertParser.parse(insertReq3);
            LOGGER.debug("insertParser: {}", insertParser);
            dbRequest.execInsertUnitRequest(insertParser);

            // prepare select dsl query -> Select parents ot the given root corresponding on the depth (depth < -2)
            JsonNode selectQuery = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile(AU_TREE_NEGATIVE_DEPTH_LEVEL_TWO)
            );
            selectParser.parse(selectQuery);
            LOGGER.debug("selectParser: {}", selectParser);

            // get query's results
            final Result<MetadataDocument<?>> result = dbRequest.execRequest(selectParser, Collections.emptyList());

            LOGGER.debug("result size: {}", result.getNbResult());
            assertEquals(2L, result.getNbResult());
            final List<MetadataDocument<?>> docs = result.getFinal();
            LOGGER.debug("result1 title: {}", docs.get(0).get(TITLE));
            assertTrue(docs.get(0).getString(TITLE).contains("Fake"));
            LOGGER.debug("result2 title: {}", docs.get(1).get(TITLE));
        } finally {
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, guidParent));
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, guidChild1));
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, guidChild2));
        }
    }

    /**
     * Test method for execRequest
     * .
     */
    @Test
    @RunWithCustomExecutor
    public void testExecRequestThroughRequestParserHelper() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        // input data
        final GUID uuid = GUIDFactory.newUnitGUID(TENANT_ID_0);
        try {
            final DbRequest dbRequest = new DbRequest();
            RequestParserMultiple requestParser = null;
            // INSERT
            final JsonNode insertRequest = createInsertRequestWithUUID(uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            InsertParserMultiple insertParserMultiple = (InsertParserMultiple) RequestParserHelper.getParser(
                insertRequest,
                mongoDbVarNameAdapter
            );
            LOGGER.debug("InsertParser: {}", insertParserMultiple);
            // Now execute the request
            dbRequest.execInsertUnitRequest(insertParserMultiple);

            // SELECT
            JsonNode selectRequest = createSelectRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            requestParser = RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // UPDATE
            final JsonNode updateRequest = createUpdateRequestWithUUID(uuid);
            // Now considering update request and parsing it as in Data Server (PATCH command)
            requestParser = RequestParserHelper.getParser(updateRequest, mongoDbVarNameAdapter);
            LOGGER.debug("UpdateParser: {}", requestParser);

            // Now execute the request
            OntologyValidator ontologyValidator = mock(OntologyValidator.class);
            doAnswer(args -> args.getArgument(0)).when(ontologyValidator).verifyAndReplaceFields(any());

            dbRequest.execUpdateRequest(
                requestParser,
                uuid.toString(),
                UNIT,
                ontologyValidator,
                mock(UnitValidator.class),
                Collections.emptyList(),
                true
            );

            // SELECT ALL
            selectRequest = createSelectAllRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            requestParser = RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // DELETE
            final JsonNode deleteRequest = createDeleteRequestWithUUID(uuid);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            requestParser = RequestParserHelper.getParser(deleteRequest, mongoDbVarNameAdapter);
            LOGGER.debug("DeleteParser: " + requestParser);

            executeRequest(dbRequest, requestParser);
        } finally {
            // clean
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid.toString()));
        }
    }

    /**
     * Test method for execRequest
     * .
     *
     * @throws InvalidParseOperationException
     * @throws MetaDataNotFoundException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataExecutionException
     */
    @Test
    @RunWithCustomExecutor
    public void testExecRequestThroughAllCommands() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        // input data
        final GUID uuid = GUIDFactory.newUnitGUID(TENANT_ID_0);
        try {
            final DbRequest dbRequest = new DbRequest();
            RequestParserMultiple requestParser = null;
            // INSERT
            final JsonNode insertRequest = createInsertRequestWithUUID(uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            InsertParserMultiple insertParserMultiple = (InsertParserMultiple) RequestParserHelper.getParser(
                insertRequest,
                mongoDbVarNameAdapter
            );
            LOGGER.debug("InsertParser: {}", insertParserMultiple);
            // Now execute the request
            dbRequest.execInsertUnitRequest(insertParserMultiple);

            // SELECT
            JsonNode selectRequest = createSelectRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            requestParser = RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // UPDATE
            final JsonNode updateRequest = clientRichUpdateBuild(uuid);
            // Now considering update request and parsing it as in Data Server (PATCH command)
            requestParser = RequestParserHelper.getParser(updateRequest, mongoDbVarNameAdapter);
            LOGGER.debug("UpdateParser: {}", requestParser);
            // Now execute the request
            OntologyValidator ontologyValidator = mock(OntologyValidator.class);
            doAnswer(args -> args.getArgument(0)).when(ontologyValidator).verifyAndReplaceFields(any());

            dbRequest.execUpdateRequest(
                requestParser,
                uuid.toString(),
                UNIT,
                ontologyValidator,
                mock(UnitValidator.class),
                Collections.emptyList(),
                true
            );

            // SELECT ALL
            selectRequest = createSelectAllRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            requestParser = RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT ALL
            selectRequest = clientRichSelectAllBuild(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            requestParser = RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // DELETE
            final JsonNode deleteRequest = createDeleteRequestWithUUID(uuid);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            requestParser = RequestParserHelper.getParser(deleteRequest, mongoDbVarNameAdapter);
            LOGGER.debug("DeleteParser: " + requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);
        } finally {
            // clean
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid.toString()));
        }
    }

    /**
     * Test method for execRequest
     * .
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void testExecRequestMultiple() throws Exception {
        // input data
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        final GUID uuid = GUIDFactory.newUnitGUID(TENANT_ID_0);
        final GUID uuid2 = GUIDFactory.newUnitGUID(TENANT_ID_0);
        try {
            final DbRequest dbRequest = new DbRequest();
            RequestParserMultiple requestParser = null;

            // INSERT
            JsonNode insertRequest = createInsertRequestWithUUID(uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            InsertParserMultiple insertParserMultiple1 = (InsertParserMultiple) RequestParserHelper.getParser(
                insertRequest,
                mongoDbVarNameAdapter
            );
            LOGGER.debug("InsertParser: {}", insertParserMultiple1);
            // Now execute the request
            dbRequest.execInsertUnitRequest(insertParserMultiple1);

            insertRequest = createInsertChild2ParentRequest(uuid2, uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            InsertParserMultiple insertParserMultiple2 = (InsertParserMultiple) RequestParserHelper.getParser(
                insertRequest,
                mongoDbVarNameAdapter
            );
            LOGGER.debug("InsertParser: {}", insertParserMultiple2);
            // Now execute the request
            dbRequest.execInsertUnitRequest(insertParserMultiple2);

            // SELECT based on UUID
            JsonNode selectRequest = createSelectRequestWithOnlyUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            requestParser = RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT based on UUID
            selectRequest = createSelectRequestWithOnlyUUID(uuid2);
            // Now considering select request and parsing it as in Data Server (GET command)
            requestParser = RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT
            selectRequest = createSelectRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            requestParser = RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT
            selectRequest = clientSelect2Build(uuid2);
            // Now considering select request and parsing it as in Data Server (GET command)
            requestParser = RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT
            selectRequest = clientSelectMultiQueryBuild(uuid, uuid2);
            // Now considering select request and parsing it as in Data Server (GET command)
            requestParser = RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // UPDATE
            final JsonNode updateRequest = createUpdateRequestWithUUID(uuid);
            // Now considering update request and parsing it as in Data Server (PATCH command)
            requestParser = RequestParserHelper.getParser(updateRequest, mongoDbVarNameAdapter);
            LOGGER.debug("UpdateParser: {}", requestParser);
            // Now execute the request
            OntologyValidator ontologyValidator = mock(OntologyValidator.class);
            doAnswer(args -> args.getArgument(0)).when(ontologyValidator).verifyAndReplaceFields(any());

            dbRequest.execUpdateRequest(
                requestParser,
                uuid.toString(),
                UNIT,
                ontologyValidator,
                mock(UnitValidator.class),
                Collections.emptyList(),
                true
            );

            // SELECT ALL
            selectRequest = createSelectAllRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            requestParser = RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // DELETE
            JsonNode deleteRequest = clientDelete2Build(uuid2);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            requestParser = RequestParserHelper.getParser(deleteRequest, mongoDbVarNameAdapter);
            LOGGER.debug("DeleteParser: " + requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);
            deleteRequest = createDeleteRequestWithUUID(uuid);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            requestParser = RequestParserHelper.getParser(deleteRequest, mongoDbVarNameAdapter);
            LOGGER.debug("DeleteParser: " + requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);
        } finally {
            // clean
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid.toString()));
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid2.toString()));
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testUpdateWithArchiveUnitProfileAndOntologyValidationOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID_0));

        String uuid = "aeaqaaaabeghay2jabzuaalbarkww4iaaaba";

        // Base ontology with custom external types
        List<OntologyModel> ontologyModels = JsonHandler.getFromInputStreamAsTypeReference(
            OntologyTestHelper.loadOntologies(),
            new TypeReference<List<OntologyModel>>() {}
        );
        ontologyModels.addAll(
            Arrays.asList(
                new OntologyModel()
                    .setType(OntologyType.BOOLEAN)
                    .setOrigin(OntologyOrigin.EXTERNAL)
                    .setIdentifier("Flag"),
                new OntologyModel()
                    .setType(OntologyType.LONG)
                    .setOrigin(OntologyOrigin.EXTERNAL)
                    .setIdentifier("Number")
            )
        );

        final Unit initialUnit = new Unit(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("unitToUpdate.json"))
        );

        UNIT.getCollection().insertOne(initialUnit);
        UNIT.getEsClient().insertFullDocument(UNIT, 0, uuid, initialUnit);

        // AUP Schema
        AdminManagementClientFactory adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
        doReturn(
            new RequestResponseOK<ArchiveUnitProfileModel>().addResult(
                new ArchiveUnitProfileModel()
                    .setControlSchema(PropertiesUtils.getResourceAsString("unitAUP_OK.json"))
                    .setStatus(ArchiveUnitProfileStatus.ACTIVE)
            )
        )
            .when(adminManagementClient)
            .findArchiveUnitProfilesByID("AUP_IDENTIFIER");
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = new CachedArchiveUnitProfileLoader(
            adminManagementClientFactory,
            100,
            300
        );

        // When
        final DbRequest dbRequest = new DbRequest(
            new MongoDbMetadataRepository<Unit>(() -> UNIT.getCollection()),
            new MongoDbMetadataRepository<ObjectGroup>(() -> OBJECTGROUP.getCollection()),
            fieldHistoryManager
        );

        final UpdateMultiQuery update = new UpdateMultiQuery();
        update.addActions(set("Title", "New Title"));
        update.addActions(set("Flag", "false"));
        update.addActions(set("Number", "12"));
        final UpdateParserMultiple updateParser = new UpdateParserMultiple(mongoDbVarNameAdapter);
        updateParser.parse(update.getFinalUpdate());

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(100, 300);

        OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        UnitValidator unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);

        UpdatedDocument updatedDocument = dbRequest.execUpdateRequest(
            updateParser,
            uuid,
            UNIT,
            ontologyValidator,
            unitValidator,
            Collections.emptyList(),
            true
        );

        // Then
        ObjectNode expectedUnit = (ObjectNode) BsonHelper.fromDocumentToJsonNode(initialUnit);
        expectedUnit.put("Title", "New Title");
        expectedUnit.putArray("Flag").add(false);
        expectedUnit.putArray("Number").add(12);
        expectedUnit.put("_v", 1);
        expectedUnit.put("_av", 1);

        String expected = JsonHandler.unprettyPrint(expectedUnit);

        var afterNode = UNIT.getCollection().find(Filters.eq("_id", uuid)).first();
        afterNode.remove(Unit.APPROXIMATE_UPDATE_DATE);
        String after = JsonHandler.unprettyPrint(afterNode);
        JsonAssert.assertJsonEquals(expected, after);
        JsonAssert.assertJsonEquals(
            BsonHelper.stringify(initialUnit),
            JsonHandler.unprettyPrint(updatedDocument.getBeforeUpdate())
        );
        ((ObjectNode) updatedDocument.getAfterUpdate()).remove(Unit.APPROXIMATE_UPDATE_DATE);
        JsonAssert.assertJsonEquals(expected, JsonHandler.unprettyPrint(updatedDocument.getAfterUpdate()));
        assertThat(updatedDocument.getDocumentId()).isEqualTo(uuid);
    }

    @RunWithCustomExecutor
    @Test
    public void testUpdateWithOntologyValidationFailure() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID_0));

        String uuid = "aeaqaaaabeghay2jabzuaalbarkww4iaaaba";

        // Base ontology with custom external types
        List<OntologyModel> ontologyModels = JsonHandler.getFromInputStreamAsTypeReference(
            OntologyTestHelper.loadOntologies(),
            new TypeReference<List<OntologyModel>>() {}
        );
        ontologyModels.addAll(
            Arrays.asList(
                new OntologyModel()
                    .setType(OntologyType.BOOLEAN)
                    .setOrigin(OntologyOrigin.EXTERNAL)
                    .setIdentifier("Flag"),
                new OntologyModel()
                    .setType(OntologyType.LONG)
                    .setOrigin(OntologyOrigin.EXTERNAL)
                    .setIdentifier("Number")
            )
        );

        final Unit initialUnit = new Unit(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("unitToUpdate.json"))
        );

        UNIT.getCollection().insertOne(initialUnit);
        UNIT.getEsClient().insertFullDocument(UNIT, 0, uuid, initialUnit);

        // No AUP Schema
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = mock(CachedArchiveUnitProfileLoader.class);

        // When
        final DbRequest dbRequest = new DbRequest(
            new MongoDbMetadataRepository<Unit>(() -> UNIT.getCollection()),
            new MongoDbMetadataRepository<ObjectGroup>(() -> OBJECTGROUP.getCollection()),
            fieldHistoryManager
        );
        final UpdateMultiQuery update = new UpdateMultiQuery();
        update.addActions(set("Title", "New Title"));
        update.addActions(set("Flag", "TEXT"));
        final UpdateParserMultiple updateParser = new UpdateParserMultiple(mongoDbVarNameAdapter);
        updateParser.parse(update.getFinalUpdate());

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(100, 300);

        OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        UnitValidator unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);

        // Then
        assertThatThrownBy(
            () ->
                dbRequest.execUpdateRequest(
                    updateParser,
                    uuid,
                    UNIT,
                    ontologyValidator,
                    unitValidator,
                    Collections.emptyList(),
                    true
                )
        ).isInstanceOf(MetadataValidationException.class);

        String expected = BsonHelper.stringify(initialUnit);
        String after = JsonHandler.unprettyPrint(UNIT.getCollection().find(Filters.eq("_id", uuid)).first());
        JsonAssert.assertJsonEquals(expected, after);
    }

    @RunWithCustomExecutor
    @Test
    public void testUpdateWithArchiveUnitProfileValidationFailure() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID_0));

        String uuid = "aeaqaaaabeghay2jabzuaalbarkww4iaaaba";

        // Base ontology
        List<OntologyModel> ontologyModels = JsonHandler.getFromInputStreamAsTypeReference(
            OntologyTestHelper.loadOntologies(),
            new TypeReference<List<OntologyModel>>() {}
        );

        final Unit initialUnit = new Unit(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("unitToUpdate.json"))
        );

        UNIT.getCollection().insertOne(initialUnit);
        UNIT.getEsClient().insertFullDocument(UNIT, 0, uuid, initialUnit);

        // AUP Schema
        String controlSchema = PropertiesUtils.getResourceAsString("unitAUP_KO.json");
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = mock(CachedArchiveUnitProfileLoader.class);
        doReturn(
            Optional.of(
                new ArchiveUnitProfileModel().setStatus(ArchiveUnitProfileStatus.ACTIVE).setControlSchema(controlSchema)
            )
        )
            .when(archiveUnitProfileLoader)
            .loadArchiveUnitProfile("AUP_IDENTIFIER");

        // When
        final DbRequest dbRequest = new DbRequest();
        final UpdateMultiQuery update = new UpdateMultiQuery();
        update.addActions(set("Title", "New Title"));
        update.addActions(set("Boo.Baa", "Illegal string value"));
        update.addActions(set("ArchiveUnitProfile", "AUP_IDENTIFIER"));

        final UpdateParserMultiple updateParser = new UpdateParserMultiple(mongoDbVarNameAdapter);
        updateParser.parse(update.getFinalUpdate());

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(100, 300);

        OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        UnitValidator unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);

        // Then
        assertThatThrownBy(
            () ->
                dbRequest.execUpdateRequest(
                    updateParser,
                    uuid,
                    UNIT,
                    ontologyValidator,
                    unitValidator,
                    Collections.emptyList(),
                    true
                )
        ).isInstanceOf(MetadataValidationException.class);

        String expected = BsonHelper.stringify(initialUnit);
        String after = JsonHandler.unprettyPrint(UNIT.getCollection().find(Filters.eq("_id", uuid)).first());
        JsonAssert.assertJsonEquals(expected, after);
    }

    @RunWithCustomExecutor
    @Test
    public void testUpdateWithInternalSchemaValidationFailure() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID_0));

        String uuid = "aeaqaaaabeghay2jabzuaalbarkww4iaaaba";

        // Base ontology
        List<OntologyModel> ontologyModels = JsonHandler.getFromInputStreamAsTypeReference(
            OntologyTestHelper.loadOntologies(),
            new TypeReference<List<OntologyModel>>() {}
        );

        final Unit initialUnit = new Unit(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("unitToUpdate.json"))
        );

        UNIT.getCollection().insertOne(initialUnit);
        UNIT.getEsClient().insertFullDocument(UNIT, 0, uuid, initialUnit);

        // No external AUP Schema
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = mock(CachedArchiveUnitProfileLoader.class);

        // When
        final DbRequest dbRequest = new DbRequest();
        final UpdateMultiQuery update = new UpdateMultiQuery();
        update.addActions(unset("Title"));

        final UpdateParserMultiple updateParser = new UpdateParserMultiple(mongoDbVarNameAdapter);
        updateParser.parse(update.getFinalUpdate());

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(100, 300);

        OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        UnitValidator unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);

        // Then
        assertThatThrownBy(
            () ->
                dbRequest.execUpdateRequest(
                    updateParser,
                    uuid,
                    UNIT,
                    ontologyValidator,
                    unitValidator,
                    Collections.emptyList(),
                    true
                )
        ).isInstanceOf(MetadataValidationException.class);

        String expected = BsonHelper.stringify(initialUnit);
        String after = JsonHandler.unprettyPrint(UNIT.getCollection().find(Filters.eq("_id", uuid)).first());
        JsonAssert.assertJsonEquals(expected, after);
    }

    /**
     * Test method for
     * execRequest
     * with sorts.
     */
    @Test
    @RunWithCustomExecutor
    public void testExecRequestWithSort() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        final GUID uuid1 = GUIDFactory.newUnitGUID(TENANT_ID_0);
        final GUID uuid2 = GUIDFactory.newUnitGUID(TENANT_ID_0);

        try {
            final DbRequest dbRequest = new DbRequest();
            final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
            final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
            // INSERT
            final ObjectNode insertRequest1 = (ObjectNode) createInsertRequestWithUUID(uuid1);
            ((ObjectNode) insertRequest1.get("$data")).put(TITLE, "mon titreA Complet");
            final ObjectNode insertRequest2 = (ObjectNode) createInsertRequestWithUUID(uuid2);
            ((ObjectNode) insertRequest2.get("$data")).put(TITLE, "mon titreB Complet");
            insertRequest2.putArray("_up").addAll((ArrayNode) JsonHandler.toJsonNode(Arrays.asList(uuid1)));
            // insert1
            insertParser.parse(insertRequest1);
            LOGGER.debug("InsertParser1: {}", insertParser);
            dbRequest.execInsertUnitRequest(insertParser);
            // insert2
            insertParser.parse(insertRequest2);
            LOGGER.debug("InsertParser2: {}", insertParser);
            dbRequest.execInsertUnitRequest(insertParser);

            // SELECT
            // select with desc sort on title and one query
            SelectMultiQuery selectRequest = new SelectMultiQuery();
            selectRequest.addUsedProjection(all());
            selectRequest.addQueries(
                or().add(eq(id(), uuid1.toString())).add(eq(id(), uuid2.toString())).setDepthLimit(2)
            );
            selectRequest.addOrderByDescFilter(TITLE);
            selectParser.parse(selectRequest.getFinalSelect());
            LOGGER.debug("SelectParser: {}", selectParser);
            final Result result0 = dbRequest.execRequest(selectParser, Collections.emptyList());
            assertEquals(2L, result0.getNbResult());
            final List<MetadataDocument<?>> list = result0.getFinal();
            LOGGER.warn(list.toString());
            assertEquals("mon titreB Complet", list.get(0).getString(TITLE));
            assertEquals("mon titreA Complet", list.get(1).getString(TITLE));

            // select with desc sort on title and two queries
            selectRequest = new SelectMultiQuery();
            selectRequest.addUsedProjection(all());
            selectRequest.addQueries(
                eq(MY_BOOLEAN, false),
                or().add(eq(id(), uuid1.toString())).add(eq(id(), uuid2.toString())).setDepthLimit(0)
            );
            selectRequest.addOrderByDescFilter(TITLE);
            selectParser.parse(selectRequest.getFinalSelect());
            LOGGER.debug("SelectParser: {}", selectParser);
            final Result result1 = dbRequest.execRequest(selectParser, Collections.emptyList());
            assertEquals(2L, result1.getNbResult());
            final List<MetadataDocument<?>> list1 = result1.getFinal();
            assertEquals("mon titreB Complet", list1.get(0).getString(TITLE));
            assertEquals("mon titreA Complet", list1.get(1).getString(TITLE));

            // select with desc sort on title and two queries and elastic search
            selectRequest = new SelectMultiQuery();
            selectRequest.addUsedProjection(all());
            selectRequest.addQueries(
                match(TITLE, "mon Complet").setDepthLimit(0),
                or().add(eq(id(), uuid1.toString())).add(eq(id(), uuid2.toString())).setDepthLimit(0)
            );
            selectRequest.addOrderByDescFilter(TITLE);
            selectParser.parse(selectRequest.getFinalSelect());
            LOGGER.debug("SelectParser: {}", selectParser);
            final Result result2 = dbRequest.execRequest(selectParser, Collections.emptyList());
            assertEquals(2L, result2.getNbResult());
            final List<MetadataDocument<?>> list2 = result2.getFinal();
            assertEquals("mon titreB Complet", list2.get(0).getString(TITLE));
            assertEquals("mon titreA Complet", list2.get(1).getString(TITLE));
        } finally {
            // clean
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid1.toString()));
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid2.toString()));
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testInsertUnitRequest() throws Exception {
        final GUID uuid = GUIDFactory.newUnitGUID(TENANT_ID_0);
        final GUID uuid2 = GUIDFactory.newUnitGUID(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        final DbRequest dbRequest = new DbRequest();
        InsertParserMultiple insertParser;

        insertParser = (InsertParserMultiple) RequestParserHelper.getParser(
            createInsertRequestWithUUID(uuid),
            mongoDbVarNameAdapter
        );
        dbRequest.execInsertUnitRequest(insertParser);
        assertEquals(1, UNIT.getCollection().countDocuments());

        insertParser = (InsertParserMultiple) RequestParserHelper.getParser(
            createInsertChild2ParentRequest(uuid2, uuid),
            mongoDbVarNameAdapter
        );
        dbRequest.execInsertUnitRequest(insertParser);
        assertEquals(2, UNIT.getCollection().countDocuments());
    }

    @Test
    @RunWithCustomExecutor
    public void shouldIndexElasticSearchWithGoodUnitSchema() throws Exception {
        final GUID uuid = GUIDFactory.newUnitGUID(TENANT_ID_0);
        final GUID uuid2 = GUIDFactory.newUnitGUID(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        final DbRequest dbRequest = new DbRequest();

        InsertParserMultiple insertParserMultiple1 = (InsertParserMultiple) RequestParserHelper.getParser(
            createInsertRequestWithUUID(uuid),
            mongoDbVarNameAdapter
        );
        dbRequest.execInsertUnitRequest(insertParserMultiple1);
        InsertParserMultiple insertParserMultiple2 = (InsertParserMultiple) RequestParserHelper.getParser(
            createInsertChild2ParentRequest(uuid2, uuid),
            mongoDbVarNameAdapter
        );
        dbRequest.execInsertUnitRequest(insertParserMultiple2);

        final QueryBuilder qb1 = QueryBuilders.matchPhraseQuery(TITLE, VALUE_MY_TITLE + 2);
        final QueryBuilder qb2 = QueryBuilders.matchPhraseQuery(TITLE, VALUE_MY_TITLE);
        // (Test for ES upgrade version): match phrase prefix not supported by vitam for not analysed document
        final QueryBuilder qb3 = QueryBuilders.matchPhraseQuery(MY_INT, 10);
        // (Test for ES upgrade version): match phrase prefix not supported by vitam for not analysed document
        final QueryBuilder qb4 = QueryBuilders.matchPhraseQuery(MY_INT, 20);
        final QueryBuilder qb5 = QueryBuilders.prefixQuery("underscore", "undersco");
        final QueryBuilder qb6 = QueryBuilders.prefixQuery("_underscore", "undersco");

        // (Test for ES upgrade version): match phrase prefix not supported keywords and field mapped as number
        final QueryBuilder qb7 = QueryBuilders.matchPhrasePrefixQuery("_nbc", 100);

        // (Test for ES upgrade version): match phrase prefix not supported by vitam for not analysed document
        final QueryBuilder qb8 = QueryBuilders.matchPhrasePrefixQuery("_unitType", "obj");

        SearchResponse response = esClientWithoutVitamBehavior.search(
            indexManager.getElasticsearchIndexAliasResolver(UNIT).resolveIndexName(TENANT_ID_0),
            qb1,
            null,
            null,
            null,
            0,
            1000,
            null,
            null,
            null,
            false
        );
        assertEquals(1, response.getHits().getTotalHits().value);

        response = esClientWithoutVitamBehavior.search(
            indexManager.getElasticsearchIndexAliasResolver(UNIT).resolveIndexName(TENANT_ID_0),
            qb2,
            null,
            null,
            null,
            0,
            1000,
            null,
            null,
            null,
            false
        );
        assertEquals(2, response.getHits().getTotalHits().value);

        response = esClientWithoutVitamBehavior.search(
            indexManager.getElasticsearchIndexAliasResolver(UNIT).resolveIndexName(TENANT_ID_0),
            qb3,
            null,
            null,
            null,
            0,
            1000,
            null,
            null,
            null,
            false
        );
        assertEquals(1, response.getHits().getTotalHits().value);

        response = esClientWithoutVitamBehavior.search(
            indexManager.getElasticsearchIndexAliasResolver(UNIT).resolveIndexName(TENANT_ID_0),
            qb4,
            null,
            null,
            null,
            0,
            1000,
            null,
            null,
            null,
            false
        );
        assertEquals(1, response.getHits().getTotalHits().value);

        response = esClientWithoutVitamBehavior.search(
            indexManager.getElasticsearchIndexAliasResolver(UNIT).resolveIndexName(TENANT_ID_0),
            qb5,
            null,
            null,
            null,
            0,
            1000,
            null,
            null,
            null,
            false
        );
        assertEquals(1, response.getHits().getTotalHits().value);

        response = esClientWithoutVitamBehavior.search(
            indexManager.getElasticsearchIndexAliasResolver(UNIT).resolveIndexName(TENANT_ID_0),
            qb6,
            null,
            null,
            null,
            0,
            1000,
            null,
            null,
            null,
            false
        );
        assertEquals(1, response.getHits().getTotalHits().value);

        try {
            response = esClientWithoutVitamBehavior.search(
                indexManager.getElasticsearchIndexAliasResolver(UNIT).resolveIndexName(TENANT_ID_0),
                qb7,
                null,
                null,
                null,
                0,
                1000,
                null,
                null,
                null,
                false
            );
            fail("should throws exception as matchPhrasePrefixQuery is no allowed for numbers");
        } catch (BadRequestException e) {
            //
        }

        try {
            response = esClientWithoutVitamBehavior.search(
                indexManager.getElasticsearchIndexAliasResolver(UNIT).resolveIndexName(TENANT_ID_0),
                qb8,
                null,
                null,
                null,
                0,
                1000,
                null,
                null,
                null,
                false
            );
            fail("should throws exception as matchPhrasePrefixQuery is no allowed for keyWords");
        } catch (BadRequestException e) {
            //
        }
    }

    /**
     * @param dbRequest
     * @param requestParser
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws SecurityException
     * @throws IllegalArgumentException
     */
    private void executeRequest(DbRequest dbRequest, RequestParserMultiple requestParser) throws Exception {
        final Result result = dbRequest.execRequest(requestParser, Collections.emptyList());
        LOGGER.warn("XXXXXXXX " + requestParser.getClass().getSimpleName() + " Result XXXXXXXX: " + result);
        assertEquals("Must have 1 result", result.getNbResult(), 1);
        assertEquals("Must have 1 result", result.getCurrentIds().size(), 1);
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_0);
        UNIT.getEsClient().refreshIndex(OBJECTGROUP, TENANT_ID_0);
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createDeleteRequestWithUUID(GUID uuid) throws Exception {
        final DeleteMultiQuery delete = new DeleteMultiQuery();
        delete.addQueries(and().add(eq(id(), uuid.toString()), eq(TITLE, VALUE_MY_TITLE)));
        LOGGER.debug("DeleteString: " + delete.getFinalDelete());
        return delete.getFinalDelete();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createSelectAllRequestWithUUID(GUID uuid)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectMultiQuery select = new SelectMultiQuery();
        select.addUsedProjection(all()).addQueries(and().add(eq(id(), uuid.toString()), match(TITLE, VALUE_MY_TITLE)));
        LOGGER.debug("SelectAllString: " + select.getFinalSelect());
        return select.getFinalSelect();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createUpdateRequestWithUUID(GUID uuid) throws InvalidCreateOperationException {
        final UpdateMultiQuery update = new UpdateMultiQuery();
        update
            .addActions(set("NewVar", false), inc(MY_INT, 2), set(DESCRIPTION, "New description"))
            .addQueries(and().add(eq(id(), uuid.toString()), match(TITLE, VALUE_MY_TITLE)));
        LOGGER.debug("UpdateString: " + update.getFinalUpdate());
        return update.getFinalUpdate();
    }

    private JsonNode createUnitUpdateRequestWithUUID(GUID uuid, JsonNode unit) throws Exception {
        final UpdateMultiQuery update = new UpdateMultiQuery();

        Map<String, JsonNode> map = new HashMap<>();
        unit.fieldNames().forEachRemaining(key -> map.put(key, unit.get(key)));

        update.addActions(set(map));
        update.addQueries(eq(id(), uuid.toString()));
        LOGGER.debug("UpdateString: " + update.getFinalUpdate());
        return update.getFinalUpdate();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createSelectRequestWithUUID(GUID uuid)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectMultiQuery select = new SelectMultiQuery();
        select
            .addUsedProjection(id(), TITLE, DESCRIPTION)
            .addQueries(and().add(eq(id(), uuid.toString()), match(TITLE, VALUE_MY_TITLE)));
        LOGGER.debug("SelectString: " + select.getFinalSelect());
        return select.getFinalSelect();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createSelectRequestWithOnlyUUID(GUID uuid) throws InvalidCreateOperationException {
        final SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(eq(id(), uuid.toString()).setDepthLimit(10));
        LOGGER.debug("SelectString: " + select.getFinalSelect());
        return select.getFinalSelect();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createInsertRequestWithUUID(GUID uuid) throws InvalidParseOperationException {
        // INSERT
        final List<String> list = Arrays.asList("val1", "val2");
        final ObjectNode data = JsonHandler.createObjectNode()
            .put(id(), uuid.toString())
            .put(TITLE, VALUE_MY_TITLE)
            .put(DESCRIPTION, "Ma description est bien détaillée")
            .put(CREATED_DATE, "" + LocalDateUtil.now())
            .put(MY_INT, 20)
            .put(tenant(), TENANT_ID_0)
            .put("underscore", "underscore")
            .put("_underscore", "underscore")
            .put("_nbc", 100)
            .put("_unitType", "object")
            .put(MY_BOOLEAN, false)
            .putNull(EMPTY_VAR)
            .put(MY_FLOAT, 2.0);
        data.putArray(ARRAY_VAR).addAll((ArrayNode) JsonHandler.toJsonNode(list));
        data.putArray(ARRAY2_VAR).addAll((ArrayNode) JsonHandler.toJsonNode(list));
        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.addData(data);
        LOGGER.debug("InsertString: " + insert.getFinalInsert());
        return insert.getFinalInsert();
    }

    /**
     * @param child child
     * @param parent parent
     * @return
     */
    private JsonNode createInsertChild2ParentRequest(GUID child, GUID parent) throws Exception {
        final ObjectNode data = JsonHandler.createObjectNode()
            .put(id(), child.toString())
            .put(TITLE, VALUE_MY_TITLE + "2")
            .put(DESCRIPTION, "Ma description2 vitam")
            .put(CREATED_DATE, "" + LocalDateUtil.now())
            .put(MY_INT, 10);
        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.addData(data).addRoots(parent.toString());
        LOGGER.debug("InsertString: " + insert.getFinalInsert());
        return insert.getFinalInsert();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode clientRichSelectAllBuild(GUID uuid)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectMultiQuery select = new SelectMultiQuery();
        select
            .addUsedProjection(all())
            .addQueries(
                and()
                    .add(
                        eq(id(), uuid.toString()),
                        match(TITLE, VALUE_MY_TITLE),
                        exists(CREATED_DATE),
                        missing(UNKNOWN_VAR),
                        isNull(EMPTY_VAR),
                        or().add(in(ARRAY_VAR, "val1"), nin(ARRAY_VAR, "val3")),
                        gt(MY_INT, 1),
                        lt(MY_INT, 100),
                        ne(MY_BOOLEAN, true),
                        range(MY_FLOAT, 0.0, false, 100.0, true),
                        term(TITLE, VALUE_MY_TITLE)
                    )
            );
        LOGGER.debug("SelectAllString: " + select.getFinalSelect());
        return select.getFinalSelect();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode clientRichUpdateBuild(GUID uuid) throws InvalidCreateOperationException {
        final UpdateMultiQuery update = new UpdateMultiQuery();
        update
            .addActions(
                set("NewVar", false),
                inc(MY_INT, 2),
                set(DESCRIPTION, "New description"),
                unset(UNKNOWN_VAR),
                push(ARRAY_VAR, "val2"),
                min(MY_FLOAT, 1.5),
                add(ARRAY2_VAR, "val2")
            )
            .addQueries(and().add(eq(id(), uuid.toString()), match(TITLE, VALUE_MY_TITLE)));
        LOGGER.debug("UpdateString: " + update.getFinalUpdate());
        return update.getFinalUpdate();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode clientSelect2Build(GUID uuid)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        final SelectMultiQuery select = new SelectMultiQuery();
        select.addUsedProjection(id(), TITLE, DESCRIPTION).addQueries(eq(id(), uuid.toString()).setDepthLimit(2));
        LOGGER.debug("SelectString: " + select.getFinalSelect());
        return select.getFinalSelect();
    }

    /**
     * @param uuid father
     * @param uuid2 son
     * @return
     */
    private JsonNode clientSelectMultiQueryBuild(GUID uuid, GUID uuid2)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        final SelectMultiQuery select = new SelectMultiQuery();
        select
            .addUsedProjection(id(), TITLE, DESCRIPTION)
            .addQueries(and().add(eq(id(), uuid.toString()), match(TITLE, VALUE_MY_TITLE)), eq(id(), uuid2.toString()));
        LOGGER.debug("SelectString: " + select.getFinalSelect());
        return select.getFinalSelect();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode clientDelete2Build(GUID uuid) throws InvalidCreateOperationException {
        final DeleteMultiQuery delete = new DeleteMultiQuery();
        delete.addQueries(path(uuid.toString()));
        LOGGER.debug("DeleteString: " + delete.getFinalDelete());
        return delete.getFinalDelete();
    }

    @Test
    public void testEmptyCollections() {
        for (final MetadataCollections col : MetadataCollections.values()) {
            if (col.getCollection() != null) {
                col.getCollection().drop();
            }
        }
        assertEquals(0, UNIT.getCollection().countDocuments());
        assertEquals(0, OBJECTGROUP.getCollection().countDocuments());
    }

    private ObjectNode createInsertRequestGO(GUID uuid, GUID uuidParent) throws InvalidParseOperationException {
        // Create Insert command as in Internal Vitam Modules
        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.resetFilter();
        insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        final JsonNode json = JsonHandler.getFromString(
            "{\"#id\":\"" +
            uuid +
            "\", \"#Originating_Agency\": \"FRAN_NP_050056\",  \"FileInfo\": { \"Filename\": \"Filename0\"}, \"#qualifiers\": [{ \"qualifier\": \"BinaryMaster\"}]}"
        );
        insert.addData((ObjectNode) json);
        insert.addRoots(uuidParent.getId());
        final ObjectNode insertRequest = insert.getFinalInsert();
        LOGGER.debug("InsertString: " + insertRequest);
        return insertRequest;
    }

    @Test
    @RunWithCustomExecutor
    public void testInsertGORequest() throws Exception {
        final GUID uuid = GUIDFactory.newUnitGUID(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final DbRequest dbRequest = new DbRequest();
        InsertParserMultiple insertParser;

        insertParser = (InsertParserMultiple) RequestParserHelper.getParser(
            createInsertRequestWithUUID(uuid),
            mongoDbVarNameAdapter
        );
        dbRequest.execInsertUnitRequest(insertParser);
        Result result = checkExistence(dbRequest, uuid, false);
        assertFalse(result.isError());

        final GUID uuid2 = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(createInsertRequestGO(uuid2, uuid));

        dbRequest.execInsertObjectGroupRequests(Collections.singletonList(insertParser));
        result = checkExistence(dbRequest, uuid2, true);
        assertFalse(result.isError());
    }

    @Test
    @RunWithCustomExecutor
    public void testOGElasticsearchIndex() throws Exception {
        final GUID uuid = GUIDFactory.newUnitGUID(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        final DbRequest dbRequest = new DbRequest();
        InsertParserMultiple insertParser;
        insertParser = (InsertParserMultiple) RequestParserHelper.getParser(
            createInsertRequestWithUUID(uuid),
            mongoDbVarNameAdapter
        );
        dbRequest.execInsertUnitRequest(insertParser);
        Result result = checkExistence(dbRequest, uuid, false);
        assertFalse(result.isError());
        final GUID uuid2 = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(createInsertRequestGO(uuid2, uuid));

        dbRequest.execInsertObjectGroupRequests(Collections.singletonList(insertParser));
        result = checkExistence(dbRequest, uuid2, true);
        assertFalse(result.isError());

        final QueryBuilder qb = QueryBuilders.termQuery("_id", uuid2.toString());

        SearchResponse response = esClientWithoutVitamBehavior.search(
            indexManager.getElasticsearchIndexAliasResolver(OBJECTGROUP).resolveIndexName(TENANT_ID_0),
            qb,
            null,
            null,
            null,
            0,
            GlobalDatas.LIMIT_LOAD,
            null,
            null,
            null,
            false
        );

        assertTrue(response != null);
        checkElasticResponseField(response, uuid.getId());
        final JsonNode jsonNode = JsonHandler.getFromString(response.toString());
        final SearchHits hits = response.getHits();
        assertEquals(1, hits.getTotalHits().value);

        LOGGER.debug("Elasticsearch Index for objectGroup ", response);
    }

    /**
     * Check elastic Search Response field
     *
     * @param response ElasticSearch response
     * @param parentUuid the parentUuid
     */
    private void checkElasticResponseField(SearchResponse response, String parentUuid) {
        final Iterator<SearchHit> iterator = response.getHits().iterator();
        while (iterator.hasNext()) {
            final SearchHit searchHit = iterator.next();
            final Map<String, Object> source = searchHit.getSourceAsMap();
            for (final String key : source.keySet()) {
                if ("_qualifiers".equals(key)) {
                    final List<Map<String, Object>> qualifiers = (List<Map<String, Object>>) source.get(key);
                    for (final Map qualifier : qualifiers) {
                        assertEquals("BinaryMaster", qualifier.get("qualifier"));
                    }
                } else if ("_Originating_Agency".equals(key)) {
                    assertEquals("FRAN_NP_050056", source.get(key));
                } else if ("FileInfo".equals(key)) {
                    final Map<String, Object> fileInfo = (Map<String, Object>) source.get(key);
                    assertEquals("Filename0", fileInfo.get("Filename"));
                } else if ("_up".equals(key)) {
                    final List<String> ups = (List<String>) source.get(key);
                    assertEquals(parentUuid, ups.get(0));
                }
            }
        }
    }

    private Result checkExistence(DbRequest dbRequest, GUID uuid, boolean isOG)
        throws InvalidCreateOperationException, InvalidParseOperationException, MetaDataExecutionException, BadRequestException, VitamDBException, ArchiveUnitOntologyValidationException {
        final SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(eq(VitamFieldsHelper.id(), uuid.getId()));
        if (isOG) {
            select.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        }
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(select.getFinalSelect());
        return dbRequest.execRequest(selectParser, Collections.emptyList());
    }

    @Test
    @RunWithCustomExecutor
    public void testUnitParentForlastInsertFilterProjection() throws Exception {
        final DbRequest dbRequest = new DbRequest();
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);

        final GUID uuidGot = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        final GUID uuidUnit = GUIDFactory.newUnitGUID(TENANT_ID_0);

        // Insert OG
        final InsertMultiQuery insert = new InsertMultiQuery();
        final ObjectNode json = (ObjectNode) JsonHandler.getFromString(
            "{\"#id\":\"" +
            uuidGot.getId() +
            "\", \"#qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\", \"Description\" : \"Test\"}}, \"Title\":\"title1\"}"
        );

        insert.addData(json);
        final ObjectNode insertNode = insert.getFinalInsert();

        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertNode);
        dbRequest.execInsertObjectGroupRequests(Collections.singletonList(insertParser));

        // Insert Unit
        final JsonNode insertRequest = createInsertRequestWithUUID(uuidUnit);
        ((ObjectNode) insertRequest.get("$data")).put("_og", uuidGot.getId());
        final InsertParserMultiple insertParser2 = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser2.parse(insertRequest);
        dbRequest.execInsertUnitRequest(insertParser2);

        // Check _og
        Result result = checkExistence(dbRequest, uuidUnit, false);
        assertFalse(result.isError());
        List<MetadataDocument<?>> list = result.getFinal();
        final MetadataDocument unit = list.get(0);
        assertThat(unit.getCollectionOrEmpty(Unit.GRAPH)).hasSize(0);
        assertThat(unit.getCollectionOrEmpty(Unit.UNITUPS)).hasSize(0);
        assertThat(unit.getMapOrEmpty(Unit.UNITDEPTHS)).hasSize(0);
        assertTrue(unit.getString(Unit.OG).equals(uuidGot.getId()));

        // Check _up is set as _og
        result = checkExistence(dbRequest, uuidGot, true);
        assertFalse(result.isError());
        list = result.getFinal();
        final Document og = list.get(0);
        assertTrue(((List<String>) og.get("_up")).contains(uuidUnit.getId()));
    }

    @Test
    @RunWithCustomExecutor
    public void testRequestWithObjectGroupQuery() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        final GUID uuid01 = GUIDFactory.newUnitGUID(TENANT_ID_0);
        final DbRequest dbRequest = new DbRequest();
        InsertParserMultiple insertParser = (InsertParserMultiple) RequestParserHelper.getParser(
            createInsertRequestWithUUID(uuid01),
            mongoDbVarNameAdapter
        );
        dbRequest.execInsertUnitRequest(insertParser);
        Result result = checkExistence(dbRequest, uuid01, false);
        assertFalse(result.isError());

        final GUID uuid1 = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        final InsertMultiQuery insert = new InsertMultiQuery();

        final ObjectNode json = (ObjectNode) JsonHandler.getFromString(
            "{\"#id\":\"" +
            uuid1 +
            "\", \"#qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\", \"Description\" : \"Test\"}}, \"Title\":\"title1\"}"
        );
        insert.addData(json).addRoots(uuid01.getId());
        ObjectNode insertRequestString = insert.getFinalInsert();
        insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequestString);
        dbRequest.execInsertObjectGroupRequests(Collections.singletonList(insertParser));
    }

    @Test
    @RunWithCustomExecutor
    public void testSelectResult() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        final DbRequest dbRequest = new DbRequest();
        final JsonNode selectRequest = JsonHandler.getFromString(REQUEST_SELECT_TEST);
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(selectRequest);
        LOGGER.debug("SelectParser: {}", selectRequest);
        final Result result = dbRequest.execRequest(selectParser, Collections.emptyList());
        assertEquals("[]", result.getFinal().toString());
    }

    @Test
    @RunWithCustomExecutor
    public void shouldSelectUnitResult() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);

        final DbRequest dbRequest = new DbRequest();
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_1);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execInsertUnitRequest(insertParser);
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_0);
        final JsonNode selectRequest = JsonHandler.getFromString(REQUEST_SELECT_TEST);
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(selectRequest);
        LOGGER.debug("SelectParser: {}", selectRequest);
        final Result result2 = dbRequest.execRequest(selectParser, Collections.emptyList());
        assertEquals(1, result2.nbResult);
    }

    @Test(expected = MetaDataExecutionException.class)
    @RunWithCustomExecutor
    public void shouldSelectNoResultSinceOtherTenantUsed() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_2);
        final DbRequest dbRequest = new DbRequest();
        // unit is insterted with TENANT_ID_0 = 0
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_2);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execInsertUnitRequest(insertParser);
        VitamThreadUtils.getVitamSession().setTenantId(3);
        final JsonNode selectRequest = JsonHandler.getFromString(REQUEST_SELECT_TEST);
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(selectRequest);
        LOGGER.debug("SelectParser: {}", selectRequest);
        final Result result2 = dbRequest.execRequest(selectParser, Collections.emptyList());
        assertEquals(0, result2.nbResult);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldSelectUnitResultWithES() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_2);

        // OG ???

        final DbRequest dbRequest = new DbRequest();
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_ES);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execInsertUnitRequest(insertParser);
        final JsonNode selectRequest1 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_1);
        final SelectParserMultiple selectParser1 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser1.parse(selectRequest1);
        LOGGER.debug("SelectParser: {}", selectRequest1);
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_2);
        final Result resultSelect1 = dbRequest.execRequest(selectParser1, Collections.emptyList());
        assertEquals(1, resultSelect1.nbResult);

        final JsonNode selectRequest2 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_2);
        final SelectParserMultiple selectParser2 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser2.parse(selectRequest2);
        LOGGER.debug("SelectParser: {}", selectRequest2);
        final Result resultSelect2 = dbRequest.execRequest(selectParser2, Collections.emptyList());
        assertEquals(1, resultSelect2.nbResult);

        final JsonNode selectRequest3 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_3);
        final SelectParserMultiple selectParser3 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser3.parse(selectRequest3);
        LOGGER.debug("SelectParser: {}", selectRequest3);
        final Result resultSelect3 = dbRequest.execRequest(selectParser3, Collections.emptyList());
        assertEquals(1, resultSelect3.nbResult);

        final JsonNode selectRequest4 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_4);
        final SelectParserMultiple selectParser4 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser4.parse(selectRequest4);
        LOGGER.debug("SelectParser: {}", selectRequest4);
        final Result resultSelect4 = dbRequest.execRequest(selectParser4, Collections.emptyList());
        assertEquals(1, resultSelect4.nbResult);

        final JsonNode selectRequest5 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_5);
        final SelectParserMultiple selectParser5 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser5.parse(selectRequest5);
        LOGGER.debug("SelectParser: {}", selectRequest5);
        final Result resultSelect5 = dbRequest.execRequest(selectParser5, Collections.emptyList());
        assertEquals(1, resultSelect5.nbResult);
        assertEquals(1, resultSelect5.facetResult.size());

        final JsonNode selectRequest6 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_6);
        final SelectParserMultiple selectParser6 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser6.parse(selectRequest6);
        LOGGER.debug("SelectParser: {}", selectRequest6);
        final Result resultSelect6 = dbRequest.execRequest(selectParser6, Collections.emptyList());
        assertEquals(1, resultSelect6.nbResult);
        assertEquals(1, resultSelect6.facetResult.size());
        FacetResult result = (FacetResult) resultSelect6.facetResult.get(0);
        assertEquals("EndDate", result.getName());
        FacetBucket bucket = result.getBuckets().get(0);
        assertEquals(1, bucket.getCount());

        final JsonNode selectRequest7 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_7);
        final SelectParserMultiple selectParser7 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser7.parse(selectRequest7);
        LOGGER.debug("SelectParser: {}", selectRequest7);
        final Result resultSelect7 = dbRequest.execRequest(selectParser7, Collections.emptyList());
        assertEquals(1, resultSelect7.nbResult);
        assertEquals(1, resultSelect7.facetResult.size());
        FacetResult facetResult7 = (FacetResult) resultSelect7.facetResult.get(0);
        assertEquals("filtersFacet", facetResult7.getName());
        FacetBucket bucket7 = facetResult7.getBuckets().get(0);
        assertEquals(1, bucket7.getCount());

        final JsonNode selectRequest8 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_8);
        final SelectParserMultiple selectParser8 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser8.parse(selectRequest8);
        LOGGER.debug("SelectParser: {}", selectRequest8);
        final Result resultSelect8 = dbRequest.execRequest(selectParser8, Collections.emptyList());
        assertEquals(1, resultSelect8.nbResult);
        assertEquals(1, resultSelect8.facetResult.size());

        List<FacetBucket> buckets = ((FacetResult) resultSelect8.facetResult.get(0)).getBuckets();
        assertEquals(1, buckets.size());
        assertEquals("true", buckets.get(0).getValue());
        assertEquals(1, buckets.get(0).getCount());

        InsertMultiQuery insert = new InsertMultiQuery();
        insert.parseData(REQUEST_INSERT_TEST_ES_2).addRoots(UUID2);
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execInsertUnitRequest(insertParser);
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_2);

        SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(match("Description", "description OK").setDepthLimit(1)).addRoots(UUID2);
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel0 = dbRequest.execRequest(selectParser1, Collections.emptyList());
        assertEquals(1, resultSelectRel0.nbResult);
        assertEquals(
            "aeaqaaaaaet33ntwablhaaku6z67pzqaaaar",
            resultSelectRel0.getCurrentIds().iterator().next().toString()
        );

        select = new SelectMultiQuery();
        select.addQueries(match("Description", "description OK").setDepthLimit(1)).addRoots(UUID2);
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel1 = dbRequest.execRequest(selectParser1, Collections.emptyList());
        assertEquals(1, resultSelectRel1.nbResult);
        assertEquals(
            "aeaqaaaaaet33ntwablhaaku6z67pzqaaaar",
            resultSelectRel1.getCurrentIds().iterator().next().toString()
        );

        select = new SelectMultiQuery();
        select.addQueries(match("Description", "description OK").setDepthLimit(3)).addRoots(UUID2);
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result<MetadataDocument<?>> resultSelectRel3 = dbRequest.execRequest(
            selectParser1,
            Collections.emptyList()
        );

        assertEquals(1, resultSelectRel3.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaar", resultSelectRel3.getCurrentIds().iterator().next());

        insert = new InsertMultiQuery();
        insert.parseData(REQUEST_INSERT_TEST_ES_3).addRoots("aeaqaaaaaet33ntwablhaaku6z67pzqaaaar");
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execInsertUnitRequest(insertParser);
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_2);

        final Result<MetadataDocument<?>> resultSelectRel4 = dbRequest.execRequest(
            selectParser1,
            Collections.emptyList()
        );
        assertEquals(2, resultSelectRel4.nbResult);
        for (final String root : resultSelectRel4.getCurrentIds()) {
            assertTrue(
                root.equalsIgnoreCase("aeaqaaaaaet33ntwablhaaku6z67pzqaaaat") ||
                root.equalsIgnoreCase("aeaqaaaaaet33ntwablhaaku6z67pzqaaaar")
            );
        }

        insert = new InsertMultiQuery();
        insert.parseData(REQUEST_INSERT_TEST_ES_4).addRoots(UUID2);
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execInsertUnitRequest(insertParser);
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_2);

        select = new SelectMultiQuery();
        select.addQueries(match("Title", "othervalue").setDepthLimit(1)).addRoots(UUID2);
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result<MetadataDocument<?>> resultSelectRel5 = dbRequest.execRequest(
            selectParser1,
            Collections.emptyList()
        );
        assertEquals(1, resultSelectRel5.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaas", resultSelectRel5.getCurrentIds().iterator().next());

        // Check for "France.pdf"
        select = new SelectMultiQuery();
        select.addRoots(UUID2).addQueries(match("Title", "Frânce").setDepthLimit(1));
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel6 = dbRequest.execRequest(selectParser1, Collections.emptyList());
        assertEquals(1, resultSelectRel6.nbResult);
        assertEquals(
            "aeaqaaaaaet33ntwablhaaku6z67pzqaaaas",
            resultSelectRel6.getCurrentIds().iterator().next().toString()
        );

        // Check for "social vs sociales"
        select = new SelectMultiQuery();
        select.addRoots(UUID2).addQueries(match("Title", "social").setDepthLimit(1));
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel7 = dbRequest.execRequest(selectParser1, Collections.emptyList());
        assertEquals(1, resultSelectRel7.nbResult);
        assertEquals(
            "aeaqaaaaaet33ntwablhaaku6z67pzqaaaas",
            resultSelectRel7.getCurrentIds().iterator().next().toString()
        );

        // Check for "name_with_underscore"
        select = new SelectMultiQuery();
        select.addQueries(match("Title", "abcd").setDepthLimit(1));
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel8 = dbRequest.execRequest(selectParser1, Collections.emptyList());
        assertEquals(1, resultSelectRel8.nbResult);
        assertEquals(
            "aeaqaaaaaet33ntwablhaaku6z67pzqaaaas",
            resultSelectRel8.getCurrentIds().iterator().next().toString()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void shouldSelectUnitResultWithESTenant1() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_1);

        final DbRequest dbRequest = new DbRequest();
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_ES_1_TENANT_1);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execInsertUnitRequest(insertParser);
        final JsonNode selectRequest1 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_1);
        final SelectParserMultiple selectParser1 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser1.parse(selectRequest1);
        LOGGER.debug("SelectParser: {}", selectRequest1);
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_1);
        final Result resultSelect1 = dbRequest.execRequest(selectParser1, Collections.emptyList());
        assertEquals(1, resultSelect1.nbResult);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldSelectUnitResultWithESTenant1ButTenant0() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_1);

        final DbRequest dbRequest = new DbRequest();
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_ES_1_TENANT_1);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execInsertUnitRequest(insertParser);
        final JsonNode selectRequest1 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_1);
        final SelectParserMultiple selectParser1 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser1.parse(selectRequest1);
        LOGGER.debug("SelectParser: {}", selectRequest1);
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_0);
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_1);
        final Result resultSelect1 = dbRequest.execRequest(selectParser1, Collections.emptyList());
        assertEquals(1, resultSelect1.nbResult);
    }

    @Test
    @RunWithCustomExecutor
    public void testUpdateUnitResult() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        // insert title ARchive 3
        final DbRequest dbRequest = new DbRequest();
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        final InsertMultiQuery insert = new InsertMultiQuery();
        String requestInsertTestEsUpdate = IOUtils.toString(
            PropertiesUtils.getResourceAsStream(REQUEST_INSERT_TEST_ES_UPDATE),
            StandardCharsets.UTF_8
        );
        insert.parseData(requestInsertTestEsUpdate);
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execInsertUnitRequest(insertParser);
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_0);

        // check value should exist in the collection
        SelectParserMultiple selectParser2 = new SelectParserMultiple(mongoDbVarNameAdapter);
        SelectMultiQuery select1 = new SelectMultiQuery();
        select1.addQueries(match("Title", "Archive").setDepthLimit(0)).addRoots(UUID1);
        selectParser2.parse(select1.getFinalSelect());
        Result resultSelectRel6 = dbRequest.execRequest(selectParser2, Collections.emptyList());
        assertEquals(1, resultSelectRel6.nbResult);
        String unitId = (String) resultSelectRel6.getCurrentIds().get(0);

        // update title Archive 3 to Archive 2
        final JsonNode updateRequest = JsonHandler.getFromString(REQUEST_UPDATE_INDEX_TEST);
        final UpdateParserMultiple updateParser = new UpdateParserMultiple(mongoDbVarNameAdapter);
        updateParser.parse(updateRequest);
        LOGGER.debug("UpdateParser: {}", updateParser.getRequest());

        OntologyValidator dummyOntologyValidator = mock(OntologyValidator.class);
        doAnswer(args -> args.getArgument(0)).when(dummyOntologyValidator).verifyAndReplaceFields(any());

        dbRequest.execUpdateRequest(
            updateParser,
            unitId,
            UNIT,
            dummyOntologyValidator,
            mock(UnitValidator.class),
            Collections.emptyList(),
            true
        );
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_0);

        // check new value
        SelectParserMultiple selectParser3 = new SelectParserMultiple(mongoDbVarNameAdapter);
        SelectMultiQuery select3 = new SelectMultiQuery();
        select3.addQueries(match("Title", "ArchiveDoubleTest").setDepthLimit(0)).addRoots(UUID1);
        selectParser3.parse(select3.getFinalSelect());
        Result resultSelectRel3 = dbRequest.execRequest(selectParser3, Collections.emptyList());
        assertEquals(1, resultSelectRel3.nbResult);
        assertEquals(UUID1, resultSelectRel3.getCurrentIds().iterator().next().toString());
        assertEquals(
            ((Unit) resultSelectRel3.getListFiltered().get(0)).getInteger(VitamFieldsHelper.version()).intValue(),
            1
        );

        // update computed field should not increment version
        final JsonNode updateRequest5 = JsonHandler.getFromString(REQUEST_UPDATE_COMPUTED_FIELD);
        final UpdateParserMultiple updateParser5 = new UpdateParserMultiple(mongoDbVarNameAdapter);
        updateParser5.parse(updateRequest5);
        LOGGER.debug("UpdateParser: {}", updateParser5.getRequest());

        dbRequest.execUpdateRequest(
            updateParser5,
            unitId,
            UNIT,
            dummyOntologyValidator,
            mock(UnitValidator.class),
            Collections.emptyList(),
            true
        );
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_0);

        // check new value
        SelectParserMultiple selectParser4 = new SelectParserMultiple(mongoDbVarNameAdapter);
        SelectMultiQuery select4 = new SelectMultiQuery();
        select4.addQueries(exists("Title").setDepthLimit(0)).addRoots(UUID1);
        selectParser4.parse(select4.getFinalSelect());
        Result resultSelectRel4 = dbRequest.execRequest(selectParser4, Collections.emptyList());
        assertEquals(1, resultSelectRel4.nbResult);
        assertEquals(UUID1, resultSelectRel4.getCurrentIds().iterator().next().toString());
        // Still 1, not incremented to 2
        assertEquals(
            ((Unit) resultSelectRel4.getListFiltered().get(0)).getInteger(VitamFieldsHelper.version()).intValue(),
            1
        );

        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = mock(CachedArchiveUnitProfileLoader.class);
        doReturn(
            Optional.of(
                new ArchiveUnitProfileModel()
                    .setControlSchema(ADDITIONAL_SCHEMA)
                    .setStatus(ArchiveUnitProfileStatus.ACTIVE)
            )
        )
            .when(archiveUnitProfileLoader)
            .loadArchiveUnitProfile("AdditionalSchema");

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(100, 300);

        OntologyValidator ontologyValidator = new OntologyValidator(Collections::emptyList);
        UnitValidator unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);

        try {
            final JsonNode updateRequest2 = JsonHandler.getFromString(REQUEST_UPDATE_INDEX_TEST_KO_SECONDARY_SCHEMA);
            final UpdateParserMultiple updateParser2 = new UpdateParserMultiple(mongoDbVarNameAdapter);
            updateParser2.parse(updateRequest2);
            LOGGER.debug("UpdateParser: {}", updateParser2.getRequest());
            dbRequest.execUpdateRequest(
                updateParser2,
                unitId,
                UNIT,
                ontologyValidator,
                unitValidator,
                Collections.emptyList(),
                true
            );
            fail("should throw an exception cause of the additional schema");
        } catch (MetadataValidationException e) {
            assertTrue(e.getCause().getMessage().contains("\"missing\":[\"specificField\"]"));
        }

        // add a new field : specificField
        final JsonNode updateRequestSchema = JsonHandler.getFromString(REQUEST_UPDATE_INDEX_TEST_OK_SECONDARY_SCHEMA);
        final UpdateParserMultiple updateParserSchema = new UpdateParserMultiple(mongoDbVarNameAdapter);
        updateParserSchema.parse(updateRequestSchema);
        LOGGER.debug("UpdateParser: {}", updateParserSchema.getRequest());
        dbRequest.execUpdateRequest(
            updateParserSchema,
            unitId,
            UNIT,
            ontologyValidator,
            unitValidator,
            Collections.emptyList(),
            true
        );
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_0);

        // check new value that should exist in the collection
        selectParser2 = new SelectParserMultiple(mongoDbVarNameAdapter);
        select1 = new SelectMultiQuery();
        select1.addQueries(exists("specificField").setDepthLimit(0)).addRoots(UUID1);
        selectParser2.parse(select1.getFinalSelect());
        resultSelectRel6 = dbRequest.execRequest(selectParser2, Collections.emptyList());
        assertEquals(1, resultSelectRel6.nbResult);
        assertEquals(UUID1, resultSelectRel6.getCurrentIds().iterator().next().toString());

        // check old value should not exist in the collection
        selectParser2 = new SelectParserMultiple(mongoDbVarNameAdapter);
        select1 = new SelectMultiQuery();
        select1.addQueries(match("Title", "ArchiveTest").setDepthLimit(0)).addRoots(UUID1);
        selectParser2.parse(select1.getFinalSelect());
        resultSelectRel6 = dbRequest.execRequest(selectParser2, Collections.emptyList());
        assertEquals(0, resultSelectRel6.nbResult);

        // check new value should exist in the collection
        final JsonNode selectRequest1 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_UPDATE);
        final SelectParserMultiple selectParser1 = new SelectParserMultiple(mongoDbVarNameAdapter);
        final SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(match("Title", "ArchiveDoubleTest").setDepthLimit(0)).addRoots(UUID1);
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectRequest1);
        final Result resultSelectRel5 = dbRequest.execRequest(selectParser1, Collections.emptyList());
        assertEquals(1, resultSelectRel5.nbResult);
        assertEquals(UUID1, resultSelectRel5.getCurrentIds().iterator().next().toString());
    }

    @Test
    @RunWithCustomExecutor
    public void testUpdateUnitSameUpdateTwiceForceUpdateFalse_onlyUpdateFirstTime() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        // insert title ARchive 3
        final DbRequest dbRequest = new DbRequest();
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        final InsertMultiQuery insert = new InsertMultiQuery();
        String requestInsertTestEsUpdate = IOUtils.toString(
            PropertiesUtils.getResourceAsStream(REQUEST_INSERT_TEST_ES_UPDATE),
            StandardCharsets.UTF_8
        );
        insert.parseData(requestInsertTestEsUpdate);
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execInsertUnitRequest(insertParser);
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_0);

        // check value should exist in the collection
        SelectParserMultiple selectParser2 = new SelectParserMultiple(mongoDbVarNameAdapter);
        SelectMultiQuery select1 = new SelectMultiQuery();
        select1.addQueries(match("Title", "Archive").setDepthLimit(0)).addRoots(UUID1);
        selectParser2.parse(select1.getFinalSelect());
        Result resultSelectRel6 = dbRequest.execRequest(selectParser2, Collections.emptyList());
        assertEquals(1, resultSelectRel6.nbResult);
        String unitId = (String) resultSelectRel6.getCurrentIds().get(0);

        // update title Archive 3 to Archive 2
        final JsonNode updateRequest = JsonHandler.getFromString(REQUEST_UPDATE_INDEX_TEST);
        final UpdateParserMultiple updateParser = new UpdateParserMultiple(mongoDbVarNameAdapter);
        updateParser.parse(updateRequest);
        LOGGER.debug("UpdateParser: {}", updateParser.getRequest());

        OntologyValidator dummyOntologyValidator = mock(OntologyValidator.class);
        doAnswer(args -> args.getArgument(0)).when(dummyOntologyValidator).verifyAndReplaceFields(any());

        UpdatedDocument updatedDocument1 = dbRequest.execUpdateRequest(
            updateParser,
            unitId,
            UNIT,
            dummyOntologyValidator,
            mock(UnitValidator.class),
            Collections.emptyList(),
            false
        );
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_0);
        assertNotEquals(updatedDocument1.getBeforeUpdate(), updatedDocument1.getAfterUpdate());

        // select query
        SelectParserMultiple selectParserUnit = new SelectParserMultiple(mongoDbVarNameAdapter);
        SelectMultiQuery selectUnit = new SelectMultiQuery();
        selectUnit.addQueries(eq("#id", UUID1));
        selectParserUnit.parse(selectUnit.getFinalSelect());

        // select the unit and check the modification
        Result resultSelectUnit = dbRequest.execRequest(selectParserUnit, Collections.emptyList());
        assertEquals(1, resultSelectUnit.nbResult);
        assertEquals(UUID1, resultSelectUnit.getCurrentIds().iterator().next().toString());
        assertEquals(
            ((Unit) resultSelectUnit.getListFiltered().get(0)).getInteger(VitamFieldsHelper.version()).intValue(),
            1
        );

        // update title Archive 3 to Archive 2
        doAnswer(args -> args.getArgument(0)).when(dummyOntologyValidator).verifyAndReplaceFields(any());

        UpdatedDocument updatedDocument2 = dbRequest.execUpdateRequest(
            updateParser,
            unitId,
            UNIT,
            dummyOntologyValidator,
            mock(UnitValidator.class),
            Collections.emptyList(),
            false
        );
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_0);
        assertEquals(updatedDocument2.getBeforeUpdate(), updatedDocument2.getAfterUpdate());
        assertFalse(updatedDocument2.isUpdated());

        // select the unit and check the non modification
        resultSelectUnit = dbRequest.execRequest(selectParserUnit, Collections.emptyList());
        assertEquals(1, resultSelectUnit.nbResult);
        assertEquals(UUID1, resultSelectUnit.getCurrentIds().iterator().next().toString());
        assertEquals(
            ((Unit) resultSelectUnit.getListFiltered().get(0)).getInteger(VitamFieldsHelper.version()).intValue(),
            1
        );
    }

    @Test
    @RunWithCustomExecutor
    public void testUpdateKOSchemaUnitResultThrowsException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        // insert title ARchive 3
        final DbRequest dbRequest = new DbRequest();
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.parseData(REQUEST_INSERT_TEST_ES_UPDATE_KO);
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execInsertUnitRequest(insertParser);
        UNIT.getEsClient().refreshIndex(UNIT, TENANT_ID_0);

        final JsonNode updateRequest = JsonHandler.getFromString(REQUEST_UPDATE_INDEX_TEST_KO);
        final UpdateParserMultiple updateParser = new UpdateParserMultiple();
        updateParser.parse(updateRequest);
        LOGGER.debug("UpdateParser: {}", updateParser.getRequest());

        OntologyValidator ontologyValidator = mock(OntologyValidator.class);
        doAnswer(args -> args.getArgument(0)).when(ontologyValidator).verifyAndReplaceFields(any());

        dbRequest.execUpdateRequest(
            updateParser,
            "aeaqaaaaaagbcaacabg44ak45e54criaaaaq",
            UNIT,
            ontologyValidator,
            mock(UnitValidator.class),
            Collections.emptyList(),
            true
        );
    }

    @Test
    @RunWithCustomExecutor
    public void testInsertUnitWithTenant() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        final DbRequest dbRequest = new DbRequest();
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_1);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execInsertUnitRequest(insertParser);

        final JsonNode selectRequest = JsonHandler.getFromString(REQUEST_SELECT_TEST);
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(selectRequest);
        LOGGER.debug("SelectParser: {}", selectRequest);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_1);
        final Result result3 = dbRequest.execRequest(selectParser, Collections.emptyList());
        assertEquals(0, result3.nbResult);
    }

    private JsonNode createInsertRequestGOTenant(GUID uuid) throws InvalidParseOperationException {
        // INSERT
        final List<String> list = Arrays.asList("val1", "val2");
        final ObjectNode data = JsonHandler.createObjectNode()
            .put(id(), uuid.toString())
            .put(TITLE, VALUE_MY_TITLE)
            .put(DESCRIPTION, "Ma description est bien détaillée")
            .put(CREATED_DATE, "" + LocalDateUtil.now())
            .put(MY_INT, 20)
            .put(tenant(), TENANT_ID_0)
            .put(MY_BOOLEAN, false)
            .putNull(EMPTY_VAR)
            .put(MY_FLOAT, 2.0);
        data.putArray(ARRAY_VAR).addAll((ArrayNode) JsonHandler.toJsonNode(list));
        data.putArray("_up").addAll((ArrayNode) JsonHandler.toJsonNode(list));
        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        insert.addData(data);
        LOGGER.debug("InsertString: " + insert.getFinalInsert());
        return insert.getFinalInsert();
    }

    @Test
    @RunWithCustomExecutor
    public void testInsertGOWithTenant() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);

        final GUID uuid = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        final DbRequest dbRequest = new DbRequest();
        InsertParserMultiple insertParser = (InsertParserMultiple) RequestParserHelper.getParser(
            createInsertRequestGOTenant(uuid),
            mongoDbVarNameAdapter
        );
        dbRequest.execInsertObjectGroupRequests(Collections.singletonList(insertParser));

        final SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(eq(VitamFieldsHelper.id(), uuid.getId()));
        select.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(select.getFinalSelect());
        final Result result = dbRequest.execRequest(selectParser, Collections.emptyList());
        assertEquals(1, result.nbResult);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_1);
        final Result result2 = dbRequest.execRequest(selectParser, Collections.emptyList());
        assertEquals(0, result2.nbResult);
    }

    @Test
    @RunWithCustomExecutor
    public void testOrAndMatch() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);

        final GUID uuid = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        final DbRequest dbRequest = new DbRequest();
        InsertParserMultiple requestParser;
        // INSERT 1
        ObjectNode data = JsonHandler.createObjectNode()
            .put(id(), uuid.toString())
            .put(TITLE, "Rectorat 1")
            .put(DESCRIPTION, "Ma description public est bien détaillée")
            .put("DescriptionLevel", "Item")
            .put(CREATED_DATE, "" + LocalDateUtil.now())
            .put(tenant(), TENANT_ID_0);
        InsertMultiQuery insert = new InsertMultiQuery();
        insert.addHintFilter(BuilderToken.FILTERARGS.UNITS.exactToken());
        insert.addData(data);
        LOGGER.debug("InsertString: " + insert.getFinalInsert());
        ObjectNode insertNode = insert.getFinalInsert();
        requestParser = (InsertParserMultiple) RequestParserHelper.getParser(insertNode, mongoDbVarNameAdapter);
        dbRequest.execInsertUnitRequest(requestParser);
        // Insert 2
        final GUID uuid2 = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        ObjectNode data2 = JsonHandler.createObjectNode()
            .put(id(), uuid2.toString())
            .put(TITLE, "Rectorat 2")
            .put(DESCRIPTION, "Ma description privé est bien détaillée")
            .put("DescriptionLevel", "Item")
            .put(CREATED_DATE, "" + LocalDateUtil.now())
            .put(tenant(), TENANT_ID_0);
        insert.reset();
        insert.addHintFilter(BuilderToken.FILTERARGS.UNITS.exactToken());
        insert.addData(data2);
        LOGGER.debug("InsertString: " + insert.getFinalInsert());
        insertNode = insert.getFinalInsert();
        requestParser = (InsertParserMultiple) RequestParserHelper.getParser(insertNode, mongoDbVarNameAdapter);
        dbRequest.execInsertUnitRequest(requestParser);
        // Insert 3 false description
        final GUID uuid3 = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        ObjectNode data3 = JsonHandler.createObjectNode()
            .put(id(), uuid3.toString())
            .put(TITLE, "Rectorat 3")
            .put(DESCRIPTION, "Ma description est bien détaillée")
            .put("DescriptionLevel", "Item")
            .put(CREATED_DATE, "" + LocalDateUtil.now())
            .put(tenant(), TENANT_ID_0);
        insert.reset();
        insert.addHintFilter(BuilderToken.FILTERARGS.UNITS.exactToken());
        insert.addData(data3);
        LOGGER.debug("InsertString: " + insert.getFinalInsert());
        insertNode = insert.getFinalInsert();
        requestParser = (InsertParserMultiple) RequestParserHelper.getParser(insertNode, mongoDbVarNameAdapter);
        dbRequest.execInsertUnitRequest(requestParser);
        // Insert 4 false Title
        final GUID uuid4 = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        ObjectNode data4 = JsonHandler.createObjectNode()
            .put(id(), uuid4.toString())
            .put(TITLE, "Title 4")
            .put(DESCRIPTION, "Ma description public est bien détaillée")
            .put("DescriptionLevel", "Item")
            .put(CREATED_DATE, "" + LocalDateUtil.now())
            .put(tenant(), TENANT_ID_0);
        insert.reset();
        insert.addHintFilter(BuilderToken.FILTERARGS.UNITS.exactToken());
        insert.addData(data4);
        LOGGER.debug("InsertString: " + insert.getFinalInsert());
        insertNode = insert.getFinalInsert();
        requestParser = (InsertParserMultiple) RequestParserHelper.getParser(insertNode, mongoDbVarNameAdapter);
        dbRequest.execInsertUnitRequest(requestParser);

        String query =
            "{\"$roots\": [],\"$query\": [{\"$or\": " +
            "[{\"$and\": [{\"$match\": {\"Title\": \"Rectorat\"}},{\"$match\": {\"Description\": \"public\"}}]}," +
            "{\"$and\": [{\"$match\": {\"Title\": \"Rectorat\"}},{\"$match\": {\"Description\": \"privé\"}}]}]," +
            "\"$depth\": 20}],\"$filter\": {\"$hint\": [\"units\"], \"$orderby\": {\"TransactedDate\": 1}}," +
            "\"$projection\": {\"$fields\": {\"TransactedDate\": 1,\"#id\": 1,\"Title\": 1,\"#object\": 1,\"Description\": 1}}}";
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(JsonHandler.getFromString(query));
        final Result result = dbRequest.execRequest(selectParser, Collections.emptyList());

        // Clean
        final DeleteMultiQuery delete = new DeleteMultiQuery();
        delete.addQueries(
            in(VitamFieldsHelper.id(), uuid.toString(), uuid2.toString(), uuid3.toString(), uuid4.toString())
        );
        delete.setMult(true);
        final DeleteParserMultiple deleteParser = new DeleteParserMultiple(mongoDbVarNameAdapter);
        deleteParser.parse(delete.getFinalDelete());
        dbRequest.execRequest(deleteParser, Collections.emptyList());
        assertEquals(2, result.nbResult);
    }

    /**
     * Create simple insert query tree with one parent.
     *
     * @param guid
     * @param title
     * @return the created insert query
     */
    private JsonNode createInsertRequestTreeWithParents(final String guid, final String title, final String op)
        throws InvalidParseOperationException {
        final ObjectNode data = JsonHandler.createObjectNode()
            .put(id(), guid)
            .put(TITLE, title)
            .put(tenant(), TENANT_ID_0)
            .put(DESCRIPTION, "Fake description");
        data.putArray(_OPS).addAll((ArrayNode) JsonHandler.toJsonNode(Arrays.asList(op)));
        final InsertMultiQuery insertQuery = new InsertMultiQuery();
        insertQuery.addData(data);

        return insertQuery.getFinalInsert();
    }

    /**
     * Test method for execRequest
     * .
     *
     * @throws InvalidParseOperationException
     * @throws MetaDataNotFoundException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataExecutionException
     */
    @Test(expected = BadRequestException.class)
    @RunWithCustomExecutor
    public void testExecRequestThroughAllCommandsForNested() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        // input data
        final GUID uuid = GUIDFactory.newUnitGUID(TENANT_ID_0);
        try {
            final DbRequest dbRequest = new DbRequest();
            RequestParserMultiple requestParser;

            // SELECT
            JsonNode selectRequest = JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream("select_request_with_nested.json")
            );
            // Now considering select request and parsing it as in Data Server (GET command)
            requestParser = RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // UPDATE
            final JsonNode updateRequest = JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream("update_request_with_nested.json")
            );
            // Now considering update request and parsing it as in Data Server (PATCH command)
            requestParser = RequestParserHelper.getParser(updateRequest, mongoDbVarNameAdapter);
            LOGGER.debug("UpdateParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // DELETE
            final JsonNode deleteRequest = JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream("delete_request_with_nested.json")
            );
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            requestParser = RequestParserHelper.getParser(deleteRequest, mongoDbVarNameAdapter);
            LOGGER.debug("DeleteParser: " + requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);
        } finally {
            // clean
            UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid.toString()));
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testUpdateRulesWithArchiveUnitProfileAndOntologyValidationOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setRequestId("aeeaaaaaacagqkjjaaxpwallds4xu6iaaaaq");

        String uuid = "aeaqaaaabeghay2jabzuaalbarkww4iaaaba";

        final Unit initialUnit = new Unit(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("unitRulesToUpdate.json"))
        );

        UNIT.getCollection().insertOne(initialUnit);
        UNIT.getEsClient().insertFullDocument(UNIT, 0, uuid, initialUnit);

        // Base ontology with custom external types
        List<OntologyModel> ontologyModels = JsonHandler.getFromInputStreamAsTypeReference(
            OntologyTestHelper.loadOntologies(),
            new TypeReference<List<OntologyModel>>() {}
        );
        ontologyModels.addAll(
            Arrays.asList(
                new OntologyModel()
                    .setType(OntologyType.BOOLEAN)
                    .setOrigin(OntologyOrigin.EXTERNAL)
                    .setIdentifier("Flag"),
                new OntologyModel()
                    .setType(OntologyType.LONG)
                    .setOrigin(OntologyOrigin.EXTERNAL)
                    .setIdentifier("Number")
            )
        );

        // AUP Schema
        AdminManagementClientFactory adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
        doReturn(
            new RequestResponseOK<ArchiveUnitProfileModel>().addResult(
                new ArchiveUnitProfileModel()
                    .setControlSchema(PropertiesUtils.getResourceAsString("unitRulesAUP_OK.json"))
                    .setStatus(ArchiveUnitProfileStatus.ACTIVE)
            )
        )
            .when(adminManagementClient)
            .findArchiveUnitProfilesByID("AUP_IDENTIFIER");
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = new CachedArchiveUnitProfileLoader(
            adminManagementClientFactory,
            100,
            300
        );

        // Request
        RuleActions ruleActions = new RuleActions();
        ruleActions
            .getAdd()
            .add(
                ImmutableMap.of(
                    "AccessRule",
                    new RuleCategoryAction()
                        .setPreventInheritance(true)
                        .setRules(
                            Collections.singletonList(new RuleAction().setRule("ACC-00001").setStartDate("2000-01-01"))
                        )
                )
            );

        ruleActions
            .getUpdate()
            .add(
                ImmutableMap.of(
                    "AccessRule",
                    new RuleCategoryAction()
                        .setRules(
                            Collections.singletonList(
                                new RuleAction().setOldRule("ACC-00002").setRule("ACC-00003").setStartDate("2000-01-01")
                            )
                        )
                )
            );

        RuleCategoryActionDeletion accessRule = new RuleCategoryActionDeletion();
        accessRule.setRules(Collections.singletonList(new RuleAction().setRule("ACC-00004")));
        ruleActions.getDelete().add(ImmutableMap.of("AccessRule", accessRule));

        Map<String, DurationData> ruleDurationByRuleId = ImmutableMap.of(
            "ACC-00001",
            new DurationData(1, ChronoUnit.DAYS),
            "ACC-00002",
            new DurationData(1, ChronoUnit.MONTHS),
            "ACC-00003",
            new DurationData(1, ChronoUnit.YEARS),
            "ACC-00004",
            new DurationData(1, ChronoUnit.CENTURIES)
        );

        // Grrrrrrrr. Since when archive unit profile is a "Rule metadata" !
        ruleActions.setAddOrUpdateMetadata(new ManagementMetadataAction().setArchiveUnitProfile("AUP_IDENTIFIER"));

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(100, 300);

        OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        UnitValidator unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);

        // When
        final DbRequest dbRequest = new DbRequest(
            new MongoDbMetadataRepository<Unit>(() -> UNIT.getCollection()),
            new MongoDbMetadataRepository<ObjectGroup>(() -> OBJECTGROUP.getCollection()),
            fieldHistoryManager
        );
        UpdatedDocument updatedDocument = dbRequest.execRuleRequest(
            uuid,
            ruleActions,
            ruleDurationByRuleId,
            ontologyValidator,
            unitValidator,
            Collections.emptyList()
        );

        // Then
        final Unit expectedUnit = new Unit(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("unitRulesAfterUpdate.json"))
        );

        String expected = BsonHelper.stringify(expectedUnit);

        var unitDocumentAfterUpdate = UNIT.getCollection().find(Filters.eq("_id", uuid)).first();
        unitDocumentAfterUpdate.remove(Unit.APPROXIMATE_UPDATE_DATE);
        String after = JsonHandler.unprettyPrint(unitDocumentAfterUpdate);
        JsonAssert.assertJsonEquals(expected, after);
        JsonAssert.assertJsonEquals(
            BsonHelper.stringify(initialUnit),
            JsonHandler.unprettyPrint(updatedDocument.getBeforeUpdate())
        );
        ((ObjectNode) updatedDocument.getAfterUpdate()).remove(Unit.APPROXIMATE_UPDATE_DATE);
        JsonAssert.assertJsonEquals(expected, JsonHandler.unprettyPrint(updatedDocument.getAfterUpdate()));
        assertThat(updatedDocument.getDocumentId()).isEqualTo(uuid);
    }

    @RunWithCustomExecutor
    @Test
    public void testUpdateRulesWithOntologyValidationFailure() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setRequestId("aeeaaaaaacagqkjjaaxpwallds4xu6iaaaaq");

        String uuid = "aeaqaaaabeghay2jabzuaalbarkww4iaaaba";

        // Corrupted unit
        final Unit initialUnit = new Unit(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("unitRulesToUpdate.json"))
        );
        initialUnit.put("StartDate", 1234);

        UNIT.getCollection().insertOne(initialUnit);
        UNIT.getEsClient().insertFullDocument(UNIT, 0, uuid, initialUnit);

        // Base ontology
        List<OntologyModel> ontologyModels = JsonHandler.getFromInputStreamAsTypeReference(
            OntologyTestHelper.loadOntologies(),
            new TypeReference<List<OntologyModel>>() {}
        );

        // AUP Schema
        AdminManagementClientFactory adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
        doReturn(
            new RequestResponseOK<ArchiveUnitProfileModel>().addResult(
                new ArchiveUnitProfileModel()
                    .setControlSchema(PropertiesUtils.getResourceAsString("unitRulesAUP_KO.json"))
                    .setStatus(ArchiveUnitProfileStatus.ACTIVE)
            )
        )
            .when(adminManagementClient)
            .findArchiveUnitProfilesByID("AUP_IDENTIFIER");
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = new CachedArchiveUnitProfileLoader(
            adminManagementClientFactory,
            100,
            300
        );

        // Request
        RuleActions ruleActions = new RuleActions();
        ruleActions
            .getAdd()
            .add(
                ImmutableMap.of(
                    "AccessRule",
                    new RuleCategoryAction()
                        .setPreventInheritance(true)
                        .setRules(
                            Collections.singletonList(new RuleAction().setRule("ACC-00001").setStartDate("2000-01-01"))
                        )
                )
            );

        Map<String, DurationData> ruleDurationByRuleId = ImmutableMap.of(
            "ACC-00001",
            new DurationData(1, ChronoUnit.DAYS),
            "ACC-00002",
            new DurationData(1, ChronoUnit.MONTHS),
            "ACC-00003",
            new DurationData(1, ChronoUnit.YEARS),
            "ACC-00004",
            new DurationData(1, ChronoUnit.CENTURIES)
        );

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(100, 300);

        OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        UnitValidator unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);

        // When
        final DbRequest dbRequest = new DbRequest(
            new MongoDbMetadataRepository<Unit>(() -> UNIT.getCollection()),
            new MongoDbMetadataRepository<ObjectGroup>(() -> OBJECTGROUP.getCollection()),
            fieldHistoryManager
        );
        assertThatThrownBy(
            () ->
                dbRequest.execRuleRequest(
                    uuid,
                    ruleActions,
                    ruleDurationByRuleId,
                    ontologyValidator,
                    unitValidator,
                    Collections.emptyList()
                )
        ).isInstanceOf(MetadataValidationException.class);

        // Then
        String expected = BsonHelper.stringify(initialUnit);

        String after = JsonHandler.unprettyPrint(UNIT.getCollection().find(Filters.eq("_id", uuid)).first());
        JsonAssert.assertJsonEquals(expected, after);
    }

    @RunWithCustomExecutor
    @Test
    public void testUpdateRulesWithArchiveUnitProfileValidationFailure() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setRequestId("aeeaaaaaacagqkjjaaxpwallds4xu6iaaaaq");

        String uuid = "aeaqaaaabeghay2jabzuaalbarkww4iaaaba";

        final Unit initialUnit = new Unit(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("unitRulesToUpdate.json"))
        );

        UNIT.getCollection().insertOne(initialUnit);
        UNIT.getEsClient().insertFullDocument(UNIT, 0, uuid, initialUnit);

        // Base ontology
        List<OntologyModel> ontologyModels = JsonHandler.getFromInputStreamAsTypeReference(
            OntologyTestHelper.loadOntologies(),
            new TypeReference<List<OntologyModel>>() {}
        );

        // AUP Schema
        AdminManagementClientFactory adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
        doReturn(
            new RequestResponseOK<ArchiveUnitProfileModel>().addResult(
                new ArchiveUnitProfileModel()
                    .setControlSchema(PropertiesUtils.getResourceAsString("unitRulesAUP_KO.json"))
                    .setStatus(ArchiveUnitProfileStatus.ACTIVE)
            )
        )
            .when(adminManagementClient)
            .findArchiveUnitProfilesByID("AUP_IDENTIFIER");
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = new CachedArchiveUnitProfileLoader(
            adminManagementClientFactory,
            100,
            300
        );

        // Request
        RuleActions ruleActions = new RuleActions();
        ruleActions.getAdd().add(ImmutableMap.of("AccessRule", new RuleCategoryAction().setPreventInheritance(true)));

        // Grrrrrrrr. Since when archive unit profile is a "Rule metadata" !
        ruleActions.setAddOrUpdateMetadata(new ManagementMetadataAction().setArchiveUnitProfile("AUP_IDENTIFIER"));

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(100, 300);

        OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        UnitValidator unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);

        // When
        final DbRequest dbRequest = new DbRequest(
            new MongoDbMetadataRepository<Unit>(() -> UNIT.getCollection()),
            new MongoDbMetadataRepository<ObjectGroup>(() -> OBJECTGROUP.getCollection()),
            fieldHistoryManager
        );
        assertThatThrownBy(
            () ->
                dbRequest.execRuleRequest(
                    uuid,
                    ruleActions,
                    emptyMap(),
                    ontologyValidator,
                    unitValidator,
                    Collections.emptyList()
                )
        ).isInstanceOf(MetadataValidationException.class);

        // Then
        String expected = BsonHelper.stringify(initialUnit);

        String after = JsonHandler.unprettyPrint(UNIT.getCollection().find(Filters.eq("_id", uuid)).first());
        JsonAssert.assertJsonEquals(expected, after);
    }

    @RunWithCustomExecutor
    @Test
    public void testUpdateRulesWithInternalSchemaValidationFailure() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setRequestId("aeeaaaaaacagqkjjaaxpwallds4xu6iaaaaq");

        String uuid = "aeaqaaaabeghay2jabzuaalbarkww4iaaaba";

        final Unit initialUnit = new Unit(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("unitRulesToUpdate.json"))
        );
        // Corrupt initial unit
        initialUnit.remove("Title");

        UNIT.getCollection().insertOne(initialUnit);
        UNIT.getEsClient().insertFullDocument(UNIT, 0, uuid, initialUnit);

        // Base ontology
        List<OntologyModel> ontologyModels = JsonHandler.getFromInputStreamAsTypeReference(
            OntologyTestHelper.loadOntologies(),
            new TypeReference<List<OntologyModel>>() {}
        );

        // No external AUP Schema
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = mock(CachedArchiveUnitProfileLoader.class);

        // Request
        RuleActions ruleActions = new RuleActions();
        ruleActions.getAdd().add(ImmutableMap.of("AccessRule", new RuleCategoryAction().setPreventInheritance(true)));

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(100, 300);

        OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        UnitValidator unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);

        // When
        final DbRequest dbRequest = new DbRequest();
        assertThatThrownBy(
            () ->
                dbRequest.execRuleRequest(
                    uuid,
                    ruleActions,
                    emptyMap(),
                    ontologyValidator,
                    unitValidator,
                    Collections.emptyList()
                )
        ).isInstanceOf(MetadataValidationException.class);

        // Then
        String expected = BsonHelper.stringify(initialUnit);

        String after = JsonHandler.unprettyPrint(UNIT.getCollection().find(Filters.eq("_id", uuid)).first());
        JsonAssert.assertJsonEquals(expected, after);
    }

    @Test
    @RunWithCustomExecutor
    public void should_exec_rule_on_ClassificationLevel_create_history() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setRequestId("aeeaaaaaacagqkjjaaxpwallds4xu6iaaaaq");

        String uuid = "aeaqaaaabeghay2jabzuaalbarkww4iaaaba";

        final Unit initialUnit = new Unit(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("unitRulesToUpdate.json"))
        );

        UNIT.getCollection().insertOne(initialUnit);
        UNIT.getEsClient().insertFullDocument(UNIT, 0, uuid, initialUnit);

        AdminManagementClientFactory adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
        doReturn(
            new RequestResponseOK<ArchiveUnitProfileModel>().addResult(
                new ArchiveUnitProfileModel()
                    .setControlSchema(PropertiesUtils.getResourceAsString("unitRulesAUP_OK.json"))
                    .setStatus(ArchiveUnitProfileStatus.ACTIVE)
            )
        )
            .when(adminManagementClient)
            .findArchiveUnitProfilesByID("AUP_IDENTIFIER");

        RuleActions ruleActions = new RuleActions();
        RuleCategoryAction classificationRule = new RuleCategoryAction()
            .setRules(Collections.singletonList(new RuleAction().setRule("ACC-00001")))
            .setClassificationLevel("Batman defense");
        ruleActions.getDelete().add(ImmutableMap.of("ClassificationLevel", new RuleCategoryActionDeletion()));

        Map<String, DurationData> ruleDurationByRuleId = ImmutableMap.of(
            "ACC-00001",
            new DurationData(1, ChronoUnit.DAYS),
            "ACC-00002",
            new DurationData(1, ChronoUnit.MONTHS),
            "ACC-00003",
            new DurationData(1, ChronoUnit.YEARS),
            "ACC-00004",
            new DurationData(1, ChronoUnit.CENTURIES)
        );

        ruleActions.setAddOrUpdateMetadata(new ManagementMetadataAction().setArchiveUnitProfile("AUP_IDENTIFIER"));

        DbRequest dbRequest = new DbRequest(
            new MongoDbMetadataRepository<Unit>(() -> UNIT.getCollection()),
            new MongoDbMetadataRepository<ObjectGroup>(() -> OBJECTGROUP.getCollection()),
            fieldHistoryManager
        );

        JsonNode history = new History("BatmanHistory", 1L, JsonHandler.createObjectNode()).getArrayNode();

        doAnswer(i -> {
            ObjectNode updatedJsonDocument = i.getArgument(1);
            updatedJsonDocument.set("_history", history);
            return null;
        })
            .when(fieldHistoryManager)
            .trigger(any(), any());

        OntologyValidator ontologyValidator = mock(OntologyValidator.class);
        doAnswer(args -> args.getArgument(0)).when(ontologyValidator).verifyAndReplaceFields(any());

        // When
        UpdatedDocument updatedDocument = dbRequest.execRuleRequest(
            uuid,
            ruleActions,
            ruleDurationByRuleId,
            ontologyValidator,
            mock(UnitValidator.class),
            Collections.emptyList()
        );

        // Then
        assertThat(
            updatedDocument.getAfterUpdate().get("_history").get(0).get("data").get("BatmanHistory")
        ).isNotNull();
        verify(fieldHistoryManager).trigger(any(JsonNode.class), any(JsonNode.class));
    }

    @RunWithCustomExecutor
    @Test
    public void testUnlockAllInheritancePreventRulesIdOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setRequestId("aeeaaaaaacagqkjjaaxpwallds4xu6iaaaaq");

        String uuid = "aeaqaaaabeghay2jabzuaalbarkww4iaaaba";

        final Unit initialUnit = new Unit(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("unitInheritanceToUpdate_1.json"))
        );

        UNIT.getCollection().insertOne(initialUnit);
        UNIT.getEsClient().insertFullDocument(UNIT, 0, uuid, initialUnit);

        // Base ontology
        List<OntologyModel> ontologyModels = JsonHandler.getFromInputStreamAsTypeReference(
            OntologyTestHelper.loadOntologies(),
            new TypeReference<List<OntologyModel>>() {}
        );

        // No external AUP Schema
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = mock(CachedArchiveUnitProfileLoader.class);

        Map<String, DurationData> ruleDurationByRuleId = ImmutableMap.of(
            "DIS-00001",
            new DurationData(1, ChronoUnit.DAYS),
            "DIS-00002",
            new DurationData(1, ChronoUnit.MONTHS)
        );
        // Request
        RuleActions ruleActions = new RuleActions();
        HashMap<String, RuleCategoryActionDeletion> e = new HashMap<>();
        e.put("DisseminationRule", null);
        ruleActions.getDelete().add(e);

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(100, 300);

        OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        UnitValidator unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);

        DbRequest dbRequest = new DbRequest(
            new MongoDbMetadataRepository<Unit>(() -> UNIT.getCollection()),
            new MongoDbMetadataRepository<ObjectGroup>(() -> OBJECTGROUP.getCollection()),
            fieldHistoryManager
        );

        // When
        UpdatedDocument updatedDocument = dbRequest.execRuleRequest(
            uuid,
            ruleActions,
            ruleDurationByRuleId,
            ontologyValidator,
            unitValidator,
            Collections.emptyList()
        );

        // Then
        assertThat(updatedDocument).isNotNull();
        assertThat(updatedDocument.getAfterUpdate().get("_mgt").isEmpty(null)).isTrue();
    }

    @RunWithCustomExecutor
    @Test
    public void shouldNotLockRuleWhenPrventInheritanceIsTrueAndPreventRulesIdIsEmpty() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setRequestId("aeeaaaaaacagqkjjaaxpwallds4xu6iaaaaq");

        String uuid = "aeaqaaaabeghay2jabzuaalbarkww4iaaaba";

        final Unit initialUnit = new Unit(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("unitInheritanceToUpdate_2.json"))
        );

        UNIT.getCollection().insertOne(initialUnit);
        UNIT.getEsClient().insertFullDocument(UNIT, 0, uuid, initialUnit);

        // Base ontology
        List<OntologyModel> ontologyModels = JsonHandler.getFromInputStreamAsTypeReference(
            OntologyTestHelper.loadOntologies(),
            new TypeReference<List<OntologyModel>>() {}
        );

        // No external AUP Schema
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = mock(CachedArchiveUnitProfileLoader.class);

        Map<String, DurationData> ruleDurationByRuleId = ImmutableMap.of(
            "DIS-00001",
            new DurationData(1, ChronoUnit.DAYS),
            "DIS-00002",
            new DurationData(1, ChronoUnit.MONTHS)
        );
        // Request
        RuleActions ruleActions = new RuleActions();
        Set<String> preventRulesIdToBlock = new HashSet<>();
        preventRulesIdToBlock.add("DIS-00001");
        ruleActions
            .getUpdate()
            .add(
                ImmutableMap.of("DisseminationRule", new RuleCategoryAction().setPreventRulesId(preventRulesIdToBlock))
            );

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(100, 300);

        OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        UnitValidator unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);

        // When
        DbRequest dbRequest = new DbRequest(
            new MongoDbMetadataRepository<Unit>(() -> UNIT.getCollection()),
            new MongoDbMetadataRepository<ObjectGroup>(() -> OBJECTGROUP.getCollection()),
            fieldHistoryManager
        );

        // When
        assertThatThrownBy(
            () ->
                dbRequest.execRuleRequest(
                    uuid,
                    ruleActions,
                    ruleDurationByRuleId,
                    ontologyValidator,
                    unitValidator,
                    Collections.emptyList()
                )
        ).isInstanceOf(MetadataValidationException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void shouldAllowInheritanceBeforeBlockingRule() throws Exception {
        // First case : Allow Inheritance before trying to Block Rule

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setRequestId("aeeaaaaaacagqkjjaaxpwallds4xu6iaaaaq");

        String uuid = "aeaqaaaabeghay2jabzuaalbarkww4iaaaba";

        final Unit initialUnit = new Unit(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("unitInheritanceToUpdate_2.json"))
        );

        UNIT.getCollection().insertOne(initialUnit);
        UNIT.getEsClient().insertFullDocument(UNIT, 0, uuid, initialUnit);

        // Base ontology
        List<OntologyModel> ontologyModels = JsonHandler.getFromInputStreamAsTypeReference(
            OntologyTestHelper.loadOntologies(),
            new TypeReference<List<OntologyModel>>() {}
        );

        // No external AUP Schema
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = mock(CachedArchiveUnitProfileLoader.class);

        Map<String, DurationData> ruleDurationByRuleId = ImmutableMap.of(
            "DIS-00001",
            new DurationData(1, ChronoUnit.DAYS),
            "DIS-00002",
            new DurationData(1, ChronoUnit.MONTHS)
        );
        // Request
        RuleActions ruleActions = new RuleActions();
        Set<String> preventRulesIdToBlock = new HashSet<>();
        Boolean preventInheritanceAllow = Boolean.FALSE;
        ruleActions
            .getUpdate()
            .add(
                ImmutableMap.of(
                    "DisseminationRule",
                    new RuleCategoryAction()
                        .setPreventRulesId(preventRulesIdToBlock)
                        .setPreventInheritance(preventInheritanceAllow)
                )
            );

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(100, 300);

        OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        UnitValidator unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);

        // When
        DbRequest dbRequest = new DbRequest(
            new MongoDbMetadataRepository<Unit>(() -> UNIT.getCollection()),
            new MongoDbMetadataRepository<ObjectGroup>(() -> OBJECTGROUP.getCollection()),
            fieldHistoryManager
        );

        UpdatedDocument updatedDocument = dbRequest.execRuleRequest(
            uuid,
            ruleActions,
            ruleDurationByRuleId,
            ontologyValidator,
            unitValidator,
            Collections.emptyList()
        );

        // Then
        assertThat(updatedDocument).isNotNull();
        assertThat(
            updatedDocument
                .getAfterUpdate()
                .get("_mgt")
                .get("DisseminationRule")
                .get("Inheritance")
                .get("PreventRulesId")
        ).isEqualTo(JsonHandler.createArrayNode());
        assertThat(
            Boolean.valueOf(
                updatedDocument
                    .getAfterUpdate()
                    .get("_mgt")
                    .get("DisseminationRule")
                    .get("Inheritance")
                    .get("PreventInheritance")
                    .asText()
            )
        ).isEqualTo(Boolean.FALSE);

        // Second case :  Block Rule After Allowing inheritance

        // Given
        Set<String> preventRulesIdToDelete = new HashSet<>();
        preventRulesIdToDelete.add("DIS-00001");
        preventRulesIdToDelete.add("DIS-00002");
        ruleActions
            .getUpdate()
            .add(
                ImmutableMap.of("DisseminationRule", new RuleCategoryAction().setPreventRulesId(preventRulesIdToDelete))
            );

        // When
        new DbRequest(
            new MongoDbMetadataRepository<Unit>(() -> UNIT.getCollection()),
            new MongoDbMetadataRepository<ObjectGroup>(() -> OBJECTGROUP.getCollection()),
            fieldHistoryManager
        );

        updatedDocument = dbRequest.execRuleRequest(
            uuid,
            ruleActions,
            ruleDurationByRuleId,
            ontologyValidator,
            unitValidator,
            Collections.emptyList()
        );

        // Then
        assertThat(updatedDocument).isNotNull();
        assertEquals(
            updatedDocument
                .getAfterUpdate()
                .get("_mgt")
                .get("DisseminationRule")
                .get("Inheritance")
                .get("PreventRulesId")
                .size(),
            2
        );
        assertThat(
            Boolean.valueOf(
                updatedDocument
                    .getAfterUpdate()
                    .get("_mgt")
                    .get("DisseminationRule")
                    .get("Inheritance")
                    .get("PreventInheritance")
                    .asText()
            )
        ).isEqualTo(Boolean.FALSE);
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_and_updated_document_with_no_diff_when_ops_already_present() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID_0));

        String uuid = "aeaqaaaabeghay2jabzuaalbarkww4iaaaba";

        // Base ontology with custom external types
        List<OntologyModel> ontologyModels = JsonHandler.getFromInputStreamAsTypeReference(
            OntologyTestHelper.loadOntologies(),
            new TypeReference<List<OntologyModel>>() {}
        );
        ontologyModels.addAll(
            Arrays.asList(
                new OntologyModel()
                    .setType(OntologyType.BOOLEAN)
                    .setOrigin(OntologyOrigin.EXTERNAL)
                    .setIdentifier("Flag"),
                new OntologyModel()
                    .setType(OntologyType.LONG)
                    .setOrigin(OntologyOrigin.EXTERNAL)
                    .setIdentifier("Number")
            )
        );

        final Unit initialUnit = new Unit(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile("unitToUpdate.json"))
        );

        UNIT.getCollection().insertOne(initialUnit);
        UNIT.getEsClient().insertFullDocument(UNIT, 0, uuid, initialUnit);

        // AUP Schema
        AdminManagementClientFactory adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
        doReturn(
            new RequestResponseOK<ArchiveUnitProfileModel>().addResult(
                new ArchiveUnitProfileModel()
                    .setControlSchema(PropertiesUtils.getResourceAsString("unitAUP_OK.json"))
                    .setStatus(ArchiveUnitProfileStatus.ACTIVE)
            )
        )
            .when(adminManagementClient)
            .findArchiveUnitProfilesByID("AUP_IDENTIFIER");
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = new CachedArchiveUnitProfileLoader(
            adminManagementClientFactory,
            100,
            300
        );

        DbRequest dbRequest = new DbRequest(
            new MongoDbMetadataRepository<Unit>(() -> UNIT.getCollection()),
            new MongoDbMetadataRepository<ObjectGroup>(() -> OBJECTGROUP.getCollection()),
            fieldHistoryManager
        );

        UpdateMultiQuery update = new UpdateMultiQuery();
        update.addActions(push("#operations", "aedqaaaabggsoscfaat22albarkwtiqaaaaq")); // <- here existing operation ID

        UpdateParserMultiple updateParser = new UpdateParserMultiple(mongoDbVarNameAdapter);
        updateParser.parse(update.getFinalUpdate());

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(100, 300);

        OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        UnitValidator unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);

        // When
        UpdatedDocument updatedDocument = dbRequest.execUpdateRequest(
            updateParser,
            uuid,
            UNIT,
            ontologyValidator,
            unitValidator,
            Collections.emptyList(),
            true
        );

        // Then
        String diff = String.join(
            "\n",
            VitamDocument.getConcernedDiffLines(
                VitamDocument.getUnifiedDiff(
                    JsonHandler.prettyPrint(updatedDocument.getBeforeUpdate()),
                    JsonHandler.prettyPrint(updatedDocument.getAfterUpdate())
                )
            )
        );
        assertThat(diff).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void shouldSelectTotalHitsWhenTrackTotalHitsEnabled() throws Exception {
        // Given: More than 10K documents
        int totalDocuments = 10010;
        List<Unit> units = IntStream.range(0, totalDocuments)
            .mapToObj(
                i ->
                    new Unit(
                        JsonHandler.createObjectNode()
                            .put(VitamDocument.ID, GUIDFactory.newGUID().toString())
                            .put(VitamDocument.TENANT_ID, TENANT_ID_0)
                            .put("MyCustomField", "MyCustomValue")
                    )
            )
            .collect(Collectors.toList());

        UNIT.getCollection().insertMany(units);
        UNIT.getEsClient().insertFullDocuments(UNIT, 0, units);

        ElasticsearchIndexAlias indexAlias = indexManager
            .getElasticsearchIndexAliasResolver(UNIT)
            .resolveIndexName(TENANT_ID_0);

        // When
        final QueryBuilder query = QueryBuilders.matchPhraseQuery("MyCustomField", "MyCustomValue");

        SearchResponse responseWithoutTrackTotalSizeFlag = esClientWithoutVitamBehavior.search(
            indexAlias,
            query,
            null,
            null,
            null,
            0,
            10,
            null,
            null,
            null,
            false
        );

        SearchResponse responseWithTrackTotalSizeFlag = esClientWithoutVitamBehavior.search(
            indexAlias,
            query,
            null,
            null,
            null,
            0,
            10,
            null,
            null,
            null,
            true
        );

        // Then
        assertThat(responseWithoutTrackTotalSizeFlag.getHits().getTotalHits().value).isLessThan(totalDocuments);
        assertThat(responseWithTrackTotalSizeFlag.getHits().getTotalHits().value).isEqualTo(totalDocuments);
    }
}
