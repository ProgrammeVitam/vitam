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

package fr.gouv.vitam.collect.internal.service;

import fr.gouv.vitam.collect.external.dto.ProjectDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.model.ProjectModel;
import fr.gouv.vitam.collect.internal.model.ProjectStatus;
import fr.gouv.vitam.collect.internal.repository.ProjectRepository;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import org.assertj.core.api.Assertions;
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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectServiceTest {
    private static final Integer TENANT_ID = 0;
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    private final static String PROJECT_ID = "PROJECT_ID";
    private final static String PROJECT_TITLE = "My Project";

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
    }

    @Test
    public void createProject() throws CollectException {
        logicalClock.freezeTime();
        final String currentTime = LocalDateUtil.now().toString();
        projectService.createProject(new ProjectDto(PROJECT_ID));

        Mockito.verify(projectRepository).createProject(argThat(e ->
            PROJECT_ID.equals(e.getId()) && currentTime.equals(e.getCreationDate())
                && currentTime.equals(e.getLastUpdate())));
    }

    @Test
    public void findProject() throws CollectException {
        projectService.findProject(PROJECT_ID);
        Mockito.verify(projectRepository).findProjectById(eq(PROJECT_ID));


    }

    @Test
    public void replaceProject() throws CollectException {
        final LocalDateTime creationDate = LocalDateUtil.now();
        logicalClock.logicalSleep(1, ChronoUnit.DAYS);
        logicalClock.freezeTime();
        final LocalDateTime currentTime = LocalDateUtil.now();
        final ProjectDto projectDto = new ProjectDto(PROJECT_ID);
        projectDto.setCreationDate(creationDate.toString());
        projectService.updateProject(projectDto);

        Mockito.verify(projectRepository).replaceProject(argThat(e ->
            PROJECT_ID.equals(e.getId()) &&
                currentTime.isAfter(LocalDateUtil.parseMongoFormattedDate(e.getCreationDate()))
                && currentTime.equals(LocalDateUtil.parseMongoFormattedDate(e.getLastUpdate()))));
    }

    @Test
    @RunWithCustomExecutor
    public void findProjects() throws CollectException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(projectRepository.findProjectsByTenant(TENANT_ID)).thenReturn(listOfProject);
        final List<ProjectDto> projectsByTenant = projectService.findProjects();
        Assertions.assertThat(projectsByTenant).extracting(ProjectDto::getName).containsOnly(PROJECT_TITLE);
    }

    @Test
    @RunWithCustomExecutor
    public void searchProject() throws CollectException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(projectRepository.searchProject(eq(PROJECT_TITLE), eq(TENANT_ID))).thenReturn(listOfProject);
        final List<ProjectDto> projects = projectService.searchProject(PROJECT_TITLE);
        Assertions.assertThat(projects).extracting(ProjectDto::getName).containsOnly(PROJECT_TITLE);
    }

    @Test
    public void deleteProjectById() {
        projectService.deleteProjectById(PROJECT_ID);
        verify(projectRepository).deleteProject(PROJECT_ID);
    }
}