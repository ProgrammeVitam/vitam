import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { IntervalObservable } from "rxjs/observable/IntervalObservable";
import { NavigationStart, Router } from '@angular/router';
import { Observable } from "rxjs";

import { ResourcesService } from '../../resources.service';
import { UploadService, ingestStatusElement } from '../upload.service';


@Component({
  selector: 'vitam-upload-referentials',
  templateUrl: './upload-referentials.component.html',
  styleUrls: ['./upload-referentials.component.css']
})

export class UploadReferentialsComponent implements OnInit {

  fileUpload: File;
  fileName : string;
  contextId : string;
  action = 'RESUME';
  displayDialog = false;
  displayUploadMessage = false;
  importError = false;

  @Input() uploadType: string;
  @Input() url: string;
  @Input() importSucessMsg: string;
  @Input() importErrorMsg: string;
  @Input() uploadAPI: string;
  @Input() extensions: string[];

  constructor(private uploadService : UploadService, private router: Router) {
    this.router.events.subscribe(event => {
      if(event instanceof NavigationStart) {
        delete this.fileName;
        delete this.fileUpload;
      }
    });
  }

  ngOnInit() {
    this.contextId = this.uploadType;
    if (!this.extensions) {
      this.extensions = ["tar", "zip"];
    }
  }

  ngOnDestroy() {
  }

  checkFileExtension(fileName : string) : boolean {
    this.fileName = fileName;
    let extension = fileName.split('.').pop();
    if (this.extensions.indexOf(extension) >= 0) {
      return true;
    } else {
      this.displayDialog = true;
      return false;
    }
  }

  onFileDrop(file) {
    if (!this.checkFileExtension(file.name)) {
      return;
    }
    this.fileUpload = file;
  }

  onChange(file) {
    if (!this.checkFileExtension(file[0].name)) {
      return;
    }
    this.fileUpload = file[0];
  }

  uploadReferential() {
    this.uploadService.uploadReferentials(this.uploadAPI, this.fileUpload).subscribe((response) => {
      this.importError = false;
      this.displayUploadMessage = true;
    }, () => {
      this.importError = true;
      this.displayUploadMessage = true;
    });
  }

}
