import { Injectable } from '@angular/core';
import {CookieService} from "angular2-cookie/core";
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Router} from '@angular/router';
import {Observable} from "rxjs/Observable";

const TENANT_COOKIE = 'REC-tenant';
const BASE_URL = '/ihm-recette/v1/api/';
const TENANTS = 'tenants';
const XSRF_TOKEN = 'REC-XSRF-TOKEN';

@Injectable()
export class ResourcesService {

  constructor(private cookies: CookieService, private http: HttpClient, private router: Router) { }

  get(url, header?: HttpHeaders, responsetype?: any): Observable<any> {
    const options: any = {};
    header = this.setDefaultHeader(header);
    options.headers = header;

    if (responsetype && responsetype !== 'json') {
      options.responseType = responsetype;
      options.observe = 'response';
    } else {
      options.responseType = 'json';
    }

    return this.http.get(`${BASE_URL}${url}`, options);
  }

  head(url, header?: HttpHeaders, responsetype?: any): Observable<any> {
    const options: any = {};
    header = this.setDefaultHeader(header);
    options.headers = header;

    if (responsetype && responsetype !== 'json') {
      options.responseType = responsetype;
      options.observe = 'response';
    } else {
      options.responseType = 'json';
    }

    return this.http.head(`${BASE_URL}${url}`, options);
  }

  post(url, header?: HttpHeaders, body?: any, responsetype?: any): Observable<any> {
    const options: any = {};
    header = this.setDefaultHeader(header);
    options.headers = header;
    options.responseType = responsetype || 'json';
    return this.http.post(`${BASE_URL}${url}`, body, options);
  }

  delete(url, header?: HttpHeaders) {
    const options: any = {};
    header = this.setDefaultHeader(header);
    options.headers = header;
    return this.http.delete(`${BASE_URL}${url}`, options);
  }

  getTenants() {
    return this.get(TENANTS);
  }

  setTenant(tenantId: string) {
    localStorage.setItem(TENANT_COOKIE, tenantId);
  }

  getTenant() {
    return localStorage.getItem(TENANT_COOKIE);
  }

  private setDefaultHeader(header?: HttpHeaders) {
    if (!header) {
      header = new HttpHeaders();
    }
    if (this.getTenant()) {
      header = header.set('X-Tenant-Id', this.getTenant());
    }
    if (localStorage.getItem(XSRF_TOKEN)) {
      header = header.set('X-CSRF-TOKEN', localStorage.getItem(XSRF_TOKEN));
    }
    return header;
  }
}
