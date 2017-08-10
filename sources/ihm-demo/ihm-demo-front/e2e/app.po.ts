import { browser, by, element } from 'protractor';

export class IhmDemoPage {
  navigateTo() {
    return browser.get('/');
  }

  getParagraphText() {
    return element(by.css('vitam-root h1')).getText();
  }
}
