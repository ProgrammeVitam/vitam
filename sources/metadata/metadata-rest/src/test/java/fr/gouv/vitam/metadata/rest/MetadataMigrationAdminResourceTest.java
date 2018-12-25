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
