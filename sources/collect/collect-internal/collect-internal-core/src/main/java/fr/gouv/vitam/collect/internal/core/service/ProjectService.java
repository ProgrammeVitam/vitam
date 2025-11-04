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
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.common.exception.CollectInternalInvalidRequestException;
import fr.gouv.vitam.collect.internal.core.common.ProjectModel;
import fr.gouv.vitam.collect.internal.core.common.ProjectStatus;
import fr.gouv.vitam.collect.internal.core.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.core.repository.ProjectRepository;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.exception.InvalidJstlTransformerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsltTransformer;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProjectService {

    public static final String ARCHIVING_FIELDS_CREATION_CONSTRAINT =
        "Fields 'ArchivingSystemId' and 'ArchivingSystemTenant' must be null when 'ConnectedToArchivingSystem' is not true, and must not be null when 'ConnectedToArchivingSystem' is true";

    public static final String ARCHIVING_FIELDS_MODIFICATION_FORBIDDEN =
        "Fields 'ArchivingSystemId', 'ArchivingSystemTenant' and 'ConnectedToArchivingSystem' must not be changed";

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * create a project model
     */
    public ProjectDto createProject(ProjectDto projectDto) throws CollectInternalException {
        Boolean connectedToArchivingSystem = projectDto.getConnectedToArchivingSystem();
        String archivingSystemId = projectDto.getArchivingSystemId();
        Integer archivingSystemTenant = projectDto.getArchivingSystemTenant();

        if (Boolean.TRUE.equals(connectedToArchivingSystem)) {
            if (
                Objects.isNull(archivingSystemId) || Objects.isNull(archivingSystemTenant)
            ) throw new CollectInternalException(ARCHIVING_FIELDS_CREATION_CONSTRAINT);
        } else {
            if (
                Objects.nonNull(archivingSystemId) || Objects.nonNull(archivingSystemTenant)
            ) throw new CollectInternalException(ARCHIVING_FIELDS_CREATION_CONSTRAINT);
        }

        // Set project initials
        final String creationDate = LocalDateUtil.nowFormatted();

        validateJslt(projectDto.getTransformationRules());

        ProjectModel projectModel = new ProjectModel.Builder()
            .id(GUIDFactory.newGUID().getId())
            .name(projectDto.getName())
            .manifestContext(CollectHelper.mapProjectDtoToManifestContext(projectDto))
            .status(ProjectStatus.OPEN)
            .creationDate(creationDate)
            .lastUpdate(creationDate)
            .unitUp(projectDto.getUnitUp())
            .unitUps(projectDto.getUnitUps())
            .tenant(ParameterHelper.getTenantParameter())
            .automaticIngest(projectDto.getAutomaticIngest())
            .archivingSystemId(archivingSystemId)
            .archivingSystemTenant(archivingSystemTenant)
            .connectedToArchivingSystem(connectedToArchivingSystem)
            .transformationRules(projectDto.getTransformationRules())
            .build();

        projectRepository.createProject(projectModel);

        return CollectHelper.convertProjectModeltoProjectDto(projectModel);
    }

    /**
     * return project according to id
     *
     * @param id model id to find
     * @return Optional<ProjectModel>
     */
    public Optional<ProjectDto> findProject(String id) throws CollectInternalException {
        return projectRepository.findProjectById(id).map(CollectHelper::convertProjectModeltoProjectDto);
    }

    public ProjectDto updateProject(ProjectDto projectDto, ProjectDto existingProjectDto)
        throws CollectInternalException {
        if (
            !Objects.equals(existingProjectDto.getArchivingSystemId(), projectDto.getArchivingSystemId()) ||
            !Objects.equals(existingProjectDto.getArchivingSystemTenant(), projectDto.getArchivingSystemTenant()) ||
            !Objects.equals(
                existingProjectDto.getConnectedToArchivingSystem(),
                projectDto.getConnectedToArchivingSystem()
            )
        ) {
            throw new CollectInternalException(ARCHIVING_FIELDS_MODIFICATION_FORBIDDEN);
        }

        // Update project initials
        projectDto.setStatus(projectDto.getStatus() != null ? projectDto.getStatus() : TransactionStatus.OPEN.name());
        final String lastUpdate = LocalDateUtil.nowFormatted();

        validateJslt(projectDto.getTransformationRules());

        ProjectModel projectModel = new ProjectModel.Builder()
            .id(projectDto.getId())
            .name(projectDto.getName())
            .manifestContext(CollectHelper.mapProjectDtoToManifestContext(projectDto))
            .status(ProjectStatus.valueOf(projectDto.getStatus()))
            .creationDate(existingProjectDto.getCreationDate())
            .lastUpdate(lastUpdate)
            .unitUp(projectDto.getUnitUp())
            .unitUps(projectDto.getUnitUps())
            .tenant(existingProjectDto.getTenant())
            .automaticIngest(projectDto.getAutomaticIngest())
            .archivingSystemId(existingProjectDto.getArchivingSystemId())
            .archivingSystemTenant(existingProjectDto.getArchivingSystemTenant())
            .connectedToArchivingSystem(existingProjectDto.getConnectedToArchivingSystem())
            .transformationRules(projectDto.getTransformationRules())
            .build();

        projectRepository.updateProject(projectModel);

        return CollectHelper.convertProjectModeltoProjectDto(projectModel);
    }

    public List<ProjectDto> searchProject(CriteriaProjectDto criteriaProjectDto) throws CollectInternalException {
        final List<ProjectModel> listProjects = projectRepository.searchProject(
            criteriaProjectDto,
            ParameterHelper.getTenantParameter()
        );

        return listProjects.stream().map(CollectHelper::convertProjectModeltoProjectDto).collect(Collectors.toList());
    }

    /**
     * delete project according to id
     *
     * @param id project to delete
     */
    public void deleteProjectById(String id) {
        projectRepository.deleteProject(id);
    }

    private void validateJslt(String transformationRules) throws CollectInternalException {
        if (StringUtils.isBlank(transformationRules)) {
            return;
        }

        try {
            JsltTransformer.validate(transformationRules);
        } catch (InvalidJstlTransformerException e) {
            throw new CollectInternalInvalidRequestException(e.getMessage(), e);
        }
    }
}
