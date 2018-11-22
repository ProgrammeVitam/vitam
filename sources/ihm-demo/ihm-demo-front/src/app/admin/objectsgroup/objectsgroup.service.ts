import { Injectable } from '@angular/core';
import {Observable} from 'rxjs';
import {VitamResponse} from '../../common/utils/response';
import {ResourcesService} from '../../common/resources.service';
import { HttpHeaders } from '@angular/common/http';

@Injectable()
export class ObjectsGroupService {
static inputRequest: any;
ARCHIVE_OBJECT_GROUP_DOWNLOAD_URL = 'archiveunit/objects/download';
ARCHIVE_UNIT_SEARCH_API = 'archivesearch';
ARCHIVE_UNIT_API = 'archiveunit';
ARCHIVE_UPDATE_API = 'archiveupdate';
TREE = 'tree';
UNITS = 'units';
UNIT = 'unit';
EXPORT = 'dipexport';
AUDIT = 'evidenceaudit';
OBJECTS = 'objects';
OBJECTS_GROUP_SEARCH_API = 'objectssearch';

static setInputRequest(request) {
    this.inputRequest = request ;
  }

  static getInputRequest() {
    return this.inputRequest;
  }

  constructor(private resourceService: ResourcesService) { }

  getResults(body: any, offset: number = 0, limit: number = 125): Observable<VitamResponse> {

    const headers = new HttpHeaders()
      .set('X-Limit', '' + limit)
      .set('X-Offset', '' + offset);

    return this.resourceService.post(`${this.OBJECTS_GROUP_SEARCH_API}/${this.OBJECTS}`, headers, body);
  }
}