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

package fr.gouv.vitam.common.database.server.elasticsearch;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;

import java.util.regex.Pattern;

/**
 * Represents an elasticsearch index or index alias
 */
public class ElasticsearchIndexAlias {

    private static final Pattern ES_INDEX_OR_ALIAS_NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");

    /**
     * Builder for collection that store documents of all tenants in same index (functional admin collections)
     */
    public static ElasticsearchIndexAlias ofCrossTenantCollection(String collectionName) {
        ParametersChecker.checkParameter("Missing collection name", collectionName);
        return new ElasticsearchIndexAlias(collectionName.toLowerCase());
    }

    /**
     * Builder for collection that store documents per-tenant indexes
     * (unit/objectgroup/logbookoperation collections when tenant is not in a tenant group)
     */
    public static ElasticsearchIndexAlias ofMultiTenantCollection(String collectionName, int tenantId) {
        ParametersChecker.checkParameter("Missing collection name", collectionName);
        return new ElasticsearchIndexAlias(collectionName.toLowerCase() + "_" + tenantId);
    }

    /**
     * Builder for collection that store documents per-tenant-group indexes
     * (unit/objectgroup/logbookoperation collections when tenant is in a tenant group)
     */
    public static ElasticsearchIndexAlias ofMultiTenantCollection(String collectionName, String tenantGroupName) {
        ParametersChecker.checkParameter("Missing collection name", collectionName);
        ParametersChecker.checkParameter("Missing tenant group name", tenantGroupName);
        return new ElasticsearchIndexAlias(collectionName.toLowerCase() + "_" + tenantGroupName);
    }

    /**
     * Stored for building indexes with exact full name (eg. index creation / alias switching)
     */
    public static ElasticsearchIndexAlias ofFullIndexName(String fullIndexName) {
        ParametersChecker.checkParameter("Missing collection name", fullIndexName);
        return new ElasticsearchIndexAlias(fullIndexName);
    }

    private final String name;

    private ElasticsearchIndexAlias(String name) {
        if (!ES_INDEX_OR_ALIAS_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalStateException("Invalid alias name '" + name + "'");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isValidAliasOfIndex(ElasticsearchIndexAlias indexName) {
        ParametersChecker.checkParameter("Missing index name", indexName);
        return indexName.getName().startsWith(this.name + "_");
    }

    public ElasticsearchIndexAlias createUniqueIndexName() {
        final String currentDate = LocalDateUtil.getFormattedDateForEsIndexes(LocalDateUtil.now());
        return ofFullIndexName(this.name + "_" + currentDate);
    }

    @Override
    public String toString() {
        return "ElasticsearchIndexAlias{" + "name='" + name + '\'' + '}';
    }
}
