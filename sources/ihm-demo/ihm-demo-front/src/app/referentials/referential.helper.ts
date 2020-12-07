import {Injectable} from '@angular/core';
import {DatePipe} from '@angular/common';
import {SelectItem} from 'primeng/primeng';

@Injectable()
export class ReferentialHelper {

  static optionLists = {
    DataObjectVersion: ['BinaryMaster', 'Dissemination', 'Thumbnail', 'TextContent', 'PhysicalMaster'],
    RuleCategoryToFilter: ['AppraisalRule', 'AccessRule', 'StorageRule', 'DisseminationRule', 'ClassificationRule', 'ReuseRule']
  };

  static preservationLists ={
    DataObjectVersion: [
      {label: '', value: undefined},
      {label: 'Original numérique', value: 'BinaryMaster'},
      {label: 'Diffusion', value: 'Dissemination'},
      {label: 'Vignette', value: 'Thumbnail'},
      {label: 'Contenu brut', value: 'TextContent'}]
  }

  constructor() {
  }

  useSwitchButton(key: string) {
    return ['Status', 'EveryDataObjectVersion', 'WritingPermission', 'EveryOriginatingAgency', 'EveryFormatType'].indexOf(key) > -1;
  }

  useChips(key: string) {
    return ['OriginatingAgencies', 'ArchiveProfiles', 'RootUnits', 'ExcludedRootUnits', 'FormatType'].indexOf(key) > -1;
  }

  useMultiSelect(key: string) {
    return 'DataObjectVersion' === key || 'RuleCategoryToFilter' === key;
  }

  public selectionOptions = {
    DataObjectVersion: [
      {label: 'Original numérique', value: 'BinaryMaster'},
      {label: 'Diffusion', value: 'Dissemination'},
      {label: 'Vignette', value: 'Thumbnail'},
      {label: 'Contenu brut', value: 'TextContent'},
      {label: 'Original papier', value: 'PhysicalMaster'}
    ],
    RuleCategoryToFilter: [
      {label: 'Durée d\'utilité administrative', value: 'AppraisalRule'},
      {label: 'Durée d\'utilité courante', value: 'StorageRule'},
      {label: 'Durée de classification', value: 'ClassificationRule'},
      {label: 'Délai de communicabilité', value: 'AccessRule'},
      {label: 'Délai de diffusion', value: 'DisseminationRule'},
      {label: 'Durée de réutilisation', value: 'ReuseRule'},
      {label: 'Durée de gel', value: 'HoldRule'}
    ]
  };

  getOptions(field: string, filter?: string[]): any[] {
    if (filter && filter.length > 0) {
      return this.selectionOptions[field].filter(obj => filter.includes(obj.value));
    } else {
      return this.selectionOptions[field];
    }
  }
  getGriffinTranslations() {
    return {
      'Identifier': 'Identifiant',
      'Name': 'Intitulé',
      'Description': 'Description',
      'CreationDate': 'Date de création',
      'LastUpdate': 'Dernière modification',
      'ExecutableName': 'Nom de l’outil\n',
      'ExecutableVersion': 'Version de l’outil'
    }
  }

  getScenarioTranslations() {
    return {
      'Identifier': 'Identifiant',
      'Name': 'Intitulé',
      'Description': 'Description',
      'CreationDate': 'Date de création',
      'LastUpdate': 'Dernière modification',
      'ActionList': 'Action(s) couverte(s)',
      'DefaultGriffin': 'Action par défaut',
      'GriffinByFormat': 'Action(s) à réaliser',
      'GriffinByFormat.FormatList': 'Format(s) concerné(s)',
      'GriffinByFormat.GriffinIdentifier':'Identifiant du griffon à exécuter',
      'DefaultGriffin.GriffinIdentifier':'Identifiant du griffon à exécuter',
      'GriffinByFormat.Timeout' :'Temps maximal de traitement',
      'DefaultGriffin.Timeout' :'Temps maximal de traitement',
      'GriffinByFormat.MaxSize' : 'Taille maximale des objets',
      'DefaultGriffin.MaxSize' : 'Taille maximale des objets',
      'GriffinByFormat.Debug'  :'Debug',
      'DefaultGriffin.Debug'  :'Debug',
      'GriffinByFormat.ActionDetail'  :'Commande(s)',
      'DefaultGriffin.ActionDetail'  :'Commande(s)',
      'GriffinByFormat.ActionDetail.Type'  :'Type',
      'DefaultGriffin.ActionDetail.Type'  :'Type',
      'GriffinByFormat.ActionDetail.Values'  :'Valeur(s)',
      'DefaultGriffin.ActionDetail.Values'  :'Valeur(s)',
      'DefaultGriffin.ActionDetail.Values.Args'  :'Argument(s)',
      'GriffinByFormat.ActionDetail.Values.Args'  :'Argument(s)',
      'GriffinByFormat.ActionDetail.Values.FilteredExtractedUnitData'  :'FilteredExtractedUnitData',
      'GriffinByFormat.ActionDetail.Values.FilteredExtractedObjectGroupData'  :'FilteredExtractedObjectGroupData',
      'DefaultGriffin.ActionDetail.Values.FilteredExtractedUnitData'  :'FilteredExtractedUnitData',
      'DefaultGriffin.ActionDetail.Values.FilteredExtractedObjectGroupData'  :'FilteredExtractedObjectGroupData'
    };
  }
}
