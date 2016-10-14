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
package fr.gouv.vitam.worker.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.utils.SedaUtils.CheckSedaValidationStatus;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class})
public class SedaUtilsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String SIP = "sip1.xml";
    private static final String OBJ = "obj";
    private WorkspaceClient workspaceClient;
    private final InputStream seda = Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP);

    private final SedaUtils utils = SedaUtilsFactory.create();
    private final WorkerParameters params = WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
        .newGUID()).setContainerName(OBJ).setUrlWorkspace(OBJ).setUrlMetadata(OBJ).setObjectName(OBJ)
        .setCurrentStep("TEST");

    @Before
    public void setUp() {
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
    }

    // TODO : Fix it bug on jenkins
    @Ignore
    @Test
    public void givenGuidWhenXmlExistThenReturnValid() throws Exception {
        when(workspaceClient.getObject(Matchers.anyObject(), Matchers.anyObject())).thenReturn(seda);
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);
        assertTrue(CheckSedaValidationStatus.VALID.equals(utils.checkSedaValidation(params)));
    }

    // TODO : Fix it bug on jenkins
    @Ignore
    @Test
    public void givenGuidWhenXmlNotXMLThenReturnNotXmlFile() throws Exception {
        final String str = "This is not an xml file";
        final InputStream is = new ByteArrayInputStream(str.getBytes());
        when(workspaceClient.getObject(Matchers.anyObject(), Matchers.anyObject())).thenReturn(is);
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);

        final CheckSedaValidationStatus status = utils.checkSedaValidation(params);
        assertTrue(CheckSedaValidationStatus.NOT_XML_FILE.equals(status));
    }

    // TODO : Fix it bug on jenkins
    @Ignore
    @Test
    public void givenGuidWhenXmlNotXMLThenReturnNotXsdValid() throws Exception {
        final String str = "<invalidTag>This is an invalid Tag</invalidTag>";
        final InputStream is = new ByteArrayInputStream(str.getBytes());
        when(workspaceClient.getObject(Matchers.anyObject(), Matchers.anyObject())).thenReturn(is);
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);

        final CheckSedaValidationStatus status = utils.checkSedaValidation(params);
        assertTrue(CheckSedaValidationStatus.NOT_XSD_VALID.equals(status));
    }

    @Test
    public void givenGuidWhenXmlNotExistThenReturnNoFile() throws Exception {
        when(workspaceClient.getObject(Matchers.anyObject(), Matchers.anyObject()))
            .thenThrow(new ContentAddressableStorageNotFoundException(""));
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);

        final CheckSedaValidationStatus status = utils.checkSedaValidation(params);
        assertTrue(CheckSedaValidationStatus.NO_FILE.equals(status));
    }

    @Test
    public void givenSedaHasMessageIdWhengetMessageIdThenReturnCorrect() throws Exception {
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml"))).thenReturn(seda);
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);
        assertEquals("Entrée_avec_groupe_d_objet", utils.getMessageIdentifier(params));
    }

    @Test
    public void givenManifestWhenGetInfoThenGetVersionList()
        throws Exception {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        final XMLEventReader evenReader = factory.createXMLEventReader(
            new FileReader(PropertiesUtils.getResourcesPath("sip.xml").toString()));
        List<String> versionList;

        versionList = utils.manifestVersionList(evenReader);
        assertEquals(5, versionList.size());
        assertTrue(versionList.contains("PhysicalMaster"));
        assertTrue(versionList.contains("BinaryMaster"));
        assertTrue(versionList.contains("Diffusion"));
        assertTrue(versionList.contains("Thumbnail"));
        assertTrue(versionList.contains("TextContent"));
    }

    @Test
    public void givenCompareVersionList() throws Exception {

        final XMLInputFactory factory = XMLInputFactory.newInstance();

        XMLEventReader evenReader = factory.createXMLEventReader(new FileReader("src/test/resources/sip.xml"));
        assertEquals(0, utils.compareVersionList(evenReader).size());

        evenReader = factory.createXMLEventReader(new FileReader("src/test/resources/sip-with-wrong-version.xml"));
        assertEquals(1, utils.compareVersionList(evenReader).size());
    }

    @Test
    public void givenCorrectObjectGroupWhenCheckStorageAvailabilityThenOK() throws Exception {
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda);
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);
        final long totalSize = utils.computeTotalSizeOfObjectsInManifest(params);
        assertTrue(totalSize > 0);
    }

    @Test(expected = ProcessingException.class)
    public void givenCorrectObjectGroupWhenCheckStorageAvailabilityThenKO() throws Exception {
        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenThrow(new ContentAddressableStorageNotFoundException(""));
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);
        utils.computeTotalSizeOfObjectsInManifest(params);
    }

    @Test
    public void givenCorrectSedaFileWhenCheckStorageAvailabilityThenOK() throws Exception {
        when(workspaceClient.getObjectInformation(anyObject(), anyObject()))
            .thenReturn(getSedaTest());
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);
        final long manifestSize = utils.getManifestSize(params);
        assertTrue(manifestSize > 0);
    }

    @Test(expected = ProcessingException.class)
    public void givenProblemWithSedaFileWhenCheckStorageAvailabilityThenKO() throws Exception {
        when(workspaceClient.getObjectInformation(anyObject(), anyObject()))
            .thenReturn(getSedaTestError());
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);
        utils.getManifestSize(params);
    }

    private JsonNode getSedaTest() {
        final ObjectNode jsonNodeObjectInformation = JsonHandler.createObjectNode();
        jsonNodeObjectInformation.put("size", new Long(1024));
        jsonNodeObjectInformation.put("object_name", "objectName");
        jsonNodeObjectInformation.put("container_name", "containerName");
        return jsonNodeObjectInformation;
    }

    private JsonNode getSedaTestError() {
        return JsonHandler.createObjectNode();
    }

}
