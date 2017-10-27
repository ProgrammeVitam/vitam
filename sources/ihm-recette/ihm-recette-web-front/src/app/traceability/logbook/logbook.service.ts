import { Injectable } from '@angular/core';
import { ResourcesService } from '../../common/resources.service';

@Injectable()
export class LogbookService {
  TRACABILITY_API = 'operations/traceability';
  TRACABILITY_LFC_API = 'lifecycles/traceability';

  constructor(private client: ResourcesService) {}

  launchTraceability() {
    return this.client.post(this.TRACABILITY_API, null);
  }
  launchTraceabilityLFC() {
    return this.client.post(this.TRACABILITY_LFC_API, null);
  }

}
