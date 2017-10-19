import { Component, OnInit, Input } from '@angular/core';
import {FormGroup, FormControl} from "@angular/forms";
import {Preresult} from "../../../../common/search/preresult";
import {ArchiveUnitService} from "../../../archive-unit.service";
import {TreeNode, NodeData} from '.././tree-node';
import {ColumnDefinition} from "../../../../common/generic-table/column-definition";
import {ArchiveUnitHelper} from "../../../archive-unit.helper";
import {Hits} from "../../../../common/utils/response";

@Component({
  selector: 'vitam-tree-search',
  templateUrl: './tree-search.component.html',
  styleUrls: ['./tree-search.component.css']
})
export class TreeSearchComponent implements OnInit {
  @Input() node: TreeNode;
  @Input() searchParents: boolean;
  @Input() label: string;
  @Input() panel;
  searchForm: FormGroup;
  hits: Hits;
  data: any[];
  displayedItems: any[];
  criteriaSearch: any;
  inList: any = {};
  firstItem = 0;
  firstPage = 0;
  lastPage = 0;
  nbRows = 5;

  public columns = [
    ColumnDefinition.makeStaticColumn('#id', 'Identifiant', undefined, () => ({'width': '325px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('Title', 'Titre', undefined, () => ({'width': '200px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('_unitType', 'Type', this.archiveUnitHelper.transformType, () => ({'width': '100px'})),
    ColumnDefinition.makeStaticColumn('#originating_agency', 'Service Producteur', undefined, () => ({'width': '200px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeSpecialValueColumn('Date de début', this.archiveUnitHelper.getStartDate, this.archiveUnitHelper.handleDate, () => ({'width': '100px'})),
    ColumnDefinition.makeSpecialValueColumn('Date de fin', this.archiveUnitHelper.getEndDate, this.archiveUnitHelper.handleDate, () => ({'width': '100px'})),
  ];

  constructor(public archiveUnitService: ArchiveUnitService, public archiveUnitHelper: ArchiveUnitHelper) { }

  ngOnInit() {
    this.searchForm = new FormGroup({
      titleAndDescription: new FormControl(''),
      startDate: new FormControl(''),
      endDate: new FormControl('')
    });
  }

  public onSubmit() {

    let request: any = {};
    if (this.searchForm.valid) {
      Object.keys(this.searchForm.controls).forEach(key => {
        request[key] = this.searchForm.controls[key].value;
      });
    } else {
    }

    let preResult = new Preresult();
    preResult.searchProcessSkip = false;

    this.updateCriteria(request);

    this.archiveUnitService.getResults(this.criteriaSearch).subscribe(
        (response) => {
          this.hits = response.$hits;
          this.data = response.$results;
          this.firstPage = 0;
          this.lastPage = this.firstPage + this.hits.limit / this.nbRows;
          this.displayedItems = this.data.slice(this.firstItem, this.firstItem + this.nbRows);
        },
        (error) => console.log('Error: ', error)
    );

    let list = [];
    if (this.searchParents) {
      list = this.node.parents;
    } else {
      list = this.node.children;
    }
    for (let item of list) {
      this.inList[item.id] = item;
    }

  }

  clearFields() {
    this.searchForm.reset();
    this.searchForm.enable();
  }

  updateCriteria(request) {

    this.criteriaSearch = {
      isAdvancedSearchFlag: "Yes",
      projection_title: 'Title',
      projection_id: '#id',
      projection_unitups: '#unitups',
      projection_unitType: '#unittype',
      projection_startdate: 'StartDate',
      projection_enddate: 'EndDate',
      projection_createddate: 'CreatedDate',
      projection_acquireddate: 'AcquiredDate',
      projection_sentdate: 'SentDate',
      projection_receiveddate: 'ReceivedDate',
      projection_registereddate: 'RegisteredDate',
      projection_transactdate: 'TransactedDate',
      projection_originatingagency: '#originating_agency'
    };

    if (request.startDate && request.startDate) {
      this.criteriaSearch.StartDate = request.startDate;
      this.criteriaSearch.EndDate = request.endDate;
    }

    if (this.searchParents) {
      this.criteriaSearch.ROOTS = this.node.data.unitups;
    } else {
      this.criteriaSearch.UNITUPS = this.node.id;
    }

    if(request.titleAndDescription) {
      this.criteriaSearch.titleAndDescription = request.titleAndDescription;
    }
  }

  switchSelected(event) {
      let data: NodeData = new NodeData(event.data['_unitType'], event.data['#unitups']);
      let node = new TreeNode(event.data.Title, event.data.id, data);
      this.inList[event.data['#id']] = node;
  }

  isInList(data) {
    return this.inList[data['#id']];
  }

  updateSelection() {
    let list = [];
    if (this.searchParents) {
      list = this.node.parents;
    } else {
      list = this.node.children;
    }

    for(let prop in this.inList) {
      if (this.inList.hasOwnProperty(prop)) {
        var item = this.inList[prop];
        let isInChildren = false;
        for (let node of list) {
          if (node.id === item.id) {
            isInChildren = true;
            break;
          }
        }
        if (!isInChildren) {
          if (this.searchParents) {
            this.node.parents.push(item);
          } else {
            this.node.children.push(item);
          }
        }
      }
    }

    this.panel.hide();
  }

  paginate(event) {
    this.firstItem = event.first;
    // TODO If unloadedPage reached (See how to trigg) => Call search with offset.
    if (event.page >= this.lastPage || event.page <= this.firstPage) {

      this.archiveUnitService.getResults(this.criteriaSearch, this.firstItem).subscribe(
          (response) => {
            this.data = Array.from('x'.repeat(event.page * this.nbRows)).concat(response.$results);
            this.firstPage = event.page;
            this.lastPage = this.firstPage + this.hits.limit / this.nbRows;
            this.displayedItems = this.data.slice(this.firstItem, this.firstItem + this.nbRows);
          },
          (error) => console.log('Error: ', error)
      );

    } else {
      this.displayedItems = this.data.slice(this.firstItem, this.firstItem + this.nbRows);
    }
  }
}
