/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.metadata.core.database.collections;

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
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.size;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBList;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.query.PathQuery;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.Delete;
import fr.gouv.vitam.common.database.builder.request.multiple.Insert;
import fr.gouv.vitam.common.database.builder.request.multiple.Select;
import fr.gouv.vitam.common.database.builder.request.multiple.Update;
import fr.gouv.vitam.common.database.parser.request.multiple.DeleteParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.InsertParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserHelper;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;


public class DbRequestTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequestTest.class);

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private static final Integer TENANT_ID_0 = new Integer(0);
    private static final Integer TENANT_ID_1 = new Integer(1);
    private final static String CLUSTER_NAME = "vitam-cluster";
    private final static String HOST_NAME = "127.0.0.1";
    private static ElasticsearchTestConfiguration config = null;

    private static ElasticsearchAccessMetadata esClient;

    private static final String DATABASE_HOST = "localhost";
    private static final boolean CREATE = false;
    private static final boolean DROP = false;
    private static final String MY_INT = "MyInt";
    private static final String CREATED_DATE = "CreatedDate";
    private static final String DESCRIPTION = "Description";
    private static final String TITLE = "Title";
    private static final String MY_BOOLEAN = "MyBoolean";
    private static final String MY_FLOAT = "MyFloat";
    private static final String UNKNOWN_VAR = "unknown";
    private static final String VALUE_MY_TITLE = "MyTitle";
    private static final String VALUE_TENANT = "MyTitle";
    private static final String ARRAY_VAR = "ArrayVar";
    private static final String ARRAY2_VAR = "Array2Var";
    private static final String EMPTY_VAR = "EmptyVar";
    static final int tenantId = 0;
    static final List tenantList =  new ArrayList(){{add(TENANT_ID_0);}};
    static final int platformId = 10;
    static MongoDbAccessMetadataImpl mongoDbAccess;
    static MongodExecutable mongodExecutable;
    static MongoClient mongoClient;
    static MongoDbVarNameAdapter mongoDbVarNameAdapter;
    static MongodProcess mongod;
    private static JunitHelper junitHelper;
    private static int port;
    private static final String REQUEST_SELECT_TEST = "{$query: {$eq: {\"id\" : \"id\" }}, $projection : []}";
    private static final String REQUEST_UPDATE_TEST = "{$query: {$eq: {\"id\" : \"id\" }}}";
    private static final String REQUEST_INSERT_TEST =
        "{ \"#id\": \"aebaaaaaaaaaaaabaahbcakzu2stfryaaaaq\", \"id\": \"id\" }";
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
    private static final String REQUEST_INSERT_TEST_ES =
        "{ \"#id\": \"aebaaaaaaaaaaaabaahbcakzu2stfryaaaaq\", \"#tenant\": 0, \"Title\": \"title vitam\", \"Description\": \"description est OK\" }";
    private static final String REQUEST_INSERT_TEST_ES_2 =
        "{ \"#id\": \"aeaqaaaaaet33ntwablhaaku6z67pzqaaaar\", \"#tenant\": 0, \"Title\": \"title vitam\", \"Description\": \"description est OK\" }";
    private static final String REQUEST_INSERT_TEST_ES_3 =
        "{ \"#id\": \"aeaqaaaaaet33ntwablhaaku6z67pzqaaaat\", \"#tenant\": 0, \"Title\": \"title vitam\", \"Description\": \"description est OK\" }";
    private static final String REQUEST_INSERT_TEST_ES_4 =
        "{ \"#id\": \"aeaqaaaaaet33ntwablhaaku6z67pzqaaaas\", \"#tenant\": 0, \"Title\": \"title sociales test_underscore othervalue france.pdf\", \"Description\": \"description est OK\" }";
    private static final String REQUEST_UPDATE_INDEX_TEST_ELASTIC =
        "{$query: { $eq : [ { $term : { 'Title' : 'vitam' , '$max_expansions' : 1  } }] } }";
    private static final String REQUEST_UPDATE_INDEX_TEST =
        "{$roots:['aeaqaaaaaaaaaaabab4roakztdjqziaaaaaq'],$query:[],$filter:{},$action:[{$set:{'date':'09/09/2015'}},{$set:{'title':'Archive2'}}]}";
    private static final String REQUEST_SELECT_TEST_ES_UPDATE =
        "{$query: { $match : { '#id' : 'aeaqaaaaaaaaaaabab4roakztdjqziaaaaaq' , '$max_expansions' : 1  } }}";
    private static final String REQUEST_INSERT_TEST_ES_UPDATE =
        "{ \"#id\": \"aeaqaaaaaaaaaaabab4roakztdjqziaaaaaq\", \"#tenant\": 0, \"title\": \"Archive3\" }";


    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {

        junitHelper = JunitHelper.getInstance();
        try {
            config = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }

        final List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode(HOST_NAME, config.getTcpPort()));

        esClient = new ElasticsearchAccessMetadata(CLUSTER_NAME, nodes);

        final MongodStarter starter = MongodStarter.getDefaultInstance();

        port = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        final MongoClientOptions options = MongoDbAccessMetadataImpl.getMongoClientOptions();

        mongoClient = new MongoClient(new ServerAddress(DATABASE_HOST, port), options);
        mongoDbAccess = new MongoDbAccessMetadataImpl(mongoClient, "vitam-test", CREATE, esClient, tenantList);
        mongoDbVarNameAdapter = new MongoDbVarNameAdapter();



    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (config == null) {
            return;
        }
        if (DROP) {
            for (final MetadataCollections col : MetadataCollections.values()) {
                if (col.getCollection() != null) {
                    col.getCollection().drop();
                }
            }
        }
        mongoDbAccess.close();
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);

        JunitHelper.stopElasticsearchForTest(config);
        esClient.close();
    }

    /**
     * Test method for
     * {@link fr.gouv.vitam.database.collections.DbRequest#execRequest(fr.gouv.vitam.request.parser.RequestParser, fr.gouv.vitam.database.collections.Result)}
     * .
     * 
     * @throws MetaDataExecutionException
     */
    @Test(expected = MetaDataExecutionException.class)
    @RunWithCustomExecutor
    public void testExecRequest() throws MetaDataExecutionException {
        // input data
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        try {
            final DbRequest dbRequest = new DbRequest();
            // INSERT
            final JsonNode insertRequest = createInsertRequestWithUUID(uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
            try {
                insertParser.parse(insertRequest);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("InsertParser: {}", insertParser);
            // Now execute the request
            executeRequest(dbRequest, insertParser);

            // SELECT
            JsonNode selectRequest = createSelectRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
            try {
                selectParser.parse(selectRequest);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", selectParser);
            // Now execute the request
            executeRequest(dbRequest, selectParser);

            // UPDATE
            final JsonNode updateRequest = createUpdateRequestWithUUID(uuid);
            // Now considering update request and parsing it as in Data Server (PATCH command)
            final UpdateParserMultiple updateParser = new UpdateParserMultiple(mongoDbVarNameAdapter);
            try {
                updateParser.parse(updateRequest);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("UpdateParser: {}", updateParser);
            // Now execute the request
            executeRequest(dbRequest, updateParser);

            // SELECT ALL
            selectRequest = createSelectAllRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                selectParser.parse(selectRequest);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", selectParser);
            // Now execute the request
            executeRequest(dbRequest, selectParser);

            // DELETE
            final JsonNode deleteRequest = createDeleteRequestWithUUID(uuid);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            final DeleteParserMultiple deleteParser = new DeleteParserMultiple(mongoDbVarNameAdapter);
            try {
                deleteParser.parse(deleteRequest);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("DeleteParser: " + deleteParser.toString());
            // Now execute the request
            executeRequest(dbRequest, deleteParser);
        } catch (MetaDataAlreadyExistException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (MetaDataNotFoundException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (InstantiationException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (InvalidParseOperationException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } finally {
            // clean
            MetadataCollections.C_UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid.toString()));
        }
    }

    /**
     * Test method for
     * {@link fr.gouv.vitam.database.collections.DbRequest#execRequest(fr.gouv.vitam.request.parser.RequestParser, fr.gouv.vitam.database.collections.Result)}
     * .
     */
    @Test(expected = MetaDataExecutionException.class)
    @RunWithCustomExecutor
    public void testExecRequestThroughRequestParserHelper() throws MetaDataExecutionException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // input data
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        try {
            final DbRequest dbRequest = new DbRequest();
            RequestParserMultiple requestParser = null;
            // INSERT
            final JsonNode insertRequest = createInsertRequestWithUUID(uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            try {
                requestParser =
                    RequestParserHelper.getParser(insertRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("InsertParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT
            JsonNode selectRequest = createSelectRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // UPDATE
            final JsonNode updateRequest = createUpdateRequestWithUUID(uuid);
            // Now considering update request and parsing it as in Data Server (PATCH command)
            try {
                requestParser =
                    RequestParserHelper.getParser(updateRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("UpdateParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT ALL
            selectRequest = createSelectAllRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // DELETE
            final JsonNode deleteRequest = createDeleteRequestWithUUID(uuid);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            try {
                requestParser =
                    RequestParserHelper.getParser(deleteRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("DeleteParser: " + requestParser.toString());

            executeRequest(dbRequest, requestParser);
        } catch (MetaDataAlreadyExistException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (MetaDataNotFoundException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (InstantiationException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (InvalidParseOperationException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } finally {
            // clean
            MetadataCollections.C_UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid.toString()));
        }
    }


    /**
     * Test method for
     * {@link fr.gouv.vitam.database.collections.DbRequest#execRequest(fr.gouv.vitam.request.parser.RequestParser, fr.gouv.vitam.database.collections.Result)}
     * .
     * 
     * @throws InvalidParseOperationException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws MetaDataNotFoundException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataExecutionException
     */
    @Test(expected = MetaDataExecutionException.class)
    @RunWithCustomExecutor
    public void testExecRequestThroughAllCommands()
        throws MetaDataExecutionException, MetaDataAlreadyExistException, MetaDataNotFoundException,
        InstantiationException, IllegalAccessException, InvalidParseOperationException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // input data
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        try {
            final DbRequest dbRequest = new DbRequest();
            RequestParserMultiple requestParser = null;
            // INSERT
            final JsonNode insertRequest = createInsertRequestWithUUID(uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            try {
                requestParser =
                    RequestParserHelper.getParser(insertRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("InsertParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT
            JsonNode selectRequest = createSelectRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // UPDATE
            final JsonNode updateRequest = clientRichUpdateBuild(uuid);
            // Now considering update request and parsing it as in Data Server (PATCH command)
            try {
                requestParser =
                    RequestParserHelper.getParser(updateRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("UpdateParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT ALL
            selectRequest = createSelectAllRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT ALL
            selectRequest = clientRichSelectAllBuild(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // DELETE
            final JsonNode deleteRequest = createDeleteRequestWithUUID(uuid);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            try {
                requestParser =
                    RequestParserHelper.getParser(deleteRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("DeleteParser: " + requestParser.toString());
            // Now execute the request
            executeRequest(dbRequest, requestParser);
        } finally {
            // clean
            MetadataCollections.C_UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid.toString()));
        }
    }

    /**
     * Test method for
     * {@link fr.gouv.vitam.database.collections.DbRequest#execRequest(fr.gouv.vitam.request.parser.RequestParser, fr.gouv.vitam.database.collections.Result)}
     * .
     *
     * @throws Exception
     */
    @Test(expected = MetaDataExecutionException.class)
    @RunWithCustomExecutor
    public void testExecRequestMultiple() throws Exception {
        // input data
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        final GUID uuid2 = GUIDFactory.newUnitGUID(tenantId);
        try {
            final DbRequest dbRequest = new DbRequest();
            RequestParserMultiple requestParser = null;

            // INSERT
            JsonNode insertRequest = createInsertRequestWithUUID(uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            requestParser = RequestParserHelper.getParser(insertRequest, mongoDbVarNameAdapter);
            LOGGER.debug("InsertParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            insertRequest = createInsertChild2ParentRequest(uuid2, uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            requestParser = RequestParserHelper.getParser(insertRequest, mongoDbVarNameAdapter);
            LOGGER.debug("InsertParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT
            JsonNode selectRequest = createSelectRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT
            selectRequest = clientSelect2Build(uuid2);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT
            selectRequest = clientSelectMultipleBuild(uuid, uuid2);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // UPDATE
            final JsonNode updateRequest = createUpdateRequestWithUUID(uuid);
            // Now considering update request and parsing it as in Data Server (PATCH command)
            try {
                requestParser =
                    RequestParserHelper.getParser(updateRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("UpdateParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT ALL
            selectRequest = createSelectAllRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // DELETE
            JsonNode deleteRequest = clientDelete2Build(uuid2);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            try {
                requestParser =
                    RequestParserHelper.getParser(deleteRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("DeleteParser: " + requestParser.toString());
            // Now execute the request
            executeRequest(dbRequest, requestParser);
            deleteRequest = createDeleteRequestWithUUID(uuid);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            try {
                requestParser =
                    RequestParserHelper.getParser(deleteRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("DeleteParser: " + requestParser.toString());
            // Now execute the request
            executeRequest(dbRequest, requestParser);
        } finally {
            // clean
            MetadataCollections.C_UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid.toString()));
            MetadataCollections.C_UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid2.toString()));
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testInsertUnitRequest() throws Exception {
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        final GUID uuid2 = GUIDFactory.newUnitGUID(tenantId);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final DbRequest dbRequest = new DbRequest();
        RequestParserMultiple requestParser = null;

        requestParser = RequestParserHelper.getParser(createInsertRequestWithUUID(uuid), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        assertEquals(1, MetadataCollections.C_UNIT.getCollection().count());

        requestParser =
            RequestParserHelper.getParser(createInsertChild2ParentRequest(uuid2, uuid), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        assertEquals(2, MetadataCollections.C_UNIT.getCollection().count());
    }

    /**
     * @param dbRequest
     * @param requestParser
     * @throws InvalidParseOperationException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws MetaDataNotFoundException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataExecutionException
     */
    private void executeRequest(DbRequest dbRequest, RequestParserMultiple requestParser)
        throws MetaDataExecutionException, MetaDataAlreadyExistException, MetaDataNotFoundException,
        InstantiationException, IllegalAccessException, InvalidParseOperationException {

        final Result result = dbRequest.execRequest(requestParser, null);
        LOGGER.debug("XXXXXXXX " + requestParser.getClass().getSimpleName() + " Result XXXXXXXX: " + result);
        assertEquals("Must have 1 result", result.getNbResult(), 1);
        assertEquals("Must have 1 result", result.getCurrentIds().size(), 1);

    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createDeleteRequestWithUUID(GUID uuid) {
        final Delete delete = new Delete();
        try {
            delete.addQueries(and().add(eq(id(), uuid.toString()), eq(TITLE, VALUE_MY_TITLE)));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("DeleteString: " + delete.getFinalDelete().toString());
        return delete.getFinalDelete();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createSelectAllRequestWithUUID(GUID uuid) {
        final Select select = new Select();
        try {
            select.addUsedProjection(all())
                .addQueries(and().add(eq(id(), uuid.toString()), eq(TITLE, VALUE_MY_TITLE)));
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // selectRequestString = select.getFinalSelect().toString();
        LOGGER.debug("SelectAllString: " + select.getFinalSelect().toString());
        return select.getFinalSelect();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createUpdateRequestWithUUID(GUID uuid) {
        final Update update = new Update();
        try {
            update.addActions(set("NewVar", false), inc(MY_INT, 2), set(DESCRIPTION, "New description"))
                .addQueries(and().add(eq(id(), uuid.toString()), eq(TITLE, VALUE_MY_TITLE)));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("UpdateString: " + update.getFinalUpdate().toString());
        return update.getFinalUpdate();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createSelectRequestWithUUID(GUID uuid) {
        final Select select = new Select();
        try {
            select.addUsedProjection(id(), TITLE, DESCRIPTION)
                .addQueries(and().add(eq(id(), uuid.toString()), eq(TITLE, VALUE_MY_TITLE)));
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("SelectString: " + select.getFinalSelect().toString());
        return select.getFinalSelect();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createInsertRequestWithUUID(GUID uuid) {
        // INSERT
        final List<String> list = Arrays.asList("val1", "val2");
        final ObjectNode data = JsonHandler.createObjectNode().put(id(), uuid.toString())
            .put(TITLE, VALUE_MY_TITLE).put(DESCRIPTION, "Ma description est bien détaillée")
            .put(CREATED_DATE, "" + LocalDateUtil.now()).put(MY_INT, 20)
            .put(tenant(), tenantId)
            .put(MY_BOOLEAN, false).putNull(EMPTY_VAR).put(MY_FLOAT, 2.0);
        try {
            data.putArray(ARRAY_VAR).addAll((ArrayNode) JsonHandler.toJsonNode(list));
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            data.putArray(ARRAY2_VAR).addAll((ArrayNode) JsonHandler.toJsonNode(list));
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        final Insert insert = new Insert();
        insert.addData(data);
        LOGGER.debug("InsertString: " + insert.getFinalInsert().toString());
        return insert.getFinalInsert();
    }


    /**
     * @param uuid child
     * @param uuid2 parent
     * @return
     */
    private JsonNode createInsertChild2ParentRequest(GUID child, GUID parent) throws Exception {
        final ObjectNode data = JsonHandler.createObjectNode().put(id(), child.toString())
            .put(TITLE, VALUE_MY_TITLE + "2").put(DESCRIPTION, "Ma description2 vitam")
            .put(CREATED_DATE, "" + LocalDateUtil.now()).put(MY_INT, 10);
        final Insert insert = new Insert();
        insert.addData(data).addQueries(eq(VitamFieldsHelper.id(), parent.toString()));
        LOGGER.debug("InsertString: " + insert.getFinalInsert().toString());
        return insert.getFinalInsert();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode clientRichSelectAllBuild(GUID uuid) {
        final Select select = new Select();
        try {
            select.addUsedProjection(all())
                .addQueries(and().add(eq(id(), uuid.toString()), eq(TITLE, VALUE_MY_TITLE),
                    exists(CREATED_DATE), missing(UNKNOWN_VAR), isNull(EMPTY_VAR),
                    or().add(in(ARRAY_VAR, "val1"), nin(ARRAY_VAR, "val3")),
                    gt(MY_INT, 1), lt(MY_INT, 100),
                    ne(MY_BOOLEAN, true), range(MY_FLOAT, 1.0, false, 100.0, true),
                    term(TITLE, VALUE_MY_TITLE), size(ARRAY2_VAR, 2)));
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("SelectAllString: " + select.getFinalSelect().toString());
        return select.getFinalSelect();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode clientRichUpdateBuild(GUID uuid) {
        final Update update = new Update();
        try {
            update.addActions(set("NewVar", false), inc(MY_INT, 2), set(DESCRIPTION, "New description"),
                unset(UNKNOWN_VAR), push(ARRAY_VAR, "val2"), min(MY_FLOAT, 1.5),
                add(ARRAY2_VAR, "val2"))
                .addQueries(and().add(eq(id(), uuid.toString()), eq(TITLE, VALUE_MY_TITLE)));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("UpdateString: " + update.getFinalUpdate().toString());
        return update.getFinalUpdate();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode clientSelect2Build(GUID uuid) {
        final Select select = new Select();
        try {
            select.addUsedProjection(id(), TITLE, DESCRIPTION)
                .addQueries(eq(id(), uuid.toString()).setDepthLimit(2));
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("SelectString: " + select.getFinalSelect().toString());
        return select.getFinalSelect();
    }

    /**
     * @param uuid father
     * @param uuid2 son
     * @return
     */
    private JsonNode clientSelectMultipleBuild(GUID uuid, GUID uuid2) {
        final Select select = new Select();
        try {
            select.addUsedProjection(id(), TITLE, DESCRIPTION)
                .addQueries(and().add(eq(id(), uuid.toString()), eq(TITLE, VALUE_MY_TITLE)),
                    eq(id(), uuid2.toString()));
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("SelectString: " + select.getFinalSelect().toString());
        return select.getFinalSelect();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode clientDelete2Build(GUID uuid) {
        final Delete delete = new Delete();
        try {
            delete.addQueries(path(uuid.toString()));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("DeleteString: " + delete.getFinalDelete().toString());
        return delete.getFinalDelete();
    }

    @Test
    @RunWithCustomExecutor
    public void testResult() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);        
        final DbRequest dbRequest = new DbRequest();
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        LOGGER.debug("InsertParser: {}", insertParser);
        final Result result = dbRequest.execRequest(insertParser, null);
        assertEquals("Document{{results=null}}", result.getFinal().toString());

        final Bson projection = null;
        result.setFinal(projection);
        assertEquals(1, result.currentIds.size());
        assertEquals(1, (int) result.nbResult);

        result.putFrom(result);
        assertEquals(1, result.currentIds.size());
        assertEquals(1, (int) result.nbResult);

        final Set<String> currentIds = new HashSet<>();
        currentIds.add("UNITS");
        currentIds.add("OBJECTGROUPS");
        result.setCurrentIds(currentIds);
        assertEquals(2, result.currentIds.size());
    }

    @Test
    public void testMongoDbAccess() {
        for (final MetadataCollections col : MetadataCollections.values()) {
            if (col.getCollection() != null) {
                col.getCollection().drop();
            }
        }
        final MongoDatabase db = mongoDbAccess.getMongoDatabase();
        assertEquals(0, db.getCollection("Unit").count());
        assertEquals(0, db.getCollection("Objectgroup").count());
        mongoDbAccess = new MongoDbAccessMetadataImpl(mongoClient, "vitam-test", CREATE, esClient, tenantList);
        assertNotNull(mongoDbAccess.toString());
    }


    private ObjectNode createInsertRequestGO(GUID uuid, GUID uuidParent) throws InvalidParseOperationException {
        // Create Insert command as in Internal Vitam Modules
        final Insert insert = new Insert();
        insert.resetFilter();
        insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        final JsonNode json = JsonHandler.getFromString("{\"#id\":\"" + uuid +
            "\", \"#qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\", \"Description\" : \"Test\"}}, \"title\":\"title1\"}");
        insert.addData((ObjectNode) json);
        insert.addRoots(uuidParent.getId());
        final ObjectNode insertRequest = insert.getFinalInsert();
        LOGGER.debug("InsertString: " + insertRequest);
        return insertRequest;
    }

    @Test
    @RunWithCustomExecutor
    public void testInsertGORequest() throws Exception {
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final DbRequest dbRequest = new DbRequest();
        RequestParserMultiple requestParser = null;

        requestParser = RequestParserHelper.getParser(createInsertRequestWithUUID(uuid), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        Result result = checkExistence(dbRequest, uuid, false);
        assertFalse(result.isError());

        final GUID uuid2 = GUIDFactory.newObjectGroupGUID(tenantId);
        requestParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        requestParser.parse(createInsertRequestGO(uuid2, uuid));

        executeRequest(dbRequest, requestParser);
        result = checkExistence(dbRequest, uuid2, true);
        assertFalse(result.isError());
    }

    private Result checkExistence(DbRequest dbRequest, GUID uuid, boolean isOG)
        throws InvalidCreateOperationException, InvalidParseOperationException, MetaDataExecutionException,
        MetaDataAlreadyExistException, MetaDataNotFoundException, InstantiationException, IllegalAccessException {
        final Select select = new Select();
        select.addQueries(eq(VitamFieldsHelper.id(), uuid.getId()));
        if (isOG) {
            select.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        }
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(select.getFinalSelect());
        return dbRequest.execRequest(selectParser, null);
    }

    @Test
    @RunWithCustomExecutor
    public void testUnitParentForlastInsertFilterProjection() throws Exception {
        final DbRequest dbRequest = new DbRequest();
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        final GUID uuid = GUIDFactory.newObjectGroupGUID(tenantId);
        final GUID uuidUnit = GUIDFactory.newUnitGUID(tenantId);


        final JsonNode insertRequest = createInsertRequestWithUUID(uuidUnit);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        try {
            insertParser.parse(insertRequest);
        } catch (final InvalidParseOperationException e) {
            fail(e.getMessage());
        }
        executeRequest(dbRequest, insertParser);

        final Insert insert = new Insert();
        insert.resetFilter();
        insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        insert.addRoots(uuidUnit.getId());
        final ObjectNode json = (ObjectNode) JsonHandler.getFromString("{\"#id\":\"" + uuid +
            "\", \"#qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\", \"Description\" : \"Test\"}}, \"title\":\"title1\"}");

        insert.addData(json);
        final ObjectNode insertNode = insert.getFinalInsert();

        final RequestParserMultiple requestParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        requestParser.parse(insertNode);
        executeRequest(dbRequest, requestParser);
        assertFalse(requestParser.getRequest().getRoots().isEmpty());
        // Check _og
        Result result = checkExistence(dbRequest, uuidUnit, false);
        assertFalse(result.isError());
        BasicDBList list = (BasicDBList) result.getFinal().get(Result.RESULT_FIELD);
        final Document unit = (Document) list.get(0);
        assertTrue(unit.getString("_og").equals(uuid.getId()));

        // Check _up is set as _og
        result = checkExistence(dbRequest, uuid, true);
        assertFalse(result.isError());
        list = (BasicDBList) result.getFinal().get(Result.RESULT_FIELD);
        final Document og = (Document) list.get(0);
        System.err.println(og);
        System.err.println(og.get("_up"));
        System.err.println(uuidUnit.getId());
        assertTrue(((List<String>) og.get("_up")).contains(uuidUnit.getId()));

    }

    @Test
    @RunWithCustomExecutor
    public void testRequestWithObjectGroupQuery() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID uuid01 = GUIDFactory.newUnitGUID(tenantId);
        final DbRequest dbRequest = new DbRequest();
        RequestParserMultiple requestParser =
            RequestParserHelper.getParser(createInsertRequestWithUUID(uuid01), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        Result result = checkExistence(dbRequest, uuid01, false);
        assertFalse(result.isError());
        final GUID uuid02 = GUIDFactory.newUnitGUID(tenantId);
        requestParser = RequestParserHelper.getParser(createInsertRequestWithUUID(uuid02), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        result = checkExistence(dbRequest, uuid02, false);
        assertFalse(result.isError());

        final GUID uuid1 = GUIDFactory.newObjectGroupGUID(tenantId);
        final GUID uuid2 = GUIDFactory.newObjectGroupGUID(tenantId);
        final Insert insert = new Insert();
        insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());

        final ObjectNode json = (ObjectNode) JsonHandler.getFromString("{\"#id\":\"" + uuid1 +
            "\", \"#qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\", \"Description\" : \"Test\"}}, \"title\":\"title1\"}");
        final ObjectNode json1 = (ObjectNode) JsonHandler.getFromString("{\"#id\":\"" + uuid2 +
            "\", \"#qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\", \"Description1\" : \"Test\"}}, \"title\":\"title1\"}");
        insert.addData(json).addRoots(uuid01.getId());
        ObjectNode insertRequestString = insert.getFinalInsert();
        requestParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        requestParser.parse(insertRequestString);
        executeRequest(dbRequest, requestParser);

        final PathQuery query = path(uuid02.getId());
        insert.reset().addQueries(query).addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        insert.addData(json1);
        insertRequestString = insert.getFinalInsert();
        final RequestParserMultiple requestParser1 = new InsertParserMultiple(mongoDbVarNameAdapter);
        requestParser1.parse(insertRequestString);
        executeRequest(dbRequest, requestParser1);
    }


    @Test
    @RunWithCustomExecutor
    public void testSelectResult() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final DbRequest dbRequest = new DbRequest();
        final JsonNode selectRequest = JsonHandler.getFromString(REQUEST_SELECT_TEST);
        final SelectParserMultiple selectParser = new SelectParserMultiple();
        selectParser.parse(selectRequest);
        LOGGER.debug("SelectParser: {}", selectRequest);
        final Result result = dbRequest.execRequest(selectParser, null);
        assertEquals("Document{{results=null}}", result.getFinal().toString());

    }


    @Test
    @RunWithCustomExecutor
    public void shouldSelectUnitResult() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        final DbRequest dbRequest = new DbRequest();
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_1);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        final JsonNode selectRequest = JsonHandler.getFromString(REQUEST_SELECT_TEST);
        final SelectParserMultiple selectParser = new SelectParserMultiple();
        selectParser.parse(selectRequest);
        LOGGER.debug("SelectParser: {}", selectRequest);
        final Result result2 = dbRequest.execRequest(selectParser, null);
        assertEquals(1, result2.nbResult);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldSelectNoResultSinceOtherTenantUsed() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(5);
        final DbRequest dbRequest = new DbRequest();
        // unit is insterted with tenantId = 0
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_2);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        final JsonNode selectRequest = JsonHandler.getFromString(REQUEST_SELECT_TEST);
        final SelectParserMultiple selectParser = new SelectParserMultiple();
        selectParser.parse(selectRequest);
        LOGGER.debug("SelectParser: {}", selectRequest);
        final Result result2 = dbRequest.execRequest(selectParser, null);
        assertEquals(0, result2.nbResult);
    }


    @Test
    @RunWithCustomExecutor
    public void shouldSelectUnitResultWithES() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        esClient.deleteIndex(MetadataCollections.C_UNIT, TENANT_ID_0);
        esClient.addIndex(MetadataCollections.C_UNIT, TENANT_ID_0);

        //OG ???
        
        final DbRequest dbRequest = new DbRequest();
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_ES);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        final JsonNode selectRequest1 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_1);
        final SelectParserMultiple selectParser1 = new SelectParserMultiple();
        selectParser1.parse(selectRequest1);
        LOGGER.debug("SelectParser: {}", selectRequest1);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_0);
        final Result resultSelect1 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelect1.nbResult);

        final JsonNode selectRequest2 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_2);
        final SelectParserMultiple selectParser2 = new SelectParserMultiple();
        selectParser2.parse(selectRequest2);
        LOGGER.debug("SelectParser: {}", selectRequest2);
        final Result resultSelect2 = dbRequest.execRequest(selectParser2, null);
        assertEquals(1, resultSelect2.nbResult);

        final JsonNode selectRequest3 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_3);
        final SelectParserMultiple selectParser3 = new SelectParserMultiple();
        selectParser3.parse(selectRequest3);
        LOGGER.debug("SelectParser: {}", selectRequest3);
        final Result resultSelect3 = dbRequest.execRequest(selectParser3, null);
        assertEquals(1, resultSelect3.nbResult);

        final JsonNode selectRequest4 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_4);
        final SelectParserMultiple selectParser4 = new SelectParserMultiple();
        selectParser4.parse(selectRequest4);
        LOGGER.debug("SelectParser: {}", selectRequest4);
        final Result resultSelect4 = dbRequest.execRequest(selectParser4, null);
        assertEquals(1, resultSelect4.nbResult);

        Insert insert = new Insert();
        insert.parseData(REQUEST_INSERT_TEST_ES_2).addRoots("aebaaaaaaaaaaaabaahbcakzu2stfryaaaaq");
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_0);

        Select select = new Select();
        select.addQueries(match("Description", "description OK").setDepthLimit(1))
            .addRoots("aebaaaaaaaaaaaabaahbcakzu2stfryaaaaq");
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel0 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel0.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaar",
            resultSelectRel0.getCurrentIds().iterator().next().toString());

        select = new Select();
        select.addQueries(match("Description", "description OK").setDepthLimit(1))
            .addRoots("aebaaaaaaaaaaaabaahbcakzu2stfryaaaaq");
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel1 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel1.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaar",
            resultSelectRel1.getCurrentIds().iterator().next().toString());

        select = new Select();
        select.addQueries(match("Description", "description OK").setDepthLimit(3))
            .addRoots("aebaaaaaaaaaaaabaahbcakzu2stfryaaaaq");
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel3 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel3.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaar",
            resultSelectRel3.getCurrentIds().iterator().next().toString());

        insert = new Insert();
        insert.parseData(REQUEST_INSERT_TEST_ES_3).addRoots("aeaqaaaaaet33ntwablhaaku6z67pzqaaaar");
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_0);

        final Result resultSelectRel4 = dbRequest.execRequest(selectParser1, null);
        assertEquals(2, resultSelectRel4.nbResult);
        for (final String root : resultSelectRel4.getCurrentIds()) {
            assertTrue(root.equalsIgnoreCase("aeaqaaaaaet33ntwablhaaku6z67pzqaaaat") ||
                root.equalsIgnoreCase("aeaqaaaaaet33ntwablhaaku6z67pzqaaaar"));
        }

        insert = new Insert();
        insert.parseData(REQUEST_INSERT_TEST_ES_4).addRoots("aebaaaaaaaaaaaabaahbcakzu2stfryaaaaq");
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_0);

        select = new Select();
        select.addQueries(match("Title", "othervalue").setDepthLimit(1))
            .addRoots("aebaaaaaaaaaaaabaahbcakzu2stfryaaaaq");
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel5 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel5.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaas",
            resultSelectRel5.getCurrentIds().iterator().next().toString());

        // Check for "France.pdf"
        select = new Select();
        select.addRoots("aebaaaaaaaaaaaabaahbcakzu2stfryaaaaq").addQueries(match("Title", "Frânce").setDepthLimit(1));
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel6 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel6.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaas",
            resultSelectRel6.getCurrentIds().iterator().next().toString());

        // Check for "social vs sociales"
        select = new Select();
        select.addRoots("aebaaaaaaaaaaaabaahbcakzu2stfryaaaaq").addQueries(match("Title", "social").setDepthLimit(1));
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel7 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel7.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaas",
            resultSelectRel7.getCurrentIds().iterator().next().toString());

        // Check for "name_with_underscore"
        select = new Select();
        select.addRoots("aebaaaaaaaaaaaabaahbcakzu2stfryaaaaq")
            .addQueries(match("Title", "underscore").setDepthLimit(1));
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel8 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel8.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaas",
            resultSelectRel8.getCurrentIds().iterator().next().toString());
    }
    
    @Test
    @RunWithCustomExecutor
    public void shouldSelectUnitResultWithESTenant1() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_1);
        esClient.addIndex(MetadataCollections.C_UNIT, TENANT_ID_1);
        
        final DbRequest dbRequest = new DbRequest();
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_ES);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        final JsonNode selectRequest1 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_1);
        final SelectParserMultiple selectParser1 = new SelectParserMultiple();
        selectParser1.parse(selectRequest1);
        LOGGER.debug("SelectParser: {}", selectRequest1);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_1);
        final Result resultSelect1 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelect1.nbResult);

        esClient.deleteIndex(MetadataCollections.C_UNIT, TENANT_ID_1);
    }


    @Test
    @RunWithCustomExecutor
    public void testUpdateUnitResult() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        // insert title ARchive 3
        final DbRequest dbRequest = new DbRequest();
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        final Insert insert = new Insert();
        insert.parseData(REQUEST_INSERT_TEST_ES_UPDATE).addRoots("aeaqaaaaaaaaaaabab4roakztdjqziaaaaaq");
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_0);

        // update title Archive 3 to Archive 2
        final JsonNode updateRequest = JsonHandler.getFromString(REQUEST_UPDATE_INDEX_TEST);
        final UpdateParserMultiple updateParser = new UpdateParserMultiple();
        updateParser.parse(updateRequest);
        LOGGER.debug("UpdateParser: {}", updateParser.getRequest());
        final Result result2 = dbRequest.execRequest(updateParser, null);
        LOGGER.debug("result2", result2.getNbResult());
        assertEquals(1, result2.nbResult);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_0);

        // check old value should not exist in the collection
        final JsonNode selectRequest2 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_UPDATE);
        final SelectParserMultiple selectParser2 = new SelectParserMultiple();
        final Select select1 = new Select();
        select1.addQueries(eq("title", "Archive3").setDepthLimit(1))
            .addRoots("aeaqaaaaaaaaaaabab4roakztdjqziaaaaaq");
        selectParser2.parse(select1.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectRequest2);
        final Result resultSelectRel6 = dbRequest.execRequest(selectParser2, null);
        assertEquals(0, resultSelectRel6.nbResult);

        // check new value should exist in the collection
        final JsonNode selectRequest1 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_UPDATE);
        final SelectParserMultiple selectParser1 = new SelectParserMultiple();
        final Select select = new Select();
        select.addQueries(eq("title", "Archive2").setDepthLimit(1)).addRoots("aeaqaaaaaaaaaaabab4roakztdjqziaaaaaq");
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectRequest1);
        final Result resultSelectRel5 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel5.nbResult);
        assertEquals("aeaqaaaaaaaaaaabab4roakztdjqziaaaaaq",
            resultSelectRel5.getCurrentIds().iterator().next().toString());
    }

    private static final JsonNode buildQueryJsonWithOptions(String query, String data)
        throws Exception {
        return JsonHandler.getFromString(new StringBuilder()
            .append("{ $roots : [ '' ], ")
            .append("$query : [ " + query + " ], ")
            .append("$data : " + data + " }")
            .toString());
    }
    
    
  @Test
  @RunWithCustomExecutor
  public void testInsertUnitWithTenant() throws Exception {
      VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
      esClient.addIndex(MetadataCollections.C_UNIT, TENANT_ID_0);
      esClient.addIndex(MetadataCollections.C_UNIT, TENANT_ID_1);
      final DbRequest dbRequest = new DbRequest();
      final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_1);
      final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
      insertParser.parse(insertRequest);
      LOGGER.debug("InsertParser: {}", insertParser);
      dbRequest.execRequest(insertParser, null);

      final JsonNode selectRequest = JsonHandler.getFromString(REQUEST_SELECT_TEST);
      final SelectParserMultiple selectParser = new SelectParserMultiple();
      selectParser.parse(selectRequest);
      LOGGER.debug("SelectParser: {}", selectRequest);
      VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_1);
      final Result result3 = dbRequest.execRequest(selectParser, null);
      assertEquals(0, result3.nbResult);
  }    

  private JsonNode createInsertRequestGOTenant(GUID uuid) throws InvalidParseOperationException {
      // INSERT
      final List<String> list = Arrays.asList("val1", "val2");
      final ObjectNode data = JsonHandler.createObjectNode().put(id(), uuid.toString())
          .put(TITLE, VALUE_MY_TITLE).put(DESCRIPTION, "Ma description est bien détaillée")
          .put(CREATED_DATE, "" + LocalDateUtil.now()).put(MY_INT, 20)
          .put(tenant(), tenantId)
          .put(MY_BOOLEAN, false).putNull(EMPTY_VAR).put(MY_FLOAT, 2.0);
      try {
          data.putArray(ARRAY_VAR).addAll((ArrayNode) JsonHandler.toJsonNode(list));
      } catch (final InvalidParseOperationException e) {
          e.printStackTrace();
          fail(e.getMessage());
      }
    try {
        data.putArray("_up").addAll((ArrayNode) JsonHandler.toJsonNode(list));
       } catch (final InvalidParseOperationException e) {
           e.printStackTrace();
           fail(e.getMessage());
       }
    final Insert insert = new Insert();
    insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
      insert.addData(data);
      LOGGER.debug("InsertString: " + insert.getFinalInsert().toString());
      return insert.getFinalInsert();
  }
  
    @Test
    @RunWithCustomExecutor
    public void testInsertGOWithTenant() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        esClient.addIndex(MetadataCollections.C_OBJECTGROUP, TENANT_ID_0);
        esClient.addIndex(MetadataCollections.C_OBJECTGROUP, TENANT_ID_1);
    
        final GUID uuid = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        final DbRequest dbRequest = new DbRequest();
        RequestParserMultiple requestParser = null;
        requestParser = RequestParserHelper.getParser(createInsertRequestGOTenant(uuid), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        
        final Select select = new Select();
        select.addQueries(eq(VitamFieldsHelper.id(), uuid.getId()));
        select.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(select.getFinalSelect());
        final Result result = dbRequest.execRequest(selectParser, null);        
        assertEquals(1, result.nbResult);
        
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_1);
        final Result result2 = dbRequest.execRequest(selectParser, null);
        assertEquals(0, result2.nbResult);
    }    
}
