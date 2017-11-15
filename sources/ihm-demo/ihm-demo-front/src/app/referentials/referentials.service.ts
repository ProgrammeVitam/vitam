import { Injectable } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import {Observable} from "rxjs/Observable";

import {VitamResponse} from "../common/utils/response";
import {ResourcesService} from "../common/resources.service";

@Injectable()
export class ReferentialsService {

  searchAPI : string;
  ACCESS_CONTRACT = 'accesscontracts';

  constructor(private resourceService: ResourcesService) { }

  getResults(body: any): Observable<VitamResponse> {

    const headers = new HttpHeaders();

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

    if (this.searchAPI === 'agencies') {
      if (!body.AgencyID) {
        body.AgencyID = 'all';
      }
      if (!body.AgencyName) {
        body.AgencyName = 'all';
      }
      if (!body.Description) {
        delete body.Description;
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

    if (this.searchAPI === 'admin/accession-register') {
      if (!body.OriginatingAgency) {
        body.ACCESSIONREGISTER = 'ACCESSIONREGISTER';
        delete body.OriginatingAgency;
      }
      body.orderby = {"field":"OriginatingAgency","sortType":"ASC"};
    }

    return this.resourceService.post(this.searchAPI, headers, body);
  }

  setSearchAPI(api : string) {
    this.searchAPI = api;
  }

  downloadProfile(id) {
    let header = new HttpHeaders().set('Accept', 'application/octet-stream');
    this.resourceService.get('profiles/' + id, header, 'blob').subscribe(
      (response) => {
        const a = document.createElement('a');
        document.body.appendChild(a);

        a.href = URL.createObjectURL(response.body);

        if (response.headers.get('content-disposition') !== undefined && response.headers.get('content-disposition') !== null) {
          a.download = response.headers.get('content-disposition').split('filename=')[1];
          a.click();
        }
      }
    );
  }

  uploadProfile(id : string, file : File) {
    let header = new HttpHeaders().set('Content-Type', 'application/octet-stream');
    return this.resourceService.put('profiles/' + id, header, file, 'text');
  }

  getFormatById(id : string) : Observable<VitamResponse> {
    return this.resourceService.post('admin/formats/' + id, null, {});
  }

  getRuleById(id : string) : Observable<VitamResponse> {
    return this.resourceService.post('admin/rules/' + id, null, {});
  }
  getAccessContractById(id : string) : Observable<VitamResponse> {
    return this.resourceService.get('accesscontracts/' + id);
  }
  getIngestContractById(id : string) : Observable<VitamResponse> {
    return this.resourceService.get('contracts/' + id);
  }
  getProfileById(id : string) : Observable<VitamResponse> {
    return this.resourceService.get('profiles/' + id);
  }

  getFundRegisterById(id : string) : Observable<VitamResponse> {
    let searchForm = {"OriginatingAgency":id};
    return this.resourceService.post('admin/accession-register', null, searchForm);
  }
  getFundRegisterDetailById(id : string) : Observable<VitamResponse> {
    let searchForm = {"OriginatingAgency":id};
    return this.resourceService.post('admin/accession-register/'+ id +'/accession-register-detail', null, searchForm);
  }
  getContextById(id : string) : Observable<VitamResponse> {
    return this.resourceService.get('contexts/' + id);
  }
  getAgenciesById(id : string) : Observable<VitamResponse> {
    return this.resourceService.get('agencies/' + id);
  }
  updateDocumentById(collection : string,id : string, body : any) : Observable<VitamResponse> {
    return this.resourceService.post(collection + '/' + id, null, body);
  }

  updateProfilById(id : string, body : any) {
    return this.resourceService.put('profiles/' + id, null, body, 'text');
  }

  getTenants() {
    return this.resourceService.getTenants();
  }

  getTenantCurrent() {
    return this.resourceService.getTenant();
  }

  getAccessContract(criteria) {
    return this.resourceService.post(`${this.ACCESS_CONTRACT}`, new HttpHeaders(), criteria);
  }
}
