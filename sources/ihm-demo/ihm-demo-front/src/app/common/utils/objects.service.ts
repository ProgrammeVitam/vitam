import { Injectable } from '@angular/core';

@Injectable()
export class ObjectsService {

  constructor() { }

  static clone(object) {
    return JSON.parse(JSON.stringify(object));
  }

  static computeSize(size) {
    let units = [' octets', ' ko', ' Mo', ' Go', ' To'];
    let indice = 0;
    while (size >= 1000 || indice >=4) {
      size = Math.round(size/1000);
      indice++;
    }
    return size + units[indice];
  }

}
