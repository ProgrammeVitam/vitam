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

package fr.gouv.vitam.common.error;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class VitamErrorTest {

    private static VitamError vitamError = new VitamError("1");

    private static final String ERROR_JSON =
        "{\"httpCode\":0,\"code\":\"0\",\"context\":\"context\",\"state\":\"state\"," +
        "\"message\":\"message\",\"description\":\"description\",\"errors\":" +
        "[{\"httpCode\":0,\"code\":\"1\"}]}";

    @Test
    public void testSetGetCode() throws Exception {
        vitamError.setCode("2");
        assertEquals("2", vitamError.getCode());
    }

    @Test
    public void testSetGetContext() throws Exception {
        vitamError.setContext("2");
        assertEquals("2", vitamError.getContext());
    }

    @Test
    public void testSetGetState() throws Exception {
        vitamError.setState("2");
        assertEquals("2", vitamError.getState());
    }

    @Test
    public void testSetGetMessage() throws Exception {
        vitamError.setMessage("2");
        assertEquals("2", vitamError.getMessage());
    }

    @Test
    public void testSetGetDescription() throws Exception {
        vitamError.setDescription("2");
        assertEquals("2", vitamError.getDescription());
    }

    @Test
    public void testSetErrors() throws Exception {
        final List<VitamError> errorList = new ArrayList<>();
        errorList.add(vitamError);
        vitamError.addAllErrors(errorList);
        assertTrue(vitamError.getErrors().contains(vitamError));
    }

    @Test
    public void testToString() throws Exception {
        final VitamError error = new VitamError("0");
        error.setMessage("message");
        error.setDescription("description");
        error.setState("state");
        error.setContext("context");
        error.addAllErrors(Collections.singletonList(new VitamError("1")));
        assertEquals(ERROR_JSON, error.toString());
    }
}
