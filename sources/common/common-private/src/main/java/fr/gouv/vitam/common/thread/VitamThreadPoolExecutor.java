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
package fr.gouv.vitam.common.thread;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.thread.VitamThreadFactory.VitamThread;
import org.eclipse.jetty.util.thread.ThreadPool;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class VitamThreadPoolExecutor extends ThreadPoolExecutor implements ThreadPool {

    // KWA TODO: SPLIT this class into two : the Jetty ThreadPool & the override of the ThreadPoolExecutor ; but first
    // understand how jetty uses this.

    private static final VitamThreadPoolExecutor VITAM_THREAD_POOL_EXECUTOR = new VitamThreadPoolExecutor();
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamThreadPoolExecutor.class);

    public VitamThreadPoolExecutor(
        int corePoolSize,
        int maximumPoolSize,
        long keepAliveTime,
        TimeUnit unit,
        BlockingQueue<Runnable> workQueue
    ) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, VitamThreadFactory.getInstance());
    }

    public VitamThreadPoolExecutor() {
        this(
            VitamConfiguration.getMinimumThreadPoolSize(),
            Integer.MAX_VALUE,
            60L,
            TimeUnit.SECONDS,
            new SynchronousQueue<>()
        );
    }

    /**
     * Create a Cached Thread Pool
     *
     * @param minimumAvailableThreads minimum Available Threads kept in the pool
     */
    public VitamThreadPoolExecutor(int minimumAvailableThreads) {
        this(minimumAvailableThreads, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    }

    /**
     * Default instance
     *
     * @return VitamThreadPoolExecutor instance
     */
    public static VitamThreadPoolExecutor getDefaultExecutor() {
        return VITAM_THREAD_POOL_EXECUTOR;
    }

    //////// ThreadPoolExecutor part of this class ////////

    /**
     * Pass the VitamSession through a VitamRunnable to the target Thread
     *
     * @param command
     */
    @Override
    public void execute(Runnable command) {
        final Thread currentThread = Thread.currentThread();

        if (LOGGER.isDebugEnabled()) {
            final String formattedStack = Arrays.stream(currentThread.getStackTrace())
                .map(StackTraceElement::toString)
                .skip(2)
                .limit(3)
                .collect(Collectors.joining(" -> ", "[", "]"));
            LOGGER.debug(command.toString() + " from " + formattedStack);
        }

        final VitamRunnable vitamRunnable;
        if (currentThread instanceof VitamThread) {
            final VitamSession session = VitamSession.from(((VitamThread) currentThread).getVitamSession());
            vitamRunnable = new VitamRunnable(command, session);
            LOGGER.debug(
                "VitamSession {} propagated from thread {} to runnable {}",
                session,
                currentThread.getName(),
                vitamRunnable
            );
        } else {
            vitamRunnable = new VitamRunnable(command);
        }

        super.execute(vitamRunnable);
    }

    /**
     * Decorator around a Runnable ; allow us to propagate the session of the runnable insite a threadPool.
     */
    private static final class VitamRunnable implements Runnable {

        private final Runnable command;

        /**
         * Attached VitamSession to set into the target execution Thread. Can be null.
         */
        private final VitamSession session;

        /**
         * @param command Command to run. Should not be null.
         * @param session VitamSession to attach to the thread that will run the command. Can be null (if no session
         * should be propagated)
         */
        public VitamRunnable(Runnable command, VitamSession session) {
            ParametersChecker.checkParameter("command should not be null", command);
            this.command = command;
            this.session = session;
        }

        /**
         * Constructor for runnable with no attached session.
         *
         * @param command
         */
        public VitamRunnable(Runnable command) {
            this(command, null);
        }

        @Override
        public void run() {
            command.run();
        }

        public VitamSession getSession() {
            return session;
        }
    }

    /**
     * <p>
     * Extract VitamSession from the given runnable, and sets it into the target (aka. current) Thread.
     * </p>
     * <p>
     * Carefully see {@link ThreadPoolExecutor#beforeExecute(Thread, Runnable)} documentation, especially about the
     * thread executing this method.
     * </p>
     *
     * @param r Cf. {@link ThreadPoolExecutor#beforeExecute(Thread, Runnable)}
     * @param t Cf. {@link ThreadPoolExecutor#beforeExecute(Thread, Runnable)}
     */
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        final Thread currentThread = Thread.currentThread();
        if (r instanceof VitamRunnable) {
            final VitamSession session = ((VitamRunnable) r).getSession();
            if (currentThread instanceof VitamThread) {
                if (session == null) {
                    LOGGER.debug("VitamSession was null in runnable {} ; nothing to propagate.", r);
                } else {
                    ((VitamThread) currentThread).getVitamSession().mutateFrom(session);
                    LOGGER.debug(
                        "VitamSession {} propagated from runnable {} to thread {}",
                        session,
                        r,
                        currentThread.getName()
                    );
                }
            } else {
                LOGGER.warn(
                    "Wrong state, eventually coding error : found a thread {} that was not a VitamThread in a VitamThreadPoolExecutor...",
                    currentThread.getName()
                );
            }
        } else {
            LOGGER.warn(
                "Wrong state, eventually coding error : inside a VitamThreadPoolExecutor, trying to setUp a thread with a Runnable that was not a VitamRunnable ..."
            );
        }
        super.beforeExecute(t, r);
    }

    /**
     * <p>
     * CLean up the session inside the thread.
     * </p>
     * <p>
     * Carefully see {@link ThreadPoolExecutor#beforeExecute(Thread, Runnable)} documentation, especially about the
     * thread executing this method.
     * </p>
     *
     * @param r Cf. {@link ThreadPoolExecutor#beforeExecute(Thread, Runnable)}
     * @param t Cf. {@link ThreadPoolExecutor#beforeExecute(Thread, Runnable)}
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        // Note : This method is invoked by the thread that executed the task.
        super.afterExecute(r, t);
        final Thread currentThread = Thread.currentThread();
        if (currentThread instanceof VitamThread) {
            final VitamThread vitamThread = (VitamThread) currentThread;
            LOGGER.debug(
                "VitamSession {} unregistered in thread {}",
                vitamThread.getVitamSession(),
                currentThread.getName()
            );
            vitamThread.getVitamSession().erase();
        } else {
            LOGGER.warn(
                "Wrong state, eventually coding error : found a thread {} that was not a VitamThread in a VitamThreadPoolExecutor...",
                currentThread.getName()
            );
        }
    }

    //////// jetty ThreadPool part of this class ////////

    @Override
    public void join() throws InterruptedException {
        while (awaitTermination(3600, TimeUnit.SECONDS)) {
            // Nothing
        }
    }

    @Override
    public int getThreads() {
        return getPoolSize();
    }

    @Override
    public int getIdleThreads() {
        return getPoolSize() - getActiveCount();
    }

    @Override
    public boolean isLowOnThreads() {
        return false;
    }
}
