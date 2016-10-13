package fr.gouv.vitam.common.server.application;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;


/**
 * TestResourceImpl implements AccessResource
 */
@Path("/test/v1")
@Consumes("application/json")
@Produces("application/json")
@javax.ws.rs.ApplicationPath("webresources")
public class TestResourceImpl extends ApplicationStatusResource {

    private static final String TEST_MODULE = "TEST";
    private static final String CODE_VITAM = "code_vitam";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TestResourceImpl.class);



    /**
     *
     * @param configuration to associate with TestResourceImpl
     */
    public TestResourceImpl(TestConfiguration configuration) {
        super(new BasicVitamStatusServiceImpl());
        LOGGER.debug("TestResource initialized");
    }


}
