package fr.gouv.vitam.functional.administration.rules.main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamConfigurationParameters;
import fr.gouv.vitam.common.configuration.SecureConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;

/**
 * Utility to launch the rule audit through command line and external scheduler
 */
public class CallRuleAudit {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CallRuleAudit.class);
    private static final String VITAM_CONF_FILE_NAME = "vitam.conf";
    private static final String VITAM_SECURISATION_NAME = "securisationDaemon.conf";

    public static void main(String[] args) {
        platformSecretConfiguration();
        try {
            File confFile = PropertiesUtils.findFile(VITAM_SECURISATION_NAME);
            final SecureConfiguration conf = PropertiesUtils.readYaml(confFile, SecureConfiguration.class);
            VitamThreadFactory instance = VitamThreadFactory.getInstance();
            Thread thread = instance.newThread(() -> {
                conf.getTenants().forEach((v) -> {
                    Integer i = Integer.parseInt(v);
                    auditRuleByTenantId(i);
                });
            });
            thread.start();
            thread.join();
        } catch (final IOException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Application Server", e);
        } catch (InterruptedException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Application Server", e);
        }
    }

    private static void auditRuleByTenantId(int tenantId) {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            AdminManagementClientFactory adminManagementClientFactory = AdminManagementClientFactory.getInstance();

            try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
                client.launchRuleAudit();
            }
        } catch (AdminManagementClientServerException e) {
            throw new IllegalStateException(" Error when auditing Tenant  :  " + tenantId, e);
        }
        finally {
            VitamThreadUtils.getVitamSession().setTenantId(null);
        }
    }

    private static void platformSecretConfiguration() {
        // Load Platform secret from vitam.conf file
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(VITAM_CONF_FILE_NAME)) {
            final VitamConfigurationParameters vitamConfigurationParameters =
                    PropertiesUtils.readYaml(yamlIS, VitamConfigurationParameters.class);

            VitamConfiguration.setSecret(vitamConfigurationParameters.getSecret());
            VitamConfiguration.setFilterActivation(vitamConfigurationParameters.isFilterActivation());

        } catch (final IOException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Application Server", e);
        }
    }
}
