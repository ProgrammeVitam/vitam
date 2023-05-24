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
package fr.gouv.vitam.metadata.core.database.collections;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import org.bson.Document;

public class MetadataSnapshot extends VitamDocument<MetadataSnapshot> {

    /**
     * the parameter name
     */
    public static final String NAME = "Name";
    /**
     * the parameter value
     */
    public static final String VALUE = "Value";


    /**
     * Parameters
     */
    public enum PARAMETERS {
        UnitsScrollNumber,
        UnitsScrollDate,
        ObjectsScrollNumber,
        ObjectsScrollDate,
    }

    @Override
    public MetadataSnapshot newInstance(JsonNode content) {
        return new MetadataSnapshot(content);
    }

    public MetadataSnapshot() {
        super();
    }

    public MetadataSnapshot(String content) {
        super(content);
    }

    public MetadataSnapshot(JsonNode content) {
        super(content);
    }

    public MetadataSnapshot(Document content) {
        super(content);
    }

    /**
     * @param id the id of parameter value
     * @return MetadataSnapshot
     */
    public MetadataSnapshot setId(String id) {
        append(VitamDocument.ID, id);
        return this;
    }

    /**
     * Name of the parameter
     *
     * @return name of the parameter
     */
    public String getName() {
        return getString(NAME);
    }

    /**
     * Set or change the parameter name
     *
     * @param name to set
     * @return this
     */
    public MetadataSnapshot setName(String name) {
        append(NAME, name);
        return this;
    }

    /**
     * Get the contract parameter value
     *
     * @return this
     */
    public <T> T getValue(Class<T> clasz) {
        return get(VALUE, clasz);
    }

    /**
     * Set or change the parameter value
     *
     * @param value to set
     * @return this
     */
    public <T> MetadataSnapshot setValue(T value) {
        append(VALUE, value);
        return this;
    }

}
