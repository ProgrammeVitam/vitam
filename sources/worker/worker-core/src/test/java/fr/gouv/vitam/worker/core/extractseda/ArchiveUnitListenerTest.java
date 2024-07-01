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
package fr.gouv.vitam.worker.core.extractseda;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitIdentifierKeyType;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.culture.archivesdefrance.seda.v2.CustodialHistoryType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectOrArchiveUnitReferenceType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectRefType;
import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataContentType;
import fr.gouv.culture.archivesdefrance.seda.v2.ManagementType;
import fr.gouv.culture.archivesdefrance.seda.v2.RelatedObjectReferenceType;
import fr.gouv.culture.archivesdefrance.seda.v2.UpdateOperationType;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingNotValidLinkingException;
import fr.gouv.vitam.processing.common.exception.ProcessingUnitLinkingException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.worker.core.utils.JsonLineDataBase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.xml.bind.JAXBElement;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArchiveUnitListenerTest {

    private static final Integer TENANT_ID = 0;
    private static MetaDataClientFactory metaDataClientFactory;
    private static MetaDataClient metaDataClient;

    private IngestContext ingestContext;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void setupClass() {
        metaDataClientFactory = mock(MetaDataClientFactory.class);
        metaDataClient = mock(MetaDataClient.class);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
    }

    @Before
    public void setup() {
        ingestContext = new IngestContext();
        ingestContext.setWorkflowUnitType(UnitType.INGEST);
        ingestContext.setOperationId("OperationID");
        ingestContext.setTypeProcess(LogbookTypeProcess.INGEST_TEST);
    }

    @Test
    @RunWithCustomExecutor
    public void testAfterUnmarshalLinkedSystemIdNotFoundKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        ArchiveUnitType target = mock(ArchiveUnitType.class);
        JAXBElement<?> parent = mock(JAXBElement.class);

        ArchiveUnitListener archiveUnitListener = new ArchiveUnitListener(
            mock(HandlerIOImpl.class),
            ingestContext,
            new IngestSession(),
            mock(JsonLineDataBase.class),
            metaDataClientFactory
        );

        when(target.getArchiveUnitRefId()).thenReturn(null);
        ManagementType management = mock(ManagementType.class);
        when(target.getManagement()).thenReturn(management);
        UpdateOperationType updateOperation = mock(UpdateOperationType.class);
        when(management.getUpdateOperation()).thenReturn(updateOperation);

        when(updateOperation.getSystemId()).thenReturn("aeaaaaaaaaaam7mxabxccakzrw47heqaaaaq");

        when(parent.isGlobalScope()).thenReturn(true);

        RequestResponseOK<JsonNode> resp = new RequestResponseOK<>();
        when(metaDataClient.selectUnits(any())).thenReturn(resp.toJsonNode());

        // Case of a not valid SystemId (not valid guid) is test in ProcessingIT testWorkflowAddAndLinkSIPWithNotGUIDSystemIDKo
        assertThatCode(() -> archiveUnitListener.extractArchiveUnit(target, parent))
            .isInstanceOf(VitamRuntimeException.class)
            .hasCauseInstanceOf(ProcessingNotFoundException.class)
            .extracting(e -> ((ProcessingNotFoundException) e.getCause()).isValidGuid())
            .isEqualTo(true);
    }

    @Test
    @RunWithCustomExecutor
    public void testAfterUnmarshalLinkedSystemIdNotFoundNotValigSystemIDKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        ArchiveUnitType target = mock(ArchiveUnitType.class);
        JAXBElement<?> parent = mock(JAXBElement.class);

        ArchiveUnitListener archiveUnitListener = new ArchiveUnitListener(
            mock(HandlerIOImpl.class),
            ingestContext,
            new IngestSession(),
            mock(JsonLineDataBase.class),
            metaDataClientFactory
        );

        when(target.getArchiveUnitRefId()).thenReturn(null);
        ManagementType management = mock(ManagementType.class);
        when(target.getManagement()).thenReturn(management);
        UpdateOperationType updateOperation = mock(UpdateOperationType.class);
        when(management.getUpdateOperation()).thenReturn(updateOperation);

        when(updateOperation.getSystemId()).thenReturn("SystemIDNotValid");

        when(parent.isGlobalScope()).thenReturn(true);

        RequestResponseOK<JsonNode> resp = new RequestResponseOK<>();
        when(metaDataClient.selectUnits(any())).thenReturn(resp.toJsonNode());

        assertThatCode(() -> archiveUnitListener.extractArchiveUnit(target, parent))
            .isInstanceOf(VitamRuntimeException.class)
            .hasCauseInstanceOf(ProcessingNotFoundException.class)
            .extracting(e -> ((ProcessingNotFoundException) e.getCause()).isValidGuid())
            .isEqualTo(false);
    }

    /**
     * Test that we cannot link Filing Unit to Ingest Unit and should throws ProcessingUnitLinkingException
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void testAfterUnmarshalLinkingFilingToIngestUnitUnauthorized() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        ArchiveUnitType target = mock(ArchiveUnitType.class);
        JAXBElement<?> parent = mock(JAXBElement.class);

        ingestContext.setWorkflowUnitType(UnitType.FILING_UNIT);
        ArchiveUnitListener archiveUnitListener = new ArchiveUnitListener(
            mock(HandlerIOImpl.class),
            ingestContext,
            new IngestSession(),
            mock(JsonLineDataBase.class),
            metaDataClientFactory
        );

        when(target.getArchiveUnitRefId()).thenReturn(null);
        ManagementType management = mock(ManagementType.class);
        when(target.getManagement()).thenReturn(management);
        UpdateOperationType updateOperation = mock(UpdateOperationType.class);
        when(management.getUpdateOperation()).thenReturn(updateOperation);

        when(updateOperation.getSystemId()).thenReturn("aeaaaaaaaaaam7mxabxccakzrw47heqaaaaq");

        when(parent.isGlobalScope()).thenReturn(true);

        RequestResponseOK<JsonNode> resp = new RequestResponseOK<>();
        ObjectNode unit = JsonHandler.createObjectNode();
        unit.put("#id", "aeaaaaaaaaaam7mxabxccakzrw47heqaaaaq");
        unit.put("#unitType", UnitType.INGEST.name());
        resp.addResult(unit);

        when(metaDataClient.selectUnits(any())).thenReturn(resp.toJsonNode());

        assertThatCode(() -> archiveUnitListener.extractArchiveUnit(target, parent))
            .isInstanceOf(VitamRuntimeException.class)
            .hasCauseInstanceOf(ProcessingUnitLinkingException.class);
    }

    /**
     * Test that we cannot link Holding Unit to Ingest Unit and should throws ProcessingUnitLinkingException
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void testAfterUnmarshalLinkingHoldingToIngestUnitUnauthorized() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        ArchiveUnitType target = mock(ArchiveUnitType.class);
        JAXBElement<?> parent = mock(JAXBElement.class);

        ingestContext.setWorkflowUnitType(UnitType.HOLDING_UNIT);
        ArchiveUnitListener archiveUnitListener = new ArchiveUnitListener(
            mock(HandlerIOImpl.class),
            ingestContext,
            new IngestSession(),
            mock(JsonLineDataBase.class),
            metaDataClientFactory
        );

        when(target.getArchiveUnitRefId()).thenReturn(null);
        ManagementType management = mock(ManagementType.class);
        when(target.getManagement()).thenReturn(management);
        UpdateOperationType updateOperation = mock(UpdateOperationType.class);
        when(management.getUpdateOperation()).thenReturn(updateOperation);

        when(updateOperation.getSystemId()).thenReturn("aeaaaaaaaaaam7mxabxccakzrw47heqaaaaq");

        when(parent.isGlobalScope()).thenReturn(true);

        RequestResponseOK<JsonNode> resp = new RequestResponseOK<>();
        ObjectNode unit = JsonHandler.createObjectNode();
        unit.put("#id", "aeaaaaaaaaaam7mxabxccakzrw47heqaaaaq");
        unit.put("#unitType", UnitType.INGEST.name());
        resp.addResult(unit);

        when(metaDataClient.selectUnits(any())).thenReturn(resp.toJsonNode());

        assertThatCode(() -> archiveUnitListener.extractArchiveUnit(target, parent))
            .isInstanceOf(VitamRuntimeException.class)
            .hasCauseInstanceOf(ProcessingUnitLinkingException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void testAfterUnmarshalHoldingDoNotGetOriginatingAgencies() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        ArchiveUnitType target = mock(ArchiveUnitType.class);
        JAXBElement<?> parent = mock(JAXBElement.class);

        List<String> agenciesList = new ArrayList<>();
        HandlerIO handlerIO = mock(HandlerIO.class);
        ArchiveUnitListener archiveUnitListener = new ArchiveUnitListener(
            handlerIO,
            ingestContext,
            new IngestSession(),
            mock(JsonLineDataBase.class),
            metaDataClientFactory
        );

        when(target.getArchiveUnitRefId()).thenReturn(null);
        ManagementType management = mock(ManagementType.class);
        when(target.getManagement()).thenReturn(management);
        UpdateOperationType updateOperation = mock(UpdateOperationType.class);
        when(management.getUpdateOperation()).thenReturn(updateOperation);

        when(updateOperation.getSystemId()).thenReturn("aeaaaaaaaaaam7mxabxccakzrw47heqaaaaq");

        when(parent.isGlobalScope()).thenReturn(true);

        RequestResponseOK<JsonNode> resp = new RequestResponseOK<>();
        ObjectNode unit = JsonHandler.createObjectNode();
        unit.put("#id", "aeaaaaaaaaaam7mxabxccakzrw47heqaaaaq");
        unit.put("#unitType", UnitType.HOLDING_UNIT.name());
        ArrayNode agencies = JsonHandler.createArrayNode();
        agencies.add("AGENCY1").add("AGENCY1");
        unit.set("#originating_agencies", agencies);

        resp.addResult(unit);
        File file = temporaryFolder.newFile();
        when(handlerIO.getNewLocalFile(anyString())).thenReturn(file);

        when(metaDataClient.selectUnits(any())).thenReturn(resp.toJsonNode());

        assertThatCode(() -> archiveUnitListener.extractArchiveUnit(target, parent)).doesNotThrowAnyException();
        assertThat(agenciesList).hasSize(0);
    }

    @Test
    @RunWithCustomExecutor
    public void testAfterUnmarshalGetOriginatingAgenciesOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        ArchiveUnitType target = mock(ArchiveUnitType.class);
        JAXBElement<?> parent = mock(JAXBElement.class);

        HandlerIO handlerIO = mock(HandlerIO.class);

        IngestSession ingestSession = new IngestSession();
        ArchiveUnitListener archiveUnitListener = new ArchiveUnitListener(
            mock(HandlerIOImpl.class),
            ingestContext,
            ingestSession,
            mock(JsonLineDataBase.class),
            metaDataClientFactory
        );

        when(target.getArchiveUnitRefId()).thenReturn(null);
        ManagementType management = mock(ManagementType.class);
        when(target.getManagement()).thenReturn(management);
        UpdateOperationType updateOperation = mock(UpdateOperationType.class);
        when(management.getUpdateOperation()).thenReturn(updateOperation);

        when(updateOperation.getSystemId()).thenReturn("aeaaaaaaaaaam7mxabxccakzrw47heqaaaaq");

        when(parent.isGlobalScope()).thenReturn(true);

        RequestResponseOK<JsonNode> resp = new RequestResponseOK<>();
        ObjectNode unit = JsonHandler.createObjectNode();
        unit.put("#id", "aeaaaaaaaaaam7mxabxccakzrw47heqaaaaq");
        unit.put("#unitType", UnitType.INGEST.name());
        ArrayNode agencies = JsonHandler.createArrayNode();
        agencies.add("AGENCY1").add("AGENCY1");
        unit.set("#originating_agencies", agencies);

        resp.addResult(unit);
        File file = temporaryFolder.newFile();
        when(handlerIO.getNewLocalFile(anyString())).thenReturn(file);

        when(metaDataClient.selectUnits(any())).thenReturn(resp.toJsonNode());

        assertThatCode(() -> archiveUnitListener.extractArchiveUnit(target, parent)).doesNotThrowAnyException();

        assertThat(ingestSession.getOriginatingAgencies()).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void testAfterUnmarshalKeyValueSystemIDOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        ArchiveUnitType target = mock(ArchiveUnitType.class);
        JAXBElement<?> parent = mock(JAXBElement.class);

        HandlerIO handlerIO = mock(HandlerIO.class);
        IngestSession ingestSession = new IngestSession();
        ArchiveUnitListener archiveUnitListener = new ArchiveUnitListener(
            handlerIO,
            ingestContext,
            ingestSession,
            mock(JsonLineDataBase.class),
            metaDataClientFactory
        );

        when(target.getArchiveUnitRefId()).thenReturn(null);
        ManagementType management = mock(ManagementType.class);
        when(target.getManagement()).thenReturn(management);
        UpdateOperationType updateOperation = mock(UpdateOperationType.class);
        when(management.getUpdateOperation()).thenReturn(updateOperation);

        ArchiveUnitIdentifierKeyType archiveUnitIdentifierKeyType = new ArchiveUnitIdentifierKeyType();
        archiveUnitIdentifierKeyType.setMetadataName("Title");
        archiveUnitIdentifierKeyType.setMetadataValue("Value");
        when(updateOperation.getArchiveUnitIdentifierKey()).thenReturn(archiveUnitIdentifierKeyType);

        when(parent.isGlobalScope()).thenReturn(true);

        RequestResponseOK<JsonNode> resp = new RequestResponseOK<>();
        ObjectNode unit = JsonHandler.createObjectNode();
        unit.put("#id", "aeaaaaaaaaaam7mxabxccakzrw47heqaaaaq");
        unit.put("#unitType", UnitType.INGEST.name());
        ArrayNode agencies = JsonHandler.createArrayNode();
        agencies.add("AGENCY1").add("AGENCY1");
        unit.set("#originating_agencies", agencies);

        resp.addResult(unit);
        File file = temporaryFolder.newFile();
        when(handlerIO.getNewLocalFile(anyString())).thenReturn(file);

        when(metaDataClient.selectUnits(any())).thenReturn(resp.toJsonNode());

        assertThatCode(() -> archiveUnitListener.extractArchiveUnit(target, parent)).doesNotThrowAnyException();

        assertThat(ingestSession.getOriginatingAgencies()).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void testAfterUnmarshalCustodialHistoryShouldNotThrowException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JAXBElement<?> parent = mock(JAXBElement.class);
        HandlerIO handlerIO = mock(HandlerIO.class);

        IngestSession ingestSession = new IngestSession();
        ingestSession.getObjectGroupIdToGuid().put("ID0011", "aeaaaaaaaaaam7mxabxccakzrw466heqaaaaq");
        ingestSession.getDataObjectIdToObjectGroupId().put("ID22", "ID0011");

        ArchiveUnitListener archiveUnitListener = new ArchiveUnitListener(
            handlerIO,
            ingestContext,
            ingestSession,
            mock(JsonLineDataBase.class),
            metaDataClientFactory
        );
        File file = temporaryFolder.newFile();
        when(handlerIO.getNewLocalFile(anyString())).thenReturn(file);
        when(parent.isGlobalScope()).thenReturn(true);

        ArchiveUnitType archiveUnitType = new ArchiveUnitType();
        DescriptiveMetadataContentType content = new DescriptiveMetadataContentType();
        CustodialHistoryType custodialHistoryType = new CustodialHistoryType();
        DataObjectRefType dataObjectRefType = new DataObjectRefType();
        dataObjectRefType.setDataObjectGroupReferenceId("ID0011");
        custodialHistoryType.setCustodialHistoryFile(dataObjectRefType);
        content.setCustodialHistory(custodialHistoryType);
        archiveUnitType.setContent(content);

        assertThatCode(
            () -> archiveUnitListener.extractArchiveUnit(archiveUnitType, parent)
        ).doesNotThrowAnyException();
    }

    @Test
    @RunWithCustomExecutor
    public void testAfterUnmarshalCustodialHistoryShouldThrowException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JAXBElement<?> parent = mock(JAXBElement.class);

        HandlerIO handlerIO = mock(HandlerIO.class);
        ArchiveUnitListener archiveUnitListener = new ArchiveUnitListener(
            handlerIO,
            ingestContext,
            new IngestSession(),
            mock(JsonLineDataBase.class),
            metaDataClientFactory
        );
        File file = temporaryFolder.newFile();
        when(handlerIO.getNewLocalFile(anyString())).thenReturn(file);
        when(parent.isGlobalScope()).thenReturn(true);

        ArchiveUnitType archiveUnitType = new ArchiveUnitType();
        DescriptiveMetadataContentType content = new DescriptiveMetadataContentType();
        CustodialHistoryType custodialHistoryType = new CustodialHistoryType();
        DataObjectRefType dataObjectRefType = new DataObjectRefType();
        dataObjectRefType.setDataObjectGroupReferenceId("ID0011");
        custodialHistoryType.setCustodialHistoryFile(dataObjectRefType);
        content.setCustodialHistory(custodialHistoryType);
        archiveUnitType.setContent(content);

        assertThatCode(() -> archiveUnitListener.extractArchiveUnit(archiveUnitType, parent))
            .isInstanceOf(VitamRuntimeException.class)
            .hasCauseInstanceOf(ProcessingNotValidLinkingException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void testAfterUnmarshalRelatedDataObjectReferenceFull() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JAXBElement<?> parent = mock(JAXBElement.class);

        IngestSession ingestSession = new IngestSession();
        ingestSession.getDataObjectIdToGuid().put("ID06", "GUID_ID06");
        ingestSession.getDataObjectIdToGuid().put("ID20", "GUID_ID20");
        ingestSession.getDataObjectIdToGuid().put("ID0009", "GUID_ID0009");
        ingestSession.getObjectGroupIdToGuid().put("ID0009", "GUID_ID0009");

        HandlerIO handlerIO = mock(HandlerIO.class);

        ArchiveUnitListener archiveUnitListener = new ArchiveUnitListener(
            handlerIO,
            ingestContext,
            ingestSession,
            mock(JsonLineDataBase.class),
            metaDataClientFactory
        );

        File file = temporaryFolder.newFile();
        when(handlerIO.getNewLocalFile(anyString())).thenReturn(file);
        when(parent.isGlobalScope()).thenReturn(true);

        ArchiveUnitType archiveUnitType = new ArchiveUnitType();
        archiveUnitType.setId("ID05");
        DescriptiveMetadataContentType content = new DescriptiveMetadataContentType();

        RelatedObjectReferenceType relatedObjectReferenceType = new RelatedObjectReferenceType();

        List<DataObjectOrArchiveUnitReferenceType> dataObjectOrArchiveUnitReferenceType = new ArrayList<>();

        DataObjectOrArchiveUnitReferenceType dataObjectOrArchiveUnitReferenceType1 =
            new DataObjectOrArchiveUnitReferenceType();

        dataObjectOrArchiveUnitReferenceType1.setArchiveUnitRefId("ID06");
        DataObjectRefType dataObjectRefType = new DataObjectRefType();
        dataObjectRefType.setDataObjectReferenceId("ID20");
        dataObjectRefType.setDataObjectGroupReferenceId("ID0009");
        dataObjectOrArchiveUnitReferenceType1.setDataObjectReference(dataObjectRefType);
        dataObjectOrArchiveUnitReferenceType1.setRepositoryArchiveUnitPID("ID06");
        dataObjectOrArchiveUnitReferenceType1.setRepositoryObjectPID("ID20");
        dataObjectOrArchiveUnitReferenceType1.setExternalReference("Bibliographie : cf annexe");

        dataObjectOrArchiveUnitReferenceType.add(dataObjectOrArchiveUnitReferenceType1);

        relatedObjectReferenceType.getIsVersionOf().addAll(dataObjectOrArchiveUnitReferenceType);

        content.setRelatedObjectReference(relatedObjectReferenceType);

        archiveUnitType.setContent(content);

        assertThatCode(
            () -> archiveUnitListener.extractArchiveUnit(archiveUnitType, parent)
        ).doesNotThrowAnyException();
    }
}
