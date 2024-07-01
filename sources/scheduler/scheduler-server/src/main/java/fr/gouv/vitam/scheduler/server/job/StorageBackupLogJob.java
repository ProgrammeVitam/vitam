/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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

package fr.gouv.vitam.scheduler.server.job;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution
public class StorageBackupLogJob implements Job {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageBackupLogJob.class);

    static final String STORAGE_BACKUP_TYPE_PARAMETER = "StorageBackupType";
    static final String STORAGE_SERVER_HOSTS_PARAMETER = "StorageServerHosts";
    private static final String SERVER_SEPARATOR = ";";
    private static final String PORT_SEPARATOR = ":";
    private final StorageClientFactory storageClientFactory;

    public StorageBackupLogJob() {
        this(StorageClientFactory.getInstance());
    }

    @VisibleForTesting
    StorageBackupLogJob(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        final Integer adminTenant = VitamConfiguration.getAdminTenant();
        VitamThreadUtils.getVitamSession().setTenantId(adminTenant);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(adminTenant));
        JobDataMap jobDataMap = context.getTrigger().getJobDataMap();
        final String[] storageServerHosts = jobDataMap
            .getString(STORAGE_SERVER_HOSTS_PARAMETER)
            .split(SERVER_SEPARATOR);
        boolean isError = false;
        for (String storageServerHost : storageServerHosts) {
            final String[] host = storageServerHost.split(PORT_SEPARATOR);
            final ClientConfiguration clientConfiguration = new ClientConfigurationImpl(
                host[0],
                Integer.parseInt(host[1])
            );
            StorageClientFactory.changeMode(clientConfiguration);
            try (StorageClient storageClient = storageClientFactory.getClient()) {
                final String storageBackupType = jobDataMap.getString(STORAGE_BACKUP_TYPE_PARAMETER);
                try {
                    switch (storageBackupType) {
                        case "AccessLog":
                            LOGGER.info(
                                "Storage " + storageBackupType + " backup started on instance " + storageServerHost
                            );
                            storageClient.storageAccessLogBackup(VitamConfiguration.getTenants());
                            break;
                        case "WriteLog":
                            LOGGER.info(
                                "Storage " + storageBackupType + " backup started on instance " + storageServerHost
                            );
                            storageClient.storageLogBackup(VitamConfiguration.getTenants());
                            break;
                        default:
                            throw new IllegalArgumentException(storageBackupType);
                    }
                    LOGGER.info("Storage " + storageBackupType + " backup finished on instance " + storageServerHost);
                } catch (StorageServerClientException | InvalidParseOperationException e) {
                    LOGGER.error(
                        "An error occurred while backup storage " +
                        storageBackupType +
                        " on instance " +
                        storageServerHost
                    );
                    isError = true;
                }
            }
        }
        if (isError) {
            throw new JobExecutionException("Storage backup log completed with error");
        }
    }
}
