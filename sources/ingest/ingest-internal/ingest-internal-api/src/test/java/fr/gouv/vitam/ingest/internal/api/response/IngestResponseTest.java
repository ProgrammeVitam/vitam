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
package fr.gouv.vitam.ingest.internal.api.response;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class IngestResponseTest {

    private final String cause = "cause";
    private final String message = "message";
    private final String containerGuid = "containerGuid";
    private final String sedaGuid = "sedaGUid";
    private final Boolean errorOccured = Boolean.TRUE;
    private final Long numberOfNumericalObject = 1L;
    private final Long numberOfElements = 1L;

    private final List<String> guidNumObjList = new ArrayList<>();

    @Test
    public void givenIngestResponseNotNull_whenInstantiateWithNotAttributes_thenReturnTrue() {
        final IngestResponse ingestResponse = new IngestResponse();

        assertThat(ingestResponse.getCause()).isNullOrEmpty();
        assertThat(ingestResponse.getContainerGuid()).isNullOrEmpty();
        assertThat(ingestResponse.getErrorOccured()).isNull();
        assertThat(ingestResponse.getGuidNumObjList()).isNullOrEmpty();
        assertThat(ingestResponse.getListGuidObjectAsString()).isNotNull().isNotEmpty();
        assertThat(ingestResponse.getMessage()).isNullOrEmpty();
        assertThat(ingestResponse.getNumberOfElements()).isNull();
        assertThat(ingestResponse.getNumberOfNumericalObject()).isNull();
        assertThat(ingestResponse.getNumericalObject()).isNull();
        assertThat(ingestResponse.getSedaGuid()).isNullOrEmpty();
    }

    @Test
    public void givenIngestResponseNotNull_whenInstantiateWithAttributes_thenReturnTrue() {
        final IngestResponse ingestResponse = new IngestResponse();

        ingestResponse
            .setCause(cause)
            .setMessage(message)
            .setContainerGuid(containerGuid)
            .setSedaGuid(sedaGuid)
            .setErrorOccured(errorOccured)
            .setNumberOfNumericalObject(numberOfNumericalObject)
            .setNumericalObject(numberOfNumericalObject)
            .setNumberOfElements(numberOfElements)
            .setGuidNumObjList(guidNumObjList)
            .addGuidNumericObject(sedaGuid);

        assertThat(ingestResponse.getCause()).isEqualTo(cause);
        assertThat(ingestResponse.getContainerGuid()).isEqualTo(containerGuid);
        assertThat(ingestResponse.getErrorOccured()).isTrue();
        assertThat(ingestResponse.getGuidNumObjList()).isNotNull().isNotEmpty();
        assertThat(ingestResponse.getListGuidObjectAsString()).isNotNull().isNotEmpty();
        assertThat(ingestResponse.getMessage()).isEqualTo(message);
        assertThat(ingestResponse.getNumberOfElements()).isEqualTo(1L);
        assertThat(ingestResponse.getNumberOfNumericalObject()).isEqualTo(1L);
        assertThat(ingestResponse.getNumericalObject()).isEqualTo(1L);
        assertThat(ingestResponse.getSedaGuid()).isEqualTo(sedaGuid);
        assertThat(ingestResponse.getGuidNumObjList().size()).isEqualTo(1);
    }
}
