import { Injectable } from '@angular/core';
import { ResourcesService } from '../../common/resources.service';

@Injectable()
export class LogbookService {
  TRACABILITY_OPERATION_API = 'operations/traceability';
  TRACABILITY_LFC_API = 'lifecycles/traceability';
  TRACABILITY_STORAGE_API = 'storages/traceability';

  constructor(private client: ResourcesService) {}

  launchTraceability() {
    return this.client.post(this.TRACABILITY_OPERATION_API, null);
  }

  launchTraceabilityLFC() {
    return this.client.post(this.TRACABILITY_LFC_API, null);
  }

  launchTraceabilityStorage() {
    return this.client.post(this.TRACABILITY_STORAGE_API, null);
  }
}
