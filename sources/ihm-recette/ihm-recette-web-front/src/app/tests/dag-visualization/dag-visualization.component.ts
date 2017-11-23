import { Component } from '@angular/core';
import { Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { SelectItem } from 'primeng/primeng';
import { Subscription } from 'rxjs/Subscription';
import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { ResourcesService } from '../../common/resources.service';
import { PageComponent } from '../../common/page/page-component';
import { Contract } from '../../common/contract';
import { TenantService } from '../../common/tenant.service';
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
  operationId: string;
  requestResponse: string;
  showError: boolean = false;
  showGraph: boolean = false;
  detail: string;
  visNetworkData: VitamNetworkData;
  visNetworkOptions: VisNetworkOptions;
  visNetwork: string = 'networkId1';

  constructor(public breadcrumbService: BreadcrumbService, public queryDslService: QueryDslService,
    private visNetworkService: VisNetworkService,
    public titleService: Title, private resourcesService: ResourcesService,
    public tenantService: TenantService) {
    super('Visualisation du Graphe', breadcrumb, titleService, breadcrumbService)
  }


  public sendRequest(): void {
    var selectedCollection = 'UNIT';
    var selectedMethod = 'GET';
    var selectedAction = 'GET';
    var contractIdentifier = !this.selectedContract ? null : this.selectedContract.Identifier;
    var jsonRequest = {
      $roots: [],
      $query: [{ $eq: {} }],
      $projection: {}
    };
    if (contractIdentifier != null) {
      jsonRequest.$query[0].$eq["#operations"] = this.operationId;
    }

    // manage errors
    this.queryDslService.executeRequest(jsonRequest, contractIdentifier,
      selectedCollection, selectedMethod, selectedAction, null).subscribe(
      (response) => {
        this.requestResponse = JSON.stringify(response.json(), null, 2);
        if (response.json().httpCode >= 400) {
          this.showError = true;
          this.showGraph = false;
        } else {
          this.showError = false;
          this.showGraph = true;
          this.displayDag(response.json().$results);
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

  private getTenant(): string {
    return this.resourcesService.getTenant();
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

  public pageOnInit(): void {
    this.getContracts();
    this.tenantService.getState().subscribe(
      () => this.getContracts()
    );
  }

  private networkInitialized(): void {
    // now we can use the service to register on events
    this.visNetworkService.on(this.visNetwork, 'click');
  }


  public displayDag(units): void {
    this.detail = "";
    // create network datas
    var nbUnits = !units ? 0 : units.length;
    var unitNodes = [];
    var unitEdges = [];
    var nbEdges = 0;
    for (var i = 0; i < nbUnits; i++) {
      var unit = {
        // data
        id: units[i]["#id"],
        label: units[i]["Title"],
        // options
        widthConstraint: { maximum: 200 },
        heightConstraint: { maximum: 200 },
        group: units[i]["#max"]

      };
      unitNodes[i] = unit;
      if (units[i]["#unitups"] != undefined) {
        var nbUps = units[i]["#unitups"].length;
        for (var e = 0; e < nbUps; e++) {
          unitEdges[nbEdges] = { from: units[i]["#id"], to: units[i]["#unitups"][e] };
          nbEdges++;
        }
      }
    }

    // create an array with nodes
    var nodes = new VisNodes(unitNodes);
    // create an array with edges
    var edges = new VisEdges(unitEdges);

    this.visNetworkData = {
      nodes,
      edges,
    };

    // create network options
    this.visNetworkOptions = {
      layout: {
        hierarchical: {
          direction: "DU",
          sortMethod: "directed"
        }
      },
      interaction: { hover: true },
      /*pathysics: {
        forceAtlas2Based: {
          gravitationalConstant: -26,
          centralGravity: 0.005,
          springLength: 230,
          springConstant: 0.18
        },
        maxVelocity: 146,
        solver: 'forceAtlas2Based',
        timestep: 0.35,
        stabilization: {iterations: 150}
      },*/
      nodes: {
        shape: 'box'
      }
    };

    // network events
    this.visNetworkService.on(this.visNetwork, 'click');

    this.visNetworkService.click.subscribe((eventData: any[]) => {
      if (eventData[0] === this.visNetwork) {
        this.detail = "";
        var nbUnits = units.length;
        for (var i = 0; i < nbUnits; i++) {
          if (units[i]["#id"] === eventData[1].nodes[0]) {
            this.detail = JSON.stringify(units[i], null, 2);
          }
        }
      }
    });

  }

}
