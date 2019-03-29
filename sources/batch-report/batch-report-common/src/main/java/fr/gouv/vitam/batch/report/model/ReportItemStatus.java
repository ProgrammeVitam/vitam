package fr.gouv.vitam.batch.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Item status for a report item
 *
 */
public class ReportItemStatus {

    @JsonProperty("id")
    private String id;
    @JsonProperty("status")
    private ReportStatus status;

    public ReportItemStatus() {

    }

    public ReportItemStatus(String id, ReportStatus status) {
        super();
        this.id = id;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

}
