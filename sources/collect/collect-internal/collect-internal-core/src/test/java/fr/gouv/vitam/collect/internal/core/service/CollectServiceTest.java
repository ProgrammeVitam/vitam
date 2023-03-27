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

package fr.gouv.vitam.collect.internal.core.service;

import fr.gouv.vitam.collect.common.dto.FileInfoDto;
import fr.gouv.vitam.collect.common.dto.ObjectDto;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.CollectUnitModel;
import fr.gouv.vitam.collect.internal.core.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.core.helpers.builders.DbVersionsModelBuilder;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.format.identification.FormatIdentifier;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.DbQualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.DbVersionsModel;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.BINARY_MASTER;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CollectServiceTest {
    private static final int TENANT_ID = 0;
    private static final String SAMPLE_ARCHIVE_UNIT = "archive_unit_from_metadata.json";
    private static final String SAMPLE_OBJECT_GROUP2 = "object_group_from_metadata2.json";
    private static final String SAMPLE_OBJECT_GROUP_WITH_QUALIFIER = "object_group_from_metadata3.json";
    private static final String SAMPLE_OBJECT_GROUP_NEW_VERSION = "object_group_from_metadata4.json";
    private static final String SAMPLE_OBJECT_GROUP5 = "object_group_from_metadata5.json";

    @Rule public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks private CollectService collectService;

    @Mock private MetadataRepository metadataRepository;
    @Mock private WorkspaceClientFactory workspaceClientFactory;
    @Mock private WorkspaceClient workspaceClient;
    @Mock private FormatIdentifierFactory formatIdentifierFactory;

    @Test
    public void getArchiveUnitModel() throws Exception {
        // Given
        String unitId = "aeeaaaaaacfm6tqsaawpgamadc4j5baaaaaq";
        when(metadataRepository.selectUnitById(any())).thenReturn(
            JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_ARCHIVE_UNIT)));
        // When
        CollectUnitModel archiveUnitModel = collectService.getArchiveUnitModel(unitId);
        // Then
        Assertions.assertThat(archiveUnitModel).isNotNull();
        Assertions.assertThat(archiveUnitModel.getId()).isEqualTo(unitId);
    }

    @Test
    public void getInputStreamFromWorkspace() throws Exception {
        // Given
        Response response =
            Response.ok(new ByteArrayInputStream("ResponseOK".getBytes())).status(Response.Status.OK).build();
        when(workspaceClient.getObject(any(), any())).thenReturn(response);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        // When
        InputStream inputStreamFromWorkspace = collectService.getInputStreamFromWorkspace("containerName", "fileName");
        // Then
        Assertions.assertThat(inputStreamFromWorkspace).isNotNull();
        Assertions.assertThat(inputStreamFromWorkspace.readAllBytes()).isEqualTo("ResponseOK".getBytes());
    }

    @Test
    public void getBinaryByUsageAndVersion() throws Exception {
        // Given
        when(metadataRepository.selectObjectGroupById("og", true)).thenReturn(
            JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_OBJECT_GROUP5)));
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        Response response =
            Response.ok(new ByteArrayInputStream("ResponseOK".getBytes())).status(Response.Status.OK).build();
        when(workspaceClient.getObject(any(), any())).thenReturn(response);
        CollectUnitModel unitModel = new CollectUnitModel();
        unitModel.setOg("og");
        // When
        Response binaryByUsageAndVersion = collectService.getBinaryByUsageAndVersion(unitModel, BINARY_MASTER, 1);
        // Then
        Assertions.assertThat(binaryByUsageAndVersion).isNotNull();
        Assertions.assertThat(binaryByUsageAndVersion.getStatus()).isEqualTo(200);
        Assertions.assertThat(binaryByUsageAndVersion.readEntity(InputStream.class).readAllBytes())
            .isEqualTo("ResponseOK".getBytes());
    }

    @Test
    @RunWithCustomExecutor
    public void testUpdateOrSaveObjectGroup_insertNewObjectGroup() throws Exception {
        // Given
        when(metadataRepository.saveObjectGroup(any())).thenReturn(
            JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_OBJECT_GROUP2)));
        VitamThreadUtils.getVitamSession().setTenantId(1);
        CollectUnitModel unitModel = new CollectUnitModel();
        unitModel.setId("1");
        unitModel.setOpi("opi");
        ObjectDto myObjectDto = new ObjectDto("1", new FileInfoDto("filename", "lastname"));
        // When
        ObjectDto objectDto = collectService.updateOrSaveObjectGroup(unitModel, BINARY_MASTER, 1, myObjectDto);
        // Then
        Assertions.assertThat(objectDto).isNotNull();
        Assertions.assertThat(objectDto.getFileInfo()).isEqualTo(new FileInfoDto("filename", "lastname"));
    }

    @Test
    @RunWithCustomExecutor
    public void testUpdateOrSaveObjectGroup_ko_collect_error() throws Exception {
        // Given
        when(metadataRepository.saveObjectGroup(any())).thenThrow(new CollectInternalException("error"));
        VitamThreadUtils.getVitamSession().setTenantId(1);
        CollectUnitModel unitModel = new CollectUnitModel();
        unitModel.setId("1");
        unitModel.setOpi("opi");
        ObjectDto myObjectDto = new ObjectDto("1", new FileInfoDto("filename", "lastname"));
        // When - Then
        assertThrows(CollectInternalException.class,
            () -> collectService.updateOrSaveObjectGroup(unitModel, BINARY_MASTER, 1, myObjectDto));
    }

    @Test
    @RunWithCustomExecutor
    public void testUpdateOrSaveObjectGroup_updateExistingObjectGroup() throws Exception {
        // Given
        when(metadataRepository.selectObjectGroupById("og", true)).thenReturn(
            JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_OBJECT_GROUP2)));
        VitamThreadUtils.getVitamSession().setTenantId(1);
        CollectUnitModel unitModel = new CollectUnitModel();
        unitModel.setId("1");
        unitModel.setOpi("opi");
        unitModel.setOg("og");
        ObjectDto myObjectDto = new ObjectDto("1", new FileInfoDto("filename", "lastname"));
        // When
        ObjectDto objectDto = collectService.updateOrSaveObjectGroup(unitModel, BINARY_MASTER, 1, myObjectDto);
        // Then
        Assertions.assertThat(objectDto).isNotNull();
        Assertions.assertThat(objectDto.getFileInfo()).isEqualTo(new FileInfoDto("filename", "lastname"));
    }

    @Test
    @RunWithCustomExecutor
    public void testUpdateOrSaveObjectGroup_ko_with_qualifier() throws Exception {
        // Given
        when(metadataRepository.selectObjectGroupById("og", true)).thenReturn(
            JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_OBJECT_GROUP_WITH_QUALIFIER)));
        VitamThreadUtils.getVitamSession().setTenantId(1);
        CollectUnitModel unitModel = new CollectUnitModel();
        unitModel.setId("1");
        unitModel.setOpi("opi");
        unitModel.setOg("og");
        ObjectDto myObjectDto = new ObjectDto("1", new FileInfoDto("filename", "lastname"));
        // When - Then
        assertThrows(CollectInternalException.class,
            () -> collectService.updateOrSaveObjectGroup(unitModel, BINARY_MASTER, 1, myObjectDto));
    }

    @Test
    @RunWithCustomExecutor
    public void testUpdateOrSaveObjectGroup_ok_with_new_version() throws Exception {
        // Given
        when(metadataRepository.selectObjectGroupById("og", true)).thenReturn(
            JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_OBJECT_GROUP_NEW_VERSION)));
        VitamThreadUtils.getVitamSession().setTenantId(1);
        CollectUnitModel unitModel = new CollectUnitModel();
        unitModel.setId("1");
        unitModel.setOpi("opi");
        unitModel.setOg("og");
        ObjectDto myObjectDto = new ObjectDto("1", new FileInfoDto("filename", "lastname"));
        ObjectDto objectDto = collectService.updateOrSaveObjectGroup(unitModel, BINARY_MASTER, 3, myObjectDto);
        // When
        Assertions.assertThat(objectDto).isNotNull();
        Assertions.assertThat(objectDto.getFileInfo()).isEqualTo(new FileInfoDto("filename", "lastname"));
    }

    @Test
    public void testGetDbObjectGroup_ko_with_IllegalArgumentException() {
        // Given - When - Then
        assertThrows(IllegalArgumentException.class, () -> collectService.getDbObjectGroup(new CollectUnitModel()));
    }

    @Test
    public void testGetDbObjectGroup_ok() throws Exception {
        // Given
        when(metadataRepository.selectObjectGroupById("og", true)).thenReturn(
            JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_OBJECT_GROUP2)));
        CollectUnitModel unitModel = new CollectUnitModel();
        unitModel.setId("1");
        unitModel.setOpi("opi");
        unitModel.setOg("og");
        // When
        DbObjectGroupModel dbObjectGroup = collectService.getDbObjectGroup(unitModel);
        // Then
        Assertions.assertThat(dbObjectGroup).isNotNull();
        Assertions.assertThat(dbObjectGroup.getId()).isEqualTo("aeeaaaaaacfm6tqsaa4kkamaddhfjfyaaaaq");
    }

    @Test
    @RunWithCustomExecutor
    public void testAddBinaryInfoToQualifier() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));
        DbObjectGroupModel dbObjectGroupModel = new DbObjectGroupModel();
        dbObjectGroupModel.setOpi("opi");
        DbQualifiersModel qualifiersModel = new DbQualifiersModel();
        qualifiersModel.setQualifier(BINARY_MASTER.getName());
        List<DbQualifiersModel> dbQualifiersModels = new ArrayList<>();
        dbQualifiersModels.add(qualifiersModel);
        String fileName = "memoire_nationale.txt";
        String versionId = "aebbaaaaacaltpovaewckal62ukh4ml5a67q";
        DataObjectVersionType usage = BINARY_MASTER;
        int version = 1;
        DbVersionsModel versionsModel = new DbVersionsModelBuilder().build(versionId, fileName, usage, version);
        List<DbVersionsModel> dbVersionsModels = new ArrayList<>();
        dbVersionsModels.add(versionsModel);
        qualifiersModel.setVersions(dbVersionsModels);
        dbObjectGroupModel.setQualifiers(dbQualifiersModels);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        Response response =
            Response.ok(new ByteArrayInputStream("ResponseOK".getBytes())).status(Response.Status.OK).build();
        when(workspaceClient.getObject(any(), any())).thenReturn(response);
        FormatIdentifier formatIdentifier = mock(FormatIdentifier.class);
        when(formatIdentifierFactory.getFormatIdentifierFor(anyString())).thenReturn(formatIdentifier);
        when(formatIdentifier.analysePath(any())).thenReturn(List.of(new FormatIdentifierResponse("", "", "", "")));
        // When
        collectService.addBinaryInfoToQualifier(dbObjectGroupModel, BINARY_MASTER, 1,
            StreamUtils.toInputStream("Vitam test"));
        DbVersionsModel objectVersionsModel =
            CollectHelper.getObjectVersionsModel(dbObjectGroupModel, BINARY_MASTER, 1);
        // Then
        Assertions.assertThat(objectVersionsModel.getMessageDigest()).isNotNull();
        Assertions.assertThat(objectVersionsModel.getUri())
            .isEqualTo("Content/aebbaaaaacaltpovaewckal62ukh4ml5a67q.txt");
    }
}