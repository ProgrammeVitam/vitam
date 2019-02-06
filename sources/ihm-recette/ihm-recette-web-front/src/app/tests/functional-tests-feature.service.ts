import { Injectable } from '@angular/core';
import {ResourcesService} from '../common/resources.service';
import { HttpHeaders } from '@angular/common/http';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class FunctionalTestsFeatureService {
  FEATURE_TNR='applicative-test/testFeature';
  SYNC='applicative-test/syncTnrPieces';
  SYNC_WITH_BRANCH='applicative-test/syncTnrPiecesWithBranch';
  GET_BRANCHES='applicative-test/gitBranches';
  GET_CURRENT_BRANCH='applicative-test/currentGitBranch';

  constructor(private resourceService: ResourcesService) {
  }

  public launchFeature(text: string): Observable<any> {
    return this.resourceService.post(`${this.FEATURE_TNR}`,null,text);

  }
  public sync(): Observable<any> {
    return this.resourceService.post(`${this.SYNC}`);

  }
  public syncWithBranch(branch: string): Observable<any> {
    return this.resourceService.post(`${this.SYNC_WITH_BRANCH}`,null,branch);
  }
  public getAllBranches(): Observable<any> {
    return this.resourceService.get(`${this.GET_BRANCHES}`);
  }

}
