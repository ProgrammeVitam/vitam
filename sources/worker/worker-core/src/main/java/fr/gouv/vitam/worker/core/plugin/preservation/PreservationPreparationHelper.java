/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.plugin.preservation;

import fr.gouv.vitam.common.model.objectgroup.FormatIdentificationModel;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static fr.gouv.vitam.common.model.PreservationRequest.DEFAULT_VERSION;
import static java.util.Optional.empty;

/**
 * PreservationPreparationHelper class
 */
class PreservationPreparationHelper {

    private PreservationPreparationHelper() {
    }

    static Optional<FormatIdentificationModel> getFormatModelFromObjectGroupModelGivenQualifierAndVersion(
        ObjectGroupResponse objectGroupModel, String qualifier, String version) {

        Optional<VersionsModel> modelOptional =
            getVersionsModelFromObjectGroupModelGivenQualifierAndVersion(objectGroupModel, qualifier, version);

        if (!modelOptional.isPresent()) {
            return empty();
        }

        VersionsModel versionsModel = modelOptional.get();
        FormatIdentificationModel formatIdentification = versionsModel.getFormatIdentification();

        return Optional.of(formatIdentification);
    }

    static Optional<VersionsModel> getVersionsModelFromObjectGroupModelGivenQualifierAndVersion(
        ObjectGroupResponse objectGroupModel, String qualifier, String version) {

        Predicate<QualifiersModel> qualifierPredicate = q -> q.getQualifier().equals(qualifier);

        List<QualifiersModel> qualifiers = objectGroupModel.getQualifiers();
        Optional<QualifiersModel> first = qualifiers.stream().filter(qualifierPredicate).findFirst();

        if (!first.isPresent()) {
            return empty();
        }

        List<VersionsModel> versions = first.get().getVersions();

        boolean checkSpecificVersion = !version.equals(DEFAULT_VERSION);

        if (checkSpecificVersion) {

            Predicate<VersionsModel> versionPredicate = v -> v.getDataObjectVersion().equals(qualifier + "_" + version);

            return versions.stream().filter(versionPredicate).findFirst();
        }

        Comparator<VersionsModel> versionsModelComparator = Comparator.comparing(VersionsModel::getDataObjectVersion);

        return versions.stream().max(versionsModelComparator);
    }
}
