package fr.gouv.vitam.workspace.client;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.test.JerseyTest;

// TODO REVIEW comment this to inform this is an utility class for test
public abstract class WorkspaceClientTest extends JerseyTest {
    protected static final String HOST = "http://localhost";
    protected static final int PORT = 8082;
    protected static final String PATH = "/workspace/v1";
    protected WorkspaceClient client;
    
    public WorkspaceClientTest() {
        client = new WorkspaceClient(HOST+":" + PORT);
    }

    protected ExpectedResults mock;

    interface ExpectedResults {
        Response post();

        Response delete();

        Response head();
        
        Response get();
    }

}
