
package fr.gouv.vitam.common.database.collections;

import fr.gouv.vitam.common.client.OntologyLoader;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyType;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CachedOntologyLoaderTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Test
    @RunWithCustomExecutor
    public void testLoading() {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        GUID guid = GUIDFactory.newRequestIdGUID(3);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        List<OntologyModel> ontologyModels = Arrays.asList(
            new OntologyModel().setType(OntologyType.KEYWORD).setIdentifier("_id"),
            new OntologyModel().setType(OntologyType.TEXT).setIdentifier("Title")
        );

        CachedOntologyLoader cachedOntologyLoader = new CachedOntologyLoader(10, 60, () -> ontologyModels);

        // When
        List<OntologyModel> result = cachedOntologyLoader.loadOntologies();

        // Then
        assertThat(result).isEqualTo(ontologyModels);
    }

    @Test
    @RunWithCustomExecutor
    public void testReLoadingFromCache() {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        GUID guid = GUIDFactory.newRequestIdGUID(3);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        List<OntologyModel> ontologyModels = Arrays.asList(
            new OntologyModel().setType(OntologyType.KEYWORD).setIdentifier("_id"),
            new OntologyModel().setType(OntologyType.TEXT).setIdentifier("Title")
        );

        OntologyLoader loader = mock(OntologyLoader.class);
        given(loader.loadOntologies()).willReturn(ontologyModels);

        CachedOntologyLoader cachedOntologyLoader = new CachedOntologyLoader(10, 60, loader);

        // When
        List<OntologyModel> result = null;
        for (int i = 0; i < 10; i++) {
            result = cachedOntologyLoader.loadOntologies();
        }

        // Then
        assertThat(result).isEqualTo(ontologyModels);
        verify(loader, times(1)).loadOntologies();

    }

    @Test
    @RunWithCustomExecutor
    public void testCacheTimeout() throws InterruptedException {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        GUID guid = GUIDFactory.newRequestIdGUID(3);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        List<OntologyModel> ontologyModels = Arrays.asList(
            new OntologyModel().setType(OntologyType.KEYWORD).setIdentifier("_id"),
            new OntologyModel().setType(OntologyType.TEXT).setIdentifier("Title")
        );

        OntologyLoader loader = mock(OntologyLoader.class);
        given(loader.loadOntologies()).willReturn(ontologyModels);

        CachedOntologyLoader cachedOntologyLoader = new CachedOntologyLoader(10, 1, loader);
        cachedOntologyLoader.loadOntologies();

        TimeUnit.SECONDS.sleep(2);

        // When
        List<OntologyModel> result = cachedOntologyLoader.loadOntologies();

        // Then
        assertThat(result).isEqualTo(ontologyModels);
        verify(loader, times(2)).loadOntologies();
    }
}