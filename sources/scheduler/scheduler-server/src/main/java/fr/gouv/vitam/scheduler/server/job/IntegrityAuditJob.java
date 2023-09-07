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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuditOptions;
import fr.gouv.vitam.common.model.DatabaseCursor;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static fr.gouv.vitam.common.model.ProcessState.COMPLETED;
import static fr.gouv.vitam.common.model.ProcessState.PAUSE;
import static fr.gouv.vitam.common.model.ProcessState.RUNNING;

@DisallowConcurrentExecution
public class IntegrityAuditJob implements Job {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IntegrityAuditJob.class);
    private static final String operationsDelayInMinutesKey = "operationsDelayInMinutes";
    private static final String AUDIT_ACTION = "AUDIT_FILE_INTEGRITY";
    private static final String DSL = "dsl";
    private static final long THRESHOLD = VitamConfiguration.getDistributionThreshold();
    private final MetaDataClientFactory metaDataClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final AdminManagementClientFactory adminManagementClientFactory;

    public IntegrityAuditJob() {
        this(MetaDataClientFactory.getInstance(), LogbookOperationsClientFactory.getInstance(),
            ProcessingManagementClientFactory.getInstance(), AdminManagementClientFactory.getInstance());
    }

    @VisibleForTesting
    public IntegrityAuditJob(MetaDataClientFactory metaDataClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        ProcessingManagementClientFactory processingManagementClientFactory,
        AdminManagementClientFactory adminManagementClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.adminManagementClientFactory = adminManagementClientFactory;
    }


    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.info("Integrity audit job in progress...");
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        int operationsDelayInMinutes = jobDataMap.getIntValue(operationsDelayInMinutesKey);
        boolean atLeastOneTenantFailed = false;
        for (Integer tenantId : VitamConfiguration.getTenants()) {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            String operationId = GUIDFactory.newRequestIdGUID(tenantId).getId();
            VitamThreadUtils.getVitamSession().setRequestId(operationId);

            String lastAuditDate = findLastSuccessfulAuditData();
            String lastUpdateDate = getLastUpdateDateFromLastUnitToAudit(operationsDelayInMinutes, lastAuditDate);
            if (lastUpdateDate == null) {
                LOGGER.info("Skip audit for tenant {} : no new data to audit", tenantId);
            } else {
                atLeastOneTenantFailed = runAudit(operationId, tenantId, lastUpdateDate, lastAuditDate);
            }
        }
        if (atLeastOneTenantFailed) {
            throw new JobExecutionException("At least one tenant has integrity audit failed");
        }
        LOGGER.info("Integrity audit job is finished");
    }

    private boolean runAudit(String operationId, Integer tenantId, String lastUpdateDate, String lastAuditDate) {
        try (AdminManagementClient adminManagementClient = adminManagementClientFactory.getClient();
            ProcessingManagementClient processingManagementClient = processingManagementClientFactory.getClient()) {
            SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
            if (lastAuditDate != null) {
                selectMultiQuery.addQueries(
                    QueryHelper.gte(VitamFieldsHelper.approximateUpdateDate(), lastAuditDate));
            }
            selectMultiQuery.addQueries(QueryHelper.lte(VitamFieldsHelper.approximateUpdateDate(), lastUpdateDate));

            AuditOptions options = new AuditOptions();
            options.setAuditActions(AUDIT_ACTION);
            options.setQuery(selectMultiQuery.getFinalSelect());
            options.setAuditType(DSL);
            adminManagementClient.launchAuditWorkflow(options, false);

            // Await for termination
            int timeSleep = 1000;
            Stopwatch stopwatch = Stopwatch.createStarted();
            ProcessState state;
            StatusCode globalStatus;
            do {

                TimeUnit.MILLISECONDS.sleep(timeSleep);
                timeSleep = Math.min(timeSleep * 2, 60000);

                ItemStatus operationProcessStatus = processingManagementClient.getOperationProcessStatus(operationId);
                state = operationProcessStatus.getGlobalState();

                globalStatus = operationProcessStatus.getGlobalStatus();
                if (globalStatus.equals(StatusCode.KO)) {
                    LOGGER.error("Integrity audit on tenant {} finished with status KO", tenantId);
                    return true;
                } else if (globalStatus.equals(StatusCode.FATAL)) {
                    throw new VitamClientException("Process finished with fatal status");
                } else if (globalStatus.equals(StatusCode.OK) && state.equals(COMPLETED)) {
                    LOGGER.info("Integrity audit successfully finished on tenant {}", tenantId);
                }

            } while ((state.equals(PAUSE) && globalStatus.equals(StatusCode.UNKNOWN) || state.equals(RUNNING)) &&
                stopwatch.elapsed(TimeUnit.MINUTES) < 30);
            return false;
        } catch (VitamClientException | InternalServerException | BadRequestException | InterruptedException |
                 AdminManagementClientServerException e) {
            throw new VitamRuntimeException(e);
        } catch (InvalidCreateOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private String findLastSuccessfulAuditData() {
        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            Select select = new Select();
            select.setQuery(QueryHelper.and()
                .add(QueryHelper.eq("events." + LogbookEvent.EV_TYPE, "AUDIT_CHECK_OBJECT." + AUDIT_ACTION),
                    QueryHelper.isNull(LogbookEvent.RIGHTS_STATEMENT_IDENTIFIER),
                    QueryHelper.in("events." + LogbookEvent.OUT_DETAIL, Stream.of(StatusCode.OK, StatusCode.WARNING)
                        .map(e -> String.format("%s.%s", Contexts.AUDIT_WORKFLOW.getEventType(), e))
                        .toArray(String[]::new))));
            select.addOrderByDescFilter(LogbookEvent.EV_DATE_TIME);
            select.setLimitFilter(0, 1);
            JsonNode result = logbookOperationsClient.selectOperation(select.getFinalSelect());
            JsonNode firstResult = RequestResponseOK.getFromJsonNode(result).getFirstResult();
            if (firstResult == null) {
                LOGGER.warn("Could not find last integrity audit");
                return null;
            } else {
                LogbookOperation logbookOperation = JsonHandler.getFromJsonNode(firstResult, LogbookOperation.class);
                Optional<String> lastUpdateDate =
                    logbookOperation.getEvents().stream().filter(e -> e.getEvType().equals("LIST_OBJECTGROUP_ID"))
                        .map(LogbookEvent::getEvDetData).map(e -> {
                            try {
                                return JsonHandler.getFromString(e);
                            } catch (InvalidParseOperationException ex) {
                                throw new RuntimeException(ex);
                            }
                        }).map(e -> e.get("Last_Update_Date")).filter(Objects::nonNull).map(JsonNode::asText).findFirst();
                return lastUpdateDate.orElseThrow();
            }
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            throw new RuntimeException(e);
        } catch (LogbookClientException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private String getLastUpdateDateFromLastUnitToAudit(int operationsDelayInMinutes, String lastAuditData) {
        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            SelectMultiQuery selectMultiQuery = new SelectMultiQuery();

            if (lastAuditData != null) {
                selectMultiQuery.addQueries(QueryHelper.gte(VitamFieldsHelper.approximateUpdateDate(), lastAuditData));
            }

            selectMultiQuery.addQueries(QueryHelper.lt(VitamFieldsHelper.approximateUpdateDate(),
                LocalDateUtil.getFormattedDateForMongo(
                    LocalDateUtil.now().minus(operationsDelayInMinutes, ChronoUnit.MINUTES))));

            selectMultiQuery.addUsedProjection(VitamFieldsHelper.id(), VitamFieldsHelper.approximateUpdateDate());
            selectMultiQuery.addOrderByAscFilter(VitamFieldsHelper.approximateUpdateDate());
            DatabaseCursor hits;
            JsonNode result;
            String scrollId = "START";
            selectMultiQuery.setScrollFilter(scrollId,
                GlobalDatasParser.DEFAULT_SCROLL_TIMEOUT,
                VitamConfiguration.getElasticSearchScrollLimit());
            String lastUpdateDate = null;
            int size = 0;
            do {
                result = client.selectUnits(selectMultiQuery.getFinalSelect());
                RequestResponseOK<JsonNode> requestResponse = RequestResponseOK.getFromJsonNode(result, JsonNode.class);
                hits = requestResponse.getHits();
                scrollId = hits.getScrollId();
                selectMultiQuery.setScrollFilter(scrollId,
                    GlobalDatasParser.DEFAULT_SCROLL_TIMEOUT,
                    VitamConfiguration.getElasticSearchScrollLimit());
                size += hits.getSize();
                JsonNode last = Iterables.getLast(requestResponse.getResults(), null);
                if (last != null) {
                    lastUpdateDate = last.get(VitamFieldsHelper.approximateUpdateDate()).asText();
                }
            } while (hits.getSize() > 0 && hits.getSize() >= VitamConfiguration.getElasticSearchScrollLimit() &&
                size < THRESHOLD);

            return lastUpdateDate;
        } catch (MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException e) {
            throw new VitamRuntimeException(e);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }
}