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
package fr.gouv.vitam.ihmdemo.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.facet.DateRangeFacet;
import fr.gouv.vitam.common.database.builder.facet.FiltersFacet;
import fr.gouv.vitam.common.database.builder.facet.RangeFacetValue;
import fr.gouv.vitam.common.database.builder.facet.TermsFacet;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.ExistsQuery;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.RangeQuery;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.SetregexAction;
import fr.gouv.vitam.common.database.builder.query.action.UnsetAction;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestFacetItem;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.missing;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.nestedSearch;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * Helper class to create DSL queries
 */
public class DslQueryHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DslQueryHelper.class);
    /**
     * the projection part of DSL
     */
    public static final String PROJECTION_DSL = "projection_";
    private static final String EVENT_TYPE_PROCESS = "evTypeProc";
    private static final String ALL = "All";
    private static final String EVENT_ID_PROCESS = "evIdProc";
    private static final String CONTEXT_ID = "ContextID";
    private static final String CONTEXT_NAME = "ContextName";
    private static final String CONTRACT_ID = "ContractID";
    private static final String CONTRACT_NAME = "ContractName";
    private static final String AGENCY_ID = "AgencyID";
    private static final String AGENCY_NAME = "AgencyName";
    private static final String PROFILE_ID = "ProfileID";
    private static final String PROFILE_IDENTIFIER = "ProfileIdentifier";
    private static final String PROFILE_NAME = "ProfileName";
    private static final String ARCHIVE_UNIT_PROFILE_ID = "ArchiveUnitProfileID";
    private static final String ARCHIVE_UNIT_PROFILE_IDENTIFIER = "ArchiveUnitProfileIdentifier";
    private static final String ARCHIVE_UNIT_PROFILE_NAME = "ArchiveUnitProfileName";
    private static final String RULE_CATEGORY = "RuleCategory";
    private static final String RULE_DATE_SUP = "RuleDateSup";
    private static final String RULE_FINAL_ACTION = "RuleFinalAction";
    private static final String ONTOLOGY_TYPE = "OntologyType";
    private static final String ONTOLOGY_NAME = "OntologyName";
    private static final String ONTOLOGY_ID = "OntologyID";
    private static final String ORIGINATING_AGENCY_TAG = "#originating_agency";
    private static final String ELIMINATION_DESTROYABLE_ORIGINATING_AGENCY_TAG =
        "#elimination.DestroyableOriginatingAgencies";
    private static final String ELIMINATION_NON_DESTROYABLE_ORIGINATING_AGENCY_TAG =
        "#elimination.NonDestroyableOriginatingAgencies";
    private static final String ELIMINATION_GLOBAL_STATUS_TAG = "#elimination.GlobalStatus";
    private static final String ELIMINATION_EXTENDED_INFO_TYPE_TAG = "#elimination.ExtendedInfo.ExtendedInfoType";

    private static final String DESCRIPTION_LEVEL_TAG = "DescriptionLevel";
    private static final String DESCRIPTION = "Description";
    private static final String ELIMINATION_OPERATION_ID = "EliminationOperationId";
    private static final String TITLE = "Title";
    private static final String TITLE_FR = "Title_.fr";
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
    private static final String UNITUPS = "UNITUPS";
    private static final String ROOTS = "ROOTS";
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
    private static final String ORIGINATING_AGENCIES = "OriginatingAgencies";
    private static final String DATEOPERATION = "EvDateTime";
    private static final String TRACEABILITY_OK = "TraceabilityOk";
    private static final String TRACEABILITY_ID = "TraceabilityId";
    private static final String TRACEABILITY_LOG_TYPE = "TraceabilityLogType";
    private static final String TRACEABILITY_START_DATE = "TraceabilityStartDate";
    private static final String TRACEABILITY_END_DATE = "TraceabilityEndDate";
    private static final String TRACEABILITY_EV_DET_DATA = "events.evDetData";
    private static final String TRACEABILITY_FIELD_ID = "evId";
    private static final String TRACEABILITY_FIELD_LOG_TYPE = "LogType";
    private static final String MANAGEMENT_KEY = "#management";
    private static final String INGEST_START_DATE = "IngestStartDate";
    private static final String INGEST_END_DATE = "IngestEndDate";
    private static final String FACETS_PREFIX = "facets";

    private static final String ASC_SORT_TYPE = "ASC";
    private static final String SORT_TYPE_ENTRY = "sortType";
    private static final String SORT_FIELD_ENTRY = "field";
    private static final String REQUEST_FACET_PREFIX = "requestFacet";
    private static final String EXISTS = "$exists";
    private static final String MISSING = "$missing";
    private static final String LANGUAGE = "Language";

    private static final String FACET_WITH_OBJECT = "FacetWithObject";
    private static final String FACET_WITHOUT_OBJECT = "FacetWithoutObject";

    private static final String GOT_FILE_FORMAT_ID = "fileFormatId";
    private static final String GOT_FILE_USAGE = "fileUsage";
    private static final String GOT_FILE_SIZE = "fileSize";
    private static final String GOT_FILE_SIZE_OPERATOR = "fileSizeOperator";
    private static final String GOT_FILE_SIZE_OPERATOR_LOWER = "<";
    private static final String GOT_FILE_SIZE_OPERATOR_GREATER_OR_EQUAL = ">=";
    private static final String FILE_FORMAT_ID_GOT_FIELD = "#qualifiers.versions.FormatIdentification.FormatId";
    private static final String FILE_USAGE_GOT_FIELD = "#qualifiers.versions.DataObjectVersion";
    private static final String FILE_SIZE_GOT_FIELD = "#qualifiers.versions.Size";
    private static final String SCENARIO_NAME = "ScenarioName";
    private static final String GRIFFIN_NAME = "GriffinName";
    private static final String SCENARIO_ID = "ScenarioID";
    private static final String GRIFFIN_ID = "GriffinID";

    private static final DslQueryHelper instance = new DslQueryHelper();

    public static DslQueryHelper getInstance() {
        return instance;
    }

    public DslQueryHelper() {}

    /**
     * generate the DSL query after receiving the search criteria
     *
     * @param searchCriteriaMap the map containing the criteria
     * @return DSL request
     * @throws InvalidParseOperationException if a parse exception is encountered
     * @throws InvalidCreateOperationException if an Invalid create operation is encountered
     */
    public JsonNode createSingleQueryDSL(Map<String, Object> searchCriteriaMap)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final fr.gouv.vitam.common.database.builder.request.single.Select select =
            new fr.gouv.vitam.common.database.builder.request.single.Select();
        final BooleanQuery query = and();
        BooleanQuery queryOr = null;

        for (final Entry<String, Object> entry : searchCriteriaMap.entrySet()) {
            final String searchKeys = entry.getKey();

            if (ORDER_BY.equalsIgnoreCase(searchKeys)) {
                Map<String, String> sortSetting = (Map<String, String>) entry.getValue();
                String sortField = sortSetting.get(SORT_FIELD_ENTRY);
                String sortType = sortSetting.get(SORT_TYPE_ENTRY);

                String realSortField = sortField;
                switch (sortField) {
                    case AGENCY_NAME:
                    case CONTRACT_NAME:
                    case PROFILE_NAME:
                    case ARCHIVE_UNIT_PROFILE_NAME:
                    case ONTOLOGY_NAME:
                        realSortField = "ApiField";
                        break;
                    case PROFILE_IDENTIFIER:
                    case ARCHIVE_UNIT_PROFILE_IDENTIFIER:
                        realSortField = "Identifier";
                        break;
                    default:
                }

                if (ASC_SORT_TYPE.equalsIgnoreCase(sortType)) {
                    select.addOrderByAscFilter(realSortField);
                } else {
                    select.addOrderByDescFilter(realSortField);
                }
            } else {
                final String searchValue = (String) entry.getValue();
                switch (searchKeys) {
                    case DEFAULT_EVENT_TYPE_PROCESS:
                        query.add(
                            or()
                                .add(
                                    eq(EVENT_TYPE_PROCESS, DEFAULT_EVENT_TYPE_PROCESS),
                                    eq(EVENT_TYPE_PROCESS, DEFAULT_EVENT_TYPE_PROCESS_TEST)
                                )
                        );
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
                            InQuery checkStatus = in(
                                EVENT_OUT_DETAIL,
                                "STP_OP_SECURISATION.OK",
                                "STP_STORAGE_SECURISATION.OK",
                                "STP_STORAGE_SECURISATION.WARNING",
                                "LOGBOOK_UNIT_LFC_TRACEABILITY.OK",
                                "LOGBOOK_UNIT_LFC_TRACEABILITY.WARNING",
                                "LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY.OK",
                                "LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY.WARNING"
                            );
                            ExistsQuery hasFilename = exists("events.evDetData.FileName");
                            query.add(checkStatus).add(hasFilename);
                        }
                        break;
                    case TRACEABILITY_ID:
                        if (!searchValue.isEmpty()) {
                            // Actually evID
                            query.add(eq(TRACEABILITY_FIELD_ID, searchValue));
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
                    case INGEST_START_DATE:
                        if (!searchValue.isEmpty()) {
                            query.add(gte(EVENT_DATE_TIME, searchValue));
                        }
                        break;
                    case INGEST_END_DATE:
                        if (!searchValue.isEmpty()) {
                            query.add(lte(EVENT_DATE_TIME, searchValue));
                        }
                        break;
                    case ONTOLOGY_TYPE:
                        if ("all".equals(searchValue)) {
                            query.add(exists("Type"));
                        } else if (!searchValue.isEmpty()) {
                            query.add(match("Type", searchValue));
                        }
                    case AGENCY_NAME:
                    case CONTEXT_NAME:
                    case CONTRACT_NAME:
                    case PROFILE_NAME:
                    case SCENARIO_NAME:
                    case GRIFFIN_NAME:
                    case ARCHIVE_UNIT_PROFILE_NAME:
                        if ("all".equals(searchValue)) {
                            query.add(exists("Name"));
                        } else if (!searchValue.isEmpty()) {
                            query.add(match("Name", searchValue));
                        }
                        break;
                    case ONTOLOGY_NAME:
                        if ("all".equals(searchValue)) {
                            query.add(or().add(exists("ApiField")).add(exists("SedaField")));
                        } else if (!searchValue.isEmpty()) {
                            query.add(or().add(eq("ApiField", searchValue)).add(eq("SedaField", searchValue)));
                        }
                        break;
                    case AGENCY_ID:
                    case CONTRACT_ID:
                    case CONTEXT_ID:
                    case PROFILE_ID:
                    case SCENARIO_ID:
                    case GRIFFIN_ID:
                    case ARCHIVE_UNIT_PROFILE_ID:
                    case ONTOLOGY_ID:
                        if (!"all".equals(searchValue)) {
                            query.add(eq("Identifier", searchValue));
                        }
                        break;
                    case PROFILE_IDENTIFIER:
                    case ARCHIVE_UNIT_PROFILE_IDENTIFIER:
                        if ("all".equals(searchValue)) {
                            query.add(exists("Identifier"));
                        } else if (!searchValue.isEmpty()) {
                            query.add(match("Identifier", searchValue));
                        }
                        break;
                    default:
                        if (!searchValue.isEmpty()) {
                            query.add(eq(searchKeys, searchValue));
                        }
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

    public JsonNode createSelectDSLQuery(Map<String, String> searchCriteriaMap)
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
            boolean noRoots = select.getRoots() == null || select.getRoots().isEmpty();
            // do no set depth when no root and first query
            if (!(noRoots && select.getNbQueries() == 0)) {
                booleanQueries.setDepthLimit(DEPTH_LIMIT);
            }
            select.addQueries(booleanQueries);
        }

        return select.getFinalSelect();
    }

    /**
     * Create GetById Select Multiple Query Dsl request that contains only projection.
     *
     * @param projectionCriteriaMap the given projection parameters
     * @return request with projection
     * @throws InvalidParseOperationException null key or value parameters
     * @throws InvalidCreateOperationException queryDsl create operation
     */
    public JsonNode createGetByIdDSLSelectMultipleQuery(Map<String, String> projectionCriteriaMap)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectMultiQuery select = new SelectMultiQuery();

        for (final Entry<String, String> entry : projectionCriteriaMap.entrySet()) {
            final String searchKeys = entry.getKey();
            final String searchValue = entry.getValue();

            if (searchKeys.isEmpty() || searchValue.isEmpty()) {
                throw new InvalidParseOperationException("Parameters should not be empty or null");
            }

            // Add projection for fields prefixed by projection_
            if (searchKeys.startsWith(PROJECTION_PREFIX)) {
                select.addUsedProjection(searchValue);
            }
        }
        // Suppress 3 parts of queryDsl for validation in getById only Projection is authorize
        ObjectNode finalSelect = select.getFinalSelectById();
        return finalSelect;
    }

    /**
     * @param searchCriteriaMap Criteria received from The IHM screen Empty Keys or Value is not allowed
     * @return the JSONDSL File
     * @throws InvalidParseOperationException thrown when an error occurred during parsing
     * @throws InvalidCreateOperationException thrown when an error occurred during creation
     */
    public JsonNode createSelectElasticsearchDSLQuery(Map<String, Object> searchCriteriaMap)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectMultiQuery select = new SelectMultiQuery();
        final BooleanQuery andQuery = and();
        BooleanQuery nestedSubQuery = null;
        final BooleanQuery booleanQueries = or();
        boolean advancedFacetQuery = false;
        String startDate = null;
        String endDate = null;
        String advancedSearchFlag = "";
        String ruleCategory = null;
        String ruleFinalAction = null;
        String ruleEndDate = null;
        String fileSize = null;
        String fileSizeOperator = null;

        for (final Entry<String, Object> entry : searchCriteriaMap.entrySet()) {
            final String searchKeys = entry.getKey();
            final Object searchValue = entry.getValue();

            if (searchKeys.isEmpty() || searchValue == null) {
                throw new InvalidParseOperationException("Parameters should not be empty or null");
            }

            // Add projection for fields prefixed by projection_
            if (searchKeys.startsWith(PROJECTION_PREFIX)) {
                select.addUsedProjection((String) searchValue);
                continue;
            }

            // add facets
            if (searchKeys.startsWith(FACETS_PREFIX)) {
                List<FacetItem> facetSettings = (List<FacetItem>) searchValue;
                for (int i = 0; i < facetSettings.size(); i++) {
                    FacetItem facetItem = JsonHandler.getFromString(
                        JsonHandler.writeAsString(facetSettings.get(i)),
                        FacetItem.class
                    );

                    if (facetItem.getFacetType() != null) {
                        switch (facetItem.getFacetType()) {
                            case TERMS:
                                select.addFacets(
                                    new TermsFacet(
                                        facetItem.getName(),
                                        facetItem.getField(),
                                        facetItem.getSubobject(),
                                        facetItem.getSize(),
                                        facetItem.getOrder()
                                    )
                                );
                                break;
                            case DATE_RANGE:
                                List<RangeFacetValue> ranges = facetItem
                                    .getRanges()
                                    .stream()
                                    .map(range -> new RangeFacetValue(range.getDateMin(), range.getDateMax()))
                                    .collect(Collectors.toList());

                                select.addFacets(
                                    new DateRangeFacet(
                                        facetItem.getName(),
                                        facetItem.getField(),
                                        facetItem.getSubobject(),
                                        facetItem.getFormat(),
                                        ranges
                                    )
                                );
                                break;
                            case FILTERS:
                                Map<String, Query> filters = new HashMap<>();
                                facetItem
                                    .getFilters()
                                    .forEach(filter -> {
                                        if (filter.getQuery().get(EXISTS) != null) {
                                            try {
                                                filters.put(
                                                    filter.getName(),
                                                    QueryHelper.exists(filter.getQuery().get(EXISTS).asText())
                                                );
                                            } catch (InvalidCreateOperationException e) {
                                                LOGGER.error(e);
                                            }
                                        } else if (filter.getQuery().get(MISSING) != null) {
                                            try {
                                                filters.put(
                                                    filter.getName(),
                                                    QueryHelper.missing(filter.getQuery().get(MISSING).asText())
                                                );
                                            } catch (InvalidCreateOperationException e) {
                                                LOGGER.error(e);
                                            }
                                        }
                                    });
                                select.addFacets(new FiltersFacet(facetItem.getName(), filters));
                                break;
                            default:
                                break;
                        }
                    }
                }
                continue;
            }

            // Add order by
            if (searchKeys.equals(ORDER_BY)) {
                Map<String, String> sortSetting = (Map<String, String>) searchValue;
                String sortField = sortSetting.get(SORT_FIELD_ENTRY);
                String sortType = sortSetting.get(SORT_TYPE_ENTRY);

                if (ASC_SORT_TYPE.equalsIgnoreCase(sortType)) {
                    select.addOrderByAscFilter(sortField);
                } else {
                    select.addOrderByDescFilter(sortField);
                }
                continue;
            }

            // Add root
            if (searchKeys.equals(UiConstants.SELECT_BY_ID.toString())) {
                select.addRoots((String) searchValue);
                continue;
            }

            // Look for a child of this node
            if (searchKeys.equalsIgnoreCase(UNITUPS)) {
                andQuery.add(in(UiConstants.UNITUPS.getResultCriteria(), (String) searchValue));
                continue;
            }

            // ADD_ROOT
            if (searchKeys.equalsIgnoreCase(ROOTS)) {
                List<String> list = (List) searchValue;
                String[] roots = list.toArray(new String[list.size()]);
                select.addRoots(roots);
                continue;
            }

            if (searchKeys.equals(TITLE_AND_DESCRIPTION)) {
                booleanQueries.add(match(TITLE, (String) searchValue));
                booleanQueries.add(match(TITLE_FR, (String) searchValue));
                booleanQueries.add(match(DESCRIPTION, (String) searchValue));
                continue;
            }
            if (searchKeys.equalsIgnoreCase(UiConstants.ID.getReceivedCriteria())) {
                BooleanQuery idQuery = or().add(eq(UiConstants.ID.getResultCriteria(), (String) searchValue));
                idQuery.add(eq("FilePlanPosition", (String) searchValue));
                idQuery.add(eq("OriginatingSystemId", (String) searchValue));
                idQuery.add(eq("OriginatingAgencySystemId", (String) searchValue));
                idQuery.add(eq("TransferringAgencySystemId", (String) searchValue));
                idQuery.add(eq("ArchivalAgencySystemId", (String) searchValue));

                andQuery.add(idQuery);
                continue;
            }
            if (searchKeys.equals(ELIMINATION_OPERATION_ID)) {
                andQuery.add(eq(VitamFieldsHelper.elimination() + ".OperationId", (String) searchValue));
                continue;
            }
            if (searchKeys.equalsIgnoreCase(TITLE)) {
                andQuery.add(or().add(match(TITLE, (String) searchValue)).add(match(TITLE_FR, (String) searchValue)));
                continue;
            }
            if (searchKeys.equalsIgnoreCase(DESCRIPTION)) {
                andQuery.add(match(DESCRIPTION, (String) searchValue));
                continue;
            }
            if (searchKeys.equalsIgnoreCase(ORIGINATING_AGENCY)) {
                andQuery.add(eq(VitamFieldsHelper.originatingAgency(), (String) searchValue));
                continue;
            }
            if (searchKeys.equalsIgnoreCase(ORIGINATING_AGENCIES)) {
                andQuery.add(eq(VitamFieldsHelper.originatingAgencies(), (String) searchValue));
                continue;
            }

            if (searchKeys.equalsIgnoreCase(GOT_FILE_FORMAT_ID)) {
                if (nestedSubQuery == null) {
                    nestedSubQuery = and();
                }
                nestedSubQuery.add(eq(FILE_FORMAT_ID_GOT_FIELD, (String) searchValue));
                continue;
            }
            if (searchKeys.equalsIgnoreCase(GOT_FILE_USAGE)) {
                if (nestedSubQuery == null) {
                    nestedSubQuery = and();
                }
                nestedSubQuery.add(eq(FILE_USAGE_GOT_FIELD, (String) searchValue));
                continue;
            }
            if (searchKeys.equalsIgnoreCase(GOT_FILE_SIZE)) {
                fileSize = (String) searchValue;
                if (fileSizeOperator != null) {
                    if (fileSizeOperator.equals(GOT_FILE_SIZE_OPERATOR_LOWER)) {
                        if (nestedSubQuery == null) {
                            nestedSubQuery = and();
                        }
                        nestedSubQuery.add(lt(FILE_SIZE_GOT_FIELD, (String) searchValue));
                    } else if (fileSizeOperator.equals(GOT_FILE_SIZE_OPERATOR_GREATER_OR_EQUAL)) {
                        if (nestedSubQuery == null) {
                            nestedSubQuery = and();
                        }
                        nestedSubQuery.add(gte(FILE_SIZE_GOT_FIELD, (String) searchValue));
                    }
                }
                continue;
            }
            if (searchKeys.equalsIgnoreCase(GOT_FILE_SIZE_OPERATOR)) {
                fileSizeOperator = (String) searchValue;
                if (fileSize != null) {
                    if (fileSizeOperator.equals(GOT_FILE_SIZE_OPERATOR_LOWER)) {
                        if (nestedSubQuery == null) {
                            nestedSubQuery = and();
                        }
                        nestedSubQuery.add(lt(FILE_SIZE_GOT_FIELD, fileSize));
                    } else if (fileSizeOperator.equals(GOT_FILE_SIZE_OPERATOR_GREATER_OR_EQUAL)) {
                        if (nestedSubQuery == null) {
                            nestedSubQuery = and();
                        }
                        nestedSubQuery.add(gte(FILE_SIZE_GOT_FIELD, fileSize));
                    }
                }
                continue;
            }

            if (searchKeys.equalsIgnoreCase(RULE_CATEGORY)) {
                ruleCategory = (String) searchValue;
            }

            if (searchKeys.equalsIgnoreCase(RULE_DATE_SUP)) {
                ruleEndDate = (String) searchValue;
            }

            if (searchKeys.equalsIgnoreCase(RULE_FINAL_ACTION)) {
                ruleFinalAction = (String) searchValue;
            }

            if (searchKeys.startsWith(START_PREFIX)) {
                startDate = (String) searchValue;
                continue;
            }
            if (searchKeys.startsWith(END_PREFIX)) {
                endDate = (String) searchValue;
                continue;
            }
            if (searchKeys.equalsIgnoreCase(ADVANCED_SEARCH_FLAG)) {
                advancedSearchFlag = (String) searchValue;
                continue;
            }

            if (searchKeys.equalsIgnoreCase(REQUEST_FACET_PREFIX)) {
                RequestFacetItem requestFacetItem = JsonHandler.getFromString(
                    JsonHandler.writeAsString(searchValue),
                    RequestFacetItem.class
                );
                if (
                    requestFacetItem != null &&
                    requestFacetItem.getField() != null &&
                    requestFacetItem.getValue() != null
                ) {
                    switch (requestFacetItem.getField()) {
                        case ELIMINATION_DESTROYABLE_ORIGINATING_AGENCY_TAG:
                        case ELIMINATION_NON_DESTROYABLE_ORIGINATING_AGENCY_TAG:
                        case ELIMINATION_GLOBAL_STATUS_TAG:
                        case ELIMINATION_EXTENDED_INFO_TYPE_TAG:
                        case ORIGINATING_AGENCY_TAG:
                        case DESCRIPTION_LEVEL_TAG:
                            advancedFacetQuery = true;
                            andQuery.add(eq(requestFacetItem.getField(), requestFacetItem.getValue()));
                            break;
                        case START_DATE:
                        case END_DATE:
                            final String[] values = requestFacetItem.getValue().split("-");
                            if (values.length == 2) {
                                advancedFacetQuery = true;
                                andQuery.add(
                                    gte(requestFacetItem.getField(), values[0]),
                                    lte(requestFacetItem.getField(), values[1])
                                );
                            }
                            break;
                        default:
                            if (
                                requestFacetItem.getField().startsWith(TITLE) ||
                                requestFacetItem.getField().startsWith(DESCRIPTION)
                            ) {
                                andQuery.add(exists(requestFacetItem.getField()));
                            }
                            if (requestFacetItem.getField().startsWith(LANGUAGE)) {
                                andQuery.add(eq(requestFacetItem.getField(), requestFacetItem.getValue()));
                            }
                            if (requestFacetItem.getValue().equalsIgnoreCase(FACET_WITH_OBJECT)) {
                                andQuery.add(exists(requestFacetItem.getField()));
                            } else if (requestFacetItem.getValue().equalsIgnoreCase(FACET_WITHOUT_OBJECT)) {
                                andQuery.add(missing(requestFacetItem.getField()));
                            }
                            advancedFacetQuery = true;
                            break;
                    }
                    continue;
                }
            }

            if (searchKeys.equalsIgnoreCase("fieldArchiveUnit") || searchKeys.equalsIgnoreCase("valueArchiveUnit")) {
                continue;
            }

            // By default add equals query
            booleanQueries.add(match(searchKeys, (String) searchValue));
        }

        if (searchCriteriaMap.get("fieldArchiveUnit") != null) {
            andQuery.add(
                eq(
                    ((String) searchCriteriaMap.get("fieldArchiveUnit")),
                    ((String) searchCriteriaMap.get("valueArchiveUnit"))
                )
            );
        }

        // US 509:start AND end date must be filled.
        if (!Strings.isNullOrEmpty(endDate) && !Strings.isNullOrEmpty(startDate)) {
            andQuery.add(createSearchUntisQueryByDate(startDate, endDate));
        }

        if (!Strings.isNullOrEmpty(ruleCategory)) {
            String managmentRuleCategory = VitamFieldsHelper.management() + "." + ruleCategory;
            if (!Strings.isNullOrEmpty(ruleFinalAction)) {
                andQuery.add(eq(managmentRuleCategory + ".FinalAction", ruleFinalAction));
            }

            if (!Strings.isNullOrEmpty(ruleEndDate)) {
                andQuery.add(lte(managmentRuleCategory + ".Rules.EndDate", ruleEndDate));
            }
        }

        if (nestedSubQuery != null) {
            andQuery.add(nestedSearch("#qualifiers.versions", nestedSubQuery.getCurrentQuery()));
        }

        boolean noRoots = select.getRoots() == null || select.getRoots().isEmpty();

        if (advancedFacetQuery) {
            if (booleanQueries.isReady()) {
                // do no set depth when no root and first query
                if (!(noRoots && select.getNbQueries() == 0)) {
                    booleanQueries.setDepthLimit(DEPTH_LIMIT);
                }
                andQuery.add(booleanQueries);
            }
            if (andQuery.isReady()) {
                // do no set depth when no root and first query
                if (!(noRoots && select.getNbQueries() == 0)) {
                    andQuery.setDepthLimit(DEPTH_LIMIT);
                }
                select.addQueries(andQuery);
            }
        } else {
            if (advancedSearchFlag.equalsIgnoreCase(YES)) {
                if (andQuery.isReady()) {
                    // do no set depth when no root and first query
                    if (!(noRoots && select.getNbQueries() == 0)) {
                        andQuery.setDepthLimit(DEPTH_LIMIT);
                    }
                    select.addQueries(andQuery);
                }
            } else {
                if (booleanQueries.isReady()) {
                    // do no set depth when no root and first query
                    if (!(noRoots && select.getNbQueries() == 0)) {
                        booleanQueries.setDepthLimit(DEPTH_LIMIT);
                    }
                    select.addQueries(booleanQueries);
                }
            }
        }

        return select.getFinalSelect();
    }

    /**
     * @param searchCriteriaMap Criteria received from The IHM screen Empty Keys or Value is not allowed
     * @param updateRules rules that must be updated in the AU.
     * @return the JSONDSL File
     * @throws InvalidParseOperationException thrown when an error occurred during parsing
     * @throws InvalidCreateOperationException thrown when an error occurred during creation
     */
    public JsonNode createUpdateByIdDSLQuery(
        Map<String, JsonNode> searchCriteriaMap,
        Map<String, JsonNode> updateRules
    ) throws InvalidParseOperationException, InvalidCreateOperationException {
        final UpdateMultiQuery update = new UpdateMultiQuery();

        for (final Entry<String, JsonNode> entry : searchCriteriaMap.entrySet()) {
            final String searchKeys = entry.getKey();
            final JsonNode searchValue = entry.getValue();

            if (searchKeys.isEmpty()) {
                throw new InvalidParseOperationException("Parameters should not be empty or null");
            }
            // Add Actions
            Map<String, JsonNode> action = new HashMap<>();
            action.put(searchKeys, searchValue);
            update.addActions(new SetAction(action));
        }

        for (final Entry<String, JsonNode> categoryRule : updateRules.entrySet()) {
            final String categoryKey = categoryRule.getKey();
            final JsonNode categoryRules = categoryRule.getValue();

            if (categoryRules.isEmpty(new DefaultSerializerProvider.Impl())) {
                update.addActions(new UnsetAction(MANAGEMENT_KEY + '.' + categoryKey));
            } else {
                Map<String, JsonNode> action = new HashMap<>();
                action.put(MANAGEMENT_KEY + '.' + categoryKey, categoryRules);
                update.addActions(new SetAction(action));
            }
        }
        return update.getFinalUpdateById();
    }

    public ObjectNode createMassiveUpdateDSLBaseQuery(JsonNode modifiedFields) {
        JsonNode query = modifiedFields.get("query");
        JsonNode threshold = modifiedFields.get("threshold");

        ObjectNode fullQuery = JsonHandler.createObjectNode();
        fullQuery.set(BuilderToken.GLOBAL.ROOTS.exactToken(), JsonHandler.createArrayNode());
        fullQuery.set(BuilderToken.GLOBAL.QUERY.exactToken(), query.get(BuilderToken.GLOBAL.QUERY.exactToken()));
        if (threshold != null && threshold.longValue() != 0L) {
            fullQuery.set(BuilderToken.GLOBAL.THRESOLD.exactToken(), threshold);
        }

        return fullQuery;
    }

    public UpdateMultiQuery getFullMetadataActionQuery(JsonNode metadataModifications)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final UpdateMultiQuery update = new UpdateMultiQuery();

        if (metadataModifications == null) {
            return null;
        }

        // Handle pattern Updates on metadata
        JsonNode metadataPatterns = metadataModifications.get("patterns");
        for (final JsonNode modifiedField : metadataPatterns) {
            String fieldName = modifiedField.get("FieldName").textValue();
            String patternControll = modifiedField.get("FieldValue").textValue();
            String patternUpdate = modifiedField.get("FieldPattern").textValue();
            if (fieldName == null || patternControll == null || patternUpdate == null) {
                throw new InvalidParseOperationException("Parameters should not be empty or null");
            }

            ObjectNode actionNode = JsonHandler.createObjectNode();
            actionNode.set("$target", modifiedField.get("FieldName"));
            actionNode.set("$controlPattern", modifiedField.get("FieldPattern"));
            actionNode.set("$updatePattern", modifiedField.get("FieldValue"));
            update.addActions(new SetregexAction(actionNode));
        }

        // Handle other Updates on metadata:
        JsonNode metadataUpdates = metadataModifications.get("updates");
        for (final JsonNode modifiedField : metadataUpdates) {
            String fieldName = modifiedField.get("FieldName").textValue();
            JsonNode fieldValue = modifiedField.get("FieldValue");
            if (fieldName == null) {
                throw new InvalidParseOperationException("Parameters should not be empty or null");
            }

            // Add Actions
            Map<String, JsonNode> action = new HashMap<>();
            action.put(fieldName, fieldValue);
            update.addActions(new SetAction(action));
        }

        // Handle Deletions on metadata
        JsonNode metadataDeletions = metadataModifications.get("deletions");
        for (final JsonNode deletedField : metadataDeletions) {
            String fieldName = deletedField.get("FieldName").textValue();
            if (fieldName == null) {
                throw new InvalidParseOperationException("Parameters should not be empty or null");
            }

            // Add Actions
            Map<String, JsonNode> action = new HashMap<>();
            action.put(fieldName, new TextNode(""));
            update.addActions(new SetAction(action));
        }

        return update;
    }

    private BooleanQuery createSearchUntisQueryByDate(String startDate, String endDate)
        throws InvalidCreateOperationException {
        LOGGER.debug("in createSearchUntisQueryByDate / beginDate:" + startDate + "/ endDate:" + endDate);

        final BooleanQuery query = or();

        if (!Strings.isNullOrEmpty(endDate) && !Strings.isNullOrEmpty(startDate)) {
            final BooleanQuery transactedDateBetween = and();
            // search by transacted date
            transactedDateBetween.add(gte(TRANSACTED_DATE, startDate));
            transactedDateBetween.add(lte(TRANSACTED_DATE, endDate));
            // search by begin and end date
            final BooleanQuery queryAroundDate = and();
            queryAroundDate.add(gte(END_DATE, startDate));
            queryAroundDate.add(lte(START_DATE, endDate));
            query.add(transactedDateBetween, queryAroundDate);
        }
        LOGGER.debug("in createSearchUntisQueryByDate / query:" + query.toString());
        return query;
    }

    public JsonNode createSearchQueryAccessionRegister(Map<String, Object> options)
        throws InvalidCreateOperationException {
        String startDate = (String) options.get("startDate");
        String endDate = (String) options.get("endDate");
        String originatingAgency = (String) options.get("OriginatingAgency");

        Date from = Date.from(LocalDateUtil.parse(startDate, ISO_OFFSET_DATE_TIME).toInstant(UTC));
        Date to = Date.from(LocalDateUtil.parse(endDate, ISO_OFFSET_DATE_TIME).toInstant(UTC));
        RangeQuery range = QueryHelper.range("CreationDate", from, true, to, true);

        CompareQuery eqOriginatingAgency = eq("OriginatingAgency", originatingAgency);

        Select select = new Select();
        select.setQuery(and().add(eqOriginatingAgency, range));

        return select.getFinalSelect();
    }

    /**
     * Create a JsonNode similar to a composed Select/Update DSL query<br/>
     * Input: {parentId: 'id', childId: 'id', action: 'ADD'} (action can be DELETE)<br/>
     * Output:
     * [{
     * "$query": [
     * {
     * "$eq": {
     * "#id": "childId"
     * }
     * }
     * ],
     * "$action": [
     * {
     * "$add": { (action can be $pull if input ask for DELETE)
     * "#up": ["parentId"]
     * }
     * }
     * ]
     * }]
     *
     * @param optionsMap input options given by frontend application
     * @return jsonQuery for adminClient
     */
    public JsonNode createSelectAndUpdateDSLQuery(Map<String, Object> optionsMap) {
        // Select part
        ArrayNode queryArray = JsonHandler.createArrayNode();
        ObjectNode query = JsonHandler.createObjectNode();
        ObjectNode eqQuery = JsonHandler.createObjectNode();
        eqQuery.set("#id", new TextNode((String) optionsMap.get("childId")));
        query.set("$eq", eqQuery);
        queryArray.add(query);

        // Update part
        ArrayNode actions = JsonHandler.createArrayNode();
        ObjectNode action = JsonHandler.createObjectNode();
        ObjectNode actionDetails = JsonHandler.createObjectNode();
        ArrayNode actionIds = JsonHandler.createArrayNode();

        String actionType;
        switch ((String) optionsMap.get("action")) {
            case "ADD":
                actionType = "$add";
                break;
            case "DELETE":
                actionType = "$pull";
                break;
            default:
                // TODO throw error ?
                return null;
        }

        actionIds.add((String) optionsMap.get("parentId"));
        actionDetails.set("#unitups", actionIds);
        action.set(actionType, actionDetails);
        actions.add(action);

        // Full query
        ArrayNode queries = JsonHandler.createArrayNode();
        ObjectNode finalQuery = JsonHandler.createObjectNode();

        finalQuery.set("$roots", JsonHandler.createArrayNode());
        finalQuery.set("$query", queryArray);
        finalQuery.set("$action", actions);
        queries.add(finalQuery);

        return queries;
    }
}
