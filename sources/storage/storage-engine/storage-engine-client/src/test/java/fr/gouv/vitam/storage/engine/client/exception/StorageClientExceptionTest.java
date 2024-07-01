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
package fr.gouv.vitam.storage.engine.client.exception;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class StorageClientExceptionTest {

    private static final String MESSAGE = "message";
    private static final Exception exception = new Exception();

    @Test
    public final void testStorageAlreadyExistsClientExceptionThrowable() {
        assertNotNull(new StorageAlreadyExistsClientException((String) null));
        assertNotNull(new StorageAlreadyExistsClientException(MESSAGE));
        assertNotNull(new StorageAlreadyExistsClientException(exception));
        assertNotNull(new StorageAlreadyExistsClientException(MESSAGE, exception));
    }

    @Test
    public final void testStorageServerClientExceptionThrowable() {
        assertNotNull(new StorageServerClientException((String) null));
        assertNotNull(new StorageServerClientException(MESSAGE));
        assertNotNull(new StorageServerClientException(exception));
        assertNotNull(new StorageServerClientException(MESSAGE, exception));
    }

    @Test
    public final void testStorageClientExceptionThrowable() {
        assertNotNull(new StorageClientException((String) null));
        assertNotNull(new StorageClientException(MESSAGE));
        assertNotNull(new StorageClientException(exception));
        assertNotNull(new StorageClientException(MESSAGE, exception));
    }

    @Test
    public final void testStorageNotFoundClientExceptionThrowable() {
        assertNotNull(new StorageNotFoundClientException((String) null));
        assertNotNull(new StorageNotFoundClientException(MESSAGE));
        assertNotNull(new StorageNotFoundClientException(exception));
        assertNotNull(new StorageNotFoundClientException(MESSAGE, exception));
    }
}
