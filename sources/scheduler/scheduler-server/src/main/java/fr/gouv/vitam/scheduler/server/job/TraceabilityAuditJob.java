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

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.AuditLogbookOptions;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.temporal.ChronoUnit;

@DisallowConcurrentExecution
public class TraceabilityAuditJob implements Job {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TraceabilityAuditJob.class);

    private static final String OPERATION_TRACEABILITY_MAX_RENEWALY_DElAY = "operationTraceabilityMaxRenewalDelay";
    private static final String OPERATION_TRACEABILITY_MAX_RENEWALY_DElAY_UNIT =
        "operationTraceabilityMaxRenewalDelayUnit";
    private static final String LIFECYCLE_TRACEABILITY_MAX_RENEWALY_DElAY = "lifecycleTraceabilityMaxRenewalDelay";
    private static final String LIFECYCLE_TRACEABILITY_MAX_RENEWALY_DElAY_Unit =
        "lifecycleTraceabilityMaxRenewalDelayUnit";

    public static final String OP_TYPE = "STP_OP_SECURISATION";

    /**
     * Max delay between 2 logbook operation traceability operations.
     */
    private Integer operationTraceabilityMaxRenewalDelay;
    private ChronoUnit operationTraceabilityMaxRenewalDelayUnit;

    /**
     * Max delay between 2 LFC traceability operations.
     */
    private Integer lifecycleTraceabilityMaxRenewalDelay;
    private ChronoUnit lifecycleTraceabilityMaxRenewalDelayUnit;

    /**
     * Launch audit of operation or lfc traceability for a specific tenant
     */
    private void auditByTenantId(int tenantId, String logbookType, int amount, ChronoUnit unit) {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);

            final LogbookOperationsClientFactory logbookOperationsClientFactory =
                LogbookOperationsClientFactory.getInstance();

            AuditLogbookOptions options = new AuditLogbookOptions(amount, unit, logbookType);
            try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
                client.traceabilityAudit(tenantId, options);
            }
        } catch (LogbookClientServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(" Error when securing Tenant  :  " + tenantId, e);
        } finally {
            VitamThreadUtils.getVitamSession().setTenantId(null);
        }
    }

    private void loadConfiguration(JobDataMap jobDataMap) {
        operationTraceabilityMaxRenewalDelay = jobDataMap.getInt(OPERATION_TRACEABILITY_MAX_RENEWALY_DElAY);
        operationTraceabilityMaxRenewalDelayUnit = ChronoUnit.valueOf(
            jobDataMap.getString(OPERATION_TRACEABILITY_MAX_RENEWALY_DElAY_UNIT)
        );
        lifecycleTraceabilityMaxRenewalDelay = jobDataMap.getInt(LIFECYCLE_TRACEABILITY_MAX_RENEWALY_DElAY);
        lifecycleTraceabilityMaxRenewalDelayUnit = ChronoUnit.valueOf(
            jobDataMap.getString(LIFECYCLE_TRACEABILITY_MAX_RENEWALY_DElAY_Unit)
        );
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.info("Traceability audit operation in progress...");
        loadConfiguration(context.getJobDetail().getJobDataMap());
        VitamConfiguration.getTenants()
            .forEach(tenant -> {
                auditByTenantId(
                    tenant,
                    OP_TYPE,
                    operationTraceabilityMaxRenewalDelay,
                    operationTraceabilityMaxRenewalDelayUnit
                );
                auditByTenantId(
                    tenant,
                    Contexts.UNIT_LFC_TRACEABILITY.getEventType(),
                    lifecycleTraceabilityMaxRenewalDelay,
                    lifecycleTraceabilityMaxRenewalDelayUnit
                );
                auditByTenantId(
                    tenant,
                    Contexts.OBJECTGROUP_LFC_TRACEABILITY.getEventType(),
                    lifecycleTraceabilityMaxRenewalDelay,
                    lifecycleTraceabilityMaxRenewalDelayUnit
                );
            });

        LOGGER.info("Traceability audit operation is finished");
    }
}
