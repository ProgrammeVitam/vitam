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
package fr.gouv.vitam.ihmrecette.soapui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Implementation of Soap UI Client that use command line executable Use a SoapUiConfig as configuration
 */
public class SoapUiClientCommand implements SoapUiClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SoapUiClientCommand.class);
    private static final String SOAP_UI_DESC_FILE = "story-tests.xml";

    private SoapUiConfig clientConfiguration;

    /**
     * Constructor : Create a new SoapUi client with the given configuration
     * 
     * @param clientConfiguration The client configuration
     */
    public SoapUiClientCommand(SoapUiConfig clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    @Override
    public void launchTests() throws IOException, InterruptedException {
        String executablePath = clientConfiguration.getSoapUiExecutable();

        String logbookProtocol = clientConfiguration.getAccessExternalProtocol();
        String logbookHostName = clientConfiguration.getAccessExternalHost();
        int logbookPort = clientConfiguration.getAccessExternalPort();
        String logbookHost = logbookProtocol + "://" + logbookHostName + ":" + logbookPort + "/";

        String ingestProtocol = clientConfiguration.getIngestProtocol();
        String ingestHostName = clientConfiguration.getIngestHost();
        int ingestPort = clientConfiguration.getIngestPort();
        String ingestHost = ingestProtocol + "://" + ingestHostName + ":" + ingestPort + "/";

        String certfile = clientConfiguration.getCertfile();
        String dataDir = clientConfiguration.getDataDir();
        String reportingDir = clientConfiguration.getReportingDir();
        String configDir = clientConfiguration.getConfigDir();

        String soapUiDescFilePath = PropertiesUtils.findFile(SOAP_UI_DESC_FILE).getAbsolutePath();

        StringBuilder cmdBuilder = new StringBuilder().append(executablePath);
        cmdBuilder.append(" -P ingestHost=").append(ingestHost);
        cmdBuilder.append(" -P logbookHost=").append(logbookHost);
        cmdBuilder.append(" -P certfile=").append(certfile);
        cmdBuilder.append(" -P dataDir=").append(dataDir);
        cmdBuilder.append(" -P reportingDir=").append(reportingDir);
        cmdBuilder.append(" -P configDir=").append(configDir);
        cmdBuilder.append(" ").append(soapUiDescFilePath);

        LOGGER.info("Launch SOAP ui test with command: " + cmdBuilder.toString());
        Process p = Runtime.getRuntime().exec(cmdBuilder.toString());

        boolean running = true;
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        while (running) {
            String line = reader.readLine();
            if (line != null) {
                LOGGER.info(line);
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    running = false;
                }
            }
            if (!p.isAlive()) {
                running = false;
            }
        }

        int exitVal = p.waitFor();
        LOGGER.debug("Exit val: " + exitVal);
    }

    @Override
    public JsonNode getLastTestReport() throws InvalidParseOperationException {
        String reportingDir = clientConfiguration.getReportingDir();
        File file = new File(reportingDir, "reporting.json");
        return JsonHandler.getFromFile(file);
    }

}
