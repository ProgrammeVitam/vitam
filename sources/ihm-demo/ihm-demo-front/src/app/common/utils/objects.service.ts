import {Injectable} from '@angular/core';

@Injectable()
export class ObjectsService {

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

  /**
   * Transform the object in an array of object where object properties become array element.
   * For Example, transform {a: value, b: value} into [{a: value}, {b: value}]
   *
   * @param object initial object
   * @returns {any[]} array of object properties
   */
  static objectToArray(object: any): any[] {
    return Object.keys(object).map(x => {
      let item = {};
      item[x] = object[x];
      return item;
    });
  }

  static isSameArray(array1: string[], array2: string[]): boolean {
    if (!array1 && !array2) { return true }
    if (!array1 || !array2) { return false }
    if (array1.length !== array2.length) { return false }

    const sortedArray1 = [...array1].sort();
    const sortedArray2 = [...array2].sort();
    return sortedArray1.sort().every((value, index) => value === sortedArray2[index] );
  }

  constructor() {
  }

}
