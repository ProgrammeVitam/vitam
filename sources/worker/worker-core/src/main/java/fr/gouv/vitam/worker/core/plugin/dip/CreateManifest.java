/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin.dip;

import static fr.gouv.vitam.common.SedaConstants.NAMESPACE_URI;
import static fr.gouv.vitam.common.SedaConstants.TAG_DATA_OBJECT_PACKAGE;
import static fr.gouv.vitam.common.SedaConstants.TAG_DESCRIPTIVE_METADATA;
import static fr.gouv.vitam.common.SedaConstants.TAG_MANAGEMENT_METADATA;
import static fr.gouv.vitam.common.SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;
import static fr.gouv.vitam.common.mapping.dip.UnitMapper.buildObjectMapper;
import static fr.gouv.vitam.worker.common.utils.SedaUtils.XSI_URI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.culture.archivesdefrance.seda.v2.BinaryDataObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectOrArchiveUnitReferenceType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectPackageType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectRefType;
import fr.gouv.culture.archivesdefrance.seda.v2.MinimalDataObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.ObjectFactory;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mapping.dip.ArchiveUnitMapper;
import fr.gouv.vitam.common.mapping.dip.ObjectGroupMapper;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * create manifest and put in on workspace
 */
public class CreateManifest extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CreateManifest.class);

    private static final String CREATE_MANIFEST = "CREATE_MANIFEST";

    private static final int MANIFEST_XML_RANK = 0;
    private static final int GUID_TO_PATH_RANK = 1;
    private static final int BINARIES_RANK = 2;

    private static JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance("fr.gouv.culture.archivesdefrance.seda.v2");
        } catch (JAXBException e) {
            LOGGER.error("unable to create jaxb context", e);
        }
    }


    private ObjectMapper objectMapper;

    private ArchiveUnitMapper archiveUnitMapper;

    private MetaDataClientFactory metaDataClientFactory;

    private ObjectGroupMapper objectGroupMapper;
    private ObjectFactory objectFactory;

    public CreateManifest() {
        archiveUnitMapper = new ArchiveUnitMapper();
        objectGroupMapper = new ObjectGroupMapper();
        objectMapper = buildObjectMapper();
        metaDataClientFactory = MetaDataClientFactory.getInstance();
        this.objectFactory = new ObjectFactory();
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO) throws ProcessingException,
        ContentAddressableStorageServerException {

        final ItemStatus itemStatus = new ItemStatus(CREATE_MANIFEST);

        try (MetaDataClient client = metaDataClientFactory.getClient();) {
            JsonNode query = handlerIO.getJsonFromWorkspace("query.json");

            JsonNode jsonNode = client.selectUnits(query);
            ArrayNode results = (ArrayNode) jsonNode.get("$results");

            Set<String> originatingAgencies = new HashSet<>();

            try {
                File manifestFile = handlerIO.getNewLocalFile(handlerIO.getOutput(MANIFEST_XML_RANK).getPath());
                OutputStream outputStream = new FileOutputStream(manifestFile);

                ListMultimap<String, String> multimap = ArrayListMultimap.create();
                Map<String, String> ogs = new HashMap<>();

                if (results.size() == 0) {
                    itemStatus.increment(StatusCode.KO);
                    ObjectNode infoNode = JsonHandler.createObjectNode();
                    infoNode.put("Reason", "the DSL query has no result");
                    String evdev = JsonHandler.unprettyPrint(infoNode);
                    itemStatus.setEvDetailData(evdev);
                    return new ItemStatus(CREATE_MANIFEST).setItemsStatus(CREATE_MANIFEST, itemStatus);
                }

                for (JsonNode result : results) {
                    String id = result.get(id()).asText();
                    ArrayNode nodes = (ArrayNode) result.get(VitamFieldsHelper.unitups());
                    for (JsonNode node : nodes) {
                        multimap.put(node.asText(), id);
                    }
                    originatingAgencies.add(result.get(VitamFieldsHelper.originatingAgency()).asText());
                    JsonNode jsonNode1 = result.get(VitamFieldsHelper.object());
                    if (jsonNode1 != null) {
                        ogs.put(id, jsonNode1.asText());
                    }
                }

                if (originatingAgencies.size() > 1) {
                    itemStatus.increment(StatusCode.KO);
                    ObjectNode infoNode = JsonHandler.createObjectNode();
                    infoNode.put("Reason",
                        "Too many originating agencies (dip must have only units of one originating agencies)");
                    String evdev = JsonHandler.unprettyPrint(infoNode);
                    itemStatus.setEvDetailData(evdev);
                    return new ItemStatus(CREATE_MANIFEST).setItemsStatus(CREATE_MANIFEST, itemStatus);
                }

                Select select = new Select();
                InQuery in = QueryHelper.in(id(), ogs.values().toArray(new String[ogs.size()]));
                select.setQuery(in);
                JsonNode node = client.selectObjectGroups(select.getFinalSelect());
                ArrayNode objects = (ArrayNode) node.get("$results");

                buildManifest(handlerIO, results, outputStream, multimap, Iterables.getOnlyElement(originatingAgencies),
                    objects, ogs);
                outputStream.close();

                handlerIO.addOuputResult(MANIFEST_XML_RANK, manifestFile, true, false);

            } catch (XMLStreamException | JAXBException | DatatypeConfigurationException | InvalidCreateOperationException e) {
                throw new ProcessingException(e);
            }

        } catch (IOException | InvalidParseOperationException | MetaDataExecutionException |
            MetaDataClientServerException | MetaDataDocumentSizeException e) {
            throw new ProcessingException(e);
        }
        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(CREATE_MANIFEST).setItemsStatus(CREATE_MANIFEST, itemStatus);
    }

    private void buildManifest(HandlerIO handlerIO, ArrayNode results, OutputStream outputStream,
        ListMultimap<String, String> multimap, String originatingAgency, JsonNode objects,
        Map<String, String> ogs)
        throws XMLStreamException, JAXBException, JsonProcessingException, DatatypeConfigurationException,
        ProcessingException {
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
        XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(outputStream);

        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

        writer.writeStartDocument();
        writer.writeStartElement("ArchiveRestitutionRequest");
        writer.writeNamespace("xlink", "http://www.w3.org/1999/xlink");
        writer.writeNamespace("pr", "info:lc/xmlns/premis-v2");
        writer.writeDefaultNamespace(NAMESPACE_URI);
        writer.writeNamespace("xsi", XSI_URI);
        writer.writeAttribute("xsi", XSI_URI, "schemaLocation", NAMESPACE_URI + " seda-2.0-main.xsd");

        writer.writeStartElement(TAG_DATA_OBJECT_PACKAGE);
        Map<String, String> idGOTWithFileName = writeGOT(objects, writer, marshaller);

        storeBinaryInformationOnWorkspace(handlerIO, idGOTWithFileName);

        writeArchiveUnits(results, multimap, ogs, writer, marshaller);

        writer.writeStartElement(NAMESPACE_URI, TAG_MANAGEMENT_METADATA);

        marshaller.marshal(new JAXBElement<>(new QName(NAMESPACE_URI, TAG_ORIGINATINGAGENCYIDENTIFIER),
            String.class, originatingAgency), writer);

        writer.writeEndElement();

        writer.writeEndElement();

        writer.writeEndElement();
        writer.writeEndDocument();
    }

    private void storeBinaryInformationOnWorkspace(HandlerIO handlerIO, Map<String, String> maps)
        throws ProcessingException {
        File guidToPathFile = handlerIO.getNewLocalFile(handlerIO.getOutput(GUID_TO_PATH_RANK).getPath());
        File binaryListFile = handlerIO.getNewLocalFile(handlerIO.getOutput(BINARIES_RANK).getPath());

        try {
            JsonHandler.writeAsFile(maps, guidToPathFile);
            JsonHandler.writeAsFile(maps.keySet(), binaryListFile);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingException(e);
        }

        // put file in workspace
        handlerIO.addOuputResult(GUID_TO_PATH_RANK, guidToPathFile, true, false);
        handlerIO.addOuputResult(BINARIES_RANK, binaryListFile, true, false);
    }

    private Map<String, String> writeGOT(JsonNode objects, XMLStreamWriter writer, Marshaller marshaller)
        throws JsonProcessingException, JAXBException, ProcessingException {
        Map<String, String> maps = new HashMap<>();

        for (JsonNode og : objects) {
            ObjectGroupResponse objectGroup = objectMapper.treeToValue(og, ObjectGroupResponse.class);
            final DataObjectPackageType xmlObject;
            try {
                xmlObject = objectGroupMapper.map(objectGroup);
                List<MinimalDataObjectType> binaryDataObjectOrPhysicalDataObject =
                    xmlObject.getBinaryDataObjectOrPhysicalDataObject();
                for (MinimalDataObjectType minimalDataObjectType : binaryDataObjectOrPhysicalDataObject) {
                    if (minimalDataObjectType instanceof BinaryDataObjectType) {
                        BinaryDataObjectType binaryDataObjectType = (BinaryDataObjectType) minimalDataObjectType;
                        maps.put(minimalDataObjectType.getId(), binaryDataObjectType.getUri());
                    }
                    marshaller.marshal(minimalDataObjectType, writer);
                }
            } catch (InternalServerException e) {
                throw new ProcessingException(e);
            }
        }
        return maps;
    }

    private void writeArchiveUnits(ArrayNode results, ListMultimap<String, String> multimap, Map<String, String> ogs,
        XMLStreamWriter writer, Marshaller marshaller)
        throws XMLStreamException, JsonProcessingException, DatatypeConfigurationException, JAXBException {
        writer.writeStartElement(TAG_DESCRIPTIVE_METADATA);

        for (JsonNode result : results) {
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

        writer.writeEndElement();
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO: add check on file listUnit.json.
    }

}
