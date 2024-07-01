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

import java.net.URI;
import java.util.Date;
import java.util.Map;

/**
 * VitamStorageMetadata interface describing storage metadata
 */
public interface VitamStorageMetadata extends VitamResourceMetadata<StorageType> {
    /**
     * Whether this resource is a container, file, etc.
     */
    @Override
    StorageType getType();

    /**
     * Unique identifier of this resource within its enclosing namespace. In some scenarios, this id
     * is not user assignable. For files, this may be an system generated key, or the full path to
     * the resource. ex. /path/to/file.txt
     */
    @Override
    String getProviderId();

    /**
     * Name of this resource. Names are dictated by the user. For files, this may be the filename,
     * ex. file.txt
     */
    @Override
    String getName();

    /**
     * URI used to access this resource
     */
    @Override
    URI getUri();

    /**
     * Any key-value pairs associated with the resource.
     */
    @Override
    Map<String, String> getUserMetadata();

    /**
     * The eTag value stored in the Etag header returned by HTTP.
     */
    String getETag();

    /**
     * Creation date of the resource, possibly null.
     */
    Date getCreationDate();

    /**
     * Last modification time of the resource
     */
    Date getLastModified();

    /**
     * Size of the resource, possibly null.
     */
    Long getSize();
}
