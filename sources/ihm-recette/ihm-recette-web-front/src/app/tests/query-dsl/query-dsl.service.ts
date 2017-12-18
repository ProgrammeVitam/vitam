import { Injectable } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import {plainToClass} from 'class-transformer';
import {Contract} from '../../common/contract';
import {Observable} from 'rxjs/Observable';
import {ResourcesService} from '../../common/resources.service';
import {isNullOrUndefined} from "util";

@Injectable()
export class QueryDslService {
  accessContracts = 'accesscontracts';
  dslQueryTestUrl = 'dslQueryTest';

  constructor(private resourceService: ResourcesService) { }

  public getContracts(): Observable<Array<Contract>> {
    const headers = new HttpHeaders({'Content-Type': 'application/json'});

    return this.resourceService.post(`${this.accessContracts}`,
      headers,
      '{"ContractID": "all", "ContractName": "all", "orderby": {"field": "Name", "sortType": "ASC"}}')
      .map((json) => plainToClass(Contract, json.$results));
  }

  public checkJson(jsonQuery: string) {
    try {
      JSON.parse(jsonQuery);
      return true;
    } catch (e) {
      return false;
    }
  }

  private getRequestHeader(contractId: string, requestedCollection: string,
                           requestMethod: string, xAction: string, objectId: string) {
    let headers = new HttpHeaders({'Content-Type': 'application/json'});
    if (!isNullOrUndefined(contractId)) {
      headers = headers.set('X-Access-Contract-Id', contractId);
    }
    if (!isNullOrUndefined(requestedCollection)) {
      headers = headers.set('X-Requested-Collection', requestedCollection);
    }
    if (!isNullOrUndefined(requestMethod)) {
      headers = headers.set('X-Http-Method-Override', requestMethod);
    }
    if (!isNullOrUndefined(objectId)) {
      headers = headers.set('X-Object-Id', objectId);
    }
    if (!isNullOrUndefined(xAction)) {
      headers = headers.set('X-Action', xAction);
    }
    return headers;
  }

  public executeRequest(query, contractId: string, requestedCollection: string,
                        requestMethod: string, xAction: string, objectId: string) {

    return this.resourceService.post(`${this.dslQueryTestUrl}`,
      this.getRequestHeader(contractId, requestedCollection, requestMethod, xAction, objectId),
      query);
  }
}
