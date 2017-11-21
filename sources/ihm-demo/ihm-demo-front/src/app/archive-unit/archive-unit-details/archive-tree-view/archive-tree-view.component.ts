import {Component, OnInit, OnChanges, Input} from '@angular/core';
import {ArchiveUnitService} from "../../archive-unit.service";
import {TreeNode, NodeData} from './tree-node';

const LIMIT = 5;

@Component({
  selector: 'vitam-archive-tree-view',
  templateUrl: './archive-tree-view.component.html',
  styleUrls: ['./archive-tree-view.component.css']
})
export class ArchiveTreeViewComponent implements OnInit, OnChanges {
  @Input() unitId: string;
  @Input() unitUps: string[];
  @Input() type: string;
  @Input() title: string;
  root: TreeNode;

  constructor(private archiveUnitService: ArchiveUnitService) { }

  ngOnChanges(change) {
    if(change.unitId) {
      this.initRoot();
    }
  }

  ngOnInit() {
  }

  initRoot() {
    this.root = new TreeNode(this.title, this.unitId, new NodeData(this.type, this.unitUps || []));
    ArchiveTreeViewComponent.getParents(this.archiveUnitService, this.root);
    ArchiveTreeViewComponent.getChildren(this.archiveUnitService, this.root);
  }

  static getParents(archiveUnitService, node) {
    if (node.parents.length > 0) {
      return;
    }

    let unitups = [];

    if (node.data.unitups.length <= LIMIT) {
      unitups = node.data.unitups;
    } else {
      unitups = node.data.unitups.slice(0, LIMIT);
      node.data.haveMoreParents = true;
    }

    let criteria = {
      ROOTS: unitups,
      isAdvancedSearchFlag: "Yes",
      projection_title: 'Title',
      projection_id: '#id',
      projection_unitups: '#unitups',
      projection_allunitups: '#allunitups',
      projection_unitType: '#unittype'
    };

    archiveUnitService.getResults(criteria).subscribe(
      (response) => {

        let parents = [];
        for (let result of response.$results) {
          let data: NodeData = new NodeData(result._unitType, result['#unitups']);
          data.allunitups = result['#allunitups'];
          let parent = new TreeNode(result.Title, result['#id'], data);

          if (result['#allunitups'].length === 0) {
            parent.leaf = true;
          }

          ArchiveTreeViewComponent.getRootNode(archiveUnitService, parent);
          parents.push(parent);
        }

        node.parents = parents;
      }
    );
  }

  static getRootNode(archiveUnitService, node: TreeNode) {
    let query = {
      "$query": [
        {
          "$and": [
            {
              "$in": {
                "#id": node.data.allunitups
              }
            },
            {
              "$eq": {
                "#max": 1
              }
            }
          ]
        }
      ],
      "$projection": {}
    };
    archiveUnitService.getByQuery(query).subscribe(
      (response) => {
        let parents = [];
        for (let result of response.$results) {
          let data: NodeData = new NodeData(result._unitType, result['#unitups']);
          let parent = new TreeNode(result.Title, result['#id'], data);
          parents.push(parent);
        }

        node.roots = parents;
      }
    );
  }

  static getChildren(archiveUnitService, node) {

    if (node.leaf || node.children.length > 0) {
      return;
    }

    let criteria = {
      UNITUPS: node.id,
      isAdvancedSearchFlag: "Yes",
      projection_title: 'Title',
      projection_id: '#id',
      projection_unitups: '#unitups',
      projection_unitType: '#unittype',
      projection_nbunits: '#nbunits'
    };
    archiveUnitService.getResults(criteria).subscribe(
      (response) => {

        let children = [];
        let number = 0;
        if (response.$results.length === 0) {
          node.leaf = true;
        }

        for (let result of response.$results) {
          if (number == LIMIT) {
            node.data.haveMoreChildren = true;
            break;
          }
          let data: NodeData = new NodeData(result['_unitType'], result['#unitups']);
          data.nbUnits = result['#nbunits'];
          let child = new TreeNode(result.Title, result['#id'], data);

          if (result['#nbunits'] === 0) {
            child.leaf = true;
          }

          children.push(child);
          number++;
        }

        node.children = children;
      }
    );
  }

}
