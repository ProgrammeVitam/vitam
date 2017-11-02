import { Injectable } from '@angular/core';
import {Observable} from "rxjs/Rx";
import {VitamResponse} from "../common/utils/response";
import {ResourcesService} from "../common/resources.service";
import { HttpHeaders } from '@angular/common/http';

@Injectable()
export class ArchiveUnitService {
  ARCHIVE_UNIT_SEARCH_API = 'archivesearch';
  ARCHIVE_UNIT_API = 'archiveunit';
  ARCHIVE_UPDATE_API = 'archiveupdate';
  UNITS = 'units';
  UNIT = 'unit';
  EXPORT = 'dipexport';
  OBJECTS = 'objects';

  static inputRequest : any;

  constructor(private resourceService: ResourcesService) { }

  getDetails(id): Observable<VitamResponse> {
    return this.resourceService.get(`${this.ARCHIVE_UNIT_SEARCH_API}/${this.UNIT}/${id}`);
  }

  getObjects(ogId): Observable<VitamResponse> {
    return this.resourceService.get(`${this.ARCHIVE_UNIT_API}/${this.OBJECTS}/${ogId}`);
  }

  getResults(body: any, offset: number = 0, limit: number = 125): Observable<VitamResponse> {

    const headers = new HttpHeaders()
      .set('X-Limit', '' + limit)
      .set('X-Offset', '' + offset);

    return this.resourceService.post(`${this.ARCHIVE_UNIT_SEARCH_API}/${this.UNITS}`, headers, body);
  }

  exportDIP(body: any): Observable<VitamResponse> {
    return this.resourceService.post(`${this.ARCHIVE_UNIT_API}/${this.EXPORT}`, undefined, body);
  }

  downloadDIP(id: string) {
    return this.resourceService.get(`${this.ARCHIVE_UNIT_API}/${this.EXPORT}/${id}`, null, 'blob');
  }

  updateMetadata(id: string, updateRequest: any) {
    return this.resourceService.post(`${this.ARCHIVE_UPDATE_API}/${this.UNITS}/${id}`, undefined, updateRequest);
  }

  static setInputRequest(request) {
    this.inputRequest = request ;
  }

  static getInputRequest() {
    return this.inputRequest;
  }
}
