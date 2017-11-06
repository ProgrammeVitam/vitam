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

  it ('should compute size correctly', () => {
    expect(ObjectsService.computeSize(124)).toBe('124 octets');
    expect(ObjectsService.computeSize(2981)).toBe('3 ko');
    expect(ObjectsService.computeSize(31902001)).toBe('32 Mo');
    expect(ObjectsService.computeSize(149003002001)).toBe('149 Go');
  });

  it ('should copy object correctly', () => {
    let item = {
      prop: 'value',
      array: [1,2],
      object: {ssProp: 'text'}
    };
    let copy = ObjectsService.clone(item);
    copy.prop = 'value2';
    copy.array.push(3);
    copy.object = {ssProp: 'text2'};
    expect(item.prop).toBe('value');
    expect(item.array.length).toBe(2);
    expect(item.object.ssProp).toBe('text');
  });
});
