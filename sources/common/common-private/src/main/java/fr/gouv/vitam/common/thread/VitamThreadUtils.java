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

import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.thread.VitamThreadFactory.VitamThread;

/**
 * Utility to get access to VitamSession of the current Thread
 */
public class VitamThreadUtils {

    /**
     * Extracts the VitamSession from the local thread and returns it
     *
     * @return VitamSession
     * @throws VitamThreadAccessException
     */
    public static VitamSession getVitamSession() {
        VitamSession session = null;

        final Thread currentThread = Thread.currentThread();
        if (currentThread instanceof VitamThread) {
            session = ((VitamThread) currentThread).getVitamSession();
        } else {
            throw new VitamThreadAccessException("Current thread is not instance of VitamThread");
        }

        if (session == null) {
            throw new VitamThreadAccessException("VitamSession should not be null");
        }

        return session;
    }
}
