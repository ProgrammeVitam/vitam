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
package fr.gouv.vitam.storage.engine.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 *
 */
@JsonInclude(NON_NULL)
public class TapeLocation {

    public static final String INDEX = "index";
    public static final String TYPE = "locationType";

    @JsonProperty(INDEX)
    private Integer index;

    @JsonProperty(TYPE)
    private TapeLocationType locationType;

    @JsonCreator
    public TapeLocation(@JsonProperty(INDEX) Integer index, @JsonProperty(TYPE) TapeLocationType locationType) {
        this.index = index;
        this.locationType = locationType;
    }

    public boolean equals(TapeLocation tapeLocation) {
        if (!this.index.equals(tapeLocation.getIndex())) {
            return false;
        }

        if (!this.locationType.getType().equals(tapeLocation.getLocationType().getType())) {
            return false;
        }

        return true;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public TapeLocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(TapeLocationType locationType) {
        this.locationType = locationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIndex(), getLocationType());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TapeLocation) {
            TapeLocation tapeLocation = (TapeLocation) obj;
            return (
                Objects.equals(tapeLocation.getIndex(), getIndex()) &&
                Objects.equals(tapeLocation.getLocationType(), getLocationType())
            );
        }
        return false;
    }
}
