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
package fr.gouv.vitam.common.database.collections;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.OntologyModel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DynamicParserTokens {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DynamicParserTokens.class);

    private final VitamDescriptionResolver vitamDescriptionResolver;
    private final Map<String, OntologyModel> ontologyModelsByName;

    public DynamicParserTokens(VitamDescriptionResolver vitamDescriptionResolver, List<OntologyModel> ontologyModels) {
        this.vitamDescriptionResolver = vitamDescriptionResolver;
        this.ontologyModelsByName =
            ontologyModels.stream().collect(Collectors.toMap(OntologyModel::getIdentifier, o -> o));
    }

    public boolean isNotAnalyzed(String name) {
        VitamDescriptionType description = vitamDescriptionResolver.resolve(name);

        if (description != null) {
            return !description.getType().equals(VitamDescriptionType.VitamType.text);
        }

        OntologyModel ontologyModel = ontologyModelsByName.get(name);
        if (ontologyModel != null) {
            return !ontologyModel.getType().isAnalyzed();
        }

        LOGGER.info(String.format("By default unknown fields are analyzed so here '%s' is ANALYZED.", name));
        return false;
    }

    public boolean isAnArray(String name) {
        VitamDescriptionType description = vitamDescriptionResolver.resolve(name);
        if (description == null) {
            LOGGER.info(String.format("By default unknown fields are array so here '%s' is an ARRAY.", name));
            return true;
        }
        return description.getCardinality().equals(VitamDescriptionType.VitamCardinality.many);
    }
}
