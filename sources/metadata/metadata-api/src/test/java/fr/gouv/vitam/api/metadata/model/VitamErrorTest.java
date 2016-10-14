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
package fr.gouv.vitam.api.metadata.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fr.gouv.vitam.common.SingletonUtils;
import fr.gouv.vitam.metadata.api.model.VitamError;

public class VitamErrorTest {

    private static final int CODE = 0;
    private static final String CONTEXT = "Context";
    private static final String DESCRIPTION = "Description";
    private static final String MESSAGE = "Message";
    private static final String STATE = "State";

    @Test
    public void testVitamError() throws Exception {
        assertEquals(CODE, new VitamError(CODE).getCode());
        assertEquals("", new VitamError(CODE).getContext());
        assertEquals("", new VitamError(CODE).getDescription());
        assertEquals("", new VitamError(CODE).getMessage());
        assertEquals("", new VitamError(CODE).getState());
        assertEquals(SingletonUtils.singletonList(), new VitamError(CODE).getErrors());

        assertEquals(123, new VitamError(CODE).setCode(123).getCode());
        assertEquals(CONTEXT, new VitamError(CODE).setContext(CONTEXT).getContext());
        assertEquals(DESCRIPTION, new VitamError(CODE).setDescription(DESCRIPTION).getDescription());
        assertEquals(MESSAGE, new VitamError(CODE).setMessage(MESSAGE).getMessage());
        assertEquals(STATE, new VitamError(CODE).setState(STATE).getState());
        final List<VitamError> ERRORS = new ArrayList<VitamError>();
        ERRORS.add(new VitamError(CODE));
        assertEquals(ERRORS, new VitamError(CODE).setErrors(ERRORS).getErrors());

    }

}
