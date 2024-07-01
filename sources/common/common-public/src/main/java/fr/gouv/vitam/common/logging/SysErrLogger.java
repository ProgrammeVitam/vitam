/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.logging;

/**
 * Utility class to be used only in classes where VitamLogger is not allowed
 */
public class SysErrLogger {

    /**
     * FAKE LOGGER used where no LOG could be done
     */
    public static final SysErrLogger FAKE_LOGGER = new SysErrLogger();

    private SysErrLogger() {
        // Empty
    }

    /**
     * Utility method to log nothing
     *
     * Used only in classes where VitamLogger is not allowed
     *
     * @param throwable to log ignore
     */
    public void ignoreLog(Throwable throwable) {
        // Nothing to do
    }

    /**
     * Utility method to log through System.err
     *
     * Used only in classes where VitamLogger is not allowed
     *
     * @param message to write for error
     */
    public void syserr(String message) {
        System.err.println("ERROR " + message); // NOSONAR
    }

    /**
     * Utility method to log through System.err the current Stacktrace
     *
     * Used only in classes where VitamLogger is not allowed
     */
    public void syserr() {
        new Exception("ERROR Stacktrace").printStackTrace(); // NOSONAR
    }

    /**
     * Utility method to log through System.err the current Stacktrace
     *
     * Used only in classes where VitamLogger is not allowed
     *
     * @param message to write for error
     * @param e throw to write as error
     */
    public void syserr(String message, Throwable e) {
        System.err.print("ERROR " + message + ": "); // NOSONAR
        e.printStackTrace(); // NOSONAR
    }
}
