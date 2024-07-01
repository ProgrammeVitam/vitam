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
package fr.gouv.vitam.functionaltest.cucumber.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import cucumber.api.java.After;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.export.dip.DipRequest;
import fr.gouv.vitam.common.utils.SupportedSedaVersions;
import fr.gouv.vitam.common.xml.XMLInputFactoryUtils;
import fr.gouv.vitam.functionaltest.models.UnitModel;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.collections4.list.TreeList;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Node;

import javax.ws.rs.core.Response;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static fr.gouv.vitam.functionaltest.models.UnitModel.UNIT_MODEL_COMPARATOR;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class DipStep extends CommonStep {

    private static final String EXPECTED_MANIFEST_START_WITH_SEDA_VERSION =
        "<?xml version=\"1.0\" ?><ArchiveDeliveryRequestReply xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:pr=\"info:lc/xmlns/premis-v2\" xmlns=\"%s\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"%s %s\"";

    private static final List<String> IGNORED_UNIT_FIELDS = List.of(
        "id",
        "ArchiveUnitRefId",
        "DataObjectReference",
        "Event",
        "SignedObjectId"
    );
    private static final List<String> IGNORED_GOT_FIELDS = List.of(
        "id",
        "DataObjectVersion",
        "DataObjectGroupId",
        "Uri",
        "DataObjectProfile",
        "Filename",
        "LogBook"
    );

    public DipStep(World world) {
        super(world);
    }

    @When("^j'exporte le dip$")
    public void exportDip() throws VitamException {
        cleanTempDipFile();
        VitamContext vitamContext = initContext();
        String query = world.getQuery();
        JsonNode jsonNode = JsonHandler.getFromString(query);

        DipRequest dipExportRequest = new DipRequest(jsonNode);
        RequestResponse<JsonNode> response = world.getAdminClientV2().exportDIP(vitamContext, dipExportRequest);

        checkExportDipResponse(response);
    }

    @When("^j'exporte le DIP$")
    public void exportDIP() throws VitamException {
        cleanTempDipFile();
        VitamContext vitamContext = initContext();

        String query = world.getQuery();
        DipRequest dipExportRequest = JsonHandler.getFromString(query, DipRequest.class);

        RequestResponse<JsonNode> response = world.getAdminClientV2().exportDIP(vitamContext, dipExportRequest);
        checkExportDipResponse(response);
    }

    @When("^je télécharge le dip$")
    public void downloadDip() throws Exception {
        VitamContext vitamContext = initContext();

        Response response = world.getAccessClient().getDIPById(vitamContext, world.getOperationId());
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        File tempFile = Files.createTempFile("DIP-" + world.getOperationId(), ".zip").toFile();
        try (InputStream dipInputStream = response.readEntity(InputStream.class)) {
            Files.copy(dipInputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        response.close();

        world.setDipFile(tempFile.toPath());
    }

    @When("^j'exporte le dip avec la version SEDA \"([^\"]*)\"")
    public void exportDipBySedaVersion(String sedaVersion) throws VitamException {
        cleanTempDipFile();
        VitamContext vitamContext = initContext();
        String query = world.getQuery();
        JsonNode jsonNode = JsonHandler.getFromString(query);

        DipRequest dipExportRequest = new DipRequest(jsonNode);
        dipExportRequest.setSedaVersion(sedaVersion);
        RequestResponse<JsonNode> response = world.getAdminClientV2().exportDIP(vitamContext, dipExportRequest);

        checkExportDipResponse(response);
    }

    @Then("^le dip contient (\\d+) unités archivistiques$")
    public void checkDipUnitCount(int nbUnits) throws Exception {
        try (ZipFile zipFile = new ZipFile(world.getDipFile().toFile())) {
            // Check manifest
            ZipArchiveEntry manifest = zipFile.getEntry("manifest.xml");
            try (InputStream is = zipFile.getInputStream(manifest)) {
                int cpt = countElements(
                    is,
                    "ArchiveDeliveryRequestReply/DataObjectPackage/DescriptiveMetadata/ArchiveUnit"
                );
                assertThat(cpt).isEqualTo(nbUnits);
            }
        }
    }

    @Then("^le dip contient (\\d+) groupes d'objets$")
    public void checkDipObjectGroupCount(int nbObjectGroups) throws Exception {
        try (ZipFile zipFile = new ZipFile(world.getDipFile().toFile())) {
            // Check manifest
            ZipArchiveEntry manifest = zipFile.getEntry("manifest.xml");
            try (InputStream is = zipFile.getInputStream(manifest)) {
                int cpt = countElements(is, "ArchiveDeliveryRequestReply/DataObjectPackage/DataObjectGroup");
                assertThat(cpt).isEqualTo(nbObjectGroups);
            }
        }
    }

    @Then("^le dip contient (\\d+) objets dont (\\d+) sont binaires$")
    public void checkDipObjectCount(int nbObjects, int nbBinaryObjects) throws Exception {
        try (ZipFile zipFile = new ZipFile(world.getDipFile().toFile())) {
            // Check manifest
            ZipArchiveEntry manifest = zipFile.getEntry("manifest.xml");
            try (InputStream is = zipFile.getInputStream(manifest)) {
                int cpt = countElements(
                    is,
                    "ArchiveDeliveryRequestReply/DataObjectPackage/DataObjectGroup/BinaryDataObject"
                );
                assertThat(cpt).isEqualTo(nbObjects);
            }

            List<ZipArchiveEntry> entries = Collections.list(zipFile.getEntries());
            long binaryFiles = entries
                .stream()
                .filter((ZipArchiveEntry entry) -> entry.getName().startsWith("Content/"))
                .count();

            assertThat(binaryFiles).isEqualTo(nbBinaryObjects);
        }
    }

    @Then("^le dip utilise la version SEDA \"([^\"]*)\"$")
    public void checkDipSedaVerion(String sedaVersion) throws Exception {
        // Get manifest from DIP
        try (ZipFile zipFile = new ZipFile(world.getDipFile().toFile())) {
            String manifest;
            ZipArchiveEntry manifestEntry = zipFile.getEntry("manifest.xml");
            try (InputStream is = zipFile.getInputStream(manifestEntry)) {
                manifest = IOUtils.toString(is, StandardCharsets.UTF_8.name());
            }

            // Get selected Seda version
            Optional<SupportedSedaVersions> supportedSedaVersion =
                SupportedSedaVersions.getSupportedSedaVersionByVersion(sedaVersion);

            // Then
            assertThat(supportedSedaVersion.isPresent()).isTrue();
            assertThat(manifest).contains(
                String.format(
                    EXPECTED_MANIFEST_START_WITH_SEDA_VERSION,
                    supportedSedaVersion.get().getNamespaceURI(),
                    supportedSedaVersion.get().getNamespaceURI(),
                    supportedSedaVersion.get().getSedaValidatorXSD()
                )
            );
        }
    }

    @Then("le SIP et le DIP sont semblables")
    public void compareSipAndDip() throws Exception {
        final String sipManifest = transform(getManifestFromZip(world.getSipFile()));
        final String dipManifest = transform(getManifestFromZip(world.getDipFile()));

        XmlMapper xmlMapper = new XmlMapper();
        final JsonNode sip = xmlMapper.readValue(sipManifest, JsonNode.class);
        final JsonNode dip = xmlMapper.readValue(dipManifest, JsonNode.class);
        final JsonNode gots = sip.at("/DataObjectPackage/DataObjectGroup");
        final JsonNode gotsDip = dip.at("/DataObjectPackage/DataObjectGroup");
        final JsonNode units = sip.at("/DataObjectPackage/DescriptiveMetadata/ArchiveUnit");
        final JsonNode unitsDip = dip.at("/DataObjectPackage/DescriptiveMetadata/ArchiveUnit");

        final ArrayNode SipGotsFiltred = filterGots(gots);
        final ArrayNode DipGotsFiltred = filterGots(gotsDip);

        purgeIgnoredFields(SipGotsFiltred, IGNORED_GOT_FIELDS);
        purgeIgnoredFields(DipGotsFiltred, IGNORED_GOT_FIELDS);
        assertEqualsGotsJson(SipGotsFiltred, DipGotsFiltred);

        purgeIgnoredFields(units, IGNORED_UNIT_FIELDS);
        purgeIgnoredFields(unitsDip, IGNORED_UNIT_FIELDS);
        assertEqualsUnitsJson(units, unitsDip);
    }

    private static void assertEqualsUnitsJson(JsonNode expectedUnits, JsonNode value) throws Exception {
        final List<UnitModel> listExpectedUnits = sortUnitJson(expectedUnits);
        final List<UnitModel> listRealUnits = sortUnitJson(value);

        JsonAssert.assertJsonEquals(
            JsonHandler.toJsonNode(listExpectedUnits),
            JsonHandler.toJsonNode(listRealUnits),
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
        );
    }

    private void assertEqualsGotsJson(JsonNode expectedGots, JsonNode value) {
        JsonAssert.assertJsonEquals(expectedGots, value, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    private ArrayNode filterGots(JsonNode gots) {
        ArrayNode listGots = JsonHandler.createArrayNode();
        if (gots.isArray()) {
            for (var i = 0; i < gots.size(); i++) {
                var got = gots.get(i);
                var binaryDataObject = got.get("BinaryDataObject");
                if (binaryDataObject.isArray()) {
                    var objects = (ArrayNode) binaryDataObject;
                    for (var j = 0; j < objects.size(); j++) {
                        var object = objects.get(j);
                        if (object.get("DataObjectVersion").asText().startsWith("BinaryMaster")) {
                            listGots.add(object);
                        }
                    }
                    ((ArrayNode) gots).set(i, objects);
                } else {
                    if (binaryDataObject.get("DataObjectVersion").asText().startsWith("BinaryMaster")) {
                        listGots.add(binaryDataObject);
                    }
                }
            }
        } else {
            listGots.add(gots);
        }

        return listGots;
    }

    private void purgeIgnoredFields(JsonNode jsonNode, List<String> ignoredFields) {
        if (jsonNode.isValueNode()) {
            return;
        }
        if (jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                purgeIgnoredFields(jsonNode.get(i), ignoredFields);
            }
            return;
        }
        if (jsonNode.isObject()) {
            ((ObjectNode) jsonNode).remove(ignoredFields);
            for (JsonNode node : jsonNode) {
                purgeIgnoredFields(node, ignoredFields);
            }
            return;
        }
        throw new IllegalStateException("Unknown type " + jsonNode);
    }

    private boolean filterNode(Node node) {
        return node.getNodeName().equalsIgnoreCase("BinaryDataObject");
    }

    private Node getChildNode(Node node, String childNodeName) {
        for (var k = 0; k < node.getChildNodes().getLength(); k++) {
            if (node.getChildNodes().item(k).getNodeName().equalsIgnoreCase(childNodeName)) return node
                .getChildNodes()
                .item(k);
        }
        return null;
    }

    private String transform(Source manifest) throws FileNotFoundException, TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer(
            new StreamSource(PropertiesUtils.getResourceAsStream("transform.xsl"))
        );

        StringWriter writer = new StringWriter();
        StreamResult streamResult = new StreamResult(writer);

        transformer.transform(manifest, streamResult);
        return writer.toString();
    }

    private Source getManifestFromZip(Path zip) throws IOException {
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            // Check manifest
            ZipArchiveEntry manifest = zipFile.getEntry("manifest.xml");

            try (InputStream is = zipFile.getInputStream(manifest)) {
                String xml = IOUtils.toString(is, StandardCharsets.UTF_8);
                return new StreamSource(new StringReader(xml));
            }
        }
    }

    private int countElements(InputStream inputStream, String path) throws XMLStreamException {
        int cpt = 0;
        final XMLInputFactory xmlInputFactory = XMLInputFactoryUtils.newInstance();
        Stack<String> elementNames = new Stack<>();
        final XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(inputStream);
        while (eventReader.hasNext()) {
            final XMLEvent event = eventReader.nextEvent();
            switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    final StartElement startElement = event.asStartElement();
                    String qName = startElement.getName().getLocalPart();
                    elementNames.add(qName);

                    String fullElementName = String.join("/", elementNames);

                    if (fullElementName.equals(path)) {
                        cpt++;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    elementNames.pop();
                    break;
            }
        }
        return cpt;
    }

    @After
    public void afterScenario() {
        cleanTempDipFile();
    }

    private void cleanTempDipFile() {
        if (world.getDipFile() != null) {
            FileUtils.deleteQuietly(world.getDipFile().toFile());
            world.setDipFile(null);
        }
    }

    private VitamContext initContext() {
        VitamContext vitamContext = new VitamContext(world.getTenantId());
        vitamContext.setApplicationSessionId(world.getApplicationSessionId());
        vitamContext.setAccessContract(world.getContractId());
        return vitamContext;
    }

    private void checkExportDipResponse(RequestResponse<JsonNode> response) throws VitamException {
        assertThat(response.isOk()).isTrue();

        final String operationId = response.getHeaderString(X_REQUEST_ID);
        world.setOperationId(operationId);

        final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());
        boolean processTimeout = vitamPoolingClient.wait(
            world.getTenantId(),
            operationId,
            ProcessState.COMPLETED,
            100,
            1_000L,
            TimeUnit.MILLISECONDS
        );

        if (!processTimeout) {
            fail("dip processing not finished. Timeout exceeded.");
        }

        assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
    }

    private static List<UnitModel> sortUnitJson(JsonNode expectedUnits) throws InvalidParseOperationException {
        final List<UnitModel> listExpectedUnits = JsonHandler.getFromJsonNode(
            expectedUnits,
            TreeList.class,
            UnitModel.class
        );
        listExpectedUnits.sort(UNIT_MODEL_COMPARATOR);
        return listExpectedUnits;
    }
}
