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
import static fr.gouv.vitam.builder.request.construct.QueryHelper.eq;

import java.util.Map;
import java.util.Map.Entry;

import fr.gouv.vitam.builder.request.construct.query.BooleanQuery;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.builder.singlerequest.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Client to create DSL request
 *
 */
public class CreateDSLClient {

    private static final String EVENT_TYPE_PROCESS = "evTypeProc";
    private static final String DEFAULT_EVENT_TYPE_PROCESS = "INGEST";
    private static final String OBJECT_IDENTIFIER_INCOME = "obIdIn";
    private static final String ORDER_BY = "orderby";

    /**
     * generate the DSL query after receiving the search criteria
     *
     *
     * @param searchCriteriaMap
     * @return DSL request
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    public static String createSelectDSLQuery(Map<String, String> searchCriteriaMap)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Select select = new Select();
        final BooleanQuery query = and();
        for (final Entry<String, String> entry : searchCriteriaMap.entrySet()) {
            final String searchKeys = entry.getKey();
            final String searchValue = entry.getValue();
            switch (searchKeys) {
                case ORDER_BY:
                    select.addOrderByAscFilter(searchValue);
                    break;

                case DEFAULT_EVENT_TYPE_PROCESS:
                    query.add(eq(EVENT_TYPE_PROCESS, DEFAULT_EVENT_TYPE_PROCESS));
                    break;

                case OBJECT_IDENTIFIER_INCOME:
                    query.add(eq("events.obIdIn", searchValue));
                    break;

                default:
                    query.add(eq(searchKeys, searchValue));

            }
        }
        select.setQuery(query);
        return select.getFinalSelect().toString();
    }

}
