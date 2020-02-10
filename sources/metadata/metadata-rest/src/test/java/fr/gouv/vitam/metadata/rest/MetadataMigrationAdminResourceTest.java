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
package fr.gouv.vitam.metadata.rest;

import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.core.migration.DataMigrationService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class MetadataMigrationAdminResourceTest {
    @Rule
    public RunWithCustomExecutorRule runInThread =
            new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Test
    @RunWithCustomExecutor
    public void startDataMigration_accepted() {

        VitamThreadUtils.getVitamSession().setTenantId(0);
        // Given
        DataMigrationService dataMigrationService = Mockito.mock(DataMigrationService.class);
        Mockito.doReturn(true).when(dataMigrationService).tryStartMongoDataUpdate();

        MetadataMigrationAdminResource instance = new MetadataMigrationAdminResource(dataMigrationService);

        // When
        Response response = instance.startDataMigration();

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void startDataMigration_alreadyRunning() {
        VitamThreadUtils.getVitamSession().setTenantId(0);

        // Given
        DataMigrationService dataMigrationService = Mockito.mock(DataMigrationService.class);
        Mockito.doReturn(false).when(dataMigrationService).tryStartMongoDataUpdate();

        MetadataMigrationAdminResource instance = new MetadataMigrationAdminResource(dataMigrationService);

        // When
        Response response = instance.startDataMigration();

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void startDataMigration_internalError() {
        VitamThreadUtils.getVitamSession().setTenantId(0);

        // Given
        DataMigrationService dataMigrationService = Mockito.mock(DataMigrationService.class);
        Mockito.doThrow(RuntimeException.class).when(dataMigrationService).tryStartMongoDataUpdate();

        MetadataMigrationAdminResource instance = new MetadataMigrationAdminResource(dataMigrationService);

        // When
        Response response = instance.startDataMigration();

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void isMongoDataUpdateInProgress_running() {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        // Given
        DataMigrationService dataMigrationService = Mockito.mock(DataMigrationService.class);
        Mockito.doReturn(true).when(dataMigrationService).isMongoDataUpdateInProgress();

        MetadataMigrationAdminResource instance = new MetadataMigrationAdminResource(dataMigrationService);

        // When
        Response response = instance.isMigrationInProgress();

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void isMongoDataUpdateInProgress_not_running() {
        VitamThreadUtils.getVitamSession().setTenantId(0);

        // Given
        DataMigrationService dataMigrationService = Mockito.mock(DataMigrationService.class);
        Mockito.doReturn(false).when(dataMigrationService).isMongoDataUpdateInProgress();

        MetadataMigrationAdminResource instance = new MetadataMigrationAdminResource(dataMigrationService);

        // When
        Response response = instance.isMigrationInProgress();

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }
}
