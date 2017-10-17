import { Injectable } from '@angular/core';
import { Headers } from '@angular/http';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import {plainToClass} from 'class-transformer';
import {Contract} from '../../common/contract';
import {Observable} from 'rxjs/Observable';
import {ResourcesService} from '../../common/resources.service';

@Injectable()
export class QueryDslService {
  accessContracts = 'accesscontracts';
  dslQueryTestUrl = 'dslQueryTest';

  constructor(private resourceService: ResourcesService) { }

  public getContracts(): Observable<Array<Contract>> {
    const headers = new Headers({'Content-Type': 'application/json'});

    return this.resourceService.post(`${this.accessContracts}`,
      headers,
      '{"ContractID": "all", "ContractName": "all", "orderby": {"field": "Name", "sortType": "ASC"}}')
      .map((response) => response.json())
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
    const headers = new Headers();
    headers.append('Content-Type', 'application/json'); // Useless soon
    headers.append('X-Access-Contract-Id', contractId);
    headers.append('X-Requested-Collection', requestedCollection);
    headers.append('X-Http-Method-Override', requestMethod);
    headers.append('X-Action', xAction);
    headers.append('X-Object-Id', objectId);
    return headers;
  }

  public executeRequest(query, contractId: string, requestedCollection: string,
                        requestMethod: string, xAction: string, objectId: string) {

    return this.resourceService.post(`${this.dslQueryTestUrl}`,
      this.getRequestHeader(contractId, requestedCollection, requestMethod, xAction, objectId),
      query);
  }
}
