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
package fr.gouv.vitam.ihmrecette.appserver.populate;

import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;

/**
 * Unit Got Model class
 */
public class UnitGotModel {

    private UnitModel unit;
    private ObjectGroupModel got;
    private LogbookLifecycle logbookLifecycleUnit;
    private LogbookLifecycle logbookLifeCycleObjectGroup;
    private int objectSize;

    /**
     * Constructor
     * 
     * @param unit the unit
     */
    public UnitGotModel(UnitModel unit) {
        this.unit = unit;
    }

    /**
     * Constructor
     * 
     * @param unit the unit
     * @param got the object group
     */
    public UnitGotModel(UnitModel unit, ObjectGroupModel got) {
        this(unit, got, 0);
    }

    /**
     * Constructor
     * 
     * @param unit the unit
     * @param got the object group
     * @param objectSize the object size
     */
    public UnitGotModel(UnitModel unit, ObjectGroupModel got, Integer objectSize) {
        this.unit = unit;
        this.got = got;
        this.objectSize = (objectSize == null) ? 0 : objectSize;
    }

    /**
     * @return the unit
     */
    public UnitModel getUnit() {
        return unit;
    }

    /**
     * @param unit
     */
    public void setUnit(UnitModel unit) {
        this.unit = unit;
    }

    /**
     * @return the object group
     */
    public ObjectGroupModel getGot() {
        return got;
    }

    /**
     * @param got
     */
    public void setGot(ObjectGroupModel got) {
        this.got = got;
    }

    /**
     * @return object size
     */
    public int getObjectSize() {
        return objectSize;
    }

    /**
     * @param objectSize
     */
    public void setObjectSize(int objectSize) {
        this.objectSize = objectSize;
    }


    /**
     * @return logbookLifecycleUnit
     */
    public LogbookLifecycle getLogbookLifecycleUnit() {
        return logbookLifecycleUnit;
    }

    /**
     * @param logbookLifecycleUnit
     */
    public void setLogbookLifecycleUnit(LogbookLifecycle logbookLifecycleUnit) {
        this.logbookLifecycleUnit = logbookLifecycleUnit;
    }

    /**
     * @return logbookLifeCycleObjectGroup
     */
    public LogbookLifecycle getLogbookLifeCycleObjectGroup() {
        return logbookLifeCycleObjectGroup;
    }

    /**
     * @param logbookLifeCycleObjectGroup
     */
    public void setLogbookLifeCycleObjectGroup(LogbookLifecycle logbookLifeCycleObjectGroup) {
        this.logbookLifeCycleObjectGroup = logbookLifeCycleObjectGroup;
    }
}
