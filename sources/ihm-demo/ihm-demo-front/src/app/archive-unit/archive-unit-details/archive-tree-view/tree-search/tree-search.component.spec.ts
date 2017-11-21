import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Observable } from "rxjs/Rx";

import { CalendarModule, OverlayPanelModule, PanelModule } from 'primeng/primeng';

import { TreeSearchComponent } from './tree-search.component';

import { ArchiveUnitService } from "../../../archive-unit.service";
import { ArchiveUnitHelper } from "../../../archive-unit.helper";
import { VitamResponse } from "../../../../common/utils/response";
import {NodeData, TreeNode} from "../tree-node";
import {ReactiveFormsModule} from "@angular/forms";

let DefaultResponse = {
  $context: {},
  $hits: {},
  $results: [],
  httpCode: 200
};

let ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of(new VitamResponse()),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(DefaultResponse),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

describe('TreeSearchComponent', () => {
  let component: TreeSearchComponent;
  let fixture: ComponentFixture<TreeSearchComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        ArchiveUnitHelper,
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
      ],
      imports: [ BrowserAnimationsModule, CalendarModule, OverlayPanelModule, PanelModule, RouterTestingModule, ReactiveFormsModule ],
      declarations: [ TreeSearchComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TreeSearchComponent);
    component = fixture.componentInstance;
    let data: NodeData = new NodeData('unit', []);
    component.node = new TreeNode('Node', 'id0', data);
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should reinit search and form when clearFields is call', () => {
    let spyOnSubmit = spyOn(component, 'onSubmit').and.callThrough();
    component.criteriaSearch.Title = 'Test';
    component.clearFields();
    expect(component.criteriaSearch.Title).toBeUndefined();
    expect(spyOnSubmit.calls.count()).toBe(1);
  });

  it('should check if element is in list', () => {
    component.inList['id0'] = new TreeNode('Name', 'id0', new NodeData('', []));
    expect(component.isInList({'#id': 'id0'})).toBeTruthy();
    expect(component.isInList({'#id': 'id1'})).toBeFalsy();
  });

  it('should check that switchSelected add/remove elements as well', () => {
    let event1 = {data: {'#id': 'id1', '#unitType': 'UNIT', '#unitups': []}};
    let event2 = {data: {'#id': 'id2', '#unitType': 'UNIT', '#unitups': []}};

    component.inList = {};

    component.switchSelected(event1);
    expect(component.isInList(event1.data)).toBeTruthy();

    component.switchSelected(event2);
    expect(component.isInList(event2.data)).toBeTruthy();

    component.switchSelected(event1);
    expect(component.isInList(event1.data)).toBeFalsy();
  });

  it('should update the good node when calling updateSelection', () => {
    component.panel = {hide: () => {}};
    let data: NodeData = new NodeData('UNIT', []);
    component.inList = {
      'Id0': new TreeNode('First', 'Id0', data),
      'Id1': new TreeNode('Second', 'Id1', data),
      'Id2': new TreeNode('Third', 'Id2', data),
      'Id3': new TreeNode('Fourth', 'Id3', data)
    };

    component.searchParents = true;
    component.node.children = [];
    component.node.parents = [];

    component.updateSelection();
    expect(component.node.parents.length).toBe(4);
    expect(component.node.children.length).toBe(0);

    component.searchParents = false;
    component.node.parents = [];

    component.updateSelection();
    expect(component.node.parents.length).toBe(0);
    expect(component.node.children.length).toBe(4);
  });

  it('should make the good query when calling updateCriteria', () => {
    let request = {'Title': 'Search'};
    component.searchParents = false;
    component.node.id = 'test';
    component.node.data.unitups = ['parent'];

    component.updateCriteria(request);

    expect(component.criteriaSearch.UNITUPS).toBe(component.node.id);
    expect(component.criteriaSearch.ROOTS).toBeUndefined();
    expect(component.criteriaSearch.Title).toBe('Search');

    component.clearFields();

    component.searchParents = true;

    component.updateCriteria(request);

    expect(component.criteriaSearch.UNITUPS).toBeUndefined();
    expect(component.criteriaSearch.ROOTS).toBe(component.node.data.unitups);
    expect(component.criteriaSearch.Title).toBe('Search');
  });
});
