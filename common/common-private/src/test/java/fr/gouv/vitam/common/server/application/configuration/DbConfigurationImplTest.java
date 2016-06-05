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
package fr.gouv.vitam.common.server.application.configuration;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 
 */
public class DbConfigurationImplTest {

    private static final String EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION =
        "Expecting exception: IllegalArgumentException";

    @Test
    public void testBadConfiguration() {
        DbConfigurationImpl dbConfiguration0 = new DbConfigurationImpl();
        try {
            dbConfiguration0.setDbPort(-16);
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {// NOSONAR ignore
        }
        try {
            dbConfiguration0.setDbHost((String) null);
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {// NOSONAR ignore
        }
        try {
            dbConfiguration0.setDbName("");
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {// NOSONAR ignore
        }
        try {
            dbConfiguration0.setDbName((String) null);
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {// NOSONAR ignore
        }
        try {
            dbConfiguration0.setDbHost("");
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {// NOSONAR ignore
        }
        try {
            dbConfiguration0.setDbPort(0);
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {// NOSONAR ignore
        }
        try {
            dbConfiguration0 = new DbConfigurationImpl("", 265, "AAA");
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {// NOSONAR ignore
        }
        try {
            dbConfiguration0 = new DbConfigurationImpl("AAA", -265, "AAA");
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {// NOSONAR ignore
        }
        try {
            dbConfiguration0 = new DbConfigurationImpl("AAA", 265, "");
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {// NOSONAR ignore
        }
    }

    @Test
    public void testGetterSetter() {
        final DbConfigurationImpl dbConfiguration0 = new DbConfigurationImpl();
        assertNull(dbConfiguration0.getDbHost());
        assertNull(dbConfiguration0.getDbName());
        assertEquals(0, dbConfiguration0.getDbPort());
        dbConfiguration0.setDbPort(394);
        assertEquals(394, dbConfiguration0.getDbPort());
        dbConfiguration0.setDbHost("AAA");
        assertEquals("AAA", dbConfiguration0.getDbHost());
        dbConfiguration0.setDbName("BBB");
        assertEquals("BBB", dbConfiguration0.getDbName());
        final DbConfigurationImpl dbConfiguration1 = new DbConfigurationImpl("AAA", 394, "BBB");
        assertEquals(394, dbConfiguration1.getDbPort());
        assertEquals("AAA", dbConfiguration1.getDbHost());
        assertEquals("BBB", dbConfiguration1.getDbName());        
    }

    @Test
    public void testEmpty() {
        final DbConfigurationImpl dbConfiguration0 = new DbConfigurationImpl();
        final String string0 = dbConfiguration0.getDbHost();
        assertNull(string0);
    }
}
