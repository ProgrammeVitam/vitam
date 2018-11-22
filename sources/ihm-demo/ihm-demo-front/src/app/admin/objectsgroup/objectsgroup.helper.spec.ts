import { TestBed, inject } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';

import { ObjectsGroupHelper } from './objectsgroup.helper';

describe('ObjectsGroupHelper', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ObjectsGroupHelper],
      schemas: [NO_ERRORS_SCHEMA]
    });
  });

  it('should be created', inject([ObjectsGroupHelper], (service: ObjectsGroupHelper) => {
    expect(service).toBeTruthy();
  }));

  it('should extract document Format', inject([ObjectsGroupHelper], (service: ObjectsGroupHelper) => {
    const ogData = {
      "#qualifiers": [
        {
            "versions": [
                {
                    "FormatIdentification": {
                        "FormatId": "fmt/219"
                    }
                }
            ]
        }
      ]
    };
    expect(service.getFormat(ogData)).toBe('fmt/219');
  }));

it('should extract document Usage', inject([ObjectsGroupHelper], (service: ObjectsGroupHelper) => {
    const ogData = {
      "#qualifiers": [
        {
            "versions": [
                {
                    "DataObjectVersion": "BinaryMaster_1"
                }
            ]
        }
      ]
    };
    expect(service.getUsage(ogData)).toBe('BinaryMaster_1');
  }));

it('should extract document Size', inject([ObjectsGroupHelper], (service: ObjectsGroupHelper) => {
    const ogData = {
      "#qualifiers": [
        {
            "versions": [
                {
                    "Size": 20000
                }
            ]
        }
      ]
    };
    expect(service.getSize(ogData)).toBe(20000);
  }));

it('should extract the titles of the units to which the document is linked', inject([ObjectsGroupHelper], (service: ObjectsGroupHelper) => {
    const ogData = {
      "UnitsTitle": [
        "title_1", "title_2", "title_3"
      ]
    };
    expect(service.getAuTitles(ogData)).toBe("title_1 | title_2 | title_3");
  }));

});