Lifecycle
*******************

Presentation
------------

|	*Parent package:* **fr.gouv.vitam.logbook**
|	*Package proposition:* **fr.gouv.vitam.logbook.lifecycle.client**

Module pour les logs lifecycle : api / rest.

Services
--------

Rest API
--------

| http://server/app/v1
| POST /operations/{id_op}/lifecycles/ -> **POST un lifecyle sur une opération**
| PUT /operations/{id_op}/lifecycles/{id_li} -> **Append sur un lifecycle existant**
| GET  /lifecycle -> **Administration du licycle **
