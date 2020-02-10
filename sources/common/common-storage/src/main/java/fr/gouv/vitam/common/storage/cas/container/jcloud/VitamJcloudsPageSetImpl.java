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

import fr.gouv.vitam.common.storage.cas.container.api.StorageType;
import fr.gouv.vitam.common.storage.cas.container.api.VitamPageSet;
import fr.gouv.vitam.common.storage.cas.container.api.VitamStorageMetadata;
import fr.gouv.vitam.common.storage.filesystem.v2.metadata.VitamStorageMetadataImpl;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;

import java.util.LinkedHashSet;

/**
 * This class wrap jcloud PageSet to vitam pageSet
 */
public class VitamJcloudsPageSetImpl extends LinkedHashSet<VitamStorageMetadata>
    implements VitamPageSet<VitamStorageMetadata> {
    protected String marker;

    public static VitamPageSet<VitamStorageMetadata> wrap(PageSet<? extends StorageMetadata> pageSet) {
        final VitamJcloudsPageSetImpl jcloudsPageSet = new VitamJcloudsPageSetImpl();
        if (null == pageSet) {
            return null;
        }
        pageSet.forEach(o ->
            jcloudsPageSet.add(new VitamStorageMetadataImpl(
                StorageType.valueOf(o.getType().name()),
                o.getProviderId(),
                o.getName(),
                VitamJcloudLocationImpl.wrap(o.getLocation()),
                o.getUri(), o.getUserMetadata(), o.getETag(),
                o.getCreationDate(), o.getLastModified(),
                o.getSize()))
        );
        jcloudsPageSet.marker = pageSet.getNextMarker();

        return jcloudsPageSet;
    }

    public String getNextMarker() {
        return this.marker;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (this.marker == null ? 0 : this.marker.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!super.equals(obj)) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            VitamPageSet<?> other = (VitamPageSet) obj;
            if (this.marker == null) {
                if (other.getNextMarker() != null) {
                    return false;
                }
            } else if (!this.marker.equals(other.getNextMarker())) {
                return false;
            }

            return true;
        }
    }

}
