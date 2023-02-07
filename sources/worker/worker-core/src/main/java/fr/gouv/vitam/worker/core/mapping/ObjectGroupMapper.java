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
package fr.gouv.vitam.worker.core.mapping;

import fr.gouv.culture.archivesdefrance.seda.v2.BinaryDataObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.CoreMetadataType;
import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveTechnicalMetadataType;
import fr.gouv.culture.archivesdefrance.seda.v2.FileInfoType;
import fr.gouv.culture.archivesdefrance.seda.v2.FormatIdentificationType;
import fr.gouv.culture.archivesdefrance.seda.v2.MeasurementType;
import fr.gouv.culture.archivesdefrance.seda.v2.MeasurementWeightType;
import fr.gouv.culture.archivesdefrance.seda.v2.MinimalDataObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.PhysicalDataObjectType;
import fr.gouv.vitam.common.model.objectgroup.DbFileInfoModel;
import fr.gouv.vitam.common.model.objectgroup.DbFormatIdentificationModel;
import fr.gouv.vitam.common.model.objectgroup.DbVersionsModel;
import fr.gouv.vitam.common.model.objectgroup.MeasurementModel;
import fr.gouv.vitam.common.model.objectgroup.MetadataModel;
import fr.gouv.vitam.common.model.objectgroup.PhysicalDimensionsModel;
import fr.gouv.vitam.common.model.preservation.OtherMetadata;
import fr.gouv.vitam.processing.common.exception.ProcessingMalformedDataException;
import fr.gouv.vitam.processing.common.exception.ProcessingObjectReferenceException;

public class ObjectGroupMapper {


    public static DbVersionsModel map(MinimalDataObjectType dataObject, String groupId, String operationId)
        throws ProcessingMalformedDataException, ProcessingObjectReferenceException {

        DbVersionsModel dbVersionsModel = new DbVersionsModel();
        dbVersionsModel.setId(dataObject.getId());
        dbVersionsModel.setDataObjectVersion(dataObject.getDataObjectVersion());
        dbVersionsModel.setDataObjectGroupId(groupId);
        dbVersionsModel.setOpi(operationId);

        if(dataObject.getDataObjectProfile() != null)
            dbVersionsModel.setDataObjectProfile(dataObject.getDataObjectProfile().getValue());

        if (dataObject instanceof BinaryDataObjectType) {
            BinaryDataObjectType dataBinaryObject = (BinaryDataObjectType) dataObject;
            dbVersionsModel.setUri(dataBinaryObject.getUri());
            if (dataBinaryObject.getSize() != null) {
                dbVersionsModel.setSize(dataBinaryObject.getSize().longValue());
            }
            if (dataBinaryObject.getMessageDigest() != null) {
                dbVersionsModel.setMessageDigest(dataBinaryObject.getMessageDigest().getValue());
                dbVersionsModel.setAlgorithm(dataBinaryObject.getMessageDigest().getAlgorithm());
            }

            FormatIdentificationType formatIdentification = dataBinaryObject.getFormatIdentification();
            if (formatIdentification != null) {
                mapFormatIdentification(dbVersionsModel, formatIdentification);
            }

            FileInfoType fileInfo = dataBinaryObject.getFileInfo();
            if (fileInfo != null) {
                mapFileInfo(dbVersionsModel, fileInfo);
            }



            CoreMetadataType metadata = dataBinaryObject.getMetadata();
            if (metadata != null) {
                mapMetadata(dbVersionsModel, metadata);
            }

            DescriptiveTechnicalMetadataType otherMetadata = dataBinaryObject.getOtherMetadata();
            if (otherMetadata != null) {
                mapOtherMetadata(dbVersionsModel, otherMetadata);
            }

        } else if (dataObject instanceof PhysicalDataObjectType) {
            PhysicalDataObjectType dataPhysicalObject = (PhysicalDataObjectType) dataObject;
            if(dataPhysicalObject.getPhysicalDimensions() != null) {
                PhysicalDimensionsModel physicalDimensionsModel = new PhysicalDimensionsModel();
                if(dataPhysicalObject.getPhysicalDimensions().getDepth() != null) {
                    physicalDimensionsModel.setDepth(
                        mapMeasurement(dataPhysicalObject.getPhysicalDimensions().getDepth()));
                }
                if(dataPhysicalObject.getPhysicalDimensions().getDiameter() != null) {
                    physicalDimensionsModel.setDiameter(
                        mapMeasurement(dataPhysicalObject.getPhysicalDimensions().getDiameter()));
                }
                if(dataPhysicalObject.getPhysicalDimensions().getHeight() != null) {
                    physicalDimensionsModel.setHeight(
                        mapMeasurement(dataPhysicalObject.getPhysicalDimensions().getHeight()));
                }
                if(dataPhysicalObject.getPhysicalDimensions().getLength() != null) {
                    physicalDimensionsModel.setLength(
                        mapMeasurement(dataPhysicalObject.getPhysicalDimensions().getLength()));
                }
                if(dataPhysicalObject.getPhysicalDimensions().getWidth() != null) {
                    physicalDimensionsModel.setWidth(
                        mapMeasurement(dataPhysicalObject.getPhysicalDimensions().getWidth()));
                }
                if(dataPhysicalObject.getPhysicalDimensions().getThickness() != null) {
                    physicalDimensionsModel.setThickness(
                        mapMeasurement(dataPhysicalObject.getPhysicalDimensions().getThickness()));
                }
                if(dataPhysicalObject.getPhysicalDimensions().getWeight() != null) {
                    physicalDimensionsModel.setWeight(
                        mapWeightMeasurement(dataPhysicalObject.getPhysicalDimensions().getWeight()));
                }
                physicalDimensionsModel.setShape(dataPhysicalObject.getPhysicalDimensions().getShape());
                physicalDimensionsModel.setNumberOfPage(dataPhysicalObject.getPhysicalDimensions().getNumberOfPage());
                physicalDimensionsModel.setAny(ElementMapper.toMap(dataPhysicalObject.getAny()));
                dbVersionsModel.setPhysicalDimensionsModel(physicalDimensionsModel);
            }
            dbVersionsModel.setPhysicalId(dataPhysicalObject.getPhysicalId().getValue());
        }


        return dbVersionsModel;
    }

    private static void mapOtherMetadata(DbVersionsModel dbVersionsModel,
        DescriptiveTechnicalMetadataType otherMetadata) {
        OtherMetadata otherMetadataModel = new OtherMetadata(ElementMapper.toMap(otherMetadata.getAny()));
        dbVersionsModel.setOtherMetadata(otherMetadataModel);

    }

    private static void mapMetadata(DbVersionsModel dbVersionsModel, CoreMetadataType metadata) {
        MetadataModel metadataModel = new MetadataModel();
        if (metadata.getAudio() != null)
            metadataModel.setAudio(ElementMapper.toMap(metadata.getAudio().getAny()));
        if (metadata.getDocument() != null)
            metadataModel.setDocument(ElementMapper.toMap(metadata.getDocument().getAny()));
        if (metadata.getImage() != null)
            metadataModel.setImage(ElementMapper.toMap(metadata.getImage().getAny()));
        if (metadata.getText() != null)
            metadataModel.setText(ElementMapper.toMap(metadata.getText().getAny()));
        if (metadata.getVideo() != null)
            metadataModel.setVideo(ElementMapper.toMap(metadata.getVideo().getAny()));
        dbVersionsModel.setMetadata(metadataModel);
    }

    private static void mapFileInfo(DbVersionsModel dbVersionsModel, FileInfoType fileInfo) {
        DbFileInfoModel dbFileInfoModel = new DbFileInfoModel();
        dbFileInfoModel.setFilename(fileInfo.getFilename());
        if (fileInfo.getLastModified() != null) {
            dbFileInfoModel.setLastModified(fileInfo.getLastModified().toString());
        }
        dbFileInfoModel.setCreatingOs(fileInfo.getCreatingOs());
        dbFileInfoModel.setCreatingOsVersion(fileInfo.getCreatingOsVersion());
        if (fileInfo.getDateCreatedByApplication() != null) {
            dbFileInfoModel.setDateCreatedByApplication(fileInfo.getDateCreatedByApplication().toString());
        }
        dbFileInfoModel.setCreatingApplicationName(fileInfo.getCreatingApplicationName());
        dbFileInfoModel.setCreatingApplicationVersion(fileInfo.getCreatingApplicationVersion());

        dbVersionsModel.setFileInfoModel(dbFileInfoModel);
    }

    private static void mapFormatIdentification(DbVersionsModel dbVersionsModel,
        FormatIdentificationType formatIdentification) {
        DbFormatIdentificationModel dbFormatIdentificationModel = new DbFormatIdentificationModel();
        dbFormatIdentificationModel.setFormatId(formatIdentification.getFormatId());
        dbFormatIdentificationModel.setFormatLitteral(formatIdentification.getFormatLitteral());
        dbFormatIdentificationModel.setMimeType(formatIdentification.getMimeType());
        dbFormatIdentificationModel.setEncoding(formatIdentification.getEncoding());
        dbVersionsModel.setFormatIdentificationModel(dbFormatIdentificationModel);
    }


    private static MeasurementModel mapMeasurement(MeasurementType measurementType) {
        MeasurementModel measurementModel = new MeasurementModel();
        measurementModel.setUnit(measurementType.getUnit());
        measurementModel.setDValue(measurementType.getValue());
        return measurementModel;
    }


    private static MeasurementModel mapWeightMeasurement(MeasurementWeightType measurementWeightType) {
        MeasurementModel measurementModel = new MeasurementModel();
        measurementModel.setUnit(measurementWeightType.getUnit().value());
        measurementModel.setDValue(measurementWeightType.getValue());
        return measurementModel;
    }
}
