DirectedCycle
#############

	Vitam utilise DirectedCycle pour verifier la structure des arbres et de s'assurer qu'on n' a pas une cycle dans le graphe.
	
Initialisation
**************

	Pour initialiser un objet DirectedCycle, il faut instancier un objet DirectedGraph à partir d'un fichier Json (vous trouvrez ci-dessous un exemple).

.. code-block:: java	
        	
                File file = PropertiesUtils.getResourcesFile("ingest_acyc.json");
                JsonNode json = JsonHandler.getFromFile(file); 
                DirectedGraph g = new DirectedGraph(json); 
                DirectedCycle graphe = new DirectedCycle(g);

Usage
*****

Pour vérifier la présence d'un cycle dans le graphe

.. code-block:: java

              graphe..isCyclic() ;

La methode isCyclic return true si on a un cycle.


Exemple de fichier json: ingest_acyc.json


{
  "ID027" : { },  "ID028" : { "_up" : [ "ID027" ]},
  "ID029" : {"_up" : [ "ID028" ]},
  "ID030" : {"_up" : [ "ID027" ]},"ID032" : {"_up" : [ "ID030", "ID029" ] },
  "ID031" : {"_up" : [ "ID027" ]}

}

Remarque
********

Pour Vitam, fonctionnellement il ne faut pas trouver des cycles au niveau des arbres des units. (au niveau de bordoreau)

====================================================================================

Graph
#############

	Vitam utilise Graph pour determiner l'order d'indexation en se basant sur la notion de chemin le plus long (longest path)
	
Initialisation
**************

	Pour initialiser un objet Graph:

.. code-block:: java	
        	
        File file = PropertiesUtils.getResourcesFile("ingest_tree.json");
        JsonNode json = JsonHandler.getFromFile(file);
        Graph graph = new Graph(json);

Usage
*****

Pour determiner l'order il faut avoir le chemin le plus plong par rapport aux différents racines :

.. code-block:: java

              graph.getGraphWithLongestPaths()

La methode getGraphWithLongestPaths return un map qui contient l'order on key et la liste (Set) des units id en valeur


Exemple de resultat:


{0=[ID027], 1=[ID030, ID031, ID028], 2=[ID029], 3=[ID032]}
