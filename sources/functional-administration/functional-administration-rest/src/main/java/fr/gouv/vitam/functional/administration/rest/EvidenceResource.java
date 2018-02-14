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
package fr.gouv.vitam.functional.administration.rest;

import com.google.common.base.Strings;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.evidence.EvidenceService;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/admin")
public class EvidenceResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EvidenceResource.class);
    private static final String UNIT_IS_MANDATORY = "unit is mandatory";
    private static final String MISSING_THE_TENANT_ID_X_TENANT_ID =
        "Missing the tenant ID (X-Tenant-Id) or wrong object Type";

    /**
     * Evidence service
     */
    private EvidenceService evidenceService = new EvidenceService();

    /**
     * String unitId
     */
    @POST
    @Path("/evidenceAudit/unit/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkUnitEvidenceAudit(@PathParam("id") String unitId,
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        ParametersChecker.checkParameter(UNIT_IS_MANDATORY, unitId);
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Integer tenantId = Integer.parseInt(xTenantId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        evidenceService.launchEvidence(unitId, LifeCycleTraceabilitySecureFileObject.MetadataType.UNIT);

        return Response.status(Response.Status.OK).build();
    }

    /**
     * String objectGroupId
     */
    @POST
    @Path("/evidenceAudit/objectgroup/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkObjectGroupEvidenceAudit(@PathParam("id") String objectGroupId,
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        ParametersChecker.checkParameter(UNIT_IS_MANDATORY, objectGroupId);
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Integer tenantId = Integer.parseInt(xTenantId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        evidenceService.launchEvidence(objectGroupId, LifeCycleTraceabilitySecureFileObject.MetadataType.OBJECTGROUP);

        return Response.status(Response.Status.OK).build();
    }
}
