package fr.gouv.vitam.functional.administration.rules.core;

import fr.gouv.vitam.common.model.administration.FileRulesModel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
        FileRulesModel ruleToDelete = new FileRulesModel("APP-00001",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");

        FileRulesModel ruleToUpdate = new FileRulesModel("APP-00002",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");

        FileRulesModel ruleUpdated = new FileRulesModel("APP-00002",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "new rule description",
                "80",
                "YEAR");

        FileRulesModel ruleToInsert = new FileRulesModel("APP-00003",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");
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
        FileRulesModel ruleThatAlreadyExists = new FileRulesModel("APP-00003",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");

        FileRulesModel ruleToUpdate = new FileRulesModel("APP-00002",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "80",
                "YEAR");

        FileRulesModel ruleUpdated = new FileRulesModel("APP-00002",
                "AppraisalRule",
                "Dossier individuel d’agent civil",
                "ruleDescription",
                "100",
                "YEAR");

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