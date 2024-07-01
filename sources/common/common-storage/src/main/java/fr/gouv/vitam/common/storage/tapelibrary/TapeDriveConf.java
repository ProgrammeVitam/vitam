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
package fr.gouv.vitam.common.storage.tapelibrary;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * Tape drive that read and write in the tape cartridge
 */
public class TapeDriveConf {

    private Integer index;
    private String device;
    private String mtPath = "mt";
    private String ddPath = "dd";

    private ReadWritePriority readWritePriority = ReadWritePriority.WRITE;

    private long timeoutInMilliseconds = 60000;

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        ParametersChecker.checkParameter("index param is required", index);
        this.index = index;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        ParametersChecker.checkParameter("device param is required", device);
        this.device = device;
    }

    public String getMtPath() {
        return mtPath;
    }

    public void setMtPath(String mtPath) {
        this.mtPath = mtPath;
    }

    public String getDdPath() {
        return ddPath;
    }

    public void setDdPath(String ddPath) {
        this.ddPath = ddPath;
    }

    public ReadWritePriority getReadWritePriority() {
        return readWritePriority;
    }

    public void setReadWritePriority(ReadWritePriority readWritePriority) {
        this.readWritePriority = readWritePriority;
    }

    public long getTimeoutInMilliseconds() {
        return timeoutInMilliseconds;
    }

    public void setTimeoutInMilliseconds(long timeoutInMilliseconds) {
        this.timeoutInMilliseconds = timeoutInMilliseconds;
    }
}
