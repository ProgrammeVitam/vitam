/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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

package fr.gouv.vitam.logbook.administration.core;

import java.io.File;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.traceability.TraceabilityService;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;

/**
 * Business class for Logbook Administration (traceability)
 */
public class LogbookAdministration {

    private final LogbookOperations logbookOperations;
    private final TimestampGenerator timestampGenerator;

    private final File tmpFolder;
    private final int operationTraceabilityTemporizationDelayInSeconds;

    @VisibleForTesting //
    LogbookAdministration(LogbookOperations logbookOperations,
        TimestampGenerator timestampGenerator, File tmpFolder,
        Integer operationTraceabilityTemporizationDelay) {
        this.logbookOperations = logbookOperations;
        this.timestampGenerator = timestampGenerator;
        this.tmpFolder = tmpFolder;
        this.operationTraceabilityTemporizationDelayInSeconds =
            validateAndGetTraceabilityTemporizationDelay(operationTraceabilityTemporizationDelay);
    }

    private static int validateAndGetTraceabilityTemporizationDelay(Integer operationTraceabilityTemporizationDelay) {
        if (operationTraceabilityTemporizationDelay == null) {
             return 0;
        }
        if (operationTraceabilityTemporizationDelay < 0) {
            throw new IllegalArgumentException("Operation traceability temporization delay cannot be negative");
        }
        return operationTraceabilityTemporizationDelay;
    }

    /**
     * Constructor
     *
     * @param logbookOperations                 logbook operation
     * @param operationTraceabilityOverlapDelay
     */
    public LogbookAdministration(LogbookOperations logbookOperations, TimestampGenerator timestampGenerator,
        Integer operationTraceabilityOverlapDelay) {
        this(logbookOperations, timestampGenerator,
            PropertiesUtils.fileFromTmpFolder("secure"), operationTraceabilityOverlapDelay);
    }
    /**
     * secure the logbook operation since last securisation.
     *
     * @return the GUID of the operation
     * @throws TraceabilityException if error on generating secure logbook
     */
    // TODO: use a distributed lock to launch this function only on one server (cf consul)
    public synchronized void generateSecureLogbook(GUID guid)
        throws TraceabilityException {

        Integer tenantId = ParameterHelper.getTenantParameter();

        LogbookOperationTraceabilityHelper helper =
            new LogbookOperationTraceabilityHelper(logbookOperations, guid,
                operationTraceabilityTemporizationDelayInSeconds);

        TraceabilityService generator =
            new TraceabilityService(timestampGenerator, helper, tenantId, tmpFolder);

        generator.secureData(VitamConfiguration.getDefaultStrategy());
    }
}
