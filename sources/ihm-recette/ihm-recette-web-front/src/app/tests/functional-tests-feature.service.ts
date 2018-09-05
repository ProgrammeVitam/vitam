import { Injectable } from '@angular/core';
import {ResourcesService} from '../common/resources.service';
import { HttpHeaders } from '@angular/common/http';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class FunctionalTestsFeatureService {
  FEATURE_TNR='applicative-test/testFeature';

  constructor(private resourceService: ResourcesService) {
  }

  public launchFeature(text: string): Observable<any> {
    return this.resourceService.post(`${this.FEATURE_TNR}`,null,text);

  }
}
