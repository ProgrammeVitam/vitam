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
package fr.gouv.vitam.processing.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.processing.WorkFlow;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Temporary process populator
 *
 * find and populates workflow java object
 *
 */
public class ProcessPopulator {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessPopulator.class);

    private static final String INTERNAL_WORKFLOWS_LIST_CONFIG = "workflows.conf";
    private static final String EXTERNAL_WORKFLOWS_FOLDER = "processing/workflows";

    private ProcessPopulator() {
        // empty constructor
    }

    /**
     * loadWorkflows, find and load all workflows
     *
     * @param poolWorkflows map to populate with workflows
     * @throws WorkflowNotFoundException when workflows folder is not found or unable to read a workflow file
     */
    public static void loadWorkflows(final Map<String, WorkFlow> poolWorkflows) throws WorkflowNotFoundException {
        loadInternalWorkflows(poolWorkflows);
        loadExternalWorkflows(poolWorkflows, null);
    }

    /**
     * reloadWorkflows, find and load new (added) workflow objects
     *
     * @param poolWorkflows poolWorkflows map of workflows to update
     * @param fromDate datetime on milliseconds to filter from, if null no filter is applied
     * @throws WorkflowNotFoundException when workflows folder is not found or unable to read a workflow file
     */
    public static void reloadWorkflows(final Map<String, WorkFlow> poolWorkflows, Long fromDate) throws WorkflowNotFoundException {
        loadExternalWorkflows(poolWorkflows, fromDate);
    }

    /**
     * loadInternalWorkflows, find and load workflows from resources folder
     * (list of files must be set in 'workflows.conf' file)
     *
     * @param poolWorkflows map to populate with workflows
     * @throws WorkflowNotFoundException when workflows folder is not found or unable to read a workflow file
     */
    private static void loadInternalWorkflows(final Map<String, WorkFlow> poolWorkflows) throws WorkflowNotFoundException {
        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("Loading internal workflow resources...");
        }

        List<String> workflowFiles = getInternalWorkflowResources();

        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("Found " + workflowFiles.size() + " workflow resource files");
        }

        for (String workflowFile : workflowFiles) {
            if(workflowFile.toLowerCase().endsWith(".json")) {
                populate(poolWorkflows, workflowFile, false);
            }
        }
    }

    /**
     * loadExternalWorkflows, find and load workflows from config folder
     *
     * @param poolWorkflows map to populate with workflows
     * @param fromDate datetime on milliseconds to filter from, if null no filter is applied
     * @throws WorkflowNotFoundException when workflows folder is not found or unable to read a workflow file
     */
    private static void loadExternalWorkflows(Map<String, WorkFlow> poolWorkflows, Long fromDate) throws WorkflowNotFoundException {
        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("Loading external workflow resources...");
        }

        File[] workflowFiles = getExternalWorkflowFiles(fromDate);

        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("Found " + workflowFiles.length + " workflow resource files");
        }

        for (File workflowFile : workflowFiles) {
            populate(poolWorkflows, workflowFile.getAbsolutePath(), true);
        }
    }

    /**
     * getExternalWorkflowFiles, find external workflow files from config folder
     *
     * @param fromDate datetime on milliseconds to filter from, if null no filter is applied
     * @return list of workflow (json) files
     * @throws WorkflowNotFoundException if workflow folder not found in config directory
     */
    private static File[] getExternalWorkflowFiles(Long fromDate) throws WorkflowNotFoundException {
        File workflowFolder = PropertiesUtils.fileFromConfigFolder(EXTERNAL_WORKFLOWS_FOLDER);

        if(!workflowFolder.isDirectory()){
            LOGGER.warn("DirectoryNotFoundException thrown by populator: {}", workflowFolder);
            throw new WorkflowNotFoundException("DirectoryNotFoundException thrown by populator");
        }
        else {
            return workflowFolder.listFiles(
                    (file) -> (file.getName().toLowerCase().endsWith(".json"))
                            && (fromDate == null || file.lastModified() > fromDate)
            );
        }
    }

    /**
     * getInternalWorkflowResources, get list of workflow files in resources folder
     * (list of files must be set in 'workflows.conf' file)
     *
     * @return list of file's path
     * @throws WorkflowNotFoundException if config file not found or unable to read it
     */
    private static List<String> getInternalWorkflowResources() throws WorkflowNotFoundException  {
        try {
            return IOUtils.readLines(PropertiesUtils.getResourceAsStream(INTERNAL_WORKFLOWS_LIST_CONFIG),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("IOException thrown by populator", e);
            throw new WorkflowNotFoundException("IOException thrown by populator", e);
        }
    }

    /**
     * populate, create and add workflow object (parse JSON file) to workflows map
     *
     * @param poolWorkflows to populate with workflows
     * @param workflowFile to parse
     * @param update if true override existing workflow
     * @throws WorkflowNotFoundException throws when workflow file not found or unable to parse it
     */
    private static void populate(final Map<String, WorkFlow> poolWorkflows, String workflowFile, boolean update){
        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("Populating workflows using file " + workflowFile);
        }

        // parse the workflow file
        WorkFlow workflow = populate(workflowFile);

        // add the parsed workflow object
        if(workflow != null){

            String wfIdentifier = workflow.getIdentifier();

            if(LOGGER.isDebugEnabled()){
                LOGGER.debug("Parsed workflow with identifier " + wfIdentifier);
            }

            if(update){
                poolWorkflows.put(wfIdentifier, workflow);
            }
            else {
                poolWorkflows.putIfAbsent(wfIdentifier, workflow);
            }
        }
    }

    /**
     * populate, create workflow object (parse JSON file)
     *
     * @param workflowFile the workflow file (path)
     * @return workflow's object
     * @throws WorkflowNotFoundException throws when workflow file not found or unable to parse it
     */
    public static WorkFlow populate(String workflowFile) throws WorkflowNotFoundException {
        ParametersChecker.checkParameter("workflowFile is a mandatory parameter", workflowFile);
        final ObjectMapper objectMapper = new ObjectMapper();
        WorkFlow process = null;
        try (final InputStream inputJSON = PropertiesUtils.getConfigAsStream(workflowFile)) {
            process = objectMapper.readValue(inputJSON, WorkFlow.class);
        } catch (final IOException e) {
            LOGGER.error("IOException thrown by populator", e);
            throw new WorkflowNotFoundException("IOException thrown by populator", e);
        }
        return process;
    }

}