import { Component } from '@angular/core';
import { CookieService } from "angular2-cookie/core";
import { Router, NavigationStart, NavigationEnd } from '@angular/router';
import 'rxjs/add/operator/filter';


@Component({
  selector: 'vitam-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})

export class AppComponent {
  title = 'vitam';
  constructor(private router : Router, private cookies: CookieService) {
    router.events
      .filter(event => event instanceof NavigationEnd)
      .subscribe((event : NavigationStart) => {
        if (localStorage.getItem("loggedIn") !== 'true') {
          this.router.navigate(["login"]);
        }
      });

  }
}
