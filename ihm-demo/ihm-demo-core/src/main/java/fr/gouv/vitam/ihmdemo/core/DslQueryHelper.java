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

package fr.gouv.vitam.ihmdemo.core;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.Select;
import fr.gouv.vitam.common.database.builder.request.multiple.Update;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Helper class to create DSL queries
 * 
 */
public final class DslQueryHelper {
    
    public static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DslQueryHelper.class);
    // TODO: faire en sorte que LogbookMongoDbName ait une version publique "#qqc" (comme #id) pour permettre de
    // "masquer" l'implémentation.
    private static final String EVENT_TYPE_PROCESS = "evTypeProc";
    private static final String DESCRIPTION = "Description";
    private static final String TITLE = "Title";
    private static final String EVENT_DATE_TIME = "evDateTime";
    private static final String DEFAULT_EVENT_TYPE_PROCESS = "INGEST";
    private static final String PUID = "PUID";
    private static final String OBJECT_IDENTIFIER_INCOME = "obIdIn";
    private static final String FORMAT = "FORMAT";
    private static final String ORDER_BY = "orderby";
    private static final String TITLE_AND_DESCRIPTION = "titleAndDescription";
    private static final String PROJECTION_PREFIX = "projection_";
    private static final int DEPTH_LIMIT = 20;

    /**
     * generate the DSL query after receiving the search criteria
     * 
     * 
     * @param searchCriteriaMap the map containing the criteria
     * @return DSL request
     * @throws InvalidParseOperationException if a parse exception is encountered
     * @throws InvalidCreateOperationException if an Invalid create operation is encountered
     */
    public static String createSingleQueryDSL(Map<String, String> searchCriteriaMap)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final fr.gouv.vitam.common.database.builder.request.single.Select select = new fr.gouv.vitam.common.database.builder.request.single.Select();
        BooleanQuery query = and();
        for (Entry<String, String> entry : searchCriteriaMap.entrySet()) {
            String searchKeys = entry.getKey();
            String searchValue = entry.getValue();

            switch (searchKeys) {
                case ORDER_BY:
                    if (EVENT_DATE_TIME.equals(searchValue)) {
                        select.addOrderByDescFilter(searchValue);
                    } else {
                        select.addOrderByAscFilter(searchValue);
                    }
                    break;

                case DEFAULT_EVENT_TYPE_PROCESS:
                    query.add(eq(EVENT_TYPE_PROCESS, DEFAULT_EVENT_TYPE_PROCESS));
                    break;

                case OBJECT_IDENTIFIER_INCOME:
                    query.add(eq("events.obIdIn", searchValue));
                    break;

                case FORMAT:
                    query.add(exists(PUID));                    
                    break;    

                default:
                    if (!searchValue.isEmpty()) {
                        query.add(eq(searchKeys, searchValue));
                    }
            }
        }

        select.setQuery(query);
        LOGGER.info(select.getFinalSelect().toString());
        return select.getFinalSelect().toString();
    }

    /**
     * @param searchCriteriaMap Criteria received from The IHM screen Empty Keys or Value is not allowed
     * @return the JSONDSL File
     * @throws InvalidParseOperationException thrown when an error occurred during parsing
     * @throws InvalidCreateOperationException thrown when an error occurred during creation
     */
    public static String createSelectDSLQuery(Map<String, String> searchCriteriaMap)
        throws InvalidParseOperationException, InvalidCreateOperationException {

        final Select select = new Select();

        // AND by default
        BooleanQuery booleanQueries = and();
        for (Entry<String, String> entry : searchCriteriaMap.entrySet()) {
            String searchKeys = entry.getKey();
            String searchValue = entry.getValue();

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

        return select.getFinalSelect().toString();
    }
    /**
     * @param searchCriteriaMap Criteria received from The IHM screen Empty Keys or Value is not allowed
     * @return the JSONDSL File
     * @throws InvalidParseOperationException thrown when an error occurred during parsing
     * @throws InvalidCreateOperationException thrown when an error occurred during creation
     */
    public static String createSelectElasticsearchDSLQuery(Map<String, String> searchCriteriaMap)
        throws InvalidParseOperationException, InvalidCreateOperationException {

        final Select select = new Select();

        BooleanQuery booleanQueries = or();
        for (Entry<String, String> entry : searchCriteriaMap.entrySet()) {
            String searchKeys = entry.getKey();
            String searchValue = entry.getValue();

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
            
            // By default add equals query
            booleanQueries.add(match(searchKeys, searchValue));
        }
        
        
        if (booleanQueries.isReady()) {
            booleanQueries.setDepthLimit(DEPTH_LIMIT);
            select.addQueries(booleanQueries);
        }

        return select.getFinalSelect().toString();
    }

    /**
     * @param searchCriteriaMap Criteria received from The IHM screen Empty Keys or Value is not allowed
     * @return the JSONDSL File
     * @throws InvalidParseOperationException thrown when an error occurred during parsing
     * @throws InvalidCreateOperationException thrown when an error occurred during creation
     */
    public static String createUpdateDSLQuery(Map<String, String> searchCriteriaMap)
        throws InvalidParseOperationException, InvalidCreateOperationException {

        final Update update = new Update();

        for (Entry<String, String> entry : searchCriteriaMap.entrySet()) {
            String searchKeys = entry.getKey();
            String searchValue = entry.getValue();

            if (searchKeys.isEmpty() || searchValue.isEmpty()) {
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
        return update.getFinalUpdate().toString();
    }

    /**
     * Creates Select Query to retrieve all parents relative to the unit specified by its id
     * 
     * @param unitId the unit id
     * @param immediateParents immediate parents (_up field value)
     * @return DSL Select Query
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    public static String createSelectUnitTreeDSLQuery(String unitId, List<String> immediateParents)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Select selectParentsDetails = new Select();

        // Add projections
        // Title
        selectParentsDetails.addUsedProjection(UiConstants.TITLE.getConstantValue());

        // id
        selectParentsDetails.addUsedProjection(UiConstants.ID.getConstantValue());

        // _up
        selectParentsDetails.addUsedProjection(UiConstants.UNITUPS.getConstantValue());

        // Add query

        // Initialize immediateParents if it is null
        if (immediateParents == null) {
            immediateParents = new ArrayList<String>();
        }

        immediateParents.add(unitId);
        String[] allParentsArray = immediateParents.stream().toArray(size -> new String[size]);

        BooleanQuery inParentsIdListQuery = and();
        inParentsIdListQuery.add(in(UiConstants.ID.getConstantValue(), allParentsArray)).setDepthLimit(DEPTH_LIMIT);

        if (inParentsIdListQuery.isReady()) {
            selectParentsDetails.addQueries(inParentsIdListQuery);
        }

        return selectParentsDetails.getFinalSelect().toString();
    }

}
