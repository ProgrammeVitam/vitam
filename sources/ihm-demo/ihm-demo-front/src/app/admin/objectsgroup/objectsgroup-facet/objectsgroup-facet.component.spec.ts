import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, ChangeDetectorRef } from '@angular/core';
import { ObjectsGroupFacetComponent } from './objectsgroup-facet.component';
import { CookieService } from 'angular2-cookie/core';
import { HttpClient, HttpHandler } from '@angular/common/http';
import { DialogService } from '../../../common/dialog/dialog.service';

import { RouterTestingModule } from '@angular/router/testing';

describe('ObjectsGroupFacetComponent', () => {
  let component: ObjectsGroupFacetComponent;
  let fixture: ComponentFixture<ObjectsGroupFacetComponent>;

  const mapFacetField = new Map()
    .set('OriginatingAgencyFacet', '#originating_agency')
    .set('FormatIdFacet', '#qualifiers.versions.FormatIdentification.FormatLitteral')
    .set('UsageFacet', '#qualifiers.versions.DataObjectVersion');

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [ObjectsGroupFacetComponent],
      providers: [
        ChangeDetectorRef,
        CookieService,
        HttpClient,
        HttpHandler,
        DialogService,
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));


  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ObjectsGroupFacetComponent]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ObjectsGroupFacetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize facets', () => {
    component.ngOnInit();
    expect(component.mapFacetField.size).toEqual(3);
  });

  it('should construct an array of facets with different types', () => {
    component.selectedFacets = ['OriginatingAgencyFacet', 'FormatIdFacet', 'UsageFacet'];
    component.searchFacets();
    expect(component.facets.length).toEqual(3);
    expect(component.facets[0].name).toEqual('OriginatingAgencyFacet');
    expect(component.facets[0].facetType).toEqual('TERMS');
    expect(component.facets[1].name).toEqual('FormatIdFacet');
    expect(component.facets[1].facetType).toEqual('TERMS');
    expect(component.facets[2].name).toEqual('UsageFacet');
    expect(component.facets[2].facetType).toEqual('TERMS');

    component.clearFacetResults();
    expect(component.facets).toEqual(undefined);
  });
});