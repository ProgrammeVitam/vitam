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
package fr.gouv.vitam.ihmdemo.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserHelper;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * DslQueryHelper junit test
 *
 */
public class DslQueryHelperTest {
    private static final String ACCESSION_REGISTER = "ACCESSIONREGISTER";
    public static final String ORIGINATING_AGENCY = "OriginatingAgency";
    private static final String RULES = "RULES";
    private static final String RULETYPE = "RuleType";
    private static final String EVENT_TYPE_PROCESS = "evTypeProc";

    private static final String result =
        "QUERY: Requests: " + "{\"$and\":[" + "{\"$eq\":{\"date\":\"2006-03-05\"}}," +
            "{\"$eq\":{\"events.obIdIn\":\"name\"}}," +
            "{\"$exists\":\"PUID\"},{\"$eq\":{\"evTypeProc\":\"INGEST\"}}," +
            "{\"$eq\":{\"title\":\"Archive2\"}}]}\n" +
            "\tFilter: {\"$limit\":10000,\"$orderby\":{\"evDateTime\":-1}}\n" +
            "\tProjection: {}";

    private static final String result2 =
        "QUERY: Requests: " + "{\"$and\":[{\"$exists\":\"evTypeProc\"},{\"$exists\":\"evIdProc\"}]}\n" +
            "\tFilter: {\"$limit\":10000}\n" +
            "\tProjection: {}";

    private static final String updateAction =
        "[ " + "{ $set : { mavar1 : 1, mavar2 : 1.2, mavar3 : true, mavar4 : 'ma chaine' } }," +
            "{ $unset : [ 'mavar5', 'mavar6' ] }," + "{ $inc : { mavar7 : 2 } }," + "{ $min : { mavar8 : 3 } }," +
            "{ $min : { mavar16 : 12 } }," + "{ $max : { mavar9 : 3 } }," + "{ $rename : { mavar10 : 'mavar11' } }," +
            "{ $push : { mavar12 : { $each : [ 1, 2 ] } } }," + "{ $add : { mavar13 : { $each : [ 1, 2 ] } } }," +
            "{ $pop : { mavar14 : -1 } }," + "{ $pull : { mavar15 : { $each : [ 1, 2 ] } } } ]";

    private static final String exampleUpdateMd = "{ $roots : [ 'id0' ], $query : [ " + "{ $path : [ 'id1', 'id2'] }," +
        "{ $and : [ {$exists : 'mavar1'}, {$missing : 'mavar2'}, {$isNull : 'mavar3'}, { $or : [ {$in : { 'mavar4' : [1, 2, 'maval1'] }}, { $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
        "{ $not : [ { $size : { 'mavar5' : 5 } }, { $gt : { 'mavar6' : 7 } }, { $lte : { 'mavar7' : 8 } } ] , $exactdepth : 4}," +
        "{ $not : [ { $eq : { 'mavar8' : 5 } }, { $ne : { 'mavar9' : 'ab' } }, { $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
        "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
        "{ $regex : { 'mavar14' : '^start?aa.*' }, $depth : -1 } " + "], " + "$filter : {$mult : false }," +
        "$action : " + updateAction + " }";

    private static final String UNIT_ID = "1";
    private static List<String> IMMEDIATE_PARENTS = new ArrayList<>();


    @BeforeClass
    public static void setup() throws Exception {
        IMMEDIATE_PARENTS.add("P1");
        IMMEDIATE_PARENTS.add("P2");
        IMMEDIATE_PARENTS.add("P3");
    }

    /**
     * Tests createLogBookSelectDSLQuery method : main scenario
     *
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     */
    @Test
    public void testCreateSingleQueryDSL()
        throws InvalidCreateOperationException, InvalidParseOperationException {

        final HashMap<String, String> myHashMap = new HashMap();
        myHashMap.put("title", "Archive2");
        myHashMap.put("date", "2006-03-05");
        myHashMap.put("orderby", "evDateTime");
        myHashMap.put("obIdIn", "name");
        myHashMap.put("INGEST", "date");
        myHashMap.put("FORMAT", "PUID");

        final JsonNode request = DslQueryHelper.createSingleQueryDSL(myHashMap);
        assertNotNull(request);
        final SelectParserSingle request2 = new SelectParserSingle();
        request2.parse(request);
        assertEquals(result, request2.toString());
    }

    /**
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test
    public void testCreateSingleQueryDSLEvent()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final HashMap<String, String> myHashMap = new HashMap();

        myHashMap.put("EventID", "all");
        myHashMap.put("EventType", "all");
        final JsonNode request = DslQueryHelper.createSingleQueryDSL(myHashMap);
        assertNotNull(request);
        final SelectParserSingle request2 = new SelectParserSingle();
        request2.parse(request);
        assertEquals(result2, request2.toString());
        System.out.println(request2.toString());

    }

    @Test
    public void testCreateSelectElasticsearchDSLQuery()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> queryMap = new HashMap();
        queryMap.put("titleAndDescription", "Arch");
        queryMap.put("orderby", "date");
        queryMap.put("projection_", "#id");
        queryMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

        final JsonNode selectRequest = DslQueryHelper.createSelectElasticsearchDSLQuery(queryMap);
        assertNotNull(selectRequest);

        final RequestParserMultiple selectParser = RequestParserHelper.getParser(selectRequest);
        assertTrue(selectParser instanceof SelectParserMultiple);
        assertTrue(selectParser.getRequest().getNbQueries() == 1);
        assertTrue(selectParser.getRequest().getRoots().size() == 1);
        assertTrue(selectParser.getRequest().getFilter().get("$orderby") != null);
        assertTrue(selectParser.getRequest().getProjection().size() == 1);
    }

    @Test
    public void testCreateSelectDSLQuery() throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> queryMap = new HashMap();
        queryMap.put("Title", "Archive2");
        queryMap.put("orderby", "date");
        queryMap.put("projection_", "#id");
        queryMap.put(UiConstants.SELECT_BY_ID.toString(), "1");
        queryMap.put("isAdvancedSearchFlag", "YES");

        final JsonNode selectRequest = DslQueryHelper.createSelectElasticsearchDSLQuery(queryMap);
        assertNotNull(selectRequest);

        final RequestParserMultiple selectParser = RequestParserHelper.getParser(selectRequest);
        assertTrue(selectParser instanceof SelectParserMultiple);
        assertTrue(selectParser.getRequest().getNbQueries() == 1);
        assertTrue(selectParser.getRequest().getRoots().size() == 1);
        assertTrue(selectParser.getRequest().getFilter().get("$orderby") != null);
        assertTrue(selectParser.getRequest().getProjection().size() == 1);
    }

    /**
     * Tests CreateSelectDSLQuery with empty queries part
     *
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test
    public void testEmptyQueries() throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> queryMap = new HashMap();
        queryMap.put("projection_id", "#id");
        queryMap.put("projection_qualifiers", "#qualifiers");

        final JsonNode selectRequest = DslQueryHelper.createSelectDSLQuery(queryMap);
        assertNotNull(selectRequest);

        final RequestParserMultiple selectParser = RequestParserHelper.getParser(selectRequest);
        assertTrue(selectParser instanceof SelectParserMultiple);
        assertTrue(selectParser.getRequest().getNbQueries() == 0);
        assertTrue(selectParser.getRequest().getRoots().size() == 0);
        assertTrue(selectParser.getRequest().getFilter().get("$orderby") == null);
        assertTrue(selectParser.getRequest().getProjection().size() == 1);
    }

    /**
     * Tests CreateSelectDSLQuery with invalid input parameter (empty value)
     *
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test(expected = InvalidParseOperationException.class)
    public void testInvalidParseOperationExceptionWithEmptyValue()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> queryMap = new HashMap();
        queryMap.put("title", "");
        DslQueryHelper.createSelectDSLQuery(queryMap);
    }

    /**
     * Tests CreateSelectDSLQuery with invalid input parameter (empty key)
     *
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test(expected = InvalidParseOperationException.class)
    public void testInvalidParseOperationExceptionWithEmptyKey()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> queryMap = new HashMap();
        queryMap.put("", "value");
        DslQueryHelper.createSelectDSLQuery(queryMap);
    }

    /**
     * Tests CreateUpdateDSLQuery mthod : main scenario
     *
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test
    public void testCreateUpdateDSLQuery() throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> queryMap = new HashMap();
        queryMap.put("title", "Archive2");
        queryMap.put("date", "09/09/2015");
        queryMap.put(UiConstants.SELECT_BY_ID.toString(), "#id");
        final JsonNode updateRequest = DslQueryHelper.createUpdateDSLQuery(queryMap);
        assertNotNull(updateRequest);

        final RequestParserMultiple updateParser = RequestParserHelper.getParser(updateRequest);
        assertTrue(updateParser instanceof UpdateParserMultiple);
        assertTrue(updateParser.getRequest().getActions().size() == 2);
        assertTrue(updateParser.getRequest().getRoots().size() == 1);

    }

    /**
     * Tests CreateUpdateDSLQuery with empty queries part
     *
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test
    public void testUpdateEmptyQueries() throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> queryMap = new HashMap();
        queryMap.put("title", "Mosqueteers");

        final JsonNode updateRequest = DslQueryHelper.createUpdateDSLQuery(queryMap);
        assertNotNull(updateRequest);

        final RequestParserMultiple updateParser = RequestParserHelper.getParser(updateRequest);
        assertTrue(updateParser instanceof UpdateParserMultiple);
        assertTrue(updateParser.getRequest().getActions().size() == 1);
    }

    /**
     * Tests CreateUpdateDSLQuery with invalid input parameter (empty key)
     *
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test(expected = InvalidParseOperationException.class)
    public void testUpdateQueryInvalidParseOperationExceptionWithEmptyKey()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> queryMap = new HashMap();
        queryMap.put("", "value");
        DslQueryHelper.createUpdateDSLQuery(queryMap);
    }

    /**
     * Tests CreateSelectUnitTreeDSLQuery method : main scenario
     *
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test
    public void testCreateSelectUnitTreeDSLQuery()
        throws InvalidParseOperationException, InvalidCreateOperationException {

        final JsonNode selectRequest = DslQueryHelper.createSelectUnitTreeDSLQuery(UNIT_ID, IMMEDIATE_PARENTS);
        assertNotNull(selectRequest);

        final RequestParserMultiple selectParser = RequestParserHelper.getParser(selectRequest);
        assertTrue(selectParser instanceof SelectParserMultiple);
        assertTrue(selectParser.getRequest().getNbQueries() == 1);
        assertTrue(selectParser.getRequest().getRoots().size() == 0);
        assertTrue(selectParser.getRequest().getFilter().get("$orderby") == null);
        assertTrue(
            selectParser.getRequest().getProjection().get("$fields")
                .has(UiConstants.ID.getResultCriteria()));
        assertTrue(
            selectParser.getRequest().getProjection().get("$fields")
                .has(UiConstants.TITLE.getResultCriteria()));
        assertTrue(
            selectParser.getRequest().getProjection().get("$fields")
                .has(UiConstants.UNITUPS.getResultCriteria()));
    }


    /**
     * Tests testFundsRegisterDSLQuery: method : main scenario
     *
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test
    public void testFundsRegisterDSLQuery()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> queryMap = new HashMap();
        queryMap.put(ACCESSION_REGISTER, "");

        final JsonNode selectRequest = DslQueryHelper.createSingleQueryDSL(queryMap);
        assertNotNull(selectRequest);

        final RequestParserMultiple selectParser = RequestParserHelper.getParser(selectRequest);
        assertTrue(selectParser instanceof SelectParserMultiple);
        assertTrue(selectParser.getRequest().getNbQueries() == 1);
        assertTrue(selectParser.getRequest().getRoots().size() == 0);
        assertTrue(selectParser.getRequest().getFilter().get("$orderby") == null);

        final Map<String, String> queryMap2 = new HashMap();
        queryMap2.put(ORIGINATING_AGENCY, "id01");

        final JsonNode selectRequest2 = DslQueryHelper.createSingleQueryDSL(queryMap2);
        assertNotNull(selectRequest2);

        final RequestParserMultiple selectParser2 = RequestParserHelper.getParser(selectRequest2);
        assertTrue(selectParser2 instanceof SelectParserMultiple);
        assertTrue(selectParser2.getRequest().getNbQueries() == 1);
        assertTrue(selectParser2.getRequest().getRoots().size() == 0);
        assertTrue(selectParser2.getRequest().getFilter().get("$orderby") == null);
    }


    /**
     * Tests testRulesDSLQuery: method : main scenario
     *
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test
    public void testRulesDSLQuery()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> queryMap = new HashMap();
        queryMap.put(RULES, "");

        final JsonNode selectRequest = DslQueryHelper.createSingleQueryDSL(queryMap);
        assertNotNull(selectRequest);

        final RequestParserMultiple selectParser = RequestParserHelper.getParser(selectRequest);
        assertTrue(selectParser instanceof SelectParserMultiple);
        assertTrue(selectParser.getRequest().getNbQueries() == 1);
        assertTrue(selectParser.getRequest().getRoots().size() == 0);
        assertTrue(selectParser.getRequest().getFilter().get("$orderby") == null);

        final Map<String, String> queryMap2 = new HashMap();
        queryMap2.put(RULETYPE, "AppraisingRule");

        final JsonNode selectRequest2 = DslQueryHelper.createSingleQueryDSL(queryMap2);
        assertNotNull(selectRequest2);

        final RequestParserMultiple selectParser2 = RequestParserHelper.getParser(selectRequest2);
        assertTrue(selectParser2 instanceof SelectParserMultiple);
        assertTrue(selectParser2.getRequest().getNbQueries() == 1);
        assertTrue(selectParser2.getRequest().getRoots().size() == 0);
        assertTrue(selectParser2.getRequest().getFilter().get("$orderby") == null);

        final Map<String, String> queryMap3 = new HashMap();
        queryMap3.put(RULETYPE, "AppraisingRule,test");

        final JsonNode selectRequest3 = DslQueryHelper.createSingleQueryDSL(queryMap3);
        assertNotNull(selectRequest3);

        final RequestParserMultiple selectParser3 = RequestParserHelper.getParser(selectRequest3);
        assertTrue(selectParser3 instanceof SelectParserMultiple);
        assertTrue(selectParser3.getRequest().getNbQueries() == 1);
        assertTrue(selectParser3.getRequest().getRoots().size() == 0);
        assertTrue(selectParser3.getRequest().getFilter().get("$orderby") == null);
    }
    
    /**
     * Tests testTraceabilityDSLQuery: method : main scenario
     *
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test
    public void testTraceabilityDSLQuery()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Map<String, String> queryMap = new HashMap();
        queryMap.put(EVENT_TYPE_PROCESS, "traceability");

        final JsonNode selectRequest = DslQueryHelper.createSingleQueryDSL(queryMap);
        assertNotNull(selectRequest);

        final RequestParserMultiple selectParser = RequestParserHelper.getParser(selectRequest);
        assertTrue(selectParser instanceof SelectParserMultiple);
        assertTrue(selectParser.getRequest().getNbQueries() == 1);
        assertTrue(selectParser.getRequest().getRoots().size() == 0);
        assertTrue(selectParser.getRequest().getFilter().get("$orderby") == null);

        final Map<String, String> queryMap2 = new HashMap();
        queryMap2.put(EVENT_TYPE_PROCESS, "traceability");
        queryMap2.put("orderby", "evDateTime");
        queryMap2.put("TraceabilityStartDate", "2017-01-01");
        queryMap2.put("TraceabilityEndDate", "2017-02-09");
        queryMap2.put("TraceabilityLogType", "OPERATION");

        final JsonNode selectRequest2 = DslQueryHelper.createSingleQueryDSL(queryMap2);
        assertNotNull(selectRequest2);

        final RequestParserMultiple selectParser2 = RequestParserHelper.getParser(selectRequest2);
        assertTrue(selectParser2 instanceof SelectParserMultiple);
        assertTrue(selectParser2.getRequest().getNbQueries() == 1);
        JsonNode query = selectParser2.getRequest().getQueries().get(0).getCurrentQuery();
        ArrayNode criterias = (ArrayNode)query.get("$and");
        assertEquals("2017-02-09", criterias.get(0).get("$lte").get("events.evDetData.EndDate").asText());
        assertEquals("traceability", criterias.get(1).get("$eq").get("evTypeProc").asText());
        assertEquals("2017-01-01", criterias.get(2).get("$gte").get("events.evDetData.StartDate").asText());
        assertEquals("OPERATION", criterias.get(3).get("$eq").get("events.evDetData.LogType").asText());
        
        assertTrue(selectParser2.getRequest().getRoots().size() == 0);
        JsonNode orderBy = selectParser2.getRequest().getFilter().get("$orderby");
        assertTrue(orderBy != null);
        assertTrue(orderBy.get("evDateTime").asInt() == -1);
    }

}
