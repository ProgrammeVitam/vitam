import { Injectable } from '@angular/core';
import 'rxjs/add/operator/map';
import {ResourcesService} from '../../common/resources.service';

@Injectable()
export class CollectionService {
  deleteRoot= 'delete';

  constructor(private resourceService: ResourcesService) { }

  removeItemsInCollection(api: string) {
    return this.resourceService.delete(`${this.deleteRoot}/${api}`);
  }

}
