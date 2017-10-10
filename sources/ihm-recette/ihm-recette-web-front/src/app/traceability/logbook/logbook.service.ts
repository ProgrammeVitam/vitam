import { Injectable } from '@angular/core';
import { ResourcesService } from '../../common/resources.service';

@Injectable()
export class LogbookService {
  TRACABILITY_API = 'operations/traceability';

  constructor(private client: ResourcesService) {}

  launchTracability() {
    return this.client.post(this.TRACABILITY_API, null);
  }

}
