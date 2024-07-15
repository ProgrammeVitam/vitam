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

package fr.gouv.vitam.worker.core.plugin.deleteGotVersions.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.DeleteGotVersionsComputedDetails;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.metrics.VitamCommonMetrics;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.util.List;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNode;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatusWithMessage;

public class DeleteGotVersionsAccessionRegisterUpdatePlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        DeleteGotVersionsAccessionRegisterUpdatePlugin.class
    );
    private static final String PLUGIN_ID = "DELETE_GOT_VERSIONS_ACCESSION_REGISTER_UPDATE";

    private final AdminManagementClientFactory adminManagementClientFactory;
    private final BatchReportClientFactory batchReportClientFactory;

    public DeleteGotVersionsAccessionRegisterUpdatePlugin() {
        this(AdminManagementClientFactory.getInstance(), BatchReportClientFactory.getInstance());
    }

    @VisibleForTesting
    public DeleteGotVersionsAccessionRegisterUpdatePlugin(
        AdminManagementClientFactory adminManagementClientFactory,
        BatchReportClientFactory batchReportClientFactory
    ) {
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.batchReportClientFactory = batchReportClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        LOGGER.debug("DeleteGotVersionsAccessionRegisterUpdatePlugin running ...");
        try {
            return updateAccessionRegister(params);
        } catch (ProcessingStatusException e) {
            LOGGER.error(String.format("Accession register update failed with status [%s]", e.getStatusCode()), e);
            return buildItemStatus(PLUGIN_ID, e.getStatusCode(), e.getEventDetails());
        }
    }

    private ItemStatus updateAccessionRegister(WorkerParameters params) throws ProcessingStatusException {
        try (AdminManagementClient adminManagementClient = adminManagementClientFactory.getClient()) {
            List<DeleteGotVersionsComputedDetails> deleteGotVersionsComputedDetails = getDeletedObjectGroups(
                params.getContainerName()
            );

            if (deleteGotVersionsComputedDetails == null) {
                return buildItemStatusWithMessage(PLUGIN_ID, WARNING, "No updates on Access Register");
            }

            for (DeleteGotVersionsComputedDetails computedDetail : deleteGotVersionsComputedDetails) {
                AccessionRegisterDetailModel accessionRegisterDetail = getAccessionRegisterDetail(
                    computedDetail.getOpc()
                );

                long sizeDeletedGots = computedDetail.getTotalSize();
                accessionRegisterDetail.setOperationType(LogbookTypeProcess.DELETE_GOT_VERSIONS.name());

                accessionRegisterDetail.getObjectSize().setIngested(0);
                accessionRegisterDetail.getObjectSize().setDeleted(sizeDeletedGots);
                accessionRegisterDetail
                    .getObjectSize()
                    .setRemained(-1 * accessionRegisterDetail.getObjectSize().getDeleted());

                accessionRegisterDetail.getTotalObjects().setIngested(0);
                accessionRegisterDetail.getTotalObjects().setDeleted(computedDetail.getTotalObjects());
                accessionRegisterDetail
                    .getTotalObjects()
                    .setRemained(-1 * accessionRegisterDetail.getTotalObjects().getDeleted());

                accessionRegisterDetail.getTotalObjectsGroups().setIngested(0);
                accessionRegisterDetail.getTotalObjectsGroups().setDeleted(0);
                accessionRegisterDetail.getTotalObjectsGroups().setRemained(0);

                accessionRegisterDetail.getTotalUnits().setIngested(0);
                accessionRegisterDetail.getTotalUnits().setDeleted(0);
                accessionRegisterDetail.getTotalUnits().setRemained(0);

                accessionRegisterDetail.setOpc(params.getContainerName());
                accessionRegisterDetail.setLastUpdate(LocalDateUtil.getFormattedDateTimeForMongo(LocalDateUtil.now()));

                adminManagementClient.createOrUpdateAccessionRegister(accessionRegisterDetail);
            }
            return buildItemStatus(PLUGIN_ID, OK);
        } catch (AdminManagementClientServerException e) {
            VitamCommonMetrics.CONSISTENCY_ERROR_COUNTER.labels(
                String.valueOf(ParameterHelper.getTenantParameter()),
                "AccessionRegister"
            ).inc();
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "[Consistency ERROR] An error occurred during accession register update",
                e
            );
        }
    }

    private AccessionRegisterDetailModel getAccessionRegisterDetail(String ingestOperationId)
        throws ProcessingStatusException {
        try (AdminManagementClient adminManagementClient = adminManagementClientFactory.getClient()) {
            Select select = new Select();
            select.setQuery(
                QueryHelper.and()
                    .add(
                        QueryHelper.eq(AccessionRegisterDetailModel.OPI, ingestOperationId),
                        exists(VitamFieldsHelper.id())
                    )
            );
            RequestResponse<AccessionRegisterDetailModel> accessionRegisterDetail =
                adminManagementClient.getAccessionRegisterDetail(select.getFinalSelect());

            if (accessionRegisterDetail.isOk()) {
                RequestResponseOK<AccessionRegisterDetailModel> accessionRegisterDetailresponse =
                    ((RequestResponseOK<AccessionRegisterDetailModel>) accessionRegisterDetail);
                if (!accessionRegisterDetailresponse.isEmpty()) {
                    return accessionRegisterDetailresponse.getFirstResult();
                } else {
                    throw new ProcessingStatusException(
                        StatusCode.FATAL,
                        String.format("The accessRegisterDetail for the ingest Opi: %s is EMPTY!", ingestOperationId)
                    );
                }
            }

            throw new ProcessingStatusException(
                StatusCode.FATAL,
                String.format("No accessRegisterDetail available for the ingest Opi: %s", ingestOperationId)
            );
        } catch (ReferentialException | InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not check existing accessing register", e);
        }
    }

    private List<DeleteGotVersionsComputedDetails> getDeletedObjectGroups(String operationId)
        throws ProcessingStatusException {
        try (BatchReportClient client = batchReportClientFactory.getClient()) {
            JsonNode resultsNode = client.readComputedDetailsFromReport(ReportType.DELETE_GOT_VERSIONS, operationId);
            if (resultsNode.isEmpty()) {
                return null;
            }
            return getFromJsonNode(resultsNode, new TypeReference<>() {});
        } catch (Exception e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not find entries from report!", e);
        }
    }
}
