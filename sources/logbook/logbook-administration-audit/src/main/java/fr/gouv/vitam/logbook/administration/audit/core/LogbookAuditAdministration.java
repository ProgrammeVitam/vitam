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
package fr.gouv.vitam.logbook.administration.audit.core;

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
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;

/**
 * Business class for Logbook traceability audit
 */
public class LogbookAuditAdministration {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookAuditAdministration.class);
    private final LogbookOperations logbookOperations;
    private final AlertService alertService;

    public LogbookAuditAdministration(LogbookOperations logbookOperations) {
        this(logbookOperations, new AlertServiceImpl());
    }

    public LogbookAuditAdministration(LogbookOperations logbookOperations, AlertService alertService) {
        this.logbookOperations = logbookOperations;
        this.alertService = alertService;
    }

    /**
     * Check existence of at least 1 traceability logbook operation in last time period.
     *
     * @param type process type
     * @param amount time amount
     * @param unit time unit
     * @return nb operations found
     * @throws LogbookAuditException Internal check error
     */
    public int auditTraceability(String type, int amount, ChronoUnit unit) throws LogbookAuditException {
        Select selectQuery = new Select();

        try {
            LocalDateTime startDateTime = LocalDateUtil.now().minus(amount, unit);

            selectQuery.setQuery(
                and()
                    .add(
                        QueryHelper.eq(
                            LogbookMongoDbName.eventTypeProcess.getDbname(),
                            LogbookTypeProcess.TRACEABILITY.name()
                        ),
                        QueryHelper.eq(LogbookMongoDbName.eventType.getDbname(), type),
                        QueryHelper.gte(
                            LogbookMongoDbName.eventDateTime.getDbname(),
                            LocalDateUtil.getFormattedDateTimeForMongo(startDateTime)
                        )
                    )
            );
            List<LogbookOperation> ops = logbookOperations.selectOperations(selectQuery.getFinalSelect());
            int nbLog = ops.size();

            if (nbLog == 0) {
                String error = String.format(
                    "No %s traceability found for tenant %d in the last %d %s",
                    type,
                    VitamThreadUtils.getVitamSession().getTenantId(),
                    amount,
                    unit
                );
                LOGGER.error(error);
                alertService.createAlert(error);
            }
            return nbLog;
        } catch (
            LogbookDatabaseException
            | InvalidParseOperationException
            | InvalidCreateOperationException
            | VitamDBException e
        ) {
            throw new LogbookAuditException(e.getMessage());
        }
    }
}
