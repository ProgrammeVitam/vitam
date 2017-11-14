import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";
import 'rxjs/add/observable/of';

import { ArchiveTreeViewComponent } from './archive-tree-view.component';
import { ArchiveUnitService } from "../../archive-unit.service";
import { VitamResponse } from "../../../common/utils/response";
import {NodeData, TreeNode} from "./tree-node";

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

describe('ArchiveTreeViewComponent', () => {
  let component: ArchiveTreeViewComponent;
  let fixture: ComponentFixture<ArchiveTreeViewComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
      ],
      declarations: [ ArchiveTreeViewComponent ],
      schemas: [ NO_ERRORS_SCHEMA ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveTreeViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it( 'should not request again if parents are setted', () => {
    const spyOngetResults = spyOn(ArchiveUnitServiceStub, 'getResults').and.callThrough();

    component.root = new TreeNode('Test Node', 'id', new NodeData('INGEST', ['idUp']));
    component.root.parents.push(new TreeNode('Title 2', 'id2', new NodeData('INGEST', ['idUpUp'])));

    ArchiveTreeViewComponent.getParents(ArchiveUnitServiceStub, component.root);

    expect(spyOngetResults.calls.count()).toEqual(0);
  });

  it( 'should not request again if chieldren are setted', () => {
    const spyOngetResults = spyOn(ArchiveUnitServiceStub, 'getResults').and.callThrough();

    component.root = new TreeNode('Test Node', 'id', new NodeData('INGEST', ['idUp']));
    component.root.children.push(new TreeNode('Title 2', 'id2', new NodeData('INGEST', ['idUpUp'])));

    ArchiveTreeViewComponent.getChildren(ArchiveUnitServiceStub, component.root);

    expect(spyOngetResults.calls.count()).toEqual(0);
  });

  it( 'should not request parents when no unitups', () => {
    const spyOngetResults = spyOn(ArchiveUnitServiceStub, 'getResults').and.callThrough();

    component.root = new TreeNode('Test Node', 'id', new NodeData('INGEST', []));

    ArchiveTreeViewComponent.getParents(ArchiveUnitServiceStub, component.root);

    expect(spyOngetResults.calls.count()).toEqual(0);
  });
});
