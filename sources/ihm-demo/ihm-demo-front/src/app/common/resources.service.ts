import { Injectable } from '@angular/core';
import {CookieService} from "angular2-cookie/core";
import 'rxjs/add/operator/map';
import {Headers, Http, RequestOptionsArgs, Response} from "@angular/http";

const TENANT_COOKIE = 'tenant';
const BASE_URL = '/ihm-recette/v1/api/';
const TENANTS = 'tenants';

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
    }

    options.headers = header;
    return this.http.get(`${BASE_URL}${url}`, options);
  }

  post(url, header?: Headers, body?: any) {
    const options: RequestOptionsArgs = new RequestOptionsTenant;
    if (!header) {
      header = new Headers();
    }
    if (this.getTenant()) {
      header.append('X-Tenant-Id', this.getTenant());
    }
    options.headers = header;
    return this.http.post(`${BASE_URL}${url}`, body, options);
  }

  delete(url) {
    const options: RequestOptionsArgs = new RequestOptionsTenant;
    const headers: Headers = new Headers();
    if ( this.getTenant()) {
      headers.append('X-Tenant-Id', this.getTenant());
    }
    options.headers = headers;
    return this.http.delete(`${BASE_URL}${url}`, options);
  }

  getTenants() {
    return this.get(TENANTS)
      .map((res: Response) => res.json());
  }

  setTenant(tenantId: string) {
    this.cookies.put(TENANT_COOKIE, tenantId);
  }

  getTenant() {
    return this.cookies.get(TENANT_COOKIE);
  }

}
