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
package fr.gouv.vitam.common.database.utils;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.ORIGINATING_AGENCIES;

public final class AccessContractRestrictionHelper {

    private AccessContractRestrictionHelper() {
        // Non instantiable helper class
    }

    public static JsonNode applyAccessContractRestriction(JsonNode queryDsl, AccessContractModel contract)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        Set<String> rootUnits = contract.getRootUnits();
        Set<String> excludedRootUnits = contract.getExcludedRootUnits();
        if ((null != rootUnits && !rootUnits.isEmpty())
            || (null != excludedRootUnits && !excludedRootUnits.isEmpty())) {
            if (null == rootUnits) {
                rootUnits = new HashSet<>();
            }
            String[] rootUnitsArray = rootUnits.toArray(new String[rootUnits.size()]);

            if (null == excludedRootUnits) {
                excludedRootUnits = new HashSet<>();
            }
            String[] excludedRootUnitsArray = excludedRootUnits.toArray(new String[excludedRootUnits.size()]);

            final SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(queryDsl);

            Query rootUnitsRestriction = QueryHelper
                .or().add(QueryHelper.in(BuilderToken.PROJECTIONARGS.ID.exactToken(), rootUnitsArray),
                    QueryHelper.in(BuilderToken.PROJECTIONARGS.ALLUNITUPS.exactToken(), rootUnitsArray));

            Query excludeRootUnitsRestriction = QueryHelper
                .and().add(QueryHelper.nin(BuilderToken.PROJECTIONARGS.ID.exactToken(), excludedRootUnitsArray),
                    QueryHelper.nin(BuilderToken.PROJECTIONARGS.ALLUNITUPS.exactToken(), excludedRootUnitsArray));


            List<Query> queryList = parser.getRequest().getQueries();
            if (queryList.isEmpty()) {
                if (rootUnitsArray.length > 0)
                    queryList.add(rootUnitsRestriction.setDepthLimit(0));
                if (excludedRootUnitsArray.length > 0)
                    queryList.add(excludeRootUnitsRestriction.setDepthLimit(0));
            } else {
                Query firstQuery = queryList.get(0);
                int depth = firstQuery.getParserRelativeDepth();
                Query restrictedQuery = QueryHelper.and().add(firstQuery);
                if (rootUnitsArray.length > 0)
                    restrictedQuery = QueryHelper.and().add(restrictedQuery, rootUnitsRestriction);
                if (excludedRootUnitsArray.length > 0)
                    restrictedQuery = QueryHelper.and().add(restrictedQuery, excludeRootUnitsRestriction);
                restrictedQuery.setDepthLimit(depth);
                parser.getRequest().getQueries().set(0, restrictedQuery);
            }
            queryDsl = parser.getRequest().getFinalSelect();
        }

        return addProdServicesToQuery(queryDsl, contract);
    }

    private static JsonNode addProdServicesToQuery(JsonNode queryDsl, AccessContractModel contract)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        Set<String> prodServices = contract.getOriginatingAgencies();
        if (contract.getEveryOriginatingAgency()) {
            return queryDsl;
        } else {
            final SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(queryDsl);
            parser.getRequest().addQueries(QueryHelper.or()
                .add(QueryHelper.in(
                    ORIGINATING_AGENCIES.exactToken(), prodServices.toArray(new String[0])))
                .add(QueryHelper.eq(BuilderToken.PROJECTIONARGS.UNITTYPE.exactToken(), UnitType.HOLDING_UNIT.name()))
                .setDepthLimit(0));
            return parser.getRequest().getFinalSelect();
        }
    }

    public static JsonNode addProdServicesToQueryForObjectGroup(JsonNode queryDsl)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final AccessContractModel contract = VitamThreadUtils.getVitamSession().getContract();
        Set<String> prodServices = contract.getOriginatingAgencies();
        if (contract.getEveryOriginatingAgency()) {
            return queryDsl;
        } else {
            final SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(queryDsl);
            parser.getRequest().addQueries(QueryHelper.or()
                .add(QueryHelper.in(
                    ORIGINATING_AGENCIES.exactToken(), prodServices.toArray(new String[0])))
                .setDepthLimit(0));
            return parser.getRequest().getFinalSelect();
        }
    }
}


