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
package fr.gouv.vitam.common.workflow;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ProcessContextLoader {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessContextLoader.class);

    private static final ProcessContextLoader INSTANCE = new ProcessContextLoader();

    private static final String PROCESS_CONTEXT_FILE = "processContext.json";
    private Map<String, ProcessContext> internalProcessContext;


    private ProcessContextLoader() {
        internalProcessContext = loadInternalProcessContext();
    }

    public static ProcessContextLoader get() {
        return INSTANCE;
    }

    private Map<String, ProcessContext> loadProcessContext(InputStream inputStream) throws IOException {
        try {
            return JsonHandler.getFromInputStream(inputStream, HashMap.class, String.class, ProcessContext.class);
        } catch (InvalidParseOperationException e) {
            throw new IOException(e);
        }
    }


    /**
     * getProcessContext, get ProcessContext from config files
     *
     * @param contextId the contextId
     * @return ProcessContext's object
     * @throws ContextNotFoundException if context not found from file data
     */
    public ProcessContext getProcessContext(String contextId) throws ContextNotFoundException {

        // Try get external Context
        Map<String, ProcessContext> externalProcessContext = loadExternalProcessContext();
        ProcessContext processContext = externalProcessContext.get(contextId);
        if (null != processContext) {
            return processContext;
        }

        processContext = internalProcessContext.get(contextId);
        if (null != processContext) {
            return processContext;
        }
        throw new ContextNotFoundException("Context id :" + contextId + " not found in :" + internalProcessContext);
    }

    private Map<String, ProcessContext> loadInternalProcessContext() {
        try {
            return loadProcessContext(PropertiesUtils.getResourceAsStream(PROCESS_CONTEXT_FILE));
        } catch (Exception e) {
            LOGGER.error("Error while load internal process context", e);
            throw new RuntimeException(e);
        }
    }

    public Map<String, ProcessContext> loadExternalProcessContext() {
        try {
            File file = PropertiesUtils.fileFromConfigFolder(PROCESS_CONTEXT_FILE);
            LOGGER.warn("load external file : {} / {}. FullPath: {}", VitamConfiguration.getVitamConfigFolder(), PROCESS_CONTEXT_FILE, file.getAbsolutePath());
            return loadProcessContext(
                    Files.newInputStream(
                            file.toPath()
                    )
            );
        } catch (Exception e) {
            LOGGER.warn("Error while load internal process context", e);
            return new HashMap<>();
        }
    }

    public Map<String, ProcessContext> getInternalProcessContext() {
        return internalProcessContext;
    }
}
