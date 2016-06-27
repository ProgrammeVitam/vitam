/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.common.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.Test;

public class VitamErrorTest {

    private final int code = 0;
    private String context = "context";
    private String state = "state";
    private String message = "message";
    private String description = "description";
    private ArrayList<VitamError> errors = new ArrayList<VitamError>();

    @Test
    public final void givenAccessConfiguration_WithNullAttributes_WhenInstanciate_ThenReturnTrue() {
        VitamError vitamError = new VitamError(code);
        assertThat(vitamError.getCode()).isEqualTo(code);
        assertThat(vitamError.getContext()).isNullOrEmpty();
        assertThat(vitamError.getState()).isNullOrEmpty();
        assertThat(vitamError.getMessage()).isNullOrEmpty();
        assertThat(vitamError.getDescription()).isNullOrEmpty();
        assertThat(vitamError.getErrors()).isNullOrEmpty();
    }

    @Test
    public final void givenAccessConfiguration_whenInstanciateAnsSetsAttributes_ThenReturnTrue() {
        VitamError vitamError = new VitamError(code);
        vitamError.setContext(context).setDescription(description).setMessage(message).setState(state)
            .addAllErrors(errors).setCode(code);

        assertThat(vitamError.getCode()).isEqualTo(code);
        assertThat(vitamError.getContext()).isEqualTo(context);
        assertThat(vitamError.getState()).isEqualTo(state);
        assertThat(vitamError.getMessage()).isEqualTo(message);
        assertThat(vitamError.getDescription()).isEqualTo(description);
        assertThat(vitamError.getErrors()).isNullOrEmpty();
    }

}
