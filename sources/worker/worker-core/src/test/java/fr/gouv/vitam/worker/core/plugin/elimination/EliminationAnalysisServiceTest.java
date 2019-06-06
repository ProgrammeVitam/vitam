package fr.gouv.vitam.worker.core.plugin.elimination;

import fr.gouv.vitam.common.model.rules.InheritedPropertyResponseModel;
import fr.gouv.vitam.common.model.rules.InheritedRuleResponseModel;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationAnalysisResult;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoAccessLinkInconsistency;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoAccessLinkInconsistencyDetails;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoFinalActionInconsistency;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoKeepAccessSp;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationGlobalStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class EliminationAnalysisServiceTest {

    private static final String OPERATION_ID = "opId";

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void analyzeElimination_EmptyRulesAndProperties() {

        // Given : No rules & no properties
        List<InheritedRuleResponseModel> rules = Collections.emptyList();
        List<InheritedPropertyResponseModel> properties = Collections.emptyList();
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance
            .analyzeElimination(OPERATION_ID, rules, properties, expirationDate, sp1);

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.KEEP);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_EmptyRules() {

        // Given : No rules & a Destroy property
        List<InheritedRuleResponseModel> rules = Collections.emptyList();
        List<InheritedPropertyResponseModel> properties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths(), "FinalAction",
                "Destroy"));
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance
            .analyzeElimination(OPERATION_ID, rules, properties, expirationDate, sp1);

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.KEEP);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_NonExpiredRule() {

        // Given : Destroy final action + Non expired rule
        List<InheritedRuleResponseModel> rules = Arrays.asList(
            new InheritedRuleResponseModel("unit1", "sp1", paths("unit1"), "R1", "2010-01-01", "2020-01-01"));
        List<InheritedPropertyResponseModel> properties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction",
                "Destroy"));
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance
            .analyzeElimination(OPERATION_ID, rules, properties, expirationDate, sp1);

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.KEEP);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_RuleWithoutEndDate() {

        // Given : Destroy final action + rule without end date
        List<InheritedRuleResponseModel> rules = Arrays.asList(
            new InheritedRuleResponseModel("unit1", "sp1", paths("unit1"), "R1", null, null));
        List<InheritedPropertyResponseModel> properties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction",
                "Destroy"));
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance
            .analyzeElimination(OPERATION_ID, rules, properties, expirationDate, sp1);

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.KEEP);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_ExpiredRule() {

        // Given : Destroy final action + expired rule
        List<InheritedRuleResponseModel> rules = Arrays.asList(
            new InheritedRuleResponseModel("unit1", "sp1", paths("unit1"), "R1", "2010-01-01", "2015-01-01"));
        List<InheritedPropertyResponseModel> properties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction",
                "Destroy"));
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance
            .analyzeElimination(OPERATION_ID, rules, properties, expirationDate, sp1);

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.DESTROY);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_MultiplePropertiesKeepAndDestroy() {

        // Given : Multiple Keep / Destroy final actions
        List<InheritedRuleResponseModel> rules = Arrays.asList(
            new InheritedRuleResponseModel("unit1", "sp1", paths("unit1"), "R1", "2010-01-01", "2015-01-01"));
        List<InheritedPropertyResponseModel> properties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy"),
            new InheritedPropertyResponseModel("unit2", "sp1", paths("unit2"), "FinalAction", "Keep"),
            new InheritedPropertyResponseModel("unit3", "sp1", paths("unit3"), "FinalAction", "Keep"));
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance
            .analyzeElimination(OPERATION_ID, rules, properties, expirationDate, sp1);

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.CONFLICT);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(0))
            .isInstanceOf(EliminationExtendedInfoFinalActionInconsistency.class);

        EliminationExtendedInfoFinalActionInconsistency conflict =
            (EliminationExtendedInfoFinalActionInconsistency) eliminationAnalysisResult.getExtendedInfo().get(0);
        assertThat(conflict.getDetails().getOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
    }

    @Test
    public void analyzeElimination_MultiplePropertiesDestroy() {

        // Given : Multiple Destroy final actions
        List<InheritedRuleResponseModel> rules = Arrays.asList(
            new InheritedRuleResponseModel("unit1", "sp1", paths("unit1"), "R1", "2010-01-01", "2015-01-01"));
        List<InheritedPropertyResponseModel> properties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy"),
            new InheritedPropertyResponseModel("unit2", "sp1", paths("unit2"), "FinalAction", "Destroy"));
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance
            .analyzeElimination(OPERATION_ID, rules, properties, expirationDate, sp1);

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.DESTROY);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_MultipleDestroyableOriginatingAgencies() {

        // Given : Multiple Destroyable SP (Destroy final action & expired rule for SP1 + SP2)
        List<InheritedRuleResponseModel> rules = Arrays.asList(
            new InheritedRuleResponseModel("unit1", "sp1", paths("unit1"), "R1", "2010-01-01", "2015-01-01"),
            new InheritedRuleResponseModel("unit2", "sp2", paths("unit2"), "R2", "2010-01-01", "2012-01-01"));
        List<InheritedPropertyResponseModel> properties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy"),
            new InheritedPropertyResponseModel("unit2", "sp2", paths("unit2"), "FinalAction", "Destroy"));
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance
            .analyzeElimination(OPERATION_ID, rules, properties, expirationDate, sp1);

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies())
            .containsExactlyInAnyOrder("sp1", "sp2");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.DESTROY);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_MultipleNonDestroyableOriginatingAgencies() {

        // Given : Multiple non destroyable SP (Keep + expired rule for SP1 : Destroy final action for SP2)
        List<InheritedRuleResponseModel> rules = Arrays.asList(
            new InheritedRuleResponseModel("unit1", "sp1", paths("unit1"), "R1", "2010-01-01", "2015-01-01"));
        List<InheritedPropertyResponseModel> properties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Keep"),
            new InheritedPropertyResponseModel("unit2", "sp2", paths("unit2"), "FinalAction", "Destroy"));
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance
            .analyzeElimination(OPERATION_ID, rules, properties, expirationDate, sp1);

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies())
            .containsExactlyInAnyOrder("sp1", "sp2");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.KEEP);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_MultipleDestroyableAndNonDestoyableOriginatingAgencies() {

        // Given : Destroy final action + non expired rule for SP1 : Destroy final action + expired rule for SP2
        List<InheritedRuleResponseModel> rules = Arrays.asList(
            new InheritedRuleResponseModel("unit1", "sp1", paths("unit1"), "R1", "2010-01-01", "2020-01-01"),
            new InheritedRuleResponseModel("unit2", "sp2", paths("unit2"), "R2", "2010-01-01", "2015-01-01"));
        List<InheritedPropertyResponseModel> properties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy"),
            new InheritedPropertyResponseModel("unit2", "sp2", paths("unit2"), "FinalAction", "Destroy"));
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance
            .analyzeElimination(OPERATION_ID, rules, properties, expirationDate, sp1);

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp2");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.CONFLICT);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_MultipleDestroyableAndNonDestoyableOriginatingAgencies_KeepAccessSP() {

        // Given : Destroy final action + expired rule for SP1 : Destroy final action + non expired rule for SP2 ==> keep access to SP1
        List<InheritedRuleResponseModel> rules = Arrays.asList(
            new InheritedRuleResponseModel("unit1", "sp1", paths("unit1"), "R1", "2010-01-01", "2015-01-01"),
            new InheritedRuleResponseModel("unit2", "sp2", paths("unit2"), "R2", "2010-01-01", "2020-01-01"));
        List<InheritedPropertyResponseModel> properties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy"),
            new InheritedPropertyResponseModel("unit2", "sp2", paths("unit2"), "FinalAction", "Destroy"));
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance
            .analyzeElimination(OPERATION_ID, rules, properties, expirationDate, sp1);

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp2");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.CONFLICT);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).hasSize(1);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(0))
            .isInstanceOf(EliminationExtendedInfoKeepAccessSp.class);
    }

    @Test
    public void analyzeElimination_AccessLinkInconsistency() {

        // Given : Parent unit2 brings 2 SP : non destroyable SP1 & destroyable SP2
        List<InheritedRuleResponseModel> rules = Arrays.asList(
            new InheritedRuleResponseModel("unit2", "sp1", paths("unit1", "unit2", "unit3"), "R2", "2010-01-01",
                "2020-01-01"),
            new InheritedRuleResponseModel("unit3", "sp2", paths("unit1", "unit2", "unit4"), "R3", "2010-01-01",
                "2015-01-01"));
        List<InheritedPropertyResponseModel> properties = Arrays.asList(
            new InheritedPropertyResponseModel("unit2", "sp1", paths("unit1", "unit2", "unit3"), "FinalAction",
                "Destroy"),
            new InheritedPropertyResponseModel("unit3", "sp2", paths("unit1", "unit2", "unit4"), "FinalAction",
                "Destroy"));
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance
            .analyzeElimination(OPERATION_ID, rules, properties, expirationDate, sp1);

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp2");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.CONFLICT);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).hasSize(1);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(0))
            .isInstanceOf(EliminationExtendedInfoAccessLinkInconsistency.class);
        EliminationExtendedInfoAccessLinkInconsistencyDetails accessLinkInconsistencyDetails =
            ((EliminationExtendedInfoAccessLinkInconsistency) eliminationAnalysisResult.getExtendedInfo().get(0))
                .getDetails();

        assertThat(accessLinkInconsistencyDetails.getParentUnitId()).isEqualTo("unit2");
        assertThat(accessLinkInconsistencyDetails.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp2");
        assertThat(accessLinkInconsistencyDetails.getNonDestroyableOriginatingAgencies())
            .containsExactlyInAnyOrder("sp1");
    }

    @Test
    public void analyzeElimination_ComplexTest() {

        // Given :
        //  - SP1 destroyable
        //  - SP2 destroyable
        //  - SP3 non destroyable
        //  - Access link inconsistency via parent unit : SP1 non destroyable & SP2 destroyable

        List<InheritedRuleResponseModel> rules = Arrays.asList(
            new InheritedRuleResponseModel("unit5", "sp1", paths("unit1", "unit2", "unit5"), "R1", "2010-01-01",
                "2015-01-01"),
            new InheritedRuleResponseModel("unit4", "sp2", paths("unit1", "unit2", "unit4"), "R2", "2010-01-01",
                "2015-01-01"),
            new InheritedRuleResponseModel("unit7", "sp1", paths("unit7"), "R3", "2010-01-01", "2015-01-01"));

        List<InheritedPropertyResponseModel> properties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy"),
            new InheritedPropertyResponseModel("unit3", "sp2", paths("unit1", "unit2", "unit3"), "FinalAction",
                "Destroy"),
            new InheritedPropertyResponseModel("unit6", "sp3", paths("unit6"), "FinalAction", "Keep"));

        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance
            .analyzeElimination(OPERATION_ID, rules, properties, expirationDate, sp1);

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies())
            .containsExactlyInAnyOrder("sp1", "sp2");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp3");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.CONFLICT);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).hasSize(2);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(0))
            .isInstanceOf(EliminationExtendedInfoKeepAccessSp.class);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(1))
            .isInstanceOf(EliminationExtendedInfoAccessLinkInconsistency.class);

        EliminationExtendedInfoAccessLinkInconsistencyDetails accessLinkInconsistencyDetails =
            ((EliminationExtendedInfoAccessLinkInconsistency) eliminationAnalysisResult.getExtendedInfo().get(1))
                .getDetails();
        assertThat(accessLinkInconsistencyDetails.getParentUnitId()).isEqualTo("unit2");
        assertThat(accessLinkInconsistencyDetails.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp2");
        assertThat(accessLinkInconsistencyDetails.getNonDestroyableOriginatingAgencies())
            .containsExactlyInAnyOrder("sp1");
    }

    private List<List<String>> paths(String... ids) {
        return Collections.singletonList(Arrays.asList(ids));
    }
}
