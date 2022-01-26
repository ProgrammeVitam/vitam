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
package fr.gouv.vitam.collect.internal.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.collect.internal.dto.ObjectGroupDto;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.DbQualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.DbVersionsModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS.OBJECTGROUPS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.QUALIFIERS;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;

public class QueryHandler {
    private QueryHandler() throws IllegalAccessException {
        throw new IllegalAccessException("Utility class!");
    }

    public static UpdateMultiQuery getQualifiersAddMultiQuery(String qualifier, int version, List<DbQualifiersModel> qualifiers,
        ObjectGroupDto objectGroupDto, String versionId) throws InvalidParseOperationException, InvalidCreateOperationException {
        DbQualifiersModel newQualifier = new DbQualifiersModelBuilder()
            .withUsage(qualifier)
            .withVersion(versionId, objectGroupDto.getFileInfo().getFileName(), qualifier,
                version)
            .build();

        qualifiers.add(newQualifier);

        Map<String, JsonNode> action = new HashMap<>();
        action.put(QUALIFIERS.exactToken(), toJsonNode(qualifiers));
        SetAction setQualifier = new SetAction(action);

        UpdateMultiQuery query = new UpdateMultiQuery();
        query.addHintFilter(OBJECTGROUPS.exactToken());
        query.addActions(setQualifier);
        return query;
    }

    public static UpdateMultiQuery getQualifiersUpdateMultiQuery(DbQualifiersModel qualifierModelToUpdate,
        DbObjectGroupModel objectGroup, String qualifier, int version, List<DbQualifiersModel> qualifiers,
        ObjectGroupDto objectGroupDto, String versionId)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        int index = qualifiers.indexOf(qualifierModelToUpdate);
        DbVersionsModel dbversion = new DbVersionsModelBuilder()
            .build(versionId, objectGroupDto.getFileInfo().getFileName(), qualifier, version);

        qualifierModelToUpdate.getVersions().add(dbversion);
        qualifierModelToUpdate.setNbc(qualifierModelToUpdate.getNbc() + 1);
        qualifiers.set(index, qualifierModelToUpdate);

        Map<String, JsonNode> action = new HashMap<>();
        action.put(QUALIFIERS.exactToken(), toJsonNode(qualifiers));

        UpdateMultiQuery query = new UpdateMultiQuery();
        query.addActions(
            UpdateActionHelper.set(VitamFieldsHelper.nbobjects(), objectGroup.getNbc() + 1L),
            new SetAction(action)
        );
        return query;
    }
}
