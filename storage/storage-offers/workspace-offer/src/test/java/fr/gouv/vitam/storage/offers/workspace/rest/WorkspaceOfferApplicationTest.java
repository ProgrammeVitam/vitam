package fr.gouv.vitam.storage.offers.workspace.rest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;

import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.server.BasicVitamServer;

/**
 * WorkspaceOfferApplication Test
 */
public class WorkspaceOfferApplicationTest {
    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";

    private static final String WORKSPACE_OFFER_CONF = "workspace-offer.conf";
    private static final String WORKSPACE_OFFER2_CONF = "workspace-offer2.conf";

    @Test
    public final void testFictiveLaunch() {
        try {
            ((BasicVitamServer) WorkspaceOfferApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(WORKSPACE_OFFER_CONF).getAbsolutePath(), "-1"
            })).stop();
        } catch (final IllegalStateException | VitamApplicationServerException | FileNotFoundException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) WorkspaceOfferApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(WORKSPACE_OFFER_CONF).getAbsolutePath(), "-1xx"
            })).stop();
        } catch (final IllegalStateException | FileNotFoundException | VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) WorkspaceOfferApplication.startApplication(new String[0])).stop();
        } catch (final IllegalStateException | VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) WorkspaceOfferApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(WORKSPACE_OFFER_CONF).getAbsolutePath(), "666"})).stop();
        } catch (final IllegalStateException | FileNotFoundException | VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) WorkspaceOfferApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(WORKSPACE_OFFER2_CONF).getAbsolutePath(), "666"})).stop();
            fail("Should raize an FileNotFoundException");
        } catch (VitamApplicationServerException | FileNotFoundException | UnsupportedOperationException exc) {
            // Expected UnsupportedOperationException
            assertTrue(exc instanceof FileNotFoundException);
        }

    }
}
