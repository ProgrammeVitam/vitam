import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {plainToClass} from 'class-transformer';
import {VitamResponse} from '../common/utils/response';
import {ArchiveUnitService} from '../archive-unit/archive-unit.service';
import {BasketInfo} from './selection';
import {ResourcesService} from '../common/resources.service';

const BASKET_LOCAL_STORAGE_VARIABLE = 'AU_Basket';

@Injectable()
export class MySelectionService {

  constructor(public archiveUnitService: ArchiveUnitService, public resourceService: ResourcesService) { }

  getQueryForBasket(offset: number, limit: number) {
    // FIXME Set Tenant !
    const basket: BasketInfo[] = this.getBasketFromLocalStorage(this.resourceService.getTenant());
    const ids = basket.map((x) => x.id);
    return {
      '$query': [
        {
          '$in': {
            '#id': ids
          }
        }
      ],
      '$filter': {
        '$limit': limit,
        '$offset': offset
      },
      '$projection': {
        '$fields': {
          '#id': 1,
          '#unitType': 1,
          '#object': 1,
          Title: 1
        }
      }
    };
  }

  getQueryForChildren(rootId: string) {
    return {
      '$query': [
        {
          '$or': [
            {
              '$in': {
                '#allunitups': [rootId]
              }
            }
          ]
        }
      ],
      '$filter': {},
      '$projection': {
        '$fields': {
          '#id': 1,
          '#unitType': 1,
          '#object': 1,
          'Title': 1
        }
      }
    };
  }

  getResults(offset, limit): Observable<VitamResponse> {
    const query = this.getQueryForBasket(offset, limit);
    return this.archiveUnitService.getByQuery(query);
  }

  getChildren(id: string): Observable<VitamResponse> {
    const query = this.getQueryForChildren(id);
    return this.archiveUnitService.getByQuery(query);
  }

  haveChildren(id: string): boolean {
    const basket: BasketInfo[] = this.getBasketFromLocalStorage(this.resourceService.getTenant());
    const find: BasketInfo = basket.find((x) => x.id === id);
    return find ? find.child : false;
  }

  getIds(isOperation: boolean, id: string) {
    let query: any;
    if (isOperation) {
      query = {
        '$query': [
          {
            '$eq': {
              '#operations': id
            }
          }
        ],
        '$projection': {
          '$fields': {
            '#id': 1
          }
        }
      };
      return this.archiveUnitService.getByQuery(query);
    }

    // else: only build a response with the id alone
    const response: VitamResponse = new VitamResponse();
    response.$results = [{'#id': id}];
    response.httpCode = 200;
    response.$hits = {
      limit: 1,
      offset: 0,
      size: 1,
      total: 1
    };
    return Observable.of(response);
  }

  getIdsToSelect(isOperation: boolean, id: string): Observable<VitamResponse> {
    return this.getIds(isOperation, id);
  }

  addToSelection(withChildren: boolean, ids: string[], tenantId: string) {
    let basketInfo: BasketInfo[] = this.getBasketFromLocalStorage(tenantId);

    if (basketInfo === null) {
      // Case of new item
      basketInfo = ids.map((x) => new BasketInfo(x, withChildren));
      this.setBasketToLocalStorage(tenantId, basketInfo);
    } else {
      // Check for duplicate/update values
      for (let i = 0; i < ids.length; i++) {
        const id = ids[i];
        const index = basketInfo.findIndex((x) => id === x.id);
        if (index === -1) {
          basketInfo.push(new BasketInfo(id, withChildren));
        } else {
          basketInfo[index].child = withChildren;
        }
      }

      this.setBasketToLocalStorage(tenantId, basketInfo);
    }
  }

  deleteFromBasket(ids: string[]) {
    const tenantId: string = this.resourceService.getTenant();
    const basketInfo: BasketInfo[] = this.getBasketFromLocalStorage(tenantId);
    this.setBasketToLocalStorage(tenantId, basketInfo.filter(x => ids.indexOf(x.id) === -1));
  }

  getBasketFromLocalStorage(tenantId: string): BasketInfo[] {
    const storageBasket: string = localStorage.getItem(`${BASKET_LOCAL_STORAGE_VARIABLE}_${tenantId}`);
    if (storageBasket === null) {
      return [];
    }
    return plainToClass(BasketInfo, JSON.parse(storageBasket));
  }

  setBasketToLocalStorage(tenantId: string, basketInfo: BasketInfo[]) {
    localStorage.setItem(`${BASKET_LOCAL_STORAGE_VARIABLE}_${tenantId}`, JSON.stringify(basketInfo));
  }
}
