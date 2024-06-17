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
package fr.gouv.vitam.metadata.core.database.collections;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.LocalDateUtil;
import org.bson.Document;

/**
 * Unit class:<br>
 *
 * @formatter:off { MD content, _id: UUID, _tenant: tenant, _profil: documentType,, _min: depthmin, _max: depthmax,
 * _mgt. Management structure, _uds: { depth1: [UUID1], depth2: [UUID2, UUID3] ... },
 * _us: [ UUID1, UUID2, ... }, // indexed and equivalent to _uds _up: [ UUID1, UUID2, ... ], //
 * limited to immediate parent _og: UUID, _nbc : immediateChildNb }
 * @formatter:on
 */
public class Unit extends MetadataDocument<Unit> {

    private static final long serialVersionUID = -4351321928647834270L;

    /**
     * UNITDEPTHS : { depth1: [UUID1], depth2: [UUID2, UUID3] }
     */
    public static final String UNITDEPTHS = "_uds";
    /**
     * UNITUPS : [ UUID1, UUID2 ]
     */
    public static final String UNITUPS = "_us";
    /**
     * MINDEPTH : min
     */
    public static final String MINDEPTH = "_min";
    /**
     * MAXDEPTH : max
     */
    public static final String MAXDEPTH = "_max";
    /**
     * Management : { various rules per themes }
     */
    public static final String MANAGEMENT = "_mgt";

    /**
     * elimination
     */
    public static final String ELIMINATION = "_elimination";
    public static final String COMPUTED_INHERITED_RULES = "_computedInheritedRules";
    public static final String VALID_COMPUTED_INHERITED_RULES = "_validComputedInheritedRules";
    public static final String OPERATION_TRANSFERS = "_opts";
    /**
     * UnitType : normal or holding scheme
     */
    public static final String UNIT_TYPE = "_unitType";
    public static final String GRAPH = "_graph";
    static final String HISTORY = "_history";

    @Override
    public MetadataDocument<Unit> newInstance(JsonNode content) {
        return new Unit(content);
    }

    /**
     * Storage Rule
     */
    static final String STORAGERULE = MANAGEMENT + ".StorageRule";
    /**
     * Appraisal Rule
     */
    static final String APPRAISALRULE = MANAGEMENT + ".AppraisalRule";
    /**
     * Access Rule
     */
    static final String ACCESSRULE = MANAGEMENT + ".AccessRule";
    /**
     * Dissemination Rule
     */
    static final String DISSEMINATIONRULE = MANAGEMENT + ".DisseminationRule";
    /**
     * Reuse Rule
     */
    static final String REUSERULE = MANAGEMENT + ".ReuseRule";
    /**
     * Classification Rule
     */
    static final String CLASSIFICATIONRULE = MANAGEMENT + ".ClassificationRule";

    /**
     * Rule
     */
    static final String RULE = ".Rules.Rule";
    /**
     * Rule end date (computed)
     */
    static final String END = ".Rules._end";

    @SuppressWarnings("javadoc")
    static final String STORAGERULES = STORAGERULE + RULE;

    @SuppressWarnings("javadoc")
    static final String STORAGEEND = STORAGERULE + END;

    @SuppressWarnings("javadoc")
    static final String APPRAISALRULES = APPRAISALRULE + RULE;

    @SuppressWarnings("javadoc")
    static final String APPRAISALEND = APPRAISALRULE + END;

    @SuppressWarnings("javadoc")
    static final String ACCESSRULES = ACCESSRULE + RULE;

    @SuppressWarnings("javadoc")
    static final String ACCESSEND = ACCESSRULE + END;

    @SuppressWarnings("javadoc")
    static final String DISSEMINATIONRULES = DISSEMINATIONRULE + RULE;

    @SuppressWarnings("javadoc")
    static final String DISSEMINATIONEND = DISSEMINATIONRULE + END;

    @SuppressWarnings("javadoc")
    static final String REUSERULES = REUSERULE + RULE;

    @SuppressWarnings("javadoc")
    static final String REUSEEND = REUSERULE + END;

    @SuppressWarnings("javadoc")
    static final String CLASSIFICATIONRULES = CLASSIFICATIONRULE + RULE;

    @SuppressWarnings("javadoc")
    static final String CLASSIFICATIONEND = CLASSIFICATIONRULE + END;

    /**
     * Empty constructor
     */
    public Unit() {
        // empty
    }

    /**
     * Constructor from Json
     *
     * @param content of type JsonNode for building Unit
     */
    public Unit(JsonNode content) {
        super(content);
    }

    /**
     * Constructor from Document
     *
     * @param content of type Document for building Unit
     */
    public Unit(Document content) {
        super(content);
    }

    /**
     * Constructor from Json as Text
     *
     * @param content of type String for building Unit
     */
    public Unit(String content) {
        super(content);
    }

    @Override
    protected MongoCollection<Unit> getCollection() {
        return MetadataCollections.UNIT.getCollection();
    }

    @Override
    protected MetadataCollections getMetadataCollections() {
        return MetadataCollections.UNIT;
    }

    /**
     * add graph information into unit.
     *
     * @param unitGraphModel
     */
    public void mergeWith(UnitGraphModel unitGraphModel) {
        put(UP, unitGraphModel.parents());
        put(UNITUPS, unitGraphModel.ancestors());
        put(GRAPH, unitGraphModel.graph());
        put(ORIGINATING_AGENCIES, unitGraphModel.originatingAgencies());
        put(UNITDEPTHS, unitGraphModel.unitDepths());

        put(MINDEPTH, unitGraphModel.minDepth());
        put(MAXDEPTH, unitGraphModel.maxDepth());

        put(GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted());
    }
}
