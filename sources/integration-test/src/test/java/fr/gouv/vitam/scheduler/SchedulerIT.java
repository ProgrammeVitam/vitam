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
package fr.gouv.vitam.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.scheduler.server.SchedulerMain;
import fr.gouv.vitam.scheduler.server.client.SchedulerClient;
import fr.gouv.vitam.scheduler.server.client.SchedulerClientFactory;
import fr.gouv.vitam.scheduler.server.job.IntegrityAuditJob;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.lang.SerializationUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.VitamServerRunner.NB_TRY;
import static fr.gouv.vitam.common.VitamServerRunner.SLEEP_TIME;
import static fr.gouv.vitam.common.VitamTestHelper.verifyOperation;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class SchedulerIT extends VitamRuleRunner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SchedulerIT.class);

    private static final Integer TENANT_ID_0 = 0;
    private static final String CONTRACT_ID = "contract";
    private static final String CONTEXT_ID = "Context_IT";
    private static final int DELAY = 5;

    @Rule public LogicalClockRule logicalClock = new LogicalClockRule();

    @ClassRule public static VitamServerRunner runner =
        new VitamServerRunner(SchedulerIT.class, mongoRule.getMongoDatabase().getName(),
            ElasticsearchRule.getClusterName(),
            Sets.newHashSet(MetadataMain.class, WorkerMain.class, AdminManagementMain.class, LogbookMain.class,
                WorkspaceMain.class, BatchReportMain.class, StorageMain.class, DefaultOfferMain.class,
                ProcessManagementMain.class, AccessInternalMain.class, IngestInternalMain.class, SchedulerMain.class));

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        mongoRule.handleAfter(
            Set.of("vitam_schedulers", "vitam_jobs", "vitam_locks", "vitam_triggers", "vitam_calendars"));
        String configurationPath =
            PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();
        FormatIdentifierFactory.getInstance().changeConfigurationFile(configurationPath);
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        runAfter();
        mongoRule.handleAfter(
            Set.of("vitam_schedulers", "vitam_jobs", "vitam_locks", "vitam_triggers", "vitam_calendars"));
        VitamClientFactory.resetConnections();
    }

    @Test
    public void test_integrity_audit_job() throws Exception {
        VitamConfiguration.setTenants(List.of(0, 1));

        // Ingest data to audit
        CompletableFuture.runAsync(() -> {
            try {
                VitamTestHelper.prepareVitamSession(TENANT_ID_0, CONTRACT_ID, CONTEXT_ID);
                VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
                final String ingestOpId = VitamTestHelper.doIngest(TENANT_ID_0, "elimination/TEST_ELIMINATION_V2.zip");
                verifyOperation(ingestOpId, OK);
                logicalClock.logicalSleep(DELAY, ChronoUnit.MINUTES);
            } catch (VitamException e) {
                throw new VitamRuntimeException(e);
            }
        }, VitamThreadPoolExecutor.getDefaultExecutor()).join();

        JobDetail job = JobBuilder.newJob(IntegrityAuditJob.class)
            .usingJobData("operationsDelayInMinutes", DELAY)
            .withIdentity("myJob", "group1")
            .build();

        try (SchedulerClient schedulerClient = SchedulerClientFactory.getInstance().getClient()) {
            schedulerClient.scheduleJob(SerializationUtils.serialize(job));
            schedulerClient.triggerJob("group1.myJob");
        }

        waitJob("group1.myJob");

        // Check
        CompletableFuture.runAsync(() -> {
            try {
                VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
                LogbookOperationsClient clients = LogbookOperationsClientFactory.getInstance().getClient();
                RequestResponseOK<JsonNode> processAudit =
                    (RequestResponseOK<JsonNode>) clients.getLastOperationByType("PROCESS_AUDIT");
                LogbookOperation logbookOperation =
                    JsonHandler.getFromJsonNode(processAudit.getFirstResult(), LogbookOperation.class);
                assertThat(logbookOperation.getEvents()).filteredOn(e -> e.getEvType().equals("LIST_OBJECTGROUP_ID"))
                    .extracting(LogbookEvent::getEvDetData).element(0).matches(e -> e.contains("Last_Update_Date"));
            } catch (InvalidParseOperationException | LogbookClientServerException e) {
                throw new RuntimeException(e);
            }
        }, VitamThreadPoolExecutor.getDefaultExecutor()).join();
    }


    public static void waitJob(String jobKey) {
        try (SchedulerClient schedulerClient = SchedulerClientFactory.getInstance().getClient()) {
            for (int nbtimes = 0; nbtimes <= NB_TRY; nbtimes++) {
                RequestResponseOK<JsonNode> states = schedulerClient.jobState(jobKey);
                if (states.getResults().size() == 0) {
                    break;
                } else if (nbtimes == NB_TRY) {
                    LOGGER.error("Job did not triggred", jobKey);
                    fail("Job did not triggred");
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
        } catch (VitamClientException e) {
            fail("An error occured while retreiving job state", e);
        }
    }
}
