import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FacetsComponent } from './facets.component';
import {NO_ERRORS_SCHEMA} from "@angular/core";

describe('FacetsComponent', () => {
  let component: FacetsComponent;
  let fixture: ComponentFixture<FacetsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FacetsComponent ],
        schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FacetsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
