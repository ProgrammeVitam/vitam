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
package fr.gouv.vitam.functional.administration.common;

import java.util.ArrayList;
import java.util.List;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.query.action.IncAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;

/**
 * ReferentialAccessionRegisterSummaryUtil
 */
public class ReferentialAccessionRegisterSummaryUtil {

    /**
     * Init AccessionRegisterSummary
     *
     * @param originatingAgency originating Agency
     * @param id                id
     * @return return AccessionRegisterSummary
     */
    public AccessionRegisterSummary initAccessionRegisterSummary(String originatingAgency, String id) {
        final RegisterValueDetailModel initialValue = new RegisterValueDetailModel();
        final AccessionRegisterSummary accessionRegister = new AccessionRegisterSummary();
        accessionRegister
                .setId(id)
                .setOriginatingAgency(originatingAgency)
                .setTotalObjects(initialValue)
                .setTotalObjectGroups(initialValue)
                .setTotalUnits(initialValue)
                .setObjectSize(initialValue)
                .setCreationDate(LocalDateUtil.now().toString());
        return accessionRegister;
    }

    /**
     * Generate update query on summary from register detail
     *
     * @param registerDetail AccessionRegisterDetail
     * @return update query
     * @throws InvalidCreateOperationException parsing query exception
     */
    public Update generateUpdateQuery(AccessionRegisterDetailModel registerDetail) throws InvalidCreateOperationException {
        List<Action> actions = createActions(registerDetail);
        Update update = new Update();
        update.setQuery(QueryHelper.eq(AccessionRegisterSummary.ORIGINATING_AGENCY, registerDetail
                .getOriginatingAgency()));
        update.addActions(actions.toArray(new IncAction[actions.size()]));
        return update;
    }

    /**
     * Add action for summary from register detail
     *
     * @param registerDetail AccessionRegisterDetail
     * @return query actions
     * @throws InvalidCreateOperationException parsing query exception
     */
    public List<Action> createActions(AccessionRegisterDetailModel registerDetail)
            throws InvalidCreateOperationException {
        ArrayList<Action> actions = new ArrayList<>();

        actions.add(new IncAction(AccessionRegisterSummary.TOTAL_OBJECTGROUPS + "." + AccessionRegisterSummary.INGESTED,
                registerDetail.getTotalObjectsGroups().getIngested()));
        actions.add(new IncAction(AccessionRegisterSummary.TOTAL_OBJECTGROUPS + "." + AccessionRegisterSummary.DELETED,
                registerDetail.getTotalObjectsGroups().getDeleted()));
        actions.add(new IncAction(AccessionRegisterSummary.TOTAL_OBJECTGROUPS + "." + AccessionRegisterSummary.REMAINED,
                registerDetail.getTotalObjectsGroups().getRemained()));

        actions.add(new IncAction(AccessionRegisterSummary.TOTAL_OBJECTS + "." + AccessionRegisterSummary.INGESTED,
                registerDetail.getTotalObjects().getIngested()));
        actions.add(new IncAction(AccessionRegisterSummary.TOTAL_OBJECTS + "." + AccessionRegisterSummary.DELETED,
                registerDetail.getTotalObjects().getDeleted()));
        actions.add(new IncAction(AccessionRegisterSummary.TOTAL_OBJECTS + "." + AccessionRegisterSummary.REMAINED,
                registerDetail.getTotalObjects().getRemained()));

        actions.add(new IncAction(AccessionRegisterSummary.TOTAL_UNITS + "." + AccessionRegisterSummary.INGESTED,
                registerDetail.getTotalUnits().getIngested()));
        actions.add(new IncAction(AccessionRegisterSummary.TOTAL_UNITS + "." + AccessionRegisterSummary.DELETED,
                registerDetail.getTotalUnits().getDeleted()));
        actions.add(new IncAction(AccessionRegisterSummary.TOTAL_UNITS + "." + AccessionRegisterSummary.REMAINED,
                registerDetail.getTotalUnits().getRemained()));

        actions.add(new IncAction(AccessionRegisterSummary.OBJECT_SIZE + "." + AccessionRegisterSummary.INGESTED,
                registerDetail.getObjectSize().getIngested()));
        actions.add(new IncAction(AccessionRegisterSummary.OBJECT_SIZE + "." + AccessionRegisterSummary.DELETED,
                registerDetail.getObjectSize().getDeleted()));
        actions.add(new IncAction(AccessionRegisterSummary.OBJECT_SIZE + "." + AccessionRegisterSummary.REMAINED,
                registerDetail.getObjectSize().getRemained()));
        return actions;
    }
}
