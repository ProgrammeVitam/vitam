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
import org.bson.Document;

/**
 * Defines a Contract Sequence collection. </BR>
 */
public class VitamSequence extends VitamDocument<VitamSequence> {

    /**
     * the contract sequence name
     */
    public static final String NAME = "Name";
    /**
     * the contract sequence description
     */
    public static final String COUNTER = "Counter";

    /**
     * Empty Constructor
     */
    public VitamSequence() {
        //nothing To do
    }

    /**
     * Constructor
     *
     * @param document data in format Document to create contact
     */
    public VitamSequence(Document document) {
        super(document);
    }

    /**
     * @param content in format JsonNode to create contract sequence
     */
    public VitamSequence(JsonNode content) {
        super(content);
    }

    /**
     * @param content in format String to create contract sequence
     */
    public VitamSequence(String content, Integer tenant) {
        super(content);
        append(TENANT_ID, tenant);
    }

    @Override
    public VitamDocument<VitamSequence> newInstance(JsonNode content) {
        return new VitamSequence(content);
    }

    /**
     * @param tenantId the working tenant
     */
    public VitamSequence(Integer tenantId) {
        append(TENANT_ID, tenantId);
    }

    /**
     * @param id the id of ingest contract sequence
     * @return AccessionRegisterDetail
     */
    public VitamSequence setId(String id) {
        append(VitamDocument.ID, id);
        return this;
    }

    /**
     * Name of the contract sequence
     *
     * @return name of contract sequence
     */
    public String getName() {
        return getString(NAME);
    }

    /**
     * Set or change the contract sequence name
     *
     * @param name to set
     * @return this
     */
    public VitamSequence setName(String name) {
        append(NAME, name);
        return this;
    }

    /**
     * Get the contract sequence counter
     *
     * @return this
     */
    public Integer getCounter() {
        return getInteger(COUNTER);
    }

    /**
     * Set or change the contract sequence counter
     *
     * @param counter to set
     * @return this
     */
    public VitamSequence setCounter(String counter) {
        append(COUNTER, counter);
        return this;
    }
}
