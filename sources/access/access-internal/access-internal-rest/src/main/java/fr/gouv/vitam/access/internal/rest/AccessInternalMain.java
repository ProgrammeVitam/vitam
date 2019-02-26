package fr.gouv.vitam.access.internal.rest;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.access.internal.api.AccessInternalModule;
import fr.gouv.vitam.access.internal.common.model.AccessInternalConfiguration;
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

import javax.ws.rs.core.Application;

public class AccessInternalMain {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessInternalMain.class);

    public static final String PARAMETER_JETTY_SERVER_PORT = "jetty.access-internal.port";

    private static final String CONF_FILE_NAME = "access-internal.conf";
    private static final String MODULE_NAME = ServerIdentity.getInstance().getRole();
    private VitamStarter vitamStarter;

    static AccessInternalModule mock = null;

    public AccessInternalMain(String configurationFile) {
        ParametersChecker.checkParameter(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
            CONF_FILE_NAME), configurationFile);
        vitamStarter = new VitamStarter(AccessInternalConfiguration.class, configurationFile,
            BusinessApplication.class, AdminApplication.class);
    }

    @VisibleForTesting
    public AccessInternalMain(String configurationFile,
        Class<? extends Application> testBusinessApplication,
        Class<? extends Application> testAdminApplication) {
        ParametersChecker.checkParameter(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
            CONF_FILE_NAME), configurationFile);
        if (null == testBusinessApplication) {
            testBusinessApplication = BusinessApplication.class;
        }

        if (null == testAdminApplication) {
            testAdminApplication = AdminApplication.class;
        }
        vitamStarter = new VitamStarter(AccessInternalConfiguration.class, configurationFile,
            testBusinessApplication, testAdminApplication);
    }

    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                LOGGER.error(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, CONF_FILE_NAME));
                throw new IllegalArgumentException(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
                    CONF_FILE_NAME));
            }
            AccessInternalMain main = new AccessInternalMain(args[0]);
            // Not useful for Storage but instantiate here VitamServiceRegistry if needed
            // VitamServiceRegistry serviceRegistry = new VitamServiceRegistry();
            // And the register needed dependencies
            VitamServiceRegistry serviceRegistry = new VitamServiceRegistry();
            serviceRegistry.checkDependencies(VitamConfiguration.getRetryNumber(), VitamConfiguration.getRetryDelay());

            main.startAndJoin();
        } catch (Exception e) {
            LOGGER.error(String.format(fr.gouv.vitam.common.server.VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) +
                e.getMessage(), e);

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

    public final VitamStarter getVitamServer() {
        return vitamStarter;
    }
}
