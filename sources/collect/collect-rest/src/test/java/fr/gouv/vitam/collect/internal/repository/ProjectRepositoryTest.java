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

import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.model.ManifestContext;
import fr.gouv.vitam.collect.internal.model.ProjectModel;
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

    @Rule
    public MongoRule mongoRule =
        new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(), PROJECT_TEST_COLLECTION);

    private ProjectRepository repository;
    private final Integer tenant = 1;
    private final static String PROJECT_1_ID = "aeeaaaaaacgw45nxaaopkalhchougsiaaaaq";
    private final static String PROJECT_2_ID = "aeaaaaaaaagh65wtab27ialg5fopxnaaaaaq";
    private final static String PROJECT_3_ID = "aeaaaaaaaagw45nxabw2ualhc4jvawqabbbq";
    private final static String PROJECT_4_ID = "aeaaaaaaaaaltpovaa2zgamd5kdsesiaaaaq";
    private final static List<String> keys = List.of("_id", "context.SubmissionAgencyIdentifier", "context.MessageIdentifier");

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        repository = new ProjectRepository(mongoDbAccess, PROJECT_TEST_COLLECTION);
    }

    @Test
    public void should_found_projects_with_name() throws CollectException {
        // GIVEN
        populateDb();

        // WHEN
        List<ProjectModel> searchProjects = repository.searchProject("test", keys, tenant);

        // THEN
        assertThat(searchProjects).hasSize(1);
        assertThat(searchProjects.get(0).getId()).isEqualTo(PROJECT_1_ID);
    }

    @Test
    public void should_found_projects_with_id() throws CollectException {
        // GIVEN
        populateDb();

        // WHEN
        List<ProjectModel> searchProjects = repository.searchProject(PROJECT_2_ID, keys, tenant);

        // THEN
        assertThat(searchProjects).hasSize(1);
        assertThat(searchProjects.get(0).getId()).isEqualTo(PROJECT_2_ID);
    }

    @Test
    public void should_escape_special_characters_when_searching_projects() throws CollectException {
        // GIVEN
        populateDb();

        // WHEN
        List<ProjectModel> searchProjects = repository.searchProject(".", keys, tenant);

        // THEN
        assertThat(searchProjects).isEmpty();
    }

    @Test
    public void should_escape_another_special_characters_when_searching_projects() throws CollectException {
        // GIVEN
        populateDb();

        // WHEN
        List<ProjectModel> searchProjects = repository.searchProject(":)", keys, tenant);

        // THEN
        assertThat(searchProjects).hasSize(1);
        assertThat(searchProjects.get(0).getId()).isEqualTo(PROJECT_4_ID);
    }

    private void populateDb() throws CollectException {
        TestDummyData dummyData = new TestDummyData();
        for (ProjectModel project : dummyData.getProjects()) {
            repository.createProject(project);
        }
    }


    class TestDummyData {
        List<ProjectModel> getProjects() {
            return List.of(
                createProject(PROJECT_1_ID, "Test"),
                createProject(PROJECT_2_ID,"Hello"),
                createProject(PROJECT_3_ID,"OK"),
                createProject(PROJECT_4_ID,":)")
            );
        }

        private ProjectModel createProject(String id, String messageIdentifier) {
            ProjectModel project = new ProjectModel();
            project.setId(id);
            ManifestContext context = new ManifestContext();
            context.setName(messageIdentifier);
            context.setStatus("OPEN");
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
