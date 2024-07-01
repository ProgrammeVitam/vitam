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

package fr.gouv.vitam.worker.core.plugin.massprocessing;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.model.administration.RuleMeasurementEnum.YEAR;
import static fr.gouv.vitam.common.model.administration.RuleType.AppraisalRule;
import static fr.gouv.vitam.common.model.administration.RuleType.HoldRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

public class UnitMetadataRulesUpdateCheckConsistencyTest {

    public static final String DELETE_PREVENT_RULES_ID_TO_REMOVE_WRONG_RULE_CATEGORY_JSON =
        "UnitMetadataRulesUpdateCheckConsistency/deletePreventRulesIdToRemoveWrongRuleCategory.json";
    public static final String DELETE_PREVENT_RULES_ID_TO_REMOVE_UNKNOWN_RULE_ID_JSON =
        "UnitMetadataRulesUpdateCheckConsistency/deletePreventRulesIdToRemoveUnknownRuleId.json";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AdminManagementClient adminManagementClient;

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    private HandlerIO handlerIO;

    private UnitMetadataRulesUpdateCheckConsistency unitMetadataRulesUpdateCheckConsistency;
    private DefaultWorkerParameters workerParameters;

    private static final String PREVENT_RULES_ID_TO_REMOVE =
        "UnitMetadataRulesUpdateCheckConsistency/deletePreventRulesIdToRemove.json";
    private static final String PREVENT_RULES_ID_TO_ADD =
        "UnitMetadataRulesUpdateCheckConsistency/addPreventRulesIdToDelete.json";

    @Before
    public void init() throws Exception {
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
        unitMetadataRulesUpdateCheckConsistency = new UnitMetadataRulesUpdateCheckConsistency(
            adminManagementClientFactory
        );
        workerParameters = WorkerParametersFactory.newWorkerParameters(
            "procId",
            "stepId",
            "container",
            "step",
            Collections.singletonList("id"),
            "url",
            "url"
        );

        initializeRuleReferential();
    }

    private void initializeRuleReferential()
        throws FileRulesException, InvalidParseOperationException, AdminManagementClientServerException {
        doThrow(new FileRulesNotFoundException("NOT FOUND")).when(adminManagementClient).getRuleByID(anyString());

        List<FileRulesModel> knownRules = Arrays.asList(
            new FileRulesModel("APP-00001", AppraisalRule, "Val", "Desc", "1", YEAR),
            new FileRulesModel("APP-00002", AppraisalRule, "Val", "Desc", "10", YEAR),
            new FileRulesModel("HOL-00001", HoldRule, "Val", "Desc", "1", YEAR),
            new FileRulesModel("HOL-00002", HoldRule, "Val", "Desc", null, null),
            new FileRulesModel("HOL-00003", HoldRule, "Val", "Desc", "10", YEAR),
            new FileRulesModel("HOL-00004", HoldRule, "Val", "Desc", "unlimited", YEAR)
        );

        for (FileRulesModel rule : knownRules) {
            doReturn(new RequestResponseOK<FileRulesModel>().addResult(rule).toJsonNode())
                .when(adminManagementClient)
                .getRuleByID((rule.getRuleId()));
        }
    }

    @Test
    public void addAppraisalRuleKoInvalidFieldDeleteHoldRuleField() throws Exception {
        // Given
        givenRuleActions(
            "UnitMetadataRulesUpdateCheckConsistency/addAppraisalRuleKoInvalidFieldDeleteHoldRuleField.json"
        );

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_UNEXPECTED_FIELD"
        );
    }

    @Test
    public void addAppraisalRuleKoInvalidFieldDeleteStartDate() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addAppraisalRuleKoInvalidFieldDeleteStartDate.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_UNEXPECTED_FIELD"
        );
    }

    @Test
    public void addAppraisalRuleKoInvalidHoldRuleField() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addAppraisalRuleKoInvalidHoldRuleField.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_UNEXPECTED_FIELD"
        );
    }

    @Test
    public void addAppraisalRuleOkFull() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addAppraisalRuleOkFull.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void addHoldRuleOkMinimal() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleOkMinimal.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void addHoldRuleOkFull() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleOkFull.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void addHoldRuleKoUnknownRuleInReferential() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoUnknownRuleInReferential.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNITS_RULES_UNKNOWN"
        );
    }

    @Test
    public void addHoldRuleKoWrongRuleCategory() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoWrongRuleCategory.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNITS_RULES_INCONSISTENCY"
        );
    }

    @Test
    public void addHoldRuleKoInvalidFieldDeleteHoldEndDate() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoInvalidFieldDeleteHoldEndDate.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_UNEXPECTED_FIELD"
        );
    }

    @Test
    public void addHoldRuleKoInvalidFieldDeleteHoldOwner() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoInvalidFieldDeleteHoldOwner.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_UNEXPECTED_FIELD"
        );
    }

    @Test
    public void addHoldRuleKoInvalidFieldDeleteHoldReason() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoInvalidFieldDeleteHoldReason.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_UNEXPECTED_FIELD"
        );
    }

    @Test
    public void addHoldRuleKoInvalidFieldDeleteHoldReassessingDate() throws Exception {
        // Given
        givenRuleActions(
            "UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoInvalidFieldDeleteHoldReassessingDate.json"
        );

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_UNEXPECTED_FIELD"
        );
    }

    @Test
    public void addHoldRuleKoInvalidFieldDeletePreventRearrangement() throws Exception {
        // Given
        givenRuleActions(
            "UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoInvalidFieldDeletePreventRearrangement.json"
        );

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_UNEXPECTED_FIELD"
        );
    }

    @Test
    public void addHoldRuleKoInvalidFieldDeleteStartDate() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoInvalidFieldDeleteStartDate.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_UNEXPECTED_FIELD"
        );
    }

    @Test
    public void addHoldRuleKoInvalidFieldEndDate() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoInvalidFieldEndDate.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_UNEXPECTED_FIELD"
        );
    }

    @Test
    public void addHoldRuleKoMissingFieldRule() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoMissingFieldRule.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_MISSING_MANDATORY_FIELD"
        );
    }

    @Test
    public void addHoldRuleKoInvalidFieldFormatHoldEndDate() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoInvalidFieldFormatHoldEndDate.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_METADATA_UPDATE_CHECK_RULES_DATE_WRONG_FORMAT"
        );
    }

    @Test
    public void addHoldRuleKoInvalidFieldFormatHoldReassessingDate() throws Exception {
        // Given
        givenRuleActions(
            "UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoInvalidFieldFormatHoldReassessingDate.json"
        );

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_METADATA_UPDATE_CHECK_RULES_DATE_WRONG_FORMAT"
        );
    }

    @Test
    public void addHoldRuleKoInvalidFieldFormatStartDate() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoInvalidFieldFormatStartDate.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_METADATA_UPDATE_CHECK_RULES_DATE_WRONG_FORMAT"
        );
    }

    @Test
    public void addHoldRuleKoInvalidFieldLimitStartDate() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoInvalidFieldLimitStartDate.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_METADATA_UPDATE_CHECK_RULES_DATE_UNAUTHORIZED"
        );
    }

    @Test
    public void addHoldRuleKoInvalidEmptyFieldHoldOwner() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoInvalidEmptyFieldHoldOwner.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_EMPTY_FIELD"
        );
    }

    @Test
    public void addHoldRuleKoInvalidEmptyFieldHoldReason() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/addHoldRuleKoInvalidEmptyFieldHoldReason.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_EMPTY_FIELD"
        );
    }

    @Test
    public void updateAppraisalRuleKoDeleteHoldRuleFields() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateAppraisalRuleKoDeleteHoldRuleFields.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_UNEXPECTED_FIELD"
        );
    }

    @Test
    public void updateAppraisalRuleKoSetAndUnsetStartDate() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateAppraisalRuleKoSetAndUnsetStartDate.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNITS_RULES_INCONSISTENCY"
        );
    }

    @Test
    public void updateAppraisalRuleKoSetHoldRuleFields() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateAppraisalRuleKoSetHoldRuleFields.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_UNEXPECTED_FIELD"
        );
    }

    @Test
    public void updateAppraisalRuleOkDeleteFields() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateAppraisalRuleOkDeleteFields.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void updateAppraisalRuleOkFullUpdateFields() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateAppraisalRuleOkFullUpdateFields.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void updateHoldRuleKoMissingOldRule() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoMissingOldRule.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_MISSING_MANDATORY_FIELD"
        );
    }

    @Test
    public void updateHoldRuleKoEmptyUpdateRequest() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoEmptyUpdateRequest.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_NOT_EXPECTED_FIELD"
        );
    }

    @Test
    public void updateHoldRuleKoSetAndDeleteHoldEndDate() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoSetAndDeleteHoldEndDate.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNITS_RULES_INCONSISTENCY"
        );
    }

    @Test
    public void updateHoldRuleKoSetAndDeleteHoldOwner() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoSetAndDeleteHoldOwner.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNITS_RULES_INCONSISTENCY"
        );
    }

    @Test
    public void updateHoldRuleKoSetAndDeleteHoldReason() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoSetAndDeleteHoldReason.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNITS_RULES_INCONSISTENCY"
        );
    }

    @Test
    public void updateHoldRuleKoSetAndDeleteHoldReassessingDate() throws Exception {
        // Given
        givenRuleActions(
            "UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoSetAndDeleteHoldReassessingDate.json"
        );

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNITS_RULES_INCONSISTENCY"
        );
    }

    @Test
    public void updateHoldRuleKoSetAndDeletePreventRearrangement() throws Exception {
        // Given
        givenRuleActions(
            "UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoSetAndDeletePreventRearrangement.json"
        );

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNITS_RULES_INCONSISTENCY"
        );
    }

    @Test
    public void updateHoldRuleKoSetAndUnsetStartDate() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoSetAndUnsetStartDate.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNITS_RULES_INCONSISTENCY"
        );
    }

    @Test
    public void updateHoldRuleKoInvalidFieldFormatHoldEndDate() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoInvalidFieldFormatHoldEndDate.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_METADATA_UPDATE_CHECK_RULES_DATE_WRONG_FORMAT"
        );
    }

    @Test
    public void updateHoldRuleKoInvalidFieldFormatHoldReassessingDate() throws Exception {
        // Given
        givenRuleActions(
            "UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoInvalidFieldFormatHoldReassessingDate.json"
        );

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_METADATA_UPDATE_CHECK_RULES_DATE_WRONG_FORMAT"
        );
    }

    @Test
    public void updateHoldRuleKoInvalidFieldFormatStartDate() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoInvalidFieldFormatStartDate.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_METADATA_UPDATE_CHECK_RULES_DATE_WRONG_FORMAT"
        );
    }

    @Test
    public void updateHoldRuleKoInvalidFieldLimitStartDate() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoInvalidFieldLimitStartDate.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_METADATA_UPDATE_CHECK_RULES_DATE_UNAUTHORIZED"
        );
    }

    @Test
    public void updateHoldRuleKoInvalidEmptyFieldHoldOwner() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoInvalidEmptyFieldHoldOwner.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_EMPTY_FIELD"
        );
    }

    @Test
    public void updateHoldRuleKoInvalidEmptyFieldHoldReason() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoInvalidEmptyFieldHoldReason.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_EMPTY_FIELD"
        );
    }

    @Test
    public void updateHoldRuleKoInvalidFieldEndDate() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleKoInvalidFieldEndDate.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNIT_RULES_NOT_EXPECTED_FIELD"
        );
    }

    @Test
    public void updateHoldRuleOkFullRemoveFields() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleOkFullRemoveFields.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void updateHoldRuleOkFullUpdateFields() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/updateHoldRuleOkFullUpdateFields.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void deleteAppraisalRuleOk() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/deleteAppraisalRuleOk.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void deleteHoldRuleCategoryOk() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/deleteHoldRuleCategoryOk.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void deleteHoldRuleOk() throws Exception {
        // Given
        givenRuleActions("UnitMetadataRulesUpdateCheckConsistency/deleteHoldRuleOk.json");

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void testAddPreventRulesIdOK() throws Exception {
        // Given
        givenRuleActions(PREVENT_RULES_ID_TO_ADD);

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void givenPreventRuleIdsToRemoveWithValidRuleIdsThenOK() throws Exception {
        // Given
        givenRuleActions(PREVENT_RULES_ID_TO_REMOVE);

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void givenPreventRuleIdsToRemoveWithUnknownRuleIdsThenKO() throws Exception {
        // Given
        givenRuleActions(DELETE_PREVENT_RULES_ID_TO_REMOVE_UNKNOWN_RULE_ID_JSON);

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNITS_RULES_UNKNOWN"
        );
    }

    @Test
    public void givenPreventRuleIdsToRemoveWithWrongRuleCategoryThenKO() throws Exception {
        // Given
        givenRuleActions(DELETE_PREVENT_RULES_ID_TO_REMOVE_WRONG_RULE_CATEGORY_JSON);

        // When
        ItemStatus itemStatus = unitMetadataRulesUpdateCheckConsistency.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(JsonHandler.getFromString(itemStatus.getEvDetailData()).get("Code").asText()).isEqualTo(
            "UNITS_RULES_INCONSISTENCY"
        );
    }

    private JsonNode givenRuleActions(String queryFile)
        throws ProcessingException, InvalidParseOperationException, FileNotFoundException {
        return doReturn(JsonHandler.getFromString(PropertiesUtils.getResourceAsString(queryFile)))
            .when(handlerIO)
            .getJsonFromWorkspace("actions.json");
    }
}
