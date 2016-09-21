/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
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
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */

package fr.gouv.vitam.storage.engine.common.referential;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.referential.model.HotStrategy;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 *
 */
public class FSProviderTest {

    @Test
    public void testGetStorageStrategy() throws Exception {
        FSProvider fsProvider = new FSProvider();
        StorageStrategy strategy = fsProvider.getStorageStrategy("whatever");
        assertEquals("default", strategy.getId());
        HotStrategy hot = strategy.getHotStrategy();
        assertNotNull(hot);
        assertEquals((Integer) 1, hot.getCopy());
        List<OfferReference> offerReferences = hot.getOffers();
        assertNotNull(offerReferences);
        assertEquals(1, offerReferences.size());
        OfferReference offerReference = offerReferences.get(0);
        assertEquals("default", offerReference.getId());
    }


    @Test
    public void testGetStorageStrategy_ForceReload() throws Exception {
        FSProvider fsProvider = new FSProvider();
        fsProvider.setStorageStrategy(null);
        StorageStrategy strategy = fsProvider.getStorageStrategy("whatever");
        assertEquals("default", strategy.getId());
        HotStrategy hot = strategy.getHotStrategy();
        assertNotNull(hot);
        assertEquals((Integer) 1, hot.getCopy());
        List<OfferReference> offerReferences = hot.getOffers();
        assertNotNull(offerReferences);
        assertEquals(1, offerReferences.size());
        OfferReference offerReference = offerReferences.get(0);
        assertEquals("default", offerReference.getId());
    }

    @Test(expected = StorageException.class)
    public void testGetStorageStrategy_ForceIoException() throws Exception {
        File strategy = PropertiesUtils.findFile("static-strategy.json");
        assertNotNull(strategy);
        strategy.setReadable(false);
        FSProvider fsProvider = new FSProvider();
        try {
            fsProvider.getStorageStrategy(null);
            fail("Expecting storage exception");
        } finally {
            strategy.setReadable(true);
        }
    }

    @Test
    public void testGetStorageOffer() throws Exception {
        FSProvider fsProvider = new FSProvider();
        StorageOffer offer = fsProvider.getStorageOffer("default");
        assertNotNull(offer);
        assertEquals("default", offer.getId());
        assertEquals("http://workspaceOfferService", offer.getBaseUrl());
        assertNotNull(offer.getParameters());
        assertEquals("bob", offer.getParameters().get("user"));
    }


    @Test
    public void testGetStorageOffer_ForceReload() throws Exception {
        FSProvider fsProvider = new FSProvider();
        fsProvider.setStorageOffer(null);
        StorageOffer offer = fsProvider.getStorageOffer("offerId");
        assertNotNull(offer);
        assertEquals("http://workspaceOfferService", offer.getBaseUrl());
        assertNotNull(offer.getParameters());
        assertEquals("bob", offer.getParameters().get("user"));
    }

    @Test(expected = StorageException.class)
    public void testGetStorageOffer_ForceIoException() throws Exception {
        File offer = PropertiesUtils.findFile("static-offer.json");
        assertNotNull(offer);
        offer.setReadable(false);
        FSProvider fsProvider = new FSProvider();
        try {
            fsProvider.getStorageOffer(null);
            fail("Expecting storage exception");
        } finally {
            offer.setReadable(true);
        }
    }
}
