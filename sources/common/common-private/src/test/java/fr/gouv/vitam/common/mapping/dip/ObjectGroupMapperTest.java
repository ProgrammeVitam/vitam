/*
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
 */
package fr.gouv.vitam.common.mapping.dip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.culture.archivesdefrance.seda.v2.*;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.objectgroup.FileInfoModel;
import fr.gouv.vitam.common.model.objectgroup.FormatIdentificationModel;
import fr.gouv.vitam.common.model.objectgroup.MeasurementModel;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.PhysicalDimensionsModel;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.StorageRacineModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import org.apache.xerces.dom.ElementNSImpl;
import org.junit.Test;



public class ObjectGroupMapperTest {

    private static final String SIMPLE_OBJECT_GROUP_DBREQUEST_RESULT_WITH_METADATA =
        "objectGroup.json";
    private static JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance("fr.gouv.culture.archivesdefrance.seda.v2");
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void should_map_object_group_with_physical_and_binary() throws JsonProcessingException,
        FileNotFoundException, InvalidParseOperationException, InternalServerException, DatatypeConfigurationException,
        JAXBException {
        final JsonNode GOTMetadataResponse = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile(SIMPLE_OBJECT_GROUP_DBREQUEST_RESULT_WITH_METADATA));
        ObjectMapper objectMapper = UnitMapper.buildObjectMapper();
        ObjectGroupResponse objectGroupSource = objectMapper.treeToValue(GOTMetadataResponse, ObjectGroupResponse.class);
        ObjectGroupMapper objectGroupMapper = new ObjectGroupMapper();
        final DataObjectPackageType dataObjectPackageType = objectGroupMapper.map(objectGroupSource);
        Marshaller marshaller = jaxbContext.createMarshaller();

        StringWriter writer = new StringWriter();
        marshaller.marshal(dataObjectPackageType, writer);

        final List<Object> dataObjectGroupList  =
            dataObjectPackageType.getDataObjectGroupOrBinaryDataObjectOrPhysicalDataObject();

        //case of ObjectGroup (Seda 2.1)
        assertNotNull(dataObjectGroupList);
        assertEquals(dataObjectGroupList.size(), 1);
        assertTrue(dataObjectGroupList.get(0) instanceof DataObjectGroupType);

            final DataObjectGroupType dataObjectGroup = (DataObjectGroupType) dataObjectGroupList.get(0);

            assertNotNull(dataObjectGroup);
            assertEquals(dataObjectGroup.getId(), objectGroupSource.getId());

            List<MinimalDataObjectType> binaryDataObjectOrPhysicalDataObject =
                dataObjectGroup.getBinaryDataObjectOrPhysicalDataObject();

            assertNotNull(binaryDataObjectOrPhysicalDataObject);
            assertTrue(!binaryDataObjectOrPhysicalDataObject.isEmpty());

            if (binaryDataObjectOrPhysicalDataObject.get(1) instanceof BinaryDataObjectType) {
                BinaryDataObjectType binaryDataObjectType =
                    (BinaryDataObjectType) binaryDataObjectOrPhysicalDataObject.get(1);
                final QualifiersModel qualifiersModel = objectGroupSource.getQualifiers().get(1);
                final VersionsModel versionsModel = qualifiersModel.getVersions().get(0);
                final FileInfoModel fileInfoModel =
                    versionsModel.getFileInfoModel();
                final FileInfoType fileInfo = binaryDataObjectType.getFileInfo();
                assertEquals(fileInfoModel.getFilename(),
                    fileInfo.getFilename());
                assertEquals(fileInfoModel.getCreatingApplicationName(), fileInfo.getCreatingApplicationName());
                assertEquals(fileInfoModel.getCreatingOs(), fileInfo.getCreatingOs());
                assertEquals(fileInfoModel.getCreatingOsVersion(), fileInfo.getCreatingOsVersion());
                assertEquals(fileInfoModel.getCreatingApplicationVersion(), fileInfo.getCreatingApplicationVersion());
                if (fileInfoModel.getLastModified() != null) {
                    final XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance()
                        .newXMLGregorianCalendar(fileInfoModel.getLastModified());
                    assertEquals(xmlGregorianCalendar, fileInfo.getDateCreatedByApplication());
                    assertEquals(xmlGregorianCalendar, fileInfo.getLastModified());
                }

                final FormatIdentificationType formatIdentificationType =
                    binaryDataObjectType.getFormatIdentification();
                final FormatIdentificationModel formatIdentificationModel = versionsModel.getFormatIdentification();
                assertEquals(formatIdentificationModel.getFormatId(), formatIdentificationType.getFormatId());
                assertEquals(formatIdentificationModel.getFormatLitteral(),
                    formatIdentificationType.getFormatLitteral());
                assertEquals(formatIdentificationModel.getMimeType(), formatIdentificationType.getMimeType());
                assertEquals(formatIdentificationModel.getEncoding(), formatIdentificationType.getEncoding());
                assertEquals(new BigInteger(String.valueOf(versionsModel.getSize())), binaryDataObjectType.getSize());
                assertEquals(versionsModel.getUri(), binaryDataObjectType.getUri());
                assertEquals(versionsModel.getMessageDigest(),
                    binaryDataObjectType.getMessageDigest().getValue());
                assertEquals(versionsModel.getAlgorithm(),
                    binaryDataObjectType.getMessageDigest().getAlgorithm());
                final MinimalDataObjectType minimalDataObjectType = binaryDataObjectOrPhysicalDataObject.get(1);
                assertEquals(versionsModel.getId(), minimalDataObjectType.getId());
                assertEquals(versionsModel.getDataObjectVersion(), minimalDataObjectType.getDataObjectVersion());

                Map<String, Object> otherMetadataFromVersionModel = versionsModel.getOtherMetadata();
                DescriptiveTechnicalMetadataType otherMetadadata =
                    ((BinaryDataObjectType) minimalDataObjectType).getOtherMetadata();
                assertNotNull(otherMetadadata);
                assertEquals(((List) otherMetadadata.getAny()).size(), otherMetadataFromVersionModel.size());
                assertEquals(((List) otherMetadadata.getAny()).size(), 1);
                assertTrue(otherMetadadata.getAny() instanceof List);

                ElementNSImpl eleNsImplObject = ((ElementNSImpl) ((List<Object>)otherMetadadata.getAny()).get(0));
                String optionalMDkey = otherMetadataFromVersionModel.keySet().iterator().next();
                Object optionalMDValue = otherMetadataFromVersionModel.get(optionalMDkey);
                assertEquals(eleNsImplObject.getNodeName(), optionalMDkey);
                assertEquals(eleNsImplObject.getTextContent(), optionalMDValue);
            }
            if (binaryDataObjectOrPhysicalDataObject.get(0) instanceof PhysicalDataObjectType) {
                final QualifiersModel qualifiersModel = objectGroupSource.getQualifiers().get(0);
                final VersionsModel versionsModel = qualifiersModel.getVersions().get(0);
                final PhysicalDimensionsModel physicalDimensionsModel = versionsModel.getPhysicalDimensionsModel();
                final PhysicalDataObjectType physicalDataObjectType =
                    (PhysicalDataObjectType) binaryDataObjectOrPhysicalDataObject.get(0);
                final DimensionsType dimensionsType = physicalDataObjectType.getPhysicalDimensions();

                check_equality_between_measurementModel_and_dimensionType(physicalDimensionsModel.getDepth(),
                    dimensionsType.getDepth());
                check_equality_between_measurementModel_and_dimensionType(physicalDimensionsModel.getDiameter(),
                    dimensionsType.getDiameter());
                check_equality_between_measurementModel_and_dimensionType(physicalDimensionsModel.getHeight(),
                    dimensionsType.getHeight());
                check_equality_between_measurementModel_and_dimensionType(physicalDimensionsModel.getLength(),
                    dimensionsType.getLength());
                assertEquals(physicalDimensionsModel.getNumberOfPage(), dimensionsType.getNumberOfPage());
                assertEquals(physicalDimensionsModel.getShape(), dimensionsType.getShape());
                check_equality_between_measurementModel_and_dimensionType(physicalDimensionsModel.getThickness(),
                    dimensionsType.getThickness());
                check_Equality_Beatween_MeasurementWeightType_And_DimensionType(physicalDimensionsModel.getWeight(),
                    dimensionsType.getWeight());
                check_equality_between_measurementModel_and_dimensionType(physicalDimensionsModel.getWidth(),
                    dimensionsType.getWidth());
            }
    }

    /**
     * check_Equality_Beatween_MeasurementModel_And_DimensionType
     *
     * @param measurementModel measurementModel
     * @param measurementType  measurementType
     */
    private void check_equality_between_measurementModel_and_dimensionType(MeasurementModel measurementModel,
        MeasurementType measurementType) {
        if (measurementModel != null) {
            assertEquals(measurementModel.getUnit(), measurementType.getUnit());
            assertEquals(measurementModel.getDValue(), measurementType.getValue());
        }

    }

    private void check_Equality_Beatween_MeasurementWeightType_And_DimensionType(MeasurementModel measurementModel,
        MeasurementWeightType measurementWeightType) {
        if (measurementModel != null) {
            assertEquals(measurementModel.getUnit(), measurementWeightType.getUnit().value());
            assertEquals(measurementModel.getDValue(), measurementWeightType.getValue());
        }
    }

}
