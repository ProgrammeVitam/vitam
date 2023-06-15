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
package fr.gouv.vitam.common.manifest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ListMultimap;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

public class ManifestBuilderTest {

    private InputStream objectGroup;

    private static final String OBJECT_GROUP = "aeaaaaaaaaaam7myaaaamakxfgivuryaaaaq.json";

    private JsonNode og;

    @Test
    public void should_write_archive_unit_without_empty_tags()
        throws JAXBException, DatatypeConfigurationException, XMLStreamException, IllegalAccessException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ManifestBuilder manifestBuilder = new ManifestBuilder(outputStream);
        ArchiveUnitModel archiveUnitModel = createArchiveUnitModel();
        ListMultimap<String, String> multimap = mock(ListMultimap.class);
        Map<String, String> ogs = mock(Map.class);
        manifestBuilder.writeArchiveUnit(archiveUnitModel, multimap, ogs);
        String xmlContent = outputStream.toString();
        // Vérifier que le contenu XML ne contient aucune balise vide
        Assert.assertFalse("La chaîne XML contient des balises vides.", containsSomeEmptyTags(xmlContent));
    }


    @Test
    public void should_write_got_without_empty_tags()
        throws JAXBException, XMLStreamException,
        FileNotFoundException, InvalidParseOperationException, InternalServerException, JsonProcessingException {
        objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP);
        og = JsonHandler.getFromInputStream(objectGroup);
        Stream<LogbookLifeCycleObjectGroup> logbookLifeCycleObjectGroupStream = Stream.empty();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ManifestBuilder manifestBuilder = new ManifestBuilder(outputStream);
        manifestBuilder.writeGOT(og, "", logbookLifeCycleObjectGroupStream);
        String xmlContent = outputStream.toString();
        // Vérifier que le contenu XML ne contient aucune balise vide
        Assert.assertFalse("La chaîne XML contient des balises vides.", containsSomeEmptyTags(xmlContent));
    }

    private boolean containsSomeEmptyTags(String xmlContent) {
        Pattern pattern = Pattern.compile("<((\\w+\\:)*(LogBook|Management|Description))>\\s*<\\/((\\w+\\:)*(LogBook|Management|Description))>");
        Matcher matcher = pattern.matcher(xmlContent);
        return matcher.find();
    }


    private ArchiveUnitModel createArchiveUnitModel() {
        ArchiveUnitModel archiveUnitModel = new ArchiveUnitModel();
        archiveUnitModel.setId("1234564");
        archiveUnitModel.setDescriptiveMetadataModel(new DescriptiveMetadataModel());
        RuleCategoryModel rule;

        //AccessRule
        rule = generateRule(SedaConstants.TAG_RULE_ACCESS);
        archiveUnitModel.getManagement().setRuleCategoryModel(rule, SedaConstants.TAG_RULE_ACCESS);

        //AppraisalRule
        rule = generateRule(SedaConstants.TAG_RULE_APPRAISAL);
        archiveUnitModel.getManagement().setRuleCategoryModel(rule, SedaConstants.TAG_RULE_APPRAISAL);

        //ClassificationRule
        rule = generateRule(SedaConstants.TAG_RULE_CLASSIFICATION);
        rule.setClassificationLevel("fakeClassificationLevel");
        rule.setClassificationOwner("fakeClassificationOwner");
        rule.setNeedReassessingAuthorization(true);
        rule.setClassificationReassessingDate("2000-01-02");
        archiveUnitModel.getManagement().setRuleCategoryModel(rule, SedaConstants.TAG_RULE_CLASSIFICATION);

        //DisseminationRule
        rule = generateRule(SedaConstants.TAG_RULE_DISSEMINATION);
        archiveUnitModel.getManagement().setRuleCategoryModel(rule, SedaConstants.TAG_RULE_DISSEMINATION);

        //ReuseRule
        rule = generateRule(SedaConstants.TAG_RULE_REUSE);
        archiveUnitModel.getManagement().setRuleCategoryModel(rule, SedaConstants.TAG_RULE_REUSE);

        //StorageRule
        rule = generateRule(SedaConstants.TAG_RULE_STORAGE);
        archiveUnitModel.getManagement().setRuleCategoryModel(rule, SedaConstants.TAG_RULE_STORAGE);

        // HoldRule
        rule = generateRule(SedaConstants.TAG_RULE_HOLD);
        archiveUnitModel.getManagement().setRuleCategoryModel(rule, SedaConstants.TAG_RULE_HOLD);

        return archiveUnitModel;
    }

    @Test
    public void should_write_archive_unit_with_empty_management_without_empty_tags()
        throws JAXBException, DatatypeConfigurationException, XMLStreamException, IllegalAccessException {
        ArchiveUnitModel archiveUnitModel = new ArchiveUnitModel();
        archiveUnitModel.setId("1234564");
        archiveUnitModel.setDescriptiveMetadataModel(new DescriptiveMetadataModel());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ManifestBuilder manifestBuilder = new ManifestBuilder(outputStream);
        ListMultimap<String, String> multimap = mock(ListMultimap.class);
        Map<String, String> ogs = mock(Map.class);
        manifestBuilder.writeArchiveUnit(archiveUnitModel, multimap, ogs);
        String xmlContent = outputStream.toString();
        // Vérifier que le contenu XML ne contient aucune balise vide
        Assert.assertFalse("La chaîne XML contient des balises vides.", containsSomeEmptyTags(xmlContent));
    }


    private RuleCategoryModel generateRule(String type) {
        RuleCategoryModel ruleCategoryModel = new RuleCategoryModel();
        ruleCategoryModel.addToPreventRulesId("R1");
        ruleCategoryModel.addToPreventRulesId("R2");
        ruleCategoryModel.setPreventInheritance(true);

        RuleModel rule = new RuleModel();
        rule.setRule("R3");
        rule.setStartDate("2000-01-01");

        switch (type) {
            case SedaConstants.TAG_RULE_STORAGE:
                ruleCategoryModel.setFinalAction("Copy");
                break;
            case SedaConstants.TAG_RULE_APPRAISAL:
                ruleCategoryModel.setFinalAction("Keep");

                break;
            case SedaConstants.TAG_RULE_ACCESS:
                rule.setStartDate(null);

            case SedaConstants.TAG_RULE_DISSEMINATION:
            case SedaConstants.TAG_RULE_REUSE:
            case SedaConstants.TAG_RULE_CLASSIFICATION:
                break;
            case SedaConstants.TAG_RULE_HOLD:
                rule.setHoldEndDate("2000-02-02");
                rule.setHoldOwner("Owner");
                rule.setHoldReason("Reason");
                rule.setHoldReassessingDate("2009-02-02");
                rule.setPreventRearrangement(true);
                break;
            default:
                throw new IllegalArgumentException("Type cannot be " + type);
        }
        ruleCategoryModel.getRules().add(rule);

        return ruleCategoryModel;
    }
}
