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
package fr.gouv.vitam.common.storage.cas.container.api;

import java.util.Map;
import java.util.Set;

/**
 * Description of where a resource is running. Note this can be physical or virtual.
 */
public interface Location {
    /**
     * Scope of the location, ex. region, zone, host
     */
    LocationScope getScope();

    /**
     * Unique ID provided by the provider (us-standard, miami, etc)
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
     * strings like "US" or "US-CA"; otherwise returns an empty list.
     * @see <a
     * href="http://www.iso.org/iso/country_codes/background_on_iso_3166/what_is_iso_3166.htm">3166</a>
     */
    Set<String> getIso3166Codes();
}
