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
package fr.gouv.vitam.worker.core.plugin.elimination;

import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.model.rules.InheritedPropertyResponseModel;
import fr.gouv.vitam.common.model.rules.InheritedRuleResponseModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationAnalysisResult;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoAccessLinkInconsistency;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoAccessLinkInconsistencyDetails;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoFinalActionInconsistency;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoHoldRule;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoHoldRuleDetails;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoKeepAccessSp;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationGlobalStatus;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class EliminationAnalysisServiceTest {

    private static final String OPERATION_ID = "opId";

    @Test
    public void analyzeElimination_EmptyRulesAndProperties() {
        // Given : No appraisal rules & properties & no hold rules
        List<InheritedRuleResponseModel> appraisalRules = Collections.emptyList();
        List<InheritedPropertyResponseModel> appraisalProperties = Collections.emptyList();
        List<InheritedRuleResponseModel> holdRules = Collections.emptyList();
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.KEEP);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_FinalActionWithoutAppraisalRules() {
        // Given : No appraisal rules + a Destroy appraisal property + no hold rules
        List<InheritedRuleResponseModel> appraisalRules = Collections.emptyList();
        List<InheritedPropertyResponseModel> appraisalProperties = Collections.singletonList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths(), "FinalAction", "Destroy")
        );
        List<InheritedRuleResponseModel> holdRules = Collections.emptyList();
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.KEEP);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_NonExpiredAppraisalRule() {
        // Given : Destroy appraisal final action + Non expired appraisal rule + no hold rules
        List<InheritedRuleResponseModel> appraisalRules = Collections.singletonList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "R1",
                ruleAttributes("2010-01-01", "2020-01-01")
            )
        );
        List<InheritedPropertyResponseModel> appraisalProperties = Collections.singletonList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy")
        );
        List<InheritedRuleResponseModel> holdRules = Collections.emptyList();
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.KEEP);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_AppraisalRuleWithoutEndDate() {
        // Given : Destroy appraisal final action + appraisal rule without end date + no hold rules
        List<InheritedRuleResponseModel> appraisalRules = Collections.singletonList(
            new InheritedRuleResponseModel("unit1", "sp1", paths("unit1"), "R1", ruleAttributes(null, null))
        );
        List<InheritedPropertyResponseModel> appraisalProperties = Collections.singletonList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy")
        );
        List<InheritedRuleResponseModel> holdRules = Collections.emptyList();
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.KEEP);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_ExpiredAppraisalRule() {
        // Given : Destroy appraisal final action + expired appraisal rule + no hold rules
        List<InheritedRuleResponseModel> appraisalRules = Collections.singletonList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "R1",
                ruleAttributes("2010-01-01", "2015-01-01")
            )
        );
        List<InheritedPropertyResponseModel> appraisalProperties = Collections.singletonList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy")
        );
        List<InheritedRuleResponseModel> holdRules = Collections.emptyList();
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.DESTROY);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_MultiplePropertiesKeepAndDestroy() {
        // Given : Multiple Keep / Destroy appraisal final actions + no hold rules
        List<InheritedRuleResponseModel> appraisalRules = Collections.singletonList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "R1",
                ruleAttributes("2010-01-01", "2015-01-01")
            )
        );
        List<InheritedPropertyResponseModel> appraisalProperties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy"),
            new InheritedPropertyResponseModel("unit2", "sp1", paths("unit2"), "FinalAction", "Keep"),
            new InheritedPropertyResponseModel("unit3", "sp1", paths("unit3"), "FinalAction", "Keep")
        );
        List<InheritedRuleResponseModel> holdRules = Collections.emptyList();
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.CONFLICT);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(0)).isInstanceOf(
            EliminationExtendedInfoFinalActionInconsistency.class
        );

        EliminationExtendedInfoFinalActionInconsistency conflict =
            (EliminationExtendedInfoFinalActionInconsistency) eliminationAnalysisResult.getExtendedInfo().get(0);
        assertThat(conflict.getDetails().getOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
    }

    @Test
    public void analyzeElimination_MultiplePropertiesDestroy() {
        // Given : Multiple Destroy final actions + no hold rules
        List<InheritedRuleResponseModel> appraisalRules = Collections.singletonList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "R1",
                ruleAttributes("2010-01-01", "2015-01-01")
            )
        );
        List<InheritedPropertyResponseModel> appraisalProperties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy"),
            new InheritedPropertyResponseModel("unit2", "sp1", paths("unit2"), "FinalAction", "Destroy")
        );
        List<InheritedRuleResponseModel> holdRules = Collections.emptyList();
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.DESTROY);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_MultipleDestroyableOriginatingAgencies() {
        // Given : Multiple Destroyable SP (Destroy final action & expired rule for SP1 + SP2) + no hold rules
        List<InheritedRuleResponseModel> appraisalRules = Arrays.asList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "R1",
                ruleAttributes("2010-01-01", "2015-01-01")
            ),
            new InheritedRuleResponseModel(
                "unit2",
                "sp2",
                paths("unit2"),
                "R2",
                ruleAttributes("2010-01-01", "2012-01-01")
            )
        );
        List<InheritedPropertyResponseModel> appraisalProperties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy"),
            new InheritedPropertyResponseModel("unit2", "sp2", paths("unit2"), "FinalAction", "Destroy")
        );
        List<InheritedRuleResponseModel> holdRules = Collections.emptyList();
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder(
            "sp1",
            "sp2"
        );
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.DESTROY);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_MultipleNonDestroyableOriginatingAgencies() {
        // Given : Multiple non destroyable SP (Keep + expired rule for SP1 : Destroy final action for SP2)
        List<InheritedRuleResponseModel> appraisalRules = Collections.singletonList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "R1",
                ruleAttributes("2010-01-01", "2015-01-01")
            )
        );
        List<InheritedPropertyResponseModel> appraisalProperties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Keep"),
            new InheritedPropertyResponseModel("unit2", "sp2", paths("unit2"), "FinalAction", "Destroy")
        );
        List<InheritedRuleResponseModel> holdRules = Collections.emptyList();
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder(
            "sp1",
            "sp2"
        );
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.KEEP);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_MultipleDestroyableAndNonDestroyableOriginatingAgencies() {
        // Given : Destroy final action + non expired rule for SP1 : Destroy final action + expired rule for SP2
        List<InheritedRuleResponseModel> appraisalRules = Arrays.asList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "R1",
                ruleAttributes("2010-01-01", "2020-01-01")
            ),
            new InheritedRuleResponseModel(
                "unit2",
                "sp2",
                paths("unit2"),
                "R2",
                ruleAttributes("2010-01-01", "2015-01-01")
            )
        );
        List<InheritedPropertyResponseModel> appraisalProperties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy"),
            new InheritedPropertyResponseModel("unit2", "sp2", paths("unit2"), "FinalAction", "Destroy")
        );
        List<InheritedRuleResponseModel> holdRules = Collections.emptyList();
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp2");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.CONFLICT);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_MultipleDestroyableAndNonDestroyableOriginatingAgencies_KeepAccessSP() {
        // Given : Destroy final action + expired rule for SP1 : Destroy final action + non expired rule for SP2 ==> keep access to SP1
        List<InheritedRuleResponseModel> appraisalRules = Arrays.asList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "R1",
                ruleAttributes("2010-01-01", "2015-01-01")
            ),
            new InheritedRuleResponseModel(
                "unit2",
                "sp2",
                paths("unit2"),
                "R2",
                ruleAttributes("2010-01-01", "2020-01-01")
            )
        );
        List<InheritedPropertyResponseModel> appraisalProperties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy"),
            new InheritedPropertyResponseModel("unit2", "sp2", paths("unit2"), "FinalAction", "Destroy")
        );
        List<InheritedRuleResponseModel> holdRules = Collections.emptyList();
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp2");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.CONFLICT);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).hasSize(1);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(0)).isInstanceOf(
            EliminationExtendedInfoKeepAccessSp.class
        );
    }

    @Test
    public void analyzeElimination_AccessLinkInconsistency() {
        // Given : Parent unit2 brings 2 SP : non destroyable SP1 & destroyable SP2
        List<InheritedRuleResponseModel> appraisalRules = Arrays.asList(
            new InheritedRuleResponseModel(
                "unit2",
                "sp1",
                paths("unit1", "unit2", "unit3"),
                "R2",
                ruleAttributes("2010-01-01", "2020-01-01")
            ),
            new InheritedRuleResponseModel(
                "unit3",
                "sp2",
                paths("unit1", "unit2", "unit4"),
                "R3",
                ruleAttributes("2010-01-01", "2015-01-01")
            )
        );
        List<InheritedPropertyResponseModel> appraisalProperties = Arrays.asList(
            new InheritedPropertyResponseModel(
                "unit2",
                "sp1",
                paths("unit1", "unit2", "unit3"),
                "FinalAction",
                "Destroy"
            ),
            new InheritedPropertyResponseModel(
                "unit3",
                "sp2",
                paths("unit1", "unit2", "unit4"),
                "FinalAction",
                "Destroy"
            )
        );
        List<InheritedRuleResponseModel> holdRules = Collections.emptyList();
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp2");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.CONFLICT);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).hasSize(1);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(0)).isInstanceOf(
            EliminationExtendedInfoAccessLinkInconsistency.class
        );
        EliminationExtendedInfoAccessLinkInconsistencyDetails accessLinkInconsistencyDetails =
            ((EliminationExtendedInfoAccessLinkInconsistency) eliminationAnalysisResult
                    .getExtendedInfo()
                    .get(0)).getDetails();

        assertThat(accessLinkInconsistencyDetails.getParentUnitId()).isEqualTo("unit2");
        assertThat(accessLinkInconsistencyDetails.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp2");
        assertThat(accessLinkInconsistencyDetails.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder(
            "sp1"
        );
    }

    @Test
    public void analyzeElimination_DestroyableUnitWithActiveHoldRule() {
        // Given : Destroyable unit (expired appraisal rule + Destroy appraisal FinalAction) + non expired HoldRule
        List<InheritedRuleResponseModel> appraisalRules = Collections.singletonList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "R1",
                ruleAttributes("2010-01-01", "2015-01-01")
            )
        );
        List<InheritedPropertyResponseModel> appraisalProperties = Collections.singletonList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy")
        );
        List<InheritedRuleResponseModel> holdRules = Collections.singletonList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "H1",
                ruleAttributes("2010-01-01", "2020-01-01")
            )
        );
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.CONFLICT);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).hasSize(1);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(0)).isInstanceOf(
            EliminationExtendedInfoHoldRule.class
        );
        EliminationExtendedInfoHoldRuleDetails extendedInfoHoldRuleDetails =
            ((EliminationExtendedInfoHoldRule) eliminationAnalysisResult.getExtendedInfo().get(0)).getDetails();
        assertThat(extendedInfoHoldRuleDetails.getHoldRuleIds()).containsExactlyInAnyOrder("H1");
    }

    @Test
    public void analyzeElimination_DestroyableUnitWithHoldRuleWithoutEndDate() {
        // Given : Destroyable unit (expired appraisal rule + Destroy appraisal FinalAction) + HoldRule without EndDate
        List<InheritedRuleResponseModel> appraisalRules = Collections.singletonList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "R1",
                ruleAttributes("2010-01-01", "2015-01-01")
            )
        );
        List<InheritedPropertyResponseModel> appraisalProperties = Collections.singletonList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy")
        );
        List<InheritedRuleResponseModel> holdRules = Collections.singletonList(
            new InheritedRuleResponseModel("unit1", "sp1", paths("unit1"), "H1", ruleAttributes("2010-01-01", null))
        );
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.CONFLICT);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).hasSize(1);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(0)).isInstanceOf(
            EliminationExtendedInfoHoldRule.class
        );
        EliminationExtendedInfoHoldRuleDetails extendedInfoHoldRuleDetails =
            ((EliminationExtendedInfoHoldRule) eliminationAnalysisResult.getExtendedInfo().get(0)).getDetails();
        assertThat(extendedInfoHoldRuleDetails.getHoldRuleIds()).containsExactlyInAnyOrder("H1");
    }

    @Test
    public void analyzeElimination_DestroyableUnitWithExpiredHoldRule() {
        // Given : Destroyable unit (expired appraisal rule + Destroy appraisal FinalAction) + expired HoldRule
        List<InheritedRuleResponseModel> appraisalRules = Collections.singletonList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "R1",
                ruleAttributes("2010-01-01", "2015-01-01")
            )
        );
        List<InheritedPropertyResponseModel> appraisalProperties = Collections.singletonList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy")
        );
        List<InheritedRuleResponseModel> holdRules = Collections.singletonList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "H1",
                ruleAttributes("2010-01-01", "2011-01-01")
            )
        );
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp1");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.DESTROY);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_NonDestroyableUnitWithHoldRule() {
        // Given : No Appraisal rule & properties + active HoldRule ==> KEEP (KEEP wins over hold rule conflicts)
        List<InheritedRuleResponseModel> appraisalRules = Collections.emptyList();
        List<InheritedPropertyResponseModel> appraisalProperties = Collections.emptyList();
        List<InheritedRuleResponseModel> holdRules = Collections.singletonList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "H1",
                ruleAttributes("2010-01-01", "2020-01-01")
            )
        );
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.KEEP);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    @Test
    public void analyzeElimination_ComplexTest() {
        // Given :
        //  - SP1 destroyable
        //  - SP2 destroyable
        //  - SP3 non destroyable
        //  - Access link inconsistency via parent unit : SP1 non destroyable & SP2 destroyable
        //  - Active Hold

        List<InheritedRuleResponseModel> appraisalRules = Arrays.asList(
            new InheritedRuleResponseModel(
                "unit5",
                "sp1",
                paths("unit1", "unit2", "unit5"),
                "R1",
                ruleAttributes("2010-01-01", "2015-01-01")
            ),
            new InheritedRuleResponseModel(
                "unit4",
                "sp2",
                paths("unit1", "unit2", "unit4"),
                "R2",
                ruleAttributes("2010-01-01", "2015-01-01")
            ),
            new InheritedRuleResponseModel(
                "unit7",
                "sp1",
                paths("unit7"),
                "R3",
                ruleAttributes("2010-01-01", "2015-01-01")
            )
        );

        List<InheritedPropertyResponseModel> appraisalProperties = Arrays.asList(
            new InheritedPropertyResponseModel("unit1", "sp1", paths("unit1"), "FinalAction", "Destroy"),
            new InheritedPropertyResponseModel(
                "unit3",
                "sp2",
                paths("unit1", "unit2", "unit3"),
                "FinalAction",
                "Destroy"
            ),
            new InheritedPropertyResponseModel("unit6", "sp3", paths("unit6"), "FinalAction", "Keep")
        );
        List<InheritedRuleResponseModel> holdRules = Collections.singletonList(
            new InheritedRuleResponseModel(
                "unit1",
                "sp1",
                paths("unit1"),
                "H1",
                ruleAttributes("2010-01-01", "2020-01-01")
            )
        );

        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.INGEST,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder(
            "sp1",
            "sp2"
        );
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp3");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.CONFLICT);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).hasSize(3);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(0)).isInstanceOf(
            EliminationExtendedInfoKeepAccessSp.class
        );
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(1)).isInstanceOf(
            EliminationExtendedInfoAccessLinkInconsistency.class
        );
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(2)).isInstanceOf(
            EliminationExtendedInfoHoldRule.class
        );

        EliminationExtendedInfoAccessLinkInconsistencyDetails accessLinkInconsistencyDetails =
            ((EliminationExtendedInfoAccessLinkInconsistency) eliminationAnalysisResult
                    .getExtendedInfo()
                    .get(1)).getDetails();
        assertThat(accessLinkInconsistencyDetails.getParentUnitId()).isEqualTo("unit2");
        assertThat(accessLinkInconsistencyDetails.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp2");
        assertThat(accessLinkInconsistencyDetails.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder(
            "sp1"
        );

        EliminationExtendedInfoHoldRuleDetails holdExtendedInfoHoldRuleDetails =
            ((EliminationExtendedInfoHoldRule) eliminationAnalysisResult.getExtendedInfo().get(2)).getDetails();
        assertThat(holdExtendedInfoHoldRuleDetails.getHoldRuleIds()).containsExactlyInAnyOrder("H1");
    }

    @Test
    public void analyzeElimination_HoldingTreeUnit() {
        // Given : No appraisal rules & properties & no hold rules
        List<InheritedRuleResponseModel> appraisalRules = Collections.emptyList();
        List<InheritedPropertyResponseModel> appraisalProperties = Collections.emptyList();
        List<InheritedRuleResponseModel> holdRules = Collections.emptyList();
        LocalDate expirationDate = LocalDate.parse("2018-01-01");
        String sp1 = "sp1";

        // When
        EliminationAnalysisService instance = new EliminationAnalysisService();
        EliminationAnalysisResult eliminationAnalysisResult = instance.analyzeElimination(
            "unit1",
            UnitType.HOLDING_UNIT,
            OPERATION_ID,
            appraisalRules,
            appraisalProperties,
            holdRules,
            expirationDate,
            sp1
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).isEmpty();
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.DESTROY);
        assertThat(eliminationAnalysisResult.getExtendedInfo()).isEmpty();
    }

    private List<List<String>> paths(String... ids) {
        return Collections.singletonList(Arrays.asList(ids));
    }

    private Map<String, Object> ruleAttributes(String startDate, String endDate) {
        Map<String, Object> attributes = new HashMap<>();
        if (startDate != null) {
            attributes.put(RuleModel.START_DATE, startDate);
        }
        if (endDate != null) {
            attributes.put(RuleModel.END_DATE, endDate);
        }
        return attributes;
    }
}
