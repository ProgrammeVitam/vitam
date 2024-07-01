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
package fr.gouv.vitam.security.internal.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * base model for identity
 */
public class CertificateBaseModel {

    public static final String SUBJECT_DN = "SubjectDN";

    public static final String SERIAL_NUMBER = "SerialNumber";

    public static final String CERTIFICATE_TAG = "Certificate";

    public static final String ISSUER_DN_TAG = "IssuerDN";

    public static final String STATUS_TAG = "Status";

    public static final String EXPIRATION_DATE_TAG = "ExpirationDate";

    public static final String REVOCATION_DATE_TAG = "RevocationDate";

    @JsonProperty("_id")
    private String id;

    @JsonProperty(SUBJECT_DN)
    private String subjectDN;

    @JsonProperty(SERIAL_NUMBER)
    private String serialNumber;

    @JsonProperty(ISSUER_DN_TAG)
    private String issuerDN;

    @JsonProperty(CERTIFICATE_TAG)
    private byte[] certificate;

    @JsonProperty(STATUS_TAG)
    private String certificateStatus = CertificateStatus.VALID.name(); //Default to 'VALID'

    @JsonProperty(EXPIRATION_DATE_TAG)
    private String expirationDate;

    @JsonProperty(REVOCATION_DATE_TAG)
    private String revocationDate;

    public CertificateBaseModel() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubjectDN() {
        return subjectDN;
    }

    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getIssuerDN() {
        return issuerDN;
    }

    public void setIssuerDN(String issuerDN) {
        this.issuerDN = issuerDN;
    }

    public byte[] getCertificate() {
        return certificate;
    }

    public void setCertificate(byte[] certificate) {
        this.certificate = certificate;
    }

    /**
     * getter for certificateStatus
     *
     * @return certificateStatus value
     */
    public CertificateStatus getCertificateStatus() {
        return CertificateStatus.valueOf(certificateStatus);
    }

    /**
     * set certificateStatus
     */
    public void setCertificateStatus(CertificateStatus certificateStatus) {
        this.certificateStatus = certificateStatus.name();
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * getter for revocationDate
     *
     * @return revocationDate value
     */
    public String getRevocationDate() {
        return revocationDate;
    }

    /**
     * set revocationDate
     */
    public void setRevocationDate(String revocationDate) {
        this.revocationDate = revocationDate;
    }
}
