import { Component, OnInit } from '@angular/core';
import {BreadcrumbService} from '../breadcrumb.service';

@Component({
  selector: 'vitam-breadcrumb',
  templateUrl: './breadcrumb.component.html',
  styleUrls: ['./breadcrumb.component.css']
})
export class BreadcrumbComponent implements OnInit {
  items = [];
  constructor(private breadcrumbService: BreadcrumbService) {
    this.items = [];
    this.breadcrumbService.getState().subscribe(
      (breadcrumb) => {
        this.items = breadcrumb;
      }
    );
  }

  ngOnInit() {
  }

}
