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
package fr.gouv.vitam.access.internal.core.identifier.query;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.model.identifier.PurgedCollectionType;
import jakarta.annotation.Nullable;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.nestedSearch;
import static fr.gouv.vitam.common.database.utils.AccessContractRestrictionHelper.applyAccessContractRestrictionForObjectGroupForSelect;
import static fr.gouv.vitam.common.database.utils.AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;

public class PersistentIdentifierMultiQueryFactory {

    public static SelectMultiQuery createSelectMultiQuery(
        final PurgedCollectionType purgedCollectionType,
        final String persistentIdentifier,
        @Nullable JsonNode selectQuery
    ) {
        if (purgedCollectionType == PurgedCollectionType.UNIT) {
            return createUnitSelectMultiQuery(persistentIdentifier, selectQuery);
        } else if (purgedCollectionType == PurgedCollectionType.OBJECT) {
            return createObjectSelectMultiQuery(persistentIdentifier, selectQuery);
        }
        throw new IllegalStateException("Purged collection type not supported");
    }

    public static SelectMultiQuery createSelectMultiQuery(
        final PurgedCollectionType purgedCollectionType,
        final String persistentIdentifier
    ) {
        return createSelectMultiQuery(purgedCollectionType, persistentIdentifier, null);
    }

    private static SelectMultiQuery jsonNodeToSelectMultiQuery(final JsonNode jsonNode)
        throws InvalidParseOperationException {
        final SelectParserMultiple selectParserMultiple = new SelectParserMultiple();
        selectParserMultiple.parse(jsonNode);
        return selectParserMultiple.getRequest();
    }

    private static SelectMultiQuery createUnitSelectMultiQuery(
        final String persistentIdentifier,
        @Nullable JsonNode selectQuery
    ) {
        try {
            final SelectParserMultiple query = new SelectParserMultiple();
            if (selectQuery != null) {
                query.parse(selectQuery);
            }
            final SelectMultiQuery multiQuery = (SelectMultiQuery) query
                .getRequest()
                .addQueries(eq("PersistentIdentifier.PersistentIdentifierContent", persistentIdentifier));
            final JsonNode finalMultiQueryWithRestrictions = applyAccessContractRestrictionForUnitForSelect(
                multiQuery.getFinalSelect(),
                getVitamSession().getContract()
            );
            return jsonNodeToSelectMultiQuery(finalMultiQueryWithRestrictions);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private static SelectMultiQuery createObjectSelectMultiQuery(
        final String persistentIdentifier,
        @Nullable JsonNode selectQuery
    ) {
        try {
            final SelectParserMultiple query = new SelectParserMultiple();
            if (selectQuery != null) {
                query.parse(selectQuery);
            }
            final SelectMultiQuery multiQuery = (SelectMultiQuery) query
                .getRequest()
                .addQueries(
                    nestedSearch(
                        "#qualifiers.versions",
                        and()
                            .add(
                                eq(
                                    "#qualifiers.versions.PersistentIdentifier.PersistentIdentifierContent",
                                    persistentIdentifier
                                )
                            )
                            .getCurrentQuery()
                    )
                );
            final JsonNode finalMultiQueryWithRestrictions = applyAccessContractRestrictionForObjectGroupForSelect(
                multiQuery.getFinalSelect(),
                getVitamSession().getContract()
            );
            return jsonNodeToSelectMultiQuery(finalMultiQueryWithRestrictions);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }
}
