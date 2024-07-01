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
package fr.gouv.vitam.common.accesslog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.model.administration.ActivationStatus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class AccessLogUtils {

    public static final String QUALIFIER = "qualifier";
    public static final String VERSION = "version";
    public static final String CONTEXT_ID = "contextId";
    public static final String ARCHIVE_ID = "archiveId";
    public static final String SIZE = "objectSize";
    public static final String FILE_NAME = "FILE_NAME";

    public static JsonNode getWorkerInfo(
        String qualifier,
        Integer version,
        Long size,
        String archiveUnitId,
        String fileName
    ) {
        ObjectNode logInfo = JsonHandler.createObjectNode();

        logInfo.set(QUALIFIER, new TextNode(qualifier));
        logInfo.set(VERSION, new IntNode(version));
        logInfo.set(SIZE, new LongNode(size));
        logInfo.set(ARCHIVE_ID, new TextNode(archiveUnitId));
        logInfo.set(FILE_NAME, new TextNode(fileName));

        return logInfo;
    }

    public static AccessLogInfoModel getInfoFromWorkerInfo(
        Map<String, Object> objectInfo,
        VitamSession session,
        Boolean mustLog
    ) {
        AccessLogInfoModel logInfo = new AccessLogInfoModel();

        logInfo.setMustLog(mustLog);

        if (mustLog) {
            logInfo.setContextId(session.getContextId());
            logInfo.setContractId(session.getContractId());
            logInfo.setRequestId(session.getRequestId());
            logInfo.setApplicationId(session.getApplicationSessionId());

            logInfo.setArchiveId((String) objectInfo.get(ARCHIVE_ID));
            logInfo.setQualifier((String) objectInfo.get(QUALIFIER));
            logInfo.setVersion((Integer) objectInfo.get(VERSION));
            logInfo.setSize(((Number) objectInfo.get(SIZE)).longValue());
        }

        return logInfo;
    }

    public static AccessLogInfoModel getInfoForAccessLog(
        String qualifier,
        Integer version,
        VitamSession session,
        Long size,
        String archiveUnitId
    ) {
        AccessLogInfoModel logInfo = new AccessLogInfoModel();

        boolean mustLog = ActivationStatus.ACTIVE.equals(session.getContract().getAccessLog());
        logInfo.setMustLog(mustLog);

        if (mustLog) {
            logInfo.setContextId(session.getContextId());
            logInfo.setContractId(session.getContractId());
            logInfo.setRequestId(session.getRequestId());
            logInfo.setApplicationId(session.getApplicationSessionId());

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
