package fr.gouv.vitam.metadata.rest;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.application.resources.VitamServiceRegistry;
import fr.gouv.vitam.common.serverv2.VitamStarter;
import fr.gouv.vitam.common.serverv2.application.AdminApplication;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;


/**
 * MetadataMain class
 */
public class MetadataMain {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataMain.class);

    /**
     * the port of metadata server
     */
    public static final String PARAMETER_JETTY_SERVER_PORT = "jetty.metadata.port";

    private static final String CONF_FILE_NAME = "storage-engine.conf";
    private static final String MODULE_NAME = ServerIdentity.getInstance().getRole();
    private VitamStarter vitamStarter;

    
    /**
     * Default constructor
     * 
     * @param configurationFile
     */
    public MetadataMain(String configurationFile) {
        ParametersChecker.checkParameter(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
            CONF_FILE_NAME), configurationFile);
        vitamStarter = new VitamStarter(MetaDataConfiguration.class, configurationFile,
            BusinessApplication.class, AdminApplication.class);
    }

    /**
     * The main method
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                LOGGER.error(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
                throw new IllegalArgumentException(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
                    CONF_FILE_NAME));
            }
            MetadataMain storageMain = new MetadataMain(args[0]);
            // Not useful for Storage but instantiate here VitamServiceRegistry if needed
            // VitamServiceRegistry serviceRegistry = new VitamServiceRegistry();
            // And the register needed dependencies
            VitamServiceRegistry serviceRegistry = new VitamServiceRegistry();
            serviceRegistry.checkDependencies(VitamConfiguration.getRetryNumber(), VitamConfiguration.getRetryDelay());

            storageMain.startAndJoin();
        } catch (Exception e) {
            LOGGER.error(String.format(fr.gouv.vitam.common.server.VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) +
                e.getMessage(), e);

            System.exit(1);
        }
    }

    /**
     * Start the server
     * 
     * @throws VitamApplicationServerException
     */
    public void start() throws VitamApplicationServerException {
        vitamStarter.start();
    }

    /**
     * Start and join the server
     * 
     * @throws VitamApplicationServerException
     */
    public void startAndJoin() throws VitamApplicationServerException {
        vitamStarter.run();
    }

    
    /**
     * Stop the server
     * 
     * @throws VitamApplicationServerException
     */
    public void stop() throws VitamApplicationServerException {
        vitamStarter.stop();
    }
}
