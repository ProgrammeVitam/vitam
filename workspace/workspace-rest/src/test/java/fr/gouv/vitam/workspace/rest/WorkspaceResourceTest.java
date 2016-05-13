package fr.gouv.vitam.workspace.rest;

import static com.jayway.restassured.RestAssured.*;

import java.io.File;
import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.common.Entry;

public class WorkspaceResourceTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
   private  WorkspaceApplication workspaceApplication;

    private static final int SERVER_PORT = 8083;
    private static final String RESOURCE_URI = "/workspace/v1";

    private static final String CONTAINER_NAME = "myContainer";
    private static final String FOLDER_NAME = "myFolder";
    private static final String OBJECT_NAME = "myObject";

    private InputStream stream = null;

    @Before
    public void setup() throws Exception {
        StorageConfiguration configuration = new StorageConfiguration();
        File tempDir = tempFolder.newFolder();
        configuration.setStoragePath(tempDir.getCanonicalPath());
        workspaceApplication= new WorkspaceApplication();
        workspaceApplication.run(configuration, SERVER_PORT);
        RestAssured.port = SERVER_PORT;
        RestAssured.basePath = RESOURCE_URI;
    }

    @After
    public void tearDown() throws Exception {
        workspaceApplication.stop();
    }

    // Status
    @Test
    public void givenStartedServerWhenGetStatusThenReturnStatusOk() {
        get("/status").then().statusCode(Status.OK.getStatusCode());
    }

    // Container
    @Test
    public void givenContainerAlreadyExistsWhenCreateContainerThenReturnConflict() {

        with().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers");

        given().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).expect()
                .statusCode(Status.CONFLICT.getStatusCode()).when().post("/containers");
    }

    @Test
    public void givenContainerNotFoundWhenDeleteContainerThenReturnNotFound() {

        with().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers");

        with().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).then()
                .statusCode(Status.NO_CONTENT.getStatusCode()).when().delete("/containers/" + CONTAINER_NAME);

        given().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).then()
                .statusCode(Status.NOT_FOUND.getStatusCode()).when().delete("/containers/" + CONTAINER_NAME);

    }

    @Test
    public void givenContainerNotFoundWhenGetStatusThenReturnNotFound() {
        given().then().statusCode(Status.NOT_FOUND.getStatusCode()).when().head("/containers/" + CONTAINER_NAME);
    }

    @Test
    public void givenContainerAlreadyExistsWhenCheckContainerThenReturnOk() {
        with().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers");

        given().then().statusCode(Status.OK.getStatusCode()).when().head("/containers/" + CONTAINER_NAME);
    }

    @Test
    public void givenContainerNotFoundWhenCreateContainerThenReturnCreated() {

        given().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers");

    }

    // Folder (Directory)
    @Test
    public void givenContainerNotFoundWhenCreateFolderThenReturnNotFound() {

        given().contentType(ContentType.JSON).body(new Entry(FOLDER_NAME)).then()
                .statusCode(Status.NOT_FOUND.getStatusCode()).when().post("/containers/" + CONTAINER_NAME + "/folders");

    }

    @Test
    public void givenFolderAlreadyExistsWhenCreateFolderThenReturnConflict() {

        with().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers");

        with().contentType(ContentType.JSON).body(new Entry(FOLDER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers/" + CONTAINER_NAME + "/folders");

        given().contentType(ContentType.JSON).body(new Entry(FOLDER_NAME)).then()
                .statusCode(Status.CONFLICT.getStatusCode()).when().post("/containers/" + CONTAINER_NAME + "/folders");

    }

    @Test
    public void givenFolderNotFoundWhenDeleteFolderThenReturnNotFound() {

        with().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers");

        given().contentType(ContentType.JSON).body(new Entry(FOLDER_NAME)).then()
                .statusCode(Status.NOT_FOUND.getStatusCode()).when()
                .delete("/containers" + CONTAINER_NAME + "/folders/" + FOLDER_NAME);

    }

    @Test
    public void givenContainerNotFoundWhenDeleteFolderThenReturnNotFound() {
        given().contentType(ContentType.JSON).body(new Entry(FOLDER_NAME)).then()
                .statusCode(Status.NOT_FOUND.getStatusCode()).when()
                .delete("/containers" + CONTAINER_NAME + "/folders/" + FOLDER_NAME);
    }

    @Test
    public void givenFolderAlreadyExistsWhenDeleteFolderThenReturnNotContent() {
        with().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers");

        with().contentType(ContentType.JSON).body(new Entry(FOLDER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers/" + CONTAINER_NAME + "/folders");

        given().contentType(ContentType.JSON).body(new Entry(FOLDER_NAME)).then()
                .statusCode(Status.NO_CONTENT.getStatusCode()).when()
                .delete("/containers/" + CONTAINER_NAME + "/folders/" + FOLDER_NAME);
    }

    @Test
    public void givenFolderNotFoundWhenGetFolderThenReturnNotFound() {
        given().then().statusCode(Status.NOT_FOUND.getStatusCode()).when()
                .head("/containers" + CONTAINER_NAME + "/folders/" + FOLDER_NAME);
    }

    @Test
    public void givenFolderAlreadyExistsWhenCheckFolderThenReturnOk() {
        with().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers");

        with().contentType(ContentType.JSON).body(new Entry(FOLDER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers/" + CONTAINER_NAME + "/folders");

        given().then().statusCode(Status.OK.getStatusCode()).when()
                .head("/containers/" + CONTAINER_NAME + "/folders/" + FOLDER_NAME);

    }

    // Object
    @Test
    public void givenContainerNotFoundWhenPutObjectThenReturnNotFound() {

        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("file1.pdf");

        given().multiPart("objectName", OBJECT_NAME).multiPart("object", OBJECT_NAME, stream).then()
                .statusCode(Status.NOT_FOUND.getStatusCode()).when().post("/containers/" + CONTAINER_NAME + "/objects");

    }

    @Test
    public void givenContainerAlreadyExistsWhenPutObjectThenReturnCreated() {
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("file1.pdf");

        with().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers");

        given().multiPart("objectName", OBJECT_NAME).multiPart("object", OBJECT_NAME, stream).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers/" + CONTAINER_NAME + "/objects");

    }

    @Test
    public void givenObjectAlreadyExistsWhenDeleteObjectThenReturnNotContent() {

        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("file1.pdf");

        with().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers");

        with().multiPart("objectName", OBJECT_NAME).multiPart("object", OBJECT_NAME, stream).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers/" + CONTAINER_NAME + "/objects");

        given().then().statusCode(Status.NO_CONTENT.getStatusCode()).when()
                .delete("/containers/" + CONTAINER_NAME + "/objects/" + OBJECT_NAME);

    }

    @Test
    public void givenObjectNotFoundWhenGetStatusThenReturnNotFound() {
        given().then().statusCode(Status.NOT_FOUND.getStatusCode()).when()
                .head("/containers/" + CONTAINER_NAME + "/objects/" + OBJECT_NAME);
    }

    @Test
    public void givenObjectNotFoundWhenDeleteObjectThenReturnNotFound() {
        given().then().statusCode(Status.NOT_FOUND.getStatusCode()).when()
                .delete("/containers/" + CONTAINER_NAME + "/objects/" + OBJECT_NAME);
    }

    @Test
    public void givenObjectAlreadyExistsWhenCheckObjectThenReturnOk() {

        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("file1.pdf");

        with().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers");

        with().multiPart("objectName", OBJECT_NAME).multiPart("object", OBJECT_NAME, stream).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers/" + CONTAINER_NAME + "/objects");

        given().then().statusCode(Status.OK.getStatusCode()).when()
                .head("/containers/" + CONTAINER_NAME + "/objects/" + OBJECT_NAME);

    }

    @Test
    public void givenObjectNotFoundWhenGetObjectThenReturnNotFound() {
        given().then().statusCode(Status.NOT_FOUND.getStatusCode()).when()
                .get("/containers/" + CONTAINER_NAME + "/objects/" + OBJECT_NAME);
    }

    @Test
    public void givenObjectAlreadyExistsWhenGetObjectThenReturnOk() {
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("file1.pdf");

        with().contentType(ContentType.JSON).body(new Entry(CONTAINER_NAME)).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers");

        with().multiPart("objectName", OBJECT_NAME).multiPart("object", OBJECT_NAME, stream).then()
                .statusCode(Status.CREATED.getStatusCode()).when().post("/containers/" + CONTAINER_NAME + "/objects");

        given().then().statusCode(Status.OK.getStatusCode()).when()
                .get("/containers/" + CONTAINER_NAME + "/objects/" + OBJECT_NAME);

    }

}
