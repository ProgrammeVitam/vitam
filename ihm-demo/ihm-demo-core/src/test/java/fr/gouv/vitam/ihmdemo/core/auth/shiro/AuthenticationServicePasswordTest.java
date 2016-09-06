/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital 
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.ihmdemo.core.auth.shiro;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.ihmdemo.core.auth.shiro.AuthenticationServiceFactory.AuthenticationServiceType;

public class AuthenticationServicePasswordTest {

    private static final String DEFAULT_USER = "user";
    private static AuthenticationService client;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AuthenticationServiceFactory.setConfiguration(AuthenticationServiceType.PRODUCTION, "src/test/resources/shiro-config.ini");
        client = AuthenticationServiceFactory.getInstance().getAuthenticationService();
    }
    @Test
    public void testAuthenticationService() throws Exception {

        Serializable sessionId = client.login(DEFAULT_USER, DEFAULT_USER, true);
        assertNotNull(sessionId);
        assertTrue(client.getSession(sessionId.toString()));
        assertFalse(client.getSession(DEFAULT_USER));
        client.logout(sessionId.toString());
    }

    @Test(expected=VitamException.class)
    public void testAuthenticationServiceLoginError() throws Exception {
        client.login("a", DEFAULT_USER, true);
    }

    @Test(expected=VitamException.class)
    public void testAuthenticationServiceLogOutError() throws Exception {
        client.logout("a");
    }


}
