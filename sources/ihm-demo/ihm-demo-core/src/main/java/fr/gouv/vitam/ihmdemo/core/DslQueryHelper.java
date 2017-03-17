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

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Helper class to create DSL queries
 *
 */
public final class DslQueryHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DslQueryHelper.class);
    /**
     * the projection part of DSL
     */
    public static final String PROJECTION_DSL = "projection_";
    private static final String EVENT_TYPE_PROCESS = "evTypeProc";
    private static final String ALL = "All";
    private static final String EVENT_ID_PROCESS = "evIdProc";
    private static final String DESCRIPTION = "Description";
    private static final String TITLE = "Title";
    private static final String EVENT_DATE_TIME = "evDateTime";
    private static final String DEFAULT_EVENT_TYPE_PROCESS = "INGEST";
    private static final String EVENT_OUT_DETAIL = "events.outDetail";
    private static final String DEFAULT_EVENT_TYPE_PROCESS_TEST = "INGEST_TEST";
    private static final String PUID = "PUID";
    private static final String RULEVALUE = "RuleValue";
    private static final String OBJECT_IDENTIFIER_INCOME = "obIdIn";
    private static final String FORMAT = "FORMAT";
    private static final String FORMAT_NAME = "FormatName";
    private static final String RULE_VALUE = "RuleValue";
    private static final String EVENTID = "EventID";
    private static final String EVENTTYPE = "EventType";
    private static final String RULES = "RULES";
    private static final String ACCESSION_REGISTER = "ACCESSIONREGISTER";
    private static final String RULETYPE = "RuleType";
    private static final String ORDER_BY = "orderby";
    private static final String TITLE_AND_DESCRIPTION = "titleAndDescription";
    private static final String PROJECTION_PREFIX = "projection_";
    private static final int DEPTH_LIMIT = 20;
    private static final String START_PREFIX = "Start";
    private static final String END_PREFIX = "End";
    private static final String START_DATE = "StartDate";
    private static final String END_DATE = "EndDate";
    private static final String TRANSACTED_DATE = "TransactedDate";
    private static final String ADVANCED_SEARCH_FLAG = "isAdvancedSearchFlag";
    private static final String YES = "yes";
    private static final String ORIGINATING_AGENCY = "OriginatingAgency";
    private static final String DATEOPERATION = "EvDateTime";
    private static final String TRACEABILITY_OK = "TraceabilityOk";
    private static final String TRACEABILITY_ID = "TraceabilityId";
    private static final String TRACEABILITY_LOG_TYPE = "TraceabilityLogType";
    private static final String TRACEABILITY_START_DATE = "TraceabilityStartDate";
    private static final String TRACEABILITY_END_DATE = "TraceabilityEndDate";
    private static final String TRACEABILITY_EV_DET_DATA = "events.evDetData";
    // FIXME when id will be implemented on traceability, change me
    private static final String TRACEABILITY_FIELD_ID = "FileName";
    private static final String TRACEABILITY_FIELD_LOG_TYPE = "LogType";


    // empty constructor
    private DslQueryHelper() {
        // empty constructor
    }


    /**
     * generate the DSL query after receiving the search criteria
     *
     *
     * @param searchCriteriaMap the map containing the criteria
     * @return DSL request
     * @throws InvalidParseOperationException if a parse exception is encountered
     * @throws InvalidCreateOperationException if an Invalid create operation is encountered
     */
    public static JsonNode createSingleQueryDSL(Map<String, String> searchCriteriaMap)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final fr.gouv.vitam.common.database.builder.request.single.Select select =
            new fr.gouv.vitam.common.database.builder.request.single.Select();
        final BooleanQuery query = and();
        BooleanQuery queryOr = null;
        for (final Entry<String, String> entry : searchCriteriaMap.entrySet()) {
            final String searchKeys = entry.getKey();
            final String searchValue = entry.getValue();

            switch (searchKeys) {
                case ORDER_BY:
                    if (EVENT_DATE_TIME.equals(searchValue)) {
                        select.addOrderByDescFilter(searchValue);
                    } else {
                        select.addOrderByAscFilter(searchValue);
                    }
                    break;

                case DEFAULT_EVENT_TYPE_PROCESS:
                    query.add(or().add(eq(EVENT_TYPE_PROCESS, DEFAULT_EVENT_TYPE_PROCESS),
                        eq(EVENT_TYPE_PROCESS, DEFAULT_EVENT_TYPE_PROCESS_TEST)));
                    break;

                case OBJECT_IDENTIFIER_INCOME:
                    query.add(eq("events.obIdIn", searchValue));
                    break;

                case FORMAT:
                    query.add(exists(PUID));
                    break;

                case FORMAT_NAME:
                    if (!searchValue.trim().isEmpty()) {
                        query.add(match("Name", searchValue));
                    }
                    break;

                case RULE_VALUE:
                    if (!searchValue.trim().isEmpty()) {
                        query.add(match(RULE_VALUE, searchValue));
                    }
                    break;

                case ACCESSION_REGISTER:
                    query.add(exists(ORIGINATING_AGENCY));
                    break;

                case ORIGINATING_AGENCY:
                    query.add(eq(ORIGINATING_AGENCY, searchValue));
                    break;

                case RULES:
                    query.add(exists(RULEVALUE));
                    break;

                case RULETYPE:
                    if (searchValue.contains(ALL)) {
                        break;
                    }
                    if (searchValue.contains(",")) {
                        queryOr = or();
                        final String[] ruleTypeArray = searchValue.split(",");
                        for (final String s : ruleTypeArray) {
                            queryOr.add(eq("RuleType", s));
                        }
                        break;
                    }
                    if (!searchValue.isEmpty()) {
                        query.add(eq("RuleType", searchValue));
                    }
                    break;

                case EVENTID:
                    if ("all".equals(searchValue)) {
                        query.add(exists(EVENT_ID_PROCESS));
                    } else {
                        query.add(eq(EVENT_ID_PROCESS, searchValue));
                    }
                    break;

                case EVENTTYPE:
                    if (!searchValue.isEmpty()) {
                        if ("all".equals(searchValue)) {
                            query.add(exists(EVENT_TYPE_PROCESS));
                        } else {
                            query.add(eq(EVENT_TYPE_PROCESS, searchValue.toUpperCase()));
                        }
                    }
                    break;
                case DATEOPERATION:
                    if (!searchValue.isEmpty()) {
                        query.add(gte(EVENT_DATE_TIME, searchValue));
                    }
                    break;
                case TRACEABILITY_OK:
                    // FIXME : check if it is normal that the end event is a step event for a traceability
                    if ("true".equals(searchValue)) {
                        query.add(eq(EVENT_OUT_DETAIL, "STP_OP_SECURISATION.OK"));
                    }
                    break;
                case TRACEABILITY_ID:
                    // FIXME : No real ID for now, search on fileName
                    if (!searchValue.isEmpty()) {
                        query.add(eq(TRACEABILITY_EV_DET_DATA + '.' + TRACEABILITY_FIELD_ID, searchValue));
                    }
                    break;
                case TRACEABILITY_LOG_TYPE:
                    if (!searchValue.isEmpty()) {
                        query.add(eq(TRACEABILITY_EV_DET_DATA + '.' + TRACEABILITY_FIELD_LOG_TYPE, searchValue));
                    }
                    break;
                case TRACEABILITY_START_DATE:
                    if (!searchValue.isEmpty()) {
                        query.add(gte(TRACEABILITY_EV_DET_DATA + '.' + START_DATE, searchValue));
                    }
                    break;
                case TRACEABILITY_END_DATE:
                    if (!searchValue.isEmpty()) {
                        query.add(lte(TRACEABILITY_EV_DET_DATA + '.' + END_DATE, searchValue));
                    }
                    break;
                default:
                    if (!searchValue.isEmpty()) {
                        query.add(eq(searchKeys, searchValue));
                    }
            }
        }
        if (queryOr != null) {
            query.add(queryOr);
        }
        select.setQuery(query);
        LOGGER.debug("{}", select.getFinalSelect());
        return select.getFinalSelect();
    }

    /**
     * @param searchCriteriaMap Criteria received from The IHM screen Empty Keys or Value is not allowed
     * @return the JSONDSL File
     * @throws InvalidParseOperationException thrown when an error occurred during parsing
     * @throws InvalidCreateOperationException thrown when an error occurred during creation
     */

    public static JsonNode createSelectDSLQuery(Map<String, String> searchCriteriaMap)
        throws InvalidParseOperationException, InvalidCreateOperationException {

        final SelectMultiQuery select = new SelectMultiQuery();

        // AND by default
        final BooleanQuery booleanQueries = and();
        for (final Entry<String, String> entry : searchCriteriaMap.entrySet()) {
            final String searchKeys = entry.getKey();
            final String searchValue = entry.getValue();

            if (searchKeys.isEmpty() || searchValue.isEmpty()) {
                throw new InvalidParseOperationException("Parameters should not be empty or null");
            }

            // Add projection for fields prefixed by projection_
            if (searchKeys.startsWith(PROJECTION_PREFIX)) {
                select.addUsedProjection(searchValue);
                continue;
            }

            // Add order by
            if (searchKeys.equals(ORDER_BY)) {
                select.addOrderByAscFilter(searchValue);
                continue;
            }

            // Add root
            if (searchKeys.equals(UiConstants.SELECT_BY_ID.toString())) {
                select.addRoots(searchValue);
                continue;
            }

            // By default add equals query
            booleanQueries.add(eq(searchKeys, searchValue));
        }

        if (booleanQueries.isReady()) {
            booleanQueries.setDepthLimit(DEPTH_LIMIT);
            select.addQueries(booleanQueries);
        }

        return select.getFinalSelect();
    }

    /**
     * @param searchCriteriaMap Criteria received from The IHM screen Empty Keys or Value is not allowed
     * @return the JSONDSL File
     * @throws InvalidParseOperationException thrown when an error occurred during parsing
     * @throws InvalidCreateOperationException thrown when an error occurred during creation
     */
    public static JsonNode createSelectElasticsearchDSLQuery(Map<String, String> searchCriteriaMap)
        throws InvalidParseOperationException, InvalidCreateOperationException {

        final SelectMultiQuery select = new SelectMultiQuery();
        final BooleanQuery andQuery = and();
        final BooleanQuery booleanQueries = or();
        String startDate = null;
        String endDate = null;
        String advancedSearchFlag = "";

        for (final Entry<String, String> entry : searchCriteriaMap.entrySet()) {
            final String searchKeys = entry.getKey();
            final String searchValue = entry.getValue();

            if (searchKeys.isEmpty() || searchValue.isEmpty()) {
                throw new InvalidParseOperationException("Parameters should not be empty or null");
            }

            // Add projection for fields prefixed by projection_
            if (searchKeys.startsWith(PROJECTION_PREFIX)) {
                select.addUsedProjection(searchValue);
                continue;
            }

            // Add order by
            if (searchKeys.equals(ORDER_BY)) {
                select.addOrderByAscFilter(searchValue);
                continue;
            }

            // Add root
            if (searchKeys.equals(UiConstants.SELECT_BY_ID.toString())) {
                select.addRoots(searchValue);
                continue;
            }

            if (searchKeys.equals(TITLE_AND_DESCRIPTION)) {
                booleanQueries.add(match(TITLE, searchValue));
                booleanQueries.add(match(DESCRIPTION, searchValue));
                continue;
            }
            if (searchKeys.equalsIgnoreCase(UiConstants.ID.getReceivedCriteria())) {
                andQuery.add(eq(UiConstants.ID.getResultCriteria(), searchValue));
                continue;
            }
            if (searchKeys.equalsIgnoreCase(TITLE)) {
                andQuery.add(match(TITLE, searchValue));
                continue;
            }
            if (searchKeys.equalsIgnoreCase(DESCRIPTION)) {
                andQuery.add(match(DESCRIPTION, searchValue));
                continue;
            }
            if (searchKeys.startsWith(START_PREFIX)) {
                startDate = searchValue;
                continue;
            }
            if (searchKeys.startsWith(END_PREFIX)) {
                endDate = searchValue;
                continue;
            }
            if (searchKeys.equalsIgnoreCase(ADVANCED_SEARCH_FLAG)) {
                advancedSearchFlag = searchValue;
                continue;
            }
            // By default add equals query
            booleanQueries.add(match(searchKeys, searchValue));
        }
        // US 509:start AND end date must be filled.
        if (!Strings.isNullOrEmpty(endDate) && !Strings.isNullOrEmpty(startDate)) {
            andQuery.add(createSearchUntisQueryByDate(startDate, endDate));
        }

        if (advancedSearchFlag.equalsIgnoreCase(YES)) {
            if (andQuery.isReady()) {
                andQuery.setDepthLimit(DEPTH_LIMIT);
                select.addQueries(andQuery);
            }
        } else {
            if (booleanQueries.isReady()) {
                booleanQueries.setDepthLimit(DEPTH_LIMIT);
                select.addQueries(booleanQueries);
            }
        }
        return select.getFinalSelect();
    }

    /**
     * @param searchCriteriaMap Criteria received from The IHM screen Empty Keys or Value is not allowed
     * @return the JSONDSL File
     * @throws InvalidParseOperationException thrown when an error occurred during parsing
     * @throws InvalidCreateOperationException thrown when an error occurred during creation
     */
    public static JsonNode createUpdateDSLQuery(Map<String, String> searchCriteriaMap)
        throws InvalidParseOperationException, InvalidCreateOperationException {

        final UpdateMultiQuery update = new UpdateMultiQuery();

        for (final Entry<String, String> entry : searchCriteriaMap.entrySet()) {
            final String searchKeys = entry.getKey();
            final String searchValue = entry.getValue();

            if (searchKeys.isEmpty()) {
                throw new InvalidParseOperationException("Parameters should not be empty or null");
            }
            // Add root
            if (searchKeys.equals(UiConstants.SELECT_BY_ID.toString())) {
                update.addRoots(searchValue);
                continue;
            }
            // Add Actions
            update.addActions(new SetAction(searchKeys, searchValue));
        }
        return update.getFinalUpdate();
    }

    /**
     * Creates Select Query to retrieve all parents relative to the unit specified by its id
     *
     * @param unitId the unit id
     * @param immediateParents immediate parents (_up field value)
     * @return DSL Select Query
     * @throws InvalidParseOperationException if error when parse json data for creating query
     * @throws InvalidCreateOperationException if exception occurred when create query
     */
    public static JsonNode createSelectUnitTreeDSLQuery(String unitId, List<String> immediateParents)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectMultiQuery selectParentsDetails = new SelectMultiQuery();

        // Add projections
        // Title
        selectParentsDetails.addUsedProjection(UiConstants.TITLE.getResultCriteria());

        // id
        selectParentsDetails.addUsedProjection(UiConstants.ID.getResultCriteria());

        // _up
        selectParentsDetails.addUsedProjection(UiConstants.UNITUPS.getResultCriteria());

        // Add query

        // Initialize immediateParents if it is null
        if (immediateParents == null) {
            immediateParents = new ArrayList<>();
        }

        immediateParents.add(unitId);
        final String[] allParentsArray = immediateParents.stream().toArray(size -> new String[size]);

        final BooleanQuery inParentsIdListQuery = and();
        inParentsIdListQuery.add(in(UiConstants.ID.getResultCriteria(), allParentsArray))
            .setDepthLimit(DEPTH_LIMIT);

        if (inParentsIdListQuery.isReady()) {
            selectParentsDetails.addQueries(inParentsIdListQuery);
        }

        return selectParentsDetails.getFinalSelect();
    }


    private static BooleanQuery createSearchUntisQueryByDate(String startDate, String endDate)
        throws InvalidCreateOperationException {

        LOGGER.debug("in createSearchUntisQueryByDate / beginDate:" + startDate + "/ endDate:" + endDate);

        final BooleanQuery query = or();

        if (!Strings.isNullOrEmpty(endDate) && !Strings.isNullOrEmpty(startDate)) {
            final BooleanQuery transactedDateBetween = and();
            // search by transacted date
            transactedDateBetween.add(gte(TRANSACTED_DATE, startDate));
            transactedDateBetween.add(lte(TRANSACTED_DATE, endDate));
            query.add(transactedDateBetween);
            // search by begin and end date
            final BooleanQuery queryAroundDate = and();
            queryAroundDate.add(gte(END_DATE, startDate));
            queryAroundDate.add(lte(START_DATE, endDate));
            query.add(queryAroundDate);
        }
        LOGGER.debug("in createSearchUntisQueryByDate / query:" + query.toString());
        return query;

    }

}
