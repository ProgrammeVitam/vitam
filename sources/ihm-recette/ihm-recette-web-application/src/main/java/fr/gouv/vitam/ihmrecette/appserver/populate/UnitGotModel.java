/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ihmrecette.appserver.populate;

import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycle;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;

public class UnitGotModel {

    private UnitModel unit;
    private ObjectGroupModel got;
    private LogbookLifecycle logbookLifecycleUnit;
    private LogbookLifecycle logbookLifeCycleObjectGroup;
    private int objectSize;

    public UnitGotModel(UnitModel unit) {
        this.unit = unit;
    }

    public UnitGotModel(UnitModel unit, ObjectGroupModel got) {
        this(unit, got, 0);
    }

    public UnitGotModel(UnitModel unit, ObjectGroupModel got, Integer objectSize) {
        this.unit = unit;
        this.got = got;
        this.objectSize = (objectSize == null) ? 0 : objectSize;
    }

    public UnitModel getUnit() {
        return unit;
    }

    public void setUnit(UnitModel unit) {
        this.unit = unit;
    }

    public ObjectGroupModel getGot() {
        return got;
    }

    public void setGot(ObjectGroupModel got) {
        this.got = got;
    }

    public int getObjectSize() {
        return objectSize;
    }

    public void setObjectSize(int objectSize) {
        this.objectSize = objectSize;
    }


    public LogbookLifecycle getLogbookLifecycleUnit() {
        return logbookLifecycleUnit;
    }

    public void setLogbookLifecycleUnit(LogbookLifecycle logbookLifecycleUnit) {
        this.logbookLifecycleUnit = logbookLifecycleUnit;
    }

    public LogbookLifecycle getLogbookLifeCycleObjectGroup() {
        return logbookLifeCycleObjectGroup;
    }

    public void setLogbookLifeCycleObjectGroup(LogbookLifecycle logbookLifeCycleObjectGroup) {
        this.logbookLifeCycleObjectGroup = logbookLifeCycleObjectGroup;
    }
}
