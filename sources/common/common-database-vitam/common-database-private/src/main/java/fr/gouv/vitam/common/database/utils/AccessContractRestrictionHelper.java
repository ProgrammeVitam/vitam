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
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.RuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import static fr.gouv.vitam.common.model.UnitType.FILING_UNIT;
import static fr.gouv.vitam.common.model.UnitType.HOLDING_UNIT;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

public final class AccessContractRestrictionHelper {

    private static final Logger log = LoggerFactory.getLogger(AccessContractRestrictionHelper.class);

    private AccessContractRestrictionHelper() {
        // Non instantiable helper class
    }

    /**
     * SELECT - UNIT:
     * Apply access contract restriction for archive unit for select request
     */
    public static JsonNode applyAccessContractRestrictionForUnitForSelect(
        JsonNode queryDsl,
        AccessContractModel accessContract
    ) throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryDsl);
        applyAccessContractRestriction(parser, accessContract, true, true);
        return parser.getRequest().getFinalSelect();
    }

    /**
     * SELECT - OBJECT GROUP:
     * Apply access contract restriction for object group for select request
     */
    public static JsonNode applyAccessContractRestrictionForObjectGroupForSelect(
        JsonNode queryDsl,
        AccessContractModel accessContract
    ) throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryDsl);
        applyAccessContractRestriction(parser, accessContract, false, true);
        return parser.getRequest().getFinalSelect();
    }

    /**
     * SELECT - OBJECT GROUP (without rule restriction):
     * Apply access contract restriction except rule restrictions for object group for select request
     */
    public static JsonNode applyAccessContractExceptRuleRestrictionsForObjectGroupForSelect(
        JsonNode queryDsl,
        AccessContractModel accessContract
    ) throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryDsl);
        applyAccessContractRestriction(parser, accessContract, false, false);
        return parser.getRequest().getFinalSelect();
    }

    /**
     * UPDATE - UNIT:
     * Apply access contract restriction for archive unit for update request
     */
    public static JsonNode applyAccessContractRestrictionForUnitForUpdate(
        JsonNode queryDsl,
        AccessContractModel accessContract
    ) throws InvalidParseOperationException, InvalidCreateOperationException {
        final UpdateParserMultiple parser = new UpdateParserMultiple();
        parser.parse(queryDsl);
        applyAccessContractRestriction(parser, accessContract, true, true);
        return parser.getRequest().getFinalUpdate();
    }

    /**
     * Apply access accessContract restriction for object group and archive unit
     */
    private static void applyAccessContractRestriction(
        RequestParserMultiple parser,
        AccessContractModel accessContract,
        boolean isUnit,
        boolean applyRuleRestrictions
    ) throws InvalidCreateOperationException {
        accessContract.initializeDefaultValue();
        BooleanQuery restrictionsForQuery = and();

        String fieldToQuery = isUnit
            ? BuilderToken.PROJECTIONARGS.ID.exactToken()
            : BuilderToken.PROJECTIONARGS.UNITUPS.exactToken();

        Set<String> rootUnits = accessContract.getRootUnits();
        if (isNotEmpty(rootUnits)) {
            restrictionsForQuery.add(getRootUnitsRestriction(rootUnits, fieldToQuery));
        }

        Set<String> excludedRootUnits = accessContract.getExcludedRootUnits();
        if (isNotEmpty(excludedRootUnits)) {
            restrictionsForQuery.add(getExcludedRootUnitsRestriction(excludedRootUnits, fieldToQuery));
        }

        AccessRightType accessRightType = getAccessRightType(accessContract);
        switch (accessRightType) {
            case ACCESS_FULL -> log.debug("no restriction on originating agencies");
            case ACCESS_BY_PRODUCERS -> addRestrictionForOriginatingAgency(
                restrictionsForQuery,
                accessContract,
                isUnit
            );
            case ACCESS_BY_EXPIRED_MANAGEMENT_RULES -> {
                if (applyRuleRestrictions) {
                    addRestrictionForManagementRules(restrictionsForQuery, accessContract);
                }
            }
            case ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES -> {
                if (applyRuleRestrictions) {
                    addRestrictionForOriginatingAgencyAndManagementRules(restrictionsForQuery, accessContract, isUnit);
                } else {
                    addRestrictionForOriginatingAgency(restrictionsForQuery, accessContract, isUnit);
                }
            }
            case ACCESS_BY_PRODUCERS_OR_EXPIRED_MANAGEMENT_RULES -> {
                if (applyRuleRestrictions) {
                    addRestrictionForOriginatingAgencyOrManagementRules(restrictionsForQuery, accessContract);
                } else if (isUnit) {
                    addRestrictionForOriginatingAgency(restrictionsForQuery, accessContract, isUnit);
                }
            }
            default -> throw new IllegalStateException("Access contract is ambigous: " + accessRightType);
        }

        if (isNotEmpty(restrictionsForQuery.getQueries())) {
            //si y a des restrictions
            List<Query> queryList = new ArrayList<>(parser.getRequest().getQueries());
            if (queryList.isEmpty()) {
                parser.getRequest().getQueries().add(restrictionsForQuery.setDepthLimit(0));
            } else {
                // In cas of one or multiple query
                for (int i = 0; i < queryList.size(); i++) {
                    final Query query = queryList.get(i);
                    int depth = query.getParserRelativeDepth();
                    Query restrictedQuery = and().add(restrictionsForQuery, query).setDepthLimit(depth);
                    parser.getRequest().getQueries().set(i, restrictedQuery);
                }
            }
        }
    }

    private static Query getRootUnitsRestriction(Set<String> rootUnits, String fieldToQuery)
        throws InvalidCreateOperationException {
        String[] rootUnitsArray = rootUnits.toArray(new String[0]);
        return or()
            .add(
                in(fieldToQuery, rootUnitsArray),
                in(BuilderToken.PROJECTIONARGS.ALLUNITUPS.exactToken(), rootUnitsArray)
            );
    }

    private static void addRestrictionForOriginatingAgency(
        BooleanQuery restrictionsForQuery,
        AccessContractModel accessContract,
        boolean isUnit
    ) throws InvalidCreateOperationException {
        Query originatingAgencyRestriction = getOriginatingAgencyRestriction(accessContract);
        if (isUnit) {
            originatingAgencyRestriction = or()
                .add(skipFiling(accessContract.getDoNotFilterFilingSchemes()), originatingAgencyRestriction);
        }
        restrictionsForQuery.add(originatingAgencyRestriction);
    }

    /**
     * // Warning: This query filters all entries when used for selecting ObjectGroups with a DSL.
     * //          An access accessContract defining a RuleCategoryToFilter cannot be used to query ObjectGroups
     * //          If this behavior is no more wanted, this code must be changed...
     */
    private static void addRestrictionForManagementRules(
        BooleanQuery restrictionsForQuery,
        AccessContractModel accessContract
    ) throws InvalidCreateOperationException {
        restrictionsForQuery.add(
            or()
                .add(
                    skipFiling(
                        firstNonNull(
                            accessContract.getSkipFilingSchemeRuleCategoryFilter(),
                            accessContract.getDoNotFilterFilingSchemes()
                        )
                    ),
                    getRuleCategoryRestriction(accessContract)
                )
        );
    }

    private static void addRestrictionForOriginatingAgencyOrManagementRules(
        BooleanQuery restrictionsForQuery,
        AccessContractModel accessContract
    ) throws InvalidCreateOperationException {
        BooleanQuery categoryRestrictionOrSkipedUnit = or()
            .add(
                skipFiling(
                    firstNonNull(
                        accessContract.getSkipFilingSchemeRuleCategoryFilter(),
                        accessContract.getDoNotFilterFilingSchemes()
                    )
                ),
                getRuleCategoryForTheOtherOriginatingAgenciesRestriction(accessContract)
            );
        BooleanQuery originatingAgencyRestrictionOrSkipedUnit = or()
            .add(
                skipFiling(accessContract.getDoNotFilterFilingSchemes()),
                getOriginatingAgencyRestriction(accessContract)
            );
        restrictionsForQuery.add(or().add(originatingAgencyRestrictionOrSkipedUnit, categoryRestrictionOrSkipedUnit));
    }

    private static void addRestrictionForOriginatingAgencyAndManagementRules(
        BooleanQuery restrictionsForQuery,
        AccessContractModel accessContract,
        boolean isUnit
    ) throws InvalidCreateOperationException {
        BooleanQuery categoryRestrictionOrSkipedUnit = or()
            .add(
                skipFiling(
                    firstNonNull(
                        accessContract.getSkipFilingSchemeRuleCategoryFilter(),
                        accessContract.getDoNotFilterFilingSchemes()
                    )
                ),
                getRuleCategoryRestriction(accessContract)
            );

        Query originatingAgencyRestrictionOrSkipedUnit = getOriginatingAgencyRestriction(accessContract);
        if (isUnit) {
            originatingAgencyRestrictionOrSkipedUnit = or()
                .add(
                    skipFiling(accessContract.getDoNotFilterFilingSchemes()),
                    originatingAgencyRestrictionOrSkipedUnit
                );
        }
        restrictionsForQuery.add(originatingAgencyRestrictionOrSkipedUnit, categoryRestrictionOrSkipedUnit);
    }

    /**
     * UNITUPS;
     * {"$nin": {"#unitups": ["excluded_root_unit_01", "excluded_root_unit_02"]}},
     * {"$nin": {"#allunitups": ["excluded_root_unit_01", "excluded_root_unit_02"]}},
     * ID:
     * {"$nin": {"#id": ["excluded_root_unit_01", "excluded_root_unit_02"]}},
     * {"$nin": {"#allunitups": ["excluded_root_unit_01", "excluded_root_unit_02"]}},
     */
    private static Query getExcludedRootUnitsRestriction(Set<String> excludedRootUnits, String fieldToQuery)
        throws InvalidCreateOperationException {
        String[] excludedRootUnitsArray = excludedRootUnits.toArray(new String[0]);
        return and()
            .add(
                nin(fieldToQuery, excludedRootUnitsArray),
                nin(BuilderToken.PROJECTIONARGS.ALLUNITUPS.exactToken(), excludedRootUnitsArray)
            );
    }

    /**
     * "$and": [
     * {"$lt": {"#computedInheritedRules.AccessRule.MaxEndDate": "2024-08-28"}},
     * ...
     * {"$lt": {"#computedInheritedRules.StorageRule.MaxEndDate": "2024-08-28"}}
     * ]
     */
    private static Query getRuleRestriction(Set<RuleType> rules) throws InvalidCreateOperationException {
        // no and when only one element ?
        if (rules.size() == 1) {
            return lt(fieldName(rules.iterator().next()), LocalDateUtil.todayFormatted());
        }
        BooleanQuery categoryRestriction = and();
        for (RuleType ruleType : rules) {
            categoryRestriction.add(lt(fieldName(ruleType), LocalDateUtil.todayFormatted()));
        }
        return categoryRestriction;
    }

    /**
     * "$and": [
     * {"$lt": {"#computedInheritedRules.AccessRule.MaxEndDate": "2024-08-28"}},
     * ...
     * {"$lt": {"#computedInheritedRules.StorageRule.MaxEndDate": "2024-08-28"}}
     * ]
     */
    private static Query getRuleCategoryRestriction(AccessContractModel accessContract)
        throws InvalidCreateOperationException {
        return getRuleRestriction(accessContract.getRuleCategoryToFilter());
    }

    /**
     * "$and": [
     * {"$lt": {"#computedInheritedRules.AccessRule.MaxEndDate": "2024-08-28"}},
     * ...
     * {"$lt": {"#computedInheritedRules.StorageRule.MaxEndDate": "2024-08-28"}}
     * ]
     */
    private static Query getRuleCategoryForTheOtherOriginatingAgenciesRestriction(AccessContractModel accessContract)
        throws InvalidCreateOperationException {
        return getRuleRestriction(accessContract.getRuleCategoryToFilterForTheOtherOriginatingAgencies());
    }

    /**
     * "#computedInheritedRules.StorageRule.MaxEndDate"
     */
    private static String fieldName(RuleType ruleType) {
        return (
            BuilderToken.PROJECTIONARGS.COMPUTED_INHERITED_RULES.exactToken() + "." + ruleType.name() + ".MaxEndDate"
        );
    }

    /**
     * {"$in": {"#originating_agencies": [
     * "originating_agency_01",
     * ...
     * "originating_agency_02",
     * ]}},
     */
    private static Query getOriginatingAgencyRestriction(AccessContractModel accessContract)
        throws InvalidCreateOperationException {
        return in(ORIGINATING_AGENCIES.exactToken(), accessContract.getOriginatingAgencies().toArray(new String[0]));
    }

    /**
     * {"$eq": {"#unitType": "HOLDING_UNIT"}},
     * {"$in": {"#unitType": ["FILING_UNIT", "HOLDING_UNIT"]}},
     * always skip HOLDING
     */
    private static Query skipFiling(boolean skipFilingScheme) throws InvalidCreateOperationException {
        if (skipFilingScheme) {
            return in(VitamFieldsHelper.unitType(), FILING_UNIT.name(), HOLDING_UNIT.name());
        }
        return eq(VitamFieldsHelper.unitType(), HOLDING_UNIT.name());
    }

    public static AccessRightType getAccessRightType(AccessContractModel accessContract) {
        if (
            accessContract.getEveryOriginatingAgency() &&
            isEmpty(accessContract.getRuleCategoryToFilter()) &&
            isEmpty(accessContract.getRuleCategoryToFilterForTheOtherOriginatingAgencies())
        ) {
            return AccessRightType.ACCESS_FULL;
        }
        if (
            accessContract.getEveryOriginatingAgency() &&
            isNotEmpty(accessContract.getRuleCategoryToFilter()) &&
            isEmpty(accessContract.getRuleCategoryToFilterForTheOtherOriginatingAgencies())
        ) {
            return AccessRightType.ACCESS_BY_EXPIRED_MANAGEMENT_RULES;
        }
        if (
            !accessContract.getEveryOriginatingAgency() &&
            isEmpty(accessContract.getRuleCategoryToFilter()) &&
            isEmpty(accessContract.getRuleCategoryToFilterForTheOtherOriginatingAgencies())
        ) {
            return AccessRightType.ACCESS_BY_PRODUCERS;
        }
        if (
            !accessContract.getEveryOriginatingAgency() &&
            isNotEmpty(accessContract.getRuleCategoryToFilter()) &&
            isEmpty(accessContract.getRuleCategoryToFilterForTheOtherOriginatingAgencies())
        ) {
            return AccessRightType.ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES;
        }
        if (
            !accessContract.getEveryOriginatingAgency() &&
            isEmpty(accessContract.getRuleCategoryToFilter()) &&
            isNotEmpty(accessContract.getRuleCategoryToFilterForTheOtherOriginatingAgencies())
        ) {
            return AccessRightType.ACCESS_BY_PRODUCERS_OR_EXPIRED_MANAGEMENT_RULES;
        }

        return AccessRightType.UNKNOWN;
    }
}
