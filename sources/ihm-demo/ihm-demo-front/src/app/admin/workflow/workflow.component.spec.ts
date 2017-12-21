import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {WorkflowComponent} from './workflow.component';
import {NO_ERRORS_SCHEMA} from "@angular/core";
import {WorkflowService} from "../workflow.service";
import {Observable} from "rxjs/Observable";
import {VitamResponse} from "../../common/utils/response";
import {BreadcrumbService} from "../../common/breadcrumb.service";

describe('WorkflowComponent', () => {
  let component: WorkflowComponent;
  let fixture: ComponentFixture<WorkflowComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        WorkflowComponent
      ],
      providers: [
        BreadcrumbService,
        {provide: WorkflowService, useValue: WorkflowServiceStub}
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  let WorkflowServiceStub = {
    getOperations: (body) => Observable.of(new VitamResponse()),

    sendOperationsAction: (id: string, body: any, action: string) => Observable.of(new VitamResponse()),

    stopOperation: (id: string, body: any) => Observable.of(new VitamResponse()),

    getWorkflowsDefinition: () => Observable.of({$results: []})
  };

  beforeEach(() => {
    fixture = TestBed.createComponent(WorkflowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should search for initial states if given', () => {
    component.searchForm = {
      "states": ["PAUSE", "RUNNING"]
    };

    let spy = spyOn(component.workflowService, 'getOperations').and.callFake(
      () => Observable.of('Data')
    );

    component.refreshButton();

    expect(spy.calls.count()).toBe(1);
    expect(spy.calls.argsFor(0)[0].states.length).toBe(2);
  });

  it('should not restore the initial states when not given', () => {
    component.searchForm = {};

    let spy = spyOn(component.workflowService, 'getOperations').and.callFake(
      () => Observable.of('Data')
    );

    component.refreshButton();

    expect(spy.calls.count()).toBe(1);
    expect(spy.calls.argsFor(0)[0].states).toBeUndefined();
  });
});
