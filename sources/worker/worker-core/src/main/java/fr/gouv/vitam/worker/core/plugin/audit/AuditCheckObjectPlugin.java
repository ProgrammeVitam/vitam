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
package fr.gouv.vitam.worker.core.plugin.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.model.ReportItemStatus;
import fr.gouv.vitam.batch.report.model.ReportStatus;
import fr.gouv.vitam.batch.report.model.entry.AuditObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.AuditObjectVersion;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditCheckObjectGroupResult;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditCheckObjectResult;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditObject;
import fr.gouv.vitam.worker.core.plugin.audit.model.AuditObjectGroup;
import org.apache.commons.collections4.IterableUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class AuditCheckObjectPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AuditCheckObjectPlugin.class);
    public static final String AUDIT_CHECK_OBJECT = "AUDIT_CHECK_OBJECT";
    private static final int STRATEGIES_IN_RANK = 0;
    private final AuditExistenceService auditExistenceService;
    private final AuditIntegrityService auditIntegrityService;
    private final AuditReportService auditReportService;

    public AuditCheckObjectPlugin() {
        this(new AuditExistenceService(), new AuditIntegrityService(), new AuditReportService());
    }

    @VisibleForTesting
    AuditCheckObjectPlugin(
        AuditExistenceService auditExistenceService,
        AuditIntegrityService auditIntegrityService,
        AuditReportService auditReportService
    ) {
        this.auditExistenceService = auditExistenceService;
        this.auditIntegrityService = auditIntegrityService;
        this.auditReportService = auditReportService;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        LOGGER.debug("Starting audit");

        try {
            return executeAudit(param, handler);
        } catch (ProcessingStatusException e) {
            LOGGER.error(String.format("Audit action failed with status [%s]", e.getStatusCode()), e);
            return buildItemStatus(AUDIT_CHECK_OBJECT, e.getStatusCode(), null);
        }
    }

    private ItemStatus executeAudit(WorkerParameters param, HandlerIO handler) throws ProcessingStatusException {
        final ItemStatus itemStatus = new ItemStatus(AUDIT_CHECK_OBJECT);
        Map<WorkerParameterName, String> mapParameters = param.getMapParameters();
        String action = mapParameters.get(WorkerParameterName.auditActions);
        AuditObjectGroup gotDetail = loadAuditLine(param);
        List<StorageStrategy> storageStrategies = loadStorageStrategies(handler);
        String actionType = null;
        AuditCheckObjectGroupResult result = null;

        if (AuditExistenceService.CHECK_EXISTENCE_ID.equals(action)) {
            result = auditExistenceService.check(gotDetail, storageStrategies);
            actionType = AuditExistenceService.CHECK_EXISTENCE_ID;
        } else if (AuditIntegrityService.CHECK_INTEGRITY_ID.equals(action)) {
            result = auditIntegrityService.check(gotDetail, storageStrategies);
            actionType = AuditIntegrityService.CHECK_INTEGRITY_ID;
        }
        addReportEntry(param.getContainerName(), createAuditObjectGroupReportEntry(gotDetail, result, actionType));
        itemStatus.setItemsStatus(actionType, buildItemStatus(actionType, result.getStatus(), null));
        if (actionType != null && itemStatus.getGlobalStatus().isGreaterOrEqualToKo()) {
            itemStatus.setGlobalOutcomeDetailSubcode(actionType);
        }

        return new ItemStatus(AUDIT_CHECK_OBJECT).setItemsStatus(AUDIT_CHECK_OBJECT, itemStatus);
    }

    private AuditObjectGroup loadAuditLine(WorkerParameters param) throws ProcessingStatusException {
        AuditObjectGroup auditDistributionLine;
        try {
            auditDistributionLine = JsonHandler.getFromJsonNode(param.getObjectMetadata(), AuditObjectGroup.class);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not load audit object group data", e);
        }
        return auditDistributionLine;
    }

    private List<StorageStrategy> loadStorageStrategies(HandlerIO handler) throws ProcessingStatusException {
        try {
            return JsonHandler.getFromFileAsTypeReference(
                (File) handler.getInput(STRATEGIES_IN_RANK),
                new TypeReference<>() {}
            );
        } catch (InvalidParseOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not load storage strategies datas", e);
        }
    }

    private AuditObjectGroupReportEntry createAuditObjectGroupReportEntry(
        AuditObjectGroup gotDetail,
        AuditCheckObjectGroupResult result,
        String outcome
    ) {
        AuditObjectGroupReportEntry auditObjectGroupReportEntry = new AuditObjectGroupReportEntry(
            gotDetail.getId(),
            gotDetail.getUnitUps(),
            gotDetail.getSp(),
            gotDetail.getOpi(),
            new ArrayList<AuditObjectVersion>(),
            ReportStatus.parseFromStatusCode(result.getStatus()),
            outcome
        );

        for (AuditCheckObjectResult objectResult : result.getObjectStatuses()) {
            AuditObject auditObject = IterableUtils.find(
                gotDetail.getObjects(),
                object -> object.getId().equals(objectResult.getIdObject())
            );

            String strategyId = null;
            if (auditObject.getStorage() != null) {
                strategyId = auditObject.getStorage().getStrategyId();
            }
            AuditObjectVersion objectVersion = new AuditObjectVersion(
                auditObject.getId(),
                auditObject.getOpi(),
                auditObject.getQualifier(),
                auditObject.getVersion(),
                strategyId,
                (List<ReportItemStatus>) objectResult
                    .getOfferStatuses()
                    .entrySet()
                    .stream()
                    .map(e -> new ReportItemStatus(e.getKey(), ReportStatus.parseFromStatusCode(e.getValue())))
                    .collect(Collectors.toList()),
                ReportStatus.parseFromStatusCode(objectResult.getGlobalStatus())
            );

            auditObjectGroupReportEntry.getObjectVersions().add(objectVersion);
        }
        return auditObjectGroupReportEntry;
    }

    private void addReportEntry(String processId, AuditObjectGroupReportEntry entry) throws ProcessingStatusException {
        auditReportService.appendEntries(processId, Arrays.asList(entry));
    }
}
