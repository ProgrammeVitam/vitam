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
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

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
