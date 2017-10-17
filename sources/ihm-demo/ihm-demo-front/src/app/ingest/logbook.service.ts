import { Injectable } from '@angular/core';
import {ResourcesService} from "../common/resources.service";
import {Headers, Response} from "@angular/http";
import {Observable} from "rxjs/Observable";
import 'rxjs/add/observable/of';
import {VitamResponse} from "../common/utils/response";

@Injectable()
export class LogbookService {
  LOGBOOK_API = 'logbook/operations';

  constructor(private resourceService: ResourcesService) { }

  getResults(body: any, offset: number = 0): Observable<VitamResponse> {

    const headers = new Headers();
    headers.append('X-Limit', '125');
    headers.append('X-Offset', '' + offset);

    return this.resourceService.post(this.LOGBOOK_API, headers, body)
        .map((res: Response) => res.json());
  }

}
