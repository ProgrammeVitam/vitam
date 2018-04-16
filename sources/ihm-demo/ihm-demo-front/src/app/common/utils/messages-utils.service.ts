import { Injectable } from '@angular/core';

@Injectable()
export class MessagesUtilsService {

  public messagesList = {
    '020121': 'Le format du fichier ne correspond pas au format attendu.',
    '020135': 'Au moins un objet déclare une valeur incorrecte.',
    'PRECONDITION_FAILED': 'Le format du fichier ne correspond pas au format attendu.'
  };

  constructor() { }

  getMessage(response: any) {
    try {
      const errorJson = JSON.parse(response.error);
      if (errorJson && errorJson.code) {
        return this.messagesList[errorJson.code] ? this.messagesList[errorJson.code] : '';
      }
    } catch (e) {
      console.warn("Unparsable error ", e);
      return '';
    }
    return '';
  }
}
