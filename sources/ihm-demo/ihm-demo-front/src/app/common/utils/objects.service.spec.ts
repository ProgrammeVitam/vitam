import { TestBed, inject } from '@angular/core/testing';

import { ObjectsService } from './objects.service';

describe('ObjectsService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ObjectsService]
    });
  });

  it('should be created', inject([ObjectsService], (service: ObjectsService) => {
    expect(service).toBeTruthy();
  }));
});
