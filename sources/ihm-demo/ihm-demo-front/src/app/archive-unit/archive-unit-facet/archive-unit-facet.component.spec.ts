import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, ChangeDetectorRef } from '@angular/core';
import { ArchiveUnitFacetComponent } from './archive-unit-facet.component';
import { CookieService } from 'angular2-cookie/core';
import { HttpClient, HttpHandler } from '@angular/common/http';
import { DialogService } from '../../common/dialog/dialog.service';
import { Observable } from 'rxjs/Rx';

import { RouterTestingModule } from '@angular/router/testing';

describe('ArchiveUnitFacetComponent', () => {
  let component: ArchiveUnitFacetComponent;
  let fixture: ComponentFixture<ArchiveUnitFacetComponent>;

  const mapFacetField = new Map()
    .set('DescriptionLevelFacet', 'DescriptionLevel')
    .set('OriginatingAgencyFacet', '#originating_agency')
    .set('StartDateFacet', 'StartDate')
    .set('endDateFacet', 'EndDate');

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [ArchiveUnitFacetComponent],
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
      declarations: [ArchiveUnitFacetComponent]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveUnitFacetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize facets', () => {
    component.ngOnInit();
    expect(component.mapFacetField.size).toEqual(41);
    expect(component.titleLangFacets.length).toEqual(35);
    expect(component.titleLangFacets[0]).toEqual('Title_ar');
    expect(component.titleLangFacets[1]).toEqual('Title_bn');

  });

  it('should construct an array of facets with different types', () => {
    component.selectedFacets = ['DescriptionLevelFacet', 'StartDateFacet', 'LanguageTitleFacet', 'OriginatingAgencyFacet'];
    component.searchFacets();
    expect(component.facets.length).toEqual(4);
    expect(component.facets[0].name).toEqual('DescriptionLevelFacet');
    expect(component.facets[0].facetType).toEqual('TERMS');
    expect(component.facets[1].name).toEqual('StartDateFacet');
    expect(component.facets[1].facetType).toEqual('DATE_RANGE');
    expect(component.facets[2].name).toEqual('LanguageTitleFacet');
    expect(component.facets[2].facetType).toEqual('FILTERS');
    expect(component.facets[3].name).toEqual('OriginatingAgencyFacet');
    expect(component.facets[3].facetType).toEqual('TERMS');

    component.clearFacetResults();
    expect(component.facets).toEqual(undefined);
  });

  it('should use filters query', () => {
    component.descriptionLangFacets = ['Title_ar', 'Title__bn'];
    component.selectedFacets = ['LanguageTitleFacet'];
    component.mapFacetField.set('Title_ar', 'Title_.ar');
    component.mapFacetField.set('Title_bn', 'Title_.bn');

    const expectedFilters = [{
      '$name': 'Title_ar',
      '$query':
        {
          '$exists': 'Title_.ar'
        }
    }, {
      '$name': 'Title_bn',
      '$query':
        {
          '$exists': 'Title_.bn'
        }
    }];
    component.searchFacets();

    expect(component.facets.length).toEqual(1);
    expect(component.facets[0].name).toEqual('LanguageTitleFacet');
    expect(component.facets[0].facetType).toEqual('FILTERS');
    expect(component.facets[0].filters.length).toEqual(35);
    expect(component.facets[0].filters[0]).toEqual(expectedFilters[0]);
    expect(component.facets[0].filters[1]).toEqual(expectedFilters[1])

  });

});
