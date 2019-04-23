package fr.gouv.vitam.common.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Difference<T> {
    private final static String empty = "EMPTY";

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
        return changes.stream()
            .allMatch(DiffNode::isEmpty);
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
        return Objects.equals(name, that.name)
            && Objects.equals(changes, that.changes);
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
            return Objects.equals(metadataName, diffNode.metadataName)
                && Objects.equals(oldMetadataValue, diffNode.oldMetadataValue)
                && Objects.equals(newMetadataValue, diffNode.newMetadataValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(metadataName, oldMetadataValue, newMetadataValue);
        }
    }
}
