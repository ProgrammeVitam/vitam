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
package fr.gouv.vitam.common.auth.core.realm;

import fr.gouv.vitam.common.auth.core.authc.X509AuthenticationInfo;
import fr.gouv.vitam.common.auth.core.authc.X509AuthenticationToken;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * X509 Keystore File Realm with Role/Permissions
 */
public class X509KeystoreFileWithRoleRealm extends AbstractX509Realm {

    private static final String REALM_NAME = "X509KeystoreFile";
    private final Set<X509Certificate> grantedIssuers = new HashSet<>();
    private Map<String, String> roleDefs;
    private Map<String, String> certificateDnRoleMapping;

    /**
     * empty constructor
     */
    public X509KeystoreFileWithRoleRealm() {
        // empty
        this.roleDefs = RealmUtils.getRoleDefs();
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String username = (String) getAvailablePrincipal(principals);
        Set<String> roleNames = new LinkedHashSet<>();
        if (this.certificateDnRoleMapping != null && this.certificateDnRoleMapping.get(username) != null) {
            roleNames.add(this.certificateDnRoleMapping.get(username));
        }
        SimpleAuthorizationInfo authInfo = new SimpleAuthorizationInfo(roleNames);
        Set<Permission> permissionsSet = RealmUtils.getPermissionsSet(this.roleDefs, roleNames);
        authInfo.setObjectPermissions(permissionsSet);

        return authInfo;
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

    @Override
    protected X509AuthenticationInfo doGetX509AuthenticationInfo(X509AuthenticationToken x509AuthenticationToken) {
        X509AuthenticationInfo x509AuthenticationInfo = RealmUtils.getX509AuthenticationInfo(
            x509AuthenticationToken,
            getGrantedKeyStoreName(),
            getGrantedKeyStorePassphrase(),
            getTrustedKeyStoreName(),
            getTrustedKeyStorePassphrase(),
            grantedIssuers,
            REALM_NAME
        );
        if (x509AuthenticationInfo != null) {
            assertCredentialsMatch(x509AuthenticationToken, x509AuthenticationInfo);
        }
        return x509AuthenticationInfo;
    }

    /**
     * @return certificateDnRoleMapping
     */
    public Map<String, String> getCertificateDnRoleMapping() {
        return certificateDnRoleMapping;
    }

    /**
     * @param certificateDnRoleMapping the mapping of certificate subject and role
     */
    public void setCertificateDnRoleMapping(Map<String, String> certificateDnRoleMapping) {
        this.certificateDnRoleMapping = certificateDnRoleMapping;
    }
}
