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
package fr.gouv.vitam.access.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import fr.gouv.vitam.access.api.AccessResource;

/**
 * Using the Mock Class in order to simulate Access Client Resource if config file does not exist
 *
 */
@Path("/accessMock")
@Consumes("application/json")
@Produces("application/json")
@javax.ws.rs.ApplicationPath("webresources")
public class AccessResourceMock implements AccessResource {

    /**
     * Empty Constructor
     */
    public AccessResourceMock() {
        // Empty Constructor
    }


    /**
     * get status
     */
    @Override
    @GET
    @Path("/status")
    public Response getStatus() {
        return Response.status(200).entity("OK_MockStatus").build();
    }

    /**
     * get units list
     */
    @Override
    @POST
    @Path("/units")
    public Response getUnits(String requestDsl, @HeaderParam("X-HTTP-Method-Override") String headerParam) {
        return Response.status(200).entity("{\"unit\" = \"OK_MockUnits\"}").build();
    }
}
