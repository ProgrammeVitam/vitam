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

package fr.gouv.vitam.common.server2.application.session;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import fr.gouv.vitam.common.GlobalDataRest;
import org.junit.Assert;

import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.slf4j.MDC;

/**
 * Server2 implementation of {@link VitamRequestIdFiltersIT} tests.
 */
public class Server2TestApplication extends AbstractTestApplication {

    protected final Class getResourceClass() {
        return Server2Resource.class;
    }

    public Server2TestApplication() {
        super("session/server2.conf");
    }

    public final static String NO_REQUEST_ID_FOUND = "<REQUEST_ID_EMPTY>";

    @Path("/server2")
    public static class Server2Resource {


        @GET
        @Path("/failIfNoRequestId")
        public String failIfNoRequestId() throws VitamThreadAccessException {
            final String reqId = VitamThreadUtils.getVitamSession().getRequestId();
            Assert.assertEquals(MDC.get(GlobalDataRest.X_REQUEST_ID), reqId); // Check that the logging framework is working
            return reqId == null ? NO_REQUEST_ID_FOUND : reqId;
        }

        @GET
        @Path("/setRequestIdInResponse")
        public String setRequestIdInResponse() throws VitamThreadAccessException {
            VitamThreadUtils.getVitamSession().setRequestId("id-from-server-2");
            return "";
        }

    }
}