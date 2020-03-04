/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.storage.offers.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class OfferLogCompactionRequest {
    private final long expirationValue;
    private final ChronoUnit expirationUnit;
    private final int compactionSize;

    @JsonCreator
    public OfferLogCompactionRequest(
        @JsonProperty("expirationValue") long expirationValue,
        @JsonProperty("expirationUnit") ChronoUnit expirationUnit,
        @JsonProperty("compactionSize") int compactionSize) {
        this.expirationValue = expirationValue;
        this.expirationUnit = expirationUnit;
        this.compactionSize = compactionSize;
    }

    @JsonIgnore
    public boolean isNotValid() {
        return expirationValue <= 0 || compactionSize < 1000 || compactionSize > 200_000 || Objects.isNull(expirationUnit);
    }

    public long getExpirationValue() {
        return expirationValue;
    }

    public ChronoUnit getExpirationUnit() {
        return expirationUnit;
    }

    public int getCompactionSize() {
        return compactionSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OfferLogCompactionRequest that = (OfferLogCompactionRequest) o;
        return expirationValue == that.expirationValue &&
            compactionSize == that.compactionSize &&
            expirationUnit == that.expirationUnit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(expirationValue, expirationUnit, compactionSize);
    }

    @Override
    public String toString() {
        return "OfferLogCompactionRequest{" +
            "expirationValue=" + expirationValue +
            ", expirationUnit=" + expirationUnit +
            ", compactionSize=" + compactionSize +
            '}';
    }
}
