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

package fr.gouv.vitam.scheduler.server.job;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StorageBackupLogJobTest {

    private static final int TENANT_ID = 1;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    @Mock
    private JobExecutionContext jobExecutionContext;

    private StorageBackupLogJob storageBackupLogJob;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Mockito.reset(storageClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        storageBackupLogJob = new StorageBackupLogJob(storageClientFactory);
    }

    @Test
    @RunWithCustomExecutor
    public void test_writelog_backup_on_multiple_servers() throws Exception {
        VitamConfiguration.setAdminTenant(TENANT_ID);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Map<String, Object> map = new HashMap<>();
        map.put("StorageBackupType", "WriteLog");
        map.put("StorageServerHosts", "host1:1000;host2:2000;host3:3000");
        final Trigger trigger = mock(Trigger.class);
        when(trigger.getJobDataMap()).thenReturn(new JobDataMap(map));
        when(jobExecutionContext.getTrigger()).thenReturn(trigger);
        storageBackupLogJob.execute(jobExecutionContext);
        verify(storageClient, times(3)).storageLogBackup(eq(VitamConfiguration.getTenants()));
    }

    @Test
    @RunWithCustomExecutor
    public void test_accesslog_backup_on_one_server() throws Exception {
        VitamConfiguration.setAdminTenant(TENANT_ID);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Map<String, Object> map = new HashMap<>();
        map.put("StorageBackupType", "AccessLog");
        map.put("StorageServerHosts", "host1:1000");
        final Trigger trigger = mock(Trigger.class);
        when(trigger.getJobDataMap()).thenReturn(new JobDataMap(map));
        when(jobExecutionContext.getTrigger()).thenReturn(trigger);
        storageBackupLogJob.execute(jobExecutionContext);
        verify(storageClient).storageAccessLogBackup(eq(VitamConfiguration.getTenants()));
    }
}
