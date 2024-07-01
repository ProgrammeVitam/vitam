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

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.HostAuthenticationToken;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.security.auth.x500.X500Principal;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Based on work: Copyright Paul Merlin 2011 (Apache Licence v2.0)
 */
public class X509AuthenticationToken implements AuthenticationToken, HostAuthenticationToken {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(X509AuthenticationToken.class);

    private static final long serialVersionUID = 1L;
    private final X509Certificate certificate;
    private final X509Certificate[] certChain;
    private final X500Principal subjectDN;
    private final X500Principal issuerDN;
    private final String hexSerialNumber;
    private final String host;

    /**
     * @param clientCertChain
     * @param host
     */
    public X509AuthenticationToken(X509Certificate[] clientCertChain, String host) {
        if (clientCertChain == null || clientCertChain.length < 1) {
            throw new IllegalArgumentException("No certificate in the chain");
        }
        certChain = clientCertChain;
        certificate = certChain[0];
        subjectDN = certificate.getSubjectX500Principal();
        issuerDN = certificate.getIssuerX500Principal();
        hexSerialNumber = certificate.getSerialNumber().toString(16);
        this.host = host;
    }

    /**
     * @param clientSubjectDN
     * @param clientIssuerDN
     * @param clientHexSerialNumber
     * @param host
     */
    public X509AuthenticationToken(
        X500Principal clientSubjectDN,
        X500Principal clientIssuerDN,
        String clientHexSerialNumber,
        String host
    ) {
        certificate = null;
        certChain = new X509Certificate[] {};
        subjectDN = clientSubjectDN;
        issuerDN = clientIssuerDN;
        hexSerialNumber = clientHexSerialNumber;
        this.host = host;
    }

    /**
     * @return the X509 certificate
     */
    public X509Certificate getX509Certificate() {
        return certificate;
    }

    /**
     * @return the JVM X509 certificate selector
     */
    public CertSelector getX509CertSelector() {
        final X509CertSelector certSelector = new X509CertSelector();
        certSelector.setCertificate(certificate);
        return certSelector;
    }

    /**
     * @return get a Store with the Cert
     */
    public CertStore getX509CertChainStore() {
        try {
            final CollectionCertStoreParameters params = new CollectionCertStoreParameters(Arrays.asList(certChain));
            return CertStore.getInstance("CERTIFICATE/COLLECTION", params, BouncyCastleProvider.PROVIDER_NAME);
        } catch (final NoSuchProviderException e) {
            LOGGER.error("Bouncy Castle is not loaded", e);
        } catch (final InvalidAlgorithmParameterException e) {
            LOGGER.error("This type of Certstore is unknown (CERTIFICATE/COLLECTION)", e);
        } catch (final NoSuchAlgorithmException e) {
            LOGGER.error("algorithm of the certificate unknown", e);
        }
        return null;
    }

    /**
     * @return the subjectDN
     */
    public X500Principal getSubjectDN() {
        return subjectDN;
    }

    /**
     * @return the Issuer DN
     */
    public X500Principal getIssuerDN() {
        return issuerDN;
    }

    /**
     * @return the Serial Number (in hexadecimal)
     */
    public String getHexSerialNumber() {
        return hexSerialNumber;
    }

    @Override
    public Object getPrincipal() {
        return subjectDN;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public String getHost() {
        return host;
    }
}
