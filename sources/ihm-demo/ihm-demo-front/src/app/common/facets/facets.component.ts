import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Preresult } from '../preresult';
import { VitamResponse } from '../utils/response';
import { Facet, FacetDefinition, FacetType } from './facet';

@Component({
  selector: 'vitam-facets',
  templateUrl: './facets.component.html',
  styleUrls: ['./facets.component.css']
})
export class FacetsComponent implements OnInit {
  @Input() data: VitamResponse;
  @Input() service: any;
  @Input() submitFunction: (service: any, emitter: EventEmitter<any>, request: any) => void;
  @Input() preSearch: (request: any, advancedMode?: boolean) => Preresult = (x) => x;
  @Input() searchRequest: any = {};
  @Input() facetList: FacetDefinition[] = [];

  @Output() responseEvent: EventEmitter<any> = new EventEmitter<any>();

  FACET_TYPES = FacetType;
  selectedFacets: string[] = [];
  facetResults: any = {};
  facetInputs: any;

  constructor() { }

  ngOnInit() {
    let inputs = {};
    for (let facet of this.facetList) {
      inputs[facet.name] = facet.getBaseInput();
    }
    this.facetInputs = inputs;
  }

  ngOnChanges() {
    if (!!this.data && this.data.$facetResults
      && this.data.$facetResults.length > 0) {
      let results = this.data.$facetResults;
      for (const facetResult of results) {
        this.facetResults[facetResult.name] = facetResult.buckets;
      }
    }
  }

  research(selectedFacetValue, facetDefinition: FacetDefinition) {
    const facetSearchCriteria: any = {};
    facetSearchCriteria.field = facetDefinition.id;
    facetSearchCriteria.value = selectedFacetValue;

    this.searchRequest.requestFacet = facetSearchCriteria;
    this.searchRequest.facets = [];
    this.processPreSearch();
  }

  processPreSearch() {
    let preSearchReturn = this.preSearch(this.searchRequest);
    if (preSearchReturn.success) {
      this.submitFunction(this.service, this.responseEvent, preSearchReturn.request);
    } else {
      // Display error ? preSearchResult.searchProcessError
    }
  }

  clearFacets() {
    this.clearFacetResults();
    this.processPreSearch();
  }

  clearFacetResults() {
    this.selectedFacets = [];
    delete this.searchRequest.requestFacet;
  }

  searchFacets() {
    const facets: Facet[] = [];
    for (const facet of this.selectedFacets) {
      let matchingDefinition = this.facetList.find(x => x.name === facet);
      facets.push(matchingDefinition.createFacet(this.facetInputs[facet]));
    }

    this.searchRequest.facets = facets;
    delete this.searchRequest.requestFacet;

    this.processPreSearch();
  }
}
