import { Injectable } from '@angular/core';
import { CookieService } from 'angular2-cookie/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';

import { VitamResponse } from './utils/response';
import { DialogService } from './dialog/dialog.service';
import { Router } from '@angular/router';

const TENANT_COOKIE = 'tenant';
const CONTRACT_COOKIE = 'accessContract';
const BASE_URL = '/ihm-demo/v1/api/';
const TENANTS = 'tenants';

@Injectable()
export class ResourcesService {

  constructor(private cookies: CookieService, private http: HttpClient,
    private dialogService: DialogService) {
  }

  get(url, header?: HttpHeaders, responsetype?: any): Observable<any> {
    header = this.setDefaultHeader(header);

    if (responsetype && responsetype !== 'json') {
      return this.http.get(`${BASE_URL}${url}`, {
        headers: header,
        responseType: responsetype,
        observe: 'response'
      });
    } else {
      return this.http.get(`${BASE_URL}${url}`, {
        headers: header,
        responseType: 'json'
      });
    }

  }

  post(url, header?: HttpHeaders, body?: any, responsetype?: any): Observable<any> {
    header = this.setDefaultHeader(header);
    return this.http.post(`${BASE_URL}${url}`, body, { headers: header, responseType: responsetype || 'json' })
      .catch((err) => {
        if (err.status === 500) {
          this.dialogService.displayMessage(err.error.message +
            ' Veuillez contacter votre administrateur', 'Erreur système');
          return Observable.throw(err);
        }
      })
  }

  put(url, header?: HttpHeaders, body?: any, responsetype?: any): Observable<any> {
    header = this.setDefaultHeader(header);
    return this.http.put(`${BASE_URL}${url}`, body, { headers: header, responseType: responsetype || 'json' });
  }

  delete(url, header?: HttpHeaders): Observable<any> {
    header = this.setDefaultHeader(header);
    return this.http.delete(`${BASE_URL}${url}`, { headers: header });
  }

  getTenants() {
    return this.get(TENANTS);
  }

  getAccessContrats(): Observable<VitamResponse> {
    return this.post('accesscontracts', null, { 'ContractName': 'all', 'Status': 'ACTIVE' });
  }

  setAccessContract(contractIdentifier: string) {
    this.cookies.put(CONTRACT_COOKIE, contractIdentifier);
  }

  getAccessContract() {
    return this.cookies.get(CONTRACT_COOKIE);
  }

  setTenant(tenantId: string) {
    this.cookies.put(TENANT_COOKIE, tenantId);
  }

  getTenant() {
    return this.cookies.get(TENANT_COOKIE);
  }

  removeSessionInfo() {
    this.cookies.remove(TENANT_COOKIE);
    this.cookies.remove(CONTRACT_COOKIE);
  }

  private setDefaultHeader(header?: HttpHeaders) {
    if (!header) {
      header = new HttpHeaders();
    }
    if (this.getTenant()) {
      header = header.set('X-Tenant-Id', this.getTenant());
    }
    if (this.getAccessContract()) {
      header = header.set('X-Access-Contract-Id', this.getAccessContract());
    }
    if (localStorage.getItem('XSRF-TOKEN')) {
      header = header.set('X-CSRF-TOKEN', localStorage.getItem('XSRF-TOKEN'));
    }
    return header;
  }

  getBaseURL(): string {
    return BASE_URL;
  }

}
