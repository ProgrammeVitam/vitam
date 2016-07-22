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
package fr.gouv.vitam.ingest.internal.upload.rest;

import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.jayway.restassured.RestAssured;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class, ProcessingManagementClientFactory.class})

public class IngestInternalResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalResourceTest.class);
    private static final String INGEST_INTERNAL_CONF = "ingest-internal.conf";
    private static final String FILE_NAME ="mySIP";
    private static VitamServer vitamServer;
    private static int port;

    private static JunitHelper junitHelper;

    private static final String REST_URI = "/ingest/v1";
    private static final String STATUS_URI = "/status";
    private static final String UPLOAD_URI = "/upload";

    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;
    private LogbookClient logbookClient;
    private InputStream inputStream;
    private LogbookOperationParameters externalOperationParameters;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = new JunitHelper();
        port = junitHelper.findAvailablePort();
        vitamServer = IngestInternalApplication .startApplication(new String[] {
            PropertiesUtils.getResourcesFile(INGEST_INTERNAL_CONF).getAbsolutePath(),
            Integer.toString(port)});
        ((BasicVitamServer) vitamServer).start();

        RestAssured.port = port;
        RestAssured.basePath = REST_URI;

        LOGGER.debug("Beginning tests");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            ((BasicVitamServer) vitamServer).stop();
            junitHelper.releasePort(port);
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
    }

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        PowerMockito.mockStatic(ProcessingManagementClientFactory.class);
        
        workspaceClient = PowerMockito.mock(WorkspaceClient.class);
        processingClient = PowerMockito.mock(ProcessingManagementClient.class);
        logbookClient = PowerMockito.mock(LogbookClient.class);
        
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        PowerMockito.when(ProcessingManagementClientFactory.create(Mockito.anyObject())).thenReturn(processingClient);
        
        GUID conatinerGuid= GUIDFactory.newGUID();
        externalOperationParameters = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newGUID(), 
            "Ingest external", 
            conatinerGuid,
            LogbookTypeProcess.INGEST, 
            LogbookOutcome.STARTED, 
            "Started: Ingest external",
            conatinerGuid);
           
    }
    // Status
    /**
     * Tests the state of the Ingest Internal service API by GET
     *
     */
    @Test
    public void givenStartedServerWhenGetStatusThenReturnStatusOk(){
        RestAssured.get(STATUS_URI).then().statusCode(Status.OK.getStatusCode());
    }

    // Upload
    /**
     * Tests the upload of the Ingest Internal service API by POST
     *
     */
    @Test
    public void givenAllServicesAvailableAndNoVirusWhenUploadSipAsStreamThenReturnOK() throws Exception {
    	
    	Mockito.doReturn(false).when(workspaceClient).isExistingContainer(Mockito.anyObject());
    	Mockito.doNothing().when(workspaceClient).createContainer(Mockito.anyObject());
        Mockito.doNothing().when(workspaceClient).unzipObject(Mockito.anyObject(), Mockito.anyObject(), Mockito.anyObject());
        
        Mockito.doReturn("OK").when(processingClient).executeVitamProcess(Mockito.anyObject(), Mockito.anyObject());
    	
        inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        RestAssured.given()
        				
        				.multiPart("part", externalOperationParameters, MediaType.APPLICATION_JSON)
        				.multiPart("part", FILE_NAME, inputStream)
        			.then().statusCode(Status.OK.getStatusCode())
        			.when().post(UPLOAD_URI);
    }
    
    @Test
    public void givenAllServicesAvailableAndVirusWhenUploadSipAsStreamThenReturnOK() throws Exception {
    	
    	Mockito.doReturn(false).when(workspaceClient).isExistingContainer(Mockito.anyObject());
    	Mockito.doNothing().when(workspaceClient).createContainer(Mockito.anyObject());
        Mockito.doNothing().when(workspaceClient).unzipObject(Mockito.anyObject(), Mockito.anyObject(), Mockito.anyObject());
        
        Mockito.doReturn("OK").when(processingClient).executeVitamProcess(Mockito.anyObject(), Mockito.anyObject());
    	
        inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        RestAssured.given()
        				
        				.multiPart("part", externalOperationParameters, MediaType.APPLICATION_JSON)
        			.then().statusCode(Status.OK.getStatusCode())
        			.when().post(UPLOAD_URI);
    }

    @Test
    public void givenContainerAlreadyExistsWhenUploadSipAsStreamThenReturnKO() throws Exception {
    	
    	Mockito.doReturn(true).when(workspaceClient).isExistingContainer(Mockito.anyObject());
    
        inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        RestAssured.given()
        				.multiPart("file", FILE_NAME, inputStream)
        				.multiPart("logbook", externalOperationParameters, MediaType.APPLICATION_JSON)
        			.then().statusCode(Status.OK.getStatusCode())
        			.when().post(UPLOAD_URI);

    }
   
    @Test
    public void givenUnzipObjectErrorWhenUploadSipAsStreamThenReturnKO() throws Exception {
        Mockito.doThrow(new ContentAddressableStorageServerException("")).when(workspaceClient)
        .unzipObject(Matchers.anyObject(), Matchers.anyObject(), Matchers.anyObject());

        inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        
        RestAssured.given()
        				.multiPart("file", FILE_NAME, inputStream)
        				.multiPart("logbook", externalOperationParameters, MediaType.APPLICATION_JSON)
        			.then().statusCode(Status.OK.getStatusCode())
        			.when().post(UPLOAD_URI);
    }
    
    @Test
    public void givenProcessUnavailableWhenUploadSipAsStreamThenRaiseAnExceptionProcessingException()
        throws Exception {

        Mockito.doThrow(new ProcessingException("")).when(processingClient).executeVitamProcess(Mockito.anyObject(),
            Mockito.anyObject());
        inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");

        RestAssured.given()
	        			.multiPart("file", FILE_NAME, inputStream)
	        			.multiPart("logbook", externalOperationParameters, MediaType.APPLICATION_JSON)
        			.then().statusCode(Status.OK.getStatusCode())
        			.when().post(UPLOAD_URI).andReturn().getBody().asString().contains("upload failed");
    }
    
    @Test
    public void givenLogbookUnavailableWhenUploadSipAsStreamThenRaiseAnExceptionLogbookClientNotFoundException()
        throws Exception {

        Mockito.doThrow(new LogbookClientServerException("")).when(logbookClient).create(Mockito.anyObject());
        inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");

        RestAssured.given()
        				.multiPart("file", FILE_NAME, inputStream)
        				.multiPart("logbook", externalOperationParameters, MediaType.APPLICATION_JSON)
        			.then().statusCode(Status.OK.getStatusCode())
        			.when().post(UPLOAD_URI).andReturn().getBody().asString().contains("upload failed");
    }

    
}

