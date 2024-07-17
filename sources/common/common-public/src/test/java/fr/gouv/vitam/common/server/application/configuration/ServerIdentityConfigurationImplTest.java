/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.server.application.configuration;

import fr.gouv.vitam.common.ServerIdentity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 *
 */
public class ServerIdentityConfigurationImplTest {

    private static final String EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION =
        "Expecting exception: IllegalArgumentException";

    @Test
    public void testBadConfiguration() {
        ServerIdentity.ServerIdentityConfigurationImpl siConf0 = new ServerIdentity.ServerIdentityConfigurationImpl();
        try {
            siConf0.setIdentityServerId(-16);
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            siConf0.setIdentityName((String) null);
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            siConf0.setIdentityName("");
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            siConf0.setIdentityRole((String) null);
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            siConf0.setIdentityRole("");
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            siConf0 = new ServerIdentity.ServerIdentityConfigurationImpl("", 265, 1, "AAA");
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            siConf0 = new ServerIdentity.ServerIdentityConfigurationImpl("AAA", -265, 1, "AAA");
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            siConf0 = new ServerIdentity.ServerIdentityConfigurationImpl("AAA", 265, 1, "");
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        siConf0 = new ServerIdentity.ServerIdentityConfigurationImpl("AAA", 265, 1, "BBB");
    }

    @Test
    public void testEmpty() {
        final ServerIdentity.ServerIdentityConfigurationImpl siConf0 =
            new ServerIdentity.ServerIdentityConfigurationImpl();
        final String string0 = siConf0.getIdentityName();
        assertNull(string0);
        assertNull(siConf0.getIdentityRole());
        assertEquals(0, siConf0.getIdentitySiteId());
        siConf0.setIdentityName("id1").setIdentitySiteId(1).setIdentityRole("role1");
        assertEquals("id1", siConf0.getIdentityName());
        assertEquals(1, siConf0.getIdentitySiteId());
        assertEquals("role1", siConf0.getIdentityRole());
    }
}
