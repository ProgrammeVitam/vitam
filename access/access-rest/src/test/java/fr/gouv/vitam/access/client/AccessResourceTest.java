/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.access.client;

import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.access.client.AccessClientFactory.AccessClientType;
import fr.gouv.vitam.access.common.exception.AccessClientException;
import fr.gouv.vitam.access.rest.AccessApplication;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;

/**
 * AccessResourceTest class
 */
public class AccessResourceTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessResourceTest.class);

    private static final String ACCESS_CONF = "access.conf";
    private static VitamServer vitamServer;
    private static final String DATABASE_HOST = "";
    private static final int SERVER_PORT = 8101;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        try {
            vitamServer = AccessApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(ACCESS_CONF).getAbsolutePath(),
                Integer.toString(SERVER_PORT)});
            ((BasicVitamServer) vitamServer).start();
        } catch (FileNotFoundException | VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Access Application Server", e);
        }

        AccessClientFactory.setConfiguration(AccessClientType.MOCK_OPERATIONS, DATABASE_HOST, SERVER_PORT);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            ((BasicVitamServer) vitamServer).stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }

    }


    @Test
    public final void test() throws AccessClientException, InvalidParseOperationException {

        final AccessClient client =
            AccessClientFactory.getInstance().getAccessOperationClient();
        final String selectQuery =
            "{ $query : [ { $eq : { 'title' : 'test' } } ], " +
                " $filter : { $orderby : { '#id' } }," +
                " $projection : {$fields : {#id : 1, title:2, transacdate:1}}" +
                " }";
        assertNotNull(client.selectUnits(selectQuery));
    }

}
