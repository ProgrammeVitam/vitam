import {Component, EventEmitter, Input, OnInit, Output, SimpleChanges} from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FieldDefinition } from './field-definition';
import { Preresult } from './preresult';
import {AccessContractService} from "../access-contract.service";

@Component({
  selector: 'vitam-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.css']
})

export class SearchComponent implements OnInit {
  searchForm: FormGroup;
  advancedSearchForm: FormGroup;
  @Input() advancedMode = false;
  preSearchReturn = new Preresult();
  allowAdvanced = false;
  @Input() public label: string;
  @Input() public data: FieldDefinition[] = [];
  @Input() public panelButtonlabel: string;
  @Input() public advancedData: FieldDefinition[];
  @Input() public submitFunction: (service: any, emitter: EventEmitter<any>, request: any) => void;
  @Input() preSearch: (request: any, advancedMode?: boolean) => Preresult = (x) => x;
  @Input() service: any;
  @Output() responseEvent: EventEmitter<any> = new EventEmitter<any>();
  @Output() panelButtonEvent: EventEmitter<any> = new EventEmitter<any>();



  frLocale = {
    dayNames: ["Dimanche","Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"],
    dayNamesShort: ["Dim.", "Lun.", "Mar.", "Mer.", "Jeu.", "Ven.", "Sam."],
    dayNamesMin: ["Di","Lu","Ma","Me","Je","Ve","Sa"],
    monthNames: [ "Janvier","Février","Mars","Avril","Mai","Juin","Juillet","Aout","Septembre","Octobre","Novembre","Décembre" ],
    monthNamesShort: [ "Jan", "Fév", "Mars", "Avr", "Mai", "Juin","Juil", "Aou", "Sep", "Oct", "Nov", "Dec" ],
    firstDayOfWeek: 1,
    today: "Aujourd'hui",
    clear: 'Vider'
  };

  constructor(private accessContractService: AccessContractService) {
    this.accessContractService.getUpdate().subscribe(
      () => {
        if (this.advancedData && this.submitFunction) {
          this.onSubmit();
        }
      }
    );
  }

  ngOnInit() {
    if (this.advancedData) {
      this.allowAdvanced = true;
    }
    this.initForm();
  }

  initForm() {
    if (this.advancedData) {
      const advancedItems = {};
      for (let i = 0; i < this.advancedData.length; i++) {
        if (this.advancedData[i].required) {
          advancedItems[this.advancedData[i].name] = new FormControl('', Validators.required);
        } else {
          advancedItems[this.advancedData[i].name] = new FormControl('');
        }
      }
      this.advancedSearchForm = new FormGroup(advancedItems);
    }

    const item = {};
    for (let i = 0; i < this.data.length; i++) {
      if (this.data[i].required) {
        item[this.data[i].name] = new FormControl('', Validators.required);
      } else {
        item[this.data[i].name] = new FormControl('');
      }
    }
    this.searchForm = new FormGroup(item);
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.data) {
      let data = changes.data.currentValue;
      const item = {};
      for (let i = 0; i < data.length; i++) {
        if (data[i].required) {
          item[data[i].name] = new FormControl('', Validators.required);
        } else {
          item[data[i].name] = new FormControl('');
        }
      }
      this.searchForm = new FormGroup(item);
    }
  }

  onSubmit() {
    this.checkAndSubmit(this.advancedMode ? this.advancedSearchForm : this.searchForm);
  }

  checkAndSubmit(form) {
    if (form.valid) {
      let body = {};
      Object.keys( form.controls).forEach(key => {
        body[key] = form.controls[key].value;
      });
      let preSearchResult: Preresult = this.processPreSearch(body);
      if (preSearchResult.success) {
        this.submitFunction(this.service, this.responseEvent, preSearchResult.request);
      } else {
        this.preSearchReturn.searchProcessError = preSearchResult.searchProcessError;
      }
    } else {
      this.preSearchReturn.searchProcessError = 'Champ(s) requis non rempli(s)';
    }
  }

  processPreSearch(request: any) {
    this.preSearchReturn = this.preSearch(request, this.advancedMode);
    return this.preSearchReturn;
  }

  clearFields() {
    this.searchForm.reset();
    this.searchForm.enable();
    if (this.advancedSearchForm) {
      this.advancedSearchForm.reset();
      this.advancedSearchForm.enable();
    }
    this.preSearchReturn = new Preresult();
    this.onSubmit();
  }

  disableOthers(field: FieldDefinition) {
    if (!this.advancedMode) {
      if (field.disableOtherFields && this.searchForm.get(field.name).value) {
        this.searchForm.disable();
        this.searchForm.get(field.name).enable();
      } else {
        this.searchForm.enable();
      }
    } else {
      if (field.disableOtherFields && this.advancedSearchForm.get(field.name).value) {
        this.advancedSearchForm.disable();
        this.advancedSearchForm.get(field.name).enable();
      } else {
        this.advancedSearchForm.enable();
      }
    }
  }

  switchMode(isAdvanced: boolean) {
    this.advancedMode = isAdvanced;
  }

  clickPanelButton() {
    this.panelButtonEvent.emit();
  }
}
