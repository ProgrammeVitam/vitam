import { Injectable } from '@angular/core';
import {ResourcesService} from "../common/resources.service";

@Injectable()
export class IngestService {
  INGEST_API = 'ingests';

  constructor(private resourceService: ResourcesService) { }

  getObject(id, type) {
    return this.resourceService.get(`${this.INGEST_API}/${id}/${type}`);
  }

}
