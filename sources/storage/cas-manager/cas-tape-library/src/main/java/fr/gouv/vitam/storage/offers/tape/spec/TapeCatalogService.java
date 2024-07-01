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
package fr.gouv.vitam.storage.offers.tape.spec;

import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibrarySpec;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TapeCatalogService {
    void create(TapeCatalog tapeCatalog) throws TapeCatalogException;

    boolean replace(TapeCatalog tapeCatalog) throws TapeCatalogException;

    boolean update(String tapeId, Map<String, Object> criteria) throws TapeCatalogException;

    /**
     * Return map of drive index with his current tape
     * If all drive are empty then return an empty map
     *
     * @param tapeLibraryIdentifier
     * @param libraryState
     * @return
     * @throws TapeCatalogException
     */
    Map<Integer, TapeCatalog> init(String tapeLibraryIdentifier, TapeLibrarySpec libraryState)
        throws TapeCatalogException;

    TapeCatalog findById(String tapeId) throws TapeCatalogException;

    List<TapeCatalog> find(List<QueryCriteria> criteria) throws TapeCatalogException;

    void markReady(String queueId) throws QueueException;

    Optional<TapeCatalog> receive(Bson inQuery) throws QueueException;
}
