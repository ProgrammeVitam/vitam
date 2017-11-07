import {TranslateLoader} from "@ngx-translate/core";
import {Observable} from "rxjs/Observable";
import {HttpClient} from "@angular/common/http";
import {Inject, Injectable} from "@angular/core";

export class CustomLoader implements TranslateLoader {


  constructor(@Inject(HttpClient)  private http: HttpClient) {
  }

  getTranslation(lang: string): Observable<any> {
    return this.http.get('/ihm-demo/v1/api/messages/logbook');
  }
}
