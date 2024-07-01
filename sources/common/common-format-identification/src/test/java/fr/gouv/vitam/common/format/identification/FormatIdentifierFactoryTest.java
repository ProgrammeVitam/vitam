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
package fr.gouv.vitam.common.format.identification;

import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierFactoryException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierConfiguration;
import fr.gouv.vitam.common.format.identification.siegfried.FormatIdentifierSiegfried;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FormatIdentifierFactoryTest {

    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not have raized an exception";
    private static final String SHOULD_RAIZED_AN_EXCEPTION = "Should have raized an exception";

    @Test
    public void testLoad() {
        FormatIdentifierFactory.getInstance().changeConfigurationFile("format-identifiers-factory-test.conf");
        try {
            final FormatIdentifier test1 = FormatIdentifierFactory.getInstance()
                .getFormatIdentifierFor("test-siegfried");
            assertNotNull(test1);
            assertTrue(test1 instanceof FormatIdentifierSiegfried);
        } catch (final VitamException e3) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }

        try {
            FormatIdentifierFactory.getInstance().getFormatIdentifierFor("test1notexist");
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final FormatIdentifierNotFoundException e) {
            // Nothing
        } catch (final VitamException e3) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }

        try {
            final FormatIdentifier testSiegfriedMock = FormatIdentifierFactory.getInstance()
                .getFormatIdentifierFor("test-siegfried-mock");
            assertNotNull(testSiegfriedMock);
            assertTrue(testSiegfriedMock instanceof FormatIdentifierSiegfried);
        } catch (final VitamException e3) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }

        try {
            final FormatIdentifier test2 = FormatIdentifierFactory.getInstance().getFormatIdentifierFor("test2");
            assertNotNull(test2);
            assertTrue(test2 instanceof FormatIdentifierMock);
        } catch (final VitamException e2) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            FormatIdentifierFactory.getInstance().addFormatIdentifier("test3", new FormatIdentifierConfiguration());
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e1) {
            // nothing
        }

        final FormatIdentifierConfiguration test3 = new FormatIdentifierConfiguration();
        test3.setType(FormatIdentifierType.MOCK);
        FormatIdentifierFactory.getInstance().addFormatIdentifier("test3", test3);
        try {
            FormatIdentifierFactory.getInstance().getFormatIdentifierFor("test3");
        } catch (final VitamException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }

        try {
            FormatIdentifierFactory.getInstance().addFormatIdentifier(null, null);
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e1) {
            // nothing
        }

        try {
            FormatIdentifierFactory.getInstance().removeFormatIdentifier(null);
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e1) {
            // nothing
        } catch (final FormatIdentifierNotFoundException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }

    @Test
    public void testAddRemoveConfiguration() {
        final FormatIdentifierConfiguration test3 = new FormatIdentifierConfiguration();
        test3.setType(FormatIdentifierType.MOCK);
        FormatIdentifierFactory.getInstance().addFormatIdentifier("test3", test3);
        try {
            FormatIdentifierFactory.getInstance().getFormatIdentifierFor("test3");
        } catch (final VitamException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }

        try {
            FormatIdentifierFactory.getInstance().getFormatIdentifierFor(null);
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final VitamException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e) {
            // nothing
        }

        try {
            FormatIdentifierFactory.getInstance().getFormatIdentifierFor("test4");
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final FormatIdentifierFactoryException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final FormatIdentifierNotFoundException e) {
            // nothing
        } catch (final FormatIdentifierTechnicalException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }

        try {
            FormatIdentifierFactory.getInstance().addFormatIdentifier(null, null);
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e1) {
            // nothing
        }

        try {
            FormatIdentifierFactory.getInstance().removeFormatIdentifier(null);
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final FormatIdentifierNotFoundException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final IllegalArgumentException e1) {
            // nothing
        }

        try {
            FormatIdentifierFactory.getInstance().removeFormatIdentifier("test3");
        } catch (final FormatIdentifierNotFoundException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }

        try {
            FormatIdentifierFactory.getInstance().removeFormatIdentifier("test4");
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (final FormatIdentifierNotFoundException e) {
            // Nothing
        }
    }
}
