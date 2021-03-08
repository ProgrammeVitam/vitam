/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.functional.administration.contract.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UnsetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.VersionUsageModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.ManagementContract;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.model.PreservationVersion.LAST;
import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.BINARY_MASTER;
import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.DISSEMINATION;
import static fr.gouv.vitam.common.model.administration.VersionUsageModel.IntermediaryVersionEnum.ALL;
import static fr.gouv.vitam.common.model.administration.VersionUsageModel.IntermediaryVersionEnum.NONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ManagementContractImplTest {

    private static final Integer TENANT_ID = 1;

    @Rule
    public MockitoRule mockitoJUnit = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());

    @Mock
    private MongoDbAccessAdminImpl mongoAccess;
    @Mock
    private VitamCounterService vitamCounterService;
    @Mock
    private StorageClient storageClient;
    @Mock
    private LogbookOperationsClient logbookOperationsClient;
    @Mock
    private FunctionalBackupService functionalBackupService;

    @Captor
    private ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor;
    @Captor
    private ArgumentCaptor<ArrayNode> contractsToPersistCaptor;

    private ContractService<ManagementContractModel> managementContractService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
        managementContractService = new ManagementContractImpl(mongoAccess, vitamCounterService, storageClient,
            logbookOperationsClient, functionalBackupService);

    }

    @Test
    @RunWithCustomExecutor
    public void when_create_should_insert_contracts_given_valid_not_slave_mode()
        throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_management_ok_no_identifiers.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(false);
        when(vitamCounterService.getNextSequenceAsString(eq(TENANT_ID), eq(SequenceType.MANAGEMENT_CONTRACT_SEQUENCE)))
            .thenReturn("MC-000001", "MC-000002", "MC-000003", "MC-000004", "MC-000005");
        when(mongoAccess.insertDocuments(any(ArrayNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isTrue();

        verify(mongoAccess, times(1)).insertDocuments(contractsToPersistCaptor.capture(),
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));
        ArrayNode contratsToPersistCalled = contractsToPersistCaptor.getValue();
        assertThat(contratsToPersistCalled).isNotNull();
        assertThat(contratsToPersistCalled.size()).isEqualTo(5);
        assertThat(contratsToPersistCalled.get(4)).isNotNull();
        assertThat(contratsToPersistCalled.get(4).get("Identifier").asText()).isEqualTo("MC-000005");
        assertThat(contratsToPersistCalled.get(4).get("VersionRetentionPolicy")).isNotNull();
        assertTrue(contratsToPersistCalled.get(4).get("VersionRetentionPolicy").get("InitialVersion").asBoolean());
        assertThat(contratsToPersistCalled.get(4).get("VersionRetentionPolicy").get("IntermediaryVersion").asText())
            .isEqualTo(LAST.toString());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
    }

    @Test
    @RunWithCustomExecutor
    public void when_create_should_init_fields_given_minimal_contract() throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_management_ok_minimal.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(false);
        when(vitamCounterService.getNextSequenceAsString(eq(TENANT_ID), eq(SequenceType.MANAGEMENT_CONTRACT_SEQUENCE)))
            .thenReturn("MC-000001");
        when(mongoAccess.insertDocuments(any(ArrayNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isTrue();

        verify(mongoAccess, times(1)).insertDocuments(contractsToPersistCaptor.capture(),
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));
        ArrayNode contratsToPersistCalled = contractsToPersistCaptor.getValue();
        assertThat(contratsToPersistCalled).isNotNull();
        assertThat(contratsToPersistCalled.size()).isEqualTo(1);
        assertThat(contratsToPersistCalled.get(0)).isNotNull();
        assertThat(contratsToPersistCalled.get(0).get("Identifier").asText()).isEqualTo("MC-000001");
        assertThat(contratsToPersistCalled.get(0).get("Status").asText()).isEqualTo("INACTIVE");
        assertThat(contratsToPersistCalled.get(0).get("ActivationDate").asText()).isNotNull();
        assertThat(contratsToPersistCalled.get(0).get("CreationDate").asText()).isNotNull();
        assertThat(contratsToPersistCalled.get(0).get("LastUpdate").asText()).isNotNull();
        assertThat(contratsToPersistCalled.get(0).get("VersionRetentionPolicy")).isNotNull();
        assertTrue(contratsToPersistCalled.get(0).get("VersionRetentionPolicy").get("InitialVersion").asBoolean());
        assertThat(contratsToPersistCalled.get(0).get("VersionRetentionPolicy").get("IntermediaryVersion").asText())
            .isEqualTo(LAST.toString());
    }

    @Test
    @RunWithCustomExecutor
    public void when_create_should_insert_contracts_given_identifier_present_in_slave_mode()
        throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_management_ok_identifiers.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(true);
        when(mongoAccess.insertDocuments(any(ArrayNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isTrue();
        verify(mongoAccess, times(1)).insertDocuments(contractsToPersistCaptor.capture(),
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));
        ArrayNode contratsToPersistCalled = contractsToPersistCaptor.getValue();
        assertThat(contratsToPersistCalled).isNotNull();
        assertThat(contratsToPersistCalled.size()).isEqualTo(5);
        assertThat(contratsToPersistCalled.get(2)).isNotNull();
        assertThat(contratsToPersistCalled.get(2).get("Identifier").asText()).isEqualTo("IdentifierMC3");
        assertThat(contratsToPersistCalled.get(2).get("VersionRetentionPolicy")).isNotNull();
        assertTrue(contratsToPersistCalled.get(2).get("VersionRetentionPolicy").get("InitialVersion").asBoolean());
        assertThat(contratsToPersistCalled.get(2).get("VersionRetentionPolicy").get("IntermediaryVersion").asText())
            .isEqualTo(LAST.toString());
    }

    @Test
    @RunWithCustomExecutor
    public void when_create_should_return_ok_given_empty_contrats() throws VitamException {
        // Given
        final List<ManagementContractModel> contractModelList = new ArrayList<>();

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isTrue();
        assertThat(((RequestResponseOK<ManagementContractModel>) response).getResults().size()).isEqualTo(0);

        verify(vitamCounterService, never()).isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID));
        verify(vitamCounterService, never()).getNextSequenceAsString(eq(TENANT_ID),
            eq(SequenceType.MANAGEMENT_CONTRACT_SEQUENCE));
        verify(mongoAccess, never()).insertDocuments(any(ArrayNode.class),
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));
        verify(storageClient, never()).getStorageStrategies();
        verify(logbookOperationsClient, never()).create(any());
        verify(logbookOperationsClient, never()).update(any());
    }

    @Test
    @RunWithCustomExecutor
    public void when_create_should_return_error_given_identifier_not_present_in_slave_mode()
        throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_management_ok_no_identifiers.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<List<ManagementContractModel>>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(true);
        when(mongoAccess.insertDocuments(any(ArrayNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isFalse();

        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(5);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.EMPTY_REQUIRED_FIELD.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription()).contains("Identifier")
            .contains("mandatory");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.EMPTY_REQUIRED_FIELD.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_create_should_return_error_given_technical_insertion_error()
        throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_management_ok_no_identifiers.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<List<ManagementContractModel>>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(false);
        when(vitamCounterService.getNextSequenceAsString(eq(TENANT_ID), eq(SequenceType.MANAGEMENT_CONTRACT_SEQUENCE)))
            .thenReturn("MC-000001");
        when(mongoAccess.insertDocuments(any(ArrayNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenThrow(new ReferentialException("creation error"));
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getDescription()).contains("Import management contracts error >");
        assertThat(((VitamError) response).getStatus()).isEqualTo(500);
        assertThat(((VitamError) response).getHttpCode())
            .isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
    }

    @Test
    @RunWithCustomExecutor
    public void when_create_should_return_error_given_contract_duplicate()
        throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_management_ok_identifiers.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<List<ManagementContractModel>>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(true);

        DbRequestResult dbRequestResultMock = mock(DbRequestResult.class);
        when(dbRequestResultMock.getCount()).thenReturn(1l);
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(dbRequestResultMock);
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(5);
        assertThat(((VitamError) response).getErrors().get(2).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.IDENTIFIER_DUPLICATION.KO");
        assertThat(((VitamError) response).getErrors().get(2).getDescription())
            .isEqualTo("The contract IdentifierMC3 already exists in database");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.IDENTIFIER_DUPLICATION.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_create_should_return_error_given_missing_name() throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_management_ko_missing_name.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(true);
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isFalse();

        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(1);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.EMPTY_REQUIRED_FIELD.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription())
            .isEqualTo("The field Name is mandatory");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.EMPTY_REQUIRED_FIELD.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_create_should_return_error_given_invalid_date() throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_management_ko_invalid_date.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<List<ManagementContractModel>>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(true);
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(1);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.EMPTY_REQUIRED_FIELD.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription())
            .isEqualTo("The field CreationDate is mandatory");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.EMPTY_REQUIRED_FIELD.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_create_should_return_error_given_id_present() throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_management_ok_identifiers.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<List<ManagementContractModel>>() {
            });
        contractModelList.get(0).setId("guid_fake");

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(true);
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(1);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("ManagementContract service error");
        assertThat(((VitamError) response).getErrors().get(0).getDescription())
            .isEqualTo("Id must be null when creating contracts (mcContract1)");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("ManagementContract service error");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventDetailData))
            .contains("managementContractCheck").contains("Id must be null when creating contracts (mcContract1)");

    }

    @Test
    @RunWithCustomExecutor
    public void when_create_should_return_error_given_strategy_not_found()
        throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_management_ko_strategies_not_found.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<List<ManagementContractModel>>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(true);
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isFalse();

        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(3);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription())
            .isEqualTo("Storage Strategy (default-not-found-unit) not found for the field Storage.UnitStrategy");
        assertThat(((VitamError) response).getErrors().get(1).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(1).getDescription()).isEqualTo(
            "Storage Strategy (default-not-found-got) not found for the field Storage.ObjectGroupStrategy");
        assertThat(((VitamError) response).getErrors().get(2).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(2).getDescription()).isEqualTo(
            "Storage Strategy (default-not-found-object) not found for the field Storage.ObjectStrategy");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_create_should_return_error_given_strategy_missing_referent_offer()
        throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts = PropertiesUtils
            .getResourceFile("contracts_management_ko_strategies_missing_referent.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<List<ManagementContractModel>>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(true);
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isFalse();

        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(2);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription()).isEqualTo(
            "Storage Strategy (fake-object) does not contains one and only one 'referent' offer for the field Storage.UnitStrategy");
        assertThat(((VitamError) response).getErrors().get(1).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(1).getDescription()).isEqualTo(
            "Storage Strategy (fake-invalid-md) does not contains one and only one 'referent' offer for the field Storage.ObjectGroupStrategy");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_create_should_return_error_given_storage_technical_error()
        throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_management_ok_identifiers.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<List<ManagementContractModel>>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(true);
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(storageClient.getStorageStrategies())
            .thenThrow(new StorageServerClientException("storage technical error"));

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isFalse();

        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(1);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription()).isEqualTo(
            "Exception while validating contract (mcContract1), Error checking storage : storage technical error");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_create_should_return_error_given_schema_error()
        throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_management_ok_no_identifiers.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<List<ManagementContractModel>>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(false);
        when(vitamCounterService.getNextSequenceAsString(eq(TENANT_ID), eq(SequenceType.MANAGEMENT_CONTRACT_SEQUENCE)))
            .thenReturn("MC-000001", "MC-000002", "MC-000003", "MC-000004", "MC-000005");
        when(mongoAccess.insertDocuments(any(ArrayNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenThrow(new SchemaValidationException("schema error test"));
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isFalse();

        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getDescription()).contains("Import management contracts error >");
        assertThat(((VitamError) response).getStatus()).isEqualTo(400);
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.BAD_REQUEST.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_update_should_update_contracts_given_valid_query()
        throws VitamException, InvalidCreateOperationException, FileNotFoundException {
        // Given
        final SetAction updateName = UpdateActionHelper.set("Name", "New name");

        // update Storage
        final SetAction updateStorageUnit = UpdateActionHelper.set("Storage.UnitStrategy", "fake-md");
        final SetAction updateStorageObjectGroup = UpdateActionHelper.set("Storage.ObjectGroupStrategy",
            "fake-md");
        final SetAction updateStorageObject = UpdateActionHelper.set("Storage.ObjectStrategy", "fake-object");

        // update version retention policy
        final SetAction updateDefaultInitialVersion =
            UpdateActionHelper.set("VersionRetentionPolicy.InitialVersion", true);
        final SetAction updateDefaultIntermediaryVersion =
            UpdateActionHelper.set("VersionRetentionPolicy.IntermediaryVersion", ALL.toString());
        VersionUsageModel binaryMasterUsage = new VersionUsageModel();
        binaryMasterUsage.setUsageName(BINARY_MASTER.getName());
        binaryMasterUsage.setInitialVersion(true);
        binaryMasterUsage.setIntermediaryVersion(ALL);

        VersionUsageModel dissemniationUsage = new VersionUsageModel();
        dissemniationUsage.setUsageName(DISSEMINATION.getName());
        dissemniationUsage.setInitialVersion(false);
        dissemniationUsage.setIntermediaryVersion(NONE);

        ObjectNode objectNode = JsonHandler.createObjectNode();
        objectNode.set("VersionRetentionPolicy.Usages",
            JsonHandler.toJsonNode(Arrays.asList(JsonHandler.toJsonNode(binaryMasterUsage),
                JsonHandler.toJsonNode(dissemniationUsage))));
        final SetAction updateUsages = UpdateActionHelper.set(objectNode);

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "IdentifierMC1"));
        update.addActions(updateName, updateStorageUnit, updateStorageObjectGroup, updateStorageObject,
            updateDefaultInitialVersion,
            updateDefaultIntermediaryVersion, updateUsages);

        DbRequestResult updateResult = new DbRequestResult();
        updateResult.setCount(1l);
        updateResult.setDiffs(Collections.singletonMap("mc_guid", Arrays.asList("Name: +New Name -Old Name")));
        when(mongoAccess.updateData(any(JsonNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(updateResult);

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        ArgumentCaptor<JsonNode> contractToUpdateCaptor = ArgumentCaptor.forClass(JsonNode.class);

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            update.getFinalUpdate());

        // Then
        assertThat(response.isOk()).isTrue();

        verify(mongoAccess, times(1)).updateData(contractToUpdateCaptor.capture(),
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));
        JsonNode contractToUpdateCalled = contractToUpdateCaptor.getValue();
        assertThat(contractToUpdateCalled).isNotNull();

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
    }

    @Test
    @RunWithCustomExecutor
    public void when_update_should_return_error_given_contract_not_exists()
        throws VitamException, InvalidCreateOperationException {
        // Given
        final SetAction updateName = UpdateActionHelper.set("Name", "New name");
        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "IdentifierMC1"));
        update.addActions(updateName);

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(0l);
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            update.getFinalUpdate());

        // Then
        verify(mongoAccess, times(0)).updateData(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getMessage()).isEqualTo("ManagementContract service error");
        assertThat(((VitamError) response).getDescription()).isEqualTo("Management contract update error");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat((response).getHttpCode()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());

        assertThat(((VitamError) response).getErrors().size()).isEqualTo(1);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.CONTRACT_NOT_FOUND.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription())
            .isEqualTo("Management contract not foundIdentifierMC1");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat((response).getHttpCode()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void when_update_should_return_error_given_query_null() throws VitamException {
        // Given

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            null);

        // Then
        verify(mongoAccess, times(0)).updateData(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));
        verify(logbookOperationsClient, never()).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, never()).update(logbookOperationParametersCaptor.capture());
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getMessage()).isEqualTo("ManagementContract service error");
        assertThat(((VitamError) response).getDescription()).isEqualTo("Management contract update error");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void when_update_should_return_error_given_invalid_status()
        throws VitamException, InvalidCreateOperationException {
        // Given
        final SetAction updateName = UpdateActionHelper.set("Status", "FAKE_STATUS");
        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "IdentifierMC1"));
        update.addActions(updateName);

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            update.getFinalUpdate());

        // Then
        verify(mongoAccess, times(0)).updateData(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(1);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.NOT_IN_ENUM.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription())
            .isEqualTo("The management contract status must be ACTIVE or INACTIVE but not FAKE_STATUS");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.NOT_IN_ENUM.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_update_should_return_error_given_strategy_not_found()
        throws VitamException, InvalidCreateOperationException, FileNotFoundException {
        // Given
        final SetAction updateStorageUnit = UpdateActionHelper.set("Storage.UnitStrategy", "default-not-found-unit");
        final SetAction updateStorageObjectGroup = UpdateActionHelper.set("Storage.ObjectGroupStrategy",
            "default-not-found-got");
        final SetAction updateStorageObject = UpdateActionHelper.set("Storage.ObjectStrategy",
            "default-not-found-object");
        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "IdentifierMC1"));
        update.addActions(updateStorageUnit, updateStorageObjectGroup, updateStorageObject);

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            update.getFinalUpdate());

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(3);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription())
            .isEqualTo("Storage Strategy (default-not-found-unit) not found for the field Storage.UnitStrategy");
        assertThat(((VitamError) response).getErrors().get(1).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(1).getDescription()).isEqualTo(
            "Storage Strategy (default-not-found-got) not found for the field Storage.ObjectGroupStrategy");
        assertThat(((VitamError) response).getErrors().get(2).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(2).getDescription()).isEqualTo(
            "Storage Strategy (default-not-found-object) not found for the field Storage.ObjectStrategy");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_update_should_return_error_given_strategy_missing_default_offer()
        throws VitamException, InvalidCreateOperationException, FileNotFoundException {
        // Given
        final SetAction updateStorageUnit = UpdateActionHelper.set("Storage.UnitStrategy", "fake-invalid-md");
        final SetAction updateStorageObjectGroup = UpdateActionHelper.set("Storage.ObjectGroupStrategy",
            "fake-invalid-md");
        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "IdentifierMC1"));
        update.addActions(updateStorageUnit, updateStorageObjectGroup);

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            update.getFinalUpdate());

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(2);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription()).isEqualTo(
            "Storage Strategy (fake-invalid-md) does not contains one and only one 'referent' offer for the field Storage.UnitStrategy");
        assertThat(((VitamError) response).getErrors().get(1).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(1).getDescription()).isEqualTo(
            "Storage Strategy (fake-invalid-md) does not contains one and only one 'referent' offer for the field Storage.ObjectGroupStrategy");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_update_should_return_error_given_storage_set_strategy_not_found()
        throws VitamException, FileNotFoundException {
        // Given
        String update =
            "{ \"$action\": [{ \"$set\": {  \"Storage\" : { \"UnitStrategy\" : \"default-not-found-unit\", \"ObjectGroupStrategy\" : \"default-not-found-got\", \"ObjectStrategy\" : \"default-not-found-object\" } } } ] }";

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            JsonHandler.getFromString(update));

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(3);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription())
            .isEqualTo("Storage Strategy (default-not-found-unit) not found for the field Storage.UnitStrategy");
        assertThat(((VitamError) response).getErrors().get(1).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(1).getDescription()).isEqualTo(
            "Storage Strategy (default-not-found-got) not found for the field Storage.ObjectGroupStrategy");
        assertThat(((VitamError) response).getErrors().get(2).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(2).getDescription()).isEqualTo(
            "Storage Strategy (default-not-found-object) not found for the field Storage.ObjectStrategy");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_update_should_return_error_given_storage_set_strategy_missing_default_offer()
        throws VitamException, FileNotFoundException {
        // Given
        String update =
            "{ \"$action\": [{ \"$set\": {  \"Storage\" : { \"UnitStrategy\" : \"fake-invalid-md\", \"ObjectGroupStrategy\" : \"fake-object\" } } } ] }";

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            JsonHandler.getFromString(update));

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(2);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription()).isEqualTo(
            "Storage Strategy (fake-invalid-md) does not contains one and only one 'referent' offer for the field Storage.UnitStrategy");
        assertThat(((VitamError) response).getErrors().get(1).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(1).getDescription()).isEqualTo(
            "Storage Strategy (fake-object) does not contains one and only one 'referent' offer for the field Storage.ObjectGroupStrategy");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.STRATEGY_VALIDATION_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_update_should_return_error_given_referential_technical_update_error()
        throws VitamException, InvalidCreateOperationException {
        // Given
        final SetAction updateName = UpdateActionHelper.set("Name", "New name");
        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "IdentifierMC1"));
        update.addActions(updateName);

        when(mongoAccess.updateData(any(JsonNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenThrow(new ReferentialException("error data update"));

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            update.getFinalUpdate());

        // Then
        verify(mongoAccess, times(1)).updateData(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));

        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getMessage()).isEqualTo("ManagementContract service error");
        assertThat(((VitamError) response).getDescription())
            .isEqualTo("Update management contract error > error data update");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode())
            .isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.FATAL");
    }

    @Test
    @RunWithCustomExecutor
    public void when_update_should_return_error_given_schema_technical_update_error()
        throws VitamException, InvalidCreateOperationException {
        // Given
        final SetAction updateName = UpdateActionHelper.set("Name", "New name");
        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "IdentifierMC1"));
        update.addActions(updateName);

        when(mongoAccess.updateData(any(JsonNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenThrow(new SchemaValidationException("mongo schema error"));

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            update.getFinalUpdate());

        // Then
        verify(mongoAccess, times(1)).updateData(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));

        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getMessage()).isEqualTo("ManagementContract service error");
        assertThat(((VitamError) response).getDescription())
            .isEqualTo("Update management contract error > mongo schema error");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.BAD_REQUEST.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_find_should_return_contracts_given_valid_query()
        throws VitamException, InvalidCreateOperationException {
        // Given
        final Select select = new Select();
        select.setQuery(QueryHelper.eq("Name", "mcContract1"));

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getRequestResponseOK(any(JsonNode.class), eq(ManagementContract.class),
            eq(ManagementContractModel.class)))
            .thenReturn(new RequestResponseOK<ManagementContractModel>()
                .addAllResults(Arrays.asList(getManagementContract("IdentifierMC1"))));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);

        ArgumentCaptor<JsonNode> queryCaptor = ArgumentCaptor.forClass(JsonNode.class);

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .findContracts(select.getFinalSelect());

        // Then
        assertThat(response.isOk()).isTrue();

        verify(mongoAccess, times(1)).findDocuments(queryCaptor.capture(),
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));
        JsonNode querySentToMongo = queryCaptor.getValue();
        assertThat(querySentToMongo).isNotNull();
        assertThat(querySentToMongo).isEqualTo(select.getFinalSelect());
    }

    @Test
    @RunWithCustomExecutor
    public void when_find_should_throw_exception_given_invalid_query() {
        assertThatThrownBy(() -> managementContractService.findContracts(null))
            .isInstanceOf(InvalidParseOperationException.class);

    }

    @Test
    @RunWithCustomExecutor
    public void when_find_by_identifier_should_return_contract_given_existing_identifier() throws VitamException {

        // Given
        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(findResultMock.getRequestResponseOK(any(JsonNode.class), eq(ManagementContract.class),
            eq(ManagementContractModel.class)))
            .thenReturn(new RequestResponseOK<ManagementContractModel>()
                .addAllResults(Arrays.asList(getManagementContract("IdentifierMC1"))));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);

        // When
        ManagementContractModel response = managementContractService.findByIdentifier("IdentifierMC1");

        // Then
        assertThat(response).isNotNull();
        verify(mongoAccess, times(1)).findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));
    }

    @Test
    @RunWithCustomExecutor
    public void when_find_by_identifier_should_return_null_given_no_result() throws VitamException {

        // Given
        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList());
        when(findResultMock.getRequestResponseOK(any(JsonNode.class), eq(ManagementContract.class),
            eq(ManagementContractModel.class)))
            .thenReturn(new RequestResponseOK<ManagementContractModel>()
                .addAllResults(Arrays.asList(getManagementContract("IdentifierMC1"))));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);

        // When
        ManagementContractModel response = managementContractService.findByIdentifier("IdentifierMC1");

        // Then
        assertThat(response).isNull();
        verify(mongoAccess, times(1)).findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));
    }

    @Test
    @RunWithCustomExecutor
    public void when_find_by_identifier_should_throw_exception_given_invalid_identifier() {
        assertThatThrownBy(() -> managementContractService.findByIdentifier("&lt;script&gt;"))
            .isInstanceOf(InvalidParseOperationException.class);

    }

    @Test
    @RunWithCustomExecutor
    public void when_create_contracts_should_insert_correct_version_retention_policy_usages()
        throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts =
            PropertiesUtils.getResourceFile("contracts_management_ok_version_retention_policy_usages.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(true);
        when(mongoAccess.insertDocuments(any(ArrayNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isTrue();
        verify(mongoAccess, times(1)).insertDocuments(contractsToPersistCaptor.capture(),
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));
        ArrayNode contratsToPersistCalled = contractsToPersistCaptor.getValue();
        assertThat(contratsToPersistCalled).isNotNull();
        assertThat(contratsToPersistCalled.size()).isEqualTo(2);
        assertThat(contratsToPersistCalled.get(0)).isNotNull();
        assertThat(contratsToPersistCalled.get(0).get("Identifier").asText()).isEqualTo("IdentifierMC1");
        assertThat(contratsToPersistCalled.get(0).get("VersionRetentionPolicy")).isNotNull();
        assertTrue(contratsToPersistCalled.get(0).get("VersionRetentionPolicy").get("InitialVersion").asBoolean());
        assertThat(contratsToPersistCalled.get(0).get("VersionRetentionPolicy").get("IntermediaryVersion").asText())
            .isEqualTo(LAST.toString());
        assertNotNull(contratsToPersistCalled.get(0).get("VersionRetentionPolicy").get("Usages"));

        List<VersionUsageModel> usages = JsonHandler.getFromJsonNode(
            contratsToPersistCalled.get(0).get("VersionRetentionPolicy").get("Usages"), new TypeReference<>() {
            });
        assertThat(usages.size()).isEqualTo(4);
    }

    @Test
    @RunWithCustomExecutor
    public void when_create_contracts_should_fail_incorrect_default_version_retention_policy()
        throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts =
            PropertiesUtils.getResourceFile("contracts_management_ko_default_version_retention_policy.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(true);
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat((response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(2);

        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription())
            .isEqualTo(
                "The version retention policy's InitialVersion parameter in Default usage is invalid in the contract mcContract1.");
        assertThat(((VitamError) response).getErrors().get(1).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(1).getDescription())
            .isEqualTo(
                "The version retention policy's IntermediaryVersion parameter in Default usage is invalid in the contract mcContract2.");


        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_create_contract_should_fail_incorrect_binaryMaster_version_retention_policy()
        throws VitamException, FileNotFoundException {
        // Given
        final File fileContracts =
            PropertiesUtils.getResourceFile("contracts_management_ko_binaryMaster_version_retention_policy.json");
        final List<ManagementContractModel> contractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
            new TypeReference<>() {
            });

        when(vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT), eq(TENANT_ID))).thenReturn(true);
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(new DbRequestResult());
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService
            .createContracts(contractModelList);

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat((response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(3);

        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription())
            .isEqualTo(
                "The version retention policy's InitialVersion parameter in BinaryMaster usage is invalid in the contract mcContract1.");
        assertThat(((VitamError) response).getErrors().get(1).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(1).getDescription())
            .isEqualTo(
                "The version retention policy's IntermediaryVersion parameter in BinaryMaster usage is invalid in the contract mcContract2.");
        assertThat(((VitamError) response).getErrors().get(2).getMessage())
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(2).getDescription())
            .isEqualTo(
                "The usage type testUsage is invalid in the contract mcContract3.");


        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_update_contracts_should_fail_invalid_default_version_retention_policy()
        throws VitamException, InvalidCreateOperationException, FileNotFoundException {
        // Given
        final SetAction updateName = UpdateActionHelper.set("Name", "New name");
        final SetAction updateDefaultInitialVersion =
            UpdateActionHelper.set("VersionRetentionPolicy.InitialVersion", false);
        final SetAction updateDefaultIntermediaryVersion =
            UpdateActionHelper.set("VersionRetentionPolicy.IntermediaryVersion", NONE.toString());

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "IdentifierMC1"));
        update.addActions(updateName, updateDefaultInitialVersion, updateDefaultIntermediaryVersion);

        DbRequestResult updateResult = new DbRequestResult();
        updateResult.setCount(1l);
        updateResult.setDiffs(Collections.singletonMap("mc_guid", Arrays.asList("Name: +New Name -Old Name")));
        when(mongoAccess.updateData(any(JsonNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(updateResult);

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            update.getFinalUpdate());

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat((response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(2);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription())
            .isEqualTo(
                "The version retention policy's InitialVersion parameter in Default usage is invalid in the contract New name.");

        assertThat(((VitamError) response).getErrors().get(1).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(1).getDescription())
            .isEqualTo("The version retention policy's IntermediaryVersion parameter in Default usage is invalid in the contract New name.");

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_update_contracts_should_fail_given_invalid_BinaryMaster_usage_version_retention_policy()
        throws VitamException, InvalidCreateOperationException, FileNotFoundException {
        // Given
        final SetAction updateName = UpdateActionHelper.set("Name", "New name");

        VersionUsageModel binaryMasterUsage = new VersionUsageModel();
        binaryMasterUsage.setUsageName(BINARY_MASTER.getName());
        binaryMasterUsage.setInitialVersion(false);
        binaryMasterUsage.setIntermediaryVersion(ALL);

        ObjectNode objectNode = JsonHandler.createObjectNode();
        objectNode.set("VersionRetentionPolicy.Usages",
            JsonHandler.toJsonNode(Arrays.asList(JsonHandler.toJsonNode(binaryMasterUsage))));
        final SetAction updateUsages = UpdateActionHelper.set(objectNode);

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "IdentifierMC1"));
        update.addActions(updateName, updateUsages);

        DbRequestResult updateResult = new DbRequestResult();
        updateResult.setCount(1l);
        updateResult.setDiffs(Collections.singletonMap("mc_guid", Arrays.asList("Name: +New Name -Old Name")));
        when(mongoAccess.updateData(any(JsonNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(updateResult);

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            update.getFinalUpdate());

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat((response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(1);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription())
            .isEqualTo(
                "The version retention policy's InitialVersion parameter in BinaryMaster usage is invalid in the contract New name.");

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_update_contracts_should_fail_given_invalid_other_usage_version_retention_policy()
        throws VitamException, InvalidCreateOperationException, FileNotFoundException {
        // Given
        final SetAction updateName = UpdateActionHelper.set("Name", "New name");

        VersionUsageModel dissemniationUsage = new VersionUsageModel();
        dissemniationUsage.setUsageName(DISSEMINATION.getName());
        dissemniationUsage.setInitialVersion(true);
        dissemniationUsage.setIntermediaryVersion(null);

        ObjectNode objectNode = JsonHandler.createObjectNode();
        objectNode.set("VersionRetentionPolicy.Usages",
            JsonHandler.toJsonNode(Arrays.asList(JsonHandler.toJsonNode(dissemniationUsage))));
        final SetAction updateUsages = UpdateActionHelper.set(objectNode);

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "IdentifierMC1"));
        update.addActions(updateName, updateUsages);

        DbRequestResult updateResult = new DbRequestResult();
        updateResult.setCount(1l);
        updateResult.setDiffs(Collections.singletonMap("mc_guid", Arrays.asList("Name: +New Name -Old Name")));
        when(mongoAccess.updateData(any(JsonNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(updateResult);

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            update.getFinalUpdate());

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat((response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(1);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription())
            .isEqualTo(
                "The version retention policy's IntermediaryVersion parameter in Dissemination usage is invalid in the contract New name.");

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void when_update_contracts_should_fail_given_invalid_name_other_usage_version_retention_policy()
        throws VitamException, InvalidCreateOperationException, FileNotFoundException {
        // Given
        final SetAction updateName = UpdateActionHelper.set("Name", "New name");

        VersionUsageModel testUsage = new VersionUsageModel();
        testUsage.setUsageName("test usage name");
        testUsage.setInitialVersion(true);
        testUsage.setIntermediaryVersion(ALL);

        ObjectNode objectNode = JsonHandler.createObjectNode();
        objectNode.set("VersionRetentionPolicy.Usages",
            JsonHandler.toJsonNode(Arrays.asList(JsonHandler.toJsonNode(testUsage))));
        final SetAction updateUsages = UpdateActionHelper.set(objectNode);

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "IdentifierMC1"));
        update.addActions(updateName, updateUsages);

        DbRequestResult updateResult = new DbRequestResult();
        updateResult.setCount(1l);
        updateResult.setDiffs(Collections.singletonMap("mc_guid", Arrays.asList("Name: +New Name -Old Name")));
        when(mongoAccess.updateData(any(JsonNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(updateResult);

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            update.getFinalUpdate());

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat((response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(((VitamError) response).getErrors().size()).isEqualTo(1);
        assertThat(((VitamError) response).getErrors().get(0).getMessage())
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
        assertThat(((VitamError) response).getErrors().get(0).getDescription())
            .isEqualTo("The usage type test usage name is invalid in the contract New name.");

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT.VERSION_RETENTION_POLICY_ERROR.KO");
    }

    private ManagementContractModel getManagementContract(String identifier) {
        ManagementContractModel mc = new ManagementContractModel();
        mc.setId("mc_guid").setName("New name").setIdentifier(identifier).setDescription("existing description")
            .setCreationdate("2017-04-10T11:30:33.798").setActivationdate("2017-04-10T11:30:33.798")
            .setLastupdate("2017-04-11T11:30:33.798").setStatus(ActivationStatus.ACTIVE);
        return mc;
    }


    @Test
    @RunWithCustomExecutor
    public void when_update_old_contracts_should_add_default_version_retention_policy_if_not_exists()
        throws VitamException, FileNotFoundException, InvalidCreateOperationException {
        // Given
        final SetAction updateName = UpdateActionHelper.set("Name", "New name");

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", "IdentifierMC1"));
        update.addActions(updateName);

        DbRequestResult updateResult = new DbRequestResult();
        updateResult.setCount(1l);
        updateResult.setDiffs(Collections.singletonMap("mc_guid", Arrays.asList("Name: +New Name -Old Name")));
        when(mongoAccess.updateData(any(JsonNode.class), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(updateResult);

        DbRequestResult findResultMock = mock(DbRequestResult.class);
        when(findResultMock.getCount()).thenReturn(1l);
        when(findResultMock.getDocuments(ManagementContract.class, ManagementContractModel.class))
            .thenReturn(Arrays.asList(getManagementContract("IdentifierMC1")));
        when(mongoAccess.findDocuments(any(), eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT)))
            .thenReturn(findResultMock);
        when(storageClient.getStorageStrategies()).thenReturn(loadStorageStrategies());

        ArgumentCaptor<JsonNode> contractToUpdateCaptor = ArgumentCaptor.forClass(JsonNode.class);

        // When
        RequestResponse<ManagementContractModel> response = managementContractService.updateContract("IdentifierMC1",
            update.getFinalUpdate());

        // Then
        assertThat(response.isOk()).isTrue();

        verify(mongoAccess, times(1)).updateData(contractToUpdateCaptor.capture(),
            eq(FunctionalAdminCollections.MANAGEMENT_CONTRACT));
        JsonNode contractToUpdateCalled = contractToUpdateCaptor.getValue();
        assertThat(contractToUpdateCalled).isNotNull();
        assertTrue(contractToUpdateCalled.get(BuilderToken.GLOBAL.ACTION.exactToken()).toString().contains(
            "{\"$set\":{\"VersionRetentionPolicy\":{\"InitialVersion\":true,\"IntermediaryVersion\":\"LAST\"}}}"));

        verify(logbookOperationsClient, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClient, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_UPDATE_MANAGEMENT_CONTRACT");
    }

    private RequestResponseOK<StorageStrategy> loadStorageStrategies()
        throws FileNotFoundException, InvalidParseOperationException {
        StorageStrategy[] storageStrategiesArray = JsonHandler.getFromFileLowerCamelCase(
            PropertiesUtils.findFile("strategies_for_contracts_management.json"), StorageStrategy[].class);
        return new RequestResponseOK<StorageStrategy>().addAllResults(Arrays.asList(storageStrategiesArray));
    }

}
