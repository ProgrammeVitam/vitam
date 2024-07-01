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
package fr.gouv.vitam.common.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Difference<T> {

    private static final String empty = "EMPTY";

    @JsonProperty("Name")
    public final String name;

    @JsonProperty("Changes")
    public final List<DiffNode<T>> changes;

    public Difference(String name, List<DiffNode<T>> changes) {
        this.name = name;
        this.changes = changes;
    }

    public Difference(String name) {
        this.name = name;
        this.changes = new ArrayList<>();
    }

    public static Difference empty() {
        return new Difference<>(empty, Collections.emptyList());
    }

    public String getName() {
        return name;
    }

    public boolean hasNoDifference() {
        return changes.stream().allMatch(DiffNode::isEmpty);
    }

    public boolean hasDifference() {
        return !hasNoDifference();
    }

    public boolean add(String key, T oldValue, T newValue) {
        return changes.add(new DiffNode<>(key, oldValue, newValue));
    }

    public boolean addAll(Collection<DiffNode<T>> diffNodes) {
        return changes.addAll(diffNodes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Difference<?> that = (Difference<?>) o;
        return Objects.equals(name, that.name) && Objects.equals(changes, that.changes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, changes);
    }

    public static class DiffNode<T> {

        public final String metadataName;
        public final T oldMetadataValue;
        public final T newMetadataValue;

        public DiffNode(String metadataName, T oldMetadataValue, T newMetadataValue) {
            this.metadataName = metadataName;
            this.oldMetadataValue = oldMetadataValue;
            this.newMetadataValue = newMetadataValue;
        }

        public static DiffNode empty() {
            return new DiffNode<>(null, null, null);
        }

        @JsonIgnore
        public boolean isEmpty() {
            return metadataName == null && oldMetadataValue == null && newMetadataValue == null;
        }

        @JsonIgnore
        public boolean isNotEmpty() {
            return !isEmpty();
        }

        public String getMetadataName() {
            return metadataName;
        }

        public T getOldMetadataValue() {
            return oldMetadataValue;
        }

        public T getNewMetadataValue() {
            return newMetadataValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DiffNode<?> diffNode = (DiffNode<?>) o;
            return (
                Objects.equals(metadataName, diffNode.metadataName) &&
                Objects.equals(oldMetadataValue, diffNode.oldMetadataValue) &&
                Objects.equals(newMetadataValue, diffNode.newMetadataValue)
            );
        }

        @Override
        public int hashCode() {
            return Objects.hash(metadataName, oldMetadataValue, newMetadataValue);
        }
    }
}
