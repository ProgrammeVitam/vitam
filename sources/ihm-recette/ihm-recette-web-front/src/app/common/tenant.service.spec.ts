import { TestBed, inject } from '@angular/core/testing';
import { TenantService } from "./tenant.service";


describe('TenantService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TenantService]
    });
  });

  it('should be created', inject([TenantService], (service: TenantService) => {
    expect(service).toBeTruthy();
  }));
});
