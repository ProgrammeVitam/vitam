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
package fr.gouv.vitam.common.thread;

import org.glassfish.jersey.spi.ThreadPoolExecutorProvider;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * VItamThreadPoolExecutorProvider
 */
public class VitamThreadPoolExecutorProvider extends ThreadPoolExecutorProvider {
    VitamThreadPoolExecutor vitamThreadPoolExecutor = VitamThreadPoolExecutor.getDefaultExecutor();

    /**
     * @param name
     */
    public VitamThreadPoolExecutorProvider(String name) {
        super(name);
    }

    @Override
    public ExecutorService getExecutorService() {
        return vitamThreadPoolExecutor;
    }

    @Override
    protected ThreadPoolExecutor createExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        return vitamThreadPoolExecutor;
    }

    @Override
    protected int getMaximumPoolSize() {
        return vitamThreadPoolExecutor.getMaximumPoolSize();
    }

    @Override
    protected long getKeepAliveTime() {
        return vitamThreadPoolExecutor.getKeepAliveTime(TimeUnit.MILLISECONDS);
    }

    @Override
    protected BlockingQueue<Runnable> getWorkQueue() {
        return vitamThreadPoolExecutor.getQueue();
    }

    @Override
    public void dispose(ExecutorService executorService) {
        // Empty ?
    }

    @Override
    protected int getCorePoolSize() {
        return vitamThreadPoolExecutor.getCorePoolSize();
    }

    @Override
    protected ThreadFactory getBackingThreadFactory() {
        return vitamThreadPoolExecutor.getThreadFactory();
    }

}
