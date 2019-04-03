import { Injectable } from '@angular/core';
import {ResourcesService} from '../../common/resources.service';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class LoadStorageService {
  DOWNLOAD_API = 'download';
  UPLOAD_API = 'replaceObject';
  DELETE_API = 'deleteObject';

  constructor(public resourcesService: ResourcesService) { }

  download(fileName, category, offerId): Observable<any> {
    return this.resourcesService.get(`${this.DOWNLOAD_API}/${offerId}/${category}/${fileName}`, null, 'blob');
  }

  uploadFile(newFile, fileName, size,category,offerId): Observable<any> {
    return this.resourcesService.post(`${this.UPLOAD_API}/${category}/${offerId}/${fileName}/${size}`, null, newFile)
  }

  delete(fileName, category, offerId): Observable<any> {
    return this.resourcesService.delete(`${this.DELETE_API}/${category}/${offerId}/${fileName}`, null)
  }

}
