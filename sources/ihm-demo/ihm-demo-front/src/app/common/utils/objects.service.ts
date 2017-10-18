import { Injectable } from '@angular/core';

@Injectable()
export class ObjectsService {

  constructor() { }

  static clone(object) {
    return JSON.parse(JSON.stringify(object));
  }
}
