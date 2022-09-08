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
package fr.gouv.vitam.collect.internal.repository;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.model.ProjectModel;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

/**
 * repository for project entities  management in mongo.
 */
public class ProjectRepository {

    public static final String PROJECT_COLLECTION = "Project";
    public static final String ID = "_id";
    public static final String TENANT_ID = "_tenant";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProjectRepository.class);

    private final MongoCollection<Document> projectCollection;

    @VisibleForTesting
    public ProjectRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        projectCollection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
    }

    public ProjectRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, PROJECT_COLLECTION);
    }

    /**
     * create a project model
     *
     * @param projectModel project model to create
     * @throws CollectException exception thrown in case of error
     */
    public void createProject(ProjectModel projectModel) throws CollectException {
        LOGGER.debug("Project to create: {}", projectModel);
        try {
            String json = JsonHandler.writeAsString(projectModel);
            projectCollection.insertOne(Document.parse(json));
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when creating project : ", e);
            throw new CollectException("Error when creating project: " + e);
        }
    }

    /**
     * replace a project model
     *
     * @param projectModel project model to replace
     * @throws CollectException exception thrown in case of error
     */
    public void replaceProject(ProjectModel projectModel) throws CollectException {
        LOGGER.debug("Project to replace: {}", projectModel);
        try {
            String json = JsonHandler.writeAsString(projectModel);
            final Bson condition = and(eq(ID, projectModel.getId()));
            projectCollection.replaceOne(condition, Document.parse(json));
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when replacing project: ", e);
            throw new CollectException("Error when replacing project: " + e);
        }
    }

    /**
     * return project according to id
     *
     * @param id project id to find
     * @return Optional<ProjectModel>
     * @throws CollectException exception thrown in case of error
     */
    public Optional<ProjectModel> findProjectById(String id) throws CollectException {
        LOGGER.debug("Project id to find : {}", id);
        try {
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            Bson query = and(eq(ID, id), eq(TENANT_ID, tenantId));
            Document first = projectCollection.find(query).first();
            if (first == null) {
                return Optional.empty();
            }
            return Optional.of(BsonHelper.fromDocumentToObject(first, ProjectModel.class));
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when searching project by id: ", e);
            throw new CollectException("Error when searching project by id: " + e);
        }
    }

    /**
     * return project according to tenant
     *
     * @param tenant tenant id to find
     * @return Optional<ProjectModel>
     * @throws CollectException exception thrown in case of error
     */
    public List<ProjectModel> findProjectsByTenant(Integer tenant) throws CollectException {
        LOGGER.debug("Project tenant to find : {}", tenant);
        try {
            List<ProjectModel> listProjects = new ArrayList<>();
            MongoCursor<Document> projectsCursor =
                projectCollection.find(eq(TENANT_ID, tenant)).cursor();
            while (projectsCursor.hasNext()) {
                Document doc = projectsCursor.next();
                listProjects.add(BsonHelper.fromDocumentToObject(doc, ProjectModel.class));
            }
            return listProjects;

        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when fetching project: ", e);
            throw new CollectException("Error when fetching project : " + e);
        }
    }

    /**
     * delete a project model
     *
     * @param id project to delete
     */
    public void deleteProject(String id) {
        LOGGER.debug("Project to delete Id: {}", id);
        projectCollection.deleteOne(eq(ID, id));
        LOGGER.debug("Project deleted Id: {}", id);
    }
}
