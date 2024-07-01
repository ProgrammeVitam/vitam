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

import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.functional.administration.common.ErrorReportAgencies;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AgenciesImportResult {

    private final Set<AgenciesModel> agenciesToImport;

    private Collection<AgenciesModel> insertedAgencies;

    private Collection<AgenciesModel> updatedAgencies;

    private Collection<AgenciesModel> deletedAgencies;

    private Collection<AgenciesModel> usedAgenciesContract;

    private Collection<AgenciesModel> usedAgenciesAU;

    private final Map<Integer, List<ErrorReportAgencies>> errorsMap;

    public AgenciesImportResult() {
        this(Collections.emptySet(), Collections.emptyMap());
    }

    public AgenciesImportResult(
        Set<AgenciesModel> agenciesToImport,
        Map<Integer, List<ErrorReportAgencies>> errorsMap
    ) {
        this.agenciesToImport = agenciesToImport;
        this.errorsMap = errorsMap;
        this.insertedAgencies = Collections.emptySet();
        this.updatedAgencies = Collections.emptySet();
        this.deletedAgencies = Collections.emptySet();
        this.usedAgenciesContract = Collections.emptySet();
        this.usedAgenciesAU = Collections.emptySet();
    }

    public Set<AgenciesModel> getAgenciesToImport() {
        return agenciesToImport;
    }

    public Map<Integer, List<ErrorReportAgencies>> getErrorsMap() {
        return errorsMap;
    }

    public Collection<AgenciesModel> getInsertedAgencies() {
        return insertedAgencies;
    }

    public void setInsertedAgencies(Collection<AgenciesModel> insertedAgencies) {
        this.insertedAgencies = insertedAgencies;
    }

    public Collection<AgenciesModel> getUpdatedAgencies() {
        return updatedAgencies;
    }

    public void setUpdatedAgencies(Collection<AgenciesModel> updatedAgencies) {
        this.updatedAgencies = updatedAgencies;
    }

    public Collection<AgenciesModel> getDeletedAgencies() {
        return deletedAgencies;
    }

    public void setDeletedAgencies(Collection<AgenciesModel> deletedAgencies) {
        this.deletedAgencies = deletedAgencies;
    }

    public Collection<AgenciesModel> getUsedAgenciesContract() {
        return usedAgenciesContract;
    }

    public void setUsedAgenciesContract(Collection<AgenciesModel> usedAgenciesContract) {
        this.usedAgenciesContract = usedAgenciesContract;
    }

    public Collection<AgenciesModel> getUsedAgenciesAU() {
        return usedAgenciesAU;
    }

    public void setUsedAgenciesAU(Collection<AgenciesModel> usedAgenciesAU) {
        this.usedAgenciesAU = usedAgenciesAU;
    }
}
