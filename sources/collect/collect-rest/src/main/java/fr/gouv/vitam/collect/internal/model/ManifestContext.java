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
package fr.gouv.vitam.collect.internal.model;

public class ManifestContext {

    private String archivalAgreement;

    private String messageIdentifier;

    private String archivalAgencyIdentifier;

    private String transferingAgencyIdentifier;

    private String originatingAgencyIdentifier;

    private String submissionAgencyIdentifier;

    private String archivalProfile;

    private String comment;

    private String unitUp;


    public ManifestContext() {
    }

    public ManifestContext(String archivalAgreement, String messageIdentifier, String archivalAgencyIdentifier,
        String transferingAgencyIdentifier, String originatingAgencyIdentifier, String submissionAgencyIdentifier,
        String archivalProfile, String comment, String unitUp) {
        this.archivalAgreement = archivalAgreement;
        this.messageIdentifier = messageIdentifier;
        this.archivalAgencyIdentifier = archivalAgencyIdentifier;
        this.transferingAgencyIdentifier = transferingAgencyIdentifier;
        this.originatingAgencyIdentifier = originatingAgencyIdentifier;
        this.submissionAgencyIdentifier = submissionAgencyIdentifier;
        this.archivalProfile = archivalProfile;
        this.comment = comment;
        this.unitUp = unitUp;
    }

    public String getArchivalAgreement() {
        return archivalAgreement;
    }

    public void setArchivalAgreement(String archivalAgreement) {
        this.archivalAgreement = archivalAgreement;
    }

    public String getMessageIdentifier() {
        return messageIdentifier;
    }

    public void setMessageIdentifier(String messageIdentifier) {
        this.messageIdentifier = messageIdentifier;
    }

    public String getArchivalAgencyIdentifier() {
        return archivalAgencyIdentifier;
    }

    public void setArchivalAgencyIdentifier(String archivalAgencyIdentifier) {
        this.archivalAgencyIdentifier = archivalAgencyIdentifier;
    }

    public String getTransferingAgencyIdentifier() {
        return transferingAgencyIdentifier;
    }

    public void setTransferingAgencyIdentifier(String transferingAgencyIdentifier) {
        this.transferingAgencyIdentifier = transferingAgencyIdentifier;
    }

    public String getOriginatingAgencyIdentifier() {
        return originatingAgencyIdentifier;
    }

    public void setOriginatingAgencyIdentifier(String originatingAgencyIdentifier) {
        this.originatingAgencyIdentifier = originatingAgencyIdentifier;
    }

    public String getSubmissionAgencyIdentifier() {
        return submissionAgencyIdentifier;
    }

    public void setSubmissionAgencyIdentifier(String submissionAgencyIdentifier) {
        this.submissionAgencyIdentifier = submissionAgencyIdentifier;
    }

    public String getArchivalProfile() {
        return archivalProfile;
    }

    public void setArchivalProfile(String archivalProfile) {
        this.archivalProfile = archivalProfile;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getUnitUp() {
        return unitUp;
    }

    public void setUnitUp(String unitUp) {
        this.unitUp = unitUp;
    }
}
