import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {NO_ERRORS_SCHEMA} from "@angular/core";

import {GenericTableComponent} from './generic-table.component';
import {Observable} from "rxjs/Observable";
import {TranslateLoader, TranslateModule} from "@ngx-translate/core";
import {CustomLoader} from "../translate/custom-loader";

describe('GenericTableComponent', () => {
  let component: GenericTableComponent;
  let fixture: ComponentFixture<GenericTableComponent>;


  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule,
        TranslateModule.forRoot({
          loader: {provide: TranslateLoader, useClass: CustomLoader}
        })],
      declarations: [GenericTableComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(GenericTableComponent);
    component = fixture.componentInstance;

    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
