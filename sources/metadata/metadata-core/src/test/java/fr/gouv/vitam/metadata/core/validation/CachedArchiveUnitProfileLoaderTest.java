package fr.gouv.vitam.metadata.core.validation;

import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
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

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CachedArchiveUnitProfileLoaderTest {

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

        ArchiveUnitProfileModel archiveUnitProfileModel = mock(ArchiveUnitProfileModel.class);
        doReturn(new RequestResponseOK<ArchiveUnitProfileModel>().addResult(archiveUnitProfileModel))
            .when(adminManagementClient).findArchiveUnitProfilesByID("MyAUP");

        // When
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader =
            new CachedArchiveUnitProfileLoader(adminManagementClientFactory, 10, 60);
        Optional<ArchiveUnitProfileModel> result = archiveUnitProfileLoader.loadArchiveUnitProfile("MyAUP");

        // Then
        assertThat(result.get()).isEqualTo(archiveUnitProfileModel);
        verify(adminManagementClient, times(1)).findArchiveUnitProfilesByID("MyAUP");
    }

    @Test
    @RunWithCustomExecutor
    public void testReLoadingFromCache() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        GUID guid = GUIDFactory.newRequestIdGUID(3);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        ArchiveUnitProfileModel archiveUnitProfileModel1 = mock(ArchiveUnitProfileModel.class);
        ArchiveUnitProfileModel archiveUnitProfileModel2 = mock(ArchiveUnitProfileModel.class);
        doReturn(new RequestResponseOK<ArchiveUnitProfileModel>().addResult(archiveUnitProfileModel1))
            .when(adminManagementClient).findArchiveUnitProfilesByID("MyAUP1");
        doReturn(new RequestResponseOK<ArchiveUnitProfileModel>().addResult(archiveUnitProfileModel2))
            .when(adminManagementClient).findArchiveUnitProfilesByID("MyAUP2");

        // When
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader =
            new CachedArchiveUnitProfileLoader(adminManagementClientFactory, 10, 60);

        Optional<ArchiveUnitProfileModel> result1 = null;
        Optional<ArchiveUnitProfileModel> result2 = null;
        for (int i = 0; i < 10; i++) {
            result1 = archiveUnitProfileLoader.loadArchiveUnitProfile("MyAUP1");
            result2 = archiveUnitProfileLoader.loadArchiveUnitProfile("MyAUP2");
        }

        // Then
        assertThat(result1.get()).isEqualTo(archiveUnitProfileModel1);
        assertThat(result2.get()).isEqualTo(archiveUnitProfileModel2);
        verify(adminManagementClient, times(1)).findArchiveUnitProfilesByID("MyAUP1");
        verify(adminManagementClient, times(1)).findArchiveUnitProfilesByID("MyAUP2");
    }

    @Test
    @RunWithCustomExecutor
    public void testCacheTimeout() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        GUID guid = GUIDFactory.newRequestIdGUID(3);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        ArchiveUnitProfileModel archiveUnitProfileModel = mock(ArchiveUnitProfileModel.class);
        doReturn(new RequestResponseOK<ArchiveUnitProfileModel>().addResult(archiveUnitProfileModel))
            .when(adminManagementClient).findArchiveUnitProfilesByID("MyAUP");

        // When
        CachedArchiveUnitProfileLoader archiveUnitProfileLoader =
            new CachedArchiveUnitProfileLoader(adminManagementClientFactory, 10, 1);
        archiveUnitProfileLoader.loadArchiveUnitProfile("MyAUP");
        TimeUnit.SECONDS.sleep(2);
        Optional<ArchiveUnitProfileModel> result = archiveUnitProfileLoader.loadArchiveUnitProfile("MyAUP");

        // Then
        assertThat(result.get()).isEqualTo(archiveUnitProfileModel);
        verify(adminManagementClient, times(2)).findArchiveUnitProfilesByID("MyAUP");
    }
}
