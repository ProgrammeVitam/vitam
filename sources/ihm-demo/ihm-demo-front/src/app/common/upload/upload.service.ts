import { Injectable } from '@angular/core';
import {CookieService} from "angular2-cookie/core";
import {BehaviorSubject} from "rxjs/BehaviorSubject"
import {Observable} from "rxjs/Observable";
import { HttpHeaders } from '@angular/common/http';

import { ResourcesService } from '../resources.service';

const BYTES_PER_CHUNK = 1024 * 1024;
const tenantKey = 'X-Tenant-Id';
const contextIdKey = 'X-Context-Id';
const actionKey = 'X-Action';

export class ingestStatusElement {
  uploadStatus: number;
  ingestStatus: string;
}

@Injectable()
export class UploadService {

  private uploadState = new BehaviorSubject<ingestStatusElement>({
    uploadStatus : 0,
    ingestStatus : 'NOT_STARTED',
  });

  private uploadedSize = 0;
  private ingestOperationId = '';
  private contextId = '';
  private action = '';

  constructor(private resourcesService : ResourcesService) { }

  private uploadXHR(component : any, file:any, requestId:any, chunkOffset:any, size:any) {
    let xhr = new XMLHttpRequest();
    xhr.open('POST', '/ihm-demo/v1/api/ingest/upload', true);
    xhr.setRequestHeader('Content-Type', 'application/octet-stream');
    xhr.setRequestHeader('X-REQUEST-ID', requestId);
    xhr.setRequestHeader('X-Chunk-Offset', chunkOffset);
    xhr.setRequestHeader('X-Size-Total', size);
    xhr.setRequestHeader(contextIdKey, component.contextId);
    xhr.setRequestHeader(actionKey, component.action);
    xhr.setRequestHeader(tenantKey, component.resourcesService.getTenant());
    xhr.onreadystatechange = function () {
      if (xhr.readyState == XMLHttpRequest.DONE && xhr.status == 200) {
        let totalSize = chunkOffset + BYTES_PER_CHUNK;

        if (totalSize < component.uploadedSize) {
          return;
        }
        if (totalSize < size) {
          component.uploadedSize = totalSize;
          component.changeUploadState({
            uploadStatus : totalSize,
            ingestStatus : 'NOT_STARTED'
          });
        } else {
          component.uploadedSize = size;
          component.changeUploadState({
            uploadStatus : size,
            ingestStatus : 'STARTED'
          });
        }
      }
    };
    return xhr.send(file);
  }

  uploadFile(file : any, contextId : string, action : string) {
    let blob = file;
    let SIZE = blob.size;

    let start = 0;
    let end = (SIZE < BYTES_PER_CHUNK) ? SIZE : BYTES_PER_CHUNK;

    this.changeUploadState({
      uploadStatus : end,
      ingestStatus : 'NOT_STARTED'
    });
    this.contextId = contextId;
    this.action = action;
    let xhr = new XMLHttpRequest();
    let requestId = '';
    xhr.open('POST', '/ihm-demo/v1/api/ingest/upload', true);
    xhr.setRequestHeader('Content-Type', 'application/octet-stream');
    xhr.setRequestHeader('X-Chunk-Offset', start.toString());
    xhr.setRequestHeader('X-Size-Total', SIZE);
    xhr.setRequestHeader(contextIdKey, contextId);
    xhr.setRequestHeader(actionKey, action);
    xhr.setRequestHeader(tenantKey, this.resourcesService.getTenant());
    var _self = this;
    xhr.onreadystatechange = function () {
      if (xhr.readyState == XMLHttpRequest.DONE && xhr.status == 200) {
        requestId = xhr.getResponseHeader('X-REQUEST-ID');
        start = end;
        end = start + BYTES_PER_CHUNK;
        _self.uploadedSize = end;
        _self.ingestOperationId = requestId;
        if (start >= SIZE) {
          _self.changeUploadState({
            uploadStatus : SIZE,
            ingestStatus : 'STARTED'
          });
          return;
        }
        var chunkNumber = 0;
        while (start < SIZE) {
          _self.uploadXHR(_self, blob.slice(start, end), requestId, start, SIZE);
          start = end;
          end = start + BYTES_PER_CHUNK;
        }
      }
    };
    xhr.send(blob.slice(start, end));
  }

  getUploadState() : Observable<ingestStatusElement>  {
    return this.uploadState;
  }

  changeUploadState(state : ingestStatusElement) {
    this.uploadState.next(state);
  }

  checkIngestStatus() {
    return this.resourcesService.get('check/' +this.ingestOperationId, null, 'blob');
  }

  clearIngest() {
    return this.resourcesService.get('clear/' +this.ingestOperationId);
  }

  downloadATR(response: any) {
    let body = response.body;
    var a = document.createElement("a");
    document.body.appendChild(a);
    var url = window.URL.createObjectURL(body);
    a.href = url;

    if(response.headers.get('content-disposition')!== undefined && response.headers.get('content-disposition')!== null){
      a.download = response.headers.get('content-disposition').split('filename=')[1];
      a.click();
      a.remove();

    }
  }

  uploadReferentials(collection : string, file: any) {
    let header = new HttpHeaders();
    header = header.set('Content-Type', 'application/octet-stream').set('X-Filename', file.name);
    return this.resourcesService.post(collection, header, file);
  }

  checkReferentials(collection : string) {
    return this.resourcesService.post('check/' +this.ingestOperationId);
  }
}
