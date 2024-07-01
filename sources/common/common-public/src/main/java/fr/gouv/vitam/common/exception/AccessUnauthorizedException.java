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
package fr.gouv.vitam.common.exception;

/**
 * Exception used when contract does not give access to the Vitam
 */
public class AccessUnauthorizedException extends VitamClientException {

    /**
     *
     */
    private static final long serialVersionUID = -2684023194234768369L;

    /**
     * Basic constructor to indicate a simple error message without stacktrace
     *
     * @param message message to log
     */
    public AccessUnauthorizedException(String message) {
        super(message);
    }

    /**
     * Constructor used to encapsulate a previously thrown exception. A generic message is used.
     *
     * @param throwable the originating exception
     */
    public AccessUnauthorizedException(Throwable throwable) {
        super("An error occurred while retrieving objects from the local thread", throwable);
    }

    /**
     * Constructor used to encapsulate a previously thrown exception with but with a custom meaningful message
     *
     * @param message the message to log throw threw
     * @param throwable the originating exception
     */
    public AccessUnauthorizedException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
