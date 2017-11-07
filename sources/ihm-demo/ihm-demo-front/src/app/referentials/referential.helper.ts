import { Injectable } from '@angular/core';
import {DatePipe} from "@angular/common";
import {SelectItem} from "primeng/primeng";

@Injectable()
export class ReferentialHelper {

  constructor() { }

  useSwitchButton(key : string) {
    if (['Status','EveryDataObjectVersion', 'WritingPermission', 'EveryOriginatingAgency'].indexOf(key) > -1) {
      return true;
    }
    return false;
  }

  useChips(key : string) {
    if (['OriginatingAgencies', 'ArchiveProfiles', 'RootUnits'].indexOf(key) > -1) {
      return true;
    }
    return false;
  }

  useMultiSelect(key : string) {
    if (['DataObjectVersion'].indexOf(key) > -1) {
      return true;
    }
    return false;
  }

  public selectionOptions = {
    'DataObjectVersion': [
      {label: 'BinaryMaster', value: 'Original numérique'},
      {label: 'Dissemination', value: 'Diffusion'},
      {label: 'Thumbnail', value: 'Vignette'},
      {label: 'TextContent', value: 'Contenu brut'},
      {label: 'PhysicalMaster', value: 'Original papier'}
    ]
  };

  getOptions(field: string): any[] {
    return this.selectionOptions[field];
  }
}
