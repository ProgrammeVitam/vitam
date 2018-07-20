import {async, ComponentFixture, TestBed} from "@angular/core/testing";
import {NO_ERRORS_SCHEMA} from "@angular/core";

import {ResultsComponent} from "./results.component";
import {Observable} from "rxjs/Observable";
import {Hits, VitamResponse} from "../utils/response";
import 'rxjs/add/observable/of';
import {MySelectionService} from "../../my-selection/my-selection.service";
import {ResourcesService} from "../resources.service";

const MySelectionServiceStub = {
  addToSelection: (child: boolean, ids: string[], tenant) => {}
};

const ResourceServiceStub = {
  getTenant: () => "0"
};

describe('ResultsComponent', () => {
  let component: ResultsComponent;
  let fixture: ComponentFixture<ResultsComponent>;

  let eventStub = {
    first: 0,
    page: 0,
    pageCount: 7,
    rows: 25
  };

  let searchFunctionStub = function (service: any, offset: number, rows?: number, searchScope?: any) {
    let vitamResponse = new VitamResponse();
    vitamResponse.$results = [{
      "#id": "aeaqaaaaaaeukucxaa5v2ak7lvdjpqiaaaaq",
      "#object": "aebaaaaaaaeukucxaa5v2ak7lvdjo2qaaaaq",
      "#originating_agency": "Identifier0",
      "#tenant": 0,
      "#unitType": "INGEST",
      "#version": 0,
      AcquiredDate: "2016-01-05T09:31:00",
      DescriptionLevel: "Item",
      EndDate: "1918-12-31T19:06:44",
      RegisteredDate: "2013-05-25T09:31:00",
      StartDate: "1914-01-01T09:31:00",
      Title: "Great War Ruins. - Anizy-le-Château. - The Parade. - LL.",
      TransactedDate: "2017-06-01T09:31:00"
    }];
    return Observable.of(vitamResponse);
  };

  let addhits = () => {
    let hits = new Hits();
    hits.limit = 5;

    return hits;
  };


  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ResultsComponent],
      providers: [
        { provide: MySelectionService, useValue: MySelectionServiceStub },
        { provide: ResourcesService, useValue: ResourceServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));


  beforeEach(() => {
    fixture = TestBed.createComponent(ResultsComponent);
    component = fixture.componentInstance;
    component.searchFunction = searchFunctionStub;
    component.hits = addhits();
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });


  it('should  have the good number of page for rowsPerPageOptions', () => {
    component.paginate(eventStub);
    expect(eventStub.rows).toEqual(component.nbRows);
    expect(eventStub.page).toEqual(component.firstPage)
  });
});
