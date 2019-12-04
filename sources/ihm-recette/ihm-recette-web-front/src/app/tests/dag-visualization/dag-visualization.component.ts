import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { SelectItem } from 'primeng/primeng';
import { Subscription } from 'rxjs/Subscription';
import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { PageComponent } from '../../common/page/page-component';
import { Contract } from '../../common/contract';
import { TenantService } from '../../common/tenant.service';
import { EggService } from '../../common/egg/egg.service';
import { QueryDslService } from '../query-dsl/query-dsl.service';

import {
  VisNode,
  VisNodes,
  VisEdges,
  VisNetworkService,
  VisNetworkData,
  VisNetworkOptions
} from 'ng2-vis';

const breadcrumb: BreadcrumbElement[] = [
  { label: 'Tests', routerLink: '' },
  { label: 'Visualisation du Graphe', routerLink: 'tests/dag-visualization' }
];

class VitamNetworkData implements VisNetworkData {
  public nodes: VisNodes;
  public edges: VisEdges;
}

@Component({
  selector: 'vitam-dag-visualization',
  templateUrl: './dag-visualization.component.html',
  styleUrls: ['./dag-visualization.component.css']
})
export class DagVisualizationComponent extends PageComponent {
  selectedContract: Contract;
  contractsList: Array<SelectItem>;
  operationIds: string[];
  requestResponse: string;
  showError = false;
  showGraph = false;
  detail: string;
  visNetworkData: VitamNetworkData;
  visNetworkOptions: VisNetworkOptions;
  visNetwork = 'networkId1';
  tenant: string;

  constructor(public breadcrumbService: BreadcrumbService, public queryDslService: QueryDslService,
    private visNetworkService: VisNetworkService,
    public titleService: Title, public tenantService: TenantService,
    public eggService: EggService) {
    super('Visualisation du Graphe', breadcrumb, titleService, breadcrumbService)
  }


  public sendRequest(): void {
    const selectedCollection = 'UNIT';
    const selectedMethod = 'GET';
    const selectedAction = 'GET';
    const contractIdentifier = !this.selectedContract ? null : this.selectedContract.Identifier;
    const jsonRequest = {
      $roots: [],
      $query: [{ $in: {} }],
      $projection: {}
    };
    if (contractIdentifier != null) {
      jsonRequest.$query[0].$in['#operations'] = this.operationIds;
    }

    // manage errors
    this.queryDslService.executeRequest(jsonRequest, contractIdentifier,
      selectedCollection, selectedMethod, selectedAction, null).subscribe(
        (response) => {
          this.requestResponse = JSON.stringify(response, null, 2);
          if (response.httpCode >= 400) {
            this.showError = true;
            this.showGraph = false;
          } else {
            this.showError = false;
            this.showGraph = true;
            this.displayDag(response.$results);
          }
        },
        (error) => {
          this.showError = true;
          this.showGraph = false;
          try {
            this.requestResponse = JSON.stringify(JSON.parse(error._body), null, 2);
          } catch (e) {
            this.requestResponse = error._body;
          }
        }
      );
  }

  public sendRequestGot(parentUnitId): void {
    const selectedCollection = 'OBJECTGROUP';
    const selectedMethod = 'GET';
    const selectedAction = 'GET';
    const contractIdentifier = !this.selectedContract ? null : this.selectedContract.Identifier;
    const jsonRequest = { $projection: {} };

    this.queryDslService.executeRequest(jsonRequest, contractIdentifier,
      selectedCollection, selectedMethod, selectedAction, parentUnitId).subscribe(
        (response) => {
          return JSON.stringify(response, null, 2);
        },
        (error) => {
          try {
            return JSON.stringify(JSON.parse(error._body), null, 2);
          } catch (e) {
            return error._body;
          }
        }
      );
  }

  public getContracts(): Subscription {
    return this.queryDslService.getContracts().subscribe(
      (response) => {
        this.contractsList = response.map(
          (contract) => {
            return { label: contract.Name, value: contract }
          }
        );
      }
    )
  }

  public pageOnInit(): Subscription {
    return this.tenantService.getState().subscribe(
      (tenant) => {
        this.tenant = tenant;
        if (this.tenant) {
          this.getContracts();
        }
      }
    );
  }

  // FIXME: Useless method
  private networkInitialized(): void {
    // now we can use the service to register on events
    this.visNetworkService.on(this.visNetwork, 'click');
  }

  public displayDag(units): void {
    this.detail = '';
    // create network datas
    const nbUnits = !units ? 0 : units.length;
    const unitNodes = [];
    const unitEdges = [];
    const gotNodes = [];
    const gotEdges = [];

    for (let i = 0; i < nbUnits; i++) {
      unitNodes.push({
        // data
        id: units[i]['#id'],
        label: units[i]['Title'],
        // options
        widthConstraint: { maximum: 200 },
        heightConstraint: { maximum: 200 },
        group: units[i]['#max']
      });
      if (units[i]['#unitups'] !== undefined) {
        const nbUps = units[i]['#unitups'].length;
        for (let e = 0; e < nbUps; e++) {
          unitEdges.push({ from: units[i]['#id'], to: units[i]['#unitups'][e], arrows: 'to' });
        }
      }
      if (units[i]['#object'] !== undefined) {
        const gotId = units[i]['#object'];

        if (gotNodes.find(function (e) { return e.id === gotId }) === undefined) {
          gotNodes.push({
            id: gotId,
            label: "ObjectGroup: " + gotId,
            // options
            widthConstraint: { maximum: 200 },
            heightConstraint: { maximum: 200 }
          });
        }
        gotEdges.push({ from: gotId, to: units[i]['#id'], arrows: 'to ', dashes: true });
      }
    }

    // create an array with nodes
    const nodes = new VisNodes(gotNodes.concat(unitNodes));
    // create an array with edges
    const edges = new VisEdges(unitEdges.concat(gotEdges));

    this.visNetworkData = {
      nodes,
      edges,
    };

    // create network options
    this.visNetworkOptions = {
      layout: {
        hierarchical: {
          direction: 'DU',
          sortMethod: 'directed'
        }
      },
      interaction: { hover: true },
      nodes: {
        shape: 'box'
      }
    };

    // network events
    this.visNetworkService.on(this.visNetwork, 'click');

    this.visNetworkService.click.subscribe((eventData: any[]) => {
      if (eventData[0] === this.visNetwork) {
        this.detail = '';
        for (let i = 0; i < units.length; i++) {
          if (units[i]['#id'] === eventData[1].nodes[0]) {
            this.detail = JSON.stringify(units[i], null, 2);
          } else if (units[i]['#object'] === eventData[1].nodes[0]) {
            const selectedCollection = 'OBJECTGROUP';
            const selectedMethod = 'GET';
            const selectedAction = 'GET';
            const contractIdentifier = !this.selectedContract ? null : this.selectedContract.Identifier;
            const jsonRequest = { $projection: {} };
            this.queryDslService.executeRequest(jsonRequest, contractIdentifier,
              selectedCollection, selectedMethod, selectedAction, units[i]['#id']).subscribe(
                (response) => {
                  this.detail = JSON.stringify(response["$results"], null, 2);
                },
                (error) => {
                  try {
                    this.detail = JSON.stringify(JSON.parse(error._body), null, 2);
                  } catch (e) {
                    this.detail = error._body;
                  }
                }
              );
          }
        }
      }
    });

  }

  toggleEgg() {
    this.eggService.changeState(!this.eggService.getState());
  }

}
