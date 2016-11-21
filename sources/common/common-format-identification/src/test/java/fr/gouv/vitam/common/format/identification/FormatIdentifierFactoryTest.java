package fr.gouv.vitam.common.format.identification;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierFactoryException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierConfiguration;
import fr.gouv.vitam.common.format.identification.siegfried.FormatIdentifierSiegfried;

public class FormatIdentifierFactoryTest {

    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not have raized an exception";
    private static final String SHOULD_RAIZED_AN_EXCEPTION = "Should have raized an exception";

    @Test
    public void testLoad() {
        FormatIdentifierFactory.getInstance().changeConfigurationFile("format-identifiers-factory-test.conf");
        try {
            final FormatIdentifier test1 =
                FormatIdentifierFactory.getInstance().getFormatIdentifierFor("test-siegfried");
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
            final FormatIdentifier testSiegfriedMock =
                FormatIdentifierFactory.getInstance().getFormatIdentifierFor("test-siegfried-mock");
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
