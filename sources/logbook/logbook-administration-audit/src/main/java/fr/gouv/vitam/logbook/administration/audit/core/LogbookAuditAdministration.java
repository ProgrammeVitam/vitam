package fr.gouv.vitam.logbook.administration.audit.core;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

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
    private static final AlertService alertService = new AlertServiceImpl();

    public LogbookAuditAdministration(LogbookOperations logbookOperations) {
        this.logbookOperations = logbookOperations;
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
