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
package fr.gouv.vitam.functional.administration.client.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object Model of Context
 */
public class ContextModel {
    /**
     * unique identifier
     */
    @JsonProperty("_id")
    private String id;

    @JsonProperty("Name")
    private String name;
    
    @JsonProperty("Status")
    private boolean status;
    
    @JsonProperty("Identifier")
    private String identifier;

    @JsonProperty("Permissions")
    private List<PermissionModel> permissions = new ArrayList<>();
    
    /**
     * Constructor of ContextModel
     * 
     * @param id
     * @param name
     * @param status
     * @param permissions
     */
    public ContextModel(@JsonProperty("_id")String id,@JsonProperty("Name") String name,@JsonProperty("Status") boolean status,@JsonProperty("Permissions") List<PermissionModel> permissions) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.permissions = permissions;
    }
    
    /**
     * empty constructor
     */
    public ContextModel() {
    }
    
    /**
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     * @return ContextModel
     */
    public ContextModel setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return status
     */
    public boolean isStatus() {
        return status;
    }

    /**
     * @param status
     */
    public void setStatus(boolean status) {
        this.status = status;
    }

    /**
     * @return list of PermissionModel
     */
    public List<PermissionModel> getPermissions() {
        return permissions;
    }

    /**
     * @param permissions
     */
    public void setPermissions(List<PermissionModel> permissions) {
        this.permissions = permissions;
    }
    
    /**
     * @return identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier
     * @return ContextModel
     */
    public ContextModel setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }
}
