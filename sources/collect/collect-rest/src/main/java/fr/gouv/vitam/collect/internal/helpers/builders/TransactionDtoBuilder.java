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
package fr.gouv.vitam.collect.internal.helpers.builders;

import fr.gouv.vitam.collect.external.dto.TransactionDto;

public class TransactionDtoBuilder {
    private String id;
    private String archivalAgreement;
    private String messageIdentifier;
    private String archivalAgencyIdentifier;
    private String transferingAgencyIdentifier;
    private String originatingAgencyIdentifier;
    private String submissionAgencyIdentifier;
    private String archivalProfile;
    private String comment;
    private Integer tenant;

    public TransactionDtoBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public TransactionDtoBuilder withArchivalAgreement(String archivalAgreement) {
        this.archivalAgreement = archivalAgreement;
        return this;
    }

    public TransactionDtoBuilder withMessageIdentifier(String messageIdentifier) {
        this.messageIdentifier = messageIdentifier;
        return this;
    }

    public TransactionDtoBuilder withArchivalAgencyIdentifier(String archivalAgencyIdentifier) {
        this.archivalAgencyIdentifier = archivalAgencyIdentifier;
        return this;
    }

    public TransactionDtoBuilder withTransferingAgencyIdentifier(String transferingAgencyIdentifier) {
        this.transferingAgencyIdentifier = transferingAgencyIdentifier;
        return this;
    }

    public TransactionDtoBuilder withOriginatingAgencyIdentifier(String originatingAgencyIdentifier) {
        this.originatingAgencyIdentifier = originatingAgencyIdentifier;
        return this;
    }

    public TransactionDtoBuilder withSubmissionAgencyIdentifier(String submissionAgencyIdentifier) {
        this.submissionAgencyIdentifier = submissionAgencyIdentifier;
        return this;
    }

    public TransactionDtoBuilder withArchivalProfile(String archivalProfile) {
        this.archivalProfile = archivalProfile;
        return this;
    }

    public TransactionDtoBuilder withComment(String comment) {
        this.comment = comment;
        return this;
    }

    public TransactionDtoBuilder withTenant(Integer tenant) {
        this.tenant = tenant;
        return this;
    }

    public TransactionDto build() {
        return new TransactionDto(id, archivalAgreement, messageIdentifier, archivalAgencyIdentifier,
            transferingAgencyIdentifier, originatingAgencyIdentifier, submissionAgencyIdentifier, archivalProfile,
            comment, tenant);
    }
}