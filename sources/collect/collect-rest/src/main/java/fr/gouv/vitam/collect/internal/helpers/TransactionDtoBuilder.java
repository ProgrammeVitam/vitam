package fr.gouv.vitam.collect.internal.helpers;

import fr.gouv.vitam.collect.internal.dto.TransactionDto;

public class TransactionDtoBuilder {
    private String id;
    private String archivalAgencyIdentifier;
    private String transferingAgencyIdentifier;
    private String originatingAgencyIdentifier;
    private String archivalProfile;
    private String comment;

    public TransactionDtoBuilder setId(String id) {
        this.id = id;
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

    public TransactionDtoBuilder withArchivalProfile(String archivalProfile) {
        this.archivalProfile = archivalProfile;
        return this;
    }

    public TransactionDtoBuilder withComment(String comment) {
        this.comment = comment;
        return this;
    }

    public TransactionDto build() {
        return new TransactionDto(id, archivalAgencyIdentifier, transferingAgencyIdentifier, originatingAgencyIdentifier, archivalProfile, comment);
    }
}