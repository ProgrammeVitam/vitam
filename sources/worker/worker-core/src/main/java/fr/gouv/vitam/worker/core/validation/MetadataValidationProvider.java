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
package fr.gouv.vitam.worker.core.validation;

import fr.gouv.vitam.common.client.OntologyLoader;
import fr.gouv.vitam.common.database.collections.CachedOntologyLoader;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.client.AdminManagementOntologyLoader;
import fr.gouv.vitam.metadata.core.validation.CachedArchiveUnitProfileLoader;
import fr.gouv.vitam.metadata.core.validation.CachedSchemaValidatorLoader;
import fr.gouv.vitam.metadata.core.validation.OntologyValidator;
import fr.gouv.vitam.metadata.core.validation.UnitValidator;

import java.util.Optional;

public final class MetadataValidationProvider {

    private static MetadataValidationProvider INSTANCE = new MetadataValidationProvider();
    private OntologyValidator unitOntologyValidator;
    private OntologyValidator objectGroupOntologyValidator;
    private UnitValidator unitValidator;

    public static MetadataValidationProvider getInstance() {
        return INSTANCE;
    }

    private MetadataValidationProvider() {
    }

    public void initialize(
        AdminManagementClientFactory adminManagementClientFactory,
        int ontologyCacheMaxEntries, int ontologyCacheTimeoutInSeconds,
        int archiveUnitProfileCacheMaxEntries, int archiveUnitProfileCacheTimeoutInSeconds,
        int schemaValidatorCacheMaxEntries, int schemaValidatorCacheTimeoutInSeconds) {

        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = new CachedArchiveUnitProfileLoader(
            adminManagementClientFactory, archiveUnitProfileCacheMaxEntries, archiveUnitProfileCacheTimeoutInSeconds);

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(
            schemaValidatorCacheMaxEntries, schemaValidatorCacheTimeoutInSeconds);

        OntologyLoader unitOntologyLoader = new CachedOntologyLoader(
            ontologyCacheMaxEntries,
            ontologyCacheTimeoutInSeconds,
            new AdminManagementOntologyLoader(adminManagementClientFactory, Optional.of(MetadataType.UNIT.getName()))
        );

        OntologyLoader objectGroupOntologyLoader = new CachedOntologyLoader(
            ontologyCacheMaxEntries,
            ontologyCacheTimeoutInSeconds,
            new AdminManagementOntologyLoader(adminManagementClientFactory,
                Optional.of(MetadataType.OBJECTGROUP.getName()))
        );

        this.unitOntologyValidator = new OntologyValidator(unitOntologyLoader);
        this.objectGroupOntologyValidator = new OntologyValidator(objectGroupOntologyLoader);

        this.unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);
    }

    public OntologyValidator getUnitOntologyValidator() {
        return unitOntologyValidator;
    }

    public OntologyValidator getObjectGroupOntologyValidator() {
        return objectGroupOntologyValidator;
    }

    public UnitValidator getUnitValidator() {
        return unitValidator;
    }
}
