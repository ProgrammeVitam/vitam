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
package fr.gouv.vitam.processing.common.model;

import fr.gouv.vitam.common.CommonMediaType;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Workspace Queue class
 */
public class WorkspaceQueue {

    private final String workspacePath;
    private final InputStream sourceFile;
    private final WorkspaceAction action;
    private String mediaType = CommonMediaType.TAR;
    private String folderName = "";
    private Path filePath = null;

    /**
     * Default Constructor
     */
    public WorkspaceQueue() {
        this.workspacePath = null;
        this.sourceFile = null;
        this.action = WorkspaceAction.INTERRUPT;
    }

    /**
     * Constructor
     *
     * @param workspacePath
     * @param sourceFile
     */
    public WorkspaceQueue(String workspacePath, InputStream sourceFile) {
        this.workspacePath = workspacePath;
        this.sourceFile = sourceFile;
        this.action = WorkspaceAction.TRANSFER;
    }

    /**
     * Constructor
     *
     * @param workspacePath
     * @param sourceFile
     * @param action
     */
    public WorkspaceQueue(String workspacePath, InputStream sourceFile, WorkspaceAction action) {
        this.workspacePath = workspacePath;
        this.sourceFile = sourceFile;
        this.action = action;
    }

    /**
     * @return workspace path
     */
    public String getWorkspacePath() {
        return workspacePath;
    }

    /**
     * @return source file
     */
    public InputStream getSourceFile() {
        return sourceFile;
    }

    /**
     * @return action
     */
    public WorkspaceAction getAction() {
        return action;
    }

    /**
     * @return file path
     */
    public Path getFilePath() {
        return filePath;
    }

    /**
     * @param filePath
     * @return workspace queue
     */
    public WorkspaceQueue setFilePath(Path filePath) {
        this.filePath = filePath;
        return this;
    }

    /**
     * @return media type
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * @param mediaType
     * @return the modified WorkspaceQueue
     */
    public WorkspaceQueue setMediaType(String mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    /**
     * @return folder name
     */
    public String getFolderName() {
        return folderName;
    }

    /**
     * @param folderName
     * @return the modified WorkspaceQueue
     */
    public WorkspaceQueue setFolderName(String folderName) {
        this.folderName = folderName;
        return this;
    }
}
