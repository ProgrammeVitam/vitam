package fr.gouv.vitam.storage.engine.server.accesslogger;

import java.util.Date;

public class AccessLogStructure {

    private final String applicativeContextId;
    private final String accessContractId;
    private final String xRequestId; /* Useless because writable in log ? */
    private final String ingestId; /* Kezako ? */
    private final String objectId;
    private final String archiveUnitId; /* is it needed ? Is it mandatory ? */
    private final String objectType; /* container ? */
    private final Long objectSize;
    private final Date responseDate;

    public AccessLogStructure(String applicativeContextId, String accessContractId, String xRequestId,
      String objectId, Long objectSize, Date responseDate, String objectType, String ingestId, String archiveUnitId) {
      this.applicativeContextId = applicativeContextId;
      this.accessContractId = accessContractId;
      this.xRequestId = xRequestId;
      this.ingestId = ingestId;
      this.objectId = objectId;
      this.objectType = objectType;
      this.objectSize = objectSize;
      this.responseDate = responseDate;
      this.archiveUnitId = archiveUnitId;
    }

    public String getApplicativeContextId() {
        return applicativeContextId;
    }

    public String getAccessContractId() {
        return accessContractId;
    }

    public String getxRequestId() {
        return xRequestId;
    }

    public String getIngestId() {
        return ingestId;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getObjectType() {
        return objectType;
    }

    public Long getObjectSize() {
        return objectSize;
    }

    public Date getResponseDate() {
        return responseDate;
    }

    public String getArchiveUnitId() {
        return archiveUnitId;
    }
}
