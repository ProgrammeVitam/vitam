/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.mapping.dip;

import fr.gouv.culture.archivesdefrance.seda.v2.BinaryDataObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectGroupType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectPackageType;
import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveTechnicalMetadataType;
import fr.gouv.culture.archivesdefrance.seda.v2.FileInfoType;
import fr.gouv.culture.archivesdefrance.seda.v2.FormatIdentificationType;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.culture.archivesdefrance.seda.v2.MessageDigestBinaryObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.MinimalDataObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.PhysicalDataObjectType;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.model.objectgroup.FileInfoModel;
import fr.gouv.vitam.common.model.objectgroup.FormatIdentificationModel;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import org.apache.commons.lang3.StringUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import java.math.BigInteger;
import java.util.Map;

import static java.util.Collections.singletonList;

/**
 * Mapper that map ObjectGroupResponse(POJO Dslquery response) to a DataObjectPackage (JAXB elements)
 */
public class ObjectGroupMapper {

    private static PhysicalDimensionsMapper physicalDimensionsMapper = new PhysicalDimensionsMapper();
    private CoreMetadataMapper coreMetadataMapper;

    public ObjectGroupMapper() {
        this.coreMetadataMapper = new CoreMetadataMapper();
    }

    /**
     * Map the object objectGroupResponse generated from queryDsl Response To a jaxb object DataObjectPackageType This
     * help convert DslQueryResponse to xml using jaxb
     *
     * @param objectGroupResponse the given queryDsl response for object Group
     * @return jaxb DataObjectPackageType
     * @throws InternalServerException
     */
    public DataObjectPackageType map(ObjectGroupResponse objectGroupResponse) throws InternalServerException {
        final DataObjectPackageType dataObjectPackageType = new DataObjectPackageType();

        if (!objectGroupResponse.getQualifiers().isEmpty()) {
            String objectGroupId = objectGroupResponse.getId();
            DataObjectGroupType dataObjectGroup = new DataObjectGroupType();
            dataObjectGroup.setId(objectGroupId);

            for (QualifiersModel qualifiersModel : objectGroupResponse.getQualifiers()) {

                final int lastIndexVersion = qualifiersModel.getVersions().size() - 1;
                final VersionsModel version = qualifiersModel.getVersions().get(lastIndexVersion);
                MinimalDataObjectType minimalDataObjectType;

                if (version != null && version.getPhysicalId() != null && !version.getPhysicalId().isEmpty()) {
                    minimalDataObjectType = mapPhysicalDataObject(version);
                } else {
                    minimalDataObjectType = mapBinaryDataObject(version);
                }

                dataObjectGroup.getBinaryDataObjectOrPhysicalDataObject()
                    .add(minimalDataObjectType);

            }

            dataObjectPackageType.getDataObjectGroupOrBinaryDataObjectOrPhysicalDataObject()
                .add(dataObjectGroup);
        }

        return dataObjectPackageType;
    }

    private BinaryDataObjectType mapBinaryDataObject(VersionsModel version)
        throws InternalServerException {
        final BinaryDataObjectType binaryDataObjectType = new BinaryDataObjectType();
        // FIXME : BinaryDataObjectType.Compressed not supported yet in SIP ingest
        // final BinaryDataObjectType.Compressed compressed = new BinaryDataObjectType.Compressed();
        // compressed.setAlgorithm(version.getAlgorithm());
        // binaryDataObjectType.setCompressed(compressed);
        if (version != null) {
            final FormatIdentificationType formatIdentificationType = new FormatIdentificationType();
            final FormatIdentificationModel formatIdentification = version.getFormatIdentification();
            if (formatIdentification != null) {
                formatIdentificationType.setFormatLitteral(formatIdentification.getFormatLitteral());
                formatIdentificationType.setMimeType(formatIdentification.getMimeType());
                formatIdentificationType.setFormatId(formatIdentification.getFormatId());
                formatIdentificationType.setEncoding(formatIdentification.getEncoding());
                binaryDataObjectType.setFormatIdentification(formatIdentificationType);
            }
        }
        final FileInfoType fileInfoType = new FileInfoType();
        final FileInfoModel fileInfoModel = version != null ? version.getFileInfoModel() : null;
        if (fileInfoModel != null) {
            fileInfoType.setFilename(fileInfoModel.getFilename());
            fileInfoType
                .setCreatingApplicationName(fileInfoModel.getCreatingApplicationName());
            fileInfoType
                .setCreatingApplicationVersion(fileInfoModel.getCreatingApplicationVersion());
            fileInfoType.setCreatingOs(fileInfoModel.getCreatingOs());
            fileInfoType.setCreatingOsVersion(fileInfoModel.getCreatingOsVersion());
            final String dateCreatedByApplication =
                fileInfoModel.getDateCreatedByApplication();
            final String lastModified = fileInfoModel.getLastModified();
            try {
                if (dateCreatedByApplication != null && !dateCreatedByApplication.isEmpty()) {
                    fileInfoType.setDateCreatedByApplication(DatatypeFactory.newInstance()
                        .newXMLGregorianCalendar(dateCreatedByApplication));
                }
                if (lastModified != null && !lastModified.isEmpty()) {
                    fileInfoType.setLastModified(DatatypeFactory.newInstance()
                        .newXMLGregorianCalendar(lastModified));
                }
            } catch (DatatypeConfigurationException e) {
                throw new InternalServerException(String
                    .format(
                        "Exception occurred During parsing of field DateCreatedByApplication or lastModified: %s",
                        dateCreatedByApplication));
            }
        }
        binaryDataObjectType.setFileInfo(fileInfoType);
        if (version != null) {
            binaryDataObjectType.setUri(version.getUri());
            binaryDataObjectType.setSize(BigInteger.valueOf(version.getSize()));
            final MessageDigestBinaryObjectType messageDigestBinaryObjectType =
                new MessageDigestBinaryObjectType();
            messageDigestBinaryObjectType.setAlgorithm(StringUtils.trimToEmpty(version.getAlgorithm()));
            messageDigestBinaryObjectType.setValue(StringUtils.trimToEmpty(version.getMessageDigest()));
            binaryDataObjectType.setMessageDigest(messageDigestBinaryObjectType);
            mapCommonInformations(version, binaryDataObjectType);
            binaryDataObjectType.setMetadata(coreMetadataMapper.map(version.getMetadata()));

            // other metadata 
            final DescriptiveTechnicalMetadataType otherMetadata = new DescriptiveTechnicalMetadataType();
            Map<String, Object> otherMetadataMap = version.getOtherMetadata();
            if (otherMetadataMap != null && !otherMetadataMap.isEmpty()) {
                otherMetadata.getAny()
                    .addAll(TransformJsonTreeToListOfXmlElement.mapJsonToElement(singletonList(otherMetadataMap)));
                binaryDataObjectType.setOtherMetadata(otherMetadata);
            }
        }
        return binaryDataObjectType;
    }

    private PhysicalDataObjectType mapPhysicalDataObject(VersionsModel version) {
        final PhysicalDataObjectType physicalDataObjectType = new PhysicalDataObjectType();
        physicalDataObjectType
            .setPhysicalDimensions(physicalDimensionsMapper.map(version.getPhysicalDimensionsModel()));
        mapCommonInformations(version, physicalDataObjectType);
        final IdentifierType identifierType = new IdentifierType();
        identifierType.setValue(version.getPhysicalId());
        physicalDataObjectType.setPhysicalId(identifierType);
        physicalDataObjectType.getAny()
            .addAll(TransformJsonTreeToListOfXmlElement.mapJsonToElement(singletonList(version.getAny())));

        return physicalDataObjectType;
    }

    /**
     * Map Common informations contains in MinimalDataObjectType
     *
     * @param version               the version of the model to map
     * @param minimalDataObjectType the given minimalDataObjectType to complete can be (physicalDataObjectType or
     *                              binaryDataObjectType
     * @param <T>                   object that extend MinimalDataObjectType
     */
    private <T extends MinimalDataObjectType> void mapCommonInformations(final VersionsModel version,
        T minimalDataObjectType) {
        // TODO : Not done yet we need informations about field List<RelationshipType> and dataObjectGroupReferenceId
        // from the SEDA 2.0 .xsd,it's is not map for the moment because don't know
        // where the fields is mapped in mongo
        minimalDataObjectType.setDataObjectVersion(version.getDataObjectVersion());
        minimalDataObjectType.setId(version.getId());
    }
}
