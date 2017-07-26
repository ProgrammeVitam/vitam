/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital 
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.functional.tnr.test;

/**
 * Junit TNR Configuration file
 */
public class JunitTnrConfiguration {
    /**
     * Home of Vitam-itest
     */
    private String homeItest = "/home/vitam/workspace2/vitam-itests/";
    /**
     * Home of Vitam-itest
     */
    private String specificItest = null;
    /**
     * Root of Vitam DATA
     */
    private String vitamData = "/vitam/data/";
    /**
     *  Path of siegfried command
     */
    private String siegfriedPath = "/usr/bin/sf";
    /**
     *  Path data of siegfried
     */
    private String dataSiegfried = vitamData + "siegfried";
    /**
     *  Is Siegfried to be launched (true) or alrady launched (using port 8102)
     */
    private boolean launchSiegfried = true;

    /**
     * 
     */
    public JunitTnrConfiguration() {
        // Empty
    }

    /**
     * @return the homeItest
     */
    public String getHomeItest() {
        return homeItest;
    }

    /**
     * @param homeItest the homeItest to set
     *
     * @return this
     */
    public JunitTnrConfiguration setHomeItest(String homeItest) {
        this.homeItest = homeItest;
        return this;
    }

    /**
     * @return the specificItest
     */
    public String getSpecificItest() {
        return specificItest;
    }

    /**
     * @param specificItest the specificItest to set
     *
     * @return this
     */
    public JunitTnrConfiguration setSpecificItest(String specificItest) {
        this.specificItest = specificItest;
        return this;
    }

    /**
     * @return the vitamData
     */
    public String getVitamData() {
        return vitamData;
    }

    /**
     * @param vitamData the vitamData to set
     *
     * @return this
     */
    public JunitTnrConfiguration setVitamData(String vitamData) {
        this.vitamData = vitamData;
        return this;
    }

    /**
     * @return the siegfriedPath
     */
    public String getSiegfriedPath() {
        return siegfriedPath;
    }

    /**
     * @param siegfriedPath the siegfriedPath to set
     *
     * @return this
     */
    public JunitTnrConfiguration setSiegfriedPath(String siegfriedPath) {
        this.siegfriedPath = siegfriedPath;
        return this;
    }

    /**
     * @return the dataSiegfried
     */
    public String getDataSiegfried() {
        return dataSiegfried;
    }

    /**
     * @param dataSiegfried the dataSiegfried to set
     *
     * @return this
     */
    public JunitTnrConfiguration setDataSiegfried(String dataSiegfried) {
        this.dataSiegfried = dataSiegfried;
        return this;
    }

    /**
     * @return the launchSiegfried
     */
    public boolean isLaunchSiegfried() {
        return launchSiegfried;
    }

    /**
     * @param launchSiegfried the launchSiegfried to set
     *
     * @return this
     */
    public JunitTnrConfiguration setLaunchSiegfried(boolean launchSiegfried) {
        this.launchSiegfried = launchSiegfried;
        return this;
    }

}
