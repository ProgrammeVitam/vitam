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
package fr.gouv.vitam.collect.internal.core.repository;

import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.ManifestContext;
import fr.gouv.vitam.collect.internal.core.common.ProjectModel;
import fr.gouv.vitam.collect.internal.core.common.ProjectStatus;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectRepositoryTest {

    private static final String PROJECT_TEST_COLLECTION = "Project" + GUIDFactory.newGUID().getId();
    private final Integer tenant = 1;

    private ProjectRepository repository;

    @Rule
    public MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(), PROJECT_TEST_COLLECTION);

    private static final String PROJECT_1_ID = "aeeaaaaaacgw45nxaaopkalhchougsiaaaaq";
    private static final String PROJECT_2_ID = "aeaaaaaaaagh65wtab27ialg5fopxnaaaaaq";
    private static final String PROJECT_3_ID = "aeaaaaaaaagw45nxabw2ualhc4jvawqabbbq";
    private static final String PROJECT_4_ID = "aeaaaaaaaaaltpovaa2zgamd5kdsesiaaaaq";

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        repository = new ProjectRepository(mongoDbAccess, PROJECT_TEST_COLLECTION);
    }

    @Test
    public void should_found_projects_with_name() throws CollectInternalException {
        // GIVEN
        populateDb();

        // WHEN
        List<ProjectModel> searchProjects = repository.searchProject("test", tenant);

        // THEN
        assertThat(searchProjects).hasSize(1);
        assertThat(searchProjects.get(0).getId()).isEqualTo(PROJECT_1_ID);
    }

    @Test
    public void should_found_projects_with_id() throws CollectInternalException {
        // GIVEN
        populateDb();

        // WHEN
        List<ProjectModel> searchProjects = repository.searchProject(PROJECT_2_ID, tenant);

        // THEN
        assertThat(searchProjects).hasSize(1);
        assertThat(searchProjects.get(0).getId()).isEqualTo(PROJECT_2_ID);
    }

    @Test
    public void should_escape_special_characters_when_searching_projects() throws CollectInternalException {
        // GIVEN
        populateDb();

        // WHEN
        List<ProjectModel> searchProjects = repository.searchProject(".", tenant);

        // THEN
        assertThat(searchProjects).isEmpty();
    }

    @Test
    public void should_escape_another_special_characters_when_searching_projects() throws CollectInternalException {
        // GIVEN
        populateDb();

        // WHEN
        List<ProjectModel> searchProjects = repository.searchProject(":)", tenant);

        // THEN
        assertThat(searchProjects).hasSize(1);
        assertThat(searchProjects.get(0).getId()).isEqualTo(PROJECT_4_ID);
    }

    private void populateDb() throws CollectInternalException {
        TestDummyData dummyData = new TestDummyData();
        for (ProjectModel project : dummyData.getProjects()) {
            repository.createProject(project);
        }
    }

    class TestDummyData {

        List<ProjectModel> getProjects() {
            return List.of(
                createProject(PROJECT_1_ID, "Test"),
                createProject(PROJECT_2_ID, "Hello"),
                createProject(PROJECT_3_ID, "OK"),
                createProject(PROJECT_4_ID, ":)")
            );
        }

        private ProjectModel createProject(String id, String messageIdentifier) {
            ProjectModel project = new ProjectModel();
            project.setId(id);
            project.setName(messageIdentifier);
            project.setStatus(ProjectStatus.OPEN);
            ManifestContext context = new ManifestContext();
            context.setArchivalAgreement("IC-000001");
            context.setMessageIdentifier(messageIdentifier);
            context.setArchivalAgencyIdentifier("Identifier0");
            context.setTransferringAgencyIdentifier("Identifier3");
            context.setOriginatingAgencyIdentifier("FRAN_NP_009915");
            context.setSubmissionAgencyIdentifier("FRAN_NP_005061");
            project.setManifestContext(context);
            project.setTenant(tenant);
            return project;
        }
    }
}
