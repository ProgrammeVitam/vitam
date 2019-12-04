import {Component} from '@angular/core';
import {EggService} from "./egg.service";

@Component({
  selector: 'vitam-egg',
  templateUrl: './egg.component.html',
  styleUrls: ['./egg.component.css']
})

export class EggComponent {

    constructor(public eggService: EggService) {}
}


