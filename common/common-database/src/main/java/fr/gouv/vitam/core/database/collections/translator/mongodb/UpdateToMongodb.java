/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.core.database.collections.translator.mongodb;

import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Updates;

import fr.gouv.vitam.builder.request.construct.action.Action;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.parser.request.parser.UpdateParser;

/**
 * Update to MongoDb
 *
 */
public class UpdateToMongodb extends RequestToMongodb {

    /**
     * @param updateParser
     */
    public UpdateToMongodb(UpdateParser updateParser) {
        super(updateParser);
    }

    /**
     * gives the update part of updateOne(query, update)
     *
     * @return the orderBy MongoDB command
     * @throws InvalidParseOperationException
     */
    public Bson getFinalUpdate() throws InvalidParseOperationException {
        final List<Action> actions = ((UpdateParser) requestParser).getRequest().getActions();
        final List<Bson> bactions = new ArrayList<Bson>(actions.size());
        for (final Action action : actions) {
            bactions.add(UpdateActionToMongodb.getCommand(action));
        }
        return Updates.combine(bactions);
    }
}
