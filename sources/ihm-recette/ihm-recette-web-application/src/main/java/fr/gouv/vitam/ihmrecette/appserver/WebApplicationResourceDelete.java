/**
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
 */
package fr.gouv.vitam.ihmrecette.appserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server2.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server2.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server2.application.resources.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessFactory;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessImpl;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.MongoDbAccessMetadataFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;

/**
 * Web Application Resource class for delete features
 */
@Path("/v1/api/delete")
public class WebApplicationResourceDelete extends ApplicationStatusResource {

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
    private static final String STP_DELETE_ALL = "STP_DELETE_ALL";
    private static final String CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION = "Cannot update delegate logbook operation";

    private final MongoDbAccessAdminImpl mongoDbAccessAdmin;
    private final LogbookMongoDbAccessImpl mongoDbAccessLogbook;
    private final MongoDbAccessMetadataImpl mongoDbAccessMetadata;

    /**
     * Default constructor
     *
     * @param webApplicationConfig application configuration
     */
    public WebApplicationResourceDelete(WebApplicationConfig webApplicationConfig) {
        super(new BasicVitamStatusServiceImpl());
        DbConfigurationImpl adminConfiguration;
        DbConfigurationImpl logbookConfiguration;
        MetaDataConfiguration metaDataConfiguration;
        if (webApplicationConfig.isDbAuthentication()) {
            adminConfiguration =
                new DbConfigurationImpl(webApplicationConfig.getMongoDbNodes(), webApplicationConfig.getMasterdataDbName(),
                    true, webApplicationConfig.getDbUserName(), webApplicationConfig.getDbPassword());
            logbookConfiguration =
                new DbConfigurationImpl(webApplicationConfig.getMongoDbNodes(), webApplicationConfig.getLogbookDbName(),
                    true, webApplicationConfig.getDbUserName(), webApplicationConfig.getDbPassword());
            metaDataConfiguration = new MetaDataConfiguration(webApplicationConfig.getMongoDbNodes(),
                webApplicationConfig.getMetadataDbName(), webApplicationConfig.getClusterName(), webApplicationConfig
                .getElasticsearchNodes(), true, webApplicationConfig.getDbUserName(), webApplicationConfig
                .getDbPassword());
        } else {
            adminConfiguration =
                new DbConfigurationImpl(webApplicationConfig.getMongoDbNodes(), webApplicationConfig.getMasterdataDbName());
            logbookConfiguration =
                new DbConfigurationImpl(webApplicationConfig.getMongoDbNodes(), webApplicationConfig.getLogbookDbName());
            metaDataConfiguration = new MetaDataConfiguration(webApplicationConfig.getMongoDbNodes(),
                webApplicationConfig.getMetadataDbName(), webApplicationConfig.getClusterName(), webApplicationConfig
                .getElasticsearchNodes());
        }
        mongoDbAccessAdmin = MongoDbAccessAdminFactory.create(adminConfiguration);
        mongoDbAccessLogbook = LogbookMongoDbAccessFactory.create(logbookConfiguration);
        mongoDbAccessMetadata = MongoDbAccessMetadataFactory.create(metaDataConfiguration);
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
        final GUID eip = GUIDFactory.newGUID();
        LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters(
            eip, STP_DELETE_FORMAT, eip,
            LogbookTypeProcess.DATA_MANAGEMENT, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_FORMAT, StatusCode.STARTED), eip);
        LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.FORMATS);
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_FORMAT).setStatus(StatusCode
                .OK).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_FORMAT, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(helper.removeCreateDelegate(eip.getId()).toArray(new
                LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (Exception exc) {
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_FORMAT).setStatus(StatusCode
                .KO).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_FORMAT, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException e) {
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
        final GUID eip = GUIDFactory.newGUID();
        LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters(
            eip, STP_DELETE_RULES, eip,
            LogbookTypeProcess.DATA_MANAGEMENT, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_RULES, StatusCode.STARTED), eip);
        LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.RULES);
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_RULES).setStatus(StatusCode
                .OK).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_RULES, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(helper.removeCreateDelegate(eip.getId()).toArray(new
                LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (Exception exc) {
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_RULES).setStatus(StatusCode
                .KO).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_RULES, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException e) {
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
        // Summary
        final GUID eip = GUIDFactory.newGUID();
        LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters(
            eip, STP_DELETE_ACCESSION_REGISTER_SUMMARY, eip,
            LogbookTypeProcess.DATA_MANAGEMENT, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_ACCESSION_REGISTER_SUMMARY, StatusCode.STARTED), eip);
            LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY);
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_ACCESSION_REGISTER_SUMMARY)
                .setStatus(StatusCode
                    .OK).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_ACCESSION_REGISTER_SUMMARY, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(helper.removeCreateDelegate(eip.getId()).toArray(new
                LogbookOperationParameters[2]));
        } catch (Exception exc) {
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_ACCESSION_REGISTER_SUMMARY).setStatus
                (StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages
                .getCodeOp(STP_DELETE_ACCESSION_REGISTER_SUMMARY, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException e) {
                LOGGER.error(CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION, e);
            }
            return updateLogbookAndGetErrorResponse(helper, eip, exc);
        }
        // Details
        final GUID eipDetail = GUIDFactory.newGUID();
        parameters = LogbookParametersFactory.newLogbookOperationParameters(
            eipDetail, STP_DELETE_ACCESSION_REGISTER_DETAIL, eipDetail,
            LogbookTypeProcess.DATA_MANAGEMENT, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_ACCESSION_REGISTER_DETAIL, StatusCode.STARTED), eipDetail);
        try {
            helper.createDelegate(parameters);
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL);
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_ACCESSION_REGISTER_DETAIL).setStatus(StatusCode
                .OK).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_ACCESSION_REGISTER_DETAIL, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(helper.removeCreateDelegate(eipDetail.getId()).toArray(new
                LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (Exception exc) {
            parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_ACCESSION_REGISTER_DETAIL).setStatus
                (StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages
                .getCodeOp(STP_DELETE_ACCESSION_REGISTER_DETAIL, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException e) {
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
        final GUID eip = GUIDFactory.newGUID();
        LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters(
            eip, STP_DELETE_LOGBOOK_OPERATION, eip,
            LogbookTypeProcess.DATA_MANAGEMENT, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_OPERATION, StatusCode.STARTED), eip);
        LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
        } catch (LogbookClientAlreadyExistsException exc) {
            LOGGER.error("Cannot create delegate logbook operation", exc);
        }
        try {
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.OPERATION);
            parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_LOGBOOK_OPERATION, StatusCode.OK)).setStatus(StatusCode.OK);
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(helper.removeCreateDelegate(eip.getId()).toArray(new
                LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (Exception exc) {
            parameters.setStatus(StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_LOGBOOK_OPERATION, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
                mongoDbAccessLogbook.createBulkLogbookOperation(helper.removeCreateDelegate(eip.getId()).toArray(new
                    LogbookOperationParameters[2]));
            } catch (LogbookAlreadyExistsException | LogbookDatabaseException e) {
                LOGGER.error(e);
            } catch (LogbookClientNotFoundException e) {
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
        final GUID eip = GUIDFactory.newGUID();
        LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters(
            eip, STP_DELETE_LOGBOOK_LIFECYCLE_OG, eip,
            LogbookTypeProcess.DATA_MANAGEMENT, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_OG, StatusCode.STARTED), eip);
        LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.LIFECYCLE_OBJECTGROUP);
            parameters.setStatus(StatusCode.OK).putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_OG, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(helper.removeCreateDelegate(eip.getId()).toArray(new
                LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (Exception exc) {
            parameters.setStatus(StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_OG, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException e) {
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
        final GUID eip = GUIDFactory.newGUID();
        LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters(
            eip, STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, eip,
            LogbookTypeProcess.DATA_MANAGEMENT, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, StatusCode.STARTED), eip);
        LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.LIFECYCLE_UNIT);
            parameters.setStatus(StatusCode.OK).putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(helper.removeCreateDelegate(eip.getId()).toArray(new
                LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (Exception exc) {
            parameters.setStatus(StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException e) {
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
        final GUID eip = GUIDFactory.newGUID();
        LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters(
            eip, STP_DELETE_METADATA_OG, eip,
            LogbookTypeProcess.DATA_MANAGEMENT, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_OG, StatusCode.STARTED), eip);
        LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
            mongoDbAccessMetadata.deleteObjectGroup();
            parameters.setStatus(StatusCode.OK).putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_OG, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(helper.removeCreateDelegate(eip.getId()).toArray(new
                LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (Exception exc) {
            parameters.setStatus(StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_OG, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException e) {
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
        final GUID eip = GUIDFactory.newGUID();
        LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters(
            eip, STP_DELETE_METADATA_UNIT, eip,
            LogbookTypeProcess.DATA_MANAGEMENT, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_UNIT, StatusCode.STARTED), eip);
        LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
            mongoDbAccessMetadata.deleteUnit();
            parameters.setStatus(StatusCode.OK).putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_UNIT, StatusCode.OK));
            helper.updateDelegate(parameters);
            mongoDbAccessLogbook.createBulkLogbookOperation(helper.removeCreateDelegate(eip.getId()).toArray(new
                LogbookOperationParameters[2]));
            return Response.status(Status.OK).build();
        } catch (Exception exc) {
            parameters.setStatus(StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(STP_DELETE_METADATA_UNIT, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException e) {
                LOGGER.error(CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION, e);
            }
            return updateLogbookAndGetErrorResponse(helper, eip, exc);
        }
    }

    /**
     * Delete all collection in database
     *
     * @return Response
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAll() {
        final GUID eip = GUIDFactory.newGUID();
        List<String> collectionKO = new ArrayList<>();
        LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters(
            eip, STP_DELETE_ALL, eip, LogbookTypeProcess.DATA_MANAGEMENT, StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_DELETE_ALL, StatusCode.STARTED), eip);
        LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        try {
            helper.createDelegate(parameters);
        } catch (LogbookClientAlreadyExistsException exc) {
            LOGGER.error("Cannot create delegate logbook operation", exc);
        }
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_METADATA_OG).setStatus(StatusCode
            .OK).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
            (STP_DELETE_METADATA_OG, StatusCode.OK));
        try {
            mongoDbAccessMetadata.deleteObjectGroup();
            helper.updateDelegate(parameters);
        } catch (Exception e) {
            parameters.setStatus(StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_METADATA_OG, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException exc) {
                LOGGER.error(CANNOT_UPDATE_DELEGATE_LOGBOOK_OPERATION, exc);
            }
            LOGGER.error(e);
            collectionKO.add(MetadataCollections.C_OBJECTGROUP.name());
        }
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_METADATA_UNIT).setStatus(StatusCode
            .OK).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
            (STP_DELETE_METADATA_UNIT, StatusCode.OK));
        try {
            mongoDbAccessMetadata.deleteUnit();
            helper.updateDelegate(parameters);
        } catch (Exception e) {
            parameters.setStatus(StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_METADATA_UNIT, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(MetadataCollections.C_UNIT.name());
        }
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_FORMAT).setStatus(StatusCode
            .OK).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
            (STP_DELETE_FORMAT, StatusCode.OK));
        try {
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.FORMATS);
            helper.updateDelegate(parameters);
        } catch (Exception e) {
            parameters.setStatus(StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_FORMAT, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.FORMATS.name());
        }
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_RULES).setStatus(StatusCode
            .OK).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
            (STP_DELETE_RULES, StatusCode.OK));
        try {
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.RULES);
            helper.updateDelegate(parameters);
        } catch (Exception e) {
            parameters.setStatus(StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_RULES, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.RULES.name());
        }
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_ACCESSION_REGISTER_SUMMARY).setStatus
            (StatusCode
            .OK).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
            (STP_DELETE_ACCESSION_REGISTER_SUMMARY, StatusCode.OK));
        try {
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY);
            helper.updateDelegate(parameters);
        } catch (Exception e) {
            parameters.setStatus(StatusCode
                .KO).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_ACCESSION_REGISTER_SUMMARY, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.name());
        }
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_ACCESSION_REGISTER_DETAIL).setStatus
            (StatusCode
            .OK).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
            (STP_DELETE_ACCESSION_REGISTER_DETAIL, StatusCode.OK));
        try {
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL);
            helper.updateDelegate(parameters);
        } catch (Exception e) {
            parameters.setStatus(StatusCode
                .KO).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_ACCESSION_REGISTER_DETAIL, StatusCode.KO));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.name());
        }
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_LOGBOOK_OPERATION).setStatus(StatusCode
            .OK).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
            (STP_DELETE_LOGBOOK_OPERATION, StatusCode.OK));
        try {
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.OPERATION);
            helper.updateDelegate(parameters);
        } catch (Exception e) {
            parameters.setStatus(StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_LOGBOOK_OPERATION, StatusCode.OK));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(LogbookCollections.OPERATION.name());
        }
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_LOGBOOK_LIFECYCLE_OG).setStatus(StatusCode
            .OK).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
            (STP_DELETE_LOGBOOK_LIFECYCLE_OG, StatusCode.OK));
        try {
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.LIFECYCLE_OBJECTGROUP);
            helper.updateDelegate(parameters);
        } catch (Exception e) {
            parameters.setStatus(StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_LOGBOOK_LIFECYCLE_OG, StatusCode.OK));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(LogbookCollections.LIFECYCLE_OBJECTGROUP.name());
        }
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_LOGBOOK_LIFECYCLE_UNIT).setStatus
            (StatusCode.OK).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
            (STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, StatusCode.OK));
        try {
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.LIFECYCLE_UNIT);
            helper.updateDelegate(parameters);
        } catch (Exception e) {
            parameters.setStatus(StatusCode.KO).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
                (STP_DELETE_LOGBOOK_LIFECYCLE_UNIT, StatusCode.OK));
            try {
                helper.updateDelegate(parameters);
            } catch (LogbookClientNotFoundException exc) {
                LOGGER.error("Cannot update delegate logbook operation", exc);
            }
            LOGGER.error(e);
            collectionKO.add(LogbookCollections.LIFECYCLE_UNIT.name());
        }
        parameters.putParameterValue(LogbookParameterName.eventType, STP_DELETE_ALL).setStatus(StatusCode
            .OK).putParameterValue(LogbookParameterName.outcomeDetailMessage, VitamLogbookMessages.getCodeOp
            (STP_DELETE_ALL, StatusCode.OK));
        try {
            helper.updateDelegate(parameters);
        } catch (LogbookClientNotFoundException exc) {
            LOGGER.error("Cannot update delegate logbook operation", exc);
        }
        try {
            Queue<LogbookOperationParameters> createQueue = helper.removeCreateDelegate(eip.getId());
            mongoDbAccessLogbook.createBulkLogbookOperation(createQueue.toArray(new
                LogbookOperationParameters[createQueue.size()]));
        } catch (LogbookDatabaseException | LogbookAlreadyExistsException e) {
            LOGGER.error("Error when logging", e);
        }
        if (collectionKO.isEmpty()) {
            return Response.status(Status.OK).build();
        } else {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(collectionKO).build();
        }
    }

    private Response updateLogbookAndGetErrorResponse(LogbookOperationsClientHelper helper, GUID eip, Exception exc) {
        try {
            Queue<LogbookOperationParameters> parameters = helper.removeCreateDelegate(eip.getId());
            mongoDbAccessLogbook.createBulkLogbookOperation(parameters.toArray(new
                LogbookOperationParameters[parameters.size()]));
        } catch (LogbookAlreadyExistsException | LogbookDatabaseException e) {
            LOGGER.error(e);
        }
        LOGGER.error(exc);
        final Status status = Status.INTERNAL_SERVER_ERROR;
        return Response.status(status).entity(exc.getMessage()).build();
    }
}
