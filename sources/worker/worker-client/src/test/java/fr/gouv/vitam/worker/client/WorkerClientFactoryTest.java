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
package fr.gouv.vitam.worker.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class WorkerClientFactoryTest {

	@Before
    public void initFileConfiguration() {
        WorkerClientFactory.getInstance().changeConfigurationFile("worker-client.conf");
    }

    @Test
    public void testInitWithoutConfigurationFile() {
        // assume that a fake file is like no file
        WorkerClientFactory.getInstance().changeConfigurationFile("tmp");
        final WorkerClient client = WorkerClientFactory.getInstance().getWorkerClient();
        assertTrue(client instanceof WorkerClientMock);
        final WorkerClientFactory.WorkerClientType type = WorkerClientFactory.getDefaultWorkerClientType();
        assertNotNull(type);
        assertEquals(WorkerClientFactory.WorkerClientType.MOCK_WORKER, type);
    }

    @Test
    public void testInitWithConfigurationFile() {
        final WorkerClient client = WorkerClientFactory.getInstance().getWorkerClient();
        assertTrue(client instanceof WorkerClientRest);
        final WorkerClientFactory.WorkerClientType type = WorkerClientFactory.getDefaultWorkerClientType();
        assertNotNull(type);
        assertEquals(WorkerClientFactory.WorkerClientType.WORKER, type);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitWithDefaultClientTypeNullThenThrowsException() throws Exception {
    	WorkerClientFactory.changeDefaultClientType(null);
    }

    @Test
    public void testWorkerClientTest() {
    	WorkerClientFactory.changeDefaultClientType(WorkerClientFactory.WorkerClientType.WORKER);
        final WorkerClientFactory.WorkerClientType type = WorkerClientFactory.getDefaultWorkerClientType();
        assertNotNull(type);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithWrongInitServerParameters() {
    	WorkerClientFactory.setConfiguration(WorkerClientFactory.WorkerClientType.WORKER, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithWrongInitPortParameters() {
    	WorkerClientFactory.setConfiguration(WorkerClientFactory.WorkerClientType.WORKER, null);
    }
	
}
