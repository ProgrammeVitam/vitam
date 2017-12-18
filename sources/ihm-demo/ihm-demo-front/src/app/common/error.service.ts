import { Injectable } from '@angular/core';
import {DialogService} from "./dialog/dialog.service";
import {Router} from "@angular/router";

@Injectable()
export class ErrorService {

  constructor(public dialogService: DialogService, public router: Router) { }

  handle404Error(error) {
    if (error.status === 404) {
      this.dialogService.displayMessage(
        'La page à laquelle vous souhaitez accéder n\'existe pas. Vous avez été redirigé vers la page d\'accueil.',
        'Page non disponible');
      this.router.navigate(['ingest', 'sip']);
    }
  }
}
