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

package fr.gouv.vitam.storage.engine.server.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.junit.Test;

import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;

public class OfferDriverInfoTest {

    @Test(expected = IllegalArgumentException.class)
    public void offerDriverInfoInitNullTest() throws Exception {
        new OfferDriverInfo(null);
    }

    @Test
    public void offerDriverInfoTest() {
        TheDriver driver = new TheDriver();
        assertNotNull(driver);
        TheDriver driver2 = new TheDriver();
        assertNotNull(driver2);

        OfferDriverInfo offerDriverInfo = new OfferDriverInfo(driver);
        assertNotNull(offerDriverInfo);
        OfferDriverInfo offerDriverInfo2 = new OfferDriverInfo(driver2);
        assertNotNull(offerDriverInfo2);
        assertNotEquals(offerDriverInfo, offerDriverInfo2);

        OfferDriverInfo offerDriverInfo3 = new OfferDriverInfo(driver);
        assertNotNull(offerDriverInfo3);
        assertEquals(offerDriverInfo, offerDriverInfo3);
    }

    class TheDriver implements Driver {

        @Override public Connection connect(String url, Properties parameters) throws StorageDriverException {
            return null;
        }

        @Override public boolean isStorageOfferAvailable(String url, Properties parameters)
            throws StorageDriverException {
            return false;
        }

        @Override public String getName() {
            return null;
        }

        @Override public int getMajorVersion() {
            return 0;
        }

        @Override public int getMinorVersion() {
            return 0;
        }
    }
}
