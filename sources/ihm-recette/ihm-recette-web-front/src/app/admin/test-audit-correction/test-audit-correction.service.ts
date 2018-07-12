import {Injectable} from '@angular/core';
import {ResourcesService} from '../../common/resources.service';
import {Observable} from 'rxjs/Observable';
import {HttpHeaders} from '@angular/common/http';

@Injectable()
export class TestAuditCorrectionService {
  LAUNCH = 'launchAudit';

  constructor(public resourcesService: ResourcesService) {
  }

  launch(operationId): Observable<any> {

    let headers = new HttpHeaders().set('X-Tenant-Id', this.resourcesService.getTenant());
    return this.resourcesService.post(`${this.LAUNCH}/${operationId}`, headers, null);

  }
}
