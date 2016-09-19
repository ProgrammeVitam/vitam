package fr.gouv.vitam.storage.offers.workspace.rest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;

import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamException;

/**
 * DefaultOfferApplication Test
 */
public class DefaultOfferApplicationTest {
    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";

    private static final String WORKSPACE_OFFER_CONF = "default-offer.conf";
    private static final String WORKSPACE_OFFER2_CONF = "workspace-offer2.conf";

    @Test
    public final void testFictiveLaunch() {
        try {
            DefaultOfferApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(WORKSPACE_OFFER_CONF).getAbsolutePath()});
            DefaultOfferApplication.stop();
        } catch (final IllegalStateException | VitamException |

            FileNotFoundException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            DefaultOfferApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(WORKSPACE_OFFER_CONF).getAbsolutePath()});
            DefaultOfferApplication.stop();
        } catch (final IllegalStateException | FileNotFoundException |
            VitamException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            DefaultOfferApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(WORKSPACE_OFFER2_CONF).getAbsolutePath()});
            fail("Should raize an FileNotFoundException");
        } catch (VitamException | FileNotFoundException |
            UnsupportedOperationException exc) {
            // Expected UnsupportedOperationException
            assertTrue(exc instanceof FileNotFoundException);
        }
    }
}
