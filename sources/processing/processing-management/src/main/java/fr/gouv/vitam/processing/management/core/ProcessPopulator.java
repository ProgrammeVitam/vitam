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
package fr.gouv.vitam.processing.management.core;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.processing.WorkFlow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Temporary process populator
 * <p>
 * find and populates workflow java object
 */
public class ProcessPopulator {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessPopulator.class);

    private static final String WORKFLOWS_FOLDER = "workflows/";

    private ProcessPopulator() {
        // empty constructor
    }

    /**
     * loadWorkflow, find and load all workflows
     *
     * @param poolWorkflows map to populate with workflows
     */
    public static void loadWorkflow(final Map<String, WorkFlow> poolWorkflows) {
        loadInternalWorkflow(poolWorkflows);
        loadExternalWorkflow(poolWorkflows, Instant.EPOCH.toEpochMilli());
    }

    /**
     * reloadWorkflow, find and load new (added) workflow objects
     *
     * @param poolWorkflows poolWorkflows map of workflows to update
     * @param fromDate datetime on milliseconds to filter from, if null no filter is applied
     */
    public static void reloadWorkflow(final Map<String, WorkFlow> poolWorkflows, Long fromDate) {
        loadExternalWorkflow(poolWorkflows, fromDate);
    }

    /**
     * loadInternalWorkflow, find and load workflows from resources folder
     * (list of files must be set in 'workflows.conf' file)
     *
     * @param poolWorkflows map to populate with workflows
     */
    private static void loadInternalWorkflow(final Map<String, WorkFlow> poolWorkflows) {
        LOGGER.debug("Loading internal workflow resources...");

        List<String> workflowFiles;
        try {
            workflowFiles = PropertiesUtils.getResourceListing(ProcessPopulator.class, WORKFLOWS_FOLDER)
                .filter(f -> f.endsWith(".json"))
                .map(f -> WORKFLOWS_FOLDER + f)
                .distinct()
                .collect(Collectors.toList());
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("Error while load internal workflow", e);
            throw new RuntimeException(e);
        }

        LOGGER.debug("Found " + workflowFiles.size() + " workflow resource files: " + workflowFiles);

        for (String workflowFile : workflowFiles) {
            try {
                LOGGER.debug("populate internal : " + workflowFile);
                populate(poolWorkflows, PropertiesUtils.getResourceAsStream(workflowFile), false);
            } catch (FileNotFoundException e) {
                LOGGER.error("Cannot load workflow file (" + workflowFile + ") ", e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * loadExternalWorkflow, find and load workflows from config folder
     *
     * @param poolWorkflows map to populate with workflows
     * @param fromDate datetime on milliseconds to filter from, if null no filter is applied
     */
    private static void loadExternalWorkflow(Map<String, WorkFlow> poolWorkflows, Long fromDate) {
        LOGGER.debug("Loading external workflow resources...");

        List<Path> workflowFiles = loadExternalWorkflowFiles(fromDate);

        LOGGER.debug("Found " + workflowFiles.size() + " workflow resource files");

        for (Path workflowFile : workflowFiles) {
            try {
                LOGGER.debug("populate external : " + workflowFile.toString());
                populate(poolWorkflows, Files.newInputStream(workflowFile), true);
            } catch (IOException e) {
                // Do not block system for external workflow
                LOGGER.warn(workflowFile.toString(), e);
            }
        }
    }

    /**
     * loadExternalWorkflowFiles, find external workflow files from config folder
     *
     * @param fromDate datetime on milliseconds to filter from, if null no filter is applied
     * @return list of workflow (json) files
     */
    private static List<Path> loadExternalWorkflowFiles(Long fromDate) {
        LOGGER.debug("load external file : {}{}", VitamConfiguration.getVitamConfigFolder(), WORKFLOWS_FOLDER);
        File workflowFolder = PropertiesUtils.fileFromConfigFolder(WORKFLOWS_FOLDER);

        if (!workflowFolder.isDirectory()) {
            LOGGER.debug("DirectoryNotFoundException thrown by populator: {}", workflowFolder);
            return new ArrayList<>();
        } else {
            LOGGER.debug("load external :" + workflowFolder.toPath().toString());
            try (Stream<Path> stream = Files.list(workflowFolder.toPath())) {
                return stream
                    .filter(f -> f.toFile().isFile())
                    .filter(f -> f.toFile().getName().endsWith(".json"))
                    .filter(f -> f.toFile().lastModified() > fromDate)
                    .distinct()
                    .collect(Collectors.toList());
            } catch (IOException e) {
                LOGGER.error("Error while list external workflow folder", e);
                return new ArrayList<>();
            }
        }
    }

    /**
     * populate, create and add workflow object (parse JSON file) to workflows map
     *
     * @param poolWorkflows to populate with workflows
     * @param workflowFile to parse
     * @param update if true override existing workflow
     */
    private static void populate(final Map<String, WorkFlow> poolWorkflows, InputStream workflowFile, boolean update) {
        LOGGER.debug("Populating workflow using file " + workflowFile);

        // parse the workflow file
        Optional<WorkFlow> workflow = populate(workflowFile);

        // add the parsed workflow object
        if (workflow.isPresent()) {
            String wfId = workflow.get().getId();

            LOGGER.debug("Parsed workflow with id " + wfId);

            if (update) {
                poolWorkflows.put(wfId, workflow.get());
            } else {
                poolWorkflows.putIfAbsent(wfId, workflow.get());
            }
        }
    }

    /**
     * populate, create workflow object (parse JSON file)
     *
     * @param workflowFileStream the workflow file (path)
     * @return workflow's object
     */
    public static Optional<WorkFlow> populate(InputStream workflowFileStream) {
        ParametersChecker.checkParameter("WorkflowFile is a mandatory parameter", workflowFileStream);
        try {
            return Optional.of(JsonHandler.getFromInputStream(workflowFileStream, WorkFlow.class));
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error while load workflow :", e);
            return Optional.empty();
        }
    }

    @VisibleForTesting
    public static Optional<WorkFlow> populate(String workflowFile) {
        try {
            return populate(PropertiesUtils.getResourceAsStream(workflowFile));
        } catch (FileNotFoundException e) {
            LOGGER.error(workflowFile, e);
            return Optional.empty();
        }
    }
}
