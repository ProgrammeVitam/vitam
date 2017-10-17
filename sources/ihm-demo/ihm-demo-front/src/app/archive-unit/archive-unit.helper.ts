import { Injectable } from '@angular/core';
import {DatePipe} from "@angular/common";
import {SelectItem} from "primeng/primeng";

@Injectable()
export class ArchiveUnitHelper {
  private mustExclude = ['#id', 'StartDate', 'EndDate', 'Title', 'DescriptionLevel', 'Description',
    ['#management.SubmissionAgency'], ['#management.OriginatingAgency'], 'inheritedRule'];
  public rulesCategories = [
    {rule: 'AccessRule', label: 'Délai de communicabilité'},
    {rule: 'AppraisalRule', label: 'Durée d\'utilité Administrative'},
    {rule: 'ClassificationRule', label: 'Durée de classification'},
    {rule: 'DisseminationRule', label: 'Délai de diffusion'},
    {rule: 'ReuseRule', label: 'Durée de réutilisation'},
    {rule: 'StorageRule', label: 'Durée d\'utilité courante'}
  ];
  public storageFinalAction = {
    RestrictAccess: {id: 'RestrictAccess', label: 'Accès Restreint'},
    Transfer: {id: 'Transfer', label: 'Transférer'},
    Copy: {id: 'Copy', label: 'Copier'}
  };
  public appraisalFinalAction = {
    Keep: {id: 'Keep', label: 'Conserver'},
    Destroy: {id: 'Destroy', label: 'Détruire'}
  };
  public textAreaFields = [
    'Description',
    'CustodialHistory.CustodialHistoryItem'
  ];
  public selectionFields = [
    'DescriptionLevel',
    '#unitType'
  ];
  public selectionOptions = {
    'DescriptionLevel': [
      {label: 'Fonds', value: 'Fonds'},
      {label: 'Subfonds', value: 'Subfonds'},
      {label: 'Class', value: 'Class'},
      {label: 'Collection', value: 'Collection'},
      {label: 'Series', value: 'Series'},
      {label: 'Subseries', value: 'Subseries'},
      {label: 'RecordGrp', value: 'RecordGrp'},
      {label: 'SubGrp', value: 'SubGrp'},
      {label: 'File', value: 'File'},
      {label: 'Item', value: 'Item'}
    ],
    '#unitType': [
      {label: 'Standard', value: 'INGEST'},
      {label: 'Plan de classement', value: 'FILING_UNIT'},
      {label: 'Arbre de positionnement', value: 'HOLDING_UNIT'}
    ]
  };

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

  getStartDate(unitData: any) {
    if (unitData.DescriptionLevel !== 'Item') {
      return unitData.StartDate;
    }
    let lowestDate = '';
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
    if (unitData.DescriptionLevel !== 'Item') {
      return unitData.EndDate;
    }
    let lowestDate = '';
    if (unitData.CreatedDate && (lowestDate === '' || lowestDate < unitData.CreatedDate)) {
      lowestDate = unitData.CreatedDate;
    }
    if (unitData.AcquiredDate && (lowestDate === '' || lowestDate < unitData.AcquiredDate)) {
      lowestDate = unitData.AcquiredDate;
    }
    if (unitData.SentDate && (lowestDate === '' || lowestDate < unitData.SentDate)) {
      lowestDate = unitData.SentDate;
    }
    if (unitData.ReceivedDate && (lowestDate === '' || lowestDate < unitData.ReceivedDate)) {
      lowestDate = unitData.ReceivedDate;
    }
    if (unitData.RegisteredDate && (lowestDate === '' || lowestDate < unitData.RegisteredDate)) {
      lowestDate = unitData.RegisteredDate;
    }
    if (unitData.TransactedDate && (lowestDate === '' || lowestDate < unitData.TransactedDate)) {
      lowestDate = unitData.TransactedDate;
    }
    return lowestDate;
  }

  // TODO Move me in some utils class ?
  handleDate(date): string {
    return new DatePipe('en-US').transform(date, 'dd/MM/yyyy');
  }

  isTextArea(field: string): boolean {
    return this.textAreaFields.indexOf(field) !== -1;
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

  getTranslationConstants() {
    return {
      'DescriptionLevel': 'Niveau de description',
      'Title': 'Titre',
      'FilePlanPosition': 'Position dans le plan de classement',
      'ID': 'Id',
      'OriginatingSystemId': 'Id système d\'origine',
      'ArchivalAgencyArchiveUnitIdentifier': 'Id métier (Service d\'archives)',
      'OriginatingAgencyArchiveUnitIdentifier': 'Id métier (Service producteur)',
      'TransferringAgencyArchiveUnitIdentifier': 'Id métier (Serivce versant)',
      'Description': 'Description',
      'CustodialHistory': 'Historique',
      'CustodialHistory.CustodialHistoryItem': 'Historique de propriété, de responsabilité et de conservation',
      'Type': 'Type d\'information (Sens OAIS)',
      'DocumentType': 'Type de document',
      'Language': 'Langue des documents',
      'DescriptionLanguage': 'Langue des descriptions',
      'Status': 'Etat de l\'objet',
      'Version': 'Version',
      'Tag': 'Mot-clés',
      'Keyword': 'Mot-clés',
      'Keyword.KeywordContent': 'Valeur du mot-clé',
      'Keyword.KeywordType': 'Type de mot-clé',
      'Coverage.Spatial': 'Couverture geographique',
      'Coverage.Temporal': 'Couverture temporelle',
      'Coverage.Juridictional': 'Couverture administrative',
      'OriginatingAgency.Identifier': 'Id Service producteur',
      'OriginatingAgency.OrganizationDescriptiveMetadata': 'Nom du service producteur',
      'SubmissionAgency.Identifier': 'Id Service versant',
      'SubmissionAgency.OrganizationDescriptiveMetadata': 'Nom du service versant',
      'AuthorizedAgent.Corpname': 'Nom du Titulaire des droits de propriété intellectuellle',
      'Writer': 'Rédacteur',
      'Writer.FirstName': 'Prénom du rédacteur',
      'Writer.BirthName': 'Nom du rédacteur',
      'Writer.Identifier': 'Identifiant du rédacteur',
      'Addressee': 'Destinataire',
      'Addressee.FirstName': 'Prénom du destinataire',
      'Addressee.BirthName': 'Nom du destinataire',
      'Addressee.Identifier': 'Identifiant du destinataire',
      'Addressee.Corpname': 'Nom du destinataire',
      'Recipient': 'Destinataire',
      'Recipient.FirstName': 'Prénom du destinataire pour information',
      'Recipient.BirthName': 'Nom du destinataire pour information',
      'Source': 'Référence papier',
      'RelatedObjectReference': 'Référence à un objet',
      'CreatedDate': 'Date de création',
      'TransactedDate': 'Date de transaction',
      'AcquiredDate': 'Date de numérisation',
      'SentDate': 'Date d\'envoi',
      '#originating_agency': 'Service producteur de l\'entrée',
      '#originating_agencies': 'Service ayant des droits  sur l\'unité',
      'ReceivedDate': 'Date de reception',
      'RegisteredDate': 'Date d\'enregistrement',
      'StartDate': 'Date de début',
      'EndDate': 'Date de fin',
      'Event.EventDateTime': 'Date et heure de l\'évènement',
      'ArchiveUnitProfile': 'Profil d\'archivage',
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
      'ClassificationRule.ClassificationOwner': 'Émetteur de la classification',
      '#mgt.NeedAuthorization': 'Autorisation requise',
      'Titles': 'Titres',
      'Titles.fr': 'Français',
      'Titles.fre': 'Français',
      'Titles.en': 'Anglais',
      'Titles.eng': 'Anglais',
      'Titles.de': 'Allemand',
      'Titles.sp': 'Espagnol',
      'Titles.it': 'Italien',
      'Descriptions': 'Descriptions',
      'Descriptions.fr': 'Français',
      'Descriptions.fre': 'Français',
      'Descriptions.en': 'Anglais',
      'Descriptions.eng': 'Anglais',
      'Descriptions.de': 'Allemand',
      'Descriptions.sp': 'Espagnol',
      'Descriptions.it': 'Italien',
      'Gps': 'Coordonnées GPS',
      'Gps.GpsLatitude': 'Latitude',
      'Gps.GpsLongitude': 'Longitude',
      'Gps.GpsAltitude': 'Altitude'
    };
  }

  getObjectGroupTranslations() {
    return {
      '_id': 'Identifiant',
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
      'Metadata.Text':'Texte',
      '_storage': 'Stockage',
      '_storage._nbc': 'Nombre de copies',
      '_storage.offerIds': 'offre de stockage',
      '_storage.strategyId': 'Stratégie de stockage'
    };
  }
}
