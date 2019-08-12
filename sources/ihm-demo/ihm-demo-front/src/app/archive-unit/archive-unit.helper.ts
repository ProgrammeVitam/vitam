import { Injectable } from '@angular/core';
import {ObjectsService} from "../common/utils/objects.service";
import {DateService} from '../common/utils/date.service';

@Injectable()
export class ArchiveUnitHelper {


  private mustExclude = ['#id', 'StartDate', 'EndDate', 'Title', 'DescriptionLevel', 'Description',
    ['#management.SubmissionAgency'], ['#management.OriginatingAgency'], 'InheritedRules', 'ArchiveUnitProfile',
    ['#management.NeedAuthorization'] ];
  public storageFinalAction = {
    RestrictAccess: {id: 'RestrictAccess', label: 'Accès Restreint'},
    Transfer: {id: 'Transfer', label: 'Transférer'},
    Copy: {id: 'Copy', label: 'Copier'}
  };
  public appraisalFinalAction = {
    Keep: {id: 'Keep', label: 'Conserver'},
    Destroy: {id: 'Destroy', label: 'Détruire'}
  };
  public finalActionSelector = { // Make better an 'empty' choice ?
    'AppraisalRule': [
      {label: 'Conserver', value: 'Keep'},
      {label: 'Détruire', value: 'Destroy'}
    ], 'StorageRule': [
      {label: 'Accès Restreint', value: 'RestrictAccess'},
      {label: 'Transférer', value: 'Transfer'},
      {label: 'Copier', value: 'Copy'}
    ]
  };

  public ruleProperties = {
    AppraisalRule: {
      FinalAction: {
        label: 'Sort Final', kind: 'enum', choices: this.finalActionSelector.AppraisalRule, displayValue: (x) => this.appraisalFinalAction[x].label
      }
    },
    StorageRule: {
      FinalAction: {
        label: 'Sort Final', kind: 'enum', choices: this.finalActionSelector.StorageRule, displayValue: (x) => this.storageFinalAction[x].label
      }
    },
    ClassificationRule: {
      ClassificationOwner: {
        label: 'Service émetteur', kind: 'string', displayValue: (x) => x
      },
      ClassificationLevel: {
        label: 'Niveau de classification', kind: 'string', displayValue: (x) => x
      },
      ClassificationAudience: {
        label: 'Champ de diffusion', kind: 'string', displayValue: (x) => x
      },
      ClassificationReassessingDate: {
        label: 'Date de réévaluation', kind: 'date', displayValue: (x) => DateService.handleDate(x)
      },
      NeedReassessingAuthorization: {
        label: 'Modifications soumises à validation', kind: 'boolean', displayValue: (x) => x ? 'Oui' : 'Non'
      },
    }
  };

  public rulesCategories = [
    {
      rule: 'AccessRule', label: 'Délai de communicabilité', properties: {}
    }, {
      rule: 'AppraisalRule', label: 'Durée d\'utilité administrative',
      properties: this.ruleProperties.AppraisalRule
    }, {
      rule: 'ClassificationRule', label: 'Durée de classification',
      properties: this.ruleProperties.ClassificationRule
    },
    {
      rule: 'DisseminationRule', label: 'Délai de diffusion', properties: {}
    },
    {
      rule: 'ReuseRule', label: 'Durée de réutilisation', properties: {}
    },
    {
      rule: 'StorageRule', label: 'Durée d\'utilité courante',
      properties: this.ruleProperties.StorageRule
    }
  ];
  public textAreaFields = [
    'Description',
    'CustodialHistory.CustodialHistoryItem',
    'ControlSchema'
  ];
  public jsonSchemaFields = [
    'ControlSchema'
  ];
  public selectionFields = [
    'DescriptionLevel',
    '#unitType'
  ];
  public selectionOptions = {
    'DescriptionLevel': [
      {label: 'Fonds', value: 'Fonds'},
      {label: 'Sous-fonds', value: 'Subfonds'},
      {label: 'Classe', value: 'Class'},
      {label: 'Collection', value: 'Collection'},
      {label: 'Série', value: 'Series'},
      {label: 'Sous-série', value: 'Subseries'},
      {label: 'Groupe d\'articles', value: 'RecordGrp'},
      {label: 'Sous-groupe d\'articles', value: 'SubGrp'},
      {label: 'Dossier', value: 'File'},
      {label: 'Pièce', value: 'Item'}
    ],
    '#unitType': [
      {label: 'Standard', value: 'INGEST'},
      {label: 'Plan de classement', value: 'FILING_UNIT'},
      {label: 'Arbre de positionnement', value: 'HOLDING_UNIT'}
    ]
  };
  public unece = [{label: 'centimètre', value: 'CMT'},
    {label: 'centimètre', value: 'centimetre'},
    {label: 'micromètre', value: 'micrometre'},
    {label: 'micromètre', value: '4H'},
    {label: 'millimètre', value: 'millimetre'},
    {label: 'millimètre', value: 'MMT'},
    {label: 'mètre', value: 'metre'},
    {label: 'pouce', value: 'inch'},
    {label: 'pouce', value: 'INH'},
    {label: 'pied', value: 'foot'},
    {label: 'pied', value: 'FOT'},
    {label: 'microgramme', value: 'microgram'},
    {label: 'microgramme', value: 'MC'},
    {label: 'milligramme', value: 'milligram'},
    {label: 'milligramme', value: 'MGM'},
    {label: 'gramme', value: 'gram'},
    {label: 'gramme', value: 'GRM'},
    {label: 'kilogramme', value: 'kilogram'},
    {label: 'kilogramme', value: 'KGM'}];

  constructor() { }

  mustExcludeFields(field: string) {
    return this.mustExclude.indexOf(field) !== -1 || field.startsWith('_') || field.startsWith('#');
  }

  transformType(unitType: string) {
    switch (unitType) {
      case 'INGEST': return 'Standard';
      case 'FILING_UNIT': return 'Plan de classement';
      case 'HOLDING_UNIT': return 'Arbre de positionnement';
      default: return unitType;
    }
  }

  getDimensions(unitType: string): string  {
    let matchingOption = this.unece.find(option => option.value === unitType);
    if (matchingOption) {
      return matchingOption.label;
    } else {
      return unitType;
    }
  }

  getTitle(unitData: any) {
    if (unitData.Title !== undefined) {
      return unitData.Title;
    } else if (unitData.Title_ !== undefined && unitData.Title_.fr !== undefined) {
      return unitData.Title_.fr;
    }
    return '';
  }

  getStartDate(unitData: any) {
    if (unitData.DescriptionLevel && unitData.DescriptionLevel !== 'Item') {
      return unitData.StartDate;
    }

    let lowestDate = '';
    if (unitData.StartDate) {
      lowestDate = unitData.StartDate;
    }
    if (unitData.CreatedDate && (lowestDate === '' || lowestDate > unitData.CreatedDate)) {
      lowestDate = unitData.CreatedDate;
    }
    if (unitData.AcquiredDate && (lowestDate === '' || lowestDate > unitData.AcquiredDate)) {
      lowestDate = unitData.AcquiredDate;
    }
    if (unitData.SentDate && (lowestDate === '' || lowestDate > unitData.SentDate)) {
      lowestDate = unitData.SentDate;
    }
    if (unitData.ReceivedDate && (lowestDate === '' || lowestDate > unitData.ReceivedDate)) {
      lowestDate = unitData.ReceivedDate;
    }
    if (unitData.RegisteredDate && (lowestDate === '' || lowestDate > unitData.RegisteredDate)) {
      lowestDate = unitData.RegisteredDate;
    }
    if (unitData.TransactedDate && (lowestDate === '' || lowestDate > unitData.TransactedDate)) {
      lowestDate = unitData.TransactedDate;
    }
    return lowestDate;
  }

  getEndDate(unitData: any) {
    if (unitData.DescriptionLevel && unitData.DescriptionLevel !== 'Item') {
      return unitData.EndDate;
    }
    let highestDate = '';
    if (unitData.EndDate) {
      highestDate = unitData.EndDate;
    }
    if (unitData.CreatedDate && (highestDate === '' || highestDate < unitData.CreatedDate)) {
      highestDate = unitData.CreatedDate;
    }
    if (unitData.AcquiredDate && (highestDate === '' || highestDate < unitData.AcquiredDate)) {
      highestDate = unitData.AcquiredDate;
    }
    if (unitData.SentDate && (highestDate === '' || highestDate < unitData.SentDate)) {
      highestDate = unitData.SentDate;
    }
    if (unitData.ReceivedDate && (highestDate === '' || highestDate < unitData.ReceivedDate)) {
      highestDate = unitData.ReceivedDate;
    }
    if (unitData.RegisteredDate && (highestDate === '' || highestDate < unitData.RegisteredDate)) {
      highestDate = unitData.RegisteredDate;
    }
    if (unitData.TransactedDate && (highestDate === '' || highestDate < unitData.TransactedDate)) {
      highestDate = unitData.TransactedDate;
    }
    return highestDate;
  }

  isTextArea(field: string): boolean {
    return this.textAreaFields.indexOf(field) !== -1;
  }

  isSchemaJsonMode(field: string): boolean {
    return this.jsonSchemaFields.indexOf(field) !== -1;
  }

  isSelection(field: string): boolean {
    return this.selectionFields.indexOf(field) !== -1;
  }

  getOptions(field: string): any[] {
    return this.selectionOptions[field];
  }

  isText(field: string): boolean {
    return !this.isTextArea(field) && !this.isSelection(field);
  }

  personOrEntityGroup = {
    '@@': 'Entité',
    'Corpname': 'Nom de l\'entité',
    'Gender': 'Sexe',
    'Nationality': 'Nationalité',
    'BirthDate': 'Date de naissance',
    'DeathDate': 'Date de décès',
    'Identifier': 'Identifiant',
    'BirthName': 'Nom de naissance',
    'FirstName': 'Prénom',
    'GivenName': 'Nom d\'Usage',
    'Function': 'Fonction',
    'Activity': 'Activité',
    'Role': 'Droits',
    'Position': 'Intitulé du poste de travail',
    'BirthPlace': {
      '@@': 'Lieu de naissance',
      'Geogname': 'Nom géographique',
      'Address': 'Adresse',
      'PostalCode': 'Code postal',
      'City': 'Ville',
      'Region': 'Région',
      'Country': 'Pays'
    },
    'DeathPlace':{
      '@@': 'Lieu de décès',
      'Geogname': 'Nom géographique',
      'Address': 'Adresse',
      'PostalCode': 'Code postal',
      'City': 'Ville',
      'Region': 'Région',
      'Country': 'Pays'
    }
  };

  multiLang = {
    '@@': 'Champ',
    'fr': 'Français',
    'fre': 'Français',
    'en': 'Anglais',
    'eng': 'Anglais',
    'de': 'Allemand',
    'sp': 'Espagnol',
    'it': 'Italien',
  };

  getPersonOrEntityGroup(entityName: string) {
    let entity = ObjectsService.clone(this.personOrEntityGroup);
    entity['@@'] = entityName;
    return entity;
  }

  getFieldWithLang(fieldName: string) {
    let field = ObjectsService.clone(this.multiLang);
    field['@@'] = fieldName;
    return field;
  }

  getTranslationConstants() {
    return {
      'DescriptionLevel': 'Niveau de description',
      'Title': 'Titre',
      'FilePlanPosition': 'Position dans le plan de classement',
      'ID': 'Id',
      'OriginatingSystemId': 'Id système d\'origine',
      'SystemId': 'Identifiant système',
      'ArchivalAgencyArchiveUnitIdentifier': 'Id métier (Service d\'archives)',
      'OriginatingAgencyArchiveUnitIdentifier': 'Id métier (Service producteur)',
      'TransferringAgencyArchiveUnitIdentifier': 'Id métier (Serivce versant)',
      'Description': 'Description',
      'CustodialHistory': {
        '@@': 'Historique',
        'CustodialHistoryItem': 'Historique de propriété, de responsabilité et de conservation'
      },
      'Type': 'Type d\'information (Sens OAIS)',
      'DocumentType': 'Type de document',
      'Language': 'Langue des documents',
      'DescriptionLanguage': 'Langue des descriptions',
      'Status': 'Etat de l\'objet',
      'Version': 'Version',
      'Tag': 'Mot-clés',
      'Keyword': {
        '@@': 'Mot-clés',
        'KeywordContent': 'Valeur du mot-clé',
        'KeywordType': 'Type de mot-clé',
        'KeywordReference': 'Identifiant du mot clé',
      },
      'Coverage': {
        '@@': 'Autres métadonnées de couverture',
        'Spatial': 'Couverture géographique',
        'Temporal': 'Couverture temporelle',
        'Juridictional': 'Couverture administrative'
      },
      'OriginatingAgency': {
        '@@': 'Service producteur',
        'Identifier': 'Id Service producteur',
        'OrganizationDescriptiveMetadata': 'Nom du service producteur'
      },
      'SubmissionAgency': {
        '@@': 'Service versant',
        'Identifier': 'Id Service versant',
        'OrganizationDescriptiveMetadata': 'Nom du service versant'
      },
      'AuthorizedAgent': this.getPersonOrEntityGroup('Titulaire des droits de propriété intellectuelle'),
      'AuthorizedAgentGroup': {
        '@@': 'Titulaire(s) des droits de propriété intellectuelle',
        'AuthorizedAgent': this.getPersonOrEntityGroup('Titulaire des droits de propriété intellectuelle')
      },
      'Writer': this.getPersonOrEntityGroup('Rédacteur'),
      'WritingGroup': {
        '@@': 'Rédacteur(s)',
        'Writer': this.getPersonOrEntityGroup('Rédacteur'),
      },
      'Addressee': this.getPersonOrEntityGroup('Destinataire'),
      'Recipient': this.getPersonOrEntityGroup('Destinataire'),
      'AudienceGroup': {
        '@@': 'Audience(s) du document',
        'Addressee': this.getPersonOrEntityGroup('Destinataire'),
        'Recipient': this.getPersonOrEntityGroup('Destinataire'),
      },
      'AddresseeGroup': {
        '@@': 'Destinataire(s) pour action',
        'Addressee': this.getPersonOrEntityGroup('Destinataire'),
      },
      'RecipientGroup': {
        '@@': 'Destinataire(s) pour information',
        'Recipient': this.getPersonOrEntityGroup('Destinataire'),
      },
      'SignerGroup': {
        '@@': 'Signataire(s) ',
        'Signer': this.getPersonOrEntityGroup('Signataire'),
      },
      'ValidationGroup': {
        '@@': 'Validateur(s) de la signature',
        'Validator': this.getPersonOrEntityGroup('Validateur de la signature'),
      },

      'Source': 'Référence papier',
      'RelatedObjectReference': 'Référence à un objet',
      'CreatedDate': 'Date de création',
      'TransactedDate': 'Date de transaction',
      'AcquiredDate': 'Date de numérisation',
      'SentDate': 'Date d\'envoi',
      '#originating_agency': 'Service producteur de l\'entrée',
      '#originating_agencies': 'Service ayant des droits  sur l\'unité',
      'ReceivedDate': 'Date de réception',
      'RegisteredDate': 'Date d\'enregistrement',
      'StartDate': 'Date de début',
      'EndDate': 'Date de fin',
      'Event': {
        '@@': 'Evénement',
        'EventDateTime': 'Date et heure de l\'événement',
        'EventIdentifier': 'Identifiant de l\'événement',
        'EventType': 'Type d\'événement',
        'EventDetail': 'Détail de l\'événement'
      },
      'ArchiveUnitProfile': 'Profil d\'archivage',
      '#mgt': {
        'NeedAuthorization': 'Autorisation requise',
      },
      'Title_': this.getFieldWithLang('Titres'),
      'Description_': this.getFieldWithLang('Descriptions'),
      'Gps': {
        '@@': 'Coordonnées GPS',
        'GpsLatitude': 'Latitude',
        'GpsLongitude': 'Longitude',
        'GpsAltitude': 'Altitude'
      },
      'StorageRule.Rule': 'Règle d\'utilité courante (DUC)',
      'StorageRule.FinalAction': 'Action finale',
      'AppraisalRule.Rule': 'Règle d\'utilité administrative (DUA)',
      'AppraisalRule.FinalAction': 'Action finale',
      'AccessRule.Rule': 'Règle de communicabilité',
      'AccessRule.FinalAction': 'Action finale',
      'DisseminationRule.Rule': 'Règle de diffusion',
      'DisseminationRule.FinalAction': 'Action finale',
      'ReuseRule.Rule': 'Règle de réutilisation',
      'ReuseRule.FinalAction': 'Action finale',
      'ClassificationRule.Rule': 'Règle de classification',
      'ClassificationRule.FinalAction': 'Action finale',
      'ClassificationRule.ClassificationLevel': 'Niveau de classification',
      'ClassificationRule.ClassificationOwner': 'Émetteur de la classification'
    };
  }

  getObjectGroupTranslations() {
    return {
      '_id': 'Identifiant',
      '#id': 'Identifiant',
      'DataObjectGroupId': 'Identifiant du groupe d\'objets techniques',
      'DataObjectVersion': 'Usage',
      'MessageDigest': 'Empreinte',
      'OtherMetadata': 'Autres métadonnées',
      'Size': 'Taille (en octets)',
      'Algorithm': 'Algorithme',
      'FormatIdentification': 'Format',
      'FormatIdentification.FormatLitteral': 'Nom littéral',
      'FormatIdentification.MimeType': 'Type Mime',
      'FormatIdentification.FormatId': 'PUID du format',
      'FileInfo': 'Fichier',
      'FileInfo.Filename': 'Nom du fichier',
      'FileInfo.CreatingApplicationName': 'Nom de l\'application utilisée pour créer le fichier',
      'FileInfo.DateCreatedByApplication': 'Date de création par l\'application',
      'FileInfo.CreatingApplicationVersion': 'Version de l\'application utilisée pour créer le fichier',
      'FileInfo.CreatingOs': 'Système d\'exploitation utilisé pour créer le fichier',
      'FileInfo.CreatingOsVersion': 'Version du système d\'exploitation utilisé pour créer le fichier',
      'FileInfo.LastModified': 'Date de dernière modification',
      'FormatIdentification.Encoding': 'Encodage',
      'Metadata': 'Métadonnées',
      'Metadata.OtherMetadata': 'Autres métadonnées',
      'Metadata.Audio': 'Audio',
      'Metadata.Image': 'Image',
      'Metadata.Text':'Texte',
      '#storage': 'Stockage',
      '#storage.#nbc': 'Nombre de copies',
      '#storage.offerIds': 'offre de stockage',
      '#storage.strategyId': 'Identifiant de la stratégie de stockage',
      'PhysicalId': 'Identifiant d\'objet physique',
      'PhysicalDimensions': 'Dimensions physiques de l\'objet',
      'PhysicalDimensions.Shape': 'Forme',
      'PhysicalDimensions.NumberOfPage': 'Nombre de pages',
      'PhysicalDimensions.Width': 'Largeur',
      'PhysicalDimensions.Width.unit':'Unité',
      'PhysicalDimensions.Width.dValue':'Valeur',
      'PhysicalDimensions.Height': 'Hauteur',
      'PhysicalDimensions.Height.unit':'Unité',
      'PhysicalDimensions.Height.dValue':'Valeur',
      'PhysicalDimensions.Depth': 'Profondeur',
      'PhysicalDimensions.Depth.unit':'Unité',
      'PhysicalDimensions.Depth.dValue':'Valeur',
      'PhysicalDimensions.Diameter': 'Diamètre',
      'PhysicalDimensions.Diameter.unit':'Unité',
      'PhysicalDimensions.Diameter.dValue':'Valeur',
      'PhysicalDimensions.Length': 'Longueur',
      'PhysicalDimensions.Length.unit':'Unité',
      'PhysicalDimensions.Length.dValue':'Valeur',
      'PhysicalDimensions.Thickness': 'Epaisseur',
      'PhysicalDimensions.Thickness.unit':'Unité',
      'PhysicalDimensions.Thickness.dValue':'Valeur',
      'PhysicalDimensions.Weight': 'Poids',
      'PhysicalDimensions.Weight.unit':'Unité',
      'PhysicalDimensions.Weight.dValue':'Valeur'
    };
  }
}
