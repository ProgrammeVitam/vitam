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
import fr.gouv.vitam.common.model.administration.OntologyOrigin;
import fr.gouv.vitam.common.model.administration.OntologyType;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines an ontology collection. </BR>
 */
public class Ontology extends VitamDocument<Ontology> {

    /**
     * the serial version uid
     */
    private static final long serialVersionUID = -5804983955259018611L;
    /**
     * the ontology identifier
     */
    public static final String IDENTIFIER = "Identifier";

    /**
     * the ontology seda field
     */
    public static final String SEDAFIELD = "SedaField";

    /**
     * the ontology api field
     */
    public static final String APIFIELD = "ApiField";

    /**
     * the ontology description
     */
    public static final String DESCRIPTION = "Description";
    /**
     * the ontology type
     */
    public static final String TYPE = "Type";

    /**
     * the ontology origin
     */
    public static final String ORIGIN = "Origin";

    /**
     * ShortName Tag
     */
    public static final String SHORT_NAME = "ShortName";

    /**
     * Collections Tag
     */
    public static final String COLLECTIONS = "Collections";
    /**
     * the creation date of the ontology
     */
    public static final String CREATIONDATE = "CreationDate";
    /**
     * the last update of ontology
     */
    public static final String LAST_UPDATE = "LastUpdate";

    /**
     * Empty Constructor
     */
    public Ontology() {}

    /**
     * Constructor
     *
     * @param document data in format Document to create ontology
     */
    public Ontology(Document document) {
        super(document);
    }

    /**
     * @param content in format JsonNode to create ontology
     */
    public Ontology(JsonNode content) {
        super(content);
    }

    /**
     * @param content in format String to create ontology
     */
    public Ontology(String content) {
        super(content);
    }

    /**
     * @param tenantId the working tenant
     */
    public Ontology(Integer tenantId) {
        append(TENANT_ID, tenantId);
    }

    @Override
    public VitamDocument<Ontology> newInstance(JsonNode content) {
        return new Ontology(content);
    }

    /**
     * @param id the id of ontology
     * @return
     */
    public Ontology setId(String id) {
        append(VitamDocument.ID, id);
        return this;
    }

    /**
     * The ontology id
     *
     * @return the id of the ontology
     */
    public String getIdentifier() {
        return getString(IDENTIFIER);
    }

    /**
     * Set or change the ontology id, the identifier must be unique by tenant
     *
     * @param identifier
     * @return this
     */
    public Ontology setIdentifier(String identifier) {
        append(IDENTIFIER, identifier);
        return this;
    }

    /**
     * The ontology seda field
     *
     * @return the seda field of the ontology
     */
    public String getSedaField() {
        return getString(SEDAFIELD);
    }

    /**
     * Set or change the ontology seda field
     *
     * @param sedaField
     * @return this
     */
    public Ontology setSedaField(String sedaField) {
        append(SEDAFIELD, sedaField);
        return this;
    }

    /**
     * The ontology api field
     *
     * @return the api field of the ontology
     */
    public String getApiField() {
        return getString(APIFIELD);
    }

    /**
     * Set or change the ontology api field
     *
     * @param apiField
     * @return this
     */
    public Ontology setApiField(String apiField) {
        append(APIFIELD, apiField);
        return this;
    }

    /**
     * Get the ontology description
     *
     * @return this
     */
    public String getDescription() {
        return getString(DESCRIPTION);
    }

    /**
     * Set or change the profile ontology
     *
     * @param description to set to contact
     * @return this
     */
    public Ontology setDescription(String description) {
        append(DESCRIPTION, description);
        return this;
    }

    /**
     * Get the ontology type
     *
     * @return status of the ontology
     */
    public OntologyType getType() {
        return (OntologyType) get(TYPE);
    }

    /**
     * Set or change the ontology type
     *
     * @param type to set
     * @return this
     */
    public Ontology setType(OntologyType type) {
        append(TYPE, type);
        return this;
    }

    /**
     * Get the ontology origin
     *
     * @return origin of the ontology
     */
    public OntologyOrigin getOrigin() {
        return (OntologyOrigin) get(ORIGIN);
    }

    /**
     * Set or change the ontology origin
     *
     * @param origin to set
     * @return this
     */
    public Ontology setOrigin(OntologyOrigin origin) {
        append(ORIGIN, origin);
        return this;
    }

    /**
     * Get the ontology ShortName
     *
     * @return shortName of the ontology
     */
    public String getShortName() {
        return getString(SHORT_NAME);
    }

    /**
     * Set or change the ontology shortName
     *
     * @param shortName to set
     * @return this
     */
    public Ontology setShortName(String shortName) {
        append(SHORT_NAME, shortName);
        return this;
    }

    public List<String> getCollections() {
        return (List<String>) get(COLLECTIONS);
    }

    /**
     * Set or change the ontology collections
     *
     * @param collections to set
     * @return this
     */
    public Ontology setCollections(List<String> collections) {
        if (!collections.isEmpty()) {
            final List<String> col = new ArrayList<>();
            col.addAll(collections);
            append(COLLECTIONS, col);
        }
        return this;
    }

    /**
     * @return creation date of ontology
     */
    public String getCreationdate() {
        return getString(CREATIONDATE);
    }

    /**
     * @param creationdate to set
     * @return this
     */
    public Ontology setCreationdate(String creationdate) {
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
    public Ontology setLastupdate(String lastupdate) {
        append(LAST_UPDATE, lastupdate);
        return this;
    }
}
