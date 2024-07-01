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
package fr.gouv.vitam.common.database.utils;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.RuleType;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.nin;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.ORIGINATING_AGENCIES;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.UNITTYPE;
import static fr.gouv.vitam.common.model.UnitType.HOLDING_UNIT;

public final class AccessContractRestrictionHelper {

    private AccessContractRestrictionHelper() {
        // Non instantiable helper class
    }

    /**
     * Apply access contract restriction for archive unit for select request
     *
     * @param queryDsl
     * @param contract
     * @return
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    public static JsonNode applyAccessContractRestrictionForUnitForSelect(
        JsonNode queryDsl,
        AccessContractModel contract
    ) throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryDsl);
        applyAccessContractRestriction(parser, contract, true, true);
        return parser.getRequest().getFinalSelect();
    }

    /**
     * Apply access contract restriction for object group for select request
     *
     * @param queryDsl
     * @param contract
     * @return JsonNode contains restriction
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    public static JsonNode applyAccessContractRestrictionForObjectGroupForSelect(
        JsonNode queryDsl,
        AccessContractModel contract
    ) throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryDsl);
        applyAccessContractRestriction(parser, contract, false, true);
        return parser.getRequest().getFinalSelect();
    }

    /**
     * Apply access contract restriction except rule restrictions for object group for select request
     *
     * @param queryDsl
     * @param contract
     * @return JsonNode contains restriction
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    public static JsonNode applyAccessContractExceptRuleRestrictionsForObjectGroupForSelect(
        JsonNode queryDsl,
        AccessContractModel contract
    ) throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryDsl);
        applyAccessContractRestriction(parser, contract, false, false);
        return parser.getRequest().getFinalSelect();
    }

    /**
     * Apply access contract restriction for archive unit for update request
     *
     * @param queryDsl
     * @param contract
     * @return
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    public static JsonNode applyAccessContractRestrictionForUnitForUpdate(
        JsonNode queryDsl,
        AccessContractModel contract
    ) throws InvalidParseOperationException, InvalidCreateOperationException {
        final UpdateParserMultiple parser = new UpdateParserMultiple();
        parser.parse(queryDsl);
        applyAccessContractRestriction(parser, contract, true, true);
        return parser.getRequest().getFinalUpdate();
    }

    /**
     * Apply access contract restriction for object group and archive unit
     *
     * @param parser
     * @param contract
     * @param isUnit
     * @param applyRuleRestrictions
     * @return JsonNode contains restriction
     * @throws InvalidCreateOperationException
     */
    private static void applyAccessContractRestriction(
        RequestParserMultiple parser,
        AccessContractModel contract,
        boolean isUnit,
        boolean applyRuleRestrictions
    ) throws InvalidCreateOperationException {
        Set<String> rootUnits = contract.getRootUnits();
        Set<String> excludedRootUnits = contract.getExcludedRootUnits();

        List<Query> queryList = new ArrayList<>(parser.getRequest().getQueries());

        BooleanQuery restrictionQueries = and();

        String fieldToQuery = isUnit
            ? BuilderToken.PROJECTIONARGS.ID.exactToken()
            : BuilderToken.PROJECTIONARGS.UNITUPS.exactToken();

        if (!rootUnits.isEmpty()) {
            String[] rootUnitsArray = rootUnits.toArray(new String[0]);
            Query rootUnitsRestriction = or()
                .add(
                    in(fieldToQuery, rootUnitsArray),
                    in(BuilderToken.PROJECTIONARGS.ALLUNITUPS.exactToken(), rootUnitsArray)
                );
            restrictionQueries.add(rootUnitsRestriction);
        }

        if (!excludedRootUnits.isEmpty()) {
            String[] excludedRootUnitsArray = excludedRootUnits.toArray(new String[0]);
            Query excludeRootUnitsRestriction = and()
                .add(
                    nin(fieldToQuery, excludedRootUnitsArray),
                    nin(BuilderToken.PROJECTIONARGS.ALLUNITUPS.exactToken(), excludedRootUnitsArray)
                );
            restrictionQueries.add(excludeRootUnitsRestriction);
        }

        if (applyRuleRestrictions && !contract.getRuleCategoryToFilter().isEmpty()) {
            Query rulesRestrictionQuery = getRulesRestrictionQuery(contract);
            restrictionQueries.add(rulesRestrictionQuery);
        }

        // Filter on originating Agencies
        if (!contract.getEveryOriginatingAgency()) {
            Set<String> prodServices = contract.getOriginatingAgencies();

            InQuery originatingAgencyRestriction = in(
                ORIGINATING_AGENCIES.exactToken(),
                prodServices.toArray(new String[0])
            );

            Query restriction = isUnit
                ? or().add(originatingAgencyRestriction, eq(UNITTYPE.exactToken(), HOLDING_UNIT.name()))
                : originatingAgencyRestriction;

            restrictionQueries.add(restriction);
        }

        if (!restrictionQueries.getQueries().isEmpty()) {
            if (queryList.isEmpty()) {
                parser.getRequest().getQueries().add(restrictionQueries.setDepthLimit(0));
            } else {
                // In cas of one or multiple query
                for (int i = 0; i < queryList.size(); i++) {
                    final Query query = queryList.get(i);
                    int depth = query.getParserRelativeDepth();
                    Query restrictedQuery = and().add(restrictionQueries, query);
                    restrictedQuery.setDepthLimit(depth);
                    parser.getRequest().getQueries().set(i, restrictedQuery);
                }
            }
        }
    }

    private static Query getRulesRestrictionQuery(AccessContractModel contract) throws InvalidCreateOperationException {
        BooleanQuery rulesRestrictionQuery = and();
        for (RuleType ruleType : contract.getRuleCategoryToFilter()) {
            String ruleFieldName =
                BuilderToken.PROJECTIONARGS.COMPUTED_INHERITED_RULES.exactToken() +
                "." +
                ruleType.name() +
                ".MaxEndDate";
            CompareQuery maxEndDateExistsAndReached = lt(ruleFieldName, LocalDate.now().toString());
            rulesRestrictionQuery.add(maxEndDateExistsAndReached);
        }
        return rulesRestrictionQuery;
    }

    /**
     * Just filter by originating agency.
     *
     * Deprecated as used just for object group, from Release 8 use applyAccessContractRestrictionForObjectGroupForSelect instead
     *
     * @param queryDsl
     * @return
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Deprecated
    public static JsonNode applyAccessContractRestrictionOnOriginatingAgencies(JsonNode queryDsl)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final AccessContractModel contract = VitamThreadUtils.getVitamSession().getContract();

        if (contract.getEveryOriginatingAgency()) {
            return queryDsl;
        } else {
            final SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(queryDsl);
            List<Query> queryList = new ArrayList<>(parser.getRequest().getQueries());

            Set<String> prodServices = contract.getOriginatingAgencies();
            Query originatingAgencyRestriction = in(
                ORIGINATING_AGENCIES.exactToken(),
                prodServices.toArray(new String[0])
            );

            if (queryList.isEmpty()) {
                parser.getRequest().getQueries().add(originatingAgencyRestriction.setDepthLimit(0));
            } else {
                // In cas of one or multiple query
                for (int i = 0; i < queryList.size(); i++) {
                    final Query query = queryList.get(i);
                    int depth = query.getParserRelativeDepth();
                    Query restrictedQuery = and().add(originatingAgencyRestriction, query);
                    restrictedQuery.setDepthLimit(depth);
                    parser.getRequest().getQueries().set(i, restrictedQuery);
                }
            }
            return parser.getRequest().getFinalSelect();
        }
    }
}
