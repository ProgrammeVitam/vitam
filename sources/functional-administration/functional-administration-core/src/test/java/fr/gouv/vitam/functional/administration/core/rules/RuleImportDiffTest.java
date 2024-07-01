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
package fr.gouv.vitam.functional.administration.core.rules;

import fr.gouv.vitam.common.model.administration.FileRulesModel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.common.model.administration.RuleMeasurementEnum.YEAR;
import static fr.gouv.vitam.common.model.administration.RuleType.AppraisalRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RuleImportDiffTest {

    @Test
    public void bothNullListReturnEmptyDiff() {
        List<FileRulesModel> ruleInDatabase = null;
        List<FileRulesModel> ruleInFile = null;
        RuleImportDiff diff = new RuleImportDiff(ruleInDatabase, ruleInFile);

        List<FileRulesModel> rulesToInsert = diff.getRulesToInsert();
        assertThat(rulesToInsert).isNotNull();
        assertThat(rulesToInsert).isEmpty();

        List<FileRulesModel> rulesToUpdate = diff.getRulesToUpdate();
        assertThat(rulesToUpdate).isNotNull();
        assertThat(rulesToUpdate).isEmpty();

        List<FileRulesModel> rulesToUpdateUnsafely = diff.getRulesToUpdateUnsafely();
        assertThat(rulesToUpdateUnsafely).isNotNull();
        assertThat(rulesToUpdateUnsafely).isEmpty();

        List<FileRulesModel> rulesToDelete = diff.getRulesToDelete();
        assertThat(rulesToDelete).isNotNull();
        assertThat(rulesToDelete).isEmpty();
    }

    @Test
    public void shouldHaveOneInsertOneUpdateOneDelete() {
        FileRulesModel ruleToDelete = new FileRulesModel(
            "APP-00001",
            AppraisalRule,
            "Dossier individuel d’agent civil",
            "ruleDescription",
            "80",
            YEAR
        );

        FileRulesModel ruleToUpdate = new FileRulesModel(
            "APP-00002",
            AppraisalRule,
            "Dossier individuel d’agent civil",
            "ruleDescription",
            "80",
            YEAR
        );

        FileRulesModel ruleUpdated = new FileRulesModel(
            "APP-00002",
            AppraisalRule,
            "Dossier individuel d’agent civil",
            "new rule description",
            "80",
            YEAR
        );

        FileRulesModel ruleToInsert = new FileRulesModel(
            "APP-00003",
            AppraisalRule,
            "Dossier individuel d’agent civil",
            "ruleDescription",
            "80",
            YEAR
        );
        List<FileRulesModel> ruleInDatabase = new ArrayList<>();
        List<FileRulesModel> ruleInFile = new ArrayList<>();

        ruleInDatabase.add(ruleToDelete);
        ruleInDatabase.add(ruleToUpdate);

        ruleInFile.add(ruleUpdated);
        ruleInFile.add(ruleToInsert);

        RuleImportDiff diff = new RuleImportDiff(ruleInFile, ruleInDatabase);
        assertNotNull(diff);

        assertNotNull(diff.getRulesToInsert());
        assertThat(diff.getRulesToInsert().size()).isEqualTo(1);
        assertEquals(ruleToInsert.getRuleId(), diff.getRulesToInsert().get(0).getRuleId());

        assertNotNull(diff.getRulesToUpdate());
        assertThat(diff.getRulesToUpdate().size()).isEqualTo(1);
        assertEquals(ruleUpdated.getRuleId(), diff.getRulesToUpdate().get(0).getRuleId());

        assertNotNull(diff.getRulesToUpdateUnsafely());
        assertThat(diff.getRulesToUpdateUnsafely().size()).isEqualTo(0);

        assertNotNull(diff.getRulesToDelete());
        assertThat(diff.getRulesToDelete().size()).isEqualTo(1);
        assertEquals(ruleToDelete.getRuleId(), diff.getRulesToDelete().get(0).getRuleId());
    }

    @Test
    public void shouldHaveOneUpdateOneUpdateUnsafely() {
        FileRulesModel ruleThatAlreadyExists = new FileRulesModel(
            "APP-00003",
            AppraisalRule,
            "Dossier individuel d’agent civil",
            "ruleDescription",
            "80",
            YEAR
        );

        FileRulesModel ruleToUpdate = new FileRulesModel(
            "APP-00002",
            AppraisalRule,
            "Dossier individuel d’agent civil",
            "ruleDescription",
            "80",
            YEAR
        );

        FileRulesModel ruleUpdated = new FileRulesModel(
            "APP-00002",
            AppraisalRule,
            "Dossier individuel d’agent civil",
            "ruleDescription",
            "100",
            YEAR
        );

        List<FileRulesModel> ruleInDatabase = new ArrayList<>();
        List<FileRulesModel> ruleInFile = new ArrayList<>();

        ruleInDatabase.add(ruleToUpdate);
        ruleInDatabase.add(ruleThatAlreadyExists);
        ruleInFile.add(ruleUpdated);
        ruleInFile.add(ruleThatAlreadyExists);

        RuleImportDiff diff = new RuleImportDiff(ruleInFile, ruleInDatabase);
        assertNotNull(diff);

        assertNotNull(diff.getRulesToUpdateUnsafely());
        assertThat(diff.getRulesToUpdateUnsafely().size()).isEqualTo(1);
        assertEquals(ruleUpdated.getRuleId(), diff.getRulesToUpdateUnsafely().get(0).getRuleId());

        assertNotNull(diff.getRulesToUpdate());
        assertThat(diff.getRulesToUpdate().size()).isEqualTo(1);
        assertEquals(ruleUpdated.getRuleId(), diff.getRulesToUpdate().get(0).getRuleId());

        assertNotNull(diff.getRulesToInsert());
        assertThat(diff.getRulesToInsert().size()).isEqualTo(0);
        assertNotNull(diff.getRulesToDelete());
        assertThat(diff.getRulesToDelete().size()).isEqualTo(0);
    }
}
