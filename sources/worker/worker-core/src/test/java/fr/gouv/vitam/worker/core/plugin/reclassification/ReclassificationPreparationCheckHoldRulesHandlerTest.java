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

package fr.gouv.vitam.worker.core.plugin.reclassification;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.rules.InheritedRuleCategoryResponseModel;
import fr.gouv.vitam.common.model.rules.InheritedRuleResponseModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationEventDetails;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationOrders;
import fr.gouv.vitam.worker.core.plugin.reclassification.utils.UnitGraphInfoLoader;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ReclassificationPreparationCheckHoldRulesHandlerTest {

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private UnitGraphInfoLoader unitGraphInfoLoader;

    @Mock
    private HandlerIO handlerIO;

    private WorkerParameters parameters;

    private ReclassificationPreparationCheckHoldRulesHandler instance;

    @Before
    public void init() throws Exception {
        doReturn(metaDataClient).when(metaDataClientFactory).getClient();

        int tenant = 0;
        String operationId = GUIDFactory.newRequestIdGUID(tenant).toString();
        String objectId = GUIDFactory.newGUID().toString();
        parameters = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(operationId)
            .setObjectNameList(Lists.newArrayList(objectId))
            .setObjectName(objectId)
            .setCurrentStep("StepName");

        instance = new ReclassificationPreparationCheckHoldRulesHandler(metaDataClientFactory, unitGraphInfoLoader, 10);

        ReclassificationOrders reclassificationOrders = buildReclassificationOrders();
        doReturn(reclassificationOrders)
            .when(handlerIO)
            .getInput(ReclassificationPreparationCheckHoldRulesHandler.RECLASSIFICATION_ORDERS_PARAMETER_RANK);
    }

    @Test
    public void testCheckPreventInheritanceWhenEmptyRuleSetThenOK() throws Exception {
        // Given
        Map<String, InheritedRuleCategoryResponseModel> inheritedRules = Map.of(
            "unit1",
            new InheritedRuleCategoryResponseModel(Collections.emptyList(), Collections.emptyList()),
            "unit2",
            new InheritedRuleCategoryResponseModel(Collections.emptyList(), Collections.emptyList()),
            "unit3",
            new InheritedRuleCategoryResponseModel(Collections.emptyList(), Collections.emptyList())
        );

        doReturn(inheritedRules)
            .when(unitGraphInfoLoader)
            .loadInheritedHoldRules(metaDataClient, Set.of("unit1", "unit2", "unit3"));

        // When
        ItemStatus itemStatus = instance.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        verify(unitGraphInfoLoader).loadInheritedHoldRules(metaDataClient, Set.of("unit1", "unit2", "unit3"));
        verifyNoMoreInteractions(unitGraphInfoLoader);
    }

    @Test
    public void testCheckPreventInheritanceWhenExpiredHoldRulesThenOK() throws Exception {
        // Given
        Map<String, InheritedRuleCategoryResponseModel> inheritedRules = Map.of(
            "unit1",
            new InheritedRuleCategoryResponseModel(
                Arrays.asList(
                    buildHoldRule("unit1", "R1", "2010-12-31", Boolean.TRUE),
                    buildHoldRule("unit1", "R2", "2010-12-31", Boolean.FALSE),
                    buildHoldRule("unit1", "R3", "2010-12-31", null)
                ),
                Collections.emptyList()
            ),
            "unit2",
            new InheritedRuleCategoryResponseModel(
                Arrays.asList(
                    buildHoldRule("unit2", "R4", today(), Boolean.TRUE),
                    buildHoldRule("unit2", "R5", today(), Boolean.FALSE),
                    buildHoldRule("unit2", "R6", today(), null)
                ),
                Collections.emptyList()
            ),
            "unit3",
            new InheritedRuleCategoryResponseModel(Collections.emptyList(), Collections.emptyList())
        );

        doReturn(inheritedRules)
            .when(unitGraphInfoLoader)
            .loadInheritedHoldRules(metaDataClient, Set.of("unit1", "unit2", "unit3"));

        // When
        ItemStatus itemStatus = instance.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        verify(unitGraphInfoLoader).loadInheritedHoldRules(metaDataClient, Set.of("unit1", "unit2", "unit3"));
        verifyNoMoreInteractions(unitGraphInfoLoader);
    }

    @Test
    public void testCheckPreventInheritanceWhenActiveHoldRulesWithoutPreventRearrangementThenOK() throws Exception {
        // Given
        Map<String, InheritedRuleCategoryResponseModel> inheritedRules = Map.of(
            "unit1",
            new InheritedRuleCategoryResponseModel(
                Arrays.asList(
                    buildHoldRule("unit1", "R1", "2099-12-31", Boolean.FALSE),
                    buildHoldRule("unit1", "R2", "2099-12-31", null)
                ),
                Collections.emptyList()
            ),
            "unit2",
            new InheritedRuleCategoryResponseModel(
                Arrays.asList(
                    buildHoldRule("unit2", "R3", null, Boolean.FALSE),
                    buildHoldRule("unit2", "R4", null, null)
                ),
                Collections.emptyList()
            ),
            "unit3",
            new InheritedRuleCategoryResponseModel(
                Arrays.asList(
                    buildHoldRule("unit3", "R5", tomorrow(), Boolean.FALSE),
                    buildHoldRule("unit3", "R6", tomorrow(), null)
                ),
                Collections.emptyList()
            )
        );

        doReturn(inheritedRules)
            .when(unitGraphInfoLoader)
            .loadInheritedHoldRules(metaDataClient, Set.of("unit1", "unit2", "unit3"));

        // When
        ItemStatus itemStatus = instance.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        verify(unitGraphInfoLoader).loadInheritedHoldRules(metaDataClient, Set.of("unit1", "unit2", "unit3"));
        verifyNoMoreInteractions(unitGraphInfoLoader);
    }

    @Test
    public void testCheckPreventInheritanceWhenActiveHoldRulesWithPreventRearrangementThenOK() throws Exception {
        // Given
        Map<String, InheritedRuleCategoryResponseModel> inheritedRules = Map.of(
            "unit1",
            new InheritedRuleCategoryResponseModel(
                Arrays.asList(
                    buildHoldRule("unit1", "R1", "2099-12-31", Boolean.FALSE),
                    buildHoldRule("unit1", "R2", "2099-12-31", Boolean.TRUE)
                ),
                Collections.emptyList()
            ),
            "unit2",
            new InheritedRuleCategoryResponseModel(
                Arrays.asList(
                    buildHoldRule("unit2", "R3", null, Boolean.FALSE),
                    buildHoldRule("unit2", "R4", null, null)
                ),
                Collections.emptyList()
            ),
            "unit3",
            new InheritedRuleCategoryResponseModel(
                Arrays.asList(
                    buildHoldRule("unit3", "R5", tomorrow(), Boolean.TRUE),
                    buildHoldRule("unit3", "R6", tomorrow(), null)
                ),
                Collections.emptyList()
            )
        );

        doReturn(inheritedRules)
            .when(unitGraphInfoLoader)
            .loadInheritedHoldRules(metaDataClient, Set.of("unit1", "unit2", "unit3"));

        // When
        ItemStatus itemStatus = instance.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(itemStatus.getEvDetailData()).isNotNull();
        ReclassificationEventDetails eventDetails = JsonHandler.getFromString(
            itemStatus.getEvDetailData(),
            ReclassificationEventDetails.class
        );
        assertThat(eventDetails.getUnitsBlockedByHoldRules()).containsExactly("unit1", "unit3");
        assertThat(eventDetails.getError()).isEqualTo(
            ReclassificationPreparationCheckHoldRulesHandler.RECLASSIFICATION_BLOCKED_BY_HOLD_RULES
        );

        verify(unitGraphInfoLoader).loadInheritedHoldRules(metaDataClient, Set.of("unit1", "unit2", "unit3"));
        verifyNoMoreInteractions(unitGraphInfoLoader);
    }

    @Test
    public void testCheckPreventInheritanceWhenUnitNotFoundThenFatal() throws Exception {
        // Given
        Map<String, InheritedRuleCategoryResponseModel> inheritedRules = Map.of(
            "unit1",
            new InheritedRuleCategoryResponseModel(Collections.emptyList(), Collections.emptyList()),
            "unit3",
            new InheritedRuleCategoryResponseModel(Collections.emptyList(), Collections.emptyList())
        );

        doReturn(inheritedRules)
            .when(unitGraphInfoLoader)
            .loadInheritedHoldRules(metaDataClient, Set.of("unit1", "unit2", "unit3"));

        // When
        ItemStatus itemStatus = instance.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(itemStatus.getEvDetailData()).isNotNull();
        ReclassificationEventDetails eventDetails = JsonHandler.getFromString(
            itemStatus.getEvDetailData(),
            ReclassificationEventDetails.class
        );
        assertThat(eventDetails.getNotFoundUnits()).containsExactly("unit2");
        assertThat(eventDetails.getError()).isEqualTo(
            ReclassificationPreparationCheckHoldRulesHandler.COULD_NOT_FIND_UNITS_INHERITED_RULES
        );

        verify(unitGraphInfoLoader).loadInheritedHoldRules(metaDataClient, Set.of("unit1", "unit2", "unit3"));
        verifyNoMoreInteractions(unitGraphInfoLoader);
    }

    private ReclassificationOrders buildReclassificationOrders() {
        HashSetValuedHashMap<String, String> childToParentAttachments = new HashSetValuedHashMap<>();
        childToParentAttachments.put("unit1", "unit2");
        childToParentAttachments.put("unit1", "unit3");
        HashSetValuedHashMap<String, String> childToParentDetachments = new HashSetValuedHashMap<>();
        childToParentDetachments.put("unit2", "unit4");
        childToParentDetachments.put("unit3", "unit5");
        return new ReclassificationOrders(childToParentAttachments, childToParentDetachments);
    }

    private InheritedRuleResponseModel buildHoldRule(
        String unitId,
        String ruleId,
        String expirationDate,
        Boolean preventRearrangement
    ) {
        Map<String, Object> ruleAttributes = new HashMap<>();
        ruleAttributes.put(RuleModel.END_DATE, expirationDate);
        if (preventRearrangement != null) {
            ruleAttributes.put(RuleModel.PREVENT_REARRANGEMENT, preventRearrangement);
        }
        return new InheritedRuleResponseModel(unitId, "sp", List.of(List.of(unitId)), ruleId, ruleAttributes);
    }

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String tomorrow() {
        return LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
