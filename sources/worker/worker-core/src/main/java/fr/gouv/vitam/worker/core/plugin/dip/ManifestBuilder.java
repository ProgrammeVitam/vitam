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
package fr.gouv.vitam.worker.core.plugin.dip;

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
import fr.gouv.culture.archivesdefrance.seda.v2.EventLogBookOgType;
import fr.gouv.culture.archivesdefrance.seda.v2.LogBookOgType;
import fr.gouv.culture.archivesdefrance.seda.v2.LogBookType;
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
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.core.mapping.LogbookMapper;
import org.apache.commons.io.FilenameUtils;
import org.bson.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.common.SedaConstants.NAMESPACE_URI;
import static fr.gouv.vitam.common.SedaConstants.TAG_ARCHIVE_DELIVERY_REQUEST_REPLY;
import static fr.gouv.vitam.common.SedaConstants.TAG_DATA_OBJECT_GROUP;
import static fr.gouv.vitam.common.SedaConstants.TAG_DATA_OBJECT_PACKAGE;
import static fr.gouv.vitam.common.SedaConstants.TAG_DESCRIPTIVE_METADATA;
import static fr.gouv.vitam.common.SedaConstants.TAG_MANAGEMENT_METADATA;
import static fr.gouv.vitam.common.SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER;
import static fr.gouv.vitam.common.mapping.dip.UnitMapper.buildObjectMapper;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.worker.common.utils.SedaUtils.XSI_URI;

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
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory = LogbookLifeCyclesClientFactory.getInstance();


    /**
     * @param outputStream
     * @throws XMLStreamException
     * @throws JAXBException
     */
    ManifestBuilder(OutputStream outputStream) throws XMLStreamException, JAXBException {
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

    Map<String, JsonNode> writeGOT(JsonNode og, String linkedAU, Set<String> dataObjectVersionFilter, boolean exportWithLogBookLFC) throws JsonProcessingException, JAXBException, ProcessingException, XMLStreamException {
        ObjectGroupResponse objectGroup = objectMapper.treeToValue(og, ObjectGroupResponse.class);
        // Usage access control
        AccessContractModel accessContractModel = VitamThreadUtils.getVitamSession().getContract();
        if (accessContractModel == null) {
            final AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
            Select select = new Select();

            try {
                Query query = QueryHelper.eq(AccessContract.IDENTIFIER, VitamThreadUtils.getVitamSession().getContractId());
                select.setQuery(query);
                accessContractModel = ((RequestResponseOK<AccessContractModel>) client.findAccessContracts(select.getFinalSelect())).getResults().get(0);
            } catch (InvalidCreateOperationException | AdminManagementClientServerException | InvalidParseOperationException e) {
                throw new ProcessingException(e.getMessage(), e.getCause());
            }
        }

        final AccessContractModel accessContract = accessContractModel;

        List<QualifiersModel> qualifiersToRemove;
        if (!accessContract.isEveryDataObjectVersion()) {
            qualifiersToRemove = objectGroup.getQualifiers().stream()
                .filter(qualifier -> !accessContract.getDataObjectVersion().contains(qualifier.getQualifier()))
                .collect(Collectors.toList());
            objectGroup.getQualifiers().removeAll(qualifiersToRemove);
        }

        if (!dataObjectVersionFilter.isEmpty()) {
            qualifiersToRemove = objectGroup.getQualifiers().stream()
                .filter(qualifier -> !dataObjectVersionFilter.contains(qualifier.getQualifier()))
                .collect(Collectors.toList());
            objectGroup.getQualifiers().removeAll(qualifiersToRemove);
        }

        if (objectGroup.getQualifiers().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> strategiesByVersion = objectGroup.getQualifiers().stream()
            .flatMap(qualifier -> qualifier.getVersions().stream())
            .filter(version -> version.getStorage() != null && version.getStorage().getStrategyId() != null)
            .collect(Collectors.toMap(VersionsModel::getDataObjectVersion, version -> version.getStorage().getStrategyId()));

        Map<String, JsonNode> maps = new HashMap<>();

        try {
            final DataObjectPackageType  xmlObject = objectGroupMapper.map(objectGroup);
            List<Object> dataObjectGroupList = xmlObject.getDataObjectGroupOrBinaryDataObjectOrPhysicalDataObject();

            if (dataObjectGroupList.isEmpty()) {
                return maps;
            }
            // must be only 1 GOT (vitam seda restriction)
            DataObjectGroupType dataObjectGroup = (DataObjectGroupType) dataObjectGroupList.get(0);

            if (exportWithLogBookLFC) {
                LogBookOgType logBookOgType = getLogBookOgType(dataObjectGroup.getId());
                dataObjectGroup.setLogBook(logBookOgType);
            }

            List<MinimalDataObjectType> binaryDataObjectOrPhysicalDataObject = dataObjectGroup.getBinaryDataObjectOrPhysicalDataObject();
            for (MinimalDataObjectType minimalDataObjectType : binaryDataObjectOrPhysicalDataObject) {
                if (minimalDataObjectType instanceof BinaryDataObjectType) {
                    BinaryDataObjectType binaryDataObjectType = (BinaryDataObjectType) minimalDataObjectType;
                    String extension = FilenameUtils.getExtension(binaryDataObjectType.getUri());
                    String fileName = StoreDIP.CONTENT + "/" + binaryDataObjectType.getId() + "." + extension;
                    binaryDataObjectType.setUri(fileName);

                    String[] dataObjectVersion = minimalDataObjectType.getDataObjectVersion().split("_");
                    String xmlQualifier = dataObjectVersion[0];
                    Integer xmlVersion = Integer.parseInt(dataObjectVersion[1]);

                    ObjectNode objectInfos =
                        (ObjectNode) AccessLogUtils.getWorkerInfo(xmlQualifier, xmlVersion, binaryDataObjectType.getSize().longValue(), linkedAU, fileName);
                    objectInfos.put("strategyId", strategiesByVersion.get(minimalDataObjectType.getDataObjectVersion()));
                    maps.put(minimalDataObjectType.getId(), objectInfos);
                }
            }
            marshallHackForNonXmlRootObject(dataObjectGroup);
            return maps;
        } catch (InternalServerException | LogbookClientException | InvalidParseOperationException e) {
            throw new ProcessingException(e);
        }
    }

    private void marshallHackForNonXmlRootObject(DataObjectGroupType dataObjectGroup) throws JAXBException {
        // Hack from https://docs.oracle.com/javase/7/docs/api/javax/xml/bind/Marshaller.html
        // Marshalling content tree rooted by a JAXB element
        // Using the dataObjectGroup.getClass() in order to have no namespace or type issue
        marshaller.marshal(new JAXBElement(new QName(NAMESPACE_URI, TAG_DATA_OBJECT_GROUP), dataObjectGroup.getClass(), dataObjectGroup), writer);
    }

    private LogBookOgType getLogBookOgType(String id) throws LogbookClientException, InvalidParseOperationException, JAXBException {
        try (LogbookLifeCyclesClient client = logbookLifeCyclesClientFactory.getClient()) {
            JsonNode response = client.selectObjectGroupLifeCycleById(id, new Select().getFinalSelect());

            List<EventLogBookOgType> events = RequestResponseOK.getFromJsonNode(response)
                    .getResults()
                    .stream()
                    .map(LogbookLifeCycleObjectGroup::new)
                    .flatMap(lifecycle -> Stream.concat(lifecycle.events().stream(), Stream.of(lifecycle)))
                    .map(LogbookMapper::getEventOGTypeFromDocument)
                    .collect(Collectors.toList());

            LogBookOgType logbookType = new LogBookOgType();
            logbookType.getEvent()
                .addAll(events);

            return logbookType;
        }
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
    void writeArchiveUnit(JsonNode result, ListMultimap<String, String> multimap, Map<String, String> ogs, boolean exportWithLogBookLFC)
        throws JsonProcessingException, DatatypeConfigurationException, JAXBException, ProcessingException {

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

        if (exportWithLogBookLFC) {
            addArchiveUnitLogbookType(xmlUnit);
        }

        marshaller.marshal(xmlUnit, writer);

    }

    private void addArchiveUnitLogbookType(ArchiveUnitType xmlUnit) throws ProcessingException {
        try (LogbookLifeCyclesClient client = logbookLifeCyclesClientFactory.getClient()) {
            Select select = new Select();

            JsonNode response = client.selectUnitLifeCycleById(xmlUnit.getId(), select.getFinalSelect());
            if (response != null && response.has(TAG_RESULTS) && response.get(TAG_RESULTS).size() > 0) {
                JsonNode rootEvent = response.get(TAG_RESULTS).get(0);
                LogbookLifeCycleUnit logbookLFC = new LogbookLifeCycleUnit(rootEvent);
                LogBookType logbookType = new LogBookType();
                for (Document event : logbookLFC.events()) {
                    logbookType.getEvent().add(LogbookMapper.getEventTypeFromDocument(event));
                }
                logbookType.getEvent().add(LogbookMapper.getEventTypeFromDocument(logbookLFC));
                xmlUnit.getManagement().setLogBook(logbookType);
            }
        } catch (LogbookClientException | InvalidParseOperationException e) {
            throw new ProcessingException(e);
        }
    }

    @Override
    public void close() throws XMLStreamException {
        writer.close();
    }

    void startDescriptiveMetadata() throws XMLStreamException {
        writer.writeStartElement(TAG_DESCRIPTIVE_METADATA);
    }

    void startDataObjectPackage() throws XMLStreamException {
        writer.writeStartElement(TAG_DATA_OBJECT_PACKAGE);
    }

    void endDescriptiveMetadata() throws XMLStreamException {
        writer.writeEndElement();
    }

    void endDataObjectPackage() throws XMLStreamException {
        writer.writeEndElement();
    }

    void writeOriginatingAgency(String originatingAgency) throws JAXBException, XMLStreamException {
        writer.writeStartElement(NAMESPACE_URI, TAG_MANAGEMENT_METADATA);

        marshaller.marshal(new JAXBElement<>(new QName(NAMESPACE_URI, TAG_ORIGINATINGAGENCYIDENTIFIER), String.class, originatingAgency), writer);

        writer.writeEndElement();
    }

    void closeManifest() throws XMLStreamException {
        writer.writeEndElement();
        writer.writeEndDocument();
    }
}
