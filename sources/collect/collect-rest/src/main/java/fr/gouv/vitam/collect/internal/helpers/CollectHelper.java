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
package fr.gouv.vitam.collect.internal.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ListMultimap;
import fr.gouv.vitam.collect.external.dto.ProjectDto;
import fr.gouv.vitam.collect.external.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.helpers.builders.ManifestContextBuilder;
import fr.gouv.vitam.collect.internal.model.ManifestContext;
import fr.gouv.vitam.collect.internal.model.ProjectModel;
import fr.gouv.vitam.collect.internal.model.ProjectStatus;
import fr.gouv.vitam.collect.internal.model.TransactionModel;
import fr.gouv.vitam.collect.internal.model.TransactionStatus;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.format.identification.siegfried.FormatIdentifierSiegfried;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.DbQualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.DbVersionsModel;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertEntry;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;

public class CollectHelper {

    public static final String UP = "_up";

    private CollectHelper() throws IllegalAccessException {
        throw new IllegalAccessException("Utility class!");
    }

    public static DataObjectVersionType fetchUsage(String usageString) throws CollectException {
        DataObjectVersionType usage = DataObjectVersionType.fromName(usageString);
        if (usage == null) {
            throw new CollectException("This usage is not permit");
        }
        return usage;
    }

    public static FormatIdentifierResponse getFirstPronomFormat(List<FormatIdentifierResponse> formats) {
        return formats.stream()
            .filter(format -> FormatIdentifierSiegfried.PRONOM_NAMESPACE.equals(format.getMatchedNamespace()))
            .findFirst().orElse(null);
    }

    public static DbVersionsModel getObjectVersionsModel(DbObjectGroupModel dbObjectGroupModel,
        DataObjectVersionType usage, int version) {

        if (dbObjectGroupModel.getQualifiers() == null) {
            return null;
        }

        final String dataObjectVersion = usage.getName() + "_" + version;

        return dbObjectGroupModel.getQualifiers().stream()
            .filter(dbQualifiersModel -> usage.getName().equals(dbQualifiersModel.getQualifier()))
            .flatMap(dbQualifiersModel -> dbQualifiersModel.getVersions().stream())
            .filter(dbVersionsModel -> dataObjectVersion.equals(dbVersionsModel.getDataObjectVersion())).findFirst()
            .orElse(null);
    }

    public static int getLastVersion(DbQualifiersModel qualifierModelToUpdate) {
        return qualifierModelToUpdate.getVersions().stream().map(DbVersionsModel::getDataObjectVersion)
            .map(dataObjectVersion -> dataObjectVersion.split("_")[1]).map(Integer::parseInt)
            .max(Comparator.naturalOrder()).orElse(0);
    }

    public static DbQualifiersModel findQualifier(List<DbQualifiersModel> qualifiers, DataObjectVersionType usage) {
        return qualifiers.stream().filter(qualifier -> qualifier.getQualifier().equals(usage.getName())).findFirst()
            .orElse(null);
    }

    public static void checkVersion(int version, int lastVersion) {
        if (version != lastVersion) {
            throw new IllegalArgumentException("version number not valid " + version);
        }
    }

    public static void createGraph(ListMultimap<String, String> multimap, Set<String> originatingAgencies,
        Map<String, String> ogs, JsonNode result) {
        String archiveUnitId = result.get(id()).asText();
        ArrayNode nodes = (ArrayNode) result.get(VitamFieldsHelper.unitups());
        for (JsonNode node : nodes) {
            multimap.put(node.asText(), archiveUnitId);
        }
        Optional<JsonNode> originatingAgency = Optional.ofNullable(result.get(VitamFieldsHelper.originatingAgency()));
        originatingAgency.ifPresent(jsonNode -> originatingAgencies.add(jsonNode.asText()));
        JsonNode objectIdNode = result.get(VitamFieldsHelper.object());
        if (objectIdNode != null) {
            ogs.put(archiveUnitId, objectIdNode.asText());
        }
    }

    public static List<BulkUnitInsertEntry> fetchBulkUnitInsertEntries(ObjectNode unitJson) {
        if (null != unitJson.get(UP) && unitJson.get(UP).size() != 0) {
            Set<String> parentUnitIds =
                StreamSupport.stream(unitJson.get(UP).spliterator(), false).map(JsonNode::asText)
                    .collect(Collectors.toSet());
            return Collections.singletonList(new BulkUnitInsertEntry(parentUnitIds, unitJson));
        }
        return Collections.singletonList(new BulkUnitInsertEntry(Collections.emptySet(), unitJson));
    }

    public static ProjectDto convertProjectModeltoProjectDto(ProjectModel projectModel) {
        ProjectDto projectDto = new ProjectDto();
        projectDto.setId(projectModel.getId());
        projectDto.setName(projectModel.getName());
        projectDto.setCreationDate(projectModel.getCreationDate());
        projectDto.setLastUpdate(projectModel.getLastUpdate());
        projectDto.setStatus(Objects.requireNonNullElse(projectModel.getStatus(), ProjectStatus.OPEN).toString());
        projectDto.setTenant(projectModel.getTenant());
        if (projectModel.getManifestContext() != null) {
            projectDto.setArchivalAgreement(projectModel.getManifestContext().getArchivalAgreement());
            projectDto.setMessageIdentifier(projectModel.getManifestContext().getMessageIdentifier());
            projectDto.setArchivalAgencyIdentifier(projectModel.getManifestContext().getArchivalAgencyIdentifier());
            projectDto.setTransferringAgencyIdentifier(
                projectModel.getManifestContext().getTransferringAgencyIdentifier());
            projectDto.setOriginatingAgencyIdentifier(
                projectModel.getManifestContext().getOriginatingAgencyIdentifier());
            projectDto.setSubmissionAgencyIdentifier(projectModel.getManifestContext().getSubmissionAgencyIdentifier());
            projectDto.setArchivalProfile(projectModel.getManifestContext().getArchivalProfile());
            projectDto.setComment(projectModel.getManifestContext().getComment());
            projectDto.setAcquisitionInformation(projectModel.getManifestContext().getAcquisitionInformation());
            projectDto.setLegalStatus(projectModel.getManifestContext().getLegalStatus());
            projectDto.setUnitUp(projectModel.getManifestContext().getUnitUp());
        }
        return projectDto;
    }

    public static TransactionDto convertTransactionModelToTransactionDto(TransactionModel transactionModel) {
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setId(transactionModel.getId());
        transactionDto.setName(transactionModel.getName());
        transactionDto.setCreationDate(transactionModel.getCreationDate());
        transactionDto.setLastUpdate(transactionModel.getLastUpdate());
        transactionDto.setStatus(
            Objects.requireNonNullElse(transactionModel.getStatus(), TransactionStatus.OPEN).toString());
        transactionDto.setTenant(transactionModel.getTenant());
        if (transactionModel.getManifestContext() != null) {
            transactionDto.setArchivalAgreement(transactionModel.getManifestContext().getArchivalAgreement());
            transactionDto.setMessageIdentifier(transactionModel.getManifestContext().getMessageIdentifier());
            transactionDto.setArchivalAgencyIdentifier(
                transactionModel.getManifestContext().getArchivalAgencyIdentifier());
            transactionDto.setTransferringAgencyIdentifier(
                transactionModel.getManifestContext().getTransferringAgencyIdentifier());
            transactionDto.setOriginatingAgencyIdentifier(
                transactionModel.getManifestContext().getOriginatingAgencyIdentifier());
            transactionDto.setSubmissionAgencyIdentifier(
                transactionModel.getManifestContext().getSubmissionAgencyIdentifier());
            transactionDto.setArchivalProfile(transactionModel.getManifestContext().getArchivalProfile());
            transactionDto.setComment(transactionModel.getManifestContext().getComment());
            transactionDto.setAcquisitionInformation(transactionModel.getManifestContext().getAcquisitionInformation());
            transactionDto.setLegalStatus(transactionModel.getManifestContext().getLegalStatus());
            transactionDto.setUnitUp(transactionModel.getManifestContext().getUnitUp());
        }
        return transactionDto;
    }

    public static ManifestContext mapProjectDtoToManifestContext(ProjectDto projectDto) {
        return new ManifestContextBuilder().withArchivalAgreement(projectDto.getArchivalAgreement())
            .withMessageIdentifier(projectDto.getMessageIdentifier())
            .withArchivalAgencyIdentifier(projectDto.getArchivalAgencyIdentifier())
            .withTransferringAgencyIdentifier(projectDto.getTransferringAgencyIdentifier())
            .withOriginatingAgencyIdentifier(projectDto.getOriginatingAgencyIdentifier())
            .withSubmissionAgencyIdentifier(projectDto.getSubmissionAgencyIdentifier())
            .withArchivalProfile(projectDto.getArchivalProfile()).withComment(projectDto.getComment())
            .withAcquisitionInformation(projectDto.getAcquisitionInformation())
            .withLegalStatus(projectDto.getLegalStatus())
            .withUnitUp(projectDto.getUnitUp()).build();
    }

    public static ManifestContext mapTransactionDtoToManifestContext(TransactionDto transactionDto) {
        return new ManifestContextBuilder()
            .withArchivalAgreement(transactionDto.getArchivalAgreement())
            .withMessageIdentifier(transactionDto.getMessageIdentifier())
            .withArchivalAgencyIdentifier(transactionDto.getArchivalAgencyIdentifier())
            .withTransferringAgencyIdentifier(transactionDto.getTransferringAgencyIdentifier())
            .withOriginatingAgencyIdentifier(transactionDto.getOriginatingAgencyIdentifier())
            .withSubmissionAgencyIdentifier(transactionDto.getSubmissionAgencyIdentifier())
            .withArchivalProfile(transactionDto.getArchivalProfile()).withComment(transactionDto.getComment())
            .withAcquisitionInformation(transactionDto.getAcquisitionInformation())
            .withLegalStatus(transactionDto.getLegalStatus())
            .withUnitUp(transactionDto.getUnitUp()).build();
    }
}
