import { Injectable } from '@angular/core';
import {ResourcesService} from '../../common/resources.service';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class LoadStorageService {
  DOWNLOAD_API = 'download';
  UPLOAD_API = 'replaceObject';

  constructor(public resourcesService: ResourcesService) { }

  download(fileName, category): Observable<any> {
    return this.resourcesService.get(`${this.DOWNLOAD_API}/${category}/${fileName}`, null, 'blob');
  }

  uploadFile(newFile, fileName, category): Observable<any> {
    return this.resourcesService.post(`${this.UPLOAD_API}/${category}/${fileName}`, null, newFile)
  }

}
