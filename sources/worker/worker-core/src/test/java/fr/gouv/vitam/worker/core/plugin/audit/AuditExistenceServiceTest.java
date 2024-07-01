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
package fr.gouv.vitam.worker.core.plugin.audit;

import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditCheckObjectGroupResult;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditObjectGroup;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.json.JsonHandler.getFromInputStream;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

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
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaahgotryaauzialjp5zkhgiaaaaq"), any())).thenReturn(
            existsResult
        );

        JsonLineModel objectGroupLine = getFromInputStream(
            getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_1.json"),
            JsonLineModel.class
        );
        AuditObjectGroup detail = getFromJsonNode(objectGroupLine.getParams(), AuditObjectGroup.class);

        final AuditCheckObjectGroupResult response = service.check(detail, loadStorageStrategiesMock());
        assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.getObjectStatuses().size()).isEqualTo(1);
        assertThat(response.getObjectStatuses().get(0)).isNotNull();
        assertThat(response.getObjectStatuses().get(0).getOfferStatuses().size()).isEqualTo(2);
        assertThat(
            response.getObjectStatuses().get(0).getOfferStatuses().get("offer-fs-1.service.int.consul")
        ).isEqualTo(StatusCode.OK);
        assertThat(
            response.getObjectStatuses().get(0).getOfferStatuses().get("offer-fs-2.service.int.consul")
        ).isEqualTo(StatusCode.OK);
        assertThat(response.getObjectStatuses().get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void shouldStatusKOWhenBinaryObjectNotExists() throws Exception {
        reset(storageClient);
        Map<String, Boolean> existsResult = new HashMap<String, Boolean>();
        existsResult.put("offer-fs-1.service.int.consul", Boolean.FALSE);
        existsResult.put("offer-fs-2.service.int.consul", Boolean.FALSE);
        // physical
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaahgotryaauzialjp6aa32iaaaaq"), any())).thenReturn(
            existsResult
        );
        // binary
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaahgotryaauzialjp6aa3zyaaaaq"), any())).thenReturn(
            existsResult
        );

        JsonLineModel objectGroupLine = getFromInputStream(
            getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_3.json"),
            JsonLineModel.class
        );
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
        assertThat(
            response.getObjectStatuses().get(1).getOfferStatuses().get("offer-fs-1.service.int.consul")
        ).isEqualTo(StatusCode.KO);
        assertThat(
            response.getObjectStatuses().get(1).getOfferStatuses().get("offer-fs-2.service.int.consul")
        ).isEqualTo(StatusCode.KO);
        assertThat(response.getObjectStatuses().get(1).getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @Test
    public void shouldStatusKOWhenPhysicalObjectExists() throws Exception {
        reset(storageClient);
        Map<String, Boolean> existsResult = new HashMap<String, Boolean>();
        existsResult.put("offer-fs-1.service.int.consul", Boolean.TRUE);
        existsResult.put("offer-fs-2.service.int.consul", Boolean.TRUE);
        // physical
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaahgotryaauzialjp6aa32iaaaaq"), any())).thenReturn(
            existsResult
        );
        // binary
        when(storageClient.exists(any(), any(), eq("aeaaaaaaaahgotryaauzialjp6aa3zyaaaaq"), any())).thenReturn(
            existsResult
        );

        JsonLineModel objectGroupLine = getFromInputStream(
            getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_3.json"),
            JsonLineModel.class
        );
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
        assertThat(
            response.getObjectStatuses().get(0).getOfferStatuses().get("offer-fs-1.service.int.consul")
        ).isEqualTo(StatusCode.KO);
        assertThat(
            response.getObjectStatuses().get(0).getOfferStatuses().get("offer-fs-2.service.int.consul")
        ).isEqualTo(StatusCode.KO);
        assertThat(response.getObjectStatuses().get(0).getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @Test
    public void storageExceptionThenFatal() throws Exception {
        reset(storageClient);
        when(storageClient.exists(any(), any(), any(), any())).thenThrow(StorageServerClientException.class);

        JsonLineModel objectGroupLine = getFromInputStream(
            getClass().getResourceAsStream("/AuditObjectWorkflow/objectGroup_3.json"),
            JsonLineModel.class
        );
        AuditObjectGroup detail = getFromJsonNode(objectGroupLine.getParams(), AuditObjectGroup.class);

        assertThatThrownBy(() -> {
            service.check(detail, loadStorageStrategiesMock());
        }).isInstanceOf(ProcessingStatusException.class);
    }

    private List<StorageStrategy> loadStorageStrategiesMock() {
        StorageStrategy defaultStrategy = new StorageStrategy();
        defaultStrategy.setId("default");
        OfferReference offer1 = new OfferReference();
        offer1.setId("offer-fs-1.service.int.consul");
        OfferReference offer2 = new OfferReference();
        offer2.setId("offer-fs-2.service.int.consul");
        List<OfferReference> offers = new ArrayList<>();
        offers.add(offer1);
        offers.add(offer2);
        defaultStrategy.setOffers(offers);
        return Collections.singletonList(defaultStrategy);
    }
}
