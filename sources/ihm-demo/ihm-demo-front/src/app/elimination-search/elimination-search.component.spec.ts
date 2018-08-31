import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {EliminationSearchComponent} from './elimination-search.component';
import {NO_ERRORS_SCHEMA} from "@angular/core";
import {ArchiveUnitService} from "../archive-unit/archive-unit.service";
import {ReferentialsService} from "../referentials/referentials.service";
import {MySelectionService} from "../my-selection/my-selection.service";
import {Observable} from "rxjs";
import {VitamResponse} from "../common/utils/response";
import {Router} from "@angular/router";
import { BreadcrumbElement, BreadcrumbService } from '../common/breadcrumb.service';
import { ArchiveUnitHelper } from '../archive-unit/archive-unit.helper';

describe('EliminationSearchComponent', () => {
  let component: EliminationSearchComponent;
  let fixture: ComponentFixture<EliminationSearchComponent>;

  let ReferentialsServiceStub = {
    getRuleById: (rule) => Observable.of(new VitamResponse())
  };

  let MySelectionServiceStub = {
    deleteAllFromBasket: (EliminationOperationId) => { },
    addToSelectionWithoutTenant: (withChildren, ids, specificBasket) => { }
  };

  const RouterStub = {
    navigate: () => {}
  };

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [EliminationSearchComponent],
      providers: [
        ArchiveUnitHelper,
        BreadcrumbService,
        {
          provide: ArchiveUnitService, useValue: {}
        },
        {
          provide: ReferentialsService, useValue: ReferentialsServiceStub
        },
        {
          provide: MySelectionService, useValue: MySelectionServiceStub
        },
        {
          provide: Router, useValue: RouterStub
        }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EliminationSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
