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
package fr.gouv.vitam.storage.offers.rest;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * DefaultOfferMain Test
 */
public class DefaultOfferApplicationTest {

    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";

    private static final String DEFAULT_OFFER_CONF = "storage-default-offer.conf";
    private static final String WORKSPACE_OFFER_CONF = "workspace-offer2.conf";

    @Test
    public final void testFictiveLaunch() {
        try {
            new DefaultOfferMain(DEFAULT_OFFER_CONF);
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }

        try {
            new DefaultOfferMain(WORKSPACE_OFFER_CONF);
            fail("Should raize an IllegalStateException");
        } catch (final Exception exc) {
            // Result Expected
        }
    }

    @Test
    public void shouldActivateShiroFilter() {
        new DefaultOfferMain("src/test/resources/storage-default-offer-ssl.conf");
    }
}
