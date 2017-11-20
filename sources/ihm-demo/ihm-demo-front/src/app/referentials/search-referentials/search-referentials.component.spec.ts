import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { SearchReferentialsComponent } from './search-referentials.component';
import { BreadcrumbService } from "../../common/breadcrumb.service";
import { ReferentialsService } from "../referentials.service";

const ReferentialsServiceStub = {
  getResults: (id) => Observable.of({'$results': [{}]})
};

describe('SearchReferentialsComponent', () => {
  let component: SearchReferentialsComponent;
  let fixture: ComponentFixture<SearchReferentialsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule.withRoutes([
        { path: 'ingest/sip', component: SearchReferentialsComponent }
      ])
      ],
      providers: [
        BreadcrumbService,
        { provide: ReferentialsService, useValue: ReferentialsServiceStub }
      ],
      declarations: [ SearchReferentialsComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchReferentialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should check item status', () => {
    let item = {
      'status': 'ACTIVE'
    };
    expect(SearchReferentialsComponent.handleStatus(item.status)).toEqual('Actif');

    item.status = 'true';
    expect(SearchReferentialsComponent.handleStatus(item.status)).toEqual('Actif');

    item.status = 'INACTIVE';
    expect(SearchReferentialsComponent.handleStatus(item.status)).toEqual('Inactif');
  });
});
