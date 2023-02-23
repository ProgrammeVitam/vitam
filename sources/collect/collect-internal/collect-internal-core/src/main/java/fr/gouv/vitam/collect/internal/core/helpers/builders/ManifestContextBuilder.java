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
package fr.gouv.vitam.collect.internal.core.helpers.builders;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.gouv.vitam.collect.internal.core.common.ManifestContext;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManifestContextBuilder {
    private String acquisitionInformation;
    private String legalStatus;
    private String archivalAgreement;
    private String messageIdentifier;
    private String archivalAgencyIdentifier;
    private String transferringAgencyIdentifier;
    private String originatingAgencyIdentifier;
    private String submissionAgencyIdentifier;
    private String archivalProfile;
    private String comment;
    private String unitUp;

    public ManifestContextBuilder withArchivalAgreement(String archivalAgreement) {
        this.archivalAgreement = archivalAgreement;
        return this;
    }

    public ManifestContextBuilder withMessageIdentifier(String messageIdentifier) {
        this.messageIdentifier = messageIdentifier;
        return this;
    }

    public ManifestContextBuilder withArchivalAgencyIdentifier(String archivalAgencyIdentifier) {
        this.archivalAgencyIdentifier = archivalAgencyIdentifier;
        return this;
    }

    public ManifestContextBuilder withTransferringAgencyIdentifier(String transferingAgencyIdentifier) {
        this.transferringAgencyIdentifier = transferingAgencyIdentifier;
        return this;
    }

    public ManifestContextBuilder withOriginatingAgencyIdentifier(String originatingAgencyIdentifier) {
        this.originatingAgencyIdentifier = originatingAgencyIdentifier;
        return this;
    }

    public ManifestContextBuilder withSubmissionAgencyIdentifier(String submissionAgencyIdentifier) {
        this.submissionAgencyIdentifier = submissionAgencyIdentifier;
        return this;
    }

    public ManifestContextBuilder withArchivalProfile(String archivalProfile) {
        this.archivalProfile = archivalProfile;
        return this;
    }

    public ManifestContextBuilder withComment(String comment) {
        this.comment = comment;
        return this;
    }

    public ManifestContextBuilder withUnitUp(String unitUp) {
        this.unitUp = unitUp;
        return this;
    }

    public ManifestContextBuilder withAcquisitionInformation(String acquisitionInformation) {
        this.acquisitionInformation = acquisitionInformation;
        return this;
    }

    public ManifestContextBuilder withLegalStatus(String legalStatus) {
        this.legalStatus = legalStatus;
        return this;
    }

    public ManifestContext build() {
        return new ManifestContext(acquisitionInformation, legalStatus,
            archivalAgreement, messageIdentifier, archivalAgencyIdentifier,
            transferringAgencyIdentifier, originatingAgencyIdentifier, submissionAgencyIdentifier, archivalProfile,
            comment, unitUp);
    }
}