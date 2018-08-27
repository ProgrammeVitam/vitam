package fr.gouv.vitam.common.accesslog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.model.administration.ActivationStatus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static javax.xml.datatype.DatatypeFactory.newInstance;

public class AccessLogUtils {

    public static final String QUALIFIER = "qualifier";
    public static final String VERSION = "version";
    public static final String CONTEXT_ID = "contextId";
    public static final String ARCHIVE_ID = "archiveId";
    public static final String SIZE = "objectSize";
    public static final String FILE_NAME = "FILE_NAME";


    public static JsonNode getWorkerInfo(String qualifier, Integer version, Integer size, String archiveUnitId,
        String fileName) {
        ObjectNode logInfo = JsonHandler.createObjectNode();

        logInfo.set(QUALIFIER, new TextNode(qualifier));
        logInfo.set(VERSION, new IntNode(version));
        logInfo.set(SIZE, new IntNode(size));
        logInfo.set(ARCHIVE_ID, new TextNode(archiveUnitId));
        logInfo.set(FILE_NAME, new TextNode(fileName));

        return logInfo;
    }

    public static AccessLogInfoModel getInfoFromWorkerInfo(Map<String, Object> objectInfo, VitamSession session, Boolean mustLog) {
        AccessLogInfoModel logInfo = new AccessLogInfoModel();

        logInfo.setMustLog(mustLog);

        if (mustLog) {
            logInfo.setContextId(session.getContextId());
            logInfo.setContractId(session.getContractId());
            logInfo.setRequestId(session.getRequestId());

            logInfo.setArchiveId((String) objectInfo.get(ARCHIVE_ID));
            logInfo.setQualifier((String) objectInfo.get(QUALIFIER));
            logInfo.setVersion((Integer) objectInfo.get(VERSION));
            logInfo.setSize((Integer) objectInfo.get(SIZE));
        }

        return logInfo;
    }

    public static AccessLogInfoModel getInfoForAccessLog(String qualifier, Integer version, VitamSession session,
        Integer size, String archiveUnitId) {
        AccessLogInfoModel logInfo = new AccessLogInfoModel();

        boolean mustLog = ActivationStatus.ACTIVE.equals(session.getContract().getAccessLog());
        logInfo.setMustLog(mustLog);

        if (mustLog) {
            logInfo.setContextId(session.getContextId());
            logInfo.setContractId(session.getContractId());
            logInfo.setRequestId(session.getRequestId());

            logInfo.setArchiveId(archiveUnitId);
            logInfo.setQualifier(qualifier);
            logInfo.setVersion(version);
            logInfo.setSize(size);
        }

        return logInfo;
    }

    public static Boolean checkFileInRequestedDates(String fileName, Date startDate, Date endDate) {
        if (startDate == null && endDate == null) {
            return true;
        }

        final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        String[] fileSplit = fileName.split("_");

        try {
            Date logCreationDate = sdfDate.parse(fileSplit[4]);
            if (endDate != null && endDate.before(logCreationDate)) {
                return false;
            }

            Date logRotationDate = sdfDate.parse(fileSplit[5]);
            if (startDate != null && startDate.after(logRotationDate)) {
                return false;
            }
        } catch (ParseException e) {
            // TODO: Thrown error ? Log Warning ? Return true or false ?
        }

        return true;
    }

    public static AccessLogInfoModel getNoLogAccessLog() {
        AccessLogInfoModel logInfo = new AccessLogInfoModel();
        logInfo.setMustLog(false);
        return logInfo;
    }

    public static Boolean mustLog(AccessLogInfoModel logInfo) {
        if (logInfo == null) {
            // Throw error: No LogInfo (Error or not ?)
            return false; // Nothing to return, Exception to be thrown
        }

        return logInfo.getMustLog();
    }
}
