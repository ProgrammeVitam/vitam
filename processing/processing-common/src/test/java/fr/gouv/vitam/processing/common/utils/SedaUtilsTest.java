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
package fr.gouv.vitam.processing.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.client.MetaDataClient;
import fr.gouv.vitam.client.MetaDataClientFactory;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.common.utils.SedaUtils.CheckSedaValidationStatus;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class})
public class SedaUtilsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String SIP = "sip1.xml";
    private static final String SIP_ARBORESCENCE = "SIP_Arborescence.xml";
    private static final String OBJ = "obj";
    private static final String ARCHIVE_UNIT = "archiveUnit.xml";
    private static final String INGEST_TREE = "INGEST_TREE.json";
    private static final String ARCHIVE_ID_TO_GUID_MAP = "ARCHIVE_ID_TO_GUID_MAP.txt";
    private static final String DIGESTMESSAGE = "ZGVmYXVsdA==";
    private static final String OBJECT_GROUP = "objectGroup.json";
    private WorkspaceClient workspaceClient;
    private MetaDataClient metadataClient;
    private MetaDataClientFactory metadataFactory;
    private final InputStream seda = Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP);
    private final InputStream seda_arborescence =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_ARBORESCENCE);
    private final InputStream archiveUnit = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream(ARCHIVE_UNIT);
    private final InputStream objectGroup = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream(OBJECT_GROUP);
    private final InputStream errorExample = new ByteArrayInputStream("test".getBytes());
    private SedaUtils utils;
    private final WorkParams params = new WorkParams().setGuuid(OBJ).setContainerName(OBJ)
        .setServerConfiguration(new ServerConfiguration().setUrlWorkspace(OBJ).setUrlMetada(OBJ))
        .setObjectName(OBJ);

    @Before
    public void setUp() {
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        metadataClient = mock(MetaDataClient.class);
        metadataFactory = mock(MetaDataClientFactory.class);
    }

    @Test
    public void givenCorrectManifestWhenSplitElementThenOK()
        throws XMLStreamException, IOException, ProcessingException, ContentAddressableStorageException,
        LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        LogbookClientNotFoundException, InvalidParseOperationException, URISyntaxException {

        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);
        utils.extractSEDA(params);

        when(workspaceClient.computeObjectDigest(anyObject(), anyObject(), anyObject())).thenReturn(DIGESTMESSAGE);

        final XMLInputFactory factory = XMLInputFactory.newInstance();
        final XMLEventReader evenReader = factory.createXMLEventReader(new FileReader("src/test/resources/sip.xml"));
        utils.compareDigestMessage(evenReader, workspaceClient, OBJ);

        assertEquals(utils.getBinaryDataObjectIdToGuid().size(), 4);
        assertEquals(utils.getBinaryDataObjectIdToGroupId().size(), 4);
        assertEquals(utils.getObjectGroupIdToBinaryDataObjectId().size(), 1);
        assertEquals(utils.getUnitIdToGuid().size(), 1);
        assertEquals(utils.getUnitIdToGroupId().size(), 1);
    }

    // TODO : Fix it bug on jenkins
    @Ignore
    @Test
    public void givenGuidWhenXmlExistThenReturnValid() throws Exception {
        when(workspaceClient.getObject(Mockito.anyObject(), Mockito.anyObject())).thenReturn(seda);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);
        assertTrue(CheckSedaValidationStatus.VALID.equals(utils.checkSedaValidation(params)));
    }

    // TODO : Fix it bug on jenkins
    @Ignore
    @Test
    public void givenGuidWhenXmlNotXMLThenReturnNotXmlFile() throws Exception {
        final String str = "This is not an xml file";
        final InputStream is = new ByteArrayInputStream(str.getBytes());
        when(workspaceClient.getObject(Mockito.anyObject(), Mockito.anyObject())).thenReturn(is);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);

        utils = new SedaUtilsFactory().create(metadataFactory);
        CheckSedaValidationStatus status = utils.checkSedaValidation(params);
        assertTrue(CheckSedaValidationStatus.NOT_XML_FILE.equals(status));
    }

    // TODO : Fix it bug on jenkins
    @Ignore
    @Test
    public void givenGuidWhenXmlNotXMLThenReturnNotXsdValid() throws Exception {
        final String str = "<invalidTag>This is an invalid Tag</invalidTag>";
        final InputStream is = new ByteArrayInputStream(str.getBytes());
        when(workspaceClient.getObject(Mockito.anyObject(), Mockito.anyObject())).thenReturn(is);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);

        utils = new SedaUtilsFactory().create(metadataFactory);
        CheckSedaValidationStatus status = utils.checkSedaValidation(params);
        assertTrue(CheckSedaValidationStatus.NOT_XSD_VALID.equals(status));
    }

    @Test
    public void givenGuidWhenXmlNotExistThenReturnNoFile() throws Exception {
        when(workspaceClient.getObject(Mockito.anyObject(), Mockito.anyObject()))
            .thenThrow(new ContentAddressableStorageNotFoundException(""));
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);

        utils = new SedaUtilsFactory().create(metadataFactory);
        CheckSedaValidationStatus status = utils.checkSedaValidation(params);
        assertTrue(CheckSedaValidationStatus.NO_FILE.equals(status));
    }

    @Test
    public void givenSedaHasMessageIdWhengetMessageIdThenReturnCorrect() throws Exception {
        when(workspaceClient.getObject(params.getGuuid(), "SIP/manifest.xml")).thenReturn(seda);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);
        assertEquals("Entrée_avec_groupe_d_objet", utils.getMessageIdentifier(params));
    }

    @Test
    public void givenCorrectArchiveUnitWhenIndexUnitThenOK() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenReturn("");
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(archiveUnit);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);

        // Create required temporary files
        writeMapToTmpFileForTest();
        saveTmpArchiveUnitTree();

        // Update params
        params.setObjectName("ID019G");

        // Run index method
        utils.indexArchiveUnit(params);

        // RollBack params modification
        params.setObjectName(OBJ);
    }

    private void writeMapToTmpFileForTest()
        throws IOException, URISyntaxException {
        File sourceMapTmpFile = new File(Thread.currentThread().getContextClassLoader()
            .getResource(ARCHIVE_ID_TO_GUID_MAP).toURI());
        File mapTmpFile = PropertiesUtils
            .fileFromTmpFolder(SedaUtils.ARCHIVE_ID_TO_GUID_MAP_FILE_NAME_PREFIX + OBJ + SedaUtils.TXT_EXTENSION);
        FileUtils.copyFile(sourceMapTmpFile, mapTmpFile);
    }

    private void saveTmpArchiveUnitTree() throws InvalidParseOperationException, URISyntaxException, IOException {
        File ingestTreeFile = new File(Thread.currentThread().getContextClassLoader()
            .getResource(INGEST_TREE).toURI());
        File treeTmpFile = PropertiesUtils
            .fileFromTmpFolder(SedaUtils.ARCHIVE_TREE_TMP_FILE_NAME_PREFIX + OBJ + SedaUtils.JSON_EXTENSION);
        FileUtils.copyFile(ingestTreeFile, treeTmpFile);
    }

    @Test(expected = ProcessingException.class)
    public void givenArchiveUnitWrongFormatWhenIndexUnitThenOK() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenReturn("");
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(errorExample);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);

        utils.indexArchiveUnit(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenArchiveUnitWrongFormatWhenIndexUnitWithMetadataThenOK() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenThrow(new InvalidParseOperationException(""));
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(archiveUnit);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);

        utils.indexArchiveUnit(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenCreateArchiveUnitErrorWhenIndexUnitThenThrowError() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenReturn("");
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(null);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);

        utils.indexArchiveUnit(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenNotExistArchiveUnitWhenIndexUnitThenThrowError() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenThrow(new MetaDataExecutionException(""));
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);

        utils.indexArchiveUnit(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenGetArchiveUnitErrorWhenIndexUnitThenThrowError() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenReturn("");
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenThrow(new ContentAddressableStorageServerException(""));
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);

        utils.indexArchiveUnit(params);
    }

    @Test
    public void givenCorrectObjectGroupWhenIndexObjectGroupThenOK() throws Exception {
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(objectGroup);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);

        utils.indexObjectGroup(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenCorrectObjectGroupWhenIndexObjectGroupThenThrowError() throws Exception {
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenThrow(new ContentAddressableStorageServerException(""));
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);

        utils = new SedaUtilsFactory().create(metadataFactory);
        utils.indexObjectGroup(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenCreateObjectGroupErrorWhenIndexObjectGroupThenThrowError() throws Exception {
        when(metadataClient.insertObjectGroup(anyObject())).thenThrow(new MetaDataExecutionException(""));
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);

        utils = new SedaUtilsFactory().create(metadataFactory);
        utils.indexObjectGroup(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenNotExistObjectGroupWhenIndexObjectGroupThenThrowError() throws Exception {
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(null);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);

        utils = new SedaUtilsFactory().create(metadataFactory);
        utils.indexObjectGroup(params);

    }

    @Test
    public void givenManifestWhenGetInfoThenGetVersionList()
        throws Exception {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        final XMLEventReader evenReader = factory.createXMLEventReader(
            new FileReader(PropertiesUtils.getResourcesPath("sip.xml").toString()));
        List<String> versionList = new ArrayList<String>();

        utils = new SedaUtilsFactory().create(metadataFactory);
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
        utils = new SedaUtilsFactory().create(metadataFactory);

        final XMLInputFactory factory = XMLInputFactory.newInstance();

        XMLEventReader evenReader = factory.createXMLEventReader(new FileReader("src/test/resources/sip.xml"));
        assertEquals(0, utils.compareVersionList(evenReader, "src/test/resources/version.conf").size());

        evenReader = factory.createXMLEventReader(new FileReader("src/test/resources/sip-with-wrong-version.xml"));
        assertEquals(1, utils.compareVersionList(evenReader, "src/test/resources/version.conf").size());
    }

    @Test
    public void givenCorrectManifestWhenArchiveUnitTreeThenOK()
        throws XMLStreamException, IOException, ProcessingException, ContentAddressableStorageException,
        LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        LogbookClientNotFoundException, InvalidParseOperationException, URISyntaxException {

        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda_arborescence);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(new MetaDataClientFactory());
        utils.extractSEDA(params);

        assertEquals(utils.getUnitIdToGuid().size(), 6);

        // Open saved Archive Tree Json file
        File archiveTreeTmpFile = PropertiesUtils
            .fileFromTmpFolder(SedaUtils.ARCHIVE_TREE_TMP_FILE_NAME_PREFIX + OBJ + SedaUtils.JSON_EXTENSION);
        JsonNode archiveTree = JsonHandler.getFromFile(archiveTreeTmpFile);
        assertTrue(archiveTree.has("ID027"));
        assertTrue(archiveTree.has("ID029"));
        assertTrue(archiveTree.get("ID029").has(SedaUtils.UP_FIELD));
        assertTrue(archiveTree.get("ID029").get(SedaUtils.UP_FIELD).isArray());
        assertTrue(archiveTree.get("ID029").get(SedaUtils.UP_FIELD).toString().contains("ID028"));
        assertTrue(archiveTree.get("ID029").get(SedaUtils.UP_FIELD).toString().contains("ID030"));
    }

    @Test
    public void givenCorrectObjectGroupWhenStoreObjectGroupThenOK() throws Exception {
        String containerName = "aeaaaaaaaaaaaaabaa4quakwgip7nuaaaaaq";
        WorkParams paramsObjectGroups =
            new WorkParams().setGuuid(OBJ).setContainerName(containerName)
                .setServerConfiguration(new ServerConfiguration().setUrlWorkspace(OBJ).setUrlMetada(OBJ))
                .setObjectName("aeaaaaaaaaaaaaabaa4quakwgip76jaaaaaq.json").setCurrentStep("Store ObjectGroup");
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(containerName, "SIP/manifest.xml")).thenReturn(seda);
        when(workspaceClient.getObject(containerName, "ObjectGroup/aeaaaaaaaaaaaaabaa4quakwgip76jaaaaaq.json"))
            .thenReturn(objectGroup);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);

        utils.retrieveStorageInformationForObjectGroup(paramsObjectGroups);
    }

    @Test(expected = ProcessingException.class)
    public void givenCorrectObjectGroupWhenStoreObjectGroupThenJsonKO() throws Exception {
        String containerName = "aeaaaaaaaaaaaaabaa4quakwgip7nuaaaaaq";
        WorkParams paramsObjectGroups =
            new WorkParams().setGuuid(OBJ).setContainerName(containerName)
                .setServerConfiguration(new ServerConfiguration().setUrlWorkspace(OBJ).setUrlMetada(OBJ))
                .setObjectName("aeaaaaaaaaaaaaabaa4quakwgip76jaaaaaq.json").setCurrentStep("Store ObjectGroup");
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(containerName, "SIP/manifest.xml")).thenReturn(seda);
        when(workspaceClient.getObject(containerName, "aeaaaaaaaaaaaaabaa4quakwgip76jaaaaaq.json"))
            .thenReturn(objectGroup);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);

        utils.retrieveStorageInformationForObjectGroup(paramsObjectGroups);
    }

    @Test
    public void givenCorrectObjectGroupWhenCheckStorageAvailabilityThenOK() throws Exception {
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);
        long totalSize = utils.computeTotalSizeOfObjectsInManifest(params);
        assertTrue(totalSize > 0);
    }

    @Test(expected = ProcessingException.class)
    public void givenCorrectObjectGroupWhenCheckStorageAvailabilityThenKO() throws Exception {
        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenThrow(new ContentAddressableStorageNotFoundException(""));
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);
        utils.computeTotalSizeOfObjectsInManifest(params);
    }

    @Test
    public void givenCorrectSedaFileWhenCheckStorageAvailabilityThenOK() throws Exception {
        when(workspaceClient.getObjectInformation(anyObject(), anyObject()))
            .thenReturn(getSedaTest());
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);
        long manifestSize = utils.getManifestSize(params);
        assertTrue(manifestSize > 0);
    }

    @Test(expected = ProcessingException.class)
    public void givenProblemWithSedaFileWhenCheckStorageAvailabilityThenKO() throws Exception {
        when(workspaceClient.getObjectInformation(anyObject(), anyObject()))
            .thenReturn(getSedaTestError());
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(metadataFactory);
        utils.getManifestSize(params);
    }

    private JsonNode getSedaTest() {
        ObjectNode jsonNodeObjectInformation = JsonHandler.createObjectNode();
        jsonNodeObjectInformation.put("size", new Long(1024));
        jsonNodeObjectInformation.put("object_name", "objectName");
        jsonNodeObjectInformation.put("container_name", "containerName");
        return jsonNodeObjectInformation;
    }

    private JsonNode getSedaTestError() {
        ObjectNode jsonNodeObjectInformation = JsonHandler.createObjectNode();
        return jsonNodeObjectInformation;
    }

    /*
     * @Test(expected = ProcessingException.class) public void
     * givenCorrectObjectGroupWhenCheckStorageAvailabilityThenKO() throws Exception {
     * when(workspaceClient.getObject(anyObject(), anyObject())) .thenThrow(new
     * ContentAddressableStorageNotFoundException(""));
     * PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient); utils = new
     * SedaUtilsFactory().create(metadataFactory); utils.computeTotalSizeOfObjectsAndManifest(params); }
     */


    // @Test
    // public void givenCompareDigestMessage() throws FileNotFoundException,
    // XMLStreamException, URISyntaxException,
    // ContentAddressableStorageNotFoundException,
    // ContentAddressableStorageServerException,
    // ContentAddressableStorageException, LogbookClientBadRequestException,
    // LogbookClientAlreadyExistsException,
    // LogbookClientServerException, LogbookClientNotFoundException,
    // InvalidParseOperationException {
    // utils = new SedaUtilsFactory().create(workspaceFactory, metadataFactory);
    // when(workspaceClient.computeObjectDigest(anyObject(), anyObject(),
    // anyObject())).thenReturn(DIGESTMESSAGE);
    //
    // final XMLInputFactory factory = XMLInputFactory.newInstance();
    // final XMLEventReader evenReader = factory.createXMLEventReader(new
    // FileReader("src/test/resources/sip.xml"));
    // utils.compareDigestMessage(evenReader, workspaceClient, OBJ);
    // }

}
