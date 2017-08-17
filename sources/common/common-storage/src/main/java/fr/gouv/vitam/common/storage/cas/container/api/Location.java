package fr.gouv.vitam.common.storage.cas.container.api;

import java.util.Map;
import java.util.Set;

/**
 * Description of where a resource is running. Note this can be physical or virtual.
 */
public interface Location {

    /**
     * Scope of the location, ex. region, zone, host
     *
     */
    LocationScope getScope();

    /**
     * Unique ID provided by the provider (us-standard, miami, etc)
     *
     */
    String getId();

    /**
     * Description of the location
     */
    String getDescription();

    /**
     * The parent, or null, if top-level
     */
    Location getParent();

    /**
     * @return immutable set of metadata relating to this location
     */
    Map<String, Object> getMetadata();

    /**
     * @return if known, the IS0 3166 or 3166-2 divisions where this service may run. ex. a set of
     *         strings like "US" or "US-CA"; otherwise returns an empty list.
     * @see <a
     *      href="http://www.iso.org/iso/country_codes/background_on_iso_3166/what_is_iso_3166.htm">3166</a>
     */
    Set<String> getIso3166Codes();
}
