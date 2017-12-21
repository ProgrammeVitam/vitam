/**
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
package fr.gouv.vitam.worker.core.plugin.dip;

import static fr.gouv.vitam.common.SedaConstants.NAMESPACE_URI;
import static fr.gouv.vitam.common.SedaConstants.TAG_DATA_OBJECT_PACKAGE;
import static fr.gouv.vitam.common.SedaConstants.TAG_DESCRIPTIVE_METADATA;
import static fr.gouv.vitam.common.SedaConstants.TAG_MANAGEMENT_METADATA;
import static fr.gouv.vitam.common.SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER;
import static fr.gouv.vitam.common.mapping.dip.UnitMapper.buildObjectMapper;
import static fr.gouv.vitam.worker.common.utils.SedaUtils.XSI_URI;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ListMultimap;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.culture.archivesdefrance.seda.v2.BinaryDataObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectOrArchiveUnitReferenceType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectPackageType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectRefType;
import fr.gouv.culture.archivesdefrance.seda.v2.MinimalDataObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.ObjectFactory;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mapping.dip.ArchiveUnitMapper;
import fr.gouv.vitam.common.mapping.dip.ObjectGroupMapper;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.processing.common.exception.ProcessingException;

/**
 * build a SEDA manifest with JAXB.
 */
public class ManifestBuilder implements AutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ManifestBuilder.class);

    private static JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance("fr.gouv.culture.archivesdefrance.seda.v2");
        } catch (JAXBException e) {
            LOGGER.error("unable to create jaxb context", e);
        }
    }

    private final XMLStreamWriter writer;
    private final Marshaller marshaller;
    private final ArchiveUnitMapper archiveUnitMapper;
    private final ObjectMapper objectMapper;
    private final ObjectFactory objectFactory;
    private ObjectGroupMapper objectGroupMapper;

    /**
     * @param outputStream
     * @throws XMLStreamException
     * @throws JAXBException
     */
    public ManifestBuilder(OutputStream outputStream) throws XMLStreamException, JAXBException {
        archiveUnitMapper = new ArchiveUnitMapper();
        this.objectFactory = new ObjectFactory();
        this.objectMapper = buildObjectMapper();
        this.objectGroupMapper = new ObjectGroupMapper();

        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
        writer = xmlOutputFactory.createXMLStreamWriter(outputStream);

        marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

        writer.writeStartDocument();
        writer.writeStartElement("ArchiveRestitutionRequest");
        writer.writeNamespace("xlink", "http://www.w3.org/1999/xlink");
        writer.writeNamespace("pr", "info:lc/xmlns/premis-v2");
        writer.writeDefaultNamespace(NAMESPACE_URI);
        writer.writeNamespace("xsi", XSI_URI);
        writer.writeAttribute("xsi", XSI_URI, "schemaLocation", NAMESPACE_URI + " seda-2.0-main.xsd");
    }

    /**
     * write a GOT
     *
     * @param og
     * @return
     * @throws JsonProcessingException
     * @throws JAXBException
     * @throws ProcessingException
     */
    public Map<String, String> writeGOT(JsonNode og)
        throws JsonProcessingException, JAXBException, ProcessingException {
        Map<String, String> maps = new HashMap<>();

        ObjectGroupResponse objectGroup = objectMapper.treeToValue(og, ObjectGroupResponse.class);
        final DataObjectPackageType xmlObject;
        try {
            xmlObject = objectGroupMapper.map(objectGroup);
            List<MinimalDataObjectType> binaryDataObjectOrPhysicalDataObject =
                xmlObject.getBinaryDataObjectOrPhysicalDataObject();
            for (MinimalDataObjectType minimalDataObjectType : binaryDataObjectOrPhysicalDataObject) {
                if (minimalDataObjectType instanceof BinaryDataObjectType) {
                    BinaryDataObjectType binaryDataObjectType = (BinaryDataObjectType) minimalDataObjectType;

                    String binaryId = binaryDataObjectType.getId();

                    String fileName = StoreDIP.CONTENT + "/" + binaryId;
                    binaryDataObjectType.setUri(fileName);
                    maps.put(minimalDataObjectType.getId(), fileName);
                }
                marshaller.marshal(minimalDataObjectType, writer);
            }
        } catch (InternalServerException e) {
            throw new ProcessingException(e);
        }
        return maps;
    }

    /**
     * write an archiveUnit.
     *
     * @param result
     * @param multimap
     * @param ogs
     * @throws JsonProcessingException
     * @throws DatatypeConfigurationException
     * @throws JAXBException
     */
    public void writeArchiveUnit(JsonNode result, ListMultimap<String, String> multimap, Map<String, String> ogs)
        throws JsonProcessingException, DatatypeConfigurationException, JAXBException {

        ArchiveUnitModel archiveUnitModel = objectMapper.treeToValue(result, ArchiveUnitModel.class);
        final ArchiveUnitType xmlUnit = archiveUnitMapper.map(archiveUnitModel);
        List<ArchiveUnitType> unitChildren = new ArrayList<>();
        if (multimap.containsKey(xmlUnit.getId())) {
            List<String> children = multimap.get(xmlUnit.getId());
            unitChildren = children.stream().map(item -> {
                ArchiveUnitType archiveUnitType = new ArchiveUnitType();
                archiveUnitType.setId(GUIDFactory.newGUID().toString());
                archiveUnitType.setArchiveUnitRefId(item);
                return archiveUnitType;
            }).collect(Collectors.toList());
        }
        xmlUnit.getArchiveUnitOrDataObjectReferenceOrDataObjectGroup().addAll(unitChildren);

        if (ogs.containsKey(xmlUnit.getId())) {

            DataObjectOrArchiveUnitReferenceType dataObjectReference = new DataObjectOrArchiveUnitReferenceType();
            DataObjectRefType value = new DataObjectRefType();
            value.setDataObjectGroupReferenceId(ogs.get(xmlUnit.getId()));

            dataObjectReference.setDataObjectReference(value);

            JAXBElement<DataObjectRefType> archiveUnitTypeDataObjectReference =
                objectFactory.createArchiveUnitTypeDataObjectReference(value);
            xmlUnit.getArchiveUnitOrDataObjectReferenceOrDataObjectGroup().add(archiveUnitTypeDataObjectReference);
        }

        marshaller.marshal(xmlUnit, writer);

    }

    @Override
    public void close() throws XMLStreamException {
        writer.close();
    }

    public void startDescriptiveMetadata() throws XMLStreamException {
        writer.writeStartElement(TAG_DESCRIPTIVE_METADATA);
    }

    public void startDataObjectPackage() throws XMLStreamException {
        writer.writeStartElement(TAG_DATA_OBJECT_PACKAGE);
    }

    public void endDescriptiveMetadata() throws XMLStreamException {
        writer.writeEndElement();
    }

    public void endDataObjectPackage() throws XMLStreamException {
        writer.writeEndElement();
    }

    public void writeOriginatingAgencyAndClose(String originatingAgency) throws JAXBException, XMLStreamException {
        writer.writeStartElement(NAMESPACE_URI, TAG_MANAGEMENT_METADATA);

        marshaller.marshal(new JAXBElement<>(new QName(NAMESPACE_URI, TAG_ORIGINATINGAGENCYIDENTIFIER),
            String.class, originatingAgency), writer);

        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
    }

}
