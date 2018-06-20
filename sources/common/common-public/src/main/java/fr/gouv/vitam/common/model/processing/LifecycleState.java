package fr.gouv.vitam.common.model.processing;

public enum LifecycleState {
    ENABLED, DISABLED;

    public boolean isEnabled() {
        return this == ENABLED;
    }

}
