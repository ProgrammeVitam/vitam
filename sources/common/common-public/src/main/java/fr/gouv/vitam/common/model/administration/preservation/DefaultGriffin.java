/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.administration.preservation;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;

/**
 * DefaultGriffin class
 */
public class DefaultGriffin {

    @NotEmpty(message = ConstraintConstants.NOT_EMPTY_ERROR_MSG)
    @JsonProperty("GriffinIdentifier")
    private String griffinIdentifier;

    @Positive(message = ConstraintConstants.POSITIVE_ERROR_MSG)
    @JsonProperty("Timeout")
    private int timeOut;

    @Positive(message = ConstraintConstants.POSITIVE_ERROR_MSG)
    @JsonProperty("MaxSize")
    private long maxSize;

    @NotNull(message = ConstraintConstants.NOT_NULL_ERROR_MSG)
    @JsonProperty("Debug")
    private Boolean debug;

    @NotEmpty(message = ConstraintConstants.NOT_EMPTY_ERROR_MSG)
    @Valid
    @JsonProperty("ActionDetail")
    private List<ActionPreservation> actionDetail;

    public DefaultGriffin() {
        //default Constructor
    }

    public DefaultGriffin(@NotEmpty String griffinIdentifier, @NotEmpty List<ActionPreservation> actionDetail) {
        this.griffinIdentifier = griffinIdentifier;
        this.actionDetail = actionDetail;
    }

    public String getGriffinIdentifier() {
        return griffinIdentifier;
    }

    public void setGriffinIdentifier(String griffinIdentifier) {
        this.griffinIdentifier = griffinIdentifier;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public Boolean isDebug() {
        return debug;
    }

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public List<ActionPreservation> getActionDetail() {
        return actionDetail;
    }

    public void setActionDetail(List<ActionPreservation> actionDetail) {
        this.actionDetail = actionDetail;
    }
}
