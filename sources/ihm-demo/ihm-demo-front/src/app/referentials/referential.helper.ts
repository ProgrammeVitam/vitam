import { Injectable } from '@angular/core';
import {DatePipe} from "@angular/common";
import {SelectItem} from "primeng/primeng";

@Injectable()
export class ReferentialHelper {

  constructor() { }

  useSwitchButton(key : string) {
    if (['Statut','Tous les services producteurs', 'Droit d\'écriture', 'Tous les usages'].indexOf(key) > -1) {
      return true;
    }
    return false;
  }

  useChips(key : string) {
    if (['Service Producteur'].indexOf(key) > -1) {
      return true;
    }
    return false;
  }

  useMultiSelect(key : string) {
    if (['Usage'].indexOf(key) > -1) {
      return true;
    }
    return false;
  }

  public selectionOptions = {
    'Usage': [
      {label: 'BinaryMaster', value: 'BinaryMaster'},
      {label: 'Dissemination', value: 'Dissemination'},
      {label: 'Thumbnail', value: 'Thumbnail'},
      {label: 'TextContent', value: 'TextContent'},
      {label: 'PhysicalMaster', value: 'PhysicalMaster'}
    ]
  };

  getOptions(field: string): any[] {
    return this.selectionOptions[field];
  }
}
