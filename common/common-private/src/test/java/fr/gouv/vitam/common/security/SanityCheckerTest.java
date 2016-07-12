/**
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

package fr.gouv.vitam.common.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

public class SanityCheckerTest {

    private String pathXMLOK = "testOK.xml";
    private String pathXMLKO = "testKO.xml";
    private File fileOK = null;
    private File fileKO = null;
    
    
    private String JSON_TEST_FILE = "json";
    private String JSON_TEST_FILE2 = "json_good_sanity";
    private final double limitFileSize = SanityChecker.getLimitFileSize();
    private final int limitValueTagSize = SanityChecker.getLimitValueTagSize();
    private final int limitJsonSize = SanityChecker.getLimitJsonSize();
    

    @Before
    public void setUp() throws FileNotFoundException {

        fileOK = PropertiesUtils.findFile(pathXMLOK); 
        fileKO = PropertiesUtils.findFile(pathXMLKO);
    }
    
    @After
    public void tearDown() {
        SanityChecker.setLimitJsonSize(limitJsonSize);
        SanityChecker.setLimitFileSize(limitFileSize);
        SanityChecker.setLimitValueTagSize(limitValueTagSize);
    }

    @Test(expected = IOException.class)
    public void checkXMLFileSize() throws IOException {
        SanityChecker.setLimitFileSize(100);
        SanityChecker.checkXMLSanityFileSize(fileOK);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void checkXMLTagSize() throws IOException, InvalidParseOperationException {
        SanityChecker.setLimitValueTagSize(100);
        SanityChecker.checkXMLSanityTagValueSize(fileKO);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void checkXMLTags() throws IOException, InvalidParseOperationException {
        SanityChecker.checkXMLSanityTags(fileKO);
    }

    @Test
    public void checkXMLAllOK() throws IOException, InvalidParseOperationException {
        assertEquals(SanityChecker.checkXMLAll(fileOK), true);
    }

    @Test
    public void givenJsonWhenValueIsTooBigORContainXMLTagThenStoreInList()    
        throws InvalidParseOperationException, IOException { 
        File file = PropertiesUtils.findFile(JSON_TEST_FILE); 
        JsonNode json = JsonHandler.getFromFile(file);
        assertNotNull(json);

        List<String> list = SanityChecker.checkJsonSanity(json);
        assertEquals(2, list.size());
        assertTrue(list.contains("tagbug"));
        assertTrue(list.contains("sizebug"));
        assertFalse(SanityChecker.checkJsonAll(json));
    }
    
    @Test()
    public void givenJsonWhenGoodSanityThenReturnTrue() 
        throws FileNotFoundException, InvalidParseOperationException{
        SanityChecker.setLimitJsonSize(100);
        File file = PropertiesUtils.findFile(JSON_TEST_FILE2); 
        JsonNode json = JsonHandler.getFromFile(file);        
        assertFalse(SanityChecker.checkJsonAll(json));
    }
}
