import { Injectable } from '@angular/core';
import {DatePipe} from '@angular/common';
import {SelectItem} from 'primeng/primeng';

@Injectable()
export class ReferentialHelper {

  static optionLists = {
    'DataObjectVersion': ['BinaryMaster', 'Dissemination', 'Thumbnail', 'TextContent', 'PhysicalMaster']
  };

  constructor() { }

  useSwitchButton(key : string) {
    return ['Status', 'EveryDataObjectVersion', 'WritingPermission', 'EveryOriginatingAgency', 'EveryFormatType'].indexOf(key) > -1;
  }

  useChips(key : string) {
    return ['OriginatingAgencies', 'ArchiveProfiles', 'RootUnits', 'ExcludedRootUnits', 'FormatType'].indexOf(key) > -1;
  }

  useMultiSelect(key : string) {
    return ['DataObjectVersion'].indexOf(key) > -1;
  }

  public selectionOptions = {
    'DataObjectVersion': [
      {label: 'Original numérique', value: 'BinaryMaster'},
      {label: 'Diffusion', value: 'Dissemination'},
      {label: 'Vignette', value: 'Thumbnail'},
      {label: 'Contenu brut', value: 'TextContent'},
      {label: 'Original papier', value: 'PhysicalMaster'}
    ]
  };

  getOptions(field: string): any[] {
    return this.selectionOptions[field];
  }
}
