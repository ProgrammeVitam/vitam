import {Injectable} from '@angular/core';

@Injectable()
export class ObjectsService {

  constructor() {
  }

  static clone(object) {
    if (object === undefined) {
      return undefined;
    }
    return JSON.parse(JSON.stringify(object));
  }

  static computeSize(size) {
    let units = [' octets', ' ko', ' Mo', ' Go', ' To'];
    let indice = 0;
    while (size >= 1000 || indice >= 4) {
      size = Math.round(size / 1000);
      indice++;
    }
    return size + units[indice];
  }

  static stringify(value) {
    if (value === undefined) {
      return '';
    }
    return String(value);
  }

  static pushAllWithoutDuplication(array: string[], otherArray: string[]) {
    if (array) {
      for (let i = array.length - 1; i >= 0; i--) {
        let item = array[i];
        if (otherArray.indexOf(item) === -1) {
          array.splice(i, 1);
        }
      }
    }
    if (otherArray) {
      for (let otherItem of otherArray) {
        if (array.indexOf(otherItem) === -1) {
          array.push(otherItem);
        }
      }
    }
  }

}
