import { Injectable } from '@angular/core';
import {Observable} from "rxjs/Observable";
import {ResourcesService} from "../../common/resources.service";
import {VitamResponse} from "../../common/utils/response";

@Injectable()
export class TraceabilityOperationService {
  TRACEABILITY_OPERATION_DETAIL ='/logbook/operations';
  EXTRACT_TIMESTAMP = '/traceability/extractTimestamp';
  CHECK_TRACEABILITY_OPERATION = '/traceability/check';

  constructor(private resourceService: ResourcesService) { }

  getDetails(id): Observable<VitamResponse> {
    return this.resourceService.post(`${this.TRACEABILITY_OPERATION_DETAIL}/${id}`, null, {});
  }

  checkTraceabilityOperation(id): Observable<VitamResponse> {
    const criteriaSearch = {
      EventID: id
    };
    return this.resourceService.post(`${this.CHECK_TRACEABILITY_OPERATION}`, null, criteriaSearch);
  }

  extractTimeStampInformation(timestampValue): Observable<any> {
    const criteriaSearch = {
      timestamp: timestampValue
    };
    return this.resourceService.post(`${this.EXTRACT_TIMESTAMP}`, null, criteriaSearch);
  }

}
