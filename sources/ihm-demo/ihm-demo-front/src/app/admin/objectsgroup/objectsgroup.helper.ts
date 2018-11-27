import { Injectable } from '@angular/core';
import {ObjectsService} from "../../common/utils/objects.service";

@Injectable()
export class ObjectsGroupHelper {

getFormat(unitData: any) {
    const qualifiers = unitData["#qualifiers"];
    if(qualifiers && qualifiers.length > 0 && qualifiers[0].versions
      && qualifiers[0].versions.length > 0 && qualifiers[0].versions[0].FormatIdentification) {
      return qualifiers[0].versions[0].FormatIdentification.FormatId;
    } else {
      return "";
    }
  }

  getUsage(unitData: any) {
    const qualifiers = unitData["#qualifiers"];
    if(qualifiers && qualifiers.length > 0 && qualifiers[0].versions
      && qualifiers[0].versions.length > 0) {
      return qualifiers[0].versions[0].DataObjectVersion;
    } else {
      return "";
    }
  }

  getSize(unitData: any) {
    const qualifiers = unitData["#qualifiers"];
    if(qualifiers && qualifiers.length > 0 && qualifiers[0].versions && qualifiers[0].versions.length > 0) {
      return qualifiers[0].versions[0].Size;
    } else {
      return "";
    }
  }

  getAuTitles(unitData: any) {
    let titles = "";
    const unitsTitle = unitData["UnitsTitle"];
    if(unitsTitle && unitsTitle.length > 0) {
      for(const title of unitsTitle) {
        titles = titles.concat(title, " | ");
      }
      titles = titles.substr(0, titles.length-3);
    }
    return titles;
  }
}