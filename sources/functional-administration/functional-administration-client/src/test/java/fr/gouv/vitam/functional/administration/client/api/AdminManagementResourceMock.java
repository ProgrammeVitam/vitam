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
package fr.gouv.vitam.functional.administration.client.api;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;

@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
public class AdminManagementResourceMock extends ApplicationStatusResource {

    private static final String AUDIT_URI = "/audit";
    private static final String AUDIT_RULE_URI = "/auditRule";

    private final ResteasyTestApplication.ExpectedResults mock;

    public AdminManagementResourceMock(ResteasyTestApplication.ExpectedResults mock) {
        this.mock = mock;
    }

    @Path("format/check")
    @POST
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_JSON)
    public Response checkFormat(InputStream xmlPronom) {
        consumeAndCloseStream(xmlPronom);
        return mock.post();
    }

    protected void consumeAndCloseStream(InputStream xmlPronom) {
        try {
            if (null != xmlPronom) {
                while (xmlPronom.read() > 0) {}
                xmlPronom.close();
            }
        } catch (IOException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    @Path("format/import")
    @POST
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_JSON)
    public Response importFormat(@Context HttpHeaders headers, InputStream xmlPronom) {
        consumeAndCloseStream(xmlPronom);
        return mock.post();
    }

    @GET
    @Path("format/{id_format:.+}")
    @Produces(APPLICATION_JSON)
    public Response findFileFormatByID(@PathParam("id_format") String formatId, @Context Request request) {
        return mock.get();
    }

    @Path("format/document")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response findFormats(JsonNode select) {
        return mock.post();
    }

    @Path("rules/check")
    @POST
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_OCTET_STREAM)
    public Response checkRulesFile(InputStream rulesStream) {
        ParametersChecker.checkParameter("rulesStream is a mandatory parameter", rulesStream);
        consumeAndCloseStream(rulesStream);
        return mock.post();
    }

    @Path("rules/import")
    @POST
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_JSON)
    public Response importRulesFile(@Context HttpHeaders headers, InputStream rulesStream) {
        consumeAndCloseStream(rulesStream);
        return mock.post();
    }

    @GET
    @Path("rules/{id_rule}")
    @Produces(APPLICATION_JSON)
    public Response findRuleByID(@PathParam("id_rule") String ruleId, @Context Request request) {
        return mock.get();
    }

    @Path("rules/document")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response findDocumentRules(JsonNode select) {
        return mock.post();
    }

    @Path("accession-register")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response createAccessionRegister(AccessionRegisterDetailModel accessionRegister) {
        return mock.post();
    }

    @Path("accession-register/document")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response findDocumentFundsRegister(JsonNode select) {
        return mock.post();
    }

    @Path("accession-register/detail/{id}")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response findDetailAccessionRegister(@PathParam("id") String documentId, JsonNode select) {
        return mock.post();
    }

    @Path(AUDIT_RULE_URI)
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response launchRuleAudit() {
        return mock.post();
    }

    @Path(AUDIT_URI)
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response launchAudit(JsonNode options) {
        return mock.post();
    }

    @POST
    @Path("accession-register/symbolic")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response createAccessionRegisterSymbolic(List<Integer> tenants) {
        return mock.post();
    }

    @GET
    @Path("accession-register/symbolic")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response getAccessionRegisterSymbolic(JsonNode queryDsl) {
        return mock.get();
    }

    @Path("/forcepause")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response forcePause(ProcessPause info) {
        return mock.post();
    }

    @Path("/removeforcepause")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeForcePause(ProcessPause info) {
        return mock.post();
    }

    @Path("/logbookoperations")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createExternalOperation(LogbookOperationParameters logbook) {
        return mock.post();
    }
}
