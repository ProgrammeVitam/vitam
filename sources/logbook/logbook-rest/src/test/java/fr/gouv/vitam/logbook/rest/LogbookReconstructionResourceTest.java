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
package fr.gouv.vitam.logbook.rest;

import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.model.reconstruction.ReconstructionRequestItem;
import fr.gouv.vitam.logbook.common.model.reconstruction.ReconstructionResponseItem;
import fr.gouv.vitam.logbook.common.server.reconstruction.ReconstructionService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

/**
 * LogbookReconstructionResource test
 */
public class LogbookReconstructionResourceTest {

    private ReconstructionService reconstructionService;
    private ReconstructionRequestItem requestItem;

    @Before
    public void setup() {
        reconstructionService = Mockito.mock(ReconstructionService.class);
        requestItem = new ReconstructionRequestItem();
        requestItem.setTenant(10).setLimit(100);
    }

    @Test
    public void should_return_ok_when_request_item_full() throws DatabaseException {
        // given
        ReconstructionResponseItem responseItem = new ReconstructionResponseItem(requestItem, StatusCode.OK);

        when(reconstructionService.reconstruct(requestItem)).thenReturn(responseItem);
        LogbookReconstructionResource reconstructionResource = new LogbookReconstructionResource(reconstructionService);
        // when
        Response response = reconstructionResource.reconstructCollection(Arrays.asList(requestItem));
        // then
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
        List<ReconstructionResponseItem> responseEntity = (ArrayList<ReconstructionResponseItem>) response.getEntity();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.size()).isEqualTo(1);
        assertThat(responseEntity.get(0).getTenant()).isEqualTo(10);
        assertThat(responseEntity.get(0).getStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void should_return_empty_response_when_that_request_empty() {
        // given
        LogbookReconstructionResource reconstructionResource = new LogbookReconstructionResource(reconstructionService);
        // when
        Response response = reconstructionResource.reconstructCollection(new ArrayList<>());
        // then
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
        List<ReconstructionResponseItem> responseEntity = (ArrayList<ReconstructionResponseItem>) response.getEntity();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity).isEmpty();
    }

    @Test
    public void should_return_ok_when__request_item_no_offset() {
        // given
        LogbookReconstructionResource reconstructionResource = new LogbookReconstructionResource(reconstructionService);
        // when + then
        assertThatCode(() -> reconstructionResource.reconstructCollection(null)).isInstanceOf(
            IllegalArgumentException.class
        );
    }
}
