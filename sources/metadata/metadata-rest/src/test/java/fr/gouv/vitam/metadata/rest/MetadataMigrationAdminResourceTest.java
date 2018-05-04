package fr.gouv.vitam.metadata.rest;

import fr.gouv.vitam.metadata.core.migration.DataMigrationService;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class MetadataMigrationAdminResourceTest {

    @Test
    public void startDataMigration_accepted() {

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
    public void startDataMigration_alreadyRunning() {

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
    public void startDataMigration_internalError() {

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
    public void isMongoDataUpdateInProgress_running() {
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
    public void isMongoDataUpdateInProgress_not_running() {
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
