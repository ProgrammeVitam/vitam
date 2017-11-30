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
package fr.gouv.vitam.common.security.rest;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.apache.shiro.realm.ldap.AbstractLdapRealm;
import org.apache.shiro.realm.ldap.LdapContextFactory;
import org.apache.shiro.realm.ldap.LdapUtils;
import org.apache.shiro.subject.PrincipalCollection;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Realm query Ldap to get users role
 */
public class LdapRealm  extends AbstractLdapRealm {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LdapRealm.class);
    private static final String NAMES_DELIMETER = ",";

    private String userDnTemplate;
    private Map<String, String> rolePermissionsMap;
    private Map<String, String> groupRolesMap;


    /**
     * set the map of group role, defined in shiro.ini
     *
     * @param groupRolesMap
     */
    public void setGroupRolesMap(Map<String, String> groupRolesMap) {
        this.groupRolesMap = groupRolesMap;
    }

    /**
     * set the map of role to permission, defined in shiro.ini
     *
     * @param rolePermissionsMap
     */
    public void setRolePermissionsMap(Map<String, String> rolePermissionsMap) {
        this.rolePermissionsMap = rolePermissionsMap;
    }

    /**
     * set Template to convert username to dn, defined in shiro.ini
     *
     * @param userDnTemplate
     */
    public void setUserDnTemplate(String userDnTemplate) {
        this.userDnTemplate = userDnTemplate;
    }

    @Override
    protected AuthenticationInfo queryForAuthenticationInfo(AuthenticationToken token, LdapContextFactory ldapContextFactory) throws NamingException {

        UsernamePasswordToken upToken = (UsernamePasswordToken) token;

        String userName = upToken.getUsername();
        String userDn = userName;

        if (!userDnTemplate.isEmpty()) {
            userDn = userDnTemplate.replace("{0}", userName);
        }

        LdapContext ctx = null;
        try {
            ctx = ldapContextFactory.getLdapContext(userDn, String.valueOf(upToken.getPassword()));
        } finally {
            LdapUtils.closeContext(ctx);
        }

        return new SimpleAuthenticationInfo(userDn, upToken.getPassword(), getName());
    }

    protected AuthorizationInfo queryForAuthorizationInfo(PrincipalCollection principals, LdapContextFactory ldapContextFactory) throws NamingException {

        String username = (String) getAvailablePrincipal(principals);

        // get ldap context admin
        LdapContext ldapContext = ldapContextFactory.getSystemLdapContext();

        try {
            return getRoleNamesForUser(username, ldapContext);
        } finally {
            LdapUtils.closeContext(ldapContext);
        }
    }

    private AuthorizationInfo getRoleNamesForUser(String userName, LdapContext ldapContext) throws NamingException {
        Set<String> roleNames = new LinkedHashSet<String>();

        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        // Search all group contain userName as member
        String searchFilter = "(&(objectClass=groupOfNames)(member={0}))";
        Object[] searchArguments = new Object[]{userName};

        NamingEnumeration answer = ldapContext.search(searchBase, searchFilter, searchArguments, searchCtls);

        while (answer.hasMoreElements()) {
            SearchResult sr = (SearchResult) answer.next();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Retrieving group names for user [" + sr.getName() + "]");
            }

            Attributes attrs = sr.getAttributes();

            if (attrs != null) {
                NamingEnumeration ae = attrs.getAll();
                while (ae.hasMore()) {
                    Attribute attr = (Attribute) ae.next();
                    if (attr.getID().equals("cn")) {

                        Collection<String> groupNames = LdapUtils.getAllAttributeValues(attr);

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Groups found for user [" + userName + "]: " + groupNames);
                        }

                        Collection<String> rolesForGroups = getRoleNamesForGroups(groupNames);
                        roleNames.addAll(rolesForGroups);
                        break;
                    }
                }
            }
        }
        SimpleAuthorizationInfo authInfo = new SimpleAuthorizationInfo(roleNames);
        final String role = roleNames.iterator().next();
        List<String> permissions = Arrays.asList(this.rolePermissionsMap.get(role).split(NAMES_DELIMETER));

        Set<Permission> permissionsSet = new HashSet<>(permissions.size());
        WildcardPermissionResolver wpResolver = new WildcardPermissionResolver();
        for (String pem : permissions) {
            permissionsSet.add(wpResolver.resolvePermission(pem));
        }
        authInfo.setObjectPermissions(permissionsSet);
        return authInfo;
    }

    protected Collection<String> getRoleNamesForGroups(Collection<String> groupNames) {
        Set<String> roleNames = new HashSet<>(groupNames.size());
        if (groupRolesMap != null) {
            for (String groupName : groupNames) {
                String strRoleNames = groupRolesMap.get(groupName);
                roleNames.add(strRoleNames);
            }
        }
        return roleNames;
    }
}
