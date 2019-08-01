import { Injectable } from '@angular/core';
import {ResourcesService} from '../../common/resources.service';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class LoadStorageService {
  DOWNLOAD_API = 'download';
  READ_ORDER_API = 'readorder';
  UPLOAD_API = 'replaceObject';
  DELETE_API = 'deleteObject';

  constructor(public resourcesService: ResourcesService) { }

  download(fileName, category, strategyId, offerId): Observable<any> {
    return this.resourcesService.get(`${this.DOWNLOAD_API}/${strategyId}/${offerId}/${category}/${fileName}`, null, 'blob');
  }

  createReadOrderRequest(fileName, category, strategyId, offerId): Observable<any> {
    return this.resourcesService.post(`${this.READ_ORDER_API}/${strategyId}/${offerId}/${category}/${fileName}`, null, 'json');
  }

  getReadOrderRequest(readOrderId, strategyId): Observable<any> {
    return this.resourcesService.head(`${this.READ_ORDER_API}/${strategyId}/${readOrderId}`, null, 'json');
  }

  uploadFile(newFile, fileName, size, category, strategyId ,offerId): Observable<any> {
    return this.resourcesService.post(`${this.UPLOAD_API}/${category}/${strategyId}/${offerId}/${fileName}/${size}`, null, newFile)
  }

  delete(fileName, category, strategyId, offerId): Observable<any> {
    return this.resourcesService.delete(`${this.DELETE_API}/${category}/${strategyId}/${offerId}/${fileName}`, null)
  }

}
