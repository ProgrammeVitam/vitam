/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.ingest.external.core;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;

public class IngestExternalImplTest {
    private static final String PATH = "/tmp";
    private static final String SCRIPT_SCAN_CLAMAV = "scan-clamav.sh";
    IngestExternalImpl ingestExternalImpl;
    private InputStream stream;
    
    private static final long timeoutScanDelay = 60000;
    
    @Before
    public void setUp(){
        IngestExternalConfiguration config = new IngestExternalConfiguration();
        config.setPath(PATH);
        config.setAntiVirusScriptName(SCRIPT_SCAN_CLAMAV);
        config.setTimeoutScanDelay(timeoutScanDelay);
        ingestExternalImpl = new IngestExternalImpl(config);
    }

    @Test
    public void givenNoVirusFile() throws Exception {
        stream = PropertiesUtils.getResourcesAsStream("no-virus.txt");
        Response xmlResponse= ingestExternalImpl.upload(stream);
        InputStream inputstream = PropertiesUtils.getResourcesAsStream("ATR_example.xml");
        assertEquals(xmlResponse.getEntity(), FileUtil.readInputStream(inputstream));
    }
    
    @Test
    public void givenFixedVirusFile() throws IngestExternalException, FileNotFoundException, XMLStreamException {
        stream = PropertiesUtils.getResourcesAsStream("fixed-virus.txt");
        ingestExternalImpl.upload(stream);
    }
    
    @Test
    public void givenUnfixedVirusFile() throws IngestExternalException, FileNotFoundException, XMLStreamException {
        stream = PropertiesUtils.getResourcesAsStream("unfixed-virus.txt");
        ingestExternalImpl.upload(stream);
    }
    
    @Test
    public void givenUnknownErrorFile() throws IngestExternalException, FileNotFoundException, XMLStreamException {
        stream = PropertiesUtils.getResourcesAsStream("unknown.txt");
        ingestExternalImpl.upload(stream);
    }

}
