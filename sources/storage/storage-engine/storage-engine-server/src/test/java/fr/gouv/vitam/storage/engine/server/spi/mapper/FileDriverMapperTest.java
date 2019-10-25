/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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

package fr.gouv.vitam.storage.engine.server.spi.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import fr.gouv.vitam.common.exception.VitamException;

public class FileDriverMapperTest {

    private static final String OFFER2 = "FileDriverMapperTestoffer2";
    private static final String OFFER1 = "FileDriverMapperTestoffer1";
    private static final String DRIVER1 = "FileDriverMapperTestdriver1";
    private static final String DRIVER2 = "FileDriverMapperTestdriver2";

    @After
    public void removeFiles() throws IOException {
        Files.deleteIfExists(Paths.get(DRIVER1));
        Files.deleteIfExists(Paths.get(DRIVER2));
    }

    @Test
    public void configurationOkTest() {
        FileDriverMapper fileDriverMapper = null;
        try {
            fileDriverMapper = FileDriverMapper.getInstance();
        } catch (final VitamException exc) {
            fail("should not raise an exception !");
        }
        assertNotNull(fileDriverMapper);
    }

    @Test
    public void addGetRemoveTest() throws VitamException {
        final FileDriverMapper fileDriverMapper = FileDriverMapper.getInstance();
        assertNotNull(fileDriverMapper);

        fileDriverMapper.addOfferTo(OFFER1, DRIVER1);

        List<String> offers = fileDriverMapper.getOffersFor(DRIVER1);
        assertNotNull(offers);
        assertEquals(1, offers.size());
        assertEquals(OFFER1, offers.get(0));

        fileDriverMapper.addOfferTo(OFFER1, DRIVER1);
        offers = fileDriverMapper.getOffersFor(DRIVER1);
        assertNotNull(offers);
        assertEquals(1, offers.size());
        assertEquals(OFFER1, offers.get(0));

        fileDriverMapper.addOfferTo(OFFER2, DRIVER1);
        offers = fileDriverMapper.getOffersFor(DRIVER1);
        assertNotNull(offers);
        assertEquals(2, offers.size());
        assertTrue(offers.contains(OFFER1));
        assertTrue(offers.contains(OFFER2));

        fileDriverMapper.removeOfferTo(OFFER1, DRIVER1);
        offers = fileDriverMapper.getOffersFor(DRIVER1);
        assertNotNull(offers);
        assertEquals(1, offers.size());
        assertEquals(OFFER2, offers.get(0));

        // remove offer that does not exist case
        fileDriverMapper.removeOfferTo("foo", DRIVER1);

        offers = fileDriverMapper.getOffersFor(DRIVER2);
        assertNotNull(offers);
        assertEquals(0, offers.size());
    }

    @Test
    public void addGetRemoveListTest() throws VitamException {
        final FileDriverMapper fileDriverMapper = FileDriverMapper.getInstance();
        assertNotNull(fileDriverMapper);

        final List<String> offers = new ArrayList<>();
        offers.add(OFFER1);
        offers.add(OFFER2);
        offers.add("FileDriverMapperTestoffer3");

        fileDriverMapper.addOffersTo(offers, DRIVER1);
        List<String> offersToCheck = fileDriverMapper.getOffersFor(DRIVER1);
        assertNotNull(offersToCheck);
        assertEquals(offers.size(), offersToCheck.size());
        assertTrue(offersToCheck.containsAll(offers));

        offers.remove(offers.indexOf(OFFER2));
        fileDriverMapper.removeOffersTo(offers, DRIVER1);
        offersToCheck = fileDriverMapper.getOffersFor(DRIVER1);
        assertNotNull(offersToCheck);
        assertEquals(1, offersToCheck.size());
        assertTrue(offersToCheck.contains(OFFER2));
    }
}
