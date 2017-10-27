import { Component } from '@angular/core';
import { Router, NavigationStart, NavigationEnd, ActivatedRoute } from '@angular/router';
import 'rxjs/add/operator/filter';

import { AuthenticationService } from './authentication/authentication.service';


@Component({
  selector: 'vitam-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'vitam';
  constructor(private router : Router, private authenticationService : AuthenticationService,
              private activatedRoute : ActivatedRoute) {
    router.events
      .filter(event => event instanceof NavigationEnd)
      .subscribe((event : NavigationStart) => {
        let permission = activatedRoute.firstChild.snapshot.data.permission;
        if (permission && !this.authenticationService.checkUserPermission(permission)) {
          this.router.navigate(['ingest/sip']);
        }
      });
  }
}
