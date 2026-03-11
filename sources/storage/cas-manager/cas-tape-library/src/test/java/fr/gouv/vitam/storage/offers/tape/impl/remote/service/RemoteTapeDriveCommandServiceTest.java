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
package fr.gouv.vitam.storage.offers.tape.impl.remote.service;

import fr.gouv.vitam.storage.cold.client.InaTapeProxyApi;
import fr.gouv.vitam.storage.cold.client.invoker.ApiException;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeDriveSpec;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeDriveState;
import fr.gouv.vitam.storage.engine.common.api.exception.TapeCommandException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RemoteTapeDriveCommandServiceTest {

    private static final Integer DRIVE_INDEX = 0;
    private InaTapeProxyApi inaTapeProxyApi;
    private RemoteTapeDriveCommandService service;

    @Before
    public void setUp() {
        inaTapeProxyApi = mock(InaTapeProxyApi.class);
        service = new RemoteTapeDriveCommandService(inaTapeProxyApi, DRIVE_INDEX);
    }

    @Test
    public void shouldReturnTapeDriveSpec_whenStatusIsCalled() throws Exception {
        TapeDriveState expectedSpec = new TapeDriveState();
        when(inaTapeProxyApi.getDriveStatus(DRIVE_INDEX)).thenReturn(expectedSpec);

        TapeDriveSpec result = service.status();

        assertSame("status() should return the value from InaTapeProxyApi", expectedSpec, result);
        verify(inaTapeProxyApi).getDriveStatus(DRIVE_INDEX);
    }

    @Test(expected = TapeCommandException.class)
    public void shouldThrowTapeCommandException_whenApiExceptionOnStatus() throws Exception {
        when(inaTapeProxyApi.getDriveStatus(DRIVE_INDEX)).thenThrow(new ApiException("API down"));

        service.status();
    }

    @Test
    public void shouldCallMoveOnColdStorageApi() throws Exception {
        service.move(10, true);

        verify(inaTapeProxyApi).move(DRIVE_INDEX, 10, true);
    }

    @Test(expected = TapeCommandException.class)
    public void shouldThrowTapeCommandException_whenApiExceptionOnMove() throws Exception {
        doThrow(new ApiException("Move error")).when(inaTapeProxyApi).move(anyInt(), anyInt(), anyBoolean());

        service.move(5, false);
    }

    @Test
    public void shouldCallRewindOnColdStorageApi() throws Exception {
        service.rewind();

        verify(inaTapeProxyApi).rewind(DRIVE_INDEX);
    }

    @Test(expected = TapeCommandException.class)
    public void shouldThrowTapeCommandException_whenApiExceptionOnRewind() throws Exception {
        doThrow(new ApiException("Rewind error")).when(inaTapeProxyApi).rewind(DRIVE_INDEX);

        service.rewind();
    }

    @Test
    public void shouldCallGoToEndOnColdStorageApi() throws Exception {
        service.goToEnd();
        verify(inaTapeProxyApi).goToEnd(DRIVE_INDEX);
    }

    @Test(expected = TapeCommandException.class)
    public void shouldThrowTapeCommandException_whenApiExceptionOnGoToEnd() throws Exception {
        doThrow(new ApiException("End error")).when(inaTapeProxyApi).goToEnd(DRIVE_INDEX);
        service.goToEnd();
    }

    @Test
    public void shouldCallEjectOnColdStorageApi() throws Exception {
        service.eject();
        verify(inaTapeProxyApi).eject(DRIVE_INDEX);
    }

    @Test(expected = TapeCommandException.class)
    public void shouldThrowTapeCommandException_whenApiExceptionOnEject() throws Exception {
        doThrow(new ApiException("Eject error")).when(inaTapeProxyApi).eject(DRIVE_INDEX);
        service.eject();
    }
}
