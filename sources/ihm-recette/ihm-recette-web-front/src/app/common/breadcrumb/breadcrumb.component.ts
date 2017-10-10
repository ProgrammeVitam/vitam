import { Component, OnInit } from '@angular/core';
import {BreadcrumbService} from '../breadcrumb.service';
import { AuthenticationService } from '../../authentication/authentication.service';

@Component({
  selector: 'vitam-breadcrumb',
  templateUrl: './breadcrumb.component.html',
  styleUrls: ['./breadcrumb.component.css']
})
export class BreadcrumbComponent implements OnInit {
  isAuthenticated: boolean;
  items = [];
  constructor(private breadcrumbService: BreadcrumbService, private authenticationService: AuthenticationService) {
    this.items = [];
    this.breadcrumbService.getState().subscribe(
      (breadcrumb) => {
        this.items = breadcrumb;
      }
    );
  }

  ngOnInit() {
    this.authenticationService.getState().subscribe((value) => {
      if (value) {
        this.isAuthenticated = true;
      } else {
        this.isAuthenticated = false;
      }
    });
  }

}
