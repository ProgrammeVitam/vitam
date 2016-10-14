/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.common.auth.core.realm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.crypto.hash.Sha256Hash;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.auth.core.authc.X509AuthenticationInfo;
import fr.gouv.vitam.common.auth.core.authc.X509AuthenticationToken;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * X509 Keystore File Realm
 */
public class X509KeystoreFileRealm extends AbstractX509Realm {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(X509KeystoreFileRealm.class);

    private static final String REALM_NAME = "X509KeystoreFile";
    private String grantedKeyStoreName;
    private String grantedKeyStorePassphrase;
    private String trustedKeyStoreName;
    private String trustedKeyStorePassphrase;
    private final Set<X509Certificate> grantedIssuers = new HashSet<>();

    /**
     * empty constructor
     */
    public X509KeystoreFileRealm() {
        // empty
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof X509AuthenticationToken;
    }

    @Override
    public Class<X509AuthenticationToken> getAuthenticationTokenClass() {
        return X509AuthenticationToken.class;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
        return doGetX509AuthenticationInfo((X509AuthenticationToken) token);
    }

    // TODO éviter de relire les 2 fichiers granted et trusted à chaque tentative d'authentification
    @Override
    protected X509AuthenticationInfo doGetX509AuthenticationInfo(X509AuthenticationToken x509AuthenticationToken) {

        X509AuthenticationInfo x509AuthenticationInfo = null;
        try {
            ParametersChecker.checkParameter(grantedKeyStorePassphrase, "grantedKeyStorePassphrase cannot be null");
            ParametersChecker.checkParameter(trustedKeyStorePassphrase, "trustedKeyStorePassphrase cannot be null");
            final KeyStore trustedks = readAndLoadKeystore(trustedKeyStoreName, trustedKeyStorePassphrase);
            for (final Enumeration<String> e = trustedks.aliases(); e.hasMoreElements();) {
                final String alias = e.nextElement();
                grantedIssuers.add((X509Certificate) trustedks.getCertificate(alias));
            }

            final KeyStore grantedks = readAndLoadKeystore(grantedKeyStoreName, grantedKeyStorePassphrase);
            for (final Enumeration<String> e = grantedks.aliases(); e.hasMoreElements();) {
                final String alias = e.nextElement();
                final X509Certificate x509cert = (X509Certificate) grantedks.getCertificate(alias);
                if (new Sha256Hash(x509cert.getEncoded())
                    .equals(new Sha256Hash(x509AuthenticationToken.getX509Certificate().getEncoded()))) {
                    x509AuthenticationInfo = new X509AuthenticationInfo(
                        x509AuthenticationToken.getSubjectDN(),
                        x509AuthenticationToken.getX509Certificate(),
                        grantedIssuers,
                        REALM_NAME);
                    break;
                }
            }
            if (x509AuthenticationInfo != null) {
                assertCredentialsMatch(x509AuthenticationToken, x509AuthenticationInfo);
            }
        } catch (final NoSuchAlgorithmException e) {
            LOGGER.error("Unable to verify the integrity of the keystore", e);
        } catch (final CertificateException e) {
            LOGGER.error("Unable to load a certificate of the keystore", e);
        } catch (final IOException e) {
            LOGGER.error("Unable to open the keystore", e);
        } catch (final KeyStoreException e) {
            // this must not happen
            LOGGER.error("The keystore type has not been loaded", e);
        }
        return x509AuthenticationInfo;
    }

    /**
     * Read and load the keystore
     *
     * @param filename : keystore file
     * @param passphrase :keystore passphrase
     * @return the loaded keystore
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    private KeyStore readAndLoadKeystore(String filename, String passphrase)
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        try {
            final File f = PropertiesUtils.findFile(filename);
            final FileInputStream fis = new FileInputStream(f);
            final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(fis, passphrase.toCharArray());
            fis.close();
            return ks;
        } catch (final FileNotFoundException e) {
            LOGGER.error("Keystore Not found : " + filename, e);
        }
        return null;
    }

    /**
     * @return the grantedKeyStoreName
     */
    public String getGrantedKeyStoreName() {
        return grantedKeyStoreName;
    }

    /**
     * @param grantedKeyStoreName the grantedKeyStoreName to set
     *
     */
    public void setGrantedKeyStoreName(String grantedKeyStoreName) {
        this.grantedKeyStoreName = grantedKeyStoreName;
    }

    /**
     * @return the grantedKeyStorePassphrase
     */
    public String getGrantedKeyStorePassphrase() {
        return grantedKeyStorePassphrase;
    }

    /**
     * @param grantedKeyStorePassphrase the grantedKeyStorePassphrase to set
     *
     */
    public void setGrantedKeyStorePassphrase(String grantedKeyStorePassphrase) {
        this.grantedKeyStorePassphrase = grantedKeyStorePassphrase;
    }

    /**
     * @return the trustedKeyStoreName
     */
    public String getTrustedKeyStoreName() {
        return trustedKeyStoreName;
    }

    /**
     * @param trustedKeyStoreName the trustedKeyStoreName to set
     *
     */
    public void setTrustedKeyStoreName(String trustedKeyStoreName) {
        this.trustedKeyStoreName = trustedKeyStoreName;
    }

    /**
     * @return the trustedKeyStorePassphrase
     */
    public String getTrustedKeyStorePassphrase() {
        return trustedKeyStorePassphrase;
    }

    /**
     * @param trustedKeyStorePassphrase the trustedKeyStorePassphrase to set
     *
     */
    public void setTrustedKeyStorePassphrase(String trustedKeyStorePassphrase) {
        this.trustedKeyStorePassphrase = trustedKeyStorePassphrase;
    }

}
