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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.RuleType;
import fr.gouv.vitam.common.time.LogicalClockRule;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

public class AccessContractRestrictionHelperTest {

    private static final Logger log = LoggerFactory.getLogger(AccessContractRestrictionHelperTest.class);
    final ObjectMapper objectMapper = new ObjectMapper();

    private static final AccessContractModel accessContract_ACCESS_FULL = new AccessContractModel()
        .setRootUnits(Collections.emptySet())
        .setExcludedRootUnits(Collections.emptySet())
        .setEveryOriginatingAgency(true)
        .setOriginatingAgencies(Collections.emptySet())
        .setRuleCategoryToFilter(Collections.emptySet())
        .setRuleCategoryToFilterForTheOtherOriginatingAgencies(Collections.emptySet());
    private static final AccessContractModel accessContract_ACCESS_WITH_ROOT_UNITS = new AccessContractModel()
        .setRootUnits(Set.of("excluded_root_unit_01", "excluded_root_unit_02"))
        .setExcludedRootUnits(Collections.emptySet())
        .setEveryOriginatingAgency(true)
        .setOriginatingAgencies(Collections.emptySet())
        .setRuleCategoryToFilter(Collections.emptySet())
        .setRuleCategoryToFilterForTheOtherOriginatingAgencies(Collections.emptySet());
    private static final AccessContractModel accessContract_ACCESS_WITH_EXCLUDED_ROOT_UNITS = new AccessContractModel()
        .setRootUnits(Collections.emptySet())
        .setExcludedRootUnits(Set.of("excluded_root_unit_01", "excluded_root_unit_02"))
        .setEveryOriginatingAgency(true)
        .setOriginatingAgencies(Collections.emptySet())
        .setRuleCategoryToFilter(Collections.emptySet())
        .setRuleCategoryToFilterForTheOtherOriginatingAgencies(Collections.emptySet());
    private static final AccessContractModel accessContract_ACCESS_BY_PRODUCERS = new AccessContractModel()
        .setRootUnits(Collections.emptySet())
        .setExcludedRootUnits(Collections.emptySet())
        .setEveryOriginatingAgency(false)
        .setOriginatingAgencies(Set.of("originating_agency_01", "originating_agency_02"))
        .setRuleCategoryToFilter(Collections.emptySet())
        .setRuleCategoryToFilterForTheOtherOriginatingAgencies(Collections.emptySet());
    private static final AccessContractModel accessContract_ACCESS_BY_PRODUCERS_DoNotFilter = new AccessContractModel()
        .setRootUnits(Collections.emptySet())
        .setExcludedRootUnits(Collections.emptySet())
        .setEveryOriginatingAgency(false)
        .setOriginatingAgencies(Set.of("originating_agency_01", "originating_agency_02"))
        .setRuleCategoryToFilter(Collections.emptySet())
        .setRuleCategoryToFilterForTheOtherOriginatingAgencies(Collections.emptySet())
        .setDoNotFilterFilingSchemes(true)
        .setSkipFilingSchemeRuleCategoryFilter(false);
    private static final AccessContractModel accessContract_ACCESS_BY_PRODUCERS_SkipFilingScheme =
        new AccessContractModel()
            .setRootUnits(Collections.emptySet())
            .setExcludedRootUnits(Collections.emptySet())
            .setEveryOriginatingAgency(false)
            .setOriginatingAgencies(Set.of("originating_agency_01", "originating_agency_02"))
            .setRuleCategoryToFilter(Collections.emptySet())
            .setRuleCategoryToFilterForTheOtherOriginatingAgencies(Collections.emptySet())
            .setDoNotFilterFilingSchemes(false)
            .setSkipFilingSchemeRuleCategoryFilter(true);
    private static final AccessContractModel accessContract_ACCESS_BY_PRODUCERS_DoNotFilter_SkipFilingScheme =
        new AccessContractModel()
            .setRootUnits(Collections.emptySet())
            .setExcludedRootUnits(Collections.emptySet())
            .setEveryOriginatingAgency(false)
            .setOriginatingAgencies(Set.of("originating_agency_01", "originating_agency_02"))
            .setRuleCategoryToFilter(Collections.emptySet())
            .setRuleCategoryToFilterForTheOtherOriginatingAgencies(Collections.emptySet())
            .setDoNotFilterFilingSchemes(true);
    private static final AccessContractModel accessContract_ACCESS_BY_EXPIRED_MANAGEMENT_RULES =
        new AccessContractModel()
            .setRootUnits(Collections.emptySet())
            .setExcludedRootUnits(Collections.emptySet())
            .setEveryOriginatingAgency(true)
            .setOriginatingAgencies(Collections.emptySet())
            .setRuleCategoryToFilter(Set.of(RuleType.AccessRule, RuleType.AppraisalRule))
            .setRuleCategoryToFilterForTheOtherOriginatingAgencies(Collections.emptySet());
    private static final AccessContractModel accessContract_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES =
        new AccessContractModel()
            .setRootUnits(Collections.emptySet())
            .setExcludedRootUnits(Collections.emptySet())
            .setEveryOriginatingAgency(false)
            .setOriginatingAgencies(Set.of("originating_agency_01", "originating_agency_02"))
            .setRuleCategoryToFilter(Set.of(RuleType.AccessRule, RuleType.AppraisalRule))
            .setRuleCategoryToFilterForTheOtherOriginatingAgencies(Collections.emptySet());
    private static final AccessContractModel accessContract_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES_DoNotFilter =
        new AccessContractModel()
            .setRootUnits(Collections.emptySet())
            .setExcludedRootUnits(Collections.emptySet())
            .setEveryOriginatingAgency(false)
            .setOriginatingAgencies(Set.of("originating_agency_01", "originating_agency_02"))
            .setRuleCategoryToFilter(Set.of(RuleType.AccessRule, RuleType.AppraisalRule))
            .setRuleCategoryToFilterForTheOtherOriginatingAgencies(Collections.emptySet())
            .setDoNotFilterFilingSchemes(true)
            .setSkipFilingSchemeRuleCategoryFilter(false);
    private static final AccessContractModel accessContract_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES_SkipFilingScheme =
        new AccessContractModel()
            .setRootUnits(Collections.emptySet())
            .setExcludedRootUnits(Collections.emptySet())
            .setEveryOriginatingAgency(false)
            .setOriginatingAgencies(Set.of("originating_agency_01", "originating_agency_02"))
            .setRuleCategoryToFilter(Set.of(RuleType.AccessRule, RuleType.AppraisalRule))
            .setRuleCategoryToFilterForTheOtherOriginatingAgencies(Collections.emptySet())
            .setDoNotFilterFilingSchemes(false)
            .setSkipFilingSchemeRuleCategoryFilter(true);
    private static final AccessContractModel accessContract_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES_DoNotFilter_SkipFilingScheme =
        new AccessContractModel()
            .setRootUnits(Collections.emptySet())
            .setExcludedRootUnits(Collections.emptySet())
            .setEveryOriginatingAgency(false)
            .setOriginatingAgencies(Set.of("originating_agency_01", "originating_agency_02"))
            .setRuleCategoryToFilter(Set.of(RuleType.AccessRule, RuleType.AppraisalRule))
            .setRuleCategoryToFilterForTheOtherOriginatingAgencies(Collections.emptySet())
            .setDoNotFilterFilingSchemes(true);
    private static final AccessContractModel accessContract_ACCESS_BY_PRODUCERS_OR_EXPIRED_MANAGEMENT_RULES =
        new AccessContractModel()
            .setRootUnits(Collections.emptySet())
            .setExcludedRootUnits(Collections.emptySet())
            .setEveryOriginatingAgency(false)
            .setOriginatingAgencies(Set.of("originating_agency_01", "originating_agency_02"))
            .setRuleCategoryToFilter(Collections.emptySet())
            .setRuleCategoryToFilterForTheOtherOriginatingAgencies(
                Set.of(
                    RuleType.StorageRule,
                    RuleType.DisseminationRule,
                    RuleType.ClassificationRule,
                    RuleType.ReuseRule
                )
            );

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Before
    public void beforeEach() {
        String updateDate = "2024-08-28T12:34:56.123";
        logicalClock.freezeTime(LocalDateUtil.parseMongoFormattedDate(updateDate));
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_test_ok() throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            emptyQueryDSL(),
            accessContract_ACCESS_FULL
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_01.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_FULL() throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_FULL
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_ACCESS_FULL.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForObjectGroupForSelect_test_ok() throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForObjectGroupForSelect(
            emptyQueryDSL(),
            accessContract_ACCESS_FULL
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_01.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractExceptRuleRestrictionsForObjectGroupForSelect_test_ok() throws Exception {
        // When
        JsonNode result =
            AccessContractRestrictionHelper.applyAccessContractExceptRuleRestrictionsForObjectGroupForSelect(
                emptyQueryDSL(),
                accessContract_ACCESS_FULL
            );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_01.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractExceptRuleRestrictionsForObjectGroupForSelect_ACCESS_BY_EXPIRED_MANAGEMENT_RULES()
        throws Exception {
        // When
        JsonNode result =
            AccessContractRestrictionHelper.applyAccessContractExceptRuleRestrictionsForObjectGroupForSelect(
                simpleQueryDSL(),
                accessContract_ACCESS_BY_EXPIRED_MANAGEMENT_RULES
            );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_ACCESS_FULL.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractExceptRuleRestrictionsForObjectGroupForSelect_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES()
        throws Exception {
        // When
        JsonNode result =
            AccessContractRestrictionHelper.applyAccessContractExceptRuleRestrictionsForObjectGroupForSelect(
                simpleQueryDSL(),
                accessContract_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES
            );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString(
                "AccessContract/select_query_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES_objects.json"
            ),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractExceptRuleRestrictionsForObjectGroupForSelect_ACCESS_BY_PRODUCERS_OR_EXPIRED_MANAGEMENT_RULES()
        throws Exception {
        // When
        JsonNode result =
            AccessContractRestrictionHelper.applyAccessContractExceptRuleRestrictionsForObjectGroupForSelect(
                simpleQueryDSL(),
                accessContract_ACCESS_BY_PRODUCERS_OR_EXPIRED_MANAGEMENT_RULES
            );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString(
                "AccessContract/select_query_ACCESS_BY_PRODUCERS_OR_EXPIRED_MANAGEMENT_RULES_objects.json"
            ),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForUpdate_test_ok() throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForUpdate(
            emptyQueryDSL(),
            accessContract_ACCESS_FULL
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/update_query_ACCESS_FULL.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_BY_PRODUCERS() throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_BY_PRODUCERS
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_ACCESS_BY_PRODUCERS.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_BY_PRODUCERS_with_empty_query() throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            emptyQueryDSL(),
            accessContract_ACCESS_BY_PRODUCERS
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString(
                "AccessContract/select_query_ACCESS_BY_PRODUCERS_with_empty_query.json"
            ),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_BY_PRODUCERS_DoNotFilter() throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_BY_PRODUCERS_DoNotFilter
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_ACCESS_BY_PRODUCERS_skip_filing.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_BY_PRODUCERS_DoNotFilter_SkipFilingScheme()
        throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_BY_PRODUCERS_DoNotFilter_SkipFilingScheme
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_ACCESS_BY_PRODUCERS_skip_filing.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForObjectGroupForSelect_ACCESS_BY_PRODUCERS_DoNotFilter()
        throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForObjectGroupForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_BY_PRODUCERS_DoNotFilter
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_ACCESS_BY_PRODUCERS_forObjects.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_BY_PRODUCERS_SkipFilingScheme() throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_BY_PRODUCERS_SkipFilingScheme
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_ACCESS_BY_PRODUCERS.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_BY_EXPIRED_MANAGEMENT_RULES() throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_BY_EXPIRED_MANAGEMENT_RULES
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_ACCESS_BY_EXPIRED_MANAGEMENT_RULES.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_BY_EXPIRED_MANAGEMENT_RULES_skip_filing_null()
        throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_BY_EXPIRED_MANAGEMENT_RULES
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString(
                "AccessContract/select_query_ACCESS_BY_EXPIRED_MANAGEMENT_RULES_skip_filing.json"
            ),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_BY_PRODUCERS_OR_EXPIRED_MANAGEMENT_RULES()
        throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_BY_PRODUCERS_OR_EXPIRED_MANAGEMENT_RULES
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString(
                "AccessContract/select_query_ACCESS_BY_PRODUCERS_OR_EXPIRED_MANAGEMENT_RULES.json"
            ),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES()
        throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString(
                "AccessContract/select_query_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES.json"
            ),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES_DoNotFilter()
        throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES_DoNotFilter
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString(
                "AccessContract/select_query_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES_DoNotFilter.json"
            ),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES_SkipFilingScheme()
        throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES_SkipFilingScheme
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString(
                "AccessContract/select_query_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES_SkipFilingScheme.json"
            ),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES_DoNotFilter_SkipFilingScheme()
        throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES_DoNotFilter_SkipFilingScheme
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString(
                "AccessContract/select_query_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES_DoNotFilter_SkipFilingScheme.json"
            ),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForObjectGroupForSelect_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES()
        throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForObjectGroupForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString(
                "AccessContract/select_query_ACCESS_BY_PRODUCERS_AND_EXPIRED_MANAGEMENT_RULES_object_group.json"
            ),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_WITH_EXCLUDED_ROOT_UNITS() throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_WITH_EXCLUDED_ROOT_UNITS
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_ACCESS_WITH_EXCLUDED_ROOT_UNITS.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_ACCESS_WITH_ROOT_UNITS() throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_WITH_ROOT_UNITS
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_ACCESS_WITH_ROOT_UNITS.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_WITH_QUERY() throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            queryDSL(
                "{" +
                "\"$query\" :" +
                "    {" +
                "      \"$match\": {" +
                "        \"Title\": \"mon titre\"" +
                "      }" +
                "    } " +
                "}"
            ),
            accessContract_ACCESS_FULL
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_02.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    @Test
    public void applyAccessContractRestrictionForUnitForSelect_every_originating_agency_should_override_list()
        throws Exception {
        // When
        JsonNode result = AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(
            simpleQueryDSL(),
            accessContract_ACCESS_FULL
        );
        // Then
        JsonAssert.assertJsonEquals(
            PropertiesUtils.getResourceAsString("AccessContract/select_query_ACCESS_FULL.json"),
            result,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    private JsonNode emptyQueryDSL() throws JsonProcessingException {
        return queryDSL("{}");
    }

    private JsonNode simpleQueryDSL() throws JsonProcessingException {
        return queryDSL(
            "{" + "\"$query\" :" + "    {" + "      \"$match\": {\"Title\": \"mon titre test\"}" + "    } " + "}"
        );
    }

    /**
     * Select Parser: { $roots: roots, $query : query, $filter : filter, $projection : projection }
     * Update Parser: { $roots: root, $query : query, $filter : filter, $action : action }
     */
    private JsonNode queryDSL(String query) throws JsonProcessingException {
        return objectMapper.readTree(query);
    }
}
