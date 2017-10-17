import { Injectable } from '@angular/core';
import {Headers, Response} from "@angular/http";
import {Observable} from "rxjs/Observable";

import {VitamResponse} from "../common/utils/response";
import {ResourcesService} from "../common/resources.service";

@Injectable()
export class ReferentialsService {

  searchAPI : string;

  constructor(private resourceService: ResourcesService) { }

  getResults(body: any): Observable<VitamResponse> {

    const headers = new Headers();

    if (this.searchAPI === 'admin/formats') {
      body.FORMAT = 'all';
      if (!body.FormatName) {
        body.FormatName = '';
      }
      if (!body.PUID) {
        body.PUID = '';
      }
      body.orderby = {"field":"Name","sortType":"ASC"};
    }

    if (this.searchAPI === 'profiles') {
      if (!body.ProfileID) {
        body.ProfileID = 'all';
      }
      if (!body.ProfileName) {
        body.ProfileName = 'all';
      }
      body.orderby = {"field":"Name","sortType":"ASC"};
    }

    if (this.searchAPI === 'contexts') {
      if (!body.ContextID) {
        body.ContextID = 'all';
      }
      if (!body.ContextName) {
        body.ContextName = 'all';
      }
      body.orderby = {"field":"Name","sortType":"ASC"};
    }

    if (this.searchAPI === 'admin/rules') {
      body.RULES = 'all';
      if (!body.RuleType) {
        body.RuleType = '';
      } else {
        let ruleType = '';
        if (typeof body.RuleType == 'object') {
          for (let index in body.RuleType) {
            ruleType = body.RuleType[index] + ',' + ruleType;
          }
          body.RuleType =  ruleType;
        }
      }
      if (!body.RuleValue) {
        body.RuleValue = '';
      }
      body.orderby = {"field":"RuleValue","sortType":"ASC"};
    }

    if (this.searchAPI === 'contracts' || this.searchAPI === 'accesscontracts' ) {
      if (!body.ContractID) {
        body.ContractID = 'all';
      }
      if (!body.ContractName) {
        body.ContractName = 'all';
      }
      body.orderby = {"field":"Name","sortType":"ASC"};
    }

    return this.resourceService.post(this.searchAPI, headers, body)
      .map((res: Response) => res.json());
  }

  setSearchAPI(api : string) {
    this.searchAPI = api;
  }

  downloadProfile(id) {
    let header = new Headers();
    header.append('Accept', 'application/octet-stream');
    this.resourceService.get('profiles/' + id, header).subscribe(
      (response) => {
        const a = document.createElement('a');
        document.body.appendChild(a);

        a.href = URL.createObjectURL(new Blob([response.text()], {type: 'application/xml'}));

        if (response.headers.get('content-disposition') !== undefined && response.headers.get('content-disposition') !== null) {
          a.download = response.headers.get('content-disposition').split('filename=')[1];
          a.click();
        }
      }
    );
  }

  getFormatById(id : string) : Observable<VitamResponse> {
    return this.resourceService.post('admin/formats/' + id, null, {})
      .map((res: Response) => res.json())
  }

  getRuleById(id : string) : Observable<VitamResponse> {
    return this.resourceService.post('admin/rules/' + id, null, {})
      .map((res: Response) => res.json())
  }
  getAccessContractById(id : string) : Observable<VitamResponse> {
    return this.resourceService.get('accesscontracts/' + id)
      .map((res: Response) => res.json())
  }
  getIngestContractById(id : string) : Observable<VitamResponse> {
    return this.resourceService.get('contracts/' + id)
      .map((res: Response) => res.json())
  }
  getProfileById(id : string) : Observable<VitamResponse> {
    return this.resourceService.get('profiles/' + id)
      .map((res: Response) => res.json())
  }

  getContextById(id : string) : Observable<VitamResponse> {
    return this.resourceService.get('contexts/' + id)
      .map((res: Response) => res.json())
  }

  updateDocumentById(collection : string,id : string, body : any) : Observable<VitamResponse> {
    return this.resourceService.post(collection + '/' + id, null, body)
      .map((res: Response) => res.json())
  }
}
