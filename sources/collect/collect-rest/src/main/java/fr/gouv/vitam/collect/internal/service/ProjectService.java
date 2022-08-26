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
import fr.gouv.vitam.collect.internal.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.helpers.builders.ProjectModelBuilder;
import fr.gouv.vitam.collect.internal.model.ProjectModel;
import fr.gouv.vitam.collect.internal.model.TransactionStatus;
import fr.gouv.vitam.collect.internal.repository.ProjectRepository;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProjectService {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProjectService.class);
    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * create a project model
     *
     * @throws CollectException exception thrown in case of error
     */
    public void createProject(ProjectDto projectDto) throws CollectException {
        // Set project initials
        final String initialDate = LocalDateUtil.now().toString();
        projectDto.setStatus(TransactionStatus.OPEN.name());
        projectDto.setCreationDate(initialDate);
        projectDto.setLastUpdate(initialDate);

        ProjectModel projectModel = new ProjectModelBuilder()
            .withId(projectDto.getId())
            .withManifestContext(CollectHelper.mapProjectDtoToManifestContext(projectDto))
            .withTenant(projectDto.getTenant())
            .build();

        projectRepository.createProject(projectModel);
    }

    /**
     * return project according to id
     *
     * @param id model id to find
     * @return Optional<ProjectModel>
     * @throws CollectException exception thrown in case of error
     */
    public Optional<ProjectModel> findProject(String id) throws CollectException {
        return projectRepository.findProjectById(id);
    }


    public void replaceProject(ProjectDto projectDto) throws CollectException {
        // Update project initials
        projectDto.setStatus(projectDto.getStatus() != null ? projectDto.getStatus() : TransactionStatus.OPEN.name());
        projectDto.setLastUpdate(LocalDateUtil.now().toString());

        ProjectModel projectModel = new ProjectModelBuilder()
            .withId(projectDto.getId())
            .withManifestContext(CollectHelper.mapProjectDtoToManifestContext(projectDto))
            .withTenant(projectDto.getTenant())
            .build();
        projectRepository.replaceProject(projectModel);
    }

    public List<ProjectDto> findProjectsByTenant(Integer tenant) throws CollectException {
        List<ProjectModel> listProjects = projectRepository.findProjectsByTenant(tenant);
        return listProjects.stream().map(CollectHelper::convertProjectModeltoProjectDto)
            .collect(Collectors.toList());
    }

    /**
     * delete project according to id
     *
     * @param id project to delete
     */
    public void deleteProjectById(String id) {
        projectRepository.deleteProject(id);
    }

}
