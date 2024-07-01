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
package fr.gouv.vitam.collect.internal.core.helpers.builders;

import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbFileInfoModel;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.DbQualifiersModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DbObjectGroupModelBuilder {

    private String id;
    private String opi;
    private DbFileInfoModel fileInfoModel;
    private List<DbQualifiersModel> qualifiers;

    public DbObjectGroupModelBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public DbObjectGroupModelBuilder withOpi(String opi) {
        this.opi = opi;
        return this;
    }

    public DbObjectGroupModelBuilder withQualifiers(List<DbQualifiersModel> qualifiers) {
        this.qualifiers = qualifiers;
        return this;
    }

    public DbObjectGroupModelBuilder withQualifiers(
        String versionId,
        String fileName,
        DataObjectVersionType usage,
        Integer version
    ) {
        this.qualifiers = Collections.singletonList(
            new DbQualifiersModelBuilder()
                .withUsage(usage)
                .withVersion(versionId, fileName, usage, version)
                .withNbc(1)
                .build()
        );
        return this;
    }

    public DbObjectGroupModelBuilder withFileInfoModel(String fileName) {
        Objects.requireNonNull(fileName, "FileName can't be null");
        fileInfoModel = new DbFileInfoModel();
        fileInfoModel.setFilename(fileName);
        return this;
    }

    public DbObjectGroupModel build() {
        Objects.requireNonNull(id, "Id can't be null");
        Objects.requireNonNull(opi, "Opi can't be null");
        Objects.requireNonNull(fileInfoModel, "FileInfoModel can't be null");
        Objects.requireNonNull(qualifiers, "QualifiersModel can't be null");

        DbObjectGroupModel model = new DbObjectGroupModel();
        model.setId(this.id);
        model.setOpi(this.opi);
        model.setFileInfo(fileInfoModel);

        int nbc = qualifiers.stream().map(DbQualifiersModel::getNbc).reduce(Integer::sum).orElse(0);
        model.setNbc(nbc);
        model.setQualifiers(qualifiers);

        return model;
    }
}
