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

package fr.gouv.vitam.storage.engine.common.model.request;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;

/**
 * Simple object holding data used to retrieve an Object from the workspace
 */
public class ObjectDescription {
    private String workspaceContainerGUID;
    private String workspaceObjectURI;
    private String objectName;
    private StorageCollectionType type;

    /**
     * Empty Constructor.
     */
    public ObjectDescription() {
        // Empty
    }

    /**
     * Constructor.
     *
     * @param workspaceContainerGUID workspace container id
     * @param workspaceObjectURI workspace object URI
     */
    public ObjectDescription(String workspaceContainerGUID, String workspaceObjectURI) {
        this.workspaceContainerGUID = workspaceContainerGUID;
        this.workspaceObjectURI = workspaceObjectURI;
    }

    /**
     * Constructor with StorageCollectionType parameter
     * 
     * @param type
     * @param workspaceContainerGUID
     * @param objectName
     */
    public ObjectDescription(StorageCollectionType type, String workspaceContainerGUID, String objectName) {
        this.type = type;
        this.workspaceContainerGUID = workspaceContainerGUID;
        this.objectName = objectName;
    }

    /**
     * @return workspaceObjectURI
     */
    public String getWorkspaceObjectURI() {
        if (StringUtils.isNotBlank(workspaceObjectURI) || this.type == null) {
            return workspaceObjectURI;
        }
        return this.type.getCollectionName() + File.separator + objectName;
    }

    /**
     * @param workspaceObjectURI
     * @return this
     */
    public ObjectDescription setWorkspaceObjectURI(String workspaceObjectURI) {
        this.workspaceObjectURI = workspaceObjectURI;
        return this;
    }

    /**
     * @return workspaceContainerGUID
     */
    public String getWorkspaceContainerGUID() {
        return workspaceContainerGUID;
    }

    /**
     * @param workspaceContainerGUID
     * @return this
     */
    public ObjectDescription setWorkspaceContainerGUID(String workspaceContainerGUID) {
        this.workspaceContainerGUID = workspaceContainerGUID;
        return this;
    }

    /**
     * @return the objectName
     * 
     */
    public String getObjectName() {
        return objectName;
    }

    /**
     * @param objectName the objectName to set
     *
     * @return this
     */
    public ObjectDescription setObjectName(String objectName) {
        this.objectName = objectName;
        return this;
    }

    /**
     * @return the type
     */
    public StorageCollectionType getType() {
        return type;
    }

    /**
     * @param type the type to set
     *
     * @return this
     */
    public ObjectDescription setType(StorageCollectionType type) {
        this.type = type;
        return this;
    }

}
