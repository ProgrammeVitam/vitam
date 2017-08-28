import { Injectable } from '@angular/core';
import {CookieService} from "angular2-cookie/core";
import 'rxjs/add/operator/map';
import {Headers, Http, RequestOptionsArgs, Response} from "@angular/http";
import {Observable} from "rxjs/Observable";

import {VitamResponse} from "./utils/response";

const TENANT_COOKIE = 'tenant';
const CONTRACT_COOKIE = 'accessContract';
const BASE_URL = '/ihm-demo/v1/api/';
const TENANTS = 'tenants/';

export class RequestOptionsTenant implements RequestOptionsArgs {}

@Injectable()
export class ResourcesService {

  constructor(private cookies: CookieService, private http: Http) { }

  get(url, header?: Headers) {
    const options: RequestOptionsArgs = new RequestOptionsTenant;
    if (!header) {
      header = new Headers();
    }

    if ( this.getTenant()) {
      header.append('X-Tenant-Id', this.getTenant());
      header.append('X-Access-Contract-Id', this.getAccessContract());
    }

    options.headers = header;
    return this.http.get(`${BASE_URL}${url}/`, options);
  }

  post(url, header?: Headers, body?: any) {
    const options: RequestOptionsArgs = new RequestOptionsTenant;
    if (!header) {
      header = new Headers();
    }
    if (this.getTenant()) {
      header.append('X-Tenant-Id', this.getTenant());
      header.append('X-Access-Contract-Id', this.getAccessContract());
    }
    options.headers = header;
    return this.http.post(`${BASE_URL}${url}`, body, options);
  }

  delete(url) {
    const options: RequestOptionsArgs = new RequestOptionsTenant;
    const headers: Headers = new Headers();
    if ( this.getTenant()) {
      headers.append('X-Tenant-Id', this.getTenant());
      headers.append('X-Access-Contract-Id', this.getAccessContract());
    }
    options.headers = headers;
    return this.http.delete(`${BASE_URL}${url}`, options);
  }

  getTenants() {
    return this.get(TENANTS)
      .map((res: Response) => res.json());
  }

  getAccessContrats() : Observable<VitamResponse> {
    return this.post('accesscontracts', null, {"ContractName":"all","Status":"ACTIVE"})
      .map((res: Response) => res.json());
  }

  setAccessContract(contractName: string) {
    this.cookies.put(CONTRACT_COOKIE, contractName);
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

}
