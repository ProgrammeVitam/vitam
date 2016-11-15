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

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server2.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server2.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server2.application.resources.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessFactory;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessImpl;
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
        try {
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.FORMATS);
            // TODO: add logbook entry
            return Response.status(Status.OK).build();
        } catch (DatabaseException exc) {
            // TODO: add logbook entry
            LOGGER.error(exc);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(exc.getMessage()).build();
        } catch (Exception e) {
            // TODO: add logbook entry
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
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
        try {
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.RULES);
            // TODO: add logbook entry
            return Response.status(Status.OK).build();
        } catch (DatabaseException exc) {
            // TODO: add logbook entry
            LOGGER.error(exc);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(exc.getMessage()).build();
        } catch (Exception e) {
            // TODO: add logbook entry
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
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
        try {
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY);
            // TODO: add logbook entry
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL);
            // TODO: add logbook entry
            return Response.status(Status.OK).build();
        } catch (DatabaseException exc) {
            // TODO: add logbook entry
            LOGGER.error(exc);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(exc.getMessage()).build();
        } catch (Exception e) {
            // TODO: add logbook entry
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
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
        try {
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.OPERATION);
            // TODO: add logbook entry
            return Response.status(Status.OK).build();
        } catch (DatabaseException exc) {
            // TODO: add logbook entry
            LOGGER.error(exc);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(exc.getMessage()).build();
        } catch (Exception e) {
            // TODO: add logbook entry
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(e.getMessage()).build();
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
        try {
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.LIFECYCLE_OBJECTGROUP);
            // TODO: add logbook entry
            return Response.status(Status.OK).build();
        } catch (DatabaseException exc) {
            // TODO: add logbook entry
            LOGGER.error(exc);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(exc.getMessage()).build();
        } catch (Exception e) {
            // TODO: add logbook entry
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
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
        try {
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.LIFECYCLE_UNIT);
            // TODO: add logbook entry
            return Response.status(Status.OK).build();
        } catch (DatabaseException exc) {
            // TODO: add logbook entry
            LOGGER.error(exc);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(exc.getMessage()).build();
        } catch (Exception e) {
            // TODO: add logbook entry
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
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
        try {
            mongoDbAccessMetadata.deleteObjectGroup();
            // TODO: add logbook entry
            return Response.status(Status.OK).build();
        } catch (DatabaseException exc) {
            // TODO: add logbook entry
            LOGGER.error(exc);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(exc.getMessage()).build();
        } catch (Exception e) {
            // TODO: add logbook entry
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
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
        try {
            mongoDbAccessMetadata.deleteUnit();
            // TODO: add logbook entry
            return Response.status(Status.OK).build();
        } catch (DatabaseException exc) {
            // TODO: add logbook entry
            LOGGER.error(exc);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(exc.getMessage()).build();
        } catch (Exception e) {
            // TODO: add logbook entry
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
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
        List<String> collectionKO = new ArrayList<>();
        // TODO: for each delete, add logbook entry (using logbook delagate to write on it)
        // TODO: also take into account DatabseException when collection had not been fully deleted
        try {
            mongoDbAccessMetadata.deleteObjectGroup();
        } catch (Exception e) {
            LOGGER.error(e);
            collectionKO.add(MetadataCollections.C_OBJECTGROUP.name());
        }
        try {
            mongoDbAccessMetadata.deleteUnit();
        } catch (Exception e) {
            LOGGER.error(e);
            collectionKO.add(MetadataCollections.C_UNIT.name());
        }
        try {
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.FORMATS);
        } catch (Exception e) {
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.FORMATS.name());
        }
        try {
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.RULES);
        } catch (Exception e) {
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.RULES.name());
        }
        try {
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY);
        } catch (Exception e) {
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.name());
        }
        try {
            mongoDbAccessAdmin.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL);
        } catch (Exception e) {
            LOGGER.error(e);
            collectionKO.add(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.name());
        }
        try {
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.OPERATION);
        } catch (Exception e) {
            LOGGER.error(e);
            collectionKO.add(LogbookCollections.OPERATION.name());
        }
        try {
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.LIFECYCLE_OBJECTGROUP);
        } catch (Exception e) {
            LOGGER.error(e);
            collectionKO.add(LogbookCollections.LIFECYCLE_OBJECTGROUP.name());
        }
        try {
            mongoDbAccessLogbook.deleteCollection(LogbookCollections.LIFECYCLE_UNIT);
        } catch (Exception e) {
            LOGGER.error(e);
            collectionKO.add(LogbookCollections.LIFECYCLE_UNIT.name());
        }
        // TODO: send logbook delegate
        if (collectionKO.isEmpty()) {
            return Response.status(Status.OK).build();
        } else {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(collectionKO).build();
        }
    }
}
