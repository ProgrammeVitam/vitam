import {Component, Input, OnInit} from '@angular/core';
import {CollectionService} from '../collection.service';

@Component({
  selector: 'vitam-remove-items',
  templateUrl: './remove-items.component.html',
  styleUrls: ['./remove-items.component.css']
})
export class RemoveItemsComponent implements OnInit {

  @Input() public label: string;
  @Input() public api: string;
  @Input() public name: string;
  public displayCheck = false;
  public displayResult = false;
  public success = false;

  constructor(private collectionService: CollectionService) { }

  ngOnInit() {
  }

  removeItems() {
    this.displayCheck = true;
  }

  displayResults(success) {
    this.displayResult = true;
    this.success = success;
  }

  doRemove() {
    this.displayCheck = false;
    this.collectionService.removeItemsInCollection(this.api)
      .subscribe(
        () => this.displayResults(true),
        (error) => this.displayResults(false)
      );
  }

}
