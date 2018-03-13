import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { IntervalObservable } from 'rxjs/observable/IntervalObservable';
import { NavigationStart, Router } from '@angular/router';
import { Observable, Subscription } from 'rxjs/Rx';

import { ResourcesService } from '../../resources.service';
import { UploadService, ingestStatusElement } from '../upload.service';
import { AuthenticationService } from '../../../authentication/authentication.service';


@Component({
  selector: 'vitam-upload',
  templateUrl: './upload-sip.component.html',
  styleUrls: ['./upload-sip.component.css']
})

export class UploadSipComponent implements OnInit {

  fileUpload: File;
  fileName: string;
  uploadState: Subscription;
  uploadInProgess = false;
  ingestInProgess = false;
  contextId: string;
  action = 'RESUME';
  uploadProgress: number;
  ingestIcon: string;
  displayDialog = false;
  displayUploadMessage = false;
  importError = false;
  isAdmin = false;

  @Input() uploadType: string;
  @Input() url: string;
  @Input() importSucessMsg: string;
  @Input() importErrorMsg: string;
  @Input() uploadAPI: string;
  @Input() extensions: string[];

  constructor(private uploadService: UploadService, private router: Router, private authenticationService: AuthenticationService) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        delete this.fileName;
        delete this.fileUpload;
      }
    });
  }

  ngOnInit() {
    this.contextId = this.uploadType;
    if (!this.extensions) {
      this.extensions = ['.zip', '.tar', '.tar.gz', '.tar.bz2'];
    }
    this.isAdmin = this.authenticationService.isAdmin();
  }

  ngOnDestroy() {
  }

  checkFileExtension(fileName: string): boolean {
    this.fileName = fileName;
    if (fileName.endsWith(this.extensions[0]) || fileName.endsWith(this.extensions[1])
      || fileName.endsWith(this.extensions[2]) || fileName.endsWith(this.extensions[3])) {
        return true;
    }else {
      this.displayDialog = true;
      return false;
    }
  }

  onFileDrop(file) {
    if (!this.checkFileExtension(file.name)) {
      return;
    }
    this.fileUpload = file;
    this.resetUpload();
  }

  onChange(file) {
    if (!this.checkFileExtension(file[0].name)) {
      return;
    }
    this.fileUpload = file[0];
    this.resetUpload();
  }

  computeSize(size) {
    const units = [' octets', ' ko', ' Mo', ' Go', ' To'];
    let indice = 0;
    while (size >= 1000 || indice >= 4) {
      size = Math.round(size / 1000);
      indice++;
    }
    return size + units[indice];
  }

  uploadFile() {
    this.uploadInProgess = true;
    this.uploadService.uploadFile(this.fileUpload, this.contextId, this.action);

    this.uploadState = this.uploadService.getUploadState().subscribe(
      (uploadProgress) => {
        if (uploadProgress.ingestStatus === 'NOT_STARTED') {
          setTimeout(() => {
            this.uploadProgress = Math.round(uploadProgress.uploadStatus * 100 / this.fileUpload.size);
          }, 0);
        } else {
          setTimeout(() => {
            this.ingestInProgess = true;
            this.uploadProgress = 100;
          }, 0);
          setTimeout(() => {
            this.checkIngestStatus()
          }
            , 2000);
        }
      }
    );
  }

  finishUpload(response: any) {
    this.ingestInProgess = false;
    this.uploadService.clearIngest().subscribe();
    if (this.action === 'RESUME') {
      this.uploadService.downloadATR(response);
    }
    this.resetUpload();
  }

  checkIngestStatus() {
    this.uploadService.checkIngestStatus().subscribe((response) => {
      if (response.status === 204) {
        this.ingestInProgess = true;
        setTimeout(() => {
          this.checkIngestStatus();
        }, 500);
      } else if (response.status === 200) {
        this.ingestIcon = 'fa-check';
        this.finishUpload(response);
      } else if (response.status === 206) {
        this.ingestIcon = 'fa-exclamation-triangle';
        this.finishUpload(response);
      }

    }, (error) => {
      this.ingestIcon = 'fa-times';
      this.finishUpload(error);
    });
  }

  resetUpload() {
    if (this.uploadState) {
      this.uploadState.unsubscribe();
    }
    this.uploadProgress = 0;
    this.uploadInProgess = false;
    this.ingestInProgess = false;
  }
}
