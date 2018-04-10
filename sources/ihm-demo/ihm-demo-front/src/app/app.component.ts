import {Component, HostListener} from '@angular/core';
import { Router, NavigationStart, NavigationEnd, ActivatedRoute } from '@angular/router';
import 'rxjs/add/operator/filter';

import { AuthenticationService } from './authentication/authentication.service';
import { TranslateService } from '@ngx-translate/core';


@Component({
  selector: 'vitam-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'vitam';
  displayGoToTop = false;
  constructor(private router : Router, private authenticationService : AuthenticationService,
              private activatedRoute : ActivatedRoute, private translate: TranslateService) {
    router.events
      .filter(event => event instanceof NavigationEnd)
      .subscribe((event : NavigationStart) => {
        let permission = activatedRoute.firstChild.snapshot.data.permission;
        if (permission && !this.authenticationService.checkUserPermission(permission)) {
          this.router.navigate(['ingest/sip']);
        }
      });
    translate.setDefaultLang('fr');
  }

  scrollToTop() {
    window.scroll(0,0);
  }

  @HostListener('window:scroll', ['$event'])
  doSomething() {
    this.displayGoToTop = window.pageYOffset !== 0;
  }

}
