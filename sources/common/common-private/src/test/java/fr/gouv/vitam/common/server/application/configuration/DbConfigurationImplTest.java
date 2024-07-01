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
package fr.gouv.vitam.common.server.application.configuration;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 *
 */
public class DbConfigurationImplTest {

    private static final String host = "localhost";
    private static final int port = 12345;

    private static final String EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION =
        "Expecting exception: IllegalArgumentException";

    @Test
    public void testBadConfiguration() {
        DbConfigurationImpl dbConfiguration0 = new DbConfigurationImpl();
        try {
            final List<MongoDbNode> nodes = new ArrayList<>();
            nodes.add(new MongoDbNode(host, -16));
            dbConfiguration0.setMongoDbNodes(nodes);
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            final List<MongoDbNode> nodes = new ArrayList<>();
            nodes.add(new MongoDbNode((String) null, port));
            dbConfiguration0.setMongoDbNodes(nodes);
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            dbConfiguration0.setDbName("");
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            dbConfiguration0.setDbName((String) null);
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            final List<MongoDbNode> nodes = new ArrayList<>();
            nodes.add(new MongoDbNode("", port));
            dbConfiguration0.setMongoDbNodes(nodes);
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            final List<MongoDbNode> nodes = new ArrayList<>();
            nodes.add(new MongoDbNode(host, 0));
            dbConfiguration0.setMongoDbNodes(nodes);
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            final List<MongoDbNode> nodes = new ArrayList<>();
            nodes.add(new MongoDbNode("", 265));
            dbConfiguration0 = new DbConfigurationImpl(nodes, "AAA");
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            final List<MongoDbNode> nodes = new ArrayList<>();
            nodes.add(new MongoDbNode("AAA", -265));
            dbConfiguration0 = new DbConfigurationImpl(nodes, "AAA");
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
        try {
            final List<MongoDbNode> nodes = new ArrayList<>();
            nodes.add(new MongoDbNode("AAA", 265));
            dbConfiguration0 = new DbConfigurationImpl(nodes, "");
            fail(EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR ignore
    }

    @Test
    public void testGetterSetter() {
        final DbConfigurationImpl dbConfiguration0 = new DbConfigurationImpl();
        assertNull(dbConfiguration0.getMongoDbNodes());
        assertNull(dbConfiguration0.getDbName());

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("AAA", 394));
        dbConfiguration0.setMongoDbNodes(nodes);
        assertEquals(394, dbConfiguration0.getMongoDbNodes().get(0).getDbPort());
        assertEquals("AAA", dbConfiguration0.getMongoDbNodes().get(0).getDbHost());

        dbConfiguration0.setDbName("BBB");
        assertEquals("BBB", dbConfiguration0.getDbName());
        final DbConfigurationImpl dbConfiguration1 = new DbConfigurationImpl(nodes, "BBB");
        assertEquals(394, dbConfiguration1.getMongoDbNodes().get(0).getDbPort());
        assertEquals("AAA", dbConfiguration1.getMongoDbNodes().get(0).getDbHost());
        assertEquals("BBB", dbConfiguration1.getDbName());
        final DbConfigurationImpl dbConfiguration2 = new DbConfigurationImpl(nodes, "BBB", true, "user", "pwd");
        assertEquals(394, dbConfiguration2.getMongoDbNodes().get(0).getDbPort());
        assertEquals("AAA", dbConfiguration2.getMongoDbNodes().get(0).getDbHost());
        assertEquals("BBB", dbConfiguration2.getDbName());
        assertEquals("user", dbConfiguration2.getDbUserName());
        assertEquals("pwd", dbConfiguration2.getDbPassword());
        assertEquals(true, dbConfiguration2.isDbAuthentication());
        dbConfiguration2.setDbAuthentication(false).setDbUserName("user2").setDbPassword("pwd2");
        assertEquals("user2", dbConfiguration2.getDbUserName());
        assertEquals("pwd2", dbConfiguration2.getDbPassword());
        assertEquals(false, dbConfiguration2.isDbAuthentication());
    }

    @Test
    public void testEmpty() {
        final DbConfigurationImpl dbConfiguration0 = new DbConfigurationImpl();
        final List<MongoDbNode> nodes = dbConfiguration0.getMongoDbNodes();
        assertNull(nodes);
    }
}
