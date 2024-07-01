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
package fr.gouv.vitam.storage.engine.server.rest;

import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageInconsistentStateException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class StorageResourceBulkTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Test
    @RunWithCustomExecutor
    public final void bulkCreateFromWorkspaceInternalServerError() throws Exception {
        // Given
        int tenantId = 2;
        String strategyId = "strategyId";
        String workspaceContainer = "workspaceContainer";
        List<String> uris = Arrays.asList("uri1", "uri2");
        DataCategory dataCategory = DataCategory.UNIT;
        List<String> objectNames = Arrays.asList("ob1", "ob2");
        String requester = "requester";

        BulkObjectStoreRequest bulkObjectStoreRequest = new BulkObjectStoreRequest(
            workspaceContainer,
            uris,
            dataCategory,
            objectNames
        );

        HttpServletRequest httpServletRequest = getHttpServletRequest(requester);

        HttpHeaders headers = getHttpHeaders(tenantId, strategyId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        StorageDistribution storageDistribution = mock(StorageDistribution.class);
        TimestampGenerator timestampGenerator = mock(TimestampGenerator.class);

        doThrow(StorageException.class)
            .when(storageDistribution)
            .bulkCreateFromWorkspace(strategyId, bulkObjectStoreRequest, requester);

        StorageResource storageResource = new StorageResource(storageDistribution, timestampGenerator);

        // When
        Response response = storageResource.bulkCreateFromWorkspace(
            httpServletRequest,
            headers,
            dataCategory.getCollectionName(),
            bulkObjectStoreRequest
        );

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public final void bulkCreateFromWorkspaceConflict() throws Exception {
        // Given
        int tenantId = 2;
        String strategyId = "strategyId";
        String workspaceContainer = "workspaceContainer";
        List<String> uris = Arrays.asList("uri1", "uri2");
        DataCategory dataCategory = DataCategory.UNIT;
        List<String> objectNames = Arrays.asList("ob1", "ob2");
        String requester = "requester";

        BulkObjectStoreRequest bulkObjectStoreRequest = new BulkObjectStoreRequest(
            workspaceContainer,
            uris,
            dataCategory,
            objectNames
        );

        HttpServletRequest httpServletRequest = getHttpServletRequest(requester);

        HttpHeaders headers = getHttpHeaders(tenantId, strategyId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        StorageDistribution storageDistribution = mock(StorageDistribution.class);
        TimestampGenerator timestampGenerator = mock(TimestampGenerator.class);

        doThrow(StorageInconsistentStateException.class)
            .when(storageDistribution)
            .bulkCreateFromWorkspace(strategyId, bulkObjectStoreRequest, requester);

        StorageResource storageResource = new StorageResource(storageDistribution, timestampGenerator);

        // When
        Response response = storageResource.bulkCreateFromWorkspace(
            httpServletRequest,
            headers,
            dataCategory.getCollectionName(),
            bulkObjectStoreRequest
        );

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.CONFLICT.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public final void bulkCreateFromWorkspaceOK() throws Exception {
        // Given
        int tenantId = 2;
        String strategyId = "strategyId";
        String workspaceContainer = "workspaceContainer";
        List<String> uris = Arrays.asList("uri1", "uri2");
        DataCategory dataCategory = DataCategory.UNIT;
        List<String> objectNames = Arrays.asList("ob1", "ob2");
        String requester = "requester";

        BulkObjectStoreRequest bulkObjectStoreRequest = new BulkObjectStoreRequest(
            workspaceContainer,
            uris,
            dataCategory,
            objectNames
        );

        BulkObjectStoreResponse bulkObjectStoreResponse = mock(BulkObjectStoreResponse.class);

        HttpServletRequest httpServletRequest = getHttpServletRequest(requester);

        HttpHeaders headers = getHttpHeaders(tenantId, strategyId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        StorageDistribution storageDistribution = mock(StorageDistribution.class);
        TimestampGenerator timestampGenerator = mock(TimestampGenerator.class);

        doReturn(bulkObjectStoreResponse)
            .when(storageDistribution)
            .bulkCreateFromWorkspace(strategyId, bulkObjectStoreRequest, requester);

        StorageResource storageResource = new StorageResource(storageDistribution, timestampGenerator);

        // When
        Response response = storageResource.bulkCreateFromWorkspace(
            httpServletRequest,
            headers,
            dataCategory.getCollectionName(),
            bulkObjectStoreRequest
        );

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.CREATED.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(bulkObjectStoreResponse);
    }

    @Test
    @RunWithCustomExecutor
    public final void bulkCreateFromWorkspaceIllegalArguments() {
        // Given
        int tenantId = 2;
        String strategyId = "strategyId";
        String workspaceContainer = "workspaceContainer";
        List<String> uris = Arrays.asList("uri1", "uri2");
        DataCategory dataCategory = DataCategory.UNIT;
        List<String> objectNames = Arrays.asList("ob1", "ob2");
        String requester = "requester";

        // Missing tenant
        checkBackRequest(
            new BulkObjectStoreRequest(workspaceContainer, uris, dataCategory, objectNames),
            getHttpServletRequest(requester),
            getHttpHeaders(null, strategyId),
            dataCategory.getCollectionName()
        );

        // Missing strategy
        checkBackRequest(
            new BulkObjectStoreRequest(workspaceContainer, uris, dataCategory, objectNames),
            getHttpServletRequest(requester),
            getHttpHeaders(tenantId, null),
            dataCategory.getCollectionName()
        );

        // Missing workspace container
        checkBackRequest(
            new BulkObjectStoreRequest("", uris, dataCategory, objectNames),
            getHttpServletRequest(requester),
            getHttpHeaders(tenantId, strategyId),
            dataCategory.getCollectionName()
        );

        checkBackRequest(
            new BulkObjectStoreRequest(null, uris, dataCategory, objectNames),
            getHttpServletRequest(requester),
            getHttpHeaders(tenantId, strategyId),
            dataCategory.getCollectionName()
        );

        // Missing uri
        checkBackRequest(
            new BulkObjectStoreRequest(workspaceContainer, null, dataCategory, objectNames),
            getHttpServletRequest(requester),
            getHttpHeaders(tenantId, strategyId),
            dataCategory.getCollectionName()
        );

        checkBackRequest(
            new BulkObjectStoreRequest(workspaceContainer, Arrays.asList("uri1", null), dataCategory, objectNames),
            getHttpServletRequest(requester),
            getHttpHeaders(tenantId, strategyId),
            dataCategory.getCollectionName()
        );

        // Missing data category
        checkBackRequest(
            new BulkObjectStoreRequest(workspaceContainer, uris, null, objectNames),
            getHttpServletRequest(requester),
            getHttpHeaders(tenantId, strategyId),
            dataCategory.getCollectionName()
        );

        // Missing object name
        checkBackRequest(
            new BulkObjectStoreRequest(workspaceContainer, uris, dataCategory, null),
            getHttpServletRequest(requester),
            getHttpHeaders(tenantId, strategyId),
            dataCategory.getCollectionName()
        );

        checkBackRequest(
            new BulkObjectStoreRequest(workspaceContainer, uris, dataCategory, Collections.emptyList()),
            getHttpServletRequest(requester),
            getHttpHeaders(tenantId, strategyId),
            dataCategory.getCollectionName()
        );

        checkBackRequest(
            new BulkObjectStoreRequest(workspaceContainer, uris, dataCategory, Arrays.asList("ob1", null)),
            getHttpServletRequest(requester),
            getHttpHeaders(tenantId, strategyId),
            dataCategory.getCollectionName()
        );

        // Invalid folder
        checkBackRequest(
            new BulkObjectStoreRequest(workspaceContainer, uris, dataCategory, objectNames),
            getHttpServletRequest(requester),
            getHttpHeaders(tenantId, strategyId),
            "invalid folder"
        );

        // Uri / object name mismatch
        checkBackRequest(
            new BulkObjectStoreRequest(
                workspaceContainer,
                Arrays.asList("uri1", "uri2", "uri3"),
                dataCategory,
                Arrays.asList("ob1", "ob2")
            ),
            getHttpServletRequest(requester),
            getHttpHeaders(tenantId, strategyId),
            dataCategory.getCollectionName()
        );
    }

    private void checkBackRequest(
        BulkObjectStoreRequest bulkObjectStoreRequest,
        HttpServletRequest httpServletRequest,
        HttpHeaders headers,
        String folder
    ) {
        StorageDistribution storageDistribution = mock(StorageDistribution.class);
        TimestampGenerator timestampGenerator = mock(TimestampGenerator.class);

        StorageResource storageResource = new StorageResource(storageDistribution, timestampGenerator);

        Response response = storageResource.bulkCreateFromWorkspace(
            httpServletRequest,
            headers,
            folder,
            bulkObjectStoreRequest
        );

        assertThat(response.getStatus()).isEqualTo(Status.PRECONDITION_FAILED.getStatusCode());
    }

    private HttpServletRequest getHttpServletRequest(String requester) {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        doReturn(requester).when(httpServletRequest).getRemoteAddr();
        return httpServletRequest;
    }

    private HttpHeaders getHttpHeaders(Integer tenantId, String strategyId) {
        HttpHeaders headers = mock(HttpHeaders.class);
        if (strategyId != null) {
            doReturn(Collections.singletonList(strategyId))
                .when(headers)
                .getRequestHeader(VitamHttpHeader.STRATEGY_ID.getName());
        }
        if (tenantId != null) {
            doReturn(Collections.singletonList(Integer.toString(tenantId)))
                .when(headers)
                .getRequestHeader(VitamHttpHeader.TENANT_ID.getName());
        }
        return headers;
    }
}
