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
package fr.gouv.vitam.functional.administration.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.function.administration.rules.core.RulesManagerFileImpl;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.format.core.ReferentialFormatFileImpl;

/**
 * FormatManagementResourceImpl implements AccessResource
 */
@Path("/adminmanagement/v1")
public class AdminManagementResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementResource.class);
    private ReferentialFormatFileImpl formatManagement;
    private RulesManagerFileImpl rulesFileManagement;

    /**
     * Constructor
     *
     * @param configuration config for constructing AdminManagement
     */
    public AdminManagementResource(AdminManagementConfiguration configuration) {
        formatManagement = new ReferentialFormatFileImpl(configuration);
        rulesFileManagement = new RulesManagerFileImpl(configuration);
        LOGGER.info("init Admin Management Resource server");
    }

    /**
     * Check the state of the admin management service API
     * 
     * @return an http response with OK status (200)
     */
    @Path("status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response status() {
        return Response.status(Status.OK).build();
    }

    /**
     * @param xmlPronom as InputStream
     * @return Response response jersey
     */
    @Path("format/check")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkFormat(InputStream xmlPronom) {
        ParametersChecker.checkParameter("xmlPronom is a mandatory parameter", xmlPronom);

        try {
            formatManagement.checkFile(xmlPronom);
        } catch (ReferentialException e) {
            LOGGER.error(e.getMessage());
            Status status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(status)
                .build();
        }
        return Response.status(Status.OK).build();
    }


    /**
     * @param xmlPronom as InputStream
     * @return Response jersey response
     */
    @Path("format/import")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importFormat(InputStream xmlPronom) {
        ParametersChecker.checkParameter("xmlPronom is a mandatory parameter", xmlPronom);
        try {
            formatManagement.importFile(xmlPronom);
        } catch (ReferentialException e) {
            LOGGER.error(e.getMessage());
            Status status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(status)
                .build();
        } catch (DatabaseConflictException e) {
            LOGGER.error(e);
            Status status = Status.CONFLICT;
            return Response.status(status)
                .entity(status)
                .build();
        }
        return Response.status(Status.OK).entity(Status.OK.name()).build();
    }

    /**
     * @return Response
     * @throws FileFormatException when delete exception
     */
    @Path("format/delete")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFormat() throws FileFormatException {
        formatManagement.deleteCollection();
        return Response.status(Status.OK).build();
    }

    /**
     * @param formatId path param as String
     * @return Response jersey response
     * @throws InvalidParseOperationException
     * @throws IOException when error json occurs
     */
    @POST
    @Path("format/{id_format}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findFileFormatByID(@PathParam("id_format") String formatId)
        throws InvalidParseOperationException, IOException {
        ParametersChecker.checkParameter("formatId is a mandatory parameter", formatId);
        FileFormat fileFormat = null;
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(formatId));
            fileFormat = formatManagement.findDocumentById(formatId);

            if (fileFormat == null) {
                throw new ReferentialException("NO DATA for the specified formatId");
            }

        } catch (ReferentialException e) {
            LOGGER.error(e.getMessage());
            Status status = Status.NOT_FOUND;
            return Response.status(status).build();
        }
        return Response.status(Status.OK).entity(JsonHandler.toJsonNode(fileFormat)).build();
    }

    /**
     * @param select as String
     * @return Response jersay Response
     * @throws IOException when error json occurs
     * @throws InvalidParseOperationException when error json occurs
     */
    @Path("format/document")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findDocument(JsonNode select)
        throws InvalidParseOperationException, IOException {
        ParametersChecker.checkParameter("select is a mandatory parameter", select);
        List<FileFormat> fileFormatList = new ArrayList<FileFormat>();
        try {
            SanityChecker.checkJsonAll(select);
            fileFormatList = formatManagement.findDocuments(select);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ReferentialException e) {
            LOGGER.error(e.getMessage());
            Status status = Status.NOT_FOUND;
            return Response.status(status).build();
        }

        return Response.status(Status.OK).entity(JsonHandler.getFromString(fileFormatListToJsonString(fileFormatList)))
            .build();
    }

    private String fileFormatListToJsonString(List<FileFormat> formatList)
        throws IOException {
        final OutputStream out = new ByteArrayOutputStream();
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(out, formatList);
        final byte[] data = ((ByteArrayOutputStream) out).toByteArray();
        final String fileFormatAsString = new String(data);
        return fileFormatAsString;
    }

    /***************************************** rules Manager *************************************/
    /**
     * @param rulesStream as InputStream
     * @return Response response jersey
     * @throws IOException
     */
    @Path("rules/check")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkRulesFile(InputStream rulesStream) throws IOException {
        ParametersChecker.checkParameter("rulesStream is a mandatory parameter", rulesStream);

        try {
            rulesFileManagement.checkFile(rulesStream);
        } catch (FileRulesException e) {
            LOGGER.error(e.getMessage());
            Status status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(status)
                .build();
        }
        return Response.status(Status.OK).build();
    }


    /**
     * @param rulesManager as InputStream
     * @return Response jersey response
     * @throws IOException when error json occurs
     * @throws InvalidParseOperationException when error json occurs
     * @throws ReferentialException
     */
    @Path("rules/import")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importRulesFile(InputStream rulesStream)
        throws InvalidParseOperationException, ReferentialException, IOException {
        ParametersChecker.checkParameter("rulesStream is a mandatory parameter", rulesStream);
        try {
            rulesFileManagement.importFile(rulesStream);
        } catch (FileRulesException e) {
            LOGGER.error(e.getMessage());
            Status status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(status)
                .build();
        } catch (DatabaseConflictException e) {
            LOGGER.error(e);
            Status status = Status.CONFLICT;
            return Response.status(status)
                .entity(status)
                .build();
        }
        return Response.status(Status.OK).entity(Status.OK.name()).build();
    }

    /**
     * @return Response
     * @throws FileRulesException when delete exception
     */
    @Path("rules/delete")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteRulesFile() throws FileRulesException {
        rulesFileManagement.deleteCollection();
        return Response.status(Status.OK).build();
    }

    /**
     * findRuleByID
     * 
     * @param rulesId path param as String
     * @return Response jersey response
     * @throws InvalidParseOperationException
     * @throws IOException when error json occurs
     * @throws ReferentialException
     */
    @POST
    @Path("rules/{id_rule}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findRuleByID(@PathParam("id_rule") String rulesId)
        throws InvalidParseOperationException, IOException,
        ReferentialException {
        ParametersChecker.checkParameter("ruleId is a mandatory parameter", rulesId);
        FileRules fileRules = null;
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(rulesId));
            fileRules = rulesFileManagement.findDocumentById(rulesId);

            if (fileRules == null) {
                throw new FileRulesException("NO DATA for the specified rulesId");
            }

        } catch (FileRulesException e) {
            LOGGER.error(e.getMessage());
            Status status = Status.NOT_FOUND;
            return Response.status(status).build();
        }
        return Response.status(Status.OK).entity(JsonHandler.toJsonNode(fileRules)).build();
    }

    /**
     * @param select as String
     * @return Response jersay Response
     * @throws IOException when error json occurs
     * @throws InvalidParseOperationException when error json occurs
     */
    @Path("rules/document")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findDocumentRules(JsonNode select)
        throws InvalidParseOperationException, IOException {
        ParametersChecker.checkParameter("select is a mandatory parameter", select);
        List<FileRules> filerulesList = new ArrayList<FileRules>();
        try {
            SanityChecker.checkJsonAll(select);
            filerulesList = rulesFileManagement.findDocuments(select);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ReferentialException e) {
            LOGGER.error(e.getMessage());
            Status status = Status.NOT_FOUND;
            return Response.status(status).build();
        }

        return Response.status(Status.OK).entity(JsonHandler.getFromString(fileRulesListToJsonString(filerulesList)))
            .build();
    }

    private String fileRulesListToJsonString(List<FileRules> rulesList)
        throws IOException {
        final OutputStream out = new ByteArrayOutputStream();
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(out, rulesList);
        final byte[] data = ((ByteArrayOutputStream) out).toByteArray();
        final String fileRulesAsString = new String(data);
        return fileRulesAsString;
    }

}
