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
package fr.gouv.vitam.common.storage.cas.container.jcloud;

import fr.gouv.vitam.common.storage.cas.container.api.Location;
import fr.gouv.vitam.common.storage.cas.container.api.LocationScope;

import java.util.Map;
import java.util.Set;

/**
 * This class wrap jcloud location to vitam location
 */
public final class VitamJcloudLocationImpl implements Location {


    private LocationScope scope;
    private String id;
    private String description;
    private Location parent;
    private Map<String, Object> metadata;
    private Set<String> iso3166Codes;

    /**
     * Wrap jcloud Location to vitam location
     * @param location
     * @return
     */
    public static Location wrap(org.jclouds.domain.Location location) {

        if (null == location) {
            return null;
        }
        VitamJcloudLocationImpl vitamJcloudLocation = new VitamJcloudLocationImpl();
        vitamJcloudLocation.scope =
            location.getScope() != null ? LocationScope.valueOf(location.getScope().name()) : null;
        vitamJcloudLocation.id = location.getId();
        vitamJcloudLocation.description = location.getDescription();
        vitamJcloudLocation.parent = wrap(location.getParent());
        vitamJcloudLocation.metadata = location.getMetadata();
        vitamJcloudLocation.iso3166Codes = location.getIso3166Codes();
        return vitamJcloudLocation;
    }

    @Override
    public LocationScope getScope() {
        return scope;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Location getParent() {
        return parent;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public Set<String> getIso3166Codes() {
        return iso3166Codes;
    }
}
