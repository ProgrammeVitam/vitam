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
package fr.gouv.vitam.common.database.parser.query.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Query from Parser Helper
 */
public class UpdateActionParserHelper extends UpdateActionHelper {

    protected UpdateActionParserHelper() {}

    /**
     * @param data JsonNode
     * @param adapter VarNameAdapter
     * @return an AddAction
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final AddAction add(final JsonNode data, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new AddAction(UPDATEACTION.ADD, data, adapter);
    }

    /**
     * @param data JsonNode
     * @param adapter VarNameAdapter
     * @return an IncAction
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final IncAction inc(final JsonNode data, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new IncAction(UPDATEACTION.INC, data, adapter);
    }

    /**
     * @param data JsonNode
     * @param adapter VarNameAdapter
     * @return an MinAction
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final MinAction min(final JsonNode data, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new MinAction(UPDATEACTION.MIN, data, adapter);
    }

    /**
     * @param data JsonNode
     * @param adapter VarNameAdapter
     * @return an MaxAction
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final MaxAction max(final JsonNode data, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new MaxAction(UPDATEACTION.MAX, data, adapter);
    }

    /**
     * @param data JsonNode
     * @param adapter VarNameAdapter
     * @return a PopAction
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final PopAction pop(final JsonNode data, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new PopAction(UPDATEACTION.POP, data, adapter);
    }

    /**
     * @param data JsonNode
     * @param adapter VarNameAdapter
     * @return a PullAction
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final PullAction pull(final JsonNode data, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new PullAction(UPDATEACTION.PULL, data, adapter);
    }

    /**
     * @param data JsonNode
     * @param adapter VarNameAdapter
     * @return a PushAction
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final PushAction push(final JsonNode data, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new PushAction(UPDATEACTION.PUSH, data, adapter);
    }

    /**
     * @param data JsonNode
     * @param adapter VarNameAdapter
     * @return a RenameAction
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final RenameAction rename(final JsonNode data, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new RenameAction(UPDATEACTION.RENAME, data, adapter);
    }

    /**
     * @param data JsonNode
     * @param adapter VarNameAdapter
     * @return a SetAction
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final SetAction set(final JsonNode data, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new SetAction(UPDATEACTION.SET, data, adapter);
    }

    /**
     * @param array JsonNode
     * @param adapter VarNameAdapter
     * @return a UnsetAction
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    public static final UnsetAction unset(final JsonNode array, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        try {
            return new UnsetAction(UPDATEACTION.UNSET, (ArrayNode) array, adapter);
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse error", e);
        }
    }

    /**
     * @param data
     * @param adapter
     * @return
     * @throws InvalidParseOperationException
     */
    public static final SetregexAction setregex(final JsonNode data, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        return new SetregexAction(UPDATEACTION.SETREGEX, data, adapter);
    }
}
