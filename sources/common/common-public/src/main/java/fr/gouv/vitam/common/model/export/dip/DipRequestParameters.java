/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.export.dip;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DipRequestParameters {

    public static final String REQUESTER_IDENTIFIER = "requesterIdentifier";
    public static final String ARCHIVAL_AGENCY_IDENTIFIER = "archivalAgencyIdentifier";
    public static final String AUTHORIZATION_REQUEST_REPLY_IDENTIFIER = "authorizationRequestReplyIdentifier";
    public static final String MESSAGE_REQUEST_IDENTIFIER = "messageRequestIdentifier";
    public static final String SUBMISSION_AGENCY_IDENTIFIER = "submissionAgencyIdentifier";
    public static final String ORIGINATING_AGENCY_IDENTIFIER = "originatingAgencyIdentifier";
    public static final String ARCHIVAL_AGREEMENT = "archivalAgreement";
    public static final String COMMENT = "comment";

    @JsonProperty(ARCHIVAL_AGREEMENT)
    private String archivalAgreement;

    @JsonProperty(ORIGINATING_AGENCY_IDENTIFIER)
    private String originatingAgencyIdentifier;

    @JsonProperty(COMMENT)
    private String comment;

    @JsonProperty(SUBMISSION_AGENCY_IDENTIFIER)
    private String submissionAgencyIdentifier;

    @JsonProperty(MESSAGE_REQUEST_IDENTIFIER)
    private String messageRequestIdentifier; // Required

    @JsonProperty(ARCHIVAL_AGENCY_IDENTIFIER)
    private String archivalAgencyIdentifier; // Required

    @JsonProperty(REQUESTER_IDENTIFIER)
    private String requesterIdentifier; // Required

    @JsonProperty(AUTHORIZATION_REQUEST_REPLY_IDENTIFIER)
    private String authorizationRequestReplyIdentifier;

    public String getArchivalAgreement() {
        return archivalAgreement;
    }

    public void setArchivalAgreement(String archivalAgreement) {
        this.archivalAgreement = archivalAgreement;
    }

    public String getOriginatingAgencyIdentifier() {
        return originatingAgencyIdentifier;
    }

    public void setOriginatingAgencyIdentifier(String originatingAgencyIdentifier) {
        this.originatingAgencyIdentifier = originatingAgencyIdentifier;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSubmissionAgencyIdentifier() {
        return submissionAgencyIdentifier;
    }

    public void setSubmissionAgencyIdentifier(String submissionAgencyIdentifier) {
        this.submissionAgencyIdentifier = submissionAgencyIdentifier;
    }

    public String getMessageRequestIdentifier() {
        return messageRequestIdentifier;
    }

    public void setMessageRequestIdentifier(String messageRequestIdentifier) {
        this.messageRequestIdentifier = messageRequestIdentifier;
    }

    public String getAuthorizationRequestReplyIdentifier() {
        return authorizationRequestReplyIdentifier;
    }

    public void setAuthorizationRequestReplyIdentifier(String authorizationRequestReplyIdentifier) {
        this.authorizationRequestReplyIdentifier = authorizationRequestReplyIdentifier;
    }

    public String getArchivalAgencyIdentifier() {
        return archivalAgencyIdentifier;
    }

    public void setArchivalAgencyIdentifier(String archivalAgencyIdentifier) {
        this.archivalAgencyIdentifier = archivalAgencyIdentifier;
    }

    public String getRequesterIdentifier() {
        return requesterIdentifier;
    }

    public void setRequesterIdentifier(String requesterIdentifier) {
        this.requesterIdentifier = requesterIdentifier;
    }
}
