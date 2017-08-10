import { IhmDemoPage } from './app.po';

describe('ihm-demo App', () => {
  let page: IhmDemoPage;

  beforeEach(() => {
    page = new IhmDemoPage();
  });

  it('should display welcome message', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('Welcome to vitam!!');
  });
});
