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
import static org.junit.Assert.assertFalse;
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;

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
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

public class SedaUtilsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String SIP = "sip1.xml";
    private static final String SIP_ARBORESCENCE = "SIP_Arborescence.xml";
    private static final String OBJ = "obj";
    private static final String ARCHIVE_UNIT = "archiveUnit.xml";
    private static final String DIGESTMESSAGE = "ZGVmYXVsdA==";
    private static final String OBJECT_GROUP = "objectGroup.json";
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceFactory;
    private MetaDataClient metadataClient;
    private MetaDataClientFactory metadataFactory;
    private final InputStream seda = Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP);
    private final InputStream seda_arborescence = Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_ARBORESCENCE);
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
        workspaceClient = mock(WorkspaceClient.class);
        workspaceFactory = mock(WorkspaceClientFactory.class);
        metadataClient = mock(MetaDataClient.class);
        metadataFactory = mock(MetaDataClientFactory.class);
    }

    @Test
    public void givenCorrectManifestWhenSplitElementThenOK()
        throws XMLStreamException, IOException, ProcessingException, ContentAddressableStorageException,
        LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        LogbookClientNotFoundException, InvalidParseOperationException, URISyntaxException {

        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda);
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(workspaceFactory, null);
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

    @Test
    public void givenGuidWhenXmlExistThenReturnTrue() throws Exception {
        when(workspaceClient.getObject(params.getGuuid(), "SIP/manifest.xml")).thenReturn(seda);
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);

        utils = new SedaUtilsFactory().create(workspaceFactory, null);
        assertTrue(utils.checkSedaValidation(params));
    }

    @Test
    public void givenGuidWhenXmlNotExistThenReturnFalse() throws Exception {
        final String str = "";
        final InputStream is = new ByteArrayInputStream(str.getBytes());
        when(workspaceClient.getObject("XXX", "SIP/manifest.xml")).thenReturn(is);
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);

        utils = new SedaUtilsFactory().create(workspaceFactory, null);
        assertFalse(utils.checkSedaValidation(params));
    }

    @Test
    public void givenSedaHasMessageIdWhengetMessageIdThenReturnCorrect() throws Exception {
        when(workspaceClient.getObject(params.getGuuid(), "SIP/manifest.xml")).thenReturn(seda);
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);

        utils = new SedaUtilsFactory().create(workspaceFactory, null);
		assertEquals("Entrée_avec_groupe_d_objet", utils.getMessageIdentifier(params));
    }

    @Test
    public void givenCorrectArchiveUnitWhenIndexUnitThenOK() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenReturn("");
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(archiveUnit);
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(workspaceFactory, metadataFactory);

        utils.indexArchiveUnit(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenArchiveUnitWrongFormatWhenIndexUnitThenOK() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenReturn("");
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(errorExample);
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(workspaceFactory, metadataFactory);

        utils.indexArchiveUnit(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenArchiveUnitWrongFormatWhenIndexUnitWithMetadataThenOK() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenThrow(new InvalidParseOperationException(""));
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(archiveUnit);
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(workspaceFactory, metadataFactory);

        utils.indexArchiveUnit(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenCreateArchiveUnitErrorWhenIndexUnitThenThrowError() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenReturn("");
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(null);
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(workspaceFactory, metadataFactory);

        utils.indexArchiveUnit(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenNotExistArchiveUnitWhenIndexUnitThenThrowError() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenThrow(new MetaDataExecutionException(""));
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(workspaceFactory, metadataFactory);

        utils.indexArchiveUnit(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenGetArchiveUnitErrorWhenIndexUnitThenThrowError() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenReturn("");
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenThrow(new ContentAddressableStorageServerException(""));
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(workspaceFactory, metadataFactory);

        utils.indexArchiveUnit(params);
    }

    @Test
    public void givenCorrectObjectGroupWhenIndexObjectGroupThenOK() throws Exception {
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(objectGroup);
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(workspaceFactory, metadataFactory);

        utils.indexObjectGroup(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenCorrectObjectGroupWhenIndexObjectGroupThenThrowError() throws Exception {
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenThrow(new ContentAddressableStorageServerException(""));
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);

        utils = new SedaUtilsFactory().create(workspaceFactory, metadataFactory);
        utils.indexObjectGroup(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenCreateObjectGroupErrorWhenIndexObjectGroupThenThrowError() throws Exception {
        when(metadataClient.insertObjectGroup(anyObject())).thenThrow(new MetaDataExecutionException(""));
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);

        utils = new SedaUtilsFactory().create(workspaceFactory, metadataFactory);
        utils.indexObjectGroup(params);
    }

    @Test(expected = ProcessingException.class)
    public void givenNotExistObjectGroupWhenIndexObjectGroupThenThrowError() throws Exception {
        when(metadataFactory.create(anyObject())).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(null);
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);

        utils = new SedaUtilsFactory().create(workspaceFactory, metadataFactory);
        utils.indexObjectGroup(params);

    }

    @Test
    public void givenManifestWhenGetInfoThenGetVersionList()
        throws Exception {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        final XMLEventReader evenReader = factory.createXMLEventReader(
            new FileReader(PropertiesUtils.getResourcesPath("sip.xml").toString()));
        List<String> versionList = new ArrayList<String>();

        utils = new SedaUtilsFactory().create(workspaceFactory, metadataFactory);
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
        utils = new SedaUtilsFactory().create(workspaceFactory, metadataFactory);

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
        when(workspaceFactory.create(anyObject())).thenReturn(workspaceClient);
        utils = new SedaUtilsFactory().create(workspaceFactory, null);
        utils.extractSEDA(params);
        
        assertEquals(utils.getUnitIdToGuid().size(), 6);
        
        // Open saved Archive Tree Json file
        File archiveTreeTmpFile = PropertiesUtils
                .fileFromTmpFolder(SedaUtils.ARCHIVE_TREE_TMP_FILE_NAME_PREFIX + OBJ + SedaUtils.JSON_EXTENSION);
        JsonNode archiveTree = JsonHandler.getFromFile(archiveTreeTmpFile);
        assertTrue(archiveTree.has("ID027"));
        assertTrue(archiveTree.has("ID032"));
        assertTrue(archiveTree.get("ID032").has(SedaUtils.UP_FIELD));
        assertTrue(archiveTree.get("ID032").get(SedaUtils.UP_FIELD).isArray());
        assertTrue(archiveTree.get("ID032").get(SedaUtils.UP_FIELD).toString().contains("ID030"));
        assertTrue(archiveTree.get("ID032").get(SedaUtils.UP_FIELD).toString().contains("ID029"));
    }

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
