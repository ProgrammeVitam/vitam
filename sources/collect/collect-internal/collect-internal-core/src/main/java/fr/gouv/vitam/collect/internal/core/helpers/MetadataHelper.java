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
package fr.gouv.vitam.collect.internal.core.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import fr.gouv.culture.archivesdefrance.seda.v2.LevelType;
import fr.gouv.vitam.collect.common.dto.MetadataUnitUp;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.FileInfoModel;
import fr.gouv.vitam.common.model.objectgroup.FormatIdentificationModel;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.model.IngestWorkflowConstants.CONTENT_FOLDER;

public class MetadataHelper {

    public static final String STATIC_ATTACHMENT = "STATIC_ATTACHEMENT";
    public static final String DYNAMIC_ATTACHEMENT = "DYNAMIC_ATTACHEMENT";

    private MetadataHelper() {
    }

    public static ArchiveUnitModel createUnit(String transactionId, LevelType descriptionLevel, String title,
        String unitParent) {
        String id = GUIDFactory.newUnitGUID(VitamThreadUtils.getVitamSession().getTenantId()).getId();
        ArchiveUnitModel unitInternalModel = new ArchiveUnitModel();

        unitInternalModel.setId(id);
        unitInternalModel.setOpi(transactionId);
        unitInternalModel.setUnitType(UnitType.INGEST.name());

        DescriptiveMetadataModel description = new DescriptiveMetadataModel();
        description.setTitle(title);
        description.setDescriptionLevel(descriptionLevel);
        unitInternalModel.setDescriptiveMetadataModel(description);
        if (unitParent != null) {
            unitInternalModel.setUnitups(Collections.singletonList(unitParent));
        }
        return unitInternalModel;
    }

    public static ObjectGroupResponse createObjectGroup(String transactionId, String fileName, String objectId,
        String newFilename, FormatIdentifierResponse format, String digest, Long size) {


        FileInfoModel fileInfoModel = new FileInfoModel();
        fileInfoModel.setFilename(fileName);
        fileInfoModel.setLastModified(LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now()));

        QualifiersModel qualifiersModel = new QualifiersModel();


        VersionsModel versionsModel = new VersionsModel();
        versionsModel.setId(objectId);
        versionsModel.setFileInfoModel(fileInfoModel);
        versionsModel.setDataObjectVersion(DataObjectVersionType.BINARY_MASTER.getName() + "_" + 1);
        versionsModel.setMessageDigest(digest);
        versionsModel.setAlgorithm(VitamConfiguration.getDefaultDigestType().getName());
        versionsModel.setSize(size);
        versionsModel.setUri(CONTENT_FOLDER + File.separator + newFilename);
        versionsModel.setOpi(transactionId);


        FormatIdentificationModel formatIdentificationModel = new FormatIdentificationModel();
        formatIdentificationModel.setFormatId(format.getPuid());
        formatIdentificationModel.setMimeType(format.getMimetype());
        formatIdentificationModel.setFormatLitteral(format.getFormatLiteral());

        versionsModel.setFormatIdentification(formatIdentificationModel);

        qualifiersModel.setQualifier(DataObjectVersionType.BINARY_MASTER.getName());
        qualifiersModel.setVersions(Collections.singletonList(versionsModel));
        qualifiersModel.setNbc("1");

        ObjectGroupResponse dbObjectGroupModel = new ObjectGroupResponse();
        dbObjectGroupModel.setId(GUIDFactory.newObjectGroupGUID(ParameterHelper.getTenantParameter()).getId());
        dbObjectGroupModel.setOpi(transactionId);
        dbObjectGroupModel.setFileInfo(fileInfoModel);
        dbObjectGroupModel.setNbc(1);
        dbObjectGroupModel.setQualifiers(Collections.singletonList(qualifiersModel));

        return dbObjectGroupModel;
    }

    public static Map.Entry<String, String> findUnitParent(ObjectNode unit, @Nonnull List<MetadataUnitUp> unitUps,
        Map<String, String> unitIds) {
        for (MetadataUnitUp metadataUnitUp : unitUps) {
            if (metadataMatches(unit, metadataUnitUp.getMetadataKey(), metadataUnitUp.getMetadataValue())) {
                final String unitTitle = String.format("%s_%s", DYNAMIC_ATTACHEMENT, metadataUnitUp.getUnitUp());
                String unitUpId = unitIds.get(unitTitle);
                return new AbstractMap.SimpleEntry<>(unit.get(VitamFieldsHelper.id()).asText(), unitUpId);
            }
        }
        return new AbstractMap.SimpleEntry<>(unit.get(VitamFieldsHelper.id()).asText(),
            (unit.get(VitamFieldsHelper.unitups()) != null) ? unit.get(VitamFieldsHelper.unitups()).asText() : null);
    }

    private static boolean metadataMatches(JsonNode objectNode, String path, String value) {
        if (Strings.isNullOrEmpty(path)) {
            return false;
        }
        String[] paths = path.split("\\.");
        JsonNode obj = objectNode.get(paths[0]);
        if (obj == null || obj.isNull()) {
            return false;
        }
        if (paths.length == 1 && (obj.isNumber() || obj.isTextual())) {
            return obj.asText().equals(value);
        }
        if (obj.isObject()) {
            return metadataMatches(obj, Arrays.stream(paths).skip(1).collect(Collectors.joining(".")), value);
        }
        if (obj.isArray()) {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(obj.elements(), Spliterator.ORDERED), false)
                .map(e -> metadataMatches(e, Arrays.stream(paths).skip(1).collect(Collectors.joining(".")), value))
                .reduce(false, Boolean::logicalOr);
        }
        return false;
    }

}
