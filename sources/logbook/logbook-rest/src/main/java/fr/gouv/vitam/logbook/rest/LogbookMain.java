package fr.gouv.vitam.logbook.rest;

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
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;

public class LogbookMain {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookMain.class);
    private static final String CONF_FILE_NAME = "logbook.conf";
    private static final String MODULE_NAME = ServerIdentity.getInstance().getRole();
    public static final String PARAMETER_JETTY_SERVER_PORT = "jetty.logbook.port";

    private VitamStarter vitamStarter;

    public LogbookMain(String configurationFile) {
        ParametersChecker.checkParameter(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
            CONF_FILE_NAME), configurationFile);
        vitamStarter = new VitamStarter(LogbookConfiguration.class, configurationFile,
            BusinessApplication.class, AdminApplication.class);
    }

    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                LOGGER.error(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
                throw new IllegalArgumentException(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
                    CONF_FILE_NAME));
            }
            LogbookMain main = new LogbookMain(args[0]);
            VitamServiceRegistry serviceRegistry = new VitamServiceRegistry();
            // Database dependency
            serviceRegistry.checkDependencies(VitamConfiguration.getRetryNumber(), VitamConfiguration.getRetryDelay());

            main.startAndJoin();
        } catch (Exception e) {
            LOGGER.error(String.format(fr.gouv.vitam.common.server.VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);

            System.exit(1);
        }
    }

    public void start() throws VitamApplicationServerException {
        vitamStarter.start();
    }

    public void startAndJoin() throws VitamApplicationServerException {
        vitamStarter.run();
    }

    public void stop() throws VitamApplicationServerException {
        vitamStarter.stop();
    }

}
