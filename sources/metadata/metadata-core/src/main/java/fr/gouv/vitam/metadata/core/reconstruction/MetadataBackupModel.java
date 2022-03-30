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
package fr.gouv.vitam.metadata.core.reconstruction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import org.bson.Document;

import java.util.List;
import java.util.Map;

/**
 * Description of metadata collection Backup model. <br/>
 */
public class MetadataBackupModel {

    /**
     * Metadatas.
     */
    @JsonProperty("metadata")
    private Document metadatas;

    /**
     * Lifecycle.
     */
    @JsonProperty("lfc")
    private Document lifecycle;

    /**
     * Offset.
     */
    @JsonProperty("offset")
    private Long offset;

    public Document getMetadatas() {
        return metadatas;
    }


    public Document getMetadatasWithApproximativesDates() {

        List<JsonNode> events = (List<JsonNode>) lifecycle.get("events");


        if (events != null && !events.isEmpty()) {

            Map<?, ?> test = (Map<?, ?>) events.get(events.size() - 1);
            var approximateDateTime = test.get("evDateTime");

            this.metadatas.put(Unit.APPROXIMATE_CREATION_DATE, lifecycle.get("evDateTime"));
            this.metadatas.put(Unit.APPROXIMATE_UPDATE_DATE, approximateDateTime);
        }


        return metadatas;
    }

    @JsonProperty("unit")
    public void setUnit(Document unit) {
        this.metadatas = unit;
    }

    @JsonProperty("got")
    public void setGot(Document got) {
        this.metadatas = got;
    }

    public Document getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(Document lifecycle) {
        this.lifecycle = lifecycle;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

}
