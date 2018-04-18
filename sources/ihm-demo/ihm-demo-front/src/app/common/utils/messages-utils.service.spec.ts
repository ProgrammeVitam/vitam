import { TestBed, inject } from '@angular/core/testing';

import { MessagesUtilsService } from './messages-utils.service';

describe('MessagesUtilsService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [MessagesUtilsService]
    });
  });

  it('should be created', inject([MessagesUtilsService], (service: MessagesUtilsService) => {
    expect(service).toBeTruthy();
  }));

  it ('should get messages properly', inject([MessagesUtilsService], (service: MessagesUtilsService) => {
    const errorWithCorrectCode = {error: "{\"code\": \"020121\"}"};
    const nonParsableError = "Unparsable Error";
    const errorWithWrongCode = {error: "{\"code\": \"Fake Message\"}"};

    expect(service.getMessage(errorWithCorrectCode)).toBe('Le format du fichier ne correspond pas au format attendu.');
    expect(service.getMessage(errorWithWrongCode)).toBe('');
    expect(service.getMessage(nonParsableError)).toBe('');
  }));

});
