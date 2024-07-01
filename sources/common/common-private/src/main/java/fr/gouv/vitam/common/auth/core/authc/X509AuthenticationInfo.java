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
/*
 * Copyright Paul Merlin 2011 (Apache Licence v2.0)
 */
package fr.gouv.vitam.common.auth.core.authc;

import org.apache.shiro.authc.SimpleAuthenticationInfo;

import javax.security.auth.x500.X500Principal;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

/**
 * Based on work: Copyright Paul Merlin 2011 (Apache Licence v2.0)
 */
public class X509AuthenticationInfo extends SimpleAuthenticationInfo {

    private static final int BASE_16 = 16;
    /**
     *
     */
    // Name Constrains Extensions (RFC 3280 4.2.1.11)
    private static final String NAME_CONSTRAINTS_ASN_1 = "2.5.29.30"; // NOSONAR : ASN-1 OID
    private static final long serialVersionUID = 1L;
    private final X509Certificate clientCertificate;
    private final Set<X509Certificate> grantedIssuers = new HashSet<>();
    private final X500Principal subjectDN;
    private final X500Principal issuerDN;
    private final String serialNumber;

    /**
     * @param principal
     * @param clientCertificate
     * @param grantedIssuers
     * @param realmName
     */
    public X509AuthenticationInfo(
        Object principal,
        X509Certificate clientCertificate,
        Set<X509Certificate> grantedIssuers,
        String realmName
    ) {
        super(principal, null, realmName);
        this.clientCertificate = clientCertificate;
        if (clientCertificate != null) {
            subjectDN = clientCertificate.getSubjectX500Principal();
            issuerDN = clientCertificate.getIssuerX500Principal();
            serialNumber = clientCertificate.getSerialNumber().toString(BASE_16);
        } else {
            subjectDN = null;
            issuerDN = null;
            serialNumber = null;
        }
        this.grantedIssuers.addAll(grantedIssuers);
    }

    /**
     * @param principal
     * @param issuerDN
     * @param serialNumber
     * @param realmName
     */
    public X509AuthenticationInfo(Object principal, X500Principal issuerDN, String serialNumber, String realmName) {
        super(principal, null, realmName);
        clientCertificate = null;
        subjectDN = null;
        this.issuerDN = issuerDN;
        this.serialNumber = serialNumber;
    }

    /**
     * @param principal
     * @param subjectDN
     * @param realmName
     */
    public X509AuthenticationInfo(Object principal, X500Principal subjectDN, String realmName) {
        super(principal, null, realmName);
        clientCertificate = null;
        this.subjectDN = subjectDN;
        issuerDN = null;
        serialNumber = null;
    }

    /**
     * @return the clientCertificate
     */
    public X509Certificate getX509Certificate() {
        return clientCertificate;
    }

    /**
     * @return the subjectDN of the certificate
     */
    public X500Principal getSubjectDN() {
        return subjectDN;
    }

    /**
     * @return the issuerDN of the certificate
     */
    public X500Principal getIssuerDN() {
        return issuerDN;
    }

    /**
     * @return the serialNumber of the certificate
     */
    public String getHexSerialNumber() {
        return serialNumber;
    }

    /**
     * @return the trusted certificates
     */
    public Set<TrustAnchor> getGrantedTrustAnchors() {
        final Set<TrustAnchor> trustAnchors = new HashSet<>();
        for (final X509Certificate eachCert : grantedIssuers) {
            trustAnchors.add(new TrustAnchor(eachCert, eachCert.getExtensionValue(NAME_CONSTRAINTS_ASN_1)));
        }
        return trustAnchors;
    }
}
