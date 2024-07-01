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
package fr.gouv.vitam.worker.core.plugin.preservation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.administration.preservation.ActionPreservation;

import java.util.List;

public class ParametersPreservation {

    @JsonProperty("RequestId")
    private String requestId;

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Actions")
    private List<ActionPreservation> actions;

    @JsonProperty("Inputs")
    private List<InputPreservation> inputs;

    @JsonProperty("Debug")
    private boolean debug;

    public ParametersPreservation() {}

    public ParametersPreservation(
        String requestId,
        String batchId,
        List<InputPreservation> input,
        List<ActionPreservation> actions,
        boolean debug
    ) {
        this.requestId = requestId;
        this.id = batchId;
        this.inputs = input;
        this.actions = actions;
        this.debug = debug;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<ActionPreservation> getActions() {
        return actions;
    }

    public void setActions(List<ActionPreservation> actions) {
        this.actions = actions;
    }

    public List<InputPreservation> getInputs() {
        return inputs;
    }

    public void setInputs(List<InputPreservation> inputs) {
        this.inputs = inputs;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public String toString() {
        return (
            "Parameters{" +
            "requestId='" +
            requestId +
            '\'' +
            ", id='" +
            id +
            '\'' +
            ", actions=" +
            actions +
            ", inputs=" +
            inputs +
            ", debug=" +
            debug +
            '}'
        );
    }
}
