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
package fr.gouv.vitam.functional.administration.common;

import fr.gouv.vitam.common.model.administration.AgenciesModel;

/**
 * error report for generate error Report when some think is wrong in import referential This class contain code in
 * vitam-error-messages.properties, the line of the error, Object in error to import and some
 * missingInformations for the import
 */
public class ErrorReportAgencies {

    /**
     * Error code
     */
    private FileAgenciesErrorCode code;
    /**
     * Line of the error in the .csv to import
     */
    private int line;
    /**
     * File Agencies Model in error in the .csv to import
     */
    private AgenciesModel fileAgenciesModel;

    /**
     * Missing informations in .csv
     */
    private String missingInformations;

    public ErrorReportAgencies() {
        super();
    }

    public ErrorReportAgencies(FileAgenciesErrorCode code, int line, AgenciesModel agenciesModel) {
        super();
        this.code = code;
        this.line = line;
        this.fileAgenciesModel = agenciesModel;
    }

    public ErrorReportAgencies(FileAgenciesErrorCode code, int line, String missingInformations) {
        super();
        this.code = code;
        this.line = line;
        this.missingInformations = missingInformations;
    }

    public String getMissingInformations() {
        return missingInformations;
    }

    public void setMissingInformations(String missingInformations) {
        this.missingInformations = missingInformations;
    }

    public FileAgenciesErrorCode getCode() {
        return code;
    }

    public void setCode(FileAgenciesErrorCode code) {
        this.code = code;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public AgenciesModel getFileAgenciesModel() {
        return fileAgenciesModel;
    }

    public void setFileAgenciesModel(AgenciesModel fileAgenciesModel) {
        this.fileAgenciesModel = fileAgenciesModel;
    }
}
