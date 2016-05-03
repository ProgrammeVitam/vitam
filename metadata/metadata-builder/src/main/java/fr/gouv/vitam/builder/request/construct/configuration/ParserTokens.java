/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.builder.request.construct.configuration;

/**
 * Main language definition
 * 
 *
 */
public class ParserTokens {
    /**
     * @formatter:off
     * For a Select :<br>
     * { $roots: roots, $query : query, $filter : filter, $projection : projection } or [ roots, query, filter, projection ]<br>
     * For an Update : <br>
     * { $roots: roots, $query : query, $filter : multi, $action : action } or [ roots, query, multi, action ]<br>
     * For an Insert : <br>
     * { $roots: roots, $query : query, $filter : multi, $data : data } or [ roots, query, multi, data ]<br>
     * For a Delete : <br>
     * { $roots: roots, $query : query, $filter : multi } or [ roots, query, multi ]<br>
     *
     * Select is in a subtree, by default next level (1), except if $exactdepth
     * is set with a value (exact depth) or $depth with a relative value (+ or
     * -, meaning leaves or parents, 0 for no limit in leaves depth) Only one of
     * $exactdepth and $depth might be set. If both are set, only $depth will be
     * kept. <br>
     *  { expression, $exactdepth : exactdepth, $depth : /- depth }
     * @formatter:on
     */
    public static enum GLOBAL {
        /**
         * Roots part (departure of the request)
         */
        roots,
        /**
         * Query part (where condition)
         */
        query,
        /**
         * Filter part (limit, order by, ... for Query, or isMulti for others)
         */
        filter,
        /**
         * Projection part (returned fields for Query)
         */
        projection,
        /**
         * Action part (in case of update)
         */
        action,
        /**
         * Data part (in case of insert)
         */
        data;

        /**
         * @return the exact token to be used in Mongo ($+code)
         */
        public final String exactToken() {
            if (GlobalDatas.COMMAND_DOLLAR) {
                return "$" + name();
            }
            return name();
        }
    }

    /**
     * Query model
     *
     */
    public static enum QUERY {
        /**
         * All expressions are grouped by an AND operator (all shall be true)<br>
         * $and : [ expression1, expression2, ... ]
         */
        and,
        /**
         * All expressions are grouped by an NOT operator (all shall be false)<br>
         *  $not : [ expression1, expression2, ... ]
         */
        not,
        /**
         * All expressions are grouped by an OR operator (at least one shall be true) <br>
         * $or : [ expression1, expression2, ... ]
         */
        or,
        /**
         * Field named 'name' shall exist <br>
         * $exists : name
         */
        exists,
        /**
         * Field named 'name' shall not exist (faster than $not : [ $exists : name ] ) <br>
         * $missing : name
         */
        missing,
        /**
         * Field named 'name' shall be empty or set to null <br>
         * $isNull : name
         */
        isNull,
        /**
         * field named 'name' contains at least one of the values 'value1', 'value2', ... <br>
         * $in : { name : [ value1, value2, ... ] }
         */
        in,
        /**
         * field named 'name' does not contain any of the values 'value1', 'value2', ... <br>
         * $nin : { name : [ value1, value2, ... ] }
         */
        nin,
        /**
         * Size of an array named 'name' equals to specified length<br>
         *  $size : { name : length }
         */
        size,
        /**
         * Comparison operator <br>
         * $gt : { name : value }
         */
        gt,
        /**
         * Comparison operator <br>
         * $lt : { name : value }
         */
        lt,
        /**
         * Comparison operator $gte <br>
         * : { name : value }
         */
        gte,
        /**
         * Comparison operator <br>
         * $lte : { name : value }
         */
        lte,
        /**
         * Comparison operator <br>
         * $ne : { name : value }
         */
        ne,
        /**
         * Comparison operator <br>
         * $eq : { name : value }
         */
        eq,
        /**
         * Optimization of comparison operator in a range <br>
         * $range : { name : { $gte : value, $lte : value } }
         */
        range,
        /**
         * type might be Point (simple lng, lta), Box, Polygon <br>
         * $geometry : { $type : "type", $coordinates : [ [ lng1, lta1 ], [ lng2, lta2 ], ... ] }
         */
        geometry,
        /**
         * $box : [ [ lng1, lta1 ], [ lng2, lta2 ] ]
         */
        box,
        /**
         * $polygon : [ [ lng1, lta1 ], [ lng2, lta2 ], ... ]
         */
        polygon,
        /**
         * $center : [ [ lng1, lta1 ], radius ]
         */
        center,
        /**
         * Selects geometries within a bounding geometry <br>
         * $geoWithin : { name : { geometry|box|polygon|center } }
         */
        geoWithin,
        /**
         * Selects geometries that intersect with a geometry <br>
         * $geoIntersects : { name : { geometry|box|polygon|center } }
         */
        geoIntersects,
        /**
         * Selects geometries in proximity to a point <br>
         * $near : { name : { geometry_point|[ lng1, lta1], $maxDistance : distance } }
         */
        near,
        /**
         * Selects where field named 'name' matches some words <br>
         * $match : { name : words, $max_expansions : n }
         */
        match,
        /**
         * Selects where field named 'name' matches a phrase (somewhere)<br>
         * $match_phrase : { name : phrase, $max_expansions : n }
         */
        match_phrase,
        /**
         * Selects where field named 'name' matches a phrase as a prefix of the field <br>
         * $match_phrase_prefix : { name : phrase, $max_expansions : n }
         */
        match_phrase_prefix,
        /**
         * Selects where field named 'name' matches a phrase as a prefix of the field <br>
         * $prefix : { name : phrase } <br>
         * Should not be used externally (but
         * possible) but in replacement of match_phrase_prefix if parameter not
         * analyzed
         */
        prefix,
        /**
         * Selects where fields named 'name' are like the one provided,
         * introducing some "fuzzy", which tends to be slower than mlt<br> 
         * $flt : { $fields : [ name1, name2 ], $like : like_text }
         */
        flt,
        /**
         * Selects where fields named 'name' are like the one provided <br>
         * $mlt : { $fields : [ name1, name2 ], $like : like_text }
         */
        mlt,
        /**
         * Selects where field named 'name' contains something relevant to the
         * search parameter. This search parameter can contain wildcards (* ?),
         * specifications:<br>
         * (x y) meaning x or y<br>
         * "x y" meaning exactly sub phrase "x y"<br>
         * +x -y meaning x must be present, y must be absent<br>
         * $search : { name : searchParameter }
         */
        search,
        /**
         * Selects where field named 'name' contains a value valid with the
         * corresponding regular expression. <br>
         * $regex : { name : regex }
         */
        regex,
        /**
         * Selects where field named 'name' contains exactly this term
         * (lowercase only, no blank). Useful in simple value field to find one
         * specific item, or for multiple tests at once (AND implicit). <br>
         * $term : { name : term, name : term }
         */
        term,
        /**
         * Selects where field named 'name' contains exactly this term
         * (lowercase only, no blank) with optional wildcard character (* and
         * ?). Useful in simple value field to find one specific item.<br> 
         * $wildcard : { name : term }
         */
        wildcard,
        /**
         * Selects a node by its exact path (succession of ids) <br>
         * $path : [ id1, id2, ... ]
         */
        path;

        /**
         * @return the exact token to be used in Mongo ($+code)
         */
        public final String exactToken() {
            return "$" + name();
        }
    }

    /**
     * SelectFilter model
     *
     */
    public static enum SELECTFILTER {
        /**
         * Limit the elements returned to the nth first elements $limit : n
         */
        limit,
        /**
         * According to an orderby, start to return the elements from rank start<br>
         * $offset : start
         */
        offset,
        /**
         * Specify an orderby to respect in the return of the elements according
         * to one field named 'name' and an orderby ascendant (+1) or descendant
         * (-1) <br>
         * $orderby : [ { key : +/-1 } ]
         */
        orderby,
        /**
         * Allows to specify some hints to the request server: cache/nocache<br>
         * $hint : [ cache/nocache, ... ]
         */
        hint;

        /**
         * @return the exact token to be used in Mongo ($+code)
         */
        public final String exactToken() {
            if (GlobalDatas.COMMAND_DOLLAR) {
                return "$" + name();
            }
            return name();
        }
    }

    /**
     * Projection model
     *
     */
    public static enum PROJECTION {
        /**
         * Specify the fields to return $fields : {name1 : 0/1, name2 : 0/1,
         * ...}
         */
        fields,
        /**
         * UsageContract reference that will be used to select the binary object
         * version to return $usage : contractId
         */
        usage;

        /**
         * @return the exact token to be used in Mongo ($+code)
         */
        public final String exactToken() {
            if (GlobalDatas.COMMAND_DOLLAR) {
                return "$" + name();
            }
            return name();
        }
    }

    /**
     * Arguments to some QUERY commands
     *
     */
    public static enum QUERYARGS {
        /**
         * Used in geometric queries
         */
        type,
        /**
         * Used in geometric queries
         */
        coordinates,
        /**
         * Used in geometric queries
         */
        maxDistance,
        /**
         * Used in MLT queries
         */
        like,
        /**
         * Used in MLT queries
         */
        fields,
        /**
         * Used in Match request
         */
        max_expansions,
        /**
         * Used in Set Depth (exact) part of each request
         */
        exactdepth,
        /**
         * Used in Set Depth (relative) part of each request
         */
        depth,
        /**
         * Used to specify that argument is a date
         */
        date;

        /**
         * @return the exact token to be used in Mongo ($+code)
         */
        public final String exactToken() {
            return "$" + name();
        }
    }

    /**
     *
     * Range args model
     *
     */
    public static enum RANGEARGS {
        /**
         * Comparison operator $gt : value
         */
        gt,
        /**
         * Comparison operator $lt : value
         */
        lt,
        /**
         * Comparison operator $gte : value
         */
        gte,
        /**
         * Comparison operator $lte : value
         */
        lte;

        /**
         * @return the exact token to be used in Mongo ($+code)
         */
        public final String exactToken() {
            return "$" + name();
        }
    }

    /**
     * specific fields: nbunits, dua, ... <br>
     * $fields : [ #nbunits:1, #dua:1, #all:1... ]
     *
     * #all:1 means all, while #all:0 means none
     */
    public static enum PROJECTIONARGS {
        /**
         * Id of the item
         */
        id,
        /**
         * Number of units from each result (Unit = subUnit, ObjectGroup = objects)
         */
        nbunits,
        /**
         * All Dua for the result
         */
        dua,
        /**
         * All fields for the result or None except Id
         */
        all,
        /**
         * Object size
         */
        size,
        /**
         * Object format
         */
        format,
        /**
         * Unit/ObjectGroup type
         */
        type;

        /**
         * @return the exact token to be used in Mongo ($+code)
         */
        public final String exactToken() {
            return "#" + name();
        }
        /**
         * 
         * @param name
         * @return True if this value is not allowed on set (insert, update)
         */
        public static boolean notAllowedOnSet(String name) {
            if (name.charAt(0) == '#') {
                try {
                    PROJECTIONARGS proj = PROJECTIONARGS.valueOf(name.substring(1));
                    switch (proj) {
                        case all:
                        case format:
                        case id:
                        case nbunits:
                        case size:
                            return true;
                        default:
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
            return false;
        }
    }

    /**
     * Specific values for Filter arguments
     *
     */
    public static enum FILTERARGS {
        /**
         * Cache could be used for this request
         */
        cache,
        /**
         * Cache should not be used for this request
         */
        nocache,
        /**
         * Query should not have a timeout (scrolling)
         */
        notimeout,
        /**
         * Query concerns Units
         */
        units,
        /**
         * Query concerns ObjectGroups
         */
        objectgroups,
        /**
         * Query concerns Objects
         */
        objects;

        /**
         * @return the exact token to be used in Mongo ($+code)
         */
        public final String exactToken() {
            return name();
        }
    }

    /**
     *
     * Update model
     *
     * <pre>
     * {\@code
     *      
     * Pour mettre à jour (update) :
     * multiple update : { $multi : true/false } pour autoriser une mise à jour de plusieurs noeuds ou pas (si la requête présente plusieurs résultats mais $multi = false => erreur )
     * action : 
     *   { $set : { clef : valeur, clef : valeur, ... } } / { $unset : { clef : "", ... } } positionne ou ajoute (si elle n'existait pas) une valeur pour une clef ou efface la clef
     *   { $inc : { clef : valeur, clef : valeur, ... } } incrémente la valeur 
     *   { $rename : { clef : nouvelleclef, ... } } renomme une clef
     *   { $push : { clef : valeur, ... } : ajoute une valeur à une clef de liste (si la liste maliste est [ a, b, c], $push : { maliste : b } donnera maliste = [ a, b, c, b])
     *     { $push : { clef : { $each : [valeur, valeur, ... ] } } } idem mais plusieurs éléments en une fois ($push : { maliste : { $each : [ b, d, e, a] } } donnera  maliste = [ a, b, c, b, d, e, a] )
     *   { $add : { clef : valeur, ... } : ajoute une valeur à une clef de liste mais si celle-ci n'y est pas déjà (si la liste maliste est [ a, b, c], $add : { maliste : b } ne changera pas la liste, tandis que $add : { maliste : d } donnera maliste = [ a, b, c, d] )
     *     Si valeur est multiple (une liste) et que chacune des valeurs doit être intégrées : $add : { maliste : { $each : [ b, d, e, a] } } donnera maliste = [ a, b, c, d, e]
     *   { $pop : { clef : 1 ou -1 } } retire le dernier (1) ou le premier (-1) élément de la liste
     *   { $pull : { clef : valeur } } retire l'élément valeur de la liste
     *     { $pull : { clef : { $each : [valeur, valeur, ... ] } } } idem mais plusieurs éléments en une fois 
     *   { $sort : { clef : 1 ou -1 } } pour trier une liste selon un ordre ascendant (1) ou descendant (-1)
     * 3 parties : critères de sélection (expression depuis root) + filtres (multi) + action
     * retourne : la requête + des info générales (nb de résultats) + les n premiers résultats (noeuds mis à jour) selon le format souhaité 
     *      
     * }
     * </pre>
     */
    public static enum UPDATEACTION {
        /**
         * $set : { name : value, name : value, ... }
         */
        set,
        /**
         * $unset : [ name, name, ... ]
         */
        unset,
        /**
         * increment one field named 'name' with default 1 or value <br>
         * $inc : { name : value }
         */
        inc,
        /**
         * set one field named 'name' with minimum value of current value and
         * given value <br>
         * $min : { name : value }
         */
        min,
        /**
         * set one field named 'name' with maximum value of current value and
         * given value <br>
         * $max : { name : value }
         */
        max,
        /**
         * rename one field named 'name' to 'newname' <br>
         * $rename : { name : newname }
         */
        rename,
        /**
         * Add one element at the end of a list value, or each element of a list
         * if $each parameter is used <br>
         * $push : { name : { $each : [ value, value, ... ] } }
         */
        push,
        /**
         * Remove one specific element from a list or each element of a list if
         * $each parameter is used <br>
         * $pull : { name : { $each : [ value, value, ... ] } }
         */
        pull,
        /**
         * Add one element (or each element of a list) if not already in the
         * list <br>
         * $add : { name : { $each : [ value, value, ... ] } }
         */
        add,
        /**
         * Remove n element from a list from the end (1) or the beginning (-1)<br>
         * $pop : { name : -1/1 }
         */
        pop;

        /**
         * @return the exact token to be used in Mongo ($+code)
         */
        public final String exactToken() {
            if (GlobalDatas.COMMAND_DOLLAR) {
                return "$" + name();
            }
            return name();
        }
    }

    /**
     * Update Args model
     *
     */
    public static enum UPDATEACTIONARGS {
        /**
         * Update argument
         */
        each;
        /**
         * @return the exact token to be used in Mongo ($+code)
         */
        public final String exactToken() {
            return "$" + name();
        }
    }

    /**
     * Action Filter model
     *
     */
    public static enum MULTIFILTER {
        /**
         * True to allow multiple update if multiple elements are found through
         * the QUERY, else False will return an error if multiple elements are
         * found. $mult : true/false
         */
        mult;

        /**
         * @return the exact token to be used in Mongo ($+code)
         */
        public final String exactToken() {
            if (GlobalDatas.COMMAND_DOLLAR) {
                return "$" + name();
            }
            return name();
        }
    }
}
