package fr.gouv.vitam.common.storage.cas.container.api;

/**
 * The scope of the location
 *
 */
public enum LocationScope {

    PROVIDER,
    REGION,
    ZONE,
    /**
     * @see <a href="http://dmtf.org/standards/cloud">cimi system docs</a>
     */
    SYSTEM,
    /**
     * E.g. the DMZ segment, secure segment.
     */
    NETWORK,
    HOST;

}
