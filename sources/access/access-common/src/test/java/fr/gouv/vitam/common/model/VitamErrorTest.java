/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.common.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

public class VitamErrorTest {

    private static final String ERROR_JSON = "{\"Code\":0,\"Context\":\"context\",\"State\":\"state\"," +
        "\"Message\":\"message\",\"Description\":\"description\",\"Errors\":[{\"Code\":1,\"Context\":\"\"," +
        "\"State\":\"\",\"Message\":\"\",\"Description\":\"\"}]}";

    private final int code = 0;
    private final String context = "context";
    private final String state = "state";
    private final String message = "message";
    private final String description = "description";
    private final ArrayList<VitamError> errors = new ArrayList<VitamError>();

    @Test
    public final void givenAccessConfiguration_WithNullAttributes_WhenInstanciate_ThenReturnTrue() {
        final VitamError vitamError = new VitamError(code);
        assertThat(vitamError.getCode()).isEqualTo(code);
        assertThat(vitamError.getContext()).isNullOrEmpty();
        assertThat(vitamError.getState()).isNullOrEmpty();
        assertThat(vitamError.getMessage()).isNullOrEmpty();
        assertThat(vitamError.getDescription()).isNullOrEmpty();
        assertThat(vitamError.getErrors()).isNullOrEmpty();
    }

    @Test
    public final void givenAccessConfiguration_whenInstanciateAnsSetsAttributes_ThenReturnTrue() {
        final VitamError vitamError = new VitamError(code);
        vitamError.setContext(context).setDescription(description).setMessage(message).setState(state)
            .addAllErrors(errors).setCode(code);

        assertThat(vitamError.getCode()).isEqualTo(code);
        assertThat(vitamError.getContext()).isEqualTo(context);
        assertThat(vitamError.getState()).isEqualTo(state);
        assertThat(vitamError.getMessage()).isEqualTo(message);
        assertThat(vitamError.getDescription()).isEqualTo(description);
        assertThat(vitamError.getErrors()).isNullOrEmpty();
    }

    @Test
    public void testToString() throws Exception {
        VitamError error = new VitamError(0);
        error.setMessage("message");
        error.setDescription("description");
        error.setState("state");
        error.setContext("context");
        error.addAllErrors(Collections.singletonList(new VitamError(1)));
        System.out.println(error.toString());
        System.out.println(ERROR_JSON);
        assertEquals(ERROR_JSON, error.toString());
    }

}
