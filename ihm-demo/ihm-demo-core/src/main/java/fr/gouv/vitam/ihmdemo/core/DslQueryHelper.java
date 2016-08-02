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

import static fr.gouv.vitam.builder.request.construct.QueryHelper.and;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.*;

import java.util.Map;
import java.util.Map.Entry;

import fr.gouv.vitam.builder.request.construct.Select;
import fr.gouv.vitam.builder.request.construct.Update;
import fr.gouv.vitam.builder.request.construct.action.SetAction;
import fr.gouv.vitam.builder.request.construct.query.BooleanQuery;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
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
    private static final String EVENT_DATE_TIME = "evDateTime";
    private static final String DEFAULT_EVENT_TYPE_PROCESS = "INGEST";
    private static final String PUID = "PUID";
    private static final String OBJECT_IDENTIFIER_INCOME = "obIdIn";
    private static final String FORMAT = "FORMAT";
    private static final String ORDER_BY = "orderby";
    private static final String PROJECTION_PREFIX = "projection_";
    private static final String UPDATE_PREFIX = "update_";

    /**
     * generate the DSL query after receiving the search criteria
     * 
     * 
     * @param searchCriteriaMap
     * @return DSL request
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    public static String createSingleQueryDSL(Map<String, String> searchCriteriaMap)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final fr.gouv.vitam.builder.singlerequest.Select select = new fr.gouv.vitam.builder.singlerequest.Select();
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
        LOGGER.error(select.getFinalSelect().toString());
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
            // update.addActions(new AddAction(searchKeys, searchValue));
        }
        return update.getFinalUpdate().toString();
    }

}
