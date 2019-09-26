/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.functionaltest.cucumber.step;


import com.fasterxml.jackson.databind.JsonNode;
import cucumber.api.java.After;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.dip.DipExportRequest;
import fr.gouv.vitam.common.model.dip.DipRequest;
import fr.gouv.vitam.common.xml.XMLInputFactoryUtils;
import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;

import javax.ws.rs.core.Response;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

public class DipStep {

    private World world;

    public DipStep(World world) {
        this.world = world;
    }

    @When("^j'exporte le dip$")
    public void exportDip() throws VitamException {

        cleanTempDipFile();

        VitamContext vitamContext = new VitamContext(world.getTenantId());
        vitamContext.setApplicationSessionId(world.getApplicationSessionId());
        vitamContext.setAccessContract(world.getContractId());

        String query = world.getQuery();
        JsonNode jsonNode = JsonHandler.getFromString(query);

        DipRequest dipExportRequest = new DipRequest(jsonNode);
        RequestResponse response = world.getAdminClientV2().exportDIP(vitamContext, dipExportRequest);

        assertThat(response.isOk()).isTrue();

        final String operationId = response.getHeaderString(X_REQUEST_ID);
        world.setOperationId(operationId);

        final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());
        boolean processTimeout = vitamPoolingClient
            .wait(world.getTenantId(), operationId, ProcessState.COMPLETED, 100, 1_000L, TimeUnit.MILLISECONDS);

        if (!processTimeout) {
            fail("dip processing not finished. Timeout exceeded.");
        }

        assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
    }

    @When("^je télécharge le dip$")
    public void downloadDip() throws Exception {

        VitamContext vitamContext = new VitamContext(world.getTenantId());
        vitamContext.setApplicationSessionId(world.getApplicationSessionId());
        vitamContext.setAccessContract(world.getContractId());

        Response response = world.getAccessClient().getDIPById(vitamContext, world.getOperationId());
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        File tempFile = Files.createTempFile("DIP-" + world.getOperationId(), ".zip").toFile();
        try (InputStream dipInputStream = response.readEntity(InputStream.class)) {
            Files.copy(dipInputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        response.close();

        world.setDipFile(tempFile.toPath());
    }

    @Then("^le dip contient (\\d+) unités archivistiques$")
    public void checkDipUnitCount(int nbUnits) throws Exception {

        ZipFile zipFile = new ZipFile(world.getDipFile().toFile());
        // Check manifest
        ZipArchiveEntry manifest = zipFile.getEntry("manifest.xml");
        try (InputStream is = zipFile.getInputStream(manifest)) {
            int cpt =
                countElements(is, "ArchiveDeliveryRequestReply/DataObjectPackage/DescriptiveMetadata/ArchiveUnit");
            assertThat(cpt).isEqualTo(nbUnits);
        }
    }

    @Then("^le dip contient (\\d+) groupes d'objets$")
    public void checkDipObjectGroupCount(int nbObjectGroups) throws Exception {

        ZipFile zipFile = new ZipFile(world.getDipFile().toFile());
        // Check manifest
        ZipArchiveEntry manifest = zipFile.getEntry("manifest.xml");
        try (InputStream is = zipFile.getInputStream(manifest)) {
            int cpt = countElements(is, "ArchiveDeliveryRequestReply/DataObjectPackage/DataObjectGroup");
            assertThat(cpt).isEqualTo(nbObjectGroups);
        }
    }

    @Then("^le dip contient (\\d+) objets dont (\\d+) sont binaires$")
    public void checkDipObjectCount(int nbObjects, int nbBinaryObjects) throws Exception {

        ZipFile zipFile = new ZipFile(world.getDipFile().toFile());
        // Check manifest
        ZipArchiveEntry manifest = zipFile.getEntry("manifest.xml");
        try (InputStream is = zipFile.getInputStream(manifest)) {
            int cpt =
                countElements(is, "ArchiveDeliveryRequestReply/DataObjectPackage/DataObjectGroup/BinaryDataObject");
            assertThat(cpt).isEqualTo(nbObjects);
        }

        List<ZipArchiveEntry> entries = EnumerationUtils.toList(zipFile.getEntries());
        long binaryFiles = entries.stream()
            .filter((ZipArchiveEntry entry) -> entry.getName().startsWith("Content/"))
            .count();

        assertThat(binaryFiles).isEqualTo(nbBinaryObjects);
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
    public void afterScenario() throws IOException {
        cleanTempDipFile();
    }

    private void cleanTempDipFile() {
        if (world.getDipFile() != null) {
            FileUtils.deleteQuietly(world.getDipFile().toFile());
            world.setDipFile(null);
        }
    }
}
