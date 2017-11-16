import {Injectable} from '@angular/core';
import {ResourcesService} from "../common/resources.service";
import {HttpHeaders} from "@angular/common/http";

@Injectable()
export class WorkflowService {

  OPERATIONS = 'operations';
  WORKFLOWS = 'workflows';

  constructor(private resourceService: ResourcesService) {

  }


  getOperations(body) {
    let headers = new HttpHeaders().set('X-Access-Contract-Id', this.resourceService.getAccessContract())
      .set('X-Tenant-Id', this.resourceService.getTenant());
    return this.resourceService.post(this.OPERATIONS, headers, body);
  }

  sendOperationsAction(id: string, body: any, action: string) {
    let headers = new HttpHeaders().set('X-Access-Contract-Id', this.resourceService.getAccessContract())
      .set('X-Tenant-Id', this.resourceService.getTenant()).set('X-Action', action);
    return this.resourceService.put(this.OPERATIONS + '/' + id, headers, body, 'text');
  }

  stopOperation(id: string, body: any) {
    let headers = new HttpHeaders().set('X-Access-Contract-Id', this.resourceService.getAccessContract())
      .set('X-Tenant-Id', this.resourceService.getTenant());
    return this.resourceService.delete(this.OPERATIONS + '/' + id, headers);
  }

  getWorkflowsDefinition() {
    let headers = new HttpHeaders().set('X-Access-Contract-Id', this.resourceService.getAccessContract())
      .set('X-Tenant-Id', this.resourceService.getTenant());
    return this.resourceService.get(this.WORKFLOWS, headers);
  }

}
