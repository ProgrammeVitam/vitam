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
package fr.gouv.vitam.metadata.core.graph.api;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.model.GraphComputeResponse;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;

import java.util.Collection;

/**
 * This class get units where calculated data are modified
 * Zip generated files and store the zipped file in the offer.
 */
public interface GraphComputeService {
    /**
     * If workflow of compute graph in progress, do not execute this method
     * Should be exposed in the API
     *
     * @return the map of collection:number of treated documents
     */
    GraphComputeResponse computeGraph(JsonNode queryDSL) throws MetaDataException;

    /**
     * Compute graph for unit/got from all parents
     *
     * @param metadataCollections the collection concerned by the build of the graph
     * @param unitsId the collection of units subject of computing graph
     * @param computeObjectGroupGraph true mean compute graph
     * @param invalidateComputedInheritedRules
     * @return The collection of object group treated or to be treated bu an other process.
     * This collection contains got's id of concerning units.
     * Empty collection is returned if computeGraph of object group.
     */
    GraphComputeResponse computeGraph(
        MetadataCollections metadataCollections,
        Collection<String> unitsId,
        boolean computeObjectGroupGraph,
        boolean invalidateComputedInheritedRules
    );

    boolean isInProgress();
}
