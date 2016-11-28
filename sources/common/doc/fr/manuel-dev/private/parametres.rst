Paramètres
##########

Présentation
------------
Dans tout le projet Vitam sont utilisés différents paramètres transmis aux différentes classes ou au différentes
méthodes. Afin de ne pas bloquer toute évolution, il est recommandé d'utiliser une classe de paramètres (afin d'éviter
 de modifier le nombre de paramètres en signature de méthodes) ou d'utiliser une Map.

Principe
--------
L'idée ici est de mettre en place une mécanique de paramètres commune à tous les modules Vitam. Pour se faire, une
interface VitamParameter a été créée.
Afin de créer une nouvelle classe de paramètre, il faut alors implémenter cette interface qui retourne une Map de
paramètre et un Set de noms de paramètre obligatoires.
Cette interface est générique et prend comme typage une énum qui dispose du nom des paramètres.

Une classe utilitaire, ParameterHelper a été mise en place afin de vérifier les champs obligatoires. Elle s'appuie
sur les deux méthodes définies dans l'interface VitamParameter.

Mise en place
-------------
Nom des paramètres
******************
Nous souhaitons mettre en place une classe de paramètre pour le module storage, StorageParameter.
Il faut dans un premier temps une énum disposant des noms de paramètre.

.. code-block:: java

    public enum StorageParameterName {
        /**
        * Nom du premier paramètre
        **/
        field1,
        /**
        * Nom du deuxième paramètre
        **/
        field2,
        /**
        * Nom du troisième paramètre
        **/
        field3;
    }

Interface
*********
Ensuite, une interface va définir les différentes methodes nécéssaires à la classe de paramètre ("définition du
contrat") tout en héritant de
l'interface VitamParameter (afin que la classe implémentant cette nouvelle interface implémente les deux méthodes
getMapParameters et getMandatoriesParameters.

.. code-block:: java

   /**
    * Exemple d'interface de paramètres
    **/
    public interface StorageParameters extends VitamParameter<StorageParameterName> {

        /**
         * Put parameterValue on mapParameters with parameterName key <br />
         * <br />
         * If parameterKey already exists, then override it (no check)
         *
         * @param parameterName the key of the parameter to put on the parameter map
         * @param parameterValue the value to put on the parameter map
         * @return actual instance of WorkerParameter (fluent like)
         * @throws IllegalArgumentException if the parameterName is null or if parameterValue is null or empty
         **/
        StorageParameters putParameterValue(StorageParameterName parameterName, String parameterValue);

        /**
         * Get the parameter according to the parameterName
         *
         * @param parameterName the wanted parameter
         * @return the value or null if not found
         * @throws IllegalArgumentException throws if parameterName is null
         **/
        String getParameterValue(StorageParameterName parameterName);

        /**
         * Set from map using String as Key
         *
         * @param map the map parameters to set
         * @return the current instance of WorkerParameters
         * @throws IllegalArgumentException if parameter key is unknown or if the map is null
         **/
        StorageParameters setMap(Map<String, String> map);

        /**
        * Get the field1 value
        *
        * @return the field1's value
        **/
        String getStorageParameterField1();
    }

Possibilité d'avoir une classe abstraite
****************************************
Le but est d'implémenter cette interface. Cependant, il est possible de vouloir plusieurs classes de paramètres en
fonction des besoins. Il est alors possible de mettre en place une classe abstraite qui implémente les méthodes
communes aux différentes classes de paramètre (par exemple les getters / setters).

.. code-block:: java

    abstract class AbstractStorageParameters implements StorageParameters {

        @JsonIgnore
        private final Map<StorageParameterName, String> mapParameters = new TreeMap<>();

        @JsonIgnore
        private Set<StorageParameterName> mandatoryParameters;

        AbstractStorageParameters(final Set<StorageParameterName> mandatory) {
            mandatoryParameters = mandatory;
        }

        @JsonCreator
        protected AbstractStorageParameters(Map<String, String> map) {
            mandatoryParameters = StorageParametersFactory.getDefaultMandatory();
            setMap(map);
        }

        @JsonIgnore
        @Override
        public Set<StorageParameterName> getMandatoriesParameters() {
            return Collections.unmodifiableSet(new HashSet<>(mandatoryParameters));
        }

        @JsonIgnore
        @Override
        public Map<StorageParameterName, String> getMapParameters() {
            return Collections.unmodifiableMap(new HashMap<>(mapParameters));
        }

        @JsonIgnore
        @Override
        public WorkerParameters putParameterValue(StorageParameterName parameterName, String parameterValue) {
            ParameterHelper.checkNullOrEmptyParameter(parameterName, parameterValue, getMandatoriesParameters());
            mapParameters.put(parameterName, parameterValue);
            return this;
        }

        @JsonIgnore
        @Override
        public String getParameterValue(StorageParameterName parameterName) {
            ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "parameterName"), parameterName);
            return mapParameters.get(parameterName);
        }

        @JsonIgnore
        @Override
        public StorageParameters setMap(Map<String, String> map) {
            ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "map"), map);
            for (String key : map.keySet()) {
                mapParameters.put(WorkerParameterName.valueOf(key), map.get(key));
            }
            return this;
        }

        @JsonIgnore
        @Override
        public String getField1() {
            return mapParameters.get(StorageParameterName.field1);
        }
    }

Possibilité d'avoir une factory
*******************************
On voit dans le code d'exemple l'utilisation d'une factory qui permet d'obetnir la bonne implémentation de la classe
de paramètres. En effet, au travers de la factory il est facilement possible de mettre en place les champs requis en
fonction des besoins. Par exemple, certains paramètres peuvent être obligatoire pour toutes les implémentations alors
 que certains sont en plus requis pour certaines implémentations.
Voir ici s'il n'est pas possible de faire une factory commune.

.. code-block:: java

    public class WorkerParametersFactory {

        private static final Set<StorageParameterName> genericMandatories = new HashSet<>();

        static {
            genericMandatories.add(StorageParameterName.field1);
            genericMandatories.add(StorageParameterName.field2);
        }

        private StorageParametersFactory() {
            // do nothing
        }

        // Méthodes de la factory
        // ...
    }

Code exemple
************
Ensuite, là où les paramètres sont nécéssaires, il suffit d'utiliser l'interface afin d'être le plus générique possible.

.. code-block:: java

    public void methode(StorageParameters parameters) {
        // Check des paramètres
        ParameterHelper.checkNullOrEmptyParameters(parameters);

        // Récupération des paramètres
        String value = parameters.getField1();
        String value 2 = parameters.get(StorageParameterName.field2);

        // etc...
    }

    // Exemple d'ajout de champs requis
    public void methode2() {

        Set<StorageParameterName> mandatoryToAdd = new Set<>();
        mandatoryToAdd.put(StorageParameterName.field3);

        // Initialisation des paramètres
        StorageParameters parameters = StorageParameterFactory.newStorageParameters(mandatoryToAdd);

        // etc..
    }

Exemple d'utilisation dans le code Vitam
----------------------------------------
Il est possible de retrouver l'utilisation des paramètres génériques Vitam dans les modules suivants :
* Processing
* Logbook
