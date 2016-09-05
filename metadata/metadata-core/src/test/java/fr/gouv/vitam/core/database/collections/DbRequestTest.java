/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * consultation-vitam@culture.gouv.fr
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
package fr.gouv.vitam.core.database.collections;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.isNull;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lt;
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
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.add;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.inc;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.min;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.push;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.set;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.unset;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
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
import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
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
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;


public class DbRequestTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequestTest.class);

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    private static File elasticsearchHome;

    private final static String CLUSTER_NAME = "vitam-cluster";
    private final static String HOST_NAME = "127.0.0.1";
    private static int TCP_PORT = 9300;
    private static int HTTP_PORT = 9200;
    private static Node node;
    
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
    private static final String ARRAY_VAR = "ArrayVar";
    private static final String ARRAY2_VAR = "Array2Var";
    private static final String EMPTY_VAR = "EmptyVar";
    static final int tenantId = 0;
    static final int platformId = 10;
    static MongoDbAccessMetadataImpl mongoDbAccess;
    static MongodExecutable mongodExecutable;
    static MongoClient mongoClient;
    static MongoDbVarNameAdapter mongoDbVarNameAdapter;
    static MongodProcess mongod;
    private static JunitHelper junitHelper;
    private static int port;
    private static final String REQUEST_SELECT_TEST =
        "{$query: {$eq: {\"id\" : \"id\" }}}";

    private static final String REQUEST_UPDATE_TEST =
        "{$query: {$eq: {\"id\" : \"id\" }}}";
    private static final String REQUEST_INSERT_TEST = "{ \"id\": \"id\" }";;


    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
        
        junitHelper = new JunitHelper();
        //ES
        TCP_PORT = junitHelper.findAvailablePort();
        HTTP_PORT = junitHelper.findAvailablePort();

        elasticsearchHome = tempFolder.newFolder();
        Settings settings = Settings.settingsBuilder()
            .put("http.enabled", true)
            .put("discovery.zen.ping.multicast.enabled", false)
            .put("transport.tcp.port", TCP_PORT)
            .put("http.port", HTTP_PORT)
            .put("path.home", elasticsearchHome.getCanonicalPath())
            .build();

        node = nodeBuilder()
            .settings(settings)
            .client(false)
            .clusterName(CLUSTER_NAME)
            .node();

       node.start();
        
        List<ElasticsearchNode> nodes = new ArrayList<ElasticsearchNode>();
        nodes.add(new ElasticsearchNode(HOST_NAME, TCP_PORT));
       
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
        mongoDbAccess = new MongoDbAccessMetadataImpl(mongoClient, "vitam-test", CREATE, esClient);
        mongoDbVarNameAdapter = new MongoDbVarNameAdapter();
        
        
        
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
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
        
        if (node != null) {
            node.close();
        }

        junitHelper.releasePort(TCP_PORT);
        junitHelper.releasePort(HTTP_PORT);
    }


    /**
     * Test method for
     * {@link fr.gouv.vitam.database.collections.DbRequest#execRequest(fr.gouv.vitam.request.parser.RequestParser, fr.gouv.vitam.database.collections.Result)}
     * .
     */
    @Test
    public void testExecRequest() {
        // input data
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
    @Test
    public void testExecRequestThroughRequestParserHelper() {
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
     */
    @Test
    public void testExecRequestThroughAllCommands() {
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
    @Test
    public void testExecRequestMultiple() throws Exception {
        // input data
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
    public void testInsertUnitRequest() throws Exception {
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        final GUID uuid2 = GUIDFactory.newUnitGUID(tenantId);
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
     */
    private void executeRequest(DbRequest dbRequest, RequestParserMultiple requestParser) {
        try {
            final Result result = dbRequest.execRequest(requestParser, null);
            LOGGER.debug("XXXXXXXX " + requestParser.getClass().getSimpleName() + " Result XXXXXXXX: " + result);
            assertEquals("Must have 1 result", result.getNbResult(), 1);
            assertEquals("Must have 1 result", result.getCurrentIds().size(), 1);
        } catch (InstantiationException | IllegalAccessException | MetaDataExecutionException |
            InvalidParseOperationException | MetaDataAlreadyExistException | MetaDataNotFoundException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
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
        insert.addData(data).addQueries(exists("Title"));
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
    public void testSetDebug() {
        final DbRequest request = new DbRequest();
        assertEquals(true, request.debug);
        request.setDebug(false);
        assertEquals(false, request.debug);
    }

    @Test
    public void testResult() throws Exception {
        final DbRequest dbRequest = new DbRequest();
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        LOGGER.debug("InsertParser: {}", insertParser);
        final Result result = dbRequest.execRequest(insertParser, null);
        assertEquals("Document{{Result=null}}", result.getFinal().toString());

        final Bson projection = null;
        result.setFinal(projection);
        assertEquals(1, result.currentIds.size());
        assertEquals(1, (int) result.nbResult);

        result.putFrom(result);
        assertEquals(1, result.currentIds.size());
        assertEquals(1, (int) result.nbResult);

        final Set<String> currentIds = new HashSet<String>();
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
        mongoDbAccess = new MongoDbAccessMetadataImpl(mongoClient, "vitam-test", CREATE, esClient);
        assertNotNull(mongoDbAccess.toString());
    }


    private ObjectNode createInsertRequestGO(GUID uuid, GUID uuidParent) throws InvalidParseOperationException {
        // Create Insert command as in Internal Vitam Modules
        Insert insert = new Insert();
        insert.resetFilter();
        insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        JsonNode json = JsonHandler.getFromString("{\"_id\":\"" + uuid +
            "\", \"_qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\", \"Description\" : \"Test\"}}, \"title\":\"title1\"}");
        insert.addData((ObjectNode) json);
        insert.addRoots(uuidParent.getId());
        final ObjectNode insertRequest = insert.getFinalInsert();
        LOGGER.debug("InsertString: " + insertRequest);
        return insertRequest;
    }

    @Test
    public void testInsertGORequest() throws Exception {
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
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
        Select select = new Select();
        select.addQueries(eq(VitamFieldsHelper.id(), uuid.getId()));
        if (isOG) {
            select.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        }
        SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(select.getFinalSelect());
        return dbRequest.execRequest(selectParser, null);
    }

    @Test
    public void testUnitParentForlastInsertFilterProjection() throws Exception {
        final DbRequest dbRequest = new DbRequest();


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

        Insert insert = new Insert();
        insert.resetFilter();
        insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        insert.addRoots(uuidUnit.getId());
        ObjectNode json = (ObjectNode) JsonHandler.getFromString("{\"_id\":\"" + uuid +
            "\", \"_qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\", \"Description\" : \"Test\"}}, \"title\":\"title1\"}");

        insert.addData((ObjectNode) json);
        final ObjectNode insertNode = insert.getFinalInsert();

        RequestParserMultiple requestParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        requestParser.parse(insertNode);
        executeRequest(dbRequest, requestParser);
        assertFalse(requestParser.getRequest().getRoots().isEmpty());
        // Check _og
        Result result = checkExistence(dbRequest, uuidUnit, false);
        assertFalse(result.isError());
        BasicDBList list = (BasicDBList) result.getFinal().get(Result.RESULT_FIELD);
        Document unit = (Document) list.get(0);
        assertTrue(unit.getString("_og").equals(uuid.getId()));

        // Check _up is set as _og
        result = checkExistence(dbRequest, uuid, true);
        assertFalse(result.isError());
        list = (BasicDBList) result.getFinal().get(Result.RESULT_FIELD);
        Document og = (Document) list.get(0);
        System.err.println(og);
        System.err.println(og.get("_up"));
        System.err.println(uuidUnit.getId());
        assertTrue(((List<String>) og.get("_up")).contains(uuidUnit.getId()));

    }

    @Test
    public void testRequestWithObjectGroupQuery() throws Exception {
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
        Insert insert = new Insert();
        insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());

        ObjectNode json = (ObjectNode) JsonHandler.getFromString("{\"_id\":\"" + uuid1 +
            "\", \"_qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\", \"Description\" : \"Test\"}}, \"title\":\"title1\"}");
        ObjectNode json1 = (ObjectNode) JsonHandler.getFromString("{\"_id\":\"" + uuid2 +
            "\", \"_qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\", \"Description1\" : \"Test\"}}, \"title\":\"title1\"}");
        insert.addData((ObjectNode) json).addRoots(uuid01.getId());
        ObjectNode insertRequestString = insert.getFinalInsert();
        requestParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        requestParser.parse(insertRequestString);
        executeRequest(dbRequest, requestParser);

        PathQuery query = path(uuid02.getId());
        insert.reset().addQueries(query).addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        insert.addData((ObjectNode) json1);
        insertRequestString = insert.getFinalInsert();
        RequestParserMultiple requestParser1 = new InsertParserMultiple(mongoDbVarNameAdapter);
        requestParser1.parse(insertRequestString);
        executeRequest(dbRequest, requestParser1);
    }


    @Test
    public void testSelectResult() throws Exception {
        final DbRequest dbRequest = new DbRequest();
        final JsonNode selectRequest = JsonHandler.getFromString(REQUEST_SELECT_TEST);
        final SelectParserMultiple selectParser = new SelectParserMultiple();
        selectParser.parse(selectRequest);
        LOGGER.debug("SelectParser: {}", selectRequest);
        final Result result = dbRequest.execRequest(selectParser, null);
        assertEquals("Document{{Result=null}}", result.getFinal().toString());

    }


    @Test
    public void shouldSelectUnitResult() throws Exception {

        final DbRequest dbRequest = new DbRequest();
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST);
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

    public void shouldUpdateUnitResult() throws Exception {

        final DbRequest dbRequest = new DbRequest();
        final JsonNode updateRequest = JsonHandler.getFromString(REQUEST_UPDATE_TEST);
        final UpdateParserMultiple updateParser = new UpdateParserMultiple();
        updateParser.parse(updateRequest);
        LOGGER.debug("UpdateParser: {}", updateRequest);
        final Result result2 = dbRequest.execRequest(updateParser, null);
        assertEquals(1, result2.nbResult);

    }

    private static final JsonNode buildQueryJsonWithOptions(String query, String data)
        throws Exception {
        return JsonHandler.getFromString(new StringBuilder()
            .append("{ $roots : [ '' ], ")
            .append("$query : [ " + query + " ], ")
            .append("$data : " + data + " }")
            .toString());
    }
}
