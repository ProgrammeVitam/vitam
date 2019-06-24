/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin.massprocessing.management;

import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.massupdate.RuleActions;
import fr.gouv.vitam.common.model.massupdate.RuleCategoryAction;
import fr.gouv.vitam.worker.core.plugin.massprocessing.MassUpdateErrorInfo;
import fr.gouv.vitam.worker.core.plugin.preservation.TestHandlerIO;
import fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.VitamConstants.TAG_RULE_APPRAISAL;
import static fr.gouv.vitam.common.model.VitamConstants.TAG_RULE_CLASSIFICATION;
import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class MassUpdateRulesCheckTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AlertService alertService;

    private MassUpdateRulesCheck massUpdateRulesCheck;

    private TestWorkerParameter EMPTY_WORKER_PARAMETER = workerParameterBuilder().build();

    private List<String> classificationLevels = Arrays.asList("Superman", "Joker");

    public static Condition<String> massUpdateErrorWithMessage(String error) {
        return new Condition<>(s -> {
            try {
                MassUpdateErrorInfo massUpdateError = JsonHandler.getFromString(s, MassUpdateErrorInfo.class);
                return massUpdateError.getError().equals(error);
            } catch (InvalidParseOperationException e) {
                throw new VitamRuntimeException(e);
            }
        }, String.format("%s JSON with Error: '%s'.", MassUpdateErrorInfo.class.getSimpleName(), error));
    }

    @Before
    public void setUp() throws Exception {
        massUpdateRulesCheck = new MassUpdateRulesCheck(alertService, classificationLevels);
    }

    @Test
    public void should_return_KO_status_when_empty_action() {
        // Given
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("actions.json", JsonHandler.createObjectNode());

        // When
        ItemStatus itemStatus = massUpdateRulesCheck.execute(EMPTY_WORKER_PARAMETER, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(KO);
        assertThat(itemStatus.getEvDetailData()).is(massUpdateErrorWithMessage("RULE_ACTION_EMPTY"));
    }

    @Test
    public void should_return_KO_when_duplicate_rule_to_add() throws Exception {
        RuleActions ruleActions = new RuleActions();
        HashMap<String, RuleCategoryAction> addRules = new HashMap<>();
        addRules.put(TAG_RULE_CLASSIFICATION, new RuleCategoryAction());
        ruleActions.setAdd(Arrays.asList(addRules, addRules));                  // <- Here 2 ClassificationRule

        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("actions.json", JsonHandler.toJsonNode(ruleActions));

        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode());

        // When
        ItemStatus itemStatus = massUpdateRulesCheck.execute(EMPTY_WORKER_PARAMETER, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(KO);
    }

    @Test
    public void should_return_KO_when_duplicate_rule_to_update() throws Exception {
        RuleActions ruleActions = new RuleActions();
        HashMap<String, RuleCategoryAction> addRules = new HashMap<>();
        addRules.put(TAG_RULE_CLASSIFICATION, new RuleCategoryAction());
        ruleActions.setUpdate(Arrays.asList(addRules, addRules));                  // <- Here 2 ClassificationRule

        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("actions.json", JsonHandler.toJsonNode(ruleActions));

        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode());

        // When
        ItemStatus itemStatus = massUpdateRulesCheck.execute(EMPTY_WORKER_PARAMETER, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(KO);
    }

    @Test
    public void should_return_KO_when_duplicate_rule_to_update_add_delete() throws Exception {
        RuleActions ruleActions = new RuleActions();
        HashMap<String, RuleCategoryAction> addRules = new HashMap<>();
        addRules.put(TAG_RULE_CLASSIFICATION, new RuleCategoryAction());
        ruleActions.setUpdate(Collections.singletonList(addRules));               // <- Here 1 ClassificationRule
        ruleActions.setAdd(Collections.singletonList(addRules));                  // <- Here 1 ClassificationRule
        ruleActions.setDelete(Collections.singletonList(addRules));               // <- Here 1 ClassificationRule

        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("actions.json", JsonHandler.toJsonNode(ruleActions));

        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode());

        // When
        ItemStatus itemStatus = massUpdateRulesCheck.execute(EMPTY_WORKER_PARAMETER, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(KO);
    }

    @Test
    public void should_return_KO_status_when_unknown_add_classification_level() throws Exception {
        // Given
        RuleActions ruleActions = new RuleActions();
        HashMap<String, RuleCategoryAction> addRules = new HashMap<>();
        RuleCategoryAction categoryAction = new RuleCategoryAction();
        categoryAction.setClassificationLevel("Batman");                        // <- unknown classification level.
        addRules.put(TAG_RULE_CLASSIFICATION, categoryAction);
        ruleActions.setAdd(Collections.singletonList(addRules));

        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("actions.json", JsonHandler.toJsonNode(ruleActions));

        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode());

        // When
        ItemStatus itemStatus = massUpdateRulesCheck.execute(EMPTY_WORKER_PARAMETER, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(KO);
        verify(alertService).createAlert("Unknown classification level 'Batman' in added rule action.");
    }

    @Test
    public void should_return_KO_status_when_unknown_update_classification_level() throws Exception {
        // Given
        RuleActions ruleActions = new RuleActions();
        HashMap<String, RuleCategoryAction> addRules = new HashMap<>();
        RuleCategoryAction categoryAction = new RuleCategoryAction();
        categoryAction.setClassificationLevel("Batman");                        // <- unknown classification level.
        addRules.put(TAG_RULE_CLASSIFICATION, categoryAction);
        ruleActions.setUpdate(Collections.singletonList(addRules));

        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("actions.json", JsonHandler.toJsonNode(ruleActions));

        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode());

        // When
        ItemStatus itemStatus = massUpdateRulesCheck.execute(EMPTY_WORKER_PARAMETER, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(KO);
        verify(alertService).createAlert("Unknown classification level 'Batman' in updated rule action.");
    }

    @Test
    public void should_return_KO_status_when_any_exception_occurs() {
        // Given
        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("actions.json", JsonHandler.createObjectNode().put("Batman", 1));

        // When
        ItemStatus itemStatus = massUpdateRulesCheck.execute(EMPTY_WORKER_PARAMETER, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(KO);
    }

    @Test
    public void should_return_OK_status() throws Exception {
        // Given
        RuleActions ruleActions = new RuleActions();
        HashMap<String, RuleCategoryAction> addRules = new HashMap<>();
        RuleCategoryAction categoryAction = new RuleCategoryAction();
        categoryAction.setClassificationLevel(classificationLevels.get(0));
        addRules.put(TAG_RULE_CLASSIFICATION, categoryAction);
        ruleActions.setUpdate(Collections.singletonList(addRules));

        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("actions.json", JsonHandler.toJsonNode(ruleActions));

        // When
        ItemStatus itemStatus = massUpdateRulesCheck.execute(EMPTY_WORKER_PARAMETER, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(OK);
    }

    @Test
    public void should_not_throw_in_null_pointer_when_no_add_classification_rule() throws Exception {
        // Given
        RuleActions ruleActions = new RuleActions();
        HashMap<String, RuleCategoryAction> addRules = new HashMap<>();
        RuleCategoryAction categoryAction = new RuleCategoryAction();
        addRules.put(TAG_RULE_APPRAISAL, categoryAction);
        ruleActions.setAdd(Collections.singletonList(addRules));

        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("actions.json", JsonHandler.toJsonNode(ruleActions));

        // When
        ItemStatus itemStatus = massUpdateRulesCheck.execute(EMPTY_WORKER_PARAMETER, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(OK);
    }

    @Test
    public void should_not_throw_in_null_pointer_when_no_update_classification_rule() throws Exception {
        // Given
        RuleActions ruleActions = new RuleActions();
        HashMap<String, RuleCategoryAction> addRules = new HashMap<>();
        RuleCategoryAction categoryAction = new RuleCategoryAction();
        addRules.put(TAG_RULE_APPRAISAL, categoryAction);
        ruleActions.setUpdate(Collections.singletonList(addRules));

        TestHandlerIO handlerIO = new TestHandlerIO();
        handlerIO.setJsonFromWorkspace("actions.json", JsonHandler.toJsonNode(ruleActions));

        // When
        ItemStatus itemStatus = massUpdateRulesCheck.execute(EMPTY_WORKER_PARAMETER, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(OK);
    }
}