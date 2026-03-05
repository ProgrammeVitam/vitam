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
package fr.gouv.vitam.collect;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.external.client.CollectExternalClient;
import fr.gouv.vitam.collect.external.client.CollectExternalClientFactory;
import fr.gouv.vitam.collect.external.exception.CollectExternalClientInvalidRequestException;
import fr.gouv.vitam.collect.external.rest.CollectExternalMain;
import fr.gouv.vitam.collect.internal.CollectInternalMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;

import static fr.gouv.vitam.collect.CollectTestHelper.initProjectData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertTrue;

public class ProjectIT extends VitamRuleRunner {

    private static final String JSLT_TRANSFORMATION =
        """
        {
          "*": ., // Copies all fields from the input JSON
          "Status": "Verified" // Adds a new field
        }""";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        ProjectIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            CollectInternalMain.class,
            CollectExternalMain.class
        )
    );

    private static final Integer TENANT_ID = 0;
    private final VitamContext vitamContext = new VitamContext(TENANT_ID);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        runner.stopMetadataServer(true);
        handleAfterClass();
        runAfter();
        fr.gouv.vitam.common.client.VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Test
    public void should_create_project() throws VitamClientException, InvalidParseOperationException {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            // Given
            ProjectDto projectDto = initProjectData();

            // When
            RequestResponse<JsonNode> createdProject = client.initProject(vitamContext, projectDto);
            ProjectDto projectDtoResult = JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) createdProject).getFirstResult(),
                ProjectDto.class
            );

            // Then
            checkProjectDto(projectDtoResult, projectDto);
        }
    }

    @Test
    public void should_create_project_with_jslt_transformation()
        throws VitamClientException, InvalidParseOperationException {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            // Given
            ProjectDto projectDto = initProjectData();
            projectDto.setTransformationRules(JSLT_TRANSFORMATION);

            // When
            RequestResponse<JsonNode> createdProject = client.initProject(vitamContext, projectDto);
            ProjectDto projectDtoResult = JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) createdProject).getFirstResult(),
                ProjectDto.class
            );

            // Then
            checkProjectDto(projectDtoResult, projectDto);
        }
    }

    @Test
    public void should_not_create_project_with_invalid_jslt_transformation() {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            // Given
            ProjectDto projectDto = initProjectData();
            projectDto.setTransformationRules("invalid");

            // When / Then
            assertThatThrownBy(() -> client.initProject(vitamContext, projectDto))
                .isInstanceOf(CollectExternalClientInvalidRequestException.class)
                .hasMessageContaining("Invalid JSLT template: Parse error");
        }
    }

    @Test
    public void should_update_project() throws VitamClientException, InvalidParseOperationException, ParseException {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            // GIVEN
            ProjectDto initialProjectDto = initProjectData();
            RequestResponse<JsonNode> createdProject = client.initProject(vitamContext, initialProjectDto);
            initialProjectDto = JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) createdProject).getFirstResult(),
                ProjectDto.class
            );

            ProjectDto projectDtoResult = getProjectDtoById(initialProjectDto.getId());
            assertThat(projectDtoResult).isNotNull();
            assertThat(projectDtoResult.getCreationDate()).isNotNull();
            assertThat(projectDtoResult.getId()).isEqualTo(initialProjectDto.getId());
            assertThat(LocalDateUtil.getDate(projectDtoResult.getCreationDate())).isEqualTo(
                LocalDateUtil.getDate(projectDtoResult.getLastUpdate())
            );

            // WHEN
            projectDtoResult.setComment("COMMENT AFTER UPDATE");
            projectDtoResult.setTransformationRules(JSLT_TRANSFORMATION);
            client.updateProject(vitamContext, projectDtoResult);
            ProjectDto projectDtoResultAfterUpdate = getProjectDtoById(projectDtoResult.getId());

            // THEN
            assertThat(projectDtoResultAfterUpdate.getComment()).isNotEqualTo(initialProjectDto.getComment());
            assertThat(projectDtoResultAfterUpdate.getComment()).isEqualTo("COMMENT AFTER UPDATE");
            assertThat(projectDtoResultAfterUpdate.getTransformationRules()).isEqualTo(JSLT_TRANSFORMATION);
            assertThat(projectDtoResultAfterUpdate.getLastUpdate()).isNotNull();
            assertTrue(
                LocalDateUtil.getDate(projectDtoResultAfterUpdate.getLastUpdate()).after(
                    LocalDateUtil.getDate(projectDtoResultAfterUpdate.getCreationDate())
                )
            );
        }
    }

    @Test
    public void should_not_update_project_with_invalid_jslt_transformation()
        throws VitamClientException, InvalidParseOperationException, ParseException {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            // GIVEN
            ProjectDto initialProjectDto = initProjectData();
            RequestResponse<JsonNode> createdProject = client.initProject(vitamContext, initialProjectDto);
            ProjectDto projectDtoResult = JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) createdProject).getFirstResult(),
                ProjectDto.class
            );

            // WHEN
            projectDtoResult.setComment("COMMENT AFTER UPDATE");
            projectDtoResult.setTransformationRules("Invalid");
            assertThatThrownBy(() -> client.updateProject(vitamContext, projectDtoResult)).isInstanceOf(
                CollectExternalClientInvalidRequestException.class
            );

            ProjectDto projectDtoResultAfterUpdate = getProjectDtoById(projectDtoResult.getId());

            // THEN
            assertThat(projectDtoResultAfterUpdate.getComment()).isEqualTo(initialProjectDto.getComment());
            assertThat(projectDtoResultAfterUpdate.getTransformationRules()).isEqualTo(
                initialProjectDto.getTransformationRules()
            );
            assertThat(projectDtoResult.getCreationDate()).isEqualTo(projectDtoResult.getLastUpdate());
        }
    }

    @Test
    public void should_delete_project() throws VitamClientException, InvalidParseOperationException, ParseException {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            // GIVEN
            ProjectDto projectDto = initProjectData();
            RequestResponse<JsonNode> createdProject = client.initProject(vitamContext, projectDto);
            projectDto = JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) createdProject).getFirstResult(),
                ProjectDto.class
            );

            ProjectDto projectDtoResult = getProjectDtoById(projectDto.getId());
            assertThat(projectDtoResult).isNotNull();
            assertThat(projectDtoResult.getCreationDate()).isNotNull();
            assertThat(projectDtoResult.getId()).isEqualTo(projectDto.getId());
            assertThat(LocalDateUtil.getDate(projectDtoResult.getCreationDate())).isEqualTo(
                LocalDateUtil.getDate(projectDtoResult.getLastUpdate())
            );

            // WHEN
            client.deleteProjectById(vitamContext, projectDto.getId());

            assertThatCode(() -> getProjectDtoById(projectDtoResult.getId())).isInstanceOf(VitamClientException.class);
        }
    }

    private ProjectDto getProjectDtoById(String projectId) throws VitamClientException, InvalidParseOperationException {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            RequestResponseOK<JsonNode> response = (RequestResponseOK<JsonNode>) client.getProjectById(
                vitamContext,
                projectId
            );
            assertThat(response.isOk()).isTrue();
            return JsonHandler.getFromJsonNode(response.getFirstResult(), ProjectDto.class);
        }
    }

    private static void checkProjectDto(ProjectDto projectDtoResult, ProjectDto projectDto) {
        assertThat(projectDtoResult.getId()).isNotNull();
        assertThat(projectDtoResult.getTenant()).isEqualTo(TENANT_ID);
        assertThat(projectDtoResult.getTransferringAgencyIdentifier()).isEqualTo(
            projectDto.getTransferringAgencyIdentifier()
        );
        assertThat(projectDtoResult.getOriginatingAgencyIdentifier()).isEqualTo(
            projectDto.getOriginatingAgencyIdentifier()
        );
        assertThat(projectDtoResult.getSubmissionAgencyIdentifier()).isEqualTo(
            projectDto.getSubmissionAgencyIdentifier()
        );
        assertThat(projectDtoResult.getMessageIdentifier()).isEqualTo(projectDto.getMessageIdentifier());
        assertThat(projectDtoResult.getArchivalAgencyIdentifier()).isEqualTo(projectDto.getArchivalAgencyIdentifier());
        assertThat(projectDtoResult.getLegalStatus()).isEqualTo(projectDto.getLegalStatus());
        assertThat(projectDtoResult.getComment()).isEqualTo(projectDto.getComment());
        assertThat(projectDtoResult.getName()).isEqualTo(projectDto.getName());
        assertThat(projectDtoResult.getArchivalAgreement()).isEqualTo(projectDto.getArchivalAgreement());
        assertThat(projectDtoResult.getTransformationRules()).isEqualTo(projectDto.getTransformationRules());
        assertThat(projectDtoResult.getCreationDate()).isNotNull();
        assertThat(projectDtoResult.getLastUpdate()).isNotNull();
        assertThat(projectDtoResult.getCreationDate()).isEqualTo(projectDtoResult.getLastUpdate());
    }
}
