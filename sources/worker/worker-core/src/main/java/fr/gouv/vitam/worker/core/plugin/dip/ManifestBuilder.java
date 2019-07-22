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

import static fr.gouv.vitam.common.SedaConstants.ATTRIBUTE_ID;
import static fr.gouv.vitam.common.SedaConstants.NAMESPACE_URI;
import static fr.gouv.vitam.common.SedaConstants.TAG_ARCHIVE_DELIVERY_REQUEST_REPLY;
import static fr.gouv.vitam.common.SedaConstants.TAG_DATA_OBJECT_GROUP;
import static fr.gouv.vitam.common.SedaConstants.TAG_DATA_OBJECT_PACKAGE;
import static fr.gouv.vitam.common.SedaConstants.TAG_DESCRIPTIVE_METADATA;
import static fr.gouv.vitam.common.SedaConstants.TAG_MANAGEMENT_METADATA;
import static fr.gouv.vitam.common.SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER;
import static fr.gouv.vitam.common.mapping.dip.UnitMapper.buildObjectMapper;
import static fr.gouv.vitam.worker.common.utils.SedaUtils.XSI_URI;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ListMultimap;

import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.culture.archivesdefrance.seda.v2.BinaryDataObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectGroupType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectOrArchiveUnitReferenceType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectPackageType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectRefType;
import fr.gouv.culture.archivesdefrance.seda.v2.MinimalDataObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.ObjectFactory;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mapping.dip.ArchiveUnitMapper;
import fr.gouv.vitam.common.mapping.dip.ObjectGroupMapper;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.utils.SedaUtils;

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
        writer.writeStartElement(TAG_ARCHIVE_DELIVERY_REQUEST_REPLY);
        writer.writeNamespace("xlink", "http://www.w3.org/1999/xlink");
        writer.writeNamespace("pr", "info:lc/xmlns/premis-v2");
        writer.writeDefaultNamespace(NAMESPACE_URI);
        writer.writeNamespace("xsi", XSI_URI);
        writer.writeAttribute("xsi", XSI_URI, "schemaLocation", NAMESPACE_URI + " " + SedaUtils.SEDA_XSD_VERSION);
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
    public Map<String, JsonNode> writeGOT(JsonNode og, String linkedAU, Optional<Set<String>> dataObjectVersionFilter)
        throws JsonProcessingException, JAXBException, ProcessingException, XMLStreamException {

        ObjectGroupResponse objectGroup = objectMapper.treeToValue(og, ObjectGroupResponse.class);
        // Usage access control
        AccessContractModel accessContractModel = VitamThreadUtils.getVitamSession().getContract();
        if(accessContractModel == null) {
            final AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
            Select select = new Select();

            try {
                Query query = QueryHelper.eq(AccessContract.IDENTIFIER, VitamThreadUtils.getVitamSession().getContractId());
                select.setQuery(query);
                accessContractModel = ((RequestResponseOK<AccessContractModel>)client.findAccessContracts(select.getFinalSelect())).getResults().get(0);
            } catch (InvalidCreateOperationException | AdminManagementClientServerException | InvalidParseOperationException e) {
                throw new ProcessingException(e.getMessage(), e.getCause());
            }
        }

        final AccessContractModel accessContract = accessContractModel;

        List<QualifiersModel> qualifiersToRemove;
        if(!accessContract.isEveryDataObjectVersion()) {
            qualifiersToRemove = objectGroup.getQualifiers().stream()
                    .filter(qualifier -> !accessContract.getDataObjectVersion().contains(qualifier.getQualifier()))
                    .collect(Collectors.toList());
            objectGroup.getQualifiers().removeAll(qualifiersToRemove);
        }

        if(dataObjectVersionFilter.isPresent()) {
            qualifiersToRemove = objectGroup.getQualifiers().stream()
                    .filter(qualifier -> !dataObjectVersionFilter.get().contains(qualifier.getQualifier()))
                    .collect(Collectors.toList());
            objectGroup.getQualifiers().removeAll(qualifiersToRemove);
        }

        if(objectGroup.getQualifiers().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> strategiesByVersion = objectGroup.getQualifiers().stream()
                .flatMap(qualifier -> qualifier.getVersions().stream())
                .filter(version -> version.getStorage() != null && version.getStorage().getStrategyId() != null)
                .collect(Collectors.toMap(VersionsModel::getDataObjectVersion, version -> version.getStorage().getStrategyId()));
        
        Map<String, JsonNode> maps = new HashMap<>();

        final DataObjectPackageType xmlObject;
        try {
            xmlObject = objectGroupMapper.map(objectGroup);

            List<Object> dataObjectGroupList = xmlObject.getDataObjectGroupOrBinaryDataObjectOrPhysicalDataObject();
            // must be only 1 GOT (vitam seda restriction)
            for (Object dataObjectGroupItem : dataObjectGroupList) {

                DataObjectGroupType dataObjectGroup = (DataObjectGroupType) dataObjectGroupItem;

                startDataObjectGroup(dataObjectGroup.getId());

                List<MinimalDataObjectType> binaryDataObjectOrPhysicalDataObject =
                    dataObjectGroup.getBinaryDataObjectOrPhysicalDataObject();
                for (MinimalDataObjectType minimalDataObjectType : binaryDataObjectOrPhysicalDataObject) {
                    if (minimalDataObjectType instanceof BinaryDataObjectType) {
                        BinaryDataObjectType binaryDataObjectType = (BinaryDataObjectType) minimalDataObjectType;
                        String extension =
                            FilenameUtils.getExtension(binaryDataObjectType.getUri());
                        String fileName = StoreDIP.CONTENT + "/" + binaryDataObjectType.getId() + "." + extension;
                        binaryDataObjectType.setUri(fileName);

                        String[] dataObjectVersion = minimalDataObjectType.getDataObjectVersion().split("_");
                        String xmlQualifier = dataObjectVersion[0];
                        Integer xmlVersion = Integer.parseInt(dataObjectVersion[1]);

                        ObjectNode objectInfos = (ObjectNode) AccessLogUtils.getWorkerInfo(xmlQualifier, xmlVersion, binaryDataObjectType.getSize().longValue(), linkedAU, fileName);
                        objectInfos.put("strategyId", strategiesByVersion.get(minimalDataObjectType.getDataObjectVersion()));
                        maps.put(minimalDataObjectType.getId(), objectInfos);
                    }
                    marshaller.marshal(minimalDataObjectType, writer);
                }

                endDataObjectGroup();
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

    public void startDataObjectGroup(String groupId) throws XMLStreamException {
        writer.writeStartElement(TAG_DATA_OBJECT_GROUP);
        writer.writeAttribute(NAMESPACE_URI, ATTRIBUTE_ID, groupId);
    }

    public void endDescriptiveMetadata() throws XMLStreamException {
        writer.writeEndElement();
    }

    public void endDataObjectPackage() throws XMLStreamException {
        writer.writeEndElement();
    }

    public void endDataObjectGroup() throws XMLStreamException {
        writer.writeEndElement();
    }

    public void writeOriginatingAgency(String originatingAgency) throws JAXBException, XMLStreamException {
        writer.writeStartElement(NAMESPACE_URI, TAG_MANAGEMENT_METADATA);

        marshaller.marshal(new JAXBElement<>(new QName(NAMESPACE_URI, TAG_ORIGINATINGAGENCYIDENTIFIER),
            String.class, originatingAgency), writer);

        writer.writeEndElement();
    }

    public void closeManifest() throws XMLStreamException {
        writer.writeEndElement();
        writer.writeEndDocument();
    }
}
