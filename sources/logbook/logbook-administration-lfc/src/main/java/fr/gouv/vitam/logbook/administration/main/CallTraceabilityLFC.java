/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.logbook.administration.main;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamConfigurationParameters;
import fr.gouv.vitam.common.configuration.SecureConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.LifecycleTraceabilityStatus;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility to launch the Traceability through command line and external scheduler
 */
public class CallTraceabilityLFC {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CallTraceabilityLFC.class);
    private static final String VITAM_CONF_FILE_NAME = "vitam.conf";
    private static final String VITAM_SECURISATION_NAME = "securisationDaemon.conf";


    enum TraceabilityType {
        ObjectGroup,
        Unit
    }

    /**
     * @param args ignored
     */
    public static void main(String[] args) {
        platformSecretConfiguration();

        if (ArrayUtils.isEmpty(args) || args.length != 1) {
            LOGGER.error("Expecting traceability type argument");
            throw new IllegalStateException("Invalid command arguments");
        }

        TraceabilityType traceabilityType;
        try {
            traceabilityType = TraceabilityType.valueOf(args[0]);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Expecting traceability type argument. Valid values: {}",
                StringUtils.join(TraceabilityType.values(), ", "));
            throw new IllegalStateException("Invalid command arguments", e);
        }

        try {
            File confFile = PropertiesUtils.findFile(VITAM_SECURISATION_NAME);
            final SecureConfiguration conf = PropertiesUtils.readYaml(confFile, SecureConfiguration.class);

            while (true) {
                boolean atLeastOneTenantReachedMaxCapacity = secureAllTenants(traceabilityType, conf);

                if (!atLeastOneTenantReachedMaxCapacity) {
                    LOGGER.info("Done !");
                    break;
                }

                LOGGER.warn("At least one traceability operation reached max capacity. Re-run traceability...");
            }

        } catch (final IOException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Application Server", e);
        } catch (Exception e) {
            LOGGER.error(e);
            throw e;
        }
    }

    private static boolean secureAllTenants(TraceabilityType traceabilityType,
        SecureConfiguration conf) {
        List<Integer> tenants = new ArrayList<>();
        conf.getTenants().forEach((v) -> tenants.add(Integer.parseInt(v)));

        VitamThreadPoolExecutor defaultExecutor = VitamThreadPoolExecutor.getDefaultExecutor();

        List<CompletableFuture> completableFutures = new ArrayList<>();
        AtomicBoolean atLeastOneTenantReachedMaxCapacity = new AtomicBoolean();
        for (Integer tenant : tenants) {
            completableFutures.add(
                CompletableFuture.runAsync(() -> {
                    if (secureByTenantId(tenant, traceabilityType)) {
                        atLeastOneTenantReachedMaxCapacity.set(true);
                    }
                }, defaultExecutor));
        }

        // Await for all tenants
        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();

        // Retry if a least one reached maximum capacity (to avoid contention)
        return atLeastOneTenantReachedMaxCapacity.get();
    }

    /**
     * Launch securization for a specific tenant
     *
     * @param tenantId to be secured
     * @param traceabilityType - Unit or ObjectGroup
     * @return true if the tenant need another run to be fully secured
     */
    private static boolean secureByTenantId(int tenantId,
        TraceabilityType traceabilityType) {
        String operationId = null;
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);

            try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {

                operationId = runLfcTraceability(tenantId, traceabilityType, client);

                if(operationId == null) {
                    LOGGER.info("No " + traceabilityType + " LFC traceability required for tenant " + tenantId);
                    return false;
                }

                // Await for termination (polling the logbook server)
                LifecycleTraceabilityStatus lifecycleTraceabilityStatus;
                int timeSleep = 1000;
                do {

                    Thread.sleep(timeSleep);
                    timeSleep = Math.min(timeSleep * 2, 60000);

                    lifecycleTraceabilityStatus = client.checkLifecycleTraceabilityWorkflowStatus(operationId);

                    LOGGER.info("Traceability operation status for tenant {}, operationId {}, status {})",
                        tenantId, operationId, lifecycleTraceabilityStatus.toString());

                } while (!lifecycleTraceabilityStatus.isCompleted());

                return lifecycleTraceabilityStatus.isMaxEntriesReached();
            }
        } catch (InvalidParseOperationException | LogbookClientServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Error when securing Tenant  :  " + tenantId, e);
        } catch (InterruptedException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Error on Thread on Tenant: " + tenantId + " for operationId: " + operationId, e);
        } finally {
            VitamThreadUtils.getVitamSession().setTenantId(null);
        }
    }

    private static String runLfcTraceability(int tenantId, TraceabilityType traceabilityType,
        LogbookOperationsClient client)

        throws LogbookClientServerException, InvalidParseOperationException {
        RequestResponseOK response;
        switch (traceabilityType) {
            case ObjectGroup:
                response = client.traceabilityLfcObjectGroup();
                break;
            case Unit:
                response = client.traceabilityLfcUnit();
                break;
            default:
                throw new IllegalStateException("Unknown traceability type " + traceabilityType);
        }

        String operationId = getOperationId(response, tenantId);
        if(operationId == null) {
            LOGGER.info("Traceability operation not required for tenant {}", tenantId);
        } else {
            LOGGER.info("Traceability operation for tenant started successfully ({})", tenantId, operationId);
        }
        return operationId;
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

    private static String getOperationId(RequestResponseOK response, int tenant) {
        return (String)response.getFirstResult();
    }

}
