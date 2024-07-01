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
package fr.gouv.vitam.common.security;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

public class VitamLoggerLogTest {

    // The class is currently a dummy log class needed by ESAPI . We make dummy unit tests .
    // TODO P1 : If the VitamLoggerLog become a 'real' class, the unit test must be improved
    @Test
    public void test_getter_setter() {
        assertThatCode(() -> {
            final VitamLoggerLog vll = new VitamLoggerLog(null);
            vll.setLevel(0);
            vll.getESAPILevel();
            vll.fatal(null, null);
            vll.fatal(null, null, null);
            vll.isFatalEnabled();
            vll.error(null, null);
            vll.error(null, null, null);
            vll.isErrorEnabled();
            vll.warning(null, null);
            vll.warning(null, null, null);
            vll.isWarningEnabled();
            vll.info(null, null);
            vll.info(null, null, null);
            vll.isInfoEnabled();
            vll.debug(null, null);
            vll.debug(null, null, null);
            vll.isDebugEnabled();
            vll.trace(null, null);
            vll.trace(null, null, null);
            vll.isTraceEnabled();
            vll.always(null, null);
            vll.always(null, null, null);
        }).doesNotThrowAnyException();
    }
}
