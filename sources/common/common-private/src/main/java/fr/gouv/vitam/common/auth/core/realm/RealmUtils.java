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
package fr.gouv.vitam.common.auth.core.realm;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.auth.core.authc.X509AuthenticationInfo;
import fr.gouv.vitam.common.auth.core.authc.X509AuthenticationToken;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.apache.shiro.config.Ini;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.realm.text.IniRealm;
import org.apache.shiro.util.PermissionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * RealmUtils
 */
class RealmUtils {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RealmUtils.class);
    private static final String ROLES_SECTION_NAME = "roles";
    private static final String SHIRO_INI_FILE = "shiro.ini";

    /**
     * load role declared in shiro.ini file
     *
     * @return mapping role/permissions
     */
    static Map<String, String> getRoleDefs() {
        try {
            IniRealm inirealm = new IniRealm(PropertiesUtils.findFile(SHIRO_INI_FILE).getPath());
            Ini iniConf = inirealm.getIni();
            if (iniConf != null) {
                return inirealm.getIni().getSection(ROLES_SECTION_NAME);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Shiro.ini file not found. You should use shiro.ini file to declare role");
        }
        return Collections.emptyMap();
    }

    /**
     * Get list of permissions for role
     *
     * @param roleDefs
     * @param roleNames
     * @return list of permissions
     */
    static Set<Permission> getPermissionsSet(Map<String, String> roleDefs, Set<String> roleNames) {
        WildcardPermissionResolver wpResolver = new WildcardPermissionResolver();
        Set<Permission> permissionsSet = new HashSet<>();
        if (roleDefs != null) {
            for (String role : roleNames) {
                String permissions = roleDefs.get(role);
                if (permissions != null) {
                    permissionsSet.addAll(PermissionUtils.resolveDelimitedPermissions(permissions, wpResolver));
                }
            }
        }
        return permissionsSet;
    }

    /**
     * @param x509AuthenticationToken
     * @param grantedKeyStoreName
     * @param grantedKeyStorePassphrase
     * @param trustedKeyStoreName
     * @param trustedKeyStorePassphrase
     * @param grantedIssuers
     * @param realmName
     * @return X509AuthenticationInfo
     */
    static X509AuthenticationInfo getX509AuthenticationInfo(
        X509AuthenticationToken x509AuthenticationToken,
        String grantedKeyStoreName,
        String grantedKeyStorePassphrase,
        String trustedKeyStoreName,
        String trustedKeyStorePassphrase,
        Set<X509Certificate> grantedIssuers,
        String realmName
    ) {
        X509AuthenticationInfo x509AuthenticationInfo = null;
        try {
            ParametersChecker.checkParameter(grantedKeyStorePassphrase, "grantedKeyStorePassphrase cannot be null");
            ParametersChecker.checkParameter(trustedKeyStorePassphrase, "trustedKeyStorePassphrase cannot be null");
            final KeyStore trustedks = readAndLoadKeystore(trustedKeyStoreName, trustedKeyStorePassphrase);
            if (trustedks != null) {
                for (final Enumeration<String> e = trustedks.aliases(); e.hasMoreElements();) {
                    final String alias = e.nextElement();
                    grantedIssuers.add((X509Certificate) trustedks.getCertificate(alias));
                }
            }

            final KeyStore grantedks = readAndLoadKeystore(grantedKeyStoreName, grantedKeyStorePassphrase);
            if (grantedks != null) {
                for (final Enumeration<String> e = grantedks.aliases(); e.hasMoreElements();) {
                    final String alias = e.nextElement();
                    final X509Certificate x509cert = (X509Certificate) grantedks.getCertificate(alias);
                    if (
                        new Sha256Hash(x509cert.getEncoded()).equals(
                            new Sha256Hash(x509AuthenticationToken.getX509Certificate().getEncoded())
                        )
                    ) {
                        x509AuthenticationInfo = new X509AuthenticationInfo(
                            x509AuthenticationToken.getSubjectDN().getName(),
                            x509AuthenticationToken.getX509Certificate(),
                            grantedIssuers,
                            realmName
                        );
                        break;
                    }
                }
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
    private static KeyStore readAndLoadKeystore(String filename, String passphrase)
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        try {
            final File f = PropertiesUtils.findFile(filename);
            try (final FileInputStream fis = new FileInputStream(f)) {
                final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(fis, passphrase.toCharArray());
                return ks;
            }
        } catch (final FileNotFoundException e) {
            LOGGER.error("Keystore Not found : " + filename, e);
        }
        return null;
    }
}
