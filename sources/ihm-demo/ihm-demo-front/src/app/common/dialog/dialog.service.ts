import { Injectable } from '@angular/core';
import { BehaviorSubject } from "rxjs/BehaviorSubject"

@Injectable()
export class DialogService {

  private dialogState = new BehaviorSubject<VitamDialog>({
    message : '',
    header: '',
    isDisplay : false
  });

  constructor() {
  }

  displayMessage(message : string, header : string ) {
    this.dialogState.next({
      message : message,
      header : header,
      isDisplay : true
    });
  }


  getdialogState(): BehaviorSubject<VitamDialog> {
    return this.dialogState;
  }

}

export class VitamDialog {
  message : string;
  header : string;
  isDisplay : boolean;
}
