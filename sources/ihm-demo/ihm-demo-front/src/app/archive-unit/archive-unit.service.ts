import { Injectable } from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {VitamResponse} from '../common/utils/response';
import {ResourcesService} from '../common/resources.service';
import { HttpHeaders } from '@angular/common/http';

@Injectable()
export class ArchiveUnitService {
  static inputRequest: any;
  ARCHIVE_OBJECT_GROUP_DOWNLOAD_URL = 'archiveunit/objects/download';
  ARCHIVE_UNIT_SEARCH_API = 'archivesearch';
  ARCHIVE_UPDATE_API = 'archiveupdate';
  ARCHIVE_UNIT_API = 'archiveunit';
  TREE = 'tree';
  UNITS = 'units';
  UNIT = 'unit';
  UNIT_WITH_INHERITED_RULES = 'unitsWithInheritedRules';
  EXPORT = 'dipexport';
  AUDIT = 'evidenceaudit';
  PROBATIVE_VALUE = 'probativevalueexport';
  OBJECTS = 'objects';
  ELIMINATION_ANALYSIS = 'elimination/analysis';
  ELIMINATION_ACTION = 'elimination/action';
  PRESERVATION = 'preservation';
  COMPUTED_INHERITED_RULES = 'computedinheritedrules';
  

  static setInputRequest(request) {
    this.inputRequest = request ;
  }

  static getInputRequest() {
    return this.inputRequest;
  }

  constructor(private resourceService: ResourcesService) { }

  getByQuery(query): Observable<VitamResponse> {
    return this.resourceService.post(`${this.ARCHIVE_UNIT_API}/${this.TREE}`, undefined, query);
  }

  getDetailsWithInheritedRules(id: string): Observable<VitamResponse> {
    return this.resourceService.get(`${this.ARCHIVE_UNIT_SEARCH_API}/${this.UNIT_WITH_INHERITED_RULES}/${id}`);
  }

  getDetails(id): Observable<VitamResponse> {
    return this.resourceService.get(`${this.ARCHIVE_UNIT_SEARCH_API}/${this.UNIT}/${id}`);
  }

  getObjects(ogId): Observable<VitamResponse> {
    return this.resourceService.get(`${this.ARCHIVE_UNIT_API}/${this.OBJECTS}/${ogId}`);
  }

  getObjectURL(ogId, options) {
    const tenant = this.resourceService.getTenant();
    const accessContract = this.resourceService.getAccessContract();
    const encodedFileName = encodeURIComponent(options.filename);
    return `${this.resourceService.getBaseURL()}${this.ARCHIVE_OBJECT_GROUP_DOWNLOAD_URL}/${ogId}` +
      `?usage=${options.usage}&filename=${encodedFileName}&tenantId=${tenant}&contractId=${accessContract}`;
  }

  getResults(body: any, offset: number = 0, limit: number = 125): Observable<VitamResponse> {

    const headers = new HttpHeaders()
      .set('X-Limit', '' + limit)
      .set('X-Offset', '' + offset);

    return this.resourceService.post(`${this.ARCHIVE_UNIT_SEARCH_API}/${this.UNITS}`, headers, body);
  }

  exportDIP(query: {$query: object, $projection: object, $filter: object}, dataObjectVersions: string[]): Observable<VitamResponse> {
    const body = {
      dslRequest: query,
      dataObjectVersionToExport: {dataObjectVersions}
    };
    return this.resourceService.post(`${this.ARCHIVE_UNIT_API}/${this.EXPORT}`, undefined, body);
  }

  audit(body: any): Observable<VitamResponse> {
    return this.resourceService.post(`${this.ARCHIVE_UNIT_API}/${this.AUDIT}`, undefined, body);
  }

  eliminationAnalysis(body: any): Observable<VitamResponse> {
    return this.resourceService.post(`${this.ELIMINATION_ANALYSIS}`, undefined, body);
  }

  eliminationAction(body: any): Observable<VitamResponse> {
    return this.resourceService.post(`${this.ELIMINATION_ACTION}`, undefined, body);
  }

  massUpdate(body: any): Observable<VitamResponse> {
    return this.resourceService.post(`${this.ARCHIVE_UPDATE_API}/${this.UNITS}`, undefined, body);
  }

  probativeValue(body: any): Observable<VitamResponse> {
    return this.resourceService.post(`${this.ARCHIVE_UNIT_API}/${this.PROBATIVE_VALUE}`, undefined, body);
  }


  downloadDIP(id: string) {
    return this.resourceService.get(`${this.ARCHIVE_UNIT_API}/${this.EXPORT}/${id}`, null, 'blob');
  }

  updateMetadata(id: string, updateRequest: any) {
    return this.resourceService.post(`${this.ARCHIVE_UPDATE_API}/${this.UNITS}/${id}`, undefined, updateRequest);
  }

  preservation(updateRequest: any) {
    return this.resourceService.post(`${this.PRESERVATION}`, undefined, updateRequest);
  }
  COMPUTEDINHERITEDRULES_SERVICE(body: any): Observable<VitamResponse> {
    return this.resourceService.post(`${this.ARCHIVE_UNIT_API}/${this.COMPUTED_INHERITED_RULES}`, undefined, body);
  }

}
