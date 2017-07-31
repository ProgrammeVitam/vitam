import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RemoveItemsComponent } from './remove-items.component';
import {CollectionService} from "../collection.service";
import {Observable} from "rxjs/Observable";
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';
import {DialogModule, PanelModule} from "primeng/primeng";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";

let CollectionServiceStub = {
  removeItemsInCollection: (api) => Observable.of('OK'),
};

describe('RemoveItemsComponent', () => {
  let component: RemoveItemsComponent;
  let collectionService: CollectionService;
  let fixture: ComponentFixture<RemoveItemsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RemoveItemsComponent ],
      providers: [
        { provide: CollectionService, useValue: CollectionServiceStub }
      ],
      imports: [PanelModule, DialogModule, BrowserAnimationsModule]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RemoveItemsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    component.api = 'test';
    component.label = 'test';
    component.name = 'test';
  });

  afterEach(() => {
    component.displayCheck = false;
    component.displayResult = false;
    component.success = false;
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should display validation message on delete', () => {
    expect(component.displayCheck).toBeFalsy();
    component.removeItems();
    expect(component.displayCheck).toBeTruthy();
  });

  it('should display success message on validation if delete is OK', () => {
    component.displayCheck = true;
    expect(component.success).toBeFalsy();
    component.doRemove();
    expect(component.success).toBeTruthy();
    expect(component.displayCheck).toBeFalsy();
  });

  it('should display error message on validation if delete is KO', () => {
    component.displayCheck = true;
    collectionService = fixture.debugElement.injector.get(CollectionService);
    spyOn(collectionService, 'removeItemsInCollection').and.returnValue(Observable.throw('error'));
    component.doRemove();
    expect(component.success).toBeFalsy();
    expect(component.displayCheck).toBeFalsy();
  });
});
