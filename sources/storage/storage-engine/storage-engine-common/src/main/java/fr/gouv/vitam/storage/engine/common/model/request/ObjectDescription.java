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
package fr.gouv.vitam.storage.engine.common.model.request;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;

import java.io.File;

/**
 * Simple object holding data used to retrieve an Object from the workspace
 */
public class ObjectDescription {

    private String workspaceContainerGUID;
    private String workspaceObjectURI;
    private String objectName;
    private DataCategory type;

    /**
     * Empty Constructor.
     */
    public ObjectDescription() {
        // Empty
    }

    /**
     * Constructor with DataCategory parameter<br/>
     *
     * @param type the collection tyoe
     * @param workspaceContainerGUID the container guid in workspace
     * @param objectName the object name
     * @param workspaceObjectUri the workspace uri of the object
     */
    public ObjectDescription(
        DataCategory type,
        String workspaceContainerGUID,
        String objectName,
        String workspaceObjectUri
    ) {
        this.type = type;
        this.workspaceContainerGUID = workspaceContainerGUID;
        this.objectName = objectName;
        this.workspaceObjectURI = workspaceObjectUri;
    }

    /**
     * @return workspaceObjectURI
     */
    public String getWorkspaceObjectURI() {
        if (ParametersChecker.isNotEmpty(workspaceObjectURI) || this.type == null) {
            return workspaceObjectURI;
        }
        return this.type.getCollectionName() + File.separator + objectName;
    }

    /**
     * @param workspaceObjectURI to set
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
     * @param workspaceContainerGUID to set
     * @return this
     */
    public ObjectDescription setWorkspaceContainerGUID(String workspaceContainerGUID) {
        this.workspaceContainerGUID = workspaceContainerGUID;
        return this;
    }

    /**
     * @return the objectName
     */
    public String getObjectName() {
        return objectName;
    }

    /**
     * @param objectName the objectName to set
     * @return this
     */
    public ObjectDescription setObjectName(String objectName) {
        this.objectName = objectName;
        return this;
    }

    /**
     * @return the type
     */
    public DataCategory getType() {
        return type;
    }

    /**
     * @param type the type to set
     * @return this
     */
    public ObjectDescription setType(DataCategory type) {
        this.type = type;
        return this;
    }
}
