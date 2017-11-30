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

const auNoParentsNoChildren =
    {
      "Title":"Base", "StartDate":"2013-01-09T10:16:46.46Z","EndDate":"2017-09-01T08:46:09.9Z",
      "#id":"aeaqaaaaaaesc7rgaa6tkak75e4zjpaaaaga","#min":0,"#max":0,
      "#nbunits":0, "#unitups":[], "#allunitups":[]
    };

const auWithParents =
    {
      "Title":"Exact 5 Parents", "StartDate":"2013-01-09T10:16:46.46Z","EndDate":"2017-09-01T08:46:09.9Z",
      "#id":"aeaqaaaaaaesc7rgaa6tkak75e4zjpaaaaga","#min":0,"#max":0,
      "#nbunits":0,
      "#unitups":['p1', 'p2', 'p3', 'p4', 'p5'],
      "#allunitups":['p1', 'p2', 'p3', 'p4', 'p5','p6','p7']
    };

const auWithChildren =
    {
      "Title":"Exact 5 Children", "StartDate":"2013-01-09T10:16:46.46Z","EndDate":"2017-09-01T08:46:09.9Z",
      "#id":"aeaqaaaaaaesc7rgaa6tkak75e4zjpaaaaga","#min":0,"#max":0,
      "#nbunits":5,
      "#unitups":[],"#allunitups":[]
    };

let ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of(new VitamResponse()),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(DefaultResponse),
  getByQuery: (query) => Observable.of(DefaultResponse),
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

  it( 'should not request again if children are setted', () => {
    const spyOngetResults = spyOn(ArchiveUnitServiceStub, 'getResults').and.callThrough();

    component.root = new TreeNode('Test Node', 'id', new NodeData('INGEST', ['idUp']));
    component.root.children.push(new TreeNode('Title 2', 'id2', new NodeData('INGEST', ['idUpUp'])));

    ArchiveTreeViewComponent.getChildren(ArchiveUnitServiceStub, component.root);

    expect(spyOngetResults.calls.count()).toEqual(0);
  });

  it('should set the correct number of children', () => {
    const spyOnGetResulsts = spyOn(ArchiveUnitServiceStub, 'getResults').and.callFake(
      () => Observable.of({$results: [auNoParentsNoChildren, auWithParents, auWithChildren]})
    );

    let node = new TreeNode('Test Node', 'id', new NodeData('INGEST', ['idUp']));

    ArchiveTreeViewComponent.getChildren(ArchiveUnitServiceStub, node);

    expect(spyOnGetResulsts.calls.count()).toEqual(1);
    expect(node.children.length).toBe(3);
  });

  it('should set the child node as leaf when no sub-child', () => {
    const spyOnGetResulsts = spyOn(ArchiveUnitServiceStub, 'getResults').and.callFake(
      () => Observable.of({$results: [auNoParentsNoChildren]})
    );
    let node = new TreeNode('Test Node', 'id', new NodeData('INGEST', ['idUp']));

    ArchiveTreeViewComponent.getChildren(ArchiveUnitServiceStub, node);

    expect(spyOnGetResulsts.calls.count()).toEqual(1);
    expect(node.children.length).toBe(1);
    expect(node.children[0].leaf).toBeTruthy();
  });

  it('should set haveMoreChildren when more than 5 children are found', () => {
    const spyOnGetResulsts = spyOn(ArchiveUnitServiceStub, 'getResults').and.callFake(
      () => Observable.of({$results: [auNoParentsNoChildren, auNoParentsNoChildren, auNoParentsNoChildren,
        auNoParentsNoChildren, auNoParentsNoChildren, auNoParentsNoChildren]})
    );
    let node = new TreeNode('Test Node', 'id', new NodeData('INGEST', ['idUp']));

    ArchiveTreeViewComponent.getChildren(ArchiveUnitServiceStub, node);

    expect(spyOnGetResulsts.calls.count()).toEqual(1);
    expect(node.children.length).toBe(5);
    expect(node.data.haveMoreChildren).toBeTruthy();
  });

  it('should not set haveMoreChildren when 5 or less children are found', () => {
    const spyOnGetResulsts = spyOn(ArchiveUnitServiceStub, 'getResults').and.callFake(
      () => Observable.of({$results: [auNoParentsNoChildren, auNoParentsNoChildren, auNoParentsNoChildren,
        auNoParentsNoChildren, auNoParentsNoChildren]})
    );
    let node = new TreeNode('Test Node', 'id', new NodeData('INGEST', ['idUp']));

    ArchiveTreeViewComponent.getChildren(ArchiveUnitServiceStub, node);

    expect(spyOnGetResulsts.calls.count()).toEqual(1);
    expect(node.children.length).toBe(5);
    expect(node.data.haveMoreChildren).toBeFalsy();
  });

  it('should set the correct number of parents', () => {
    const spyOnGetResulsts = spyOn(ArchiveUnitServiceStub, 'getResults').and.callFake(
      () => Observable.of({$results: [auNoParentsNoChildren, auWithParents, auWithChildren]})
    );

    let node = new TreeNode('Test Node', 'id', new NodeData('INGEST', ['idUp']));

    ArchiveTreeViewComponent.getParents(ArchiveUnitServiceStub, node);

    expect(spyOnGetResulsts.calls.count()).toEqual(1);
    expect(node.parents.length).toBe(3);
  });

  it('should set the parent node as leaf when no sup-parents', () => {
    const spyOnGetResulsts = spyOn(ArchiveUnitServiceStub, 'getResults').and.callFake(
      () => Observable.of({$results: [auNoParentsNoChildren]})
    );
    let node = new TreeNode('Test Node', 'id', new NodeData('INGEST', ['idUp']));

    ArchiveTreeViewComponent.getParents(ArchiveUnitServiceStub, node);

    expect(spyOnGetResulsts.calls.count()).toEqual(1);
    expect(node.parents.length).toBe(1);
    expect(node.parents[0].leaf).toBeTruthy();
  });

  it('should set haveMoreParents when more than 5 parents are found', () => {
    const spyOnGetResulsts = spyOn(ArchiveUnitServiceStub, 'getResults').and.callThrough();
    let node = new TreeNode('Test Node', 'id',
      new NodeData('INGEST', ['p1', 'p2', 'p3', 'p4', 'p5', 'p6', 'p7'])
    );

    ArchiveTreeViewComponent.getParents(ArchiveUnitServiceStub, node);

    expect(spyOnGetResulsts.calls.count()).toEqual(1);
    expect(node.data.haveMoreParents).toBeTruthy();
  });

  it('should not set haveMoreParents when 5 or less parents are found', () => {
    const spyOnGetResulsts = spyOn(ArchiveUnitServiceStub, 'getResults').and.callThrough();
    let node = new TreeNode('Test Node', 'id', new NodeData('INGEST', ['p1', 'p2', 'p3', 'p4', 'p5']));

    ArchiveTreeViewComponent.getParents(ArchiveUnitServiceStub, node);

    expect(spyOnGetResulsts.calls.count()).toEqual(1);
    expect(node.data.haveMoreParents).toBeFalsy();
  });
});
