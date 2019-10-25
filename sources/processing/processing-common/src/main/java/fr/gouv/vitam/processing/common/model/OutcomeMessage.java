/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.processing.common.model;

import fr.gouv.vitam.common.model.VitamConstants;

/**
 * Enum StatusCode
 *
 * different constants status code for workflow , action handler and process
 *
 */
public enum OutcomeMessage {
    /**
     * OK : success message
     */
    CHECK_CONFORMITY_OK("Contrôle de conformité des objets réalisé avec succès"),

    /**
     * KO : fail message
     */
    CHECK_CONFORMITY_KO("Erreur de contrôle de conformité des objets"),

    /**
     * OK : success message
     */
    CHECK_OBJECT_NUMBER_OK("Contrôle du nombre des objets réalisé avec succès"),

    /**
     * KO : fail message
     */
    CHECK_OBJECT_NUMBER_KO("Erreur de contrôle du nombre des objets"),

    /**
     * OK : success message
     */
    CHECK_VERSION_OK("Contrôle des versions réalisé avec succès"),

    /**
     * KO : fail message
     */
    CHECK_VERSION_KO("Erreur de contrôle des versions"),

    /**
     * OK : success message
     */
    CHECK_MANIFEST_OK("Contrôle du bordereau réalisé avec succès"),

    /**
     * KO : fail message
     */
    CHECK_MANIFEST_KO("Erreur de contrôle du bordereau"),

    /**
     * KO : fail message no manifest file in the SIP
     */
    CHECK_MANIFEST_NO_FILE("Absence du bordereau"),

    /**
     * KO : fail message, manifest is not an XML file
     */
    CHECK_MANIFEST_NOT_XML_FILE("Bordereau au mauvais format"),

    /**
     * KO : fail message, manifest is not a valid SEDA file
     */
    CHECK_MANIFEST_NOT_XSD_VALID("Bordereau non conforme au schéma SEDA "+ VitamConstants.SEDA_CURRENT_VERSION),

    /**
     * OK : success message
     */
    EXTRACT_MANIFEST_OK("Extraction du bordereau réalisé avec succès"),

    /**
     * KO : fail message
     */
    EXTRACT_MANIFEST_KO("Erreur de l'extraction du bordereau"),

    /**
     * OK : success message
     */
    INDEX_UNIT_OK("Index unit réalisé avec succès"),

    /**
     * KO : fail message
     */
    INDEX_UNIT_KO("Erreur de l'index unit"),

    /**
     * OK : success message
     */
    INDEX_OBJECT_GROUP_OK("Index objectgroup réalisé avec succès"),

    /**
     * KO : fail message
     */
    INDEX_OBJECT_GROUP_KO("Erreur de l'index objectgroup"),

    /**
     * KO : fail message
     */
    STORAGE_OFFER_KO_UNAVAILABLE("Offre de stockage non disponible"),

    /**
     * KO : fail message
     */
    STORAGE_OFFER_SPACE_KO("Disponibilité de l'offre de stockage insuffisante"),

    /**
     * OK : success message
     */
    STORAGE_OFFER_SPACE_OK("Succès de la vérification de la disponibilité de l’offre de stockage"),


    /**
     * KO logbook lifecycle
     */
    LOGBOOK_COMMIT_KO("Erreur lors de l'enregistrement du journal du cycle de vie"),

    /**
     * Create logbook lifecycle
     */
    CREATE_LOGBOOK_LIFECYCLE("Création du journal du cycle de vie"),

    /**
     * Create logbookLifecycle ok
     */
    CREATE_LOGBOOK_LIFECYCLE_OK("Journal du cycle de vie créé avec succès"),

    /**
     * update logbooklifecycle KO
     */
    UPDATE_LOGBOOK_LIFECYCLE_KO("Erreur lors de la mise à jour du journal du cycle de vie"),

    /**
     * Check BDO
     */
    CHECK_BDO("Vérification de l'empreinte de l'objet"),

    /**
     * Check BDO
     */
    CHECK_BDO_OK("Empreinte de l'objet vérifié avec succès"),

    /**
     * Check BDO
     */
    CHECK_BDO_KO("Echec de la vérification de l'emprunte de l'objet"),

    /**
     * Check Digest
     */
    CHECK_DIGEST("Digest Check, Vérification de l'empreinte des objets"),

    /**
     * Check Digest OK
     */
    CHECK_DIGEST_OK("Succès de la vérification de l'empreinte"),

    /**
     * Check Digest KO
     */
    CHECK_DIGEST_KO("Échec de la vérification de l'empreinte"),

    /**
     * Check Digest Start
     */
    CHECK_DIGEST_STARTED("Début de la vérification de l'empreinte"),

    /**
     * File Format KO
     */
    FILE_FORMAT_KO("Echec de la vérification des formats"),

    /**
     * File Format OK
     */
    FILE_FORMAT_OK("Succès de la vérification des formats"),

    /**
     * File format not found in tool
     */
    FILE_FORMAT_NOT_FOUND("Format de l’objet non identifié"),

    /**
     * File format PUID not found into internal referential
     */
    FILE_FORMAT_PUID_NOT_FOUND("Identification du format de l’objet (PUID) absente dans le référentiel interne"),

    /**
     * File format data update
     */
    FILE_FORMAT_METADATA_UPDATE("Complétion des métadonnées sur les formats"),

    /**
     * File format not found in Vitam referential
     */
    FILE_FORMAT_NOT_FOUND_REFERENTIAL("Le format de fichier n'a pas été trouvé dans le reférentiel Vitam"),

    /**
     * File format referentiel search error
     */
    FILE_FORMAT_REFERENTIAL_ERROR("Une erreur est survenue lors de la recherche du format de fichier dans le " +
        "réferentiel Vitam"),

    /**
     * File format object wrong file path
     */
    FILE_FORMAT_OBJECT_NOT_FOUND("L'objet à analyser n'a pas été trouvé"),

    /**
     * File format technical error
     */
    FILE_FORMAT_TECHNICAL_ERROR("Un erreur technique est survenue lors de l'analyse du format de fichier"),

    /**
     * File format tool does not respond
     */
    FILE_FORMAT_TOOL_DOES_NOT_ANSWER("L'outil d'analyse des formats de fichier ne répond pas"),

    /**
     * Workflow ingest OK : success message
     */
    WORKFLOW_INGEST_OK("Entrée effectuée avec succès"),

    /**
     * Workflow ingest KO/FATAL : fail message
     */
    WORKFLOW_INGEST_KO("Entrée en échec"),

    /**
     * Store object OK : success message
     */
    STORE_OBJECT_OK("Succès du rangement des Objets"),

    /**
     * Store object KO/FATAL : fail message
     */
    STORE_OBJECT_KO("Echec du rangement des Objets"),

    /**
     * Getting format identifier failed message (FATAL)
     */
    GETTING_FORMAT_IDENTIFIER_FATAL("L'outil d'analyse de format de fichier n'a pu être initialisé"),

    /**
     * ART KO : fail message
     */
    FUND_REGISTER_OK("Succès d'enregistrement des archives prises en charge dans le registre des fonds"),

    /**
     * ART OK : succes message
     */
    FUND_REGISTER_KO("Echec d'enregistrement des archives prises en charge dans le registre des fonds"),

    /**
     * ART KO : fail message
     */
    ATR_KO("Erreur de Notification ATR"),

    /**
     * ART OK : succes message
     */
    ATR_OK("Notification envoyée");


    private String value;

    private OutcomeMessage(String value) {
        this.value = value;
    }

    /**
     * value
     *
     * @return : value of status code
     */
    public String value() {
        return value;
    }

}
