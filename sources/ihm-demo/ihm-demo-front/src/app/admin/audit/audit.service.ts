import { Injectable } from '@angular/core';
import { Observable } from "rxjs/Observable";

import { VitamResponse } from "../../common/utils/response";
import { ResourcesService } from "../../common/resources.service";

@Injectable()
export class AuditService {

  constructor(private resourceService: ResourcesService) { }

  launchAudit(body : any) : Observable<VitamResponse> {
    return this.resourceService.post('audits', null, body);
  }
}
