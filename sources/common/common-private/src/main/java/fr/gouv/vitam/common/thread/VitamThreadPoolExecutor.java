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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.jetty.util.thread.ThreadPool;
import org.glassfish.jersey.server.ManagedAsyncExecutor;
import org.glassfish.jersey.spi.ExecutorServiceProvider;

import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.thread.VitamThreadFactory.VitamThread;

/**
 * Vitam ThreadPoolExecutor compatible with Jersey which copy the VitamSession from the main thread to the subthread
 */
@Named("threadpool")
@ManagedAsyncExecutor
public class VitamThreadPoolExecutor extends ThreadPoolExecutor implements ThreadPool, ExecutorServiceProvider {

    private static final VitamThreadPoolExecutor VITAM_THREAD_POOL_EXECUTOR = new VitamThreadPoolExecutor();
    
    VitamThreadFactory factory = new VitamThreadFactory();

    /**
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param workQueue
     */
    @Inject @Named("threadpool")
    public VitamThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
        BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, VitamThreadFactory.getInstance());
    }

    /**
     * Create a Cached Thread Pool
     */
    @Inject @Named("threadpool")
    public VitamThreadPoolExecutor() {
        this(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        Thread currentThread = Thread.currentThread();
        if (t instanceof VitamThread && currentThread instanceof VitamThread) {
            ThreadLocal<VitamSession> vitamSessionTL = ((VitamThread) currentThread).getVitamSession();
            ((VitamThread) t).getVitamSession().set(vitamSessionTL.get());
        }
        super.beforeExecute(t, r);
    }

    @Override
    public void join() throws InterruptedException {
        while (awaitTermination(3600, TimeUnit.SECONDS)) {
            // Nothing
        }
    }

    @Override
    public int getThreads() {
        return this.getPoolSize();
    }

    @Override
    public int getIdleThreads() {
        return this.getPoolSize() - this.getActiveCount();
    }

    @Override
    public boolean isLowOnThreads() {
        return false;
    }

    @Override
    public ExecutorService getExecutorService() {
        return this;
    }

    @Override
    public void dispose(ExecutorService executorService) {
        // Empty ?
    }
    
    /**
     * 
     * @return VitamThreadPoolExecutor instance
     */
    public static VitamThreadPoolExecutor getInstance() {
        return VITAM_THREAD_POOL_EXECUTOR;
    }
}
