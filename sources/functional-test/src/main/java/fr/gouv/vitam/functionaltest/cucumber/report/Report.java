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
package fr.gouv.vitam.functionaltest.cucumber.report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * cucumber report for an individual test
 */
public class Report {

    /**
     * name of the feature
     */
    private String feature;

    /**
     * id operation
     */
    private String operationId;

    /**
     * type of operation (i.e. ingest)
     */
    private String type;

    /**
     * description of the test
     */
    private String description;

    /**
     * list of tags
     */
    private List<String> tags = new ArrayList<>();

    /**
     * list of errors
     */
    private List<String> errors = new ArrayList<>();

    /**
     * Start time
     */
    private LocalDateTime start;

    /**
     * End time
     */
    private LocalDateTime end;

    /**
     * @return name of the feature
     */
    public String getFeature() {
        return feature;
    }

    /**
     * @param feature name of the feature
     */
    public void setFeature(String feature) {
        this.feature = feature;
    }

    /**
     * @return id operation
     */
    public String getOperationId() {
        return operationId;
    }

    /**
     * @param operationId id operation
     */
    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    /**
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return true if the error list is empty, false otherwise
     */
    public boolean isOK() {
        return getErrors().size() == 0;
    }

    /**
     * @return list of tags
     */
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * @return list of errors
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * @param error add error to the errors list
     */
    public void addError(String error) {
        errors.add(error);
    }

    /**
     * @param tagName add tagName to the Tags list
     */
    public void addTag(String tagName) {
        tags.add(tagName);
    }

    /**
     * @return the start
     */
    public LocalDateTime getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    /**
     * @return the end
     */
    public LocalDateTime getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     */
    public void setEnd(LocalDateTime end) {
        this.end = end;
    }
}
