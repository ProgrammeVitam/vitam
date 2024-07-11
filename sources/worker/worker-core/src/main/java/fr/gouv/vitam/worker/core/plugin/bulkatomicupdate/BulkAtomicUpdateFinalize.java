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

package fr.gouv.vitam.worker.core.plugin.bulkatomicupdate;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.core.plugin.UpdateUnitFinalize;

import java.util.Map;
import java.util.Optional;

import static fr.gouv.vitam.batch.report.model.ReportType.BULK_UPDATE_UNIT;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.worker.core.plugin.bulkatomicupdate.BulkAtomicUpdateProcess.BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME;
import static fr.gouv.vitam.worker.core.plugin.bulkatomicupdate.PrepareBulkAtomicUpdate.PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME;

public class BulkAtomicUpdateFinalize extends UpdateUnitFinalize {

    private static final String BULK_ATOMIC_UPDATE_FINALIZE_PLUGIN_NAME = "BULK_ATOMIC_UPDATE_FINALIZE";

    public BulkAtomicUpdateFinalize() {
        this(
            BatchReportClientFactory.getInstance(),
            LogbookOperationsClientFactory.getInstance(),
            StorageClientFactory.getInstance()
        );
    }

    @VisibleForTesting
    private BulkAtomicUpdateFinalize(
        BatchReportClientFactory batchReportClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        StorageClientFactory storageClientFactory
    ) {
        super(batchReportClientFactory, logbookOperationsClientFactory, storageClientFactory);
    }

    @Override
    protected ReportSummary getReport(LogbookOperation logbook) {
        Optional<LogbookEventOperation> logbookEventPrepare = logbook
            .getEvents()
            .stream()
            .filter(e -> e.getEvType().startsWith(PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST_PLUGIN_NAME))
            .reduce((a, b) -> b);
        Optional<LogbookEventOperation> logbookEventUpdate = logbook
            .getEvents()
            .stream()
            .filter(e -> e.getEvType().startsWith(BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME))
            .reduce((a, b) -> b);

        String startDate = logbook.getEvDateTime();
        String endDate = LocalDateUtil.getString(LocalDateUtil.now());

        if (logbookEventPrepare.isEmpty()) {
            return new ReportSummary(startDate, endDate, BULK_UPDATE_UNIT, null, null);
        }

        Map<StatusCode, Integer> codesNumberPrepare = getStatusStatistic(logbookEventPrepare.get());
        int nbOk = 0;
        int nbKo = codesNumberPrepare.get(KO) == null ? 0 : codesNumberPrepare.get(KO);
        int nbWarning = codesNumberPrepare.get(WARNING) == null ? 0 : codesNumberPrepare.get(WARNING);

        if (logbookEventUpdate.isEmpty()) {
            ReportResults results = new ReportResults(nbOk, nbKo, nbWarning);
            return new ReportSummary(startDate, endDate, BULK_UPDATE_UNIT, results, null);
        }

        Map<StatusCode, Integer> codesNumber = getStatusStatistic(logbookEventUpdate.get());
        nbOk = codesNumber.get(OK) == null ? nbOk : nbOk + codesNumber.get(OK);
        nbKo = codesNumber.get(KO) == null ? nbKo : nbKo + codesNumber.get(KO);
        nbWarning = codesNumber.get(WARNING) == null ? nbWarning : nbWarning + codesNumber.get(WARNING);

        ReportResults results = new ReportResults(nbOk, nbKo, nbWarning);
        return new ReportSummary(startDate, endDate, BULK_UPDATE_UNIT, results, null);
    }

    @Override
    protected String getPluginId() {
        return BULK_ATOMIC_UPDATE_FINALIZE_PLUGIN_NAME;
    }

    @Override
    protected String getUpdateType() {
        return "BulkAtomicUpdate";
    }

    @Override
    protected String getUpdateActionKey() {
        return BULK_ATOMIC_UPDATE_UNITS_PLUGIN_NAME;
    }

    @Override
    protected ReportType getReportType() {
        return BULK_UPDATE_UNIT;
    }
}
