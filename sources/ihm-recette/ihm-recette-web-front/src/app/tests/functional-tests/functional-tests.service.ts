import { Injectable } from '@angular/core';
import 'rxjs/add/operator/map';
import {Observable} from 'rxjs/Observable';
import {plainToClass} from 'class-transformer';

import { Report } from './detail/report';
import { ResourcesService } from '../../common/resources.service';

@Injectable()
export class FunctionalTestsService {
  TNR_API = 'applicative-test';

  constructor(private client: ResourcesService) { }

  getResults() {
    return this.client.get(this.TNR_API);
  }

  getResultDetail(fileName: string): Observable<Report> {
    return this.client.get(this.TNR_API + '/' + fileName)
      .map((json) => plainToClass<Report, Object>(Report, json));
  }

  launchTests() {
    return this.client.post(this.TNR_API, null);
  }

}
