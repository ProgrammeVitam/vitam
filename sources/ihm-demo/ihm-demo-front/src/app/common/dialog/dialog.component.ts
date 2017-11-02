import { Component, OnInit } from '@angular/core';

import { DialogService } from './dialog.service';

@Component({
  selector: 'vitam-dialog',
  templateUrl: './dialog.component.html',
  styleUrls: ['./dialog.component.css']
})
export class DialogComponent implements OnInit {

  isDisplay = false;
  dialogMessage = '';
  dialogHeader = '';

  constructor(private dialogService : DialogService) { }

  ngOnInit() {
    this.dialogService.getdialogState().subscribe((value) => {
      if (value) {
        this.dialogHeader = value.header;
        this.dialogMessage = value.message;
        this.isDisplay = value.isDisplay;
      }
    });
  }

}
