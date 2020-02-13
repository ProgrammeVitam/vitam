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
package fr.gouv.vitam.logbook.administration.audit.core;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.administration.audit.exception.LogbookAuditException;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;

/**
 * Business class for Logbook traceability audit
 */
public class LogbookAuditAdministration {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookAuditAdministration.class);
    private final LogbookOperations logbookOperations;
    private final AlertService alertService;

    /**
     * Constructor
     */
    public LogbookAuditAdministration(LogbookOperations logbookOperations) {
        this(logbookOperations, new AlertServiceImpl());
    }

    /**
     * Constructor for testing
     */
    @VisibleForTesting
    LogbookAuditAdministration(LogbookOperations logbookOperations, AlertService alertService) {
        this.logbookOperations = logbookOperations;
        this.alertService = alertService;
    }

    /**
     * Check whether the number of the traceability logbook is as expected
     *
     * @param type
     * @param nbDay
     * @return
     * @throws InvalidParseOperationException
     * @throws LogbookDatabaseException
     * @throws InvalidCreateOperationException
     */
    public int auditTraceability(String type, int nbDay, int times) throws LogbookAuditException {
        int nbLog;
        int nbLogExcepted = nbDay * times;
        Select selectQuery = new Select();

        try {
            Date now = new Date();
            Calendar cal = new GregorianCalendar();
            cal.setTime(now);
            cal.add(Calendar.DAY_OF_MONTH, -nbDay);
            Date before = cal.getTime();

            selectQuery.setQuery(
                    and().add(QueryHelper.eq(LogbookMongoDbName.eventTypeProcess.getDbname(), "TRACEABILITY"),
                            QueryHelper.eq(LogbookMongoDbName.eventType.getDbname(), type),
                            QueryHelper.gte(LogbookMongoDbName.eventDateTime.getDbname(), LocalDateUtil.getString(before)),
                            QueryHelper.lte(LogbookMongoDbName.eventDateTime.getDbname(), LocalDateUtil.getString(now))
                    ));
            List<LogbookOperation> ops = logbookOperations.select(selectQuery.getFinalSelect());
            nbLog = ops.size();

        } catch (LogbookNotFoundException e) {
            nbLog = 0;
        } catch (LogbookDatabaseException | InvalidParseOperationException | InvalidCreateOperationException | VitamDBException e) {
            throw new LogbookAuditException(e.getMessage());
        }

        if (nbLog != nbLogExcepted) {
            LOGGER.error("We just find" + nbLog + " traceability logbook(s) of tenant " +
                            VitamThreadUtils.getVitamSession().getTenantId());
            alertService.createAlert("There are missing traceability logbooks of tenant " +
                    VitamThreadUtils.getVitamSession().getTenantId() + " for " + nbDay + " days");
            return nbLog;
        }
        return nbLog;
    }
}
