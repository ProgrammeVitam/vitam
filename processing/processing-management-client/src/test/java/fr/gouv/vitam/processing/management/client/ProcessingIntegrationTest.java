/**
  * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital 
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.processing.management.client;

import static com.jayway.restassured.RestAssured.get;
import static org.junit.Assert.assertFalse;

import java.io.InputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.RestAssured;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.metadata.rest.MetaDataApplication;
import fr.gouv.vitam.processing.management.rest.ProcessManagementApplication;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceApplication;

/**
 * 
 */
public class ProcessingIntegrationTest {
    private static final int DATABASE_PORT = 12346;
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    private static final int PORT_SERVICE_PROCESSING = 8098; 
    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_METADATA = 8096;
    private static final String SIP_FOLDER = "SIP";
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/api/v0.0.3";
    private static final String WORKSPACE_PATH = "/workspace/v1";

    private static String CONFIG_PROCESSING_PATH = "";
    private static String CONFIG_WORKSPACE_PATH = "";
    private static String CONFIG_METADATA_PATH = "";
    
    private static ProcessManagementApplication processApplication;
    private static WorkspaceApplication workspaceApplication;
    private static MetaDataApplication medtadataApplication;
    
    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;
 
    
    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;

    private static String WORFKLOW_NAME = "DefaultIngestWorkflow";
    private static String CONTAINER_NAME = GUIDFactory.newGUID().toString();
    private static String SIP_FILE_OK_NAME = "SIP.zip";
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CONFIG_METADATA_PATH = PropertiesUtils.getResourcesPath("metadata.conf").toString();
        CONFIG_PROCESSING_PATH = PropertiesUtils.getResourcesPath("processing.conf").toString();
        CONFIG_WORKSPACE_PATH = PropertiesUtils.getResourcesPath("workspace.conf").toString();
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();      

        // launch metadata     
        medtadataApplication = new MetaDataApplication();
        medtadataApplication.configure(CONFIG_METADATA_PATH, Integer.toString(PORT_SERVICE_METADATA));
        
        // launch processing     
        processApplication = new ProcessManagementApplication();
        processApplication.configure(CONFIG_PROCESSING_PATH, Integer.toString(PORT_SERVICE_PROCESSING));
   
        // launch workspace     
        workspaceApplication = new WorkspaceApplication();
        workspaceApplication.configure(CONFIG_WORKSPACE_PATH, Integer.toString(PORT_SERVICE_WORKSPACE));

        CONTAINER_NAME = GUIDFactory.newGUID().toString();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        mongod.stop();
        mongodExecutable.stop();
    }

    @Test
    public void testServersStatus() throws Exception {
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        get("/status").then().statusCode(200);

        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get("/status").then().statusCode(200);

        RestAssured.port = PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get("/status").then().statusCode(200);
    }
    
    @Test
    public void testWorkflow() throws Exception {
                                
       // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH; 
        
        InputStream zipInputStreamSipObject = Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_FILE_OK_NAME);        
        WorkspaceClientFactory worspaceClienFactory = new WorkspaceClientFactory();
        workspaceClient = worspaceClienFactory.create(WORKSPACE_URL);  
        workspaceClient.createContainer(CONTAINER_NAME);
        workspaceClient.unzipObject(CONTAINER_NAME, SIP_FOLDER, zipInputStreamSipObject);
        
       //call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;        
        processingClient =  new ProcessingManagementClient(PROCESSING_URL);
        String s = processingClient.executeVitamProcess(CONTAINER_NAME, WORFKLOW_NAME);
        assertFalse(s.contains("FATAL"));
     }
}
