import { Injectable } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import { ResourcesService } from '../../common/resources.service';
import { isNullOrUndefined } from "util";

@Injectable()
export class LinkAuService {
  UPDATE_LINK_API = 'updateLinks';

  constructor(public resourcesService: ResourcesService) { }

  updateLinks(request: any, contractId: string): Observable<any> {

    let headers = new HttpHeaders({'Content-Type': 'application/json'});
    if (!isNullOrUndefined(contractId)) {
      headers = headers.set('X-Access-Contract-Id', contractId);
    }

    return this.resourcesService.post(`${this.UPDATE_LINK_API}`, headers, request);
  }

}
