import { UnitTest } from './unit-test';
import { TagInfo } from './tag-info';


export class Report {
  public NumberOfTestOK : number;
  public NumberOfTestKO : number;
  public Reports : UnitTest[];
  public Tags : TagInfo[];
}