package fr.gouv.vitam.worker.core.plugin.dip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.culture.archivesdefrance.seda.v2.OrganizationWithIdType;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mapping.dip.ArchiveUnitMapper;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.SedaConstants.NAMESPACE_URI;
import static fr.gouv.vitam.common.SedaConstants.TAG_DATA_OBJECT_PACKAGE;
import static fr.gouv.vitam.common.SedaConstants.TAG_DESCRIPTIVE_METADATA;
import static fr.gouv.vitam.common.SedaConstants.TAG_MANAGEMENT_METADATA;
import static fr.gouv.vitam.common.SedaConstants.TAG_ORIGINATINGAGENCY;
import static fr.gouv.vitam.common.mapping.dip.UnitMapper.buildObjectMapper;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.SEDA_FILE;
import static fr.gouv.vitam.worker.common.utils.SedaUtils.XSI_URI;

public class CreateManifest extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CreateManifest.class);

    private static final String CREATE_MANIFEST = "CREATE_MANIFEST";

    private ArchiveUnitMapper archiveUnitMapper;
    private ObjectMapper objectMapper;
    private static JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance("fr.gouv.culture.archivesdefrance.seda.v2");
        } catch (JAXBException e) {
            LOGGER.error("unable to create jaxb context", e);
        }
    }

    public CreateManifest() {
        archiveUnitMapper = new ArchiveUnitMapper();
        objectMapper = buildObjectMapper();
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO)
        throws ProcessingException, ContentAddressableStorageServerException {

        final ItemStatus itemStatus = new ItemStatus(CREATE_MANIFEST);

        try {
            JsonNode query = handlerIO.getJsonFromWorkspace("query.json");

            MetaDataClient client = MetaDataClientFactory.getInstance().getClient();
            JsonNode jsonNode = client.selectUnits(query);
            ArrayNode results = (ArrayNode) jsonNode.get("$results");

            Set<String> orginatingAgencies = new HashSet<>();

            try {
                File manifestFile = handlerIO.getNewLocalFile(SEDA_FILE);
                OutputStream outputStream = new FileOutputStream(manifestFile);

                ListMultimap<String, String> multimap = ArrayListMultimap.create();

                for (JsonNode result : results) {
                    String id = result.get(VitamFieldsHelper.id()).asText();
                    ArrayNode nodes = (ArrayNode) result.get(VitamFieldsHelper.allunitups());
                    for (JsonNode node : nodes) {
                        multimap.put(node.asText(), id);
                    }
                    orginatingAgencies.add(result.get(VitamFieldsHelper.originatingAgency()).asText());
                }

                if (orginatingAgencies.size() > 1) {
                    itemStatus.increment(StatusCode.KO);
                    ObjectNode infoNode = JsonHandler.createObjectNode();
                    infoNode.put("Reason",
                        "Too many originating agencies (dip must have only units of one originating agencies)");
                    String evdev = JsonHandler.unprettyPrint(infoNode);
                    itemStatus.setEvDetailData( evdev );
                    return new ItemStatus(CREATE_MANIFEST).setItemsStatus(CREATE_MANIFEST, itemStatus);
                }

                buildManifest(results, outputStream, multimap, Iterables.getOnlyElement(orginatingAgencies));
                outputStream.close();
                handlerIO.transferFileToWorkspace(SEDA_FILE, manifestFile, true, false);

                itemStatus.increment(StatusCode.OK);
            } catch (XMLStreamException | JAXBException | DatatypeConfigurationException e) {
                throw new ProcessingException(e);
            }

        } catch (IOException | InvalidParseOperationException | MetaDataExecutionException |
            MetaDataClientServerException | MetaDataDocumentSizeException e) {
            throw new ProcessingException(e);
        }
        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(CREATE_MANIFEST).setItemsStatus(CREATE_MANIFEST, itemStatus);
    }

    private void buildManifest(ArrayNode results, OutputStream outputStream,
        ListMultimap<String, String> multimap, String originatingAgency)
        throws XMLStreamException, JAXBException, com.fasterxml.jackson.core.JsonProcessingException,
        DatatypeConfigurationException {
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
        writer.writeAttribute("xsi", XSI_URI, "schemaLocation",
            NAMESPACE_URI + " seda-2.0-main.xsd");

        writer.writeStartElement(TAG_DATA_OBJECT_PACKAGE);
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
            marshaller.marshal(xmlUnit, writer);
        }

        writer.writeEndElement();

        writer.writeStartElement(NAMESPACE_URI, TAG_MANAGEMENT_METADATA);
        OrganizationWithIdType originatingAgencyType = new OrganizationWithIdType();
        IdentifierType identifierType = new IdentifierType();
        identifierType.setValue(originatingAgency);

        originatingAgencyType.setIdentifier(identifierType);
        marshaller.marshal(
            new JAXBElement<>(new QName(NAMESPACE_URI, TAG_ORIGINATINGAGENCY),
                OrganizationWithIdType.class, originatingAgencyType), writer);

        writer.writeEndElement();

        writer.writeEndElement();

        writer.writeEndElement();
        writer.writeEndDocument();
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO: add check on file listUnit.json.
    }

}
