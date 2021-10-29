import { Injectable } from '@angular/core';
import {ResourcesService} from '../../common/resources.service';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class LoadStorageService {
  DOWNLOAD_API = 'download';
  ACCESS_REQUEST_API = 'access-request';
  UPLOAD_API = 'replaceObject';
  DELETE_API = 'deleteObject';

  constructor(public resourcesService: ResourcesService) { }

  download(fileName, category, strategyId, offerId): Observable<any> {
    return this.resourcesService.get(`${this.DOWNLOAD_API}/${strategyId}/${offerId}/${category}/${fileName}`, null, 'blob');
  }

  createAccessRequest(fileName, category, strategyId, offerId): Observable<any> {
    return this.resourcesService.post(`${this.ACCESS_REQUEST_API}/${strategyId}/${offerId}/${category}/${fileName}`, null, 'json');
  }

  checkAccessRequestStatus(accessRequestId, strategyId, offerId): Observable<any> {
    return this.resourcesService.get(`${this.ACCESS_REQUEST_API}/${strategyId}/${offerId}/${accessRequestId}`, null, 'json');
  }

  deleteAccessRequest(accessRequestId, strategyId, offerId): Observable<any> {
    return this.resourcesService.delete(`${this.ACCESS_REQUEST_API}/${strategyId}/${offerId}/${accessRequestId}`, null);
  }

  uploadFile(newFile, fileName, size, category, strategyId, offerId): Observable<any> {
    return this.resourcesService.post(`${this.UPLOAD_API}/${category}/${strategyId}/${offerId}/${fileName}/${size}`, null, newFile)
  }

  delete(fileName, category, strategyId, offerId): Observable<any> {
    return this.resourcesService.delete(`${this.DELETE_API}/${category}/${strategyId}/${offerId}/${fileName}`, null)
  }

}
