package fr.gouv.vitam.worker.core.plugin.audit;

import static fr.gouv.vitam.common.json.JsonHandler.getFromInputStream;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.*;

import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.plugin.audit.exception.AuditException;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditCheckObjectGroupResult;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditObjectGroup;

public class AuditExistenceServiceTest {
    private AuditExistenceService service;
    private StorageClient storageClient;
    private StorageClientFactory storageClientFactory;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {

        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        storageClient = mock(StorageClient.class);
        storageClientFactory = mock(StorageClientFactory.class);

        when(storageClientFactory.getClient()).thenReturn(storageClient);
        service = new AuditExistenceService(storageClientFactory);
    }

    @Test
    public void shouldStatusOKWhenBinaryObjectExists() throws Exception {

        reset(storageClient);
        Map<String, Boolean> existsResult = new HashMap<String, Boolean>();
        existsResult.put("offer-fs-1.service.int.consul", Boolean.TRUE);
        existsResult.put("offer-fs-2.service.int.consul", Boolean.TRUE);
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaahgotryaauzialjp5zkhgiaaaaq"), any()))
                .thenReturn(existsResult);

        JsonLineModel objectGroupLine = getFromInputStream(
                getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_1.json"), JsonLineModel.class);
        AuditObjectGroup detail = getFromJsonNode(objectGroupLine.getParams(), AuditObjectGroup.class);

        final AuditCheckObjectGroupResult response = service.check(detail, loadStorageStrategiesMock());
        assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.getObjectStatuses().size()).isEqualTo(1);
        assertThat(response.getObjectStatuses().get(0)).isNotNull();
        assertThat(response.getObjectStatuses().get(0).getOfferStatuses().size()).isEqualTo(2);
        assertThat(response.getObjectStatuses().get(0).getOfferStatuses().get("offer-fs-1.service.int.consul"))
                .isEqualTo(StatusCode.OK);
        assertThat(response.getObjectStatuses().get(0).getOfferStatuses().get("offer-fs-2.service.int.consul"))
                .isEqualTo(StatusCode.OK);
        assertThat(response.getObjectStatuses().get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void shouldStatusKOWhenBinaryObjectNotExists() throws Exception {
        reset(storageClient);
        Map<String, Boolean> existsResult = new HashMap<String, Boolean>();
        existsResult.put("offer-fs-1.service.int.consul", Boolean.FALSE);
        existsResult.put("offer-fs-2.service.int.consul", Boolean.FALSE);
        // physical
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaahgotryaauzialjp6aa32iaaaaq"), any()))
                .thenReturn(existsResult);
        // binary
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaahgotryaauzialjp6aa3zyaaaaq"), any()))
                .thenReturn(existsResult);

        JsonLineModel objectGroupLine = getFromInputStream(
                getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_3.json"), JsonLineModel.class);
        AuditObjectGroup detail = getFromJsonNode(objectGroupLine.getParams(), AuditObjectGroup.class);

        final AuditCheckObjectGroupResult response = service.check(detail, loadStorageStrategiesMock());
        assertThat(response.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getObjectStatuses().size()).isEqualTo(2);
        assertThat(response.getObjectStatuses().get(0)).isNotNull();
        assertThat(response.getObjectStatuses().get(0).getOfferStatuses().size()).isEqualTo(2);
        assertThat(response.getObjectStatuses().get(0).getIdObject()).isEqualTo("aeaaaaaaaahgotryaauzialjp6aa32iaaaaq");
        assertThat(response.getObjectStatuses().get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.getObjectStatuses().get(1)).isNotNull();
        assertThat(response.getObjectStatuses().get(1).getOfferStatuses().size()).isEqualTo(2);
        assertThat(response.getObjectStatuses().get(1).getIdObject()).isEqualTo("aeaaaaaaaahgotryaauzialjp6aa3zyaaaaq");
        assertThat(response.getObjectStatuses().get(1).getOfferStatuses().get("offer-fs-1.service.int.consul"))
                .isEqualTo(StatusCode.KO);
        assertThat(response.getObjectStatuses().get(1).getOfferStatuses().get("offer-fs-2.service.int.consul"))
                .isEqualTo(StatusCode.KO);
        assertThat(response.getObjectStatuses().get(1).getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @Test
    public void shouldStatusKOWhenPhysicalObjectExists() throws Exception {
        reset(storageClient);
        Map<String, Boolean> existsResult = new HashMap<String, Boolean>();
        existsResult.put("offer-fs-1.service.int.consul", Boolean.TRUE);
        existsResult.put("offer-fs-2.service.int.consul", Boolean.TRUE);
        // physical
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaahgotryaauzialjp6aa32iaaaaq"), any()))
                .thenReturn(existsResult);
        // binary
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaahgotryaauzialjp6aa3zyaaaaq"), any()))
                .thenReturn(existsResult);

        JsonLineModel objectGroupLine = getFromInputStream(
                getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_3.json"), JsonLineModel.class);
        AuditObjectGroup detail = getFromJsonNode(objectGroupLine.getParams(), AuditObjectGroup.class);

        final AuditCheckObjectGroupResult response = service.check(detail, loadStorageStrategiesMock());
        assertThat(response.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getObjectStatuses().size()).isEqualTo(2);
        assertThat(response.getObjectStatuses().get(1)).isNotNull();
        assertThat(response.getObjectStatuses().get(1).getIdObject()).isEqualTo("aeaaaaaaaahgotryaauzialjp6aa3zyaaaaq");
        assertThat(response.getObjectStatuses().get(1).getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.getObjectStatuses().get(0)).isNotNull();
        assertThat(response.getObjectStatuses().get(0).getOfferStatuses().size()).isEqualTo(2);
        assertThat(response.getObjectStatuses().get(0).getIdObject()).isEqualTo("aeaaaaaaaahgotryaauzialjp6aa32iaaaaq");
        assertThat(response.getObjectStatuses().get(0).getOfferStatuses().get("offer-fs-1.service.int.consul"))
                .isEqualTo(StatusCode.KO);
        assertThat(response.getObjectStatuses().get(0).getOfferStatuses().get("offer-fs-2.service.int.consul"))
                .isEqualTo(StatusCode.KO);
        assertThat(response.getObjectStatuses().get(0).getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @Test
    public void storageExceptionThenFatal() throws Exception {
        reset(storageClient);
        when(storageClient.exists(any(), any(), any(), any())).thenThrow(StorageServerClientException.class);

        JsonLineModel objectGroupLine = getFromInputStream(
                getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_3.json"), JsonLineModel.class);
        AuditObjectGroup detail = getFromJsonNode(objectGroupLine.getParams(), AuditObjectGroup.class);

        assertThatThrownBy(() -> {
            service.check(detail, loadStorageStrategiesMock());
        }).isInstanceOf(AuditException.class);

    }

    private List<StorageStrategy> loadStorageStrategiesMock(){
        StorageStrategy defaultStrategy = new StorageStrategy();
        defaultStrategy.setId("default");
        OfferReference offer1 = new OfferReference();
        offer1.setId("offer-fs-1.service.int.consul");
        OfferReference offer2 = new OfferReference();
        offer2.setId("offer-fs-2.service.int.consul");
        List<OfferReference> offers = new ArrayList<>();
        defaultStrategy.setOffers(offers);
        return Collections.singletonList(defaultStrategy);
    }
}
