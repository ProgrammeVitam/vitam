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
package fr.gouv.vitam.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The class SedaVersion used to get the list the versions by type of Data Object from the file version.conf
 */
public class SedaVersion {

    private List<String> binaryDataObjectVersions;
    private List<String> physicalDataObjectVersions;

    /**
     * constructor
     */
    public SedaVersion() {
        binaryDataObjectVersions = new ArrayList<String>();
        physicalDataObjectVersions = new ArrayList<String>();
    }

    /**
     * @param type
     * @return version list for this type
     */
    public List<String> getVersionForType(String type) {
        if (SedaConstants.TAG_BINARY_DATA_OBJECT.equals(type)) {
            return binaryDataObjectVersions;
        } else if (SedaConstants.TAG_PHYSICAL_DATA_OBJECT.equals(type)) {
            return physicalDataObjectVersions;
        }
        return new ArrayList<>();
    }

    /**
     * @param type
     * @return version list for other type than the one requested
     */
    public List<String> getVersionForOtherType(String type) {
        if (SedaConstants.TAG_BINARY_DATA_OBJECT.equals(type)) {
            return physicalDataObjectVersions;
        } else if (SedaConstants.TAG_PHYSICAL_DATA_OBJECT.equals(type)) {
            return binaryDataObjectVersions;
        }
        return new ArrayList<>();
    }

    /**
     * @param binaryDataObjectVersions
     */
    public void setBinaryDataObjectVersions(String[] binaryDataObjectVersions) {
        this.binaryDataObjectVersions = Arrays.asList(binaryDataObjectVersions);
    }

    /**
     * @param physicalDataObjectVersions
     */
    public void setPhysicalDataObjectVersions(String[] physicalDataObjectVersions) {
        this.physicalDataObjectVersions = Arrays.asList(physicalDataObjectVersions);
    }

    /**
     * @param version
     * @return true if version is supported
     */
    public boolean isSupportedVesion(final String version) {
        if (binaryDataObjectVersions.contains(version) || physicalDataObjectVersions.contains(version)) {
            return true;
        }
        return false;
    }
}
