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

import fr.gouv.vitam.common.auth.core.authc.X509AuthenticationInfo;
import fr.gouv.vitam.common.auth.core.authc.X509AuthenticationToken;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.AuthorizingRealm;

/**
 * Based on work: Copyright Paul Merlin 2011 (Apache Licence v2.0)
 */
public abstract class AbstractX509Realm extends AuthorizingRealm {

    private String grantedKeyStoreName;
    private String grantedKeyStorePassphrase;
    private String trustedKeyStoreName;
    private String trustedKeyStorePassphrase;

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
        return doGetX509AuthenticationInfo((X509AuthenticationToken) token);
    }

    protected abstract X509AuthenticationInfo doGetX509AuthenticationInfo(
        X509AuthenticationToken x509AuthenticationToken
    );

    /**
     * @return the grantedKeyStoreName
     */
    public String getGrantedKeyStoreName() {
        return grantedKeyStoreName;
    }

    /**
     * @param grantedKeyStoreName the grantedKeyStoreName to set
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
     */
    public void setTrustedKeyStorePassphrase(String trustedKeyStorePassphrase) {
        this.trustedKeyStorePassphrase = trustedKeyStorePassphrase;
    }
}
