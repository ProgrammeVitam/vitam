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
package fr.gouv.vitam.common.model.export;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.export.dip.DipRequestParameters;
import fr.gouv.vitam.common.model.export.transfer.TransferRequestParameters;

import java.util.List;

public class ExportRequestParameters {

    public static final String TRANSFERRING_AGENCY = "transferringAgency";
    public static final String REQUESTER_IDENTIFIER = "requesterIdentifier";
    public static final String ARCHIVAL_AGENCY_IDENTIFIER = "archivalAgencyIdentifier";
    public static final String AUTHORIZATION_REQUEST_REPLY_IDENTIFIER = "authorizationRequestReplyIdentifier";
    public static final String TRANSFER_REQUEST_REPLY_IDENTIFIER = "transferRequestReplyIdentifier";
    public static final String RELATED_TRANSFER_REFERENCE = "relatedTransferReference";
    public static final String MESSAGE_REQUEST_IDENTIFIER = "messageRequestIdentifier";
    public static final String SUBMISSION_AGENCY_IDENTIFIER = "submissionAgencyIdentifier";
    public static final String ORIGINATING_AGENCY_IDENTIFIER = "originatingAgencyIdentifier";
    public static final String ARCHIVAL_AGREEMENT = "archivalAgreement";
    public static final String COMMENT = "comment";

    @JsonProperty(COMMENT)
    private String comment;

    @JsonProperty(ARCHIVAL_AGREEMENT)
    private String archivalAgreement; // Cardinality (1-1) for ArchiveTransfer optional for ArchiveDeliveryRequestReply

    @JsonProperty(ORIGINATING_AGENCY_IDENTIFIER)
    private String originatingAgencyIdentifier; // Cardinality (1-1) for ArchiveTransfer must be given from query

    @JsonProperty(SUBMISSION_AGENCY_IDENTIFIER)
    private String submissionAgencyIdentifier;

    @JsonProperty(MESSAGE_REQUEST_IDENTIFIER)
    private String messageRequestIdentifier; // ArchiveDeliveryRequestReply only Cardinality (1-1)

    @JsonProperty(RELATED_TRANSFER_REFERENCE)
    private List<String> relatedTransferReference; // ArchiveTransfer only

    @JsonProperty(TRANSFER_REQUEST_REPLY_IDENTIFIER)
    private String transferRequestReplyIdentifier; // ArchiveTransfer only

    @JsonProperty(AUTHORIZATION_REQUEST_REPLY_IDENTIFIER)
    private String authorizationRequestReplyIdentifier; // ArchiveDeliveryRequestReply only

    @JsonProperty(ARCHIVAL_AGENCY_IDENTIFIER)
    private String archivalAgencyIdentifier; // Cardinality (1-1)

    @JsonProperty(REQUESTER_IDENTIFIER)
    private String requesterIdentifier; // ArchiveDeliveryRequestReply only Cardinality (1-1)

    @JsonProperty(TRANSFERRING_AGENCY)
    private String transferringAgency; // ArchiveTransfer only

    public ExportRequestParameters() {
        //Empty
    }

    // Dip Constructor
    public ExportRequestParameters(
        String archivalAgreement,
        String originatingAgencyIdentifier,
        String comment,
        String submissionAgencyIdentifier,
        String messageRequestIdentifier,
        String authorizationRequestReplyIdentifier,
        String archivalAgencyIdentifier,
        String requesterIdentifier
    ) {
        this.archivalAgreement = archivalAgreement;
        this.originatingAgencyIdentifier = originatingAgencyIdentifier;
        this.comment = comment;
        this.submissionAgencyIdentifier = submissionAgencyIdentifier;
        this.messageRequestIdentifier = messageRequestIdentifier;
        this.authorizationRequestReplyIdentifier = authorizationRequestReplyIdentifier;
        this.archivalAgencyIdentifier = archivalAgencyIdentifier;
        this.requesterIdentifier = requesterIdentifier;
    }

    // Transfer Constructor
    public ExportRequestParameters(
        String archivalAgreement,
        String originatingAgencyIdentifier,
        String comment,
        String submissionAgencyIdentifier,
        List<String> relatedTransferReference,
        String transferRequestReplyIdentifier,
        String archivalAgencyIdentifier,
        String transferringAgency
    ) {
        this.archivalAgreement = archivalAgreement;
        this.originatingAgencyIdentifier = originatingAgencyIdentifier;
        this.comment = comment;
        this.submissionAgencyIdentifier = submissionAgencyIdentifier;
        this.relatedTransferReference = relatedTransferReference;
        this.transferRequestReplyIdentifier = transferRequestReplyIdentifier;
        this.archivalAgencyIdentifier = archivalAgencyIdentifier;
        this.transferringAgency = transferringAgency;
    }

    public static ExportRequestParameters from(DipRequestParameters dipRequestParameters) {
        if (null == dipRequestParameters) {
            return null;
        }

        return new ExportRequestParameters(
            dipRequestParameters.getArchivalAgreement(),
            dipRequestParameters.getOriginatingAgencyIdentifier(),
            dipRequestParameters.getComment(),
            dipRequestParameters.getSubmissionAgencyIdentifier(),
            dipRequestParameters.getMessageRequestIdentifier(),
            dipRequestParameters.getAuthorizationRequestReplyIdentifier(),
            dipRequestParameters.getArchivalAgencyIdentifier(),
            dipRequestParameters.getRequesterIdentifier()
        );
    }

    public static ExportRequestParameters from(TransferRequestParameters transferRequestParameters) {
        return new ExportRequestParameters(
            transferRequestParameters.getArchivalAgreement(),
            transferRequestParameters.getOriginatingAgencyIdentifier(),
            transferRequestParameters.getComment(),
            transferRequestParameters.getSubmissionAgencyIdentifier(),
            transferRequestParameters.getRelatedTransferReference(),
            transferRequestParameters.getTransferRequestReplyIdentifier(),
            transferRequestParameters.getArchivalAgencyIdentifier(),
            transferRequestParameters.getTransferringAgency()
        );
    }

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

    public List<String> getRelatedTransferReference() {
        return relatedTransferReference;
    }

    public void setRelatedTransferReference(List<String> relatedTransferReference) {
        this.relatedTransferReference = relatedTransferReference;
    }

    public String getTransferRequestReplyIdentifier() {
        return transferRequestReplyIdentifier;
    }

    public void setTransferRequestReplyIdentifier(String transferRequestReplyIdentifier) {
        this.transferRequestReplyIdentifier = transferRequestReplyIdentifier;
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

    public String getTransferringAgency() {
        return transferringAgency;
    }

    public void setTransferringAgency(String transferringAgency) {
        this.transferringAgency = transferringAgency;
    }
}
