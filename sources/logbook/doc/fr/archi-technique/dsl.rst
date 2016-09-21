DSL
***

Analyse
-------

Présentation
^^^^^^^^^^^^
| L'objet de cette analyse est de chercher quel pourrait être le langage de requête pour le journal.
| A noter : les requêtes doivent disposer de quelques critères libres.
|
| Plusieurs implémentations en ligne de mire possibles :
 
* Requêtes équivalentes à l'usage dans des collections REST classiques en URL, avec la contrainte VITAM (cela doit être dans le Body)
   + Exemple Projection Google : ?fields=url,object(content, attachments/url)
   + Exemple de recherche classique : ?name=napoli&type=chinese,japanese&zipcode=75*&sort=rating,name&desc=rating&range=0-9
* Requêtes dans le body permettant d'être un peu plus riche et notamment dans la composition :
   + Des classes "Expression" permettent de gérer les différents cas de recherche : AND/OR, Property=value, opérateurs autres (IN, NE, GT, GTE...), NOT...


Un exemple de classes "Expression" :

* Interface Expression;
   + Interface AExpression;
      - Abstract LogicalExpression;
         • Class AndExpression;
         • Class OrExpression;
      - Class PropertyExpression;
   + Interface BExpression;
      - Abstract OperatorExpression;
         • Class EqualExpression;
         • Class GreaterThanExpression;
         • ...
      - Class NotExpression;

Explication
^^^^^^^^^^^

| Une interface **Expression**.
| 2 interfaces **AExpression** et **BExpression**.
| 
| Une classe abstract **LogicalExpression** permettant de gérer les expressions logiques AND et OR.

.. code-block:: java

    LogicalExpression{
      Operator ope;//(ENUM)
      AExpression exp;
    }           
   
Les 2 classes implémentées sont *AndExpression* et *OrExpression*.
La classe **PropertyExpression** permet de gérer les requetes sur les champs à proprement parler.

.. code-block:: java

    PropertyExpression{
      String propertyName;
      BExpression exp;
    }

Une classe abstract **OperatorExpression** permettant de gérer les opérateurs IN, NE, GT, GTE...

.. code-block:: java

    OperatorExpression{
      Operator ope; //(ENUM)
      Scalar|Array value; //(Int, String...)
    }

Les classes implémentées sont entre autres *InExpression*, *GteExpression*....
La classe **NotExpression** permet de gérer les expressions NOT.

.. code-block:: java

    NotExpression{
      Operator ope; //(ENUM -> NOT)
      BExpression exp;
    }

Utilisation
^^^^^^^^^^^

| Classe Query pour y intégrer une expression

.. code-block:: java

    Query{
      Expression exp;
    }

| Classe SearchQuery pour y intégrer une liste de Query

.. code-block:: java

    SearchQuery{
      List<Query> queries;
    }

Conclusion
----------

| Il apparait clairement que - même s'il est compliqué - le DSL Vitam existant est très proche de l'analyse effectuée. Il pourra donc être utilisé pour la recherche dans le logbook, en adaptant les classes Query et Request (ou en adaptant les Helpers associés).
| 
| La réutilisation du même DSL va aussi dans le sens de la simplification du point de vue de l'utilisateur des API par l'uniformisation des DSL utilisés.
| 
| **La recommandation de l'étude porte donc sur la réutilisation du DSL Vitam destiné aux Units et ObjectGroups pour les Journaux.**
| 
| Néanmoins, il y aura quelques différences (pas de **roots**, ni de **depth**). Il y aura enfin un effort de refactoring à faire pour mutualiser ce qui doit l'être entre Metadata et Logbook.
