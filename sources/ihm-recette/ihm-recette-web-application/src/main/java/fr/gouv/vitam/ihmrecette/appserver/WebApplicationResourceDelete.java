/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.ihmrecette.appserver;

import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.common.client.OntologyLoader;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.config.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessFactory;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessImpl;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.api.mapping.MappingLoader;
import fr.gouv.vitam.metadata.core.MongoDbAccessMetadataFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;


/**
 * Web Application Resource class for delete features
 */
// FIXME find a way to remove VitamSession from ihm-recette from mongoDbAccess
@Path("/v1/api/delete")
public class WebApplicationResourceDelete {

    private static final String CONTEXT_NAME = "Name";
    private static final String CONTEXT_TO_SAVE = "admin-context";
    private static final String SECURITY_PROFIL_NAME = "Name";
    private static final String SECURITY_PROFIL_NAME_TO_SAVE = "admin-security-profile";
    private static final String ONTOLOGY_ORIGIN = "Origin";
    private static final String ONTOLOGY_EXTERNAL = "EXTERNAL";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WebApplicationResourceDelete.class);
    private static final String STP_DELETE_FORMAT = "STP_DELETE_FORMAT";
    private static final String STP_DELETE_RULES = "STP_DELETE_RULES";
    private static final String STP_DELETE_ACCESSION_REGISTER_DETAIL = "STP_DELETE_ACCESSION_REGISTER_DETAIL";
    private static final String STP_DELETE_ACCESSION_REGISTER_SUMMARY = "STP_DELETE_ACCESSION_REGISTER_SUMMARY";
    private static final String STP_DELETE_LOGBOOK_OPERATION = "STP_DELETE_LOGBOOK_OPERATION";
    private static final String STP_DELETE_LOGBOOK_LIFECYCLE_OG = "STP_DELETE_LOGBOOK_LIFECYCLE_OG";
    private static final String STP_DELETE_LOGBOOK_LIFECYCLE_UNIT = "STP_DELETE_LOGBOOK_LIFECYCLE_UNIT";
    private static final String STP_DELETE_METADATA_OG = "STP_DELETE_METADATA_OG";
    private static final String STP_DELETE_METADATA_UNIT = "STP_DELETE_METADATA_UNIT";
    private static final String STP_DELETE_MASTERDATA = "STP_DELETE_MASTERDATA";
    private static final String STP_DELETE_MASTERDATA_INGEST_CONTRACT = "STP_DELETE_MASTERDATA_INGEST_CONTRACT";
    private static final String STP_DELETE_MASTERDATA_ACCESS_CONTRACT = "STP_DELETE_MASTERDATA_ACCESS_CONTRACT";
    private static final String STP_DELETE_MASTERDATA_MANAGEMENT_CONTRACT = "STP_DELETE_MASTERDATA_MANAGEMENT_CONTRACT";
    private static final String STP_DELETE_MASTERDATA_PROFILE = "STP_DELETE_MASTERDATA_PROFILE";
    private static final String STP_DELETE_MASTERDATA_ARCHIVE_UNIT_PROFILE =
        "STP_DELETE_MASTERDATA_ARCHIVE_UNIT_PROFILE";
    private static final String STP_DELETE_MASTERDATA_AGENCIES = "STP_DELETE_MASTERDATA_AGENCIES";
    private static final String STP_DELETE_MASTERDATA_CONTEXT = "STP_DELETE_MASTERDATA_CONTEXT";

    private static final String STP_DELETE_ALL = "STP_DELETE_ALL";
    private static final String CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION = "Cannot update delegate logbook operation";

    private final MongoDbAccessAdminImpl mongoDbAccessAdmin;
    private final LogbookMongoDbAccessImpl mongoDbAccessLogbook;
    private final MongoDbAccessMetadataImpl mongoDbAccessMetadata;
    private final UserInterfaceTransactionManager userInterfaceTransactionManager = UserInterfaceTransactionManager.getInstance();

    /**
     * Default constructor
     *
     * @param webApplicationConfig application configuration
     * @param ontologyLoader
     */
    public WebApplicationResourceDelete(WebApplicationConfig webApplicationConfig, OntologyLoader ontologyLoader) {
        DbConfigurationImpl adminConfiguration;
        LogbookConfiguration logbookConfiguration;
        MetaDataConfiguration metaDataConfiguration;
        MappingLoader mappingLoader;
        if (webApplicationConfig.isDbAuthentication()) {
            adminConfiguration =
                new DbConfigurationImpl(webApplicationConfig.getMongoDbNodes(),
                    webApplicationConfig.getMasterdataDbName(),
                    true, webApplicationConfig.getDbUserName(), webApplicationConfig.getDbPassword());
            logbookConfiguration =
                new LogbookConfiguration(webApplicationConfig.getMongoDbNodes(),
                    webApplicationConfig.getLogbookDbName(), webApplicationConfig.getClusterName(), webApplicationConfig
                    .getElasticsearchNodes(),
                    true, webApplicationConfig.getDbUserName(), webApplicationConfig.getDbPassword());
            mappingLoader = new MappingLoader(webApplicationConfig.getElasticsearchExternalMetadataMappings());
            metaDataConfiguration = new MetaDataConfiguration(webApplicationConfig.getMongoDbNodes(),
                webApplicationConfig.getMetadataDbName(), webApplicationConfig.getClusterName(), webApplicationConfig
                .getElasticsearchNodes(),
                true, webApplicationConfig.getDbUserName(), webApplicationConfig
                .getDbPassword(), mappingLoader);
        } else {
            adminConfiguration =
                new DbConfigurationImpl(webApplicationConfig.getMongoDbNodes(),
                    webApplicationConfig.getMasterdataDbName());
            logbookConfiguration =
                new LogbookConfiguration(webApplicationConfig.getMongoDbNodes(),
                    webApplicationConfig.getLogbookDbName(), webApplicationConfig.getClusterName(),
                    webApplicationConfig
                        .getElasticsearchNodes());
            mappingLoader = new MappingLoader(webApplicationConfig.getElasticsearchExternalMetadataMappings());
            metaDataConfiguration = new MetaDataConfiguration(webApplicationConfig.getMongoDbNodes(),
                webApplicationConfig.getMetadataDbName(), webApplicationConfig.getClusterName(), webApplicationConfig
                .getElasticsearchNodes(), mappingLoader);
        }
        mongoDbAccessAdmin = MongoDbAccessAdminFactory.create(adminConfiguration, webApplicationConfig.getClusterName(), webApplicationConfig.getElasticsearchNodes(), ontologyLoader);
        mongoDbAccessLogbook = LogbookMongoDbAccessFactory.create(logbookConfiguration, ontologyLoader);
        mongoDbAccessMetadata = MongoDbAccessMetadataFactory.create(metaDataConfiguration,mappingLoader);
        LOGGER.debug("init Admin Management Resource server");
    }

    /**
     * Get mongoDb access
     *
     * @return the mongoDb access
     */
    public MongoDbAccess getMongoDbAccessAdmin() {
        return mongoDbAccessAdmin;
    }

    /**
     * Delete the referential format in the base
     *
     * @return Response
     */
    @Path("formats")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFormat() {
        return deleteFormats();
    }

    private Response deleteFormats() {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        // FIXME tenantDefault for operation to replace with one from config
        if (tenantId == null) {
            VitamThreadUtils.getVitamSession().setTenantId(0);
            tenantId = 0;
        }
        final GUID eip = GUIDFactory.newEventGUID(tenantId);
        final LogbookOperationParameters parameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip, STP_DELETE_FORMAT, eip,
            LogbookTypeProcess.MASTERDATA, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_FORMAT, StatusCode.STARTED), eip);
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.FORMATS).close();
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_FORMAT).setStatus(StatusCode.OK)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_FORMAT, StatusCode.OK))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_FORMAT, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(
                helper.removeCreateDelegate(eip.getId()).toArray(new LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (final Exception exc) {
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_FORMAT).setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_FORMAT, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_FORMAT, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException e) {
                LOGGER.error(CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION, e);
            }
            return updateLogbookAndGetErrorResponse(helper, eip, exc);
        }
    }

    /**
     * Delete the referential rules in the base
     *
     * @return Response
     */
    @Path("rules")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteRulesFile() {
        return deleteRules();
    }

    private Response deleteRules() {
        Integer tenantId = ParameterHelper.getTenantParameter();
        final GUID eip = GUIDFactory.newEventGUID(tenantId);
        final LogbookOperationParameters parameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip, STP_DELETE_RULES, eip,
            LogbookTypeProcess.MASTERDATA, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_RULES, StatusCode.STARTED), eip);
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.RULES).close();
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_RULES).setStatus(StatusCode.OK)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_RULES, StatusCode.OK))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_RULES, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(
                helper.removeCreateDelegate(eip.getId()).toArray(new LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (final Exception exc) {
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_RULES).setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_RULES, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_RULES, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException e) {
                LOGGER.error(CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION, e);
            }
            return updateLogbookAndGetErrorResponse(helper, eip, exc);
        }
    }

    /**
     * Delete the referential accession register in database
     *
     * @return Response
     */
    @Path("accessionregisters")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAccessionRegister() {
        return deleteRegister();
    }

    private Response deleteRegister() {
        Integer tenantId = ParameterHelper.getTenantParameter();
        final GUID eip = GUIDFactory.newOperationLogbookGUID(tenantId);
        LogbookOperationParameters parameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip, STP_DELETE_ACCESSION_REGISTER_SUMMARY, eip,
            LogbookTypeProcess.MASTERDATA, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_ACCESSION_REGISTER_SUMMARY, StatusCode.STARTED), eip);
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();

        try {
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC).close();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(e.getMessage())
                .build();
        }

        try {
            helper.createDelegate(parameters);
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY).close();
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_ACCESSION_REGISTER_SUMMARY)
                .setStatus(StatusCode.OK)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_ACCESSION_REGISTER_SUMMARY, StatusCode.OK))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_ACCESSION_REGISTER_SUMMARY, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(
                helper.removeCreateDelegate(eip.getId()).toArray(new LogbookOperationParameters[2]));
        } catch (final Exception exc) {
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_ACCESSION_REGISTER_SUMMARY)
                .setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_ACCESSION_REGISTER_SUMMARY, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_ACCESSION_REGISTER_SUMMARY, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException e) {
                LOGGER.error(CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION, e);
            }
            return updateLogbookAndGetErrorResponse(helper, eip, exc);
        }
        // Details
        final GUID eipDetail = GUIDFactory.newGUID();
        parameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipDetail, STP_DELETE_ACCESSION_REGISTER_DETAIL, eipDetail,
            LogbookTypeProcess.MASTERDATA, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_ACCESSION_REGISTER_DETAIL, StatusCode.STARTED), eipDetail);
        try {
            helper.createDelegate(parameters);
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL).close();
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_ACCESSION_REGISTER_DETAIL)
                .setStatus(StatusCode.OK)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_ACCESSION_REGISTER_DETAIL, StatusCode.OK))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_ACCESSION_REGISTER_DETAIL, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(
                helper.removeCreateDelegate(eipDetail.getId()).toArray(new LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (final Exception exc) {
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_ACCESSION_REGISTER_DETAIL)
                .setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_ACCESSION_REGISTER_DETAIL, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_ACCESSION_REGISTER_DETAIL, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException e) {
                LOGGER.error(CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION, e);
            }
            return updateLogbookAndGetErrorResponse(helper, eipDetail, exc);
        }
    }

    /**
     * Delete the logbook operation in database
     *
     * @return Response
     */
    @Path("logbook/operation")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteLogbookOperation() {
        return deleteLogBook();
    }

    private Response deleteLogBook() {
        Integer tenantId = ParameterHelper.getTenantParameter();
        final GUID eip = GUIDFactory.newOperationLogbookGUID(tenantId);
        final LogbookOperationParameters parameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip, STP_DELETE_LOGBOOK_OPERATION, eip,
            LogbookTypeProcess.MASTERDATA, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_OPERATION, StatusCode.STARTED), eip);
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
        } catch (final LogbookClientAlreadyExistsException exc) {
            LOGGER.error("Cannot create delegate logbook operation", exc);
        }
        try {
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.OPERATION);
            parameters
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_LOGBOOK_OPERATION, StatusCode.OK))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_OPERATION, StatusCode.OK))
                .setStatus(StatusCode.OK);
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(
                helper.removeCreateDelegate(eip.getId()).toArray(new LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (final Exception exc) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_LOGBOOK_OPERATION, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_OPERATION, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
                mongoDbAccessLogbook.createBulkLogbookOperation(
                    helper.removeCreateDelegate(eip.getId()).toArray(new LogbookOperationParameters[2]));
            } catch (LogbookAlreadyExistsException | LogbookDatabaseException e) {
                LOGGER.error(e);
            } catch (final LogbookClientNotFoundException e) {
                LOGGER.error(CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION, e);
            }
            LOGGER.error(exc);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(exc.getMessage()).build();
        }
    }

    /**
     * Delete the logbook lifecyle for objectgroup in database
     *
     * @return Response
     */
    @Path("logbook/lifecycle/objectgroup")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteLogbookLifecycleOG() {
        return deleteLifecycleOg();
    }

    private Response deleteLifecycleOg() {
        Integer tenantId = ParameterHelper.getTenantParameter();
        final GUID eip = GUIDFactory.newOperationLogbookGUID(tenantId);
        final LogbookOperationParameters parameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip, STP_DELETE_LOGBOOK_LIFECYCLE_OG, eip,
            LogbookTypeProcess.MASTERDATA, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_OG, StatusCode.STARTED), eip);
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.LIFECYCLE_OBJECTGROUP);
            parameters.setStatus(StatusCode.OK)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_LOGBOOK_LIFECYCLE_OG, StatusCode.OK))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_OG, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(
                helper.removeCreateDelegate(eip.getId()).toArray(new LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (final Exception exc) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_LOGBOOK_LIFECYCLE_OG, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_OG, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException e) {
                LOGGER.error(CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION, e);
            }
            return updateLogbookAndGetErrorResponse(helper, eip, exc);
        }
    }

    /**
     * Delete the logbook lifecycle for unit in database
     *
     * @return Response
     */
    @Path("logbook/lifecycle/unit")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteLogbookLifecycleUnit() {
        return deleteLifecycleUnits();
    }

    private Response deleteLifecycleUnits() {
        Integer tenantId = ParameterHelper.getTenantParameter();
        final GUID eip = GUIDFactory.newOperationLogbookGUID(tenantId);
        final LogbookOperationParameters parameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip, STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, eip,
            LogbookTypeProcess.MASTERDATA, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, StatusCode.STARTED), eip);
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.LIFECYCLE_UNIT);
            parameters.setStatus(StatusCode.OK)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, StatusCode.OK))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(
                helper.removeCreateDelegate(eip.getId()).toArray(new LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (final Exception exc) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException e) {
                LOGGER.error(CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION, e);
            }
            return updateLogbookAndGetErrorResponse(helper, eip, exc);
        }
    }

    /**
     * Delete the metadata for objectgroup in database
     *
     * @return Response
     */
    @Path("metadata/objectgroup")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMetadataObjectGroup() {
        return deleteMetadataOg();
    }

    private Response deleteMetadataOg() {
        Integer tenantId = ParameterHelper.getTenantParameter();
        final GUID eip = GUIDFactory.newOperationLogbookGUID(tenantId);
        final LogbookOperationParameters parameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip, STP_DELETE_METADATA_OG, eip,
            LogbookTypeProcess.MASTERDATA, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_OG, StatusCode.STARTED), eip);
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
            mongoDbAccessMetadata.deleteObjectGroupByTenant(tenantId);
            parameters.setStatus(StatusCode.OK)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_METADATA_OG, StatusCode.OK))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_OG, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(
                helper.removeCreateDelegate(eip.getId()).toArray(new LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (final Exception exc) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_METADATA_OG, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_OG, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException e) {
                LOGGER.error(CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION, e);
            }
            return updateLogbookAndGetErrorResponse(helper, eip, exc);
        }
    }

    /**
     * Delete the metadata for unit in database
     *
     * @return Response
     */
    @Path("metadata/unit")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMetadataUnit() {
        return deleteMetadataUnits();
    }

    private Response deleteMetadataUnits() {
        Integer tenantId = ParameterHelper.getTenantParameter();
        final GUID eip = GUIDFactory.newOperationLogbookGUID(tenantId);
        final LogbookOperationParameters parameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip, STP_DELETE_METADATA_UNIT, eip,
            LogbookTypeProcess.MASTERDATA, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_UNIT, StatusCode.STARTED), eip);
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
            mongoDbAccessMetadata.deleteUnitByTenant(tenantId);
            parameters.setStatus(StatusCode.OK)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_METADATA_UNIT, StatusCode.OK))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_UNIT, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(
                helper.removeCreateDelegate(eip.getId()).toArray(new LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (final Exception exc) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_METADATA_UNIT, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_UNIT, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException e) {
                LOGGER.error(CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION, e);
            }
            return updateLogbookAndGetErrorResponse(helper, eip, exc);
        }
    }

    /**
     * Delete the masterdata for accessContract in database
     *
     * @return Response
     */
    @Path("masterdata/accessContract")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMasterdaAccessContract() {
        return deleteMasterDataCollection(FunctionalAdminCollections.ACCESS_CONTRACT);

    }

    /**
     * Delete the masterdata for ingestContract in database
     *
     * @return Response
     */
    @Path("masterdata/ingestContract")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMasterdaIngestContract() {
        return deleteMasterDataCollection(FunctionalAdminCollections.INGEST_CONTRACT);

    }

    /**
     * Delete the masterdata for managementContract in database
     *
     * @return Response
     */
    @Path("masterdata/managementContract")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMasterdaManagementContract() {
        return deleteMasterDataCollection(FunctionalAdminCollections.MANAGEMENT_CONTRACT);

    }

    /**
     * Delete the masterdata for profile in database
     *
     * @return Response
     */
    @Path("masterdata/profile")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMasterdataProfile() {
        return deleteMasterDataCollection(FunctionalAdminCollections.PROFILE);

    }

    /**
     * Delete the masterdata for archive unit profile in database
     *
     * @return Response
     */
    @Path("masterdata/archiveUnitProfile")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMasterdataArchiveUnitProfile() {
        return deleteMasterDataCollection(FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE);
    }

    /**
     * Delete the masterdata for agencies in database
     *
     * @return Response
     */
    @Path("masterdata/agencies")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMasterdataAgencies() {
        return deleteMasterDataCollection(FunctionalAdminCollections.AGENCIES);
    }


    /**
     * Delete all entries for the context collection except the "admin" context
     *
     * @return Response
     */
    @Path("masterdata/context")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMasterdataContext() {
        return deleteMasterDataCollection(FunctionalAdminCollections.CONTEXT);
    }

    /**
     * Delete all entries for the context collection except the "admin" context
     *
     * @return Response
     */
    @Path("masterdata/securityProfil")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMasterdataSecurityProfil() {
        Delete delete = null;

        try {
            delete = queryDeleteSecurityProfil();

            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.SECURITY_PROFILE, delete);

            return Response.status(Status.OK).build();
        } catch (InvalidCreateOperationException | DatabaseException | ReferentialException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    /**
     * Delete the EXTERNAL entries for the ontology collection and reimport the INTERNAL entries
     *
     * @return Response
     */
    @Path("masterdata/ontologies")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAndImportOntologies(@Context HttpServletRequest request) throws IOException {

        //Delete the EXTERNAL ontologies
        Response response = deleteMasterDataCollection(FunctionalAdminCollections.ONTOLOGY);

        //recreate the internals ontologies
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {

            InputStream input = this.getClass().getResourceAsStream("/VitamOntology.json");
            VitamContext context = userInterfaceTransactionManager.getVitamContext(request);
            context.setTenantId(1);
            RequestResponse requestResponse =
                adminClient.importOntologies(true, context, input);
            if (requestResponse.isOk()) {
                return Response.status(Status.OK).build();
            }
            if (requestResponse instanceof VitamError) {
                final VitamError error = (VitamError) requestResponse;
                return Response.status(error.getHttpCode()).entity(requestResponse).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();

        } catch (final Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("masterdata/griffins")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGriffins(@Context HttpServletRequest request) throws IOException {

        deleteMasterDataCollection(FunctionalAdminCollections.GRIFFIN);

        return Response.status(Status.OK).build();
    }

    @Path("masterdata/scenarios")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteScenarios(@Context HttpServletRequest request) throws IOException {
        deleteMasterDataCollection(FunctionalAdminCollections.PRESERVATION_SCENARIO);

        return Response.status(Status.OK).build();
    }

    private Response deleteMasterDataCollection(FunctionalAdminCollections collection) {
        if (!(collection.equals(FunctionalAdminCollections.ACCESS_CONTRACT) ||
            collection.equals(FunctionalAdminCollections.INGEST_CONTRACT) ||
            collection.equals(FunctionalAdminCollections.MANAGEMENT_CONTRACT) ||
            collection.equals(FunctionalAdminCollections.PROFILE) ||
            collection.equals(FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE) ||
            collection.equals(FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC) ||
            collection.equals(FunctionalAdminCollections.ONTOLOGY) ||
            collection.equals(FunctionalAdminCollections.AGENCIES) ||
            collection.equals(FunctionalAdminCollections.GRIFFIN) ||
            collection.equals(FunctionalAdminCollections.PRESERVATION_SCENARIO) ||
            collection.equals(FunctionalAdminCollections.CONTEXT))) {
            throw new IllegalArgumentException("unsupported collection");
        }
        boolean fakeTenant = false;
        if (FunctionalAdminCollections.CONTEXT.equals(collection)) {
            // HACK: context is a multi tenant collection, so it is possible to not have tenant in session when
            // delete them
            if (VitamThreadUtils.getVitamSession().getTenantId() == null) {
                fakeTenant = true;
                VitamThreadUtils.getVitamSession().setTenantId(0);
            }
        }

        final GUID eip = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters parameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip, STP_DELETE_MASTERDATA + "_" + collection.name(), eip,
            LogbookTypeProcess.MASTERDATA, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA + "_" + collection.name(), StatusCode.STARTED), eip);
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
            if (FunctionalAdminCollections.CONTEXT.equals(collection)) {
                // HACK: CT-0001, admin-context have not to be deleted
                mongoDbAccessAdmin.deleteCollection(collection, queryDeleteContext()).close();
            } else if (FunctionalAdminCollections.ONTOLOGY.equals(collection)) {
                mongoDbAccessAdmin.deleteCollection(collection, queryDeleteExternalOntology()).close();
            } else {
                mongoDbAccessAdmin.deleteCollection(collection).close();
            }
            parameters.setStatus(StatusCode.OK)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages
                        .getOutcomeDetail(STP_DELETE_MASTERDATA + "_" + collection.name(), StatusCode.OK))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA + "_" + collection.name(), StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(
                helper.removeCreateDelegate(eip.getId()).toArray(new LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (final Exception exc) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages
                        .getOutcomeDetail(STP_DELETE_MASTERDATA + "_" + collection.name(), StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA + "_" + collection.name(), StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException e) {
                LOGGER.error(CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION, e);
            }
            return updateLogbookAndGetErrorResponse(helper, eip, exc);
        } finally {
            // HACK: context is a multi tenant collection, rollback tenantID
            if (fakeTenant) {
                VitamThreadUtils.getVitamSession().setTenantId(null);
            }
        }

    }

    /**
     * @deprecated Delete for tnr use only for tnr
     */
    @Path("deleteTnr")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response purgeDataForTnr() {

        deleteLogBook().close();

        deleteMasterDataCollection(FunctionalAdminCollections.AGENCIES).close();

        deleteMasterDataCollection(FunctionalAdminCollections.INGEST_CONTRACT).close();

        deleteMasterDataCollection(FunctionalAdminCollections.ACCESS_CONTRACT).close();

        deleteMasterDataCollection(FunctionalAdminCollections.MANAGEMENT_CONTRACT).close();

        deleteMasterDataCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC).close();

        deleteLifecycleUnits().close();

        deleteLifecycleOg().close();

        deleteMetadataOg().close();

        deleteMetadataUnits().close();

        deleteAccessionRegister().close();

        deleteRules().close();

        deleteFormats().close();

        deleteMasterdataProfile().close();

        deleteMasterdataArchiveUnitProfile().close();

        deleteMasterdataContext().close();

        deleteMasterdataSecurityProfil().close();

        return Response.status(Status.OK).build();
    }


    /**
     * Delete all collection in database
     *
     * @return Response
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAll() {
        Integer tenantId = ParameterHelper.getTenantParameter();
        final GUID eip = GUIDFactory.newOperationLogbookGUID(tenantId);
        final List<String> collectionKO = new ArrayList<>();
        final LogbookOperationParameters parameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip, STP_DELETE_ALL, eip, LogbookTypeProcess.MASTERDATA, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_ALL, StatusCode.STARTED), eip);
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
        } catch (final LogbookClientAlreadyExistsException exc) {
            LOGGER.error("Cannot create delegate logbook operation", exc);
        }

        deleteMetadaOg(tenantId, collectionKO, parameters, helper);

        deleteMetadaUnit(tenantId, collectionKO, parameters, helper);

        deleteRules(collectionKO, parameters, helper);

        deleteAccessionRegisterSummary(collectionKO, parameters, helper);

        deleteAccessionRegister(collectionKO, parameters, helper);

        deleteLogbookoperations(collectionKO, parameters, helper);

        deleteLogbookLifeCyleOg(collectionKO, parameters, helper);

        deleteLogbookLifeCycles(collectionKO, parameters, helper);

        deleteProfils(collectionKO, parameters, helper);

        deleteArchiveUnitProfils(collectionKO, parameters, helper);

        deleteAgencies(collectionKO, parameters, helper);

        deleteIngestContracts(collectionKO, parameters, helper);

        deleteManagementContracts(collectionKO, parameters, helper);

        deleteAccessContracts(collectionKO, parameters, helper);

        deleteContext(collectionKO, parameters, helper);

        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_ALL).setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(STP_DELETE_ALL, StatusCode.OK))
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_ALL, StatusCode.OK));
        try {
            helper.updateDelegate(parameters);
        } catch (final LogbookClientNotFoundException exc) {
            LOGGER.error("Cannot update delegate logbook operation", exc);
        }
        try {
            final Queue<LogbookOperationParameters> createQueue = helper.removeCreateDelegate(eip.getId());
            mongoDbAccessLogbook
                .createBulkLogbookOperation(createQueue.toArray(new LogbookOperationParameters[createQueue.size()]));
        } catch (LogbookDatabaseException | LogbookAlreadyExistsException e) {
            LOGGER.error("Error when logging", e);
        }
        if (collectionKO.isEmpty()) {
            return Response.status(Status.OK).build();
        } else {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(collectionKO).build();
        }
    }

    public void deleteMetadaOg(Integer tenantId, List<String> collectionKO, LogbookOperationParameters parameters,
        LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_METADATA_OG).setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(STP_DELETE_METADATA_OG, StatusCode.OK))
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_OG, StatusCode.OK));
        try {
            mongoDbAccessMetadata.deleteObjectGroupByTenant(tenantId);
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_METADATA_OG, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_OG, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error(CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION, exc);
            }
            LOGGER.error(e);
            collectionKO.add(MetadataCollections.OBJECTGROUP.name());
        }
    }

    public void deleteMetadaUnit(Integer tenantId, List<String> collectionKO, LogbookOperationParameters parameters,
        LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_METADATA_UNIT).setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_UNIT, StatusCode.OK));
        try {
            mongoDbAccessMetadata.deleteUnitByTenant(tenantId);
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_UNIT, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(MetadataCollections.UNIT.name());
        }
    }

    public void deleteRules(List<String> collectionKO, LogbookOperationParameters parameters,
        LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_RULES).setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(STP_DELETE_RULES, StatusCode.OK))
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_RULES, StatusCode.OK));
        try (DbRequestResult result = mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.RULES)) {
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_RULES, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_RULES, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.RULES.name());
        }
    }

    public void deleteAccessionRegisterSummary(List<String> collectionKO, LogbookOperationParameters parameters,
        LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_ACCESSION_REGISTER_SUMMARY)
            .setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(STP_DELETE_ACCESSION_REGISTER_SUMMARY, StatusCode.OK))
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_ACCESSION_REGISTER_SUMMARY, StatusCode.OK));
        try (DbRequestResult result =
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY)) {
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_ACCESSION_REGISTER_SUMMARY, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_ACCESSION_REGISTER_SUMMARY, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.name());
        }
    }

    public void deleteAccessionRegister(List<String> collectionKO, LogbookOperationParameters parameters,
        LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_ACCESSION_REGISTER_DETAIL)
            .setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(STP_DELETE_ACCESSION_REGISTER_DETAIL, StatusCode.OK))
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_ACCESSION_REGISTER_DETAIL, StatusCode.OK));
        try (DbRequestResult result =
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL)) {
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_ACCESSION_REGISTER_DETAIL, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_ACCESSION_REGISTER_DETAIL, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.name());
        }
    }

    public void deleteLogbookoperations(List<String> collectionKO, LogbookOperationParameters parameters,
        LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_LOGBOOK_OPERATION)
            .setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(STP_DELETE_LOGBOOK_OPERATION, StatusCode.OK))
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_OPERATION, StatusCode.OK));
        try {
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.OPERATION);
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_LOGBOOK_OPERATION, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_OPERATION, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(LogbookCollections.OPERATION.name());
        }
    }

    private void deleteLogbookLifeCyleOg(List<String> collectionKO, LogbookOperationParameters parameters,
        LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_LOGBOOK_LIFECYCLE_OG)
            .setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(STP_DELETE_LOGBOOK_LIFECYCLE_OG, StatusCode.OK))
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_OG, StatusCode.OK));
        try {
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.LIFECYCLE_OBJECTGROUP);
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_LOGBOOK_LIFECYCLE_OG, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_OG, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(LogbookCollections.LIFECYCLE_OBJECTGROUP.name());
        }
    }

    private void deleteLogbookLifeCycles(List<String> collectionKO, LogbookOperationParameters parameters,
        LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_LOGBOOK_LIFECYCLE_UNIT)
            .setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, StatusCode.OK))
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, StatusCode.OK));
        try {
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.LIFECYCLE_UNIT);
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(LogbookCollections.LIFECYCLE_UNIT.name());
        }
    }

    private void deleteProfils(List<String> collectionKO, LogbookOperationParameters parameters,
        LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType,
            VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_PROFILE, StatusCode.OK))
            .setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(STP_DELETE_MASTERDATA_PROFILE, StatusCode.OK))
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_PROFILE, StatusCode.OK));
        try (DbRequestResult result = mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.PROFILE)) {
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_MASTERDATA_PROFILE, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_PROFILE, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.PROFILE.name());
        }
    }

    private void deleteArchiveUnitProfils(List<String> collectionKO, LogbookOperationParameters parameters,
        LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType,
            VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_ARCHIVE_UNIT_PROFILE, StatusCode.OK))
            .setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(STP_DELETE_MASTERDATA_ARCHIVE_UNIT_PROFILE, StatusCode.OK))
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_ARCHIVE_UNIT_PROFILE, StatusCode.OK));
        try (DbRequestResult result = mongoDbAccessAdmin
            .deleteCollection(FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE)) {
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_MASTERDATA_ARCHIVE_UNIT_PROFILE, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_ARCHIVE_UNIT_PROFILE, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE.name());
        }
    }

    private void deleteAgencies(List<String> collectionKO, LogbookOperationParameters parameters,
        LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType,
            VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_AGENCIES, StatusCode.OK))
            .setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(STP_DELETE_MASTERDATA_AGENCIES, StatusCode.OK))
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_AGENCIES, StatusCode.OK));
        try (DbRequestResult result = mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.AGENCIES)) {
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_MASTERDATA_AGENCIES, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_AGENCIES, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.AGENCIES.name());
        }
    }

    private void deleteIngestContracts(List<String> collectionKO, LogbookOperationParameters parameters,
        LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType,
            VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_INGEST_CONTRACT, StatusCode.OK))
            .setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(STP_DELETE_MASTERDATA_INGEST_CONTRACT, StatusCode.OK))
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_INGEST_CONTRACT, StatusCode.OK));
        try (DbRequestResult result = mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.INGEST_CONTRACT)) {
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_MASTERDATA_INGEST_CONTRACT, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_INGEST_CONTRACT, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.INGEST_CONTRACT.name());
        }
    }

    private void deleteAccessContracts(List<String> collectionKO, LogbookOperationParameters parameters,
        LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType,
            VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_ACCESS_CONTRACT, StatusCode.OK))
            .setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(STP_DELETE_MASTERDATA_ACCESS_CONTRACT, StatusCode.OK))
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_ACCESS_CONTRACT, StatusCode.OK));
        try (DbRequestResult result = mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESS_CONTRACT)) {
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_MASTERDATA_ACCESS_CONTRACT, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_ACCESS_CONTRACT, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.ACCESS_CONTRACT.name());
        }
    }

    private void deleteManagementContracts(List<String> collectionKO, LogbookOperationParameters parameters,
                                       LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType,
                VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_MANAGEMENT_CONTRACT, StatusCode.OK))
                .setStatus(StatusCode.OK)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                        VitamLogbookMessages.getOutcomeDetail(STP_DELETE_MASTERDATA_MANAGEMENT_CONTRACT, StatusCode.OK))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_MANAGEMENT_CONTRACT, StatusCode.OK));
        try (DbRequestResult result = mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESS_CONTRACT)) {
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO)
                    .putParameterValue(LogbookParameterName.outcomeDetail,
                            VitamLogbookMessages.getOutcomeDetail(STP_DELETE_MASTERDATA_MANAGEMENT_CONTRACT, StatusCode.KO))
                    .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                            VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_MANAGEMENT_CONTRACT, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.MANAGEMENT_CONTRACT.name());
        }
    }

    private void deleteContext(List<String> collectionKO, LogbookOperationParameters parameters,
        LogbookOperationsClientHelper helper) {
        parameters.putParameterValue(LogbookParameterName.eventType,
            VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_CONTEXT, StatusCode.OK))
            .setStatus(StatusCode.OK)
            .putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(STP_DELETE_MASTERDATA_CONTEXT, StatusCode.OK))
            .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_CONTEXT, StatusCode.OK));
        try {
            Delete delete = queryDeleteContext();
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.CONTEXT, delete);
            helper.updateDelegate(parameters);
        } catch (final Exception e) {
            parameters.setStatus(StatusCode.KO)
                .putParameterValue(LogbookParameterName.outcomeDetail,
                    VitamLogbookMessages.getOutcomeDetail(STP_DELETE_MASTERDATA_CONTEXT, StatusCode.KO))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(STP_DELETE_MASTERDATA_CONTEXT, StatusCode.OK));
            try {
                helper.updateDelegate(parameters);
            } catch (final LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.CONTEXT.name());
        }
    }

    private Delete queryDeleteContext() throws InvalidCreateOperationException {

        final Delete delete = new Delete();
        final Query query = QueryHelper.not().add(QueryHelper.eq(CONTEXT_NAME, CONTEXT_TO_SAVE));
        delete.setQuery(query);
        return delete;
    }

    private Delete queryDeleteSecurityProfil() throws InvalidCreateOperationException {

        final Delete delete = new Delete();
        final Query query = QueryHelper.not().add(QueryHelper.eq(SECURITY_PROFIL_NAME, SECURITY_PROFIL_NAME_TO_SAVE));
        delete.setQuery(query);
        return delete;
    }


    private Delete queryDeleteExternalOntology() throws InvalidCreateOperationException {
        final Delete delete = new Delete();
        final Query query = QueryHelper.eq(ONTOLOGY_ORIGIN, ONTOLOGY_EXTERNAL);
        delete.setQuery(query);
        return delete;
    }

    private Response updateLogbookAndGetErrorResponse(LogbookOperationsClientHelper helper, GUID eip, Exception exc) {
        try {
            final Queue<LogbookOperationParameters> parameters = helper.removeCreateDelegate(eip.getId());
            mongoDbAccessLogbook
                .createBulkLogbookOperation(parameters.toArray(new LogbookOperationParameters[parameters.size()]));
        } catch (LogbookAlreadyExistsException | LogbookDatabaseException e) {
            LOGGER.error(e);
        }
        LOGGER.error(exc);
        final Status status = Status.INTERNAL_SERVER_ERROR;
        return Response.status(status).entity(exc.getMessage()).build();
    }

}
