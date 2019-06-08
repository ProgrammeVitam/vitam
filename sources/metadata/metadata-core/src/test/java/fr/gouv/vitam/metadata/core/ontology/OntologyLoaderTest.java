package fr.gouv.vitam.metadata.core.ontology;

import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyType;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OntologyLoaderTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Mock
    AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    AdminManagementClient adminManagementClient;

    @Before
    public void before() {
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
    }

    @Test
    @RunWithCustomExecutor
    public void testLoading() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        GUID guid = GUIDFactory.newRequestIdGUID(3);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        List<OntologyModel> ontologyModels = Arrays.asList(
            new OntologyModel().setType(OntologyType.KEYWORD).setIdentifier("_id"),
            new OntologyModel().setType(OntologyType.TEXT).setIdentifier("Title")
        );
        doReturn(new RequestResponseOK<OntologyModel>().addAllResults(ontologyModels))
            .when(adminManagementClient).findOntologies(any());

        // When
        OntologyLoader ontologyLoader = new OntologyLoader(adminManagementClientFactory, 10, 60);
        List<OntologyModel> result = ontologyLoader.loadOntologies();

        // Then
        assertThat(result).isEqualTo(ontologyModels);
        verify(adminManagementClient, times(1)).findOntologies(any());
    }

    @Test
    @RunWithCustomExecutor
    public void testReLoadingFromCache() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        GUID guid = GUIDFactory.newRequestIdGUID(3);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        List<OntologyModel> ontologyModels = Arrays.asList(
            new OntologyModel().setType(OntologyType.KEYWORD).setIdentifier("_id"),
            new OntologyModel().setType(OntologyType.TEXT).setIdentifier("Title")
        );
        doReturn(new RequestResponseOK<OntologyModel>().addAllResults(ontologyModels))
            .when(adminManagementClient).findOntologies(any());

        // When
        OntologyLoader ontologyLoader = new OntologyLoader(adminManagementClientFactory, 10, 60);

        List<OntologyModel> result = null;
        for (int i = 0; i < 10; i++) {
            result = ontologyLoader.loadOntologies();
        }

        // Then
        assertThat(result).isEqualTo(ontologyModels);
        verify(adminManagementClient, times(1)).findOntologies(any());
    }

    @Test
    @RunWithCustomExecutor
    public void testCacheTimeout() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        GUID guid = GUIDFactory.newRequestIdGUID(3);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        List<OntologyModel> ontologyModels = Arrays.asList(
            new OntologyModel().setType(OntologyType.KEYWORD).setIdentifier("_id"),
            new OntologyModel().setType(OntologyType.TEXT).setIdentifier("Title")
        );
        doReturn(new RequestResponseOK<OntologyModel>().addAllResults(ontologyModels))
            .when(adminManagementClient).findOntologies(any());

        // When
        OntologyLoader ontologyLoader = new OntologyLoader(adminManagementClientFactory, 10, 1);
        ontologyLoader.loadOntologies();
        TimeUnit.SECONDS.sleep(2);
        List<OntologyModel> result = ontologyLoader.loadOntologies();

        // Then
        assertThat(result).isEqualTo(ontologyModels);
        verify(adminManagementClient, times(2)).findOntologies(any());
    }
}
