import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, ChangeDetectorRef } from '@angular/core';
import { ArchiveUnitFacetComponent } from './archive-unit-facet.component';
import { ResourcesService } from '../../common/resources.service';
import { CookieService } from 'angular2-cookie/core';
import { HttpClient, HttpHandler } from '@angular/common/http';
import { DialogService } from '../../common/dialog/dialog.service';
import { Observable } from 'rxjs/Rx';

import { RouterTestingModule } from '@angular/router/testing';

describe('ArchiveUnitFacetComponent', () => {
  let component: ArchiveUnitFacetComponent;
  let fixture: ComponentFixture<ArchiveUnitFacetComponent>;
  let resourcesService: ResourcesService;

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
        ResourcesService,
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

  it('should use resourcesService', () => {
    const stubLanguages = ['fr', 'en'];
    resourcesService = TestBed.get(ResourcesService);
    const resourcesServiceSpy = spyOn(resourcesService, 'getLanguagesFile').and.returnValue(
      Observable.of(stubLanguages)
    );
    component.ngOnInit();

    expect(resourcesServiceSpy).toHaveBeenCalled();
    expect(resourcesServiceSpy.calls.count())
      .toBe(1, 'spy getLanguagesFile method was called once');

    expect(component.mapFacetField.size).toEqual(8);
    expect(component.titleLangFacets.length).toEqual(2);
    expect(component.titleLangFacets[0]).toEqual('Title_fr');
    expect(component.titleLangFacets[1]).toEqual('Title_en');
    expect(component.descriptionLangFacets.length).toEqual(2);
    expect(component.descriptionLangFacets[0]).toEqual('Description_fr');
    expect(component.descriptionLangFacets[1]).toEqual('Description_en');
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
    component.descriptionLangFacets = ['Description_fr', 'Description_en'];
    component.selectedFacets = ['LanguageDescFacet'];
    component.mapFacetField.set('Description_fr', 'Description_.fr');
    component.mapFacetField.set('Description_en', 'Description_.en');

    const expectedFilters = [{
      '$name': 'Description_fr',
      '$query':
        {
          '$exists': 'Description_.fr'
        }
    }, {
      '$name': 'Description_en',
      '$query':
        {
          '$exists': 'Description_.en'
        }
    }];
    component.searchFacets();

    expect(component.facets.length).toEqual(1);
    expect(component.facets[0].name).toEqual('LanguageDescFacet');
    expect(component.facets[0].facetType).toEqual('FILTERS');
    expect(component.facets[0].filters.length).toEqual(2);
    expect(component.facets[0].filters[0]).toEqual(expectedFilters[0]);
    expect(component.facets[0].filters[1]).toEqual(expectedFilters[1])

  });

});
