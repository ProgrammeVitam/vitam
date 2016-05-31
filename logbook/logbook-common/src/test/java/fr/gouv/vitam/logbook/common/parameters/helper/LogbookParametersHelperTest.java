/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.logbook.common.parameters.helper;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.base.Strings;

/**
 * Logbook Paramaters Helper test
 */
public class LogbookParametersHelperTest {

    @Test
    public void checkNullOrEmptyParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("field1", "field1Value");
        parameters.put("field2", "");

        assertNotNull(parameters);
        assertNotNull(parameters.get("field2"));
        assertTrue(Strings.isNullOrEmpty(parameters.get("field2")));

        Set<String> mandatories = new HashSet<>();
        mandatories.add("field1");
        mandatories.add("field2");

        boolean catchException = false;
        try {
            LogbookParametersHelper.checkNullOrEmptyParameters(parameters, mandatories);
        } catch (IllegalArgumentException iae) {
            catchException = true;
        }
        assertTrue(catchException);

        mandatories.remove("field2");
        catchException = false;
        try {
            LogbookParametersHelper.checkNullOrEmptyParameters(parameters, mandatories);
        } catch (IllegalArgumentException iae) {
            catchException = true;
        }
        assertFalse(catchException);

        mandatories.add("field2");
        parameters.put("field2", "field2");
        catchException = false;
        try {
            LogbookParametersHelper.checkNullOrEmptyParameters(parameters, mandatories);
        } catch (IllegalArgumentException iae) {
            catchException = true;
        }
        assertFalse(catchException);

        parameters.put("field3", null);
        catchException = false;
        try {
            LogbookParametersHelper.checkNullOrEmptyParameters(parameters, mandatories);
        } catch (IllegalArgumentException iae) {
            catchException = true;
        }
        assertFalse(catchException);

        mandatories.add("field3");
        catchException = false;
        try {
            LogbookParametersHelper.checkNullOrEmptyParameters(parameters, mandatories);
        } catch (IllegalArgumentException iae) {
            catchException = true;
        }
        assertTrue(catchException);

        parameters.remove("field3");
        catchException = false;
        try {
            LogbookParametersHelper.checkNullOrEmptyParameters(parameters, mandatories);
        } catch (IllegalArgumentException iae) {
            catchException = true;
        }
        assertTrue(catchException);
    }
}
