import {Injectable} from '@angular/core';
import {ResourcesService} from '../../common/resources.service';
import {Observable} from 'rxjs/Observable';
import {HttpHeaders} from '@angular/common/http';
import { isNullOrUndefined } from "util";

@Injectable()
export class TestAuditCorrectionService {
  LAUNCH = 'launchAudit';

  constructor(public resourcesService: ResourcesService) {
  }

  launch(operationId: string, contractId: string): Observable<any> {

    let headers = new HttpHeaders().set('X-Tenant-Id', this.resourcesService.getTenant());
    if (contractId) {
      headers = headers.set('X-Access-Contract-Id', contractId);
    }
    return this.resourcesService.post(`${this.LAUNCH}/${operationId}`, headers, null);

  }
}
