import {Injectable} from '@angular/core';
import {HttpHeaders} from '@angular/common/http';
import {Observable} from 'rxjs/Observable';

import {VitamResponse} from '../common/utils/response';
import {ResourcesService} from '../common/resources.service';

@Injectable()
export class ReferentialsService {

  searchAPI: string;
  ACCESS_CONTRACT: string = 'accesscontracts';

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
      body.orderby = {'field': 'Name', 'sortType': 'ASC'};
    }

    if (this.searchAPI === 'profiles') {
      if (!body.ProfileID) {
        body.ProfileID = 'all';
      }
      if (!body.ProfileName) {
        body.ProfileName = 'all';
      }
      body.orderby = {'field': 'Name', 'sortType': 'ASC'};
    }

    if (this.searchAPI === 'preservationScenarios') {
      if (!body.ScenarioID) {
        body.ScenarioID = 'all';
      }
      if (!body.ScenarioName) {
        body.ScenarioName = 'all';
      }
      body.orderby = {'field': 'Name', 'sortType': 'ASC'};
    }

    if (this.searchAPI === 'griffins') {
      if (!body.GriffinID) {
        body.GriffinID = 'all';
      }
      if (!body.GriffinName) {
        body.GriffinName = 'all';
      }
      body.orderby = {'field': 'Name', 'sortType': 'ASC'};
    }

    if (this.searchAPI === 'archiveunitprofiles') {
      if (!body.ArchiveUnitProfileID) {
        body.ArchiveUnitProfileID = 'all';
      }
      if (!body.ArchiveUnitProfileName) {
        body.ArchiveUnitProfileName = 'all';
      }
      body.orderby = {'field': 'Name', 'sortType': 'ASC'};
    }

    if (this.searchAPI === 'contexts') {
      if (!body.ContextID) {
        body.ContextID = 'all';
      }
      if (!body.ContextName) {
        body.ContextName = 'all';
      }
      body.orderby = {'field': 'Name', 'sortType': 'ASC'};
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
      body.orderby = {'field': 'Name', 'sortType': 'ASC'};
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
          body.RuleType = ruleType;
        }
      }
      if (!body.RuleValue) {
        body.RuleValue = '';
      }
    }

    if (this.searchAPI === 'contracts' || this.searchAPI === 'accesscontracts' || this.searchAPI === 'managementcontracts') {
      if (!body.ContractID) {
        body.ContractID = 'all';
      }
      if (!body.ContractName) {
        body.ContractName = 'all';
      }
      body.orderby = {'field': 'Name', 'sortType': 'ASC'};
    }

    if (this.searchAPI === 'admin/accession-register') {
      if (!body.OriginatingAgency) {
        body.ACCESSIONREGISTER = 'ACCESSIONREGISTER';
        delete body.OriginatingAgency;
      }
      body.orderby = {'field': 'OriginatingAgency', 'sortType': 'ASC'};
    }

    if (this.searchAPI === 'ontologies') {
      if (!body.OntologyName) {
        body.OntologyName = 'all';
      }
      if (!body.OntologyID) {
        body.OntologyID = 'all';
      }
      body.orderby = {'field': 'ApiField', 'sortType': 'ASC'};
    }

    return this.resourceService.post(this.searchAPI, headers, body);
  }

  setSearchAPI(api: string) {
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

  uploadProfile(id: string, file: File) {
    let header = new HttpHeaders().set('Content-Type', 'application/octet-stream');
    return this.resourceService.put('profiles/' + id, header, file, 'text');
  }

  downloadArchiveUnitProfile(id) {
    let header = new HttpHeaders().set('Accept', 'application/octet-stream');
    this.resourceService.get('archiveunitprofiles/' + id, header, 'blob').subscribe(
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

  uploadArchiveUnitProfile(id: string, file: File) {
    let header = new HttpHeaders().set('Content-Type', 'application/octet-stream');
    return this.resourceService.put('archiveunitprofiles/' + id, header, file, 'text');
  }

  uploadOntology(id: string, file: File) {
    let header = new HttpHeaders().set('Content-Type', 'application/octet-stream');
    return this.resourceService.put('ontologies/' + id, header, file, 'text');
  }

  getFormatById(id: string): Observable<VitamResponse> {
    return this.resourceService.post('admin/formats/' + decodeURIComponent(id), null, {});
  }

  getRuleById(id: string): Observable<VitamResponse> {
    return this.resourceService.post('admin/rules/' + id, null, {});
  }

  getAccessContractById(id: string): Observable<VitamResponse> {
    return this.resourceService.get('accesscontracts/' + id);
  }

  getIngestContractById(id: string): Observable<VitamResponse> {
    return this.resourceService.get('contracts/' + id);
  }

  getManagementContractById(id: string): Observable<VitamResponse> {
    return this.resourceService.get('managementcontracts/' + id);
  }

  getProfileById(id: string): Observable<VitamResponse> {
    return this.resourceService.get('profiles/' + id);
  }

  getArchiveUnitProfileById(id: string): Observable<VitamResponse> {
    return this.resourceService.get('archiveunitprofiles/' + id);
  }

  getOntologyById(id: string): Observable<VitamResponse> {
    return this.resourceService.get('ontologies/' + id);
  }

  getFundRegisterById(id: string, limit: number): Observable<VitamResponse> {
    const headers = new HttpHeaders()
      .set('X-Limit', `${limit}`)
      .set('X-Offset', '0');
    return this.resourceService.post('admin/accession-register', headers, {OriginatingAgency: id});
  }

  getAccessionRegisterSymbolic(id: string, limit: number = 3, offset: number = 0): Observable<VitamResponse> {
    const headers = new HttpHeaders()
      .set('X-Limit', `${limit}`)
      .set('X-Offset', `${offset}`);
    const searchForm = {OriginatingAgency: id, orderby: {field: 'CreationDate', sortType: 'DESC'}};
    return this.resourceService.post('admin/accession-register/symbolic', headers, searchForm);
  }

  getAccessionRegisterSymbolicByDate(id: string, startDate: Date, endDate: Date): Observable<VitamResponse> {
    const headers = new HttpHeaders()
      .set('X-Limit', '900')
      .set('X-Offset', '0');
    const searchForm = {OriginatingAgency: id, orderby: {field: 'CreationDate', sortType: 'DESC'}, startDate, endDate};
    return this.resourceService.post('admin/accession-register/symbolic', headers, searchForm);
  }

  getFundRegisterDetailById(originatingAgency: string, limit: number, offset: number): Observable<VitamResponse> {
    const headers = new HttpHeaders()
      .set('X-Limit', `${limit}`)
      .set('X-Offset', `${offset}`);
    const searchForm = {OriginatingAgency: originatingAgency, orderby: {field: 'EndDate', sortType: 'DESC'}};
    return this.resourceService.post(`admin/accession-register/${originatingAgency}/accession-register-detail`, headers, searchForm);
  }

  getContextById(id: string): Observable<VitamResponse> {
    return this.resourceService.get('contexts/' + id);
  }

  getAgenciesById(id: string): Observable<VitamResponse> {
    return this.resourceService.get('agencies/' + id);
  }

  updateDocumentById(collection: string, id: string, body: any): Observable<VitamResponse> {
    return this.resourceService.post(collection + '/' + id, null, body);
  }

  updateProfilById(id: string, body: any) {
    return this.resourceService.put('profiles/' + id, null, body, 'text');
  }

  updateArchiveUnitProfileById(id: string, body: any) {
    return this.resourceService.put('archiveunitprofiles/' + id, null, body, 'text');
  }

  getGriffinById(id: string) {
    return this.resourceService.get('griffin/' + id);
  }

  getScenarioById(id: string) {
    return this.resourceService.get('scenario/' + id);
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

  getScenarios(criteria) {
    return this.resourceService.post('preservationScenarios', new HttpHeaders(), criteria);
  }
}
