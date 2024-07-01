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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class FormattingTupleTest {

    private static final String NON_SENSICAL_EMPTY_OR_NULL_ARGUMENT_ARRAY = "non-sensical empty or null argument array";

    @Test
    public void testTuple() {
        Object[] objectArray0 = new Object[1];
        FormattingTuple formattingTuple0 = new FormattingTuple((String) null, objectArray0, (Throwable) null);
        String string0 = formattingTuple0.getMessage();
        assertNull(string0);
        objectArray0 = new Object[1];
        formattingTuple0 = new FormattingTuple((String) null, objectArray0, (Throwable) null);
        Object[] objectArray1 = formattingTuple0.getArgArray();
        assertSame(objectArray0, objectArray1);
        objectArray0 = new Object[0];
        formattingTuple0 = new FormattingTuple("@2tO", objectArray0, (Throwable) null);
        objectArray1 = formattingTuple0.getArgArray();
        assertSame(objectArray0, objectArray1);
        objectArray0 = new Object[9];
        objectArray1 = FormattingTuple.trimmedCopy(objectArray0);
        assertNotSame(objectArray1, objectArray0);
        objectArray0 = new Object[1];
        objectArray1 = FormattingTuple.trimmedCopy(objectArray0);
        assertNotSame(objectArray1, objectArray0);
        formattingTuple0 = new FormattingTuple(NON_SENSICAL_EMPTY_OR_NULL_ARGUMENT_ARRAY);
        string0 = formattingTuple0.getMessage();
        assertEquals(NON_SENSICAL_EMPTY_OR_NULL_ARGUMENT_ARRAY, string0);
        objectArray0 = new Object[1];
        formattingTuple0 = new FormattingTuple((String) null, objectArray0, (Throwable) null);
        final Throwable throwable0 = formattingTuple0.getThrowable();
        assertNull(throwable0);
        formattingTuple0 = new FormattingTuple(NON_SENSICAL_EMPTY_OR_NULL_ARGUMENT_ARRAY);
        objectArray0 = formattingTuple0.getArgArray();
        assertNull(objectArray0);
        objectArray0 = new Object[1];
        formattingTuple0 = new FormattingTuple((String) null, objectArray0, new Throwable("test"));
        assertNotNull(formattingTuple0.getArgArray());
    }

    @Test
    public void testError() {
        final Object[] objectArray0 = new Object[0];
        try {
            FormattingTuple.trimmedCopy(objectArray0);
            fail("Expecting exception: IllegalStateException");
        } catch (final IllegalStateException e) { // NOSONAR
            // Ignore
        }
        try {
            FormattingTuple.trimmedCopy((Object[]) null);
            fail("Expecting exception: IllegalStateException");
        } catch (final IllegalStateException e) { // NOSONAR
            // Ignore
        }
    }
}
