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
package fr.gouv.vitam.functional.administration.core.agencies;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;

/**
 * Data Transfer Object Model of Reporting
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AgenciesReport {

    public static final String AGENCIES_TO_IMPORT = "AgenciesToImport";

    public static final String INSERTED_AGENCIES = "InsertedAgencies";

    public static final String UPDATED_AGENCIES = "UpdatedAgencies";

    public static final String DELETED_AGENCIES = "DeletedAgencies";

    public static final String USED_AGENCIES_BY_CONTRACTS = "UsedAgenciesByContracts";

    public static final String USED_AGENCIES_BY_AU = "UsedAgenciesByAu";

    public static final String OPERATION = "Operation";

    public static final String ERRORS = "Errors";

    @JsonProperty(AGENCIES_TO_IMPORT)
    private List<String> agenciesToImport;

    @JsonProperty(INSERTED_AGENCIES)
    private List<String> insertedAgencies;

    @JsonProperty(UPDATED_AGENCIES)
    private List<String> updatedAgencies;

    @JsonProperty(DELETED_AGENCIES)
    private List<String> agenciesToDelete;

    @JsonProperty(USED_AGENCIES_BY_CONTRACTS)
    private List<String> usedAgenciesByContracts;

    @JsonProperty(USED_AGENCIES_BY_AU)
    private List<String> usedAgenciesByAu;

    @JsonProperty(OPERATION)
    private HashMap<String, String> operation;

    @JsonProperty(ERRORS)
    private HashMap<String, Object> errors;

    public List<String> getAgenciesToImport() {
        return agenciesToImport;
    }

    public void setAgenciesToImport(List<String> agenciesToImport) {
        this.agenciesToImport = agenciesToImport;
    }

    public List<String> getInsertedAgencies() {
        return insertedAgencies;
    }

    public void setInsertedAgencies(List<String> insertedAgencies) {
        this.insertedAgencies = insertedAgencies;
    }

    public List<String> getUpdatedAgencies() {
        return updatedAgencies;
    }

    public void setUpdatedAgencies(List<String> updatedAgencies) {
        this.updatedAgencies = updatedAgencies;
    }

    public List<String> getAgenciesToDelete() {
        return agenciesToDelete;
    }

    public void setAgenciesToDelete(List<String> agenciesToDelete) {
        this.agenciesToDelete = agenciesToDelete;
    }

    public List<String> getUsedAgenciesByContracts() {
        return usedAgenciesByContracts;
    }

    public void setUsedAgenciesByContracts(List<String> usedAgenciesByContracts) {
        this.usedAgenciesByContracts = usedAgenciesByContracts;
    }

    public List<String> getUsedAgenciesByAu() {
        return usedAgenciesByAu;
    }

    public void setUsedAgenciesByAu(List<String> usedAgenciesByAu) {
        this.usedAgenciesByAu = usedAgenciesByAu;
    }

    public HashMap<String, String> getOperation() {
        return operation;
    }

    public void setOperation(HashMap<String, String> operation) {
        this.operation = operation;
    }

    public HashMap<String, Object> getErrors() {
        return errors;
    }

    public void setErrors(HashMap<String, Object> errors) {
        this.errors = errors;
    }
}
