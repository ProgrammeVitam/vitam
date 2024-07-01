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

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.VitamSession;

import javax.validation.constraints.NotNull;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple ThreadFactory setting Threads to be Daemon threads (do not prevent shutdown) ; in addition, creates
 * VitamThread allowing us to put session information in those threads.
 */
public class VitamThreadFactory implements ThreadFactory {

    private static final VitamThreadFactory VITAM_THREAD_FACTORY = new VitamThreadFactory();
    private final AtomicLong threadNumber = new AtomicLong(0);
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamThreadFactory.class);

    @Override
    public Thread newThread(Runnable arg0) {
        return new VitamThread(arg0, threadNumber.getAndIncrement());
    }

    /**
     * @return the default {@link VitamThreadFactory}
     */
    public static final VitamThreadFactory getInstance() {
        return VITAM_THREAD_FACTORY;
    }

    /**
     * Vitam Thread implementation ; mainly used to attach a threadlocal session to it.
     */
    public static class VitamThread extends Thread {

        /**
         * vitamSession should be conserved during the entire lifetime of the thread ; only its content may change.
         */
        private final ThreadLocal<VitamSession> vitamSession = ThreadLocal.withInitial(() -> new VitamSession(this));

        /**
         * Thread constructor
         *
         * @param runnable the real runnable
         * @param rank the thread rank
         */
        public VitamThread(Runnable runnable, long rank) {
            super(runnable, "vitam-thread-" + rank);
            LOGGER.debug("Created vitam-thread-{}", rank);
            setDaemon(true);
            // KWA Note : no need to pass the VitamSession from the creating thread to this new thread ; the
            // VitamSession propagation between thread is the Executor's job.
        }

        /**
         * @return the ThreadLocal<VitamSession> ; never returns null.
         */
        @NotNull
        public VitamSession getVitamSession() {
            return vitamSession.get();
        }
    }
}
