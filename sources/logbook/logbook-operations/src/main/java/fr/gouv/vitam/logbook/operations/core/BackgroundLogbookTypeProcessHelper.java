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

package fr.gouv.vitam.logbook.operations.core;

import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class BackgroundLogbookTypeProcessHelper {

    private BackgroundLogbookTypeProcessHelper() {
        // No constructor for static class
    }

    public static List<LogbookTypeProcess> getBackgroundLogbookTypeProcesses() {
        return Arrays.stream(LogbookTypeProcess.values())
            .filter(BackgroundLogbookTypeProcessHelper::isBackgroundProcess)
            .collect(Collectors.toList());
    }

    private static boolean isBackgroundProcess(LogbookTypeProcess logbookTypeProcess) {
        switch (logbookTypeProcess) {
            case TRACEABILITY:
            case STORAGE_BACKUP:
            case COMPUTE_INHERITED_RULES:
            case DATA_CONSISTENCY_AUDIT:
                return true;
            case INGEST:
            case INGEST_CLEANUP:
            case AUDIT:
            case DESTRUCTION:
            case PRESERVATION:
            case CHECK:
            case UPDATE:
            case MASTERDATA:
            case INGEST_TEST:
            case STORAGE_LOGBOOK:
            case STORAGE_RULE:
            case STORAGE_AGENCIES:
            case HOLDINGSCHEME:
            case FILINGSCHEME:
            case EXPORT_DIP:
            case ARCHIVE_TRANSFER:
            case DATA_MIGRATION:
            case RECLASSIFICATION:
            case MASS_UPDATE:
            case BULK_UPDATE:
            case ELIMINATION:
            case EXPORT_PROBATIVE_VALUE:
            case EXTERNAL_LOGBOOK:
            case TRANSFER_REPLY:
            case COMPUTE_INHERITED_RULES_DELETE:
            case INTERNAL_OPERATING_OP:
            case DELETE_GOT_VERSIONS:
                return false;
            default:
                // /:\ Please ensure that any new LogbookTypeProcess is identified properly configured
                throw new IllegalStateException("Unexpected value: " + logbookTypeProcess);
        }
    }
}
