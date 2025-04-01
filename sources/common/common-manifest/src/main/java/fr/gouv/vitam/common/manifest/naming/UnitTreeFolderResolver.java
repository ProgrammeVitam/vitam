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

package fr.gouv.vitam.common.manifest.naming;

import fr.gouv.vitam.common.exception.ExportException;
import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UnitTreeFolderResolver implements FolderResolver {

    private static final String PATH_SEPARATOR = "/";
    private static final String DEFAULT_TITLE_LANGUAGE = "fr";
    private static final int MAX_DIRECTORY_NAME_SIZE_LIMIT = 100;

    private final Map<String, ArchiveUnitTreeExportModel> unitsByIds;
    private final Map<String, String> unitIdsByObjectGroupId = new HashMap<>();
    private final Map<String, String> resolvedFolderByUnitId = new HashMap<>();
    private final Set<String> resolvedBasePaths = new HashSet<>();

    public UnitTreeFolderResolver(List<ArchiveUnitTreeExportModel> units) throws ExportException {
        checkDuplicatePathsForUnits(units);
        checkDuplicatePathsForObjectGroups(units);

        this.unitsByIds = units.stream().collect(Collectors.toMap(ArchiveUnitTreeExportModel::id, unit -> unit));

        for (ArchiveUnitTreeExportModel unit : units) {
            Optional<String> objectGroupId = Optional.ofNullable(unit.objectGroupId());
            objectGroupId.ifPresent(ogId -> this.unitIdsByObjectGroupId.put(ogId, unit.id()));
        }
    }

    private static void checkDuplicatePathsForUnits(List<ArchiveUnitTreeExportModel> units) throws ExportException {
        Set<String> unitIds = units.stream().map(ArchiveUnitTreeExportModel::id).collect(Collectors.toSet());
        for (ArchiveUnitTreeExportModel unit : units) {
            List<String> exportedParentUnitIds = unit.parentUnitIds().stream().filter(unitIds::contains).toList();
            if (exportedParentUnitIds.size() > 1) {
                throw new ExportException("Multiple paths for unit with ID " + unit.id());
            }
        }
    }

    private static void checkDuplicatePathsForObjectGroups(List<ArchiveUnitTreeExportModel> units)
        throws ExportException {
        // Ensure no multiple units reference the same object group id
        Set<String> objectGroupIds = new HashSet<>();
        for (ArchiveUnitTreeExportModel unit : units) {
            Optional<String> objectGroupId = Optional.ofNullable(unit.objectGroupId());
            if (objectGroupId.isPresent() && !objectGroupIds.add(objectGroupId.get())) {
                throw new ExportException("Multiple paths for object group with ID " + objectGroupId.get());
            }
        }
    }

    @Override
    public String resolve(String objectGroupId) {
        if (!this.unitIdsByObjectGroupId.containsKey(objectGroupId)) {
            throw new IllegalStateException("Unknown unit for object group with ID " + objectGroupId);
        }

        String unitId = this.unitIdsByObjectGroupId.get(objectGroupId);

        return resolveUnitPath(unitId);
    }

    private String resolveUnitPath(String unitId) {
        if (resolvedFolderByUnitId.containsKey(unitId)) {
            return resolvedFolderByUnitId.get(unitId);
        }

        ArchiveUnitTreeExportModel unit = unitsByIds.get(unitId);
        if (unit == null) {
            throw new IllegalStateException("Unknown unit with ID " + unitId);
        }

        List<String> exportParentUnitIds = unit.parentUnitIds().stream().filter(unitsByIds::containsKey).toList();

        if (exportParentUnitIds.size() > 2) {
            throw new IllegalStateException("Multiple paths not expected for unit with ID " + unitId);
        }

        String baseParentPath;
        if (exportParentUnitIds.isEmpty()) {
            baseParentPath = "";
        } else {
            String parentUnitId = exportParentUnitIds.get(0);
            baseParentPath = resolveUnitPath(parentUnitId) + PATH_SEPARATOR;
        }

        String title = getTitle(unit);
        String normalizedTitle = FileNameCleaner.cleanFileName(title);

        if (
            this.resolvedBasePaths.contains(baseParentPath + normalizedTitle) ||
            normalizedTitle.length() > MAX_DIRECTORY_NAME_SIZE_LIMIT
        ) {
            // If duplicate path OR path is too long ==> truncate unit title & suffix with guid
            normalizedTitle = truncateAndAddSuffix(normalizedTitle, unitId);

            if (this.resolvedBasePaths.contains(baseParentPath + normalizedTitle)) {
                throw new IllegalStateException(
                    "Deduplicated title still exists for unit with ID " + unitId + ". Title: '" + normalizedTitle + "'"
                );
            }
        }

        this.resolvedBasePaths.add(baseParentPath + normalizedTitle);
        this.resolvedFolderByUnitId.put(unitId, baseParentPath + normalizedTitle);
        return baseParentPath + normalizedTitle;
    }

    private static String truncateAndAddSuffix(String sanitizedTitle, String unitId) {
        int maxLength = Math.min(MAX_DIRECTORY_NAME_SIZE_LIMIT, sanitizedTitle.length() + 1 + unitId.length());
        return sanitizedTitle.substring(0, maxLength - unitId.length() - 1) + "_" + unitId;
    }

    private static String getTitle(ArchiveUnitTreeExportModel unit) {
        if (unit.title() != null) {
            return unit.title();
        }
        if (MapUtils.isEmpty(unit.title_())) {
            throw new IllegalStateException("Unit with ID " + unit.id() + " does not have a Title or Title_ field");
        }
        // If "Title" is missing, and multiple "Title_.*" fields are found, use "fr" as default
        if (unit.title_().containsKey(DEFAULT_TITLE_LANGUAGE)) {
            return unit.title_().get(DEFAULT_TITLE_LANGUAGE);
        }
        return unit.title_().values().iterator().next();
    }
}
