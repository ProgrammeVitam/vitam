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
package fr.gouv.vitam.functional.administration.common;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.model.administration.ProfileStatus;
import org.bson.Document;

/**
 * Defines an archive unit Profile collection. </BR>
 */
public class ArchiveUnitProfile extends VitamDocument<ArchiveUnitProfile> {

    /**
     *
     */
    private static final long serialVersionUID = 6725199861835099339L;

    /**
     * the profile id
     */
    public static final String IDENTIFIER = "Identifier";

    /**
     * the profile name
     */
    public static final String NAME = "Name";
    /**
     * the profile description
     */
    public static final String DESCRIPTION = "Description";
    /**
     * the profile status
     */
    public static final String STATUS = "Status";

    /**
     * the creatation date of the profile
     */
    public static final String CREATIONDATE = "CreationDate";
    /**
     * the last update of profile
     */
    public static final String LAST_UPDATE = "LastUpdate";
    /**
     * the activation date of the profile
     */
    public static final String ACTIVATIONDATE = "ActivationDate";
    /**
     * the desactication date of the profile
     */
    public static final String DEACTIVATIONDATE = "DeactivationDate";
    /**
     * the controlSchema of the profile
     */
    public static final String CONTROLSCHEMA = "ControlSchema";

    /**
     * Empty Constructor
     */
    public ArchiveUnitProfile() {}

    /**
     * Constructor
     *
     * @param document data in format Document to create contact
     */
    public ArchiveUnitProfile(Document document) {
        super(document);
    }

    /**
     * @param content in format JsonNode to create profile
     */
    public ArchiveUnitProfile(JsonNode content) {
        super(content);
    }

    /**
     * @param content in format String to create profile
     */
    public ArchiveUnitProfile(String content) {
        super(content);
    }

    /**
     * @param tenantId the working tenant
     */
    public ArchiveUnitProfile(Integer tenantId) {
        append(TENANT_ID, tenantId);
    }

    @Override
    public VitamDocument<ArchiveUnitProfile> newInstance(JsonNode content) {
        return new ArchiveUnitProfile(content);
    }

    /**
     * @param id the id of ingest profile
     * @return AccessionRegisterDetail
     */
    public ArchiveUnitProfile setId(String id) {
        append(VitamDocument.ID, id);
        return this;
    }

    /**
     * The profile id
     *
     * @return the id of the profile
     */
    public String getIdentifier() {
        return getString(IDENTIFIER);
    }

    /**
     * Set or change the profile id, the identifier must be unique by tenant
     *
     * @param identifier
     * @return this
     */
    public ArchiveUnitProfile setIdentifier(String identifier) {
        append(IDENTIFIER, identifier);
        return this;
    }

    /**
     * Name of the profile
     *
     * @return name of profile
     */
    public String getName() {
        return getString(NAME);
    }

    /**
     * Set or change the profile name
     *
     * @param name to set
     * @return this
     */
    public ArchiveUnitProfile setName(String name) {
        append(NAME, name);
        return this;
    }

    /**
     * Get the profile description
     *
     * @return this
     */
    public String getDescription() {
        return getString(DESCRIPTION);
    }

    /**
     * Set or change the profile description
     *
     * @param description to set to contact
     * @return this
     */
    public ArchiveUnitProfile setDescription(String description) {
        append(DESCRIPTION, description);
        return this;
    }

    /**
     * Get the profile status If toJson called without MongoClient configuration this will thow codec exception
     *
     * @return status of ingest contact
     */
    public ProfileStatus getStatus() {
        return (ProfileStatus) get(STATUS);
    }

    /**
     * Set or change the profile status
     *
     * @param status to set
     * @return this
     */
    public ArchiveUnitProfile setStatus(ProfileStatus status) {
        append(STATUS, status);
        return this;
    }

    /**
     * @return creation date of profile
     */
    public String getCreationdate() {
        return getString(CREATIONDATE);
    }

    /**
     * @param creationdate to set
     * @return this
     */
    public ArchiveUnitProfile setCreationdate(String creationdate) {
        append(CREATIONDATE, creationdate);
        return this;
    }

    /**
     * @return last update of profile
     */
    public String getLastupdate() {
        return getString(LAST_UPDATE);
    }

    /**
     * @param lastupdate to set
     * @return this
     */
    public ArchiveUnitProfile setLastupdate(String lastupdate) {
        append(LAST_UPDATE, lastupdate);
        return this;
    }

    /**
     * @return activation date of profile
     */
    public String getActivationdate() {
        return getString(ACTIVATIONDATE);
    }

    /**
     * @param activationdate to set
     * @return this
     */
    public ArchiveUnitProfile setActivationdate(String activationdate) {
        append(ACTIVATIONDATE, activationdate);
        return this;
    }

    /**
     * @return desactivation date of profile
     */
    public String getDeactivationdate() {
        return getString(DEACTIVATIONDATE);
    }

    /**
     * @param deactivationdate to set
     * @return this
     */
    public ArchiveUnitProfile setDeactivationdate(String deactivationdate) {
        append(DEACTIVATIONDATE, deactivationdate);
        return this;
    }

    /**
     * @return the ControlSchema of the profile
     */
    public String getControlSchema() {
        return getString(CONTROLSCHEMA);
    }

    /**
     * @param ControlSchema to set
     * @return this
     */
    public ArchiveUnitProfile setControlSchema(String ControlSchema) {
        append(CONTROLSCHEMA, ControlSchema);
        return this;
    }
}
