package fr.gouv.vitam.core.database.collections;

/**
 * Link used in relation between objects as hierarchies in Vitam
 */
// TODO REVIEW should be UPPER CASE
public enum LinkType {
    /**
     * Link N-N
     */
    SymLinkNN,
    /**
     * Link N-
     */
    AsymLinkN,
    /**
     * Link 1-N
     */
    SymLink1N,
    /**
     * Link N-1
     */
    SymLinkN1,
    /**
     * Link 1-1
     */
    SymLink11,
    /**
     * Link 1-
     */
    AsymLink1,
    /**
     * False Link (N)-N
     */
    SymLink_N_N
}
