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

import fr.gouv.vitam.collect.common.dto.CriteriaProjectDto;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.ProjectModel;
import fr.gouv.vitam.collect.internal.core.common.ProjectStatus;
import fr.gouv.vitam.collect.internal.core.exceptions.CollectInvalidJsltTransformerException;
import fr.gouv.vitam.collect.internal.core.repository.ProjectRepository;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static fr.gouv.vitam.collect.internal.core.service.ProjectService.ARCHIVING_FIELDS_CREATION_CONSTRAINT;
import static fr.gouv.vitam.collect.internal.core.service.ProjectService.ARCHIVING_FIELDS_MODIFICATION_FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectServiceTest {

    private static final Integer TENANT_ID = 0;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final String PROJECT_ID = "PROJECT_ID";
    private static final String PROJECT_TITLE = "My Project";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Mock
    private ProjectRepository projectRepository;

    private ProjectService projectService;

    private static List<ProjectModel> listOfProject;

    @BeforeClass
    public static void setUpBeforeClass() {
        final ProjectModel project = new ProjectModel();
        project.setName(PROJECT_TITLE);
        project.setStatus(ProjectStatus.OPEN);
        listOfProject = List.of(project);
    }

    @Before
    public void setUp() {
        projectService = new ProjectService(projectRepository);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void createProject() throws CollectInternalException {
        logicalClock.freezeTime();
        final String currentTime = LocalDateUtil.nowFormatted();
        ProjectDto project = projectService.createProject(
            new ProjectDto()
                .setAcquisitionInformation("AcquisitionInformation")
                .setComment("Comment")
                .setArchivalProfile("ArchivalProfile")
                .setName("My Project")
                .setArchivalAgencyIdentifier("ArchivalAgencyIdentifier")
                .setArchivalAgreement("ArchivalAgreement")
                .setAutomaticIngest(Boolean.TRUE)
                .setLegalStatus("LegalStatus")
                .setMessageIdentifier("MessageIdentifier")
                .setTransferringAgencyIdentifier("TransferringAgencyIdentifier")
                .setTransformationRules("{}")
                .setConnectedToArchivingSystem(Boolean.TRUE)
                .setArchivingSystemId("Remote-Vitam")
                .setArchivingSystemTenant(1)
        );

        assertThat(project.getId()).isNotNull();
        assertThat(project.getTenant()).isEqualTo(TENANT_ID);
        assertThat(project.getCreationDate()).isEqualTo(currentTime);
        assertThat(project.getLastUpdate()).isEqualTo(currentTime);
        assertThat(project.getAcquisitionInformation()).isEqualTo("AcquisitionInformation");
        assertThat(project.getComment()).isEqualTo("Comment");
        assertThat(project.getArchivalProfile()).isEqualTo("ArchivalProfile");
        assertThat(project.getName()).isEqualTo("My Project");
        assertThat(project.getArchivalAgencyIdentifier()).isEqualTo("ArchivalAgencyIdentifier");
        assertThat(project.getArchivalAgreement()).isEqualTo("ArchivalAgreement");
        assertThat(project.getAutomaticIngest()).isEqualTo(Boolean.TRUE);
        assertThat(project.getLegalStatus()).isEqualTo("LegalStatus");
        assertThat(project.getMessageIdentifier()).isEqualTo("MessageIdentifier");
        assertThat(project.getTransferringAgencyIdentifier()).isEqualTo("TransferringAgencyIdentifier");
        assertThat(project.getTransformationRules()).isEqualTo("{}");
        assertThat(project.getConnectedToArchivingSystem()).isEqualTo(Boolean.TRUE);
        assertThat(project.getArchivingSystemId()).isEqualTo("Remote-Vitam");
        assertThat(project.getArchivingSystemTenant()).isEqualTo(1);
    }

    @Test
    @RunWithCustomExecutor
    public void createProject_shouldFailWhenArchivingFieldsAreInconsistent() {
        // Case 1: connected = true but missing archivingSystemId
        ProjectDto case1 = new ProjectDto()
            .setConnectedToArchivingSystem(true)
            .setArchivingSystemId(null)
            .setArchivingSystemTenant(42); // only tenant provided

        assertThatThrownBy(() -> projectService.createProject(case1))
            .isInstanceOf(CollectInternalException.class)
            .hasMessageContaining(ARCHIVING_FIELDS_CREATION_CONSTRAINT);

        // Case 2: connected = false but archiving fields are set
        ProjectDto case2 = new ProjectDto()
            .setConnectedToArchivingSystem(false)
            .setArchivingSystemId("System-X")
            .setArchivingSystemTenant(42);

        assertThatThrownBy(() -> projectService.createProject(case2))
            .isInstanceOf(CollectInternalException.class)
            .hasMessageContaining(ARCHIVING_FIELDS_CREATION_CONSTRAINT);

        // Case 3: connected = false but archivingSystemId is set alone
        ProjectDto case3 = new ProjectDto().setConnectedToArchivingSystem(false).setArchivingSystemId("System-Y");

        assertThatThrownBy(() -> projectService.createProject(case3))
            .isInstanceOf(CollectInternalException.class)
            .hasMessageContaining(ARCHIVING_FIELDS_CREATION_CONSTRAINT);

        // Case 4: connected = true but both archiving fields are missing
        ProjectDto case4 = new ProjectDto().setConnectedToArchivingSystem(true);

        assertThatThrownBy(() -> projectService.createProject(case4))
            .isInstanceOf(CollectInternalException.class)
            .hasMessageContaining(ARCHIVING_FIELDS_CREATION_CONSTRAINT);
    }

    @Test
    @RunWithCustomExecutor
    public void updateProject_shouldNotAllowArchivingSystemFieldsModification() throws CollectInternalException {
        ProjectDto createdProject = projectService.createProject(
            new ProjectDto()
                .setName("My Project")
                .setArchivingSystemId("Remote-Vitam")
                .setArchivingSystemTenant(1)
                .setConnectedToArchivingSystem(Boolean.TRUE)
        );

        ProjectDto updateAttempt = new ProjectDto()
            .setId(createdProject.getId())
            .setArchivingSystemId("HACKED-System")
            .setArchivingSystemTenant(99)
            .setConnectedToArchivingSystem(Boolean.FALSE);

        assertThatThrownBy(() -> projectService.updateProject(updateAttempt, createdProject))
            .isInstanceOf(CollectInternalException.class)
            .hasMessageContaining(ARCHIVING_FIELDS_MODIFICATION_FORBIDDEN);
    }

    @Test
    @RunWithCustomExecutor
    public void updateProject_shouldAllowArchivingSystemFieldsModification() throws CollectInternalException {
        // Create the initial project with archiving fields set
        ProjectDto createdProject = projectService.createProject(
            new ProjectDto()
                .setName("My Project")
                .setArchivingSystemId("Remote-Vitam")
                .setArchivingSystemTenant(1)
                .setConnectedToArchivingSystem(Boolean.TRUE)
        );

        // Attempt to update the project without modifying archiving fields
        ProjectDto updateAttempt = new ProjectDto()
            .setId(createdProject.getId())
            .setName("Updated Project Name") // allowed change
            .setArchivingSystemId("Remote-Vitam") // unchanged
            .setArchivingSystemTenant(1) // unchanged
            .setConnectedToArchivingSystem(Boolean.TRUE); // unchanged

        ProjectDto updatedProject = projectService.updateProject(updateAttempt, createdProject);

        // Vérifications
        assertThat(updatedProject.getId()).isEqualTo(createdProject.getId());
        assertThat(updatedProject.getName()).isEqualTo("Updated Project Name");
        assertThat(updatedProject.getArchivingSystemId()).isEqualTo("Remote-Vitam");
        assertThat(updatedProject.getArchivingSystemTenant()).isEqualTo(1);
        assertThat(updatedProject.getConnectedToArchivingSystem()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void createProjectWithInvalidJsltTransformation() throws CollectInternalException {
        logicalClock.freezeTime();
        assertThatThrownBy(() -> projectService.createProject(new ProjectDto().setTransformationRules("invalid")))
            .isInstanceOf(CollectInvalidJsltTransformerException.class)
            .hasMessageStartingWith("Invalid JSLT template: Parse error");
    }

    @Test
    @RunWithCustomExecutor
    public void findProject() throws CollectInternalException {
        projectService.findProject(PROJECT_ID);
        Mockito.verify(projectRepository).findProjectById(eq(PROJECT_ID));
    }

    @Test
    @RunWithCustomExecutor
    public void updateProject_changes_lastUpdate() throws CollectInternalException {
        final ProjectDto projectDto = new ProjectDto(PROJECT_ID);
        final LocalDateTime creationDateTime = LocalDateUtil.now();
        projectDto.setCreationDate(LocalDateUtil.getFormattedDateTimeForMongo(creationDateTime));
        logicalClock.logicalSleep(1, ChronoUnit.DAYS);
        logicalClock.freezeTime();
        final LocalDateTime currentTime = LocalDateUtil.now();
        // When
        projectService.updateProject(projectDto, projectDto);
        // Then
        Mockito.verify(projectRepository).updateProject(
            argThat(
                projectModel ->
                    PROJECT_ID.equals(projectModel.getId()) &&
                    creationDateTime.isEqual(LocalDateUtil.parseMongoFormattedDate(projectModel.getCreationDate())) &&
                    currentTime.equals(LocalDateUtil.parseMongoFormattedDate(projectModel.getLastUpdate()))
            )
        );
    }

    @Test
    @RunWithCustomExecutor
    public void findProjects() throws CollectInternalException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(projectRepository.searchProject(isNull(), eq(TENANT_ID))).thenReturn(listOfProject);
        final List<ProjectDto> projectsByTenant = projectService.searchProject(null);
        assertThat(projectsByTenant).extracting(ProjectDto::getName).containsOnly(PROJECT_TITLE);
    }

    @Test
    @RunWithCustomExecutor
    public void searchProject() throws CollectInternalException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final CriteriaProjectDto criteriaProjectDto = new CriteriaProjectDto();
        criteriaProjectDto.setQuery(PROJECT_TITLE);
        when(projectRepository.searchProject(eq(criteriaProjectDto), eq(TENANT_ID))).thenReturn(listOfProject);
        final List<ProjectDto> projects = projectService.searchProject(criteriaProjectDto);
        assertThat(projects).extracting(ProjectDto::getName).containsOnly(PROJECT_TITLE);
    }

    @Test
    @RunWithCustomExecutor
    public void deleteProjectById() {
        projectService.deleteProjectById(PROJECT_ID);
        verify(projectRepository).deleteProject(PROJECT_ID);
    }
}
