import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LogbookComponent } from './logbook.component';
import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { Observable } from 'rxjs/Observable';
import { LogbookService } from './logbook.service';
import {GrowlModule} from 'primeng/primeng';

let value: BreadcrumbElement[];

const BreadcrumbServiceStub = {
  changeState: (myChange: BreadcrumbElement[]) => {
    value = myChange;
  },
  getState: () => Observable.of(value)
};
let response = {status : 200};

const LogbookServiceStub = {
  launchTracability: () => Observable.of(response)
};

describe('LogbookComponent', () => {
  let component: LogbookComponent;
  let fixture: ComponentFixture<LogbookComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LogbookComponent ],
      imports: [GrowlModule],
      providers: [
        { provide: BreadcrumbService, useValue: BreadcrumbServiceStub },
        { provide: LogbookService, useValue: LogbookServiceStub },
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogbookComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('launch traceability ok', () => {
    expect(component).toBeTruthy();
    component.launchTraceability();
    expect(component.messages).toEqual([{severity: 'info', summary: 'Sécurisation', detail: 'Succès de l\'opération de sécurisation des journaux'}]);
  });

  it('launch traceability ko', () => {
    response = { status : 500 };
    component.launchTraceability();
    expect(component.messages).toEqual([{severity: 'error', summary: 'Sécurisation', detail: `Echec de l'opération de sécurisation des journaux`}]);
  });
});
