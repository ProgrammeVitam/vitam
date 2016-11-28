/**
 * angular-shiro
 * @version v0.1.2 - 2015-05-03
 * @link https://github.com/gnavarro77/angular-shiro
 * @author Gilles Navarro ()
 * @license MIT License, http://www.opensource.org/licenses/MIT
 */
(function (window, document, undefined) {
  'use strict';
  function AngularShiroConfigProvider() {
    this.options = {
      urls: {
        '/': 'anon',
        '/index': 'anon',
        '/login': 'anon',
        '/signin': 'anon',
        '/logout': 'logout',
        '/signout': 'logout'
      },
      login: {
        api: '/api/authenticate',
        path: '/login'
      },
      logout: {
        api: '/api/logout',
        path: '/'
      }
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name AngularShiroConfigProvider#setFilter
     * @methodOf angularShiro.services.angularShiroConfigProvider
     * 
     * @description Associate the specified filter(s) with the given path
     * 
     * @param {string}
     *                path the path for which the filter(s) should be applied
     * @param {string|array}
     *                the filter(s) to apply to the path
     * 
     * @example
     * 
     * <pre>
     * app.config([ 'angularShiroConfigProvider', function(config) {
     *     config.setFilter();
     * } ]);
     * </pre>
     */
    this.setFilter = function (path, filterName) {
      angular.extend(this.options, { path: filterName });
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name AngularShiroConfigProvider#setLogoutPath
     * @methodOf angularShiro.services.angularShiroConfigProvider
     * 
     * @description The `logoutPath` specifies the path to apply to `$location`
     *              when the user log out of the application.
     * 
     * @param {string}
     *                logoutPath the application path to be applied on log out
     * 
     * @example
     * 
     * <pre>
     * app.config([ 'angularShiroConfigProvider', function(config) {
     *     config.setLogoutPath('/my/logout/page');
     * } ]);
     * </pre>
     */
    this.setLogoutPath = function (logoutPath) {
      this.options.logout.path = logoutPath;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name AngularShiroConfigProvider#setLoginPath
     * @methodOf angularShiro.services.angularShiroConfigProvider
     * 
     * @description The `loginPath` specifies the path used for redirecting the
     *              user on an attempt to reach a denied url.
     * 
     * @param {string}
     *                loginPath The path to the login page (default '`/login`')
     * 
     * @example
     * 
     * <pre>
     * app.config([ 'angularShiroConfigProvider', function(config) {
     *     config.setLoginPath('/my/login/page');
     * } ]);
     * </pre>
     */
    this.setLoginPath = function (loginPath) {
      this.options.login.path = loginPath;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name AngularShiroConfigProvider#setAuthenticateUrl
     * @methodOf angularShiro.services.angularShiroConfigProvider
     * 
     * @description The `authenticateUrl` specifies the url to call when the
     *              user try to login to the application. The url is called with
     *              a `POST` method.
     * 
     * @param {string}
     *                authenticateUrl The uri to be called on a login attempt
     *                (default '`/api/authenticate`')
     * @example
     * 
     * <pre>
     * app.config([ 'angularShiroConfigProvider', function(config) {
     *     config.setAuthenticateUrl('/my/custom/url');
     * } ]);
     * </pre>
     */
    this.setAuthenticateUrl = function (authenticateUrl) {
      this.options.login.api = authenticateUrl;
    };
    this.$get = [function () {
        return this.options;
      }];
  }
  function AuthenticatorProvider() {
    this.$get = [
      '$q',
      '$http',
      '$timeout',
      'angularShiroConfig',
      function ($q, $http, $timeout, config) {
        return {
          authenticate: function (token) {
            var promise = null;
            if (!token || !token.getPrincipal() || !token.getCredentials()) {
              throw '[Autheticate] Can not authenticate. Invalid token provided!';
            }
            if (config && config.login && config.login.api) {
              var deferred = $q.defer();
              $http.post(config.login.api, {
                token: {
                  principal: token.getPrincipal(),
                  credentials: token.getCredentials()
                }
              }).success(function (data, status, headers, config) {
                deferred.resolve(data);
              }).error(function (data, status, headers, config) {
                deferred.reject(data);
              });
              promise = deferred.promise;
            } else {
              throw '[Autheticate] Can not authenticate since no \'config.login.api\' is provided. Please check your configuration.';
            }
            return promise;
          }
        };
      }
    ];
  }
  /**
 * @ngdoc object
 * @name angularShiro.services.UsernamePasswordToken
 * 
 * @description <code>UsernamePasswordToken</code> is a simple
 *              username/password authentication token.
 * 
 * @since 0.0.1
 */
  function UsernamePasswordToken(username, password, rememberMe) {
    /**
     * @ngdoc property
     * @name UsernamePasswordToken#username
     * @propertyOf angularShiro.services.UsernamePasswordToken
     * @description the Subject's user name
     */
    this.username = username || null;
    /**
     * @ngdoc property
     * @name UsernamePasswordToken#password
     * @propertyOf angularShiro.services.UsernamePasswordToken
     * @description the Subject's password
     */
    this.password = password || null;
    /**
     * @ngdoc property
     * @name UsernamePasswordToken#rememberMe
     * @propertyOf angularShiro.services.UsernamePasswordToken
     * @description Whether or not 'rememberMe' should be enabled for the
     *              corresponding login attempt; default is `false`
     */
    this.rememberMe = rememberMe || false;
    /**
     * @ngdoc method
     * @name UsernamePasswordToken#getPrincipal
     * @methodOf angularShiro.services.UsernamePasswordToken
     * 
     * @description Returns <code>username</code> value
     * @return {string} <code>username</code> value
     */
    this.getPrincipal = function () {
      return this.username;
    };
    /**
     * @ngdoc method
     * @name UsernamePasswordToken#getCredentials
     * @methodOf angularShiro.services.UsernamePasswordToken
     * 
     * @description Returns the <code>password</code> value
     * 
     * @return {string} <code>password</code> value
     */
    this.getCredentials = function () {
      return this.password;
    };
    /**
     * @ngdoc method
     * @name UsernamePasswordToken#isRememberMe
     * @methodOf angularShiro.services.UsernamePasswordToken
     * 
     * 
     * @description Returns `true` if the `Subject` is to be remembered, i.e if
     *              the credentials should be stored in the browser
     *              `sessionStorage`, `false` otherwise (`false` by default)
     * 
     * @return {boolean} `true` if the `Subject` is to be remembered, i.e if the
     *         credentials should be stored in the browser `sessionStorage`,
     *         `false` otherwise (`false` by default)
     */
    this.isRememberMe = function () {
      return this.rememberMe;
    };
    /**
     * 
     * @ngdoc method
     * @name UsernamePasswordToken#setRememberMe
     * @methodOf angularShiro.services.UsernamePasswordToken
     * 
     * @description If set to `true`, in cas of page reload, if a valid session
     *              corresponding to the url `sessionId` param is found, use the
     *              serialiazed token to auto login to reload authorizations
     * 
     * @param {boolean}
     *                rememberMe value
     */
    this.setRememberMe = function (rememberMe) {
      this.rememberMe = rememberMe;
    };
    /**
     * @ngdoc method
     * @name UsernamePasswordToken#clear
     * @methodOf angularShiro.services.UsernamePasswordToken
     * 
     * @description Clear all the data
     * 
     */
    this.clear = function () {
      this.username = this.password = null;
      this.rememberMe = false;
    };
    /**
     * @ngdoc method
     * @name UsernamePasswordToken#serialize
     * @methodOf angularShiro.services.UsernamePasswordToken
     * 
     * @description Clear all the data
     * 
     * @return {string} the current token serialized as a json string
     */
    this.serialize = function () {
      return angular.toJson(this);
    };
    /**
     * @ngdoc method
     * @name UsernamePasswordToken#deserialize
     * @methodOf angularShiro.services.UsernamePasswordToken
     * 
     * @description Deserialize the json string representing the token and copy
     *              the value to the current object
     * 
     * @param {string}
     *                serializedToken json string representing the token
     */
    this.deserialize = function (serializedToken) {
      var obj = angular.fromJson(serializedToken);
      angular.extend(this, obj);
    };
  }
  /**
 * @ngdoc object
 * @name angularShiro.services.AuthenticationInfo
 * 
 * @description <code>AuthenticationInfo</code> represents the Subject's
 *              informations regarding the authentication process
 * 
 * @param {string}
 *                principal Subject's principal (ex : Subject's login, username,
 *                ...)
 * 
 * @param {string}
 *                credentials Subject's principal (ex : Subject's login,
 *                username, ...)
 * 
 * @since 0.0.1
 */
  function AuthenticationInfo(principal, credentials) {
    /**
     * @name AuthenticationInfo#principal
     * @propertyOf angularShiro.services.AuthenticationInfo
     * @description the Subject's principal
     * @returns {string} the Subject's principal
     */
    this.principal = principal;
    /**
     * @name AuthenticationInfo#username
     * @propertyOf angularShiro.services.AuthenticationInfo
     * @description the Subject's credentials
     * @returns {object} the Subject's credentials
     */
    this.credentials = credentials;
    /**
     * @ngdoc method
     * @name AuthenticationInfo#getCredentials
     * @methodOf angularShiro.services.AuthenticationInfo
     * 
     * @description Returns the Suject's principal
     * 
     * @return {object} the Subject's principal
     * @since 0.0.1
     */
    this.getPrincipal = function () {
      return this.principal;
    };
    /**
     * @ngdoc method
     * @name AuthenticationInfo#getCredentials
     * @methodOf angularShiro.services.AuthenticationInfo
     * 
     * @description Returns the Subject's credentials . A credential verifies
     *              the Subject's principal, such as a password or private key
     * 
     * @returns {object} the Subject's credentials
     * @since 0.0.1
     */
    this.getCredentials = function () {
      return this.credentials;
    };
  }
  function Permission(wildcardString, caseSensitive) {
    /**
     * @ngdoc property
     * @name Permission#WILDCARD_TOKEN
     * @propertyOf angularShiro.services.Permission
     * @description the token representing wildcard
     * @returns {string} the token representing wildcard
     */
    this.WILDCARD_TOKEN = '*';
    /**
     * @ngdoc property
     * @name Permission#PART_DIVIDER_TOKEN
     * @propertyOf angularShiro.services.Permission
     * @description the string used to separate the different parts of a token
     * @returns {string} the string used to separate the different parts of a
     *          token
     */
    this.PART_DIVIDER_TOKEN = ':';
    /**
     * @ngdoc property
     * @name Permission#SUBPART_DIVIDER_TOKEN
     * @propertyOf angularShiro.services.Permission
     * @description the string used to separate multiple tokens
     * @returns {string} the string used to separate multiple tokens
     */
    this.SUBPART_DIVIDER_TOKEN = ',';
    /**
     * @ngdoc property
     * @name Permission#caseSensitive
     * @propertyOf angularShiro.services.Permission
     * @description flag indicating if the comparisons are case sensitive or not
     * @returns {string} flag indicating if the comparisons are case sensitive
     *          or not
     */
    this.caseSensitive = caseSensitive ? caseSensitive : false;
    /**
     * @ngdoc property
     * @name Permission#parts
     * @propertyOf angularShiro.services.Permission
     * @description Represents the differents parts of a token
     * @returns {array} Represents the differents parts of a token
     */
    this.parts;
    /**
     * @ngdoc method
     * @name Permission#implies
     * @methodOf angularShiro.services.Permission
     * 
     * @description Returns <code>true</code> if this current instance implies
     *              all the functionality and/or resource access described by
     *              the specified <code>Permission</code> argument,
     *              <code>false</code> otherwise
     * 
     * @param {string |
     *                Permission} permission the permission to check for
     *                behavior/functionality comparison
     * 
     * @return {boolean} <code>true</code> if this current instance implies
     *         all the functionality and/or resource access described by the
     *         specified <code>Permission</code> argument, <code>false</code>
     *         otherwise
     */
    this.implies = function (permission) {
      var implies = angular.isDefined(permission) && this.getParts().length > 0;
      if (implies) {
        permission = angular.isString(permission) ? new Permission(permission) : permission;
        var theirParts = permission.getParts();
        var theirPartsLength = theirParts.length;
        var ourParts = this.getParts();
        for (var i = 0, len = ourParts.length; i < len; i++) {
          var ourPart = ourParts[i];
          if (i < theirPartsLength) {
            var theirPart = theirParts[i];
            if (!this.containsWildCardToken(ourPart) && !this.containsAll(ourPart, theirPart)) {
              implies = false;
              break;
            }
          } else {
            if (!this.containsWildCardToken(ourPart)) {
              implies = false;
              break;
            }
          }
        }
      }
      return implies;
    };
    /**
     * Returns <code>true</code> if all their parts are contained in our
     * parts, <code>false</code> otherwise.
     * 
     * @param {array}
     *                ourPart
     * @param {array}
     *                theirPart
     * 
     * @return {boolean} <code>true</code> if all their parts are contained in
     *         our parts, <code>false</code> otherwise
     * @private
     */
    this.containsAll = function (ourPart, theirPart) {
      var contains = true;
      for (var i = 0; i < theirPart.length; i++) {
        if (ourPart.indexOf(theirPart[i]) === -1) {
          contains = false;
          break;
        }
      }
      return contains;
    };
    /**
     * Returns <code>true</code> if the part contains the wildcard token,
     * <code>false</code> otherwise.
     * 
     * @param {String}
     *                part part of the token to be tested
     * @return {boolean} <code>true</code> if the part contains the wildcard
     *         token, <code>false</code> otherwise
     * @private
     */
    this.containsWildCardToken = function (part) {
      return part.indexOf(this.WILDCARD_TOKEN) > -1;
    };
    /**
     * Returns the parts composing the specified wildcard string
     * 
     * @param {string}
     *                wildcardString string representing the permission
     * @param {boolean}
     *                caseSensitive flag indicating if the comparison is case
     *                sensitive
     * @return {array} the parts composinf the wildcard string
     * @private
     */
    this.resolveParts = function (wildcardString, caseSensitive) {
      var parts = [];
      if (angular.isDefined(wildcardString) && angular.isString(wildcardString)) {
        wildcardString = wildcardString.trim ? wildcardString.trim() : wildcardString;
        var tokens = wildcardString.split(this.PART_DIVIDER_TOKEN);
        angular.forEach(tokens, function (token) {
          parts.push(this.resolveSubParts(token, caseSensitive));
        }, this);
      }
      return parts;
    };
    /**
     * Split a part of the overall wildcardString into its sub parts
     * 
     * @method resolveSubParts
     * @param part
     *                {string} a part of the overall wildcardString
     * @param caseSensitive
     *                {boolean} flag indicating if the comparison is case
     *                sensitive
     * @retun {array} the subparts
     * @private
     */
    this.resolveSubParts = function (part, caseSensitive) {
      var subParts = [];
      var tokens = part.split(this.SUBPART_DIVIDER_TOKEN);
      var idx = 0;
      angular.forEach(tokens, function (token) {
        token = token.trim ? token.trim() : token;
        token = caseSensitive === false ? angular.lowercase(token) : token;
        subParts[idx++] = token;
      });
      return subParts;
    };
    /**
     * Returns the wildcardString parts
     * 
     * @return {array} wildcard string parts
     * @private
     */
    this.getParts = function () {
      return this.parts;
    };
    // initialize
    this.parts = this.resolveParts(wildcardString, caseSensitive);
  }
  /**
 * @ngdoc object
 * @name angularShiro.services.AuthorizationInfo
 * 
 * 
 * @description AuthorizationInfo represents a Subject's authorization
 *              informations (roles, permissions, etc) used for access control
 *              operations
 * 
 * @param {array}
 *                roles the list of the <code>Subject</code> roles
 * @param {array}
 *                permissions the list of the <code>Subject</code> permissions
 * 
 * 
 */
  function AuthorizationInfo(roles, permissions) {
    /**
     * @name AuthorizationInfo#roles
     * @propertyOf angularShiro.services.AuthorizationInfo
     * @description names of all roles assigned to a corresponding
     *              <code>Subject</code>
     * @returns {array} the Subject's roles
     */
    this.roles = angular.isArray(roles) ? roles : [];
    /**
     * @name AuthorizationInfo#permissions
     * @propertyOf angularShiro.services.AuthorizationInfo
     * @description list of the permissions assigned to the corresponding
     *              <code>Subject</code>
     * @returns {array} the Subject's permissions
     */
    this.permissions = angular.isArray(permissions) ? permissions : [];
    /**
     * @ngdoc method
     * @name AuthorizationInfo#getRoles
     * @methodOf angularShiro.services.AuthorizationInfo
     * 
     * @description Returns names of all roles assigned to a corresponding
     *              <code>Subject</code>
     * 
     * @return {Array} all roles assigned to a corresponding
     *         <code>Subject</code>
     */
    this.getRoles = function () {
      return this.roles;
    };
    /**
     * @ngdoc method
     * @name AuthorizationInfo#getPermissions
     * @methodOf angularShiro.services.AuthorizationInfo
     * 
     * @description Returns all permissions assigned to the <code>Subject</code>
     * 
     * @return {Array} all permissions assigned to the <code>Subject</code>
     */
    this.getPermissions = function () {
      return this.permissions;
    };
    /**
     * Returns all string-based permissions assigned to the corresponding
     * Subject
     * 
     * @method getStringPermissions
     * @return {Array} all string-based permissions assigned to the
     *         corresponding Subject
     * @since 0.0.1
     * 
     */
    this.getStringPermissions = function () {
      var permissions = [];
      for (var i = 0, len = this.permissions.length; i < len; i++) {
        var p = this.permissions[i];
        if (angular.isString(p)) {
          permissions.push(p);
        }
      }
      return permissions;
    };
    /**
     * Returns all <code>Permission</code>s assigned to the corresponding
     * Subject.
     * 
     * @method getObjectPermissions
     * @return {Array} all <code>Permission</code>s assigned to the
     *         corresponding Subject
     * @since 0.0.1
     */
    this.getObjectPermissions = function () {
      var permissions = [];
      for (var i = 0, len = this.permissions.length; i < len; i++) {
        var p = this.permissions[i];
        if (p instanceof Permission) {
          permissions.push(p);
        }
      }
      return permissions;
    };
  }
  /**
 * @ngdoc object
 * @name angularShiro.services.Authorizer
 * 
 * 
 * @description An <code>Authorizer</code> performs authorization (access
 *              control) operations for any given Subject
 * 
 * All the authorization operations are based upon the instance of
 * <code>AuthorizationInfo</code> that is injected through the
 * <code>Authorizer</code> constructor.
 * 
 * 
 * @param {AuthorizationInfo}
 *                authorizationInfo All informations regarding the Subject
 *                authorizations
 */
  function Authorizer() {
    /**
     * @name Authorizer#permissions
     * @propertyOf angularShiro.services.Authorizer
     * @description list of the permission objects assigned to the corresponding
     *              Subject
     * @return {array} the Subject's permissions
     */
    this.permissions;
    /**
     * <code>AuthorizationInfo</code> instance support of authorization
     * operations
     * 
     * @private
     */
    this.authorizationInfo;
    /**
     * Set the <code>AuthorizationInfo</code> instance
     */
    this.setAuthorizationInfo = function (authorizationInfo) {
      if (!authorizationInfo || authorizationInfo === null || !(authorizationInfo instanceof AuthorizationInfo)) {
        throw { 'illegalArgumentException': 'invalid value for authorizationInfo' };
      }
      this.authorizationInfo = authorizationInfo;
      this.permissions = this.getPermissions(authorizationInfo);
    };
    /**
     * Clear the properties of the current <code>Authorizer</code> instance.
     * 
     * @private
     */
    this.clear = function () {
      this.authorizationInfo = this.permissions = null;
    };
    /**
     * @ngdoc method
     * @name Authorizer#isPermitted
     * @methodOf angularShiro.services.Authorizer
     * 
     * @description Returns <code>true</code> if the current
     *              <code>Subject</code> has the specified permission(s),
     *              <code>false</code> otherwise.
     * 
     * @param {string |
     *                Permission | array} permission a permission
     * @return {boolean | array} <code>true</code> if the current
     *         <code>Subject</code> has the specified permission(s),
     *         <code>false</code> otherwise.
     * 
     */
    this.isPermitted = function (permission) {
      var result;
      if (angular.isArray(permission)) {
        result = [];
        angular.forEach(permission, function (p) {
          result.push(this.isObjectPermissionPermitted(this.resolvePermission(p)));
        }, this);
      } else {
        result = this.isObjectPermissionPermitted(this.resolvePermission(permission));
      }
      return result;
    };
    /**
     * @ngdoc method
     * @name Authorizer#isPermittedAll
     * @methodOf angularShiro.services.Authorizer
     * 
     * @description Returns <code>true</code> if the <code>Subject</code>
     *              has all the specified permissions.
     * 
     * @param {array}
     *                permissions a list of permission
     * @return {boolean} <code>true</code> if the <code>Subject</code> has
     *         all the specified permissions, <code>false</code> otherwise.
     */
    this.isPermittedAll = function (permissions) {
      return this.isPermitted(permissions).indexOf(false) === -1;
    };
    /**
     * @ngdoc method
     * @name Authorizer#hasRole
     * @methodOf angularShiro.services.Authorizer
     * 
     * @description Returns <code>true</code> if the current
     *              <code>Subject</code> has the specified role,
     *              <code>false</code> otherwise.
     * 
     * @param {string}
     *                role role to check
     * @return {boolean} <code>true</code> if the current Subject has the
     *         specified role, <code>false</code> otherwise.
     * 
     */
    this.hasRole = function (role) {
      var hasRole = false;
      if (role) {
        hasRole = this.authorizationInfo.getRoles().indexOf(role) > -1;
      }
      return hasRole;
    };
    /**
     * @ngdoc method
     * @name Authorizer#hasRoles
     * @methodOf angularShiro.services.Authorizer
     * 
     * @description Returns an <code>array</code> of booleans whose indices
     *              correspond to the index of the roles in the given
     *              identifiers. At a given index, a <code>true</code> value
     *              indicates that the user has the role, a <code>false</code>
     *              value indicates that he does not have the role.
     * 
     * @param {array}
     *                roles the list of roles to check against the
     *                <code>Subject</code>'s roles
     * @return {array} an array of booleans
     */
    this.hasRoles = function (roles) {
      var result = [];
      if (roles && angular.isArray(roles)) {
        angular.forEach(roles, function (role) {
          result.push(this.hasRole(role));
        }, this);
      }
      return result;
    };
    /**
     * @ngdoc method
     * @name Authorizer#hasAllRoles
     * @methodOf angularShiro.services.Authorizer
     * 
     * @description Returns <code>true</code> if the current
     *              <code>Subject</code> has all of the specified roles,
     *              <code>false</code> otherwise.
     * 
     * @param {array}
     *                roles the list of roles to check
     * @return {boolean} <code>true</code> if the current <code>Subject</code>
     *         has all of the specified roles, <code>false</code> otherwise.
     */
    this.hasAllRoles = function (roles) {
      return this.hasRoles(roles).indexOf(false) === -1;
    };
    /**
     * Returns the permission objects collected out of the
     * <code>AuthorizationInfo</code> object
     * 
     * @method getPermissions
     * @param {AuthorizationInfo}
     *                authorizationInfo Subject/User authorization data
     * @return {Array} the permissions (object) collected out of the
     *         <code>AuthorizationInfo</code> object
     */
    this.getPermissions = function (authInfo) {
      var permissions = authInfo.getObjectPermissions();
      var stringPermissions = authInfo.getStringPermissions();
      for (var i = 0, len = stringPermissions.length; i < len; i++) {
        permissions.push(new Permission(stringPermissions[i]));
      }
      return permissions;
    };
    /**
     * Evaluate if the permission is granted to the Subject/User
     * 
     * @method isObjectPermissionPermitted
     * @param {Permission}
     *                permission Permission object to evaluate
     * @retun {boolean} <code>true</code> if the permission is granted,
     *        <code>false</code>otherwise
     */
    this.isObjectPermissionPermitted = function (permission) {
      var permitted = false;
      for (var i = 0, len = this.permissions.length; i < len && !permitted; i++) {
        permitted = this.permissions[i].implies(permission);
      }
      return permitted;
    };
    /**
     * Returns an object Permission
     * 
     * @param {string |
     *                Permission} permission a permission
     * @retun {Permission} a Permission object
     */
    this.resolvePermission = function (permission) {
      return angular.isString(permission) ? new Permission(permission) : permission;
    };
  }
  /**
 * @ngdoc object
 * @name angularShiro.services.SessionException
 * 
 * @description Exception associated with the session management
 * 
 */
  function SessionException(msg) {
    this.msg = msg;
  }
  /**
 * @ngdoc object
 * @name angularShiro.services.Session
 * 
 * @description A `Session` is a stateful data context associated with a single
 *              `Subject` who interacts with a software system over a period of
 *              time.
 * 
 */
  function Session() {
    /**
     * @name Session#uid
     * @description unique identifier assigned by the system
     * @propertyOf angularShiro.services.Session
     */
    this.uid;
    /**
     * @name Session#startTimestamp
     * @description time the session was started
     * @propertyOf angularShiro.services.Session
     */
    this.startTimestamp = new Date();
    /**
     * @name Session#stopTimestamp
     * @description time the session was stopped
     * @propertyOf angularShiro.services.Session
     */
    this.stopTimestamp = null;
    /**
     * @name Session#lastAccessTime
     * @description last time the application received a request or method
     *              invocation
     * @propertyOf angularShiro.services.Session
     */
    this.lastAccessTime = new Date();
    /**
     * @name Session#timeout
     * @description the time in milliseconds that the session session may remain
     *              idle before expiring. Default value is 30 minutes
     * @propertyOf angularShiro.services.Session
     */
    this.timeout = 30 * 60 * 1000;
    /**
     * @name Session#expired
     * @description flag indicating if the session is expired
     * @propertyOf angularShiro.services.Session
     */
    this.expired = false;
    /**
     * @name Session#attributes
     * @description the attributes attached to the session
     * @propertyOf angularShiro.services.Session
     */
    this.attributes = {};
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#getId
     * @methodOf angularShiro.services.Session
     * 
     * @description Returns the unique identifier assigned by the system upon
     *              session creation
     * 
     * @returns {string} unique identifier assigned by the system upon session
     *          creation
     */
    this.getId = function () {
      return this.uid;
    };
    /**
     * @ngdoc method
     * @function
     * @name Session#setId
     * @methodOf angularShiro.services.Session
     * 
     * @description Assign a generated session ID to the session instance
     *              directly
     */
    this.setId = function (uid) {
      this.uid = uid;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#getStartTimestamp
     * @methodOf angularShiro.services.Session
     * 
     * @description Returns the time the session was started
     * 
     * @returns {date} time the session was started
     */
    this.getStartTimestamp = function () {
      return this.startTimestamp;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#getLastAccessTime
     * @methodOf angularShiro.services.Session
     * 
     * @description Returns the last time the application received a request or
     *              method invocation from the user associated with this session
     * 
     * @returns {date} last time the application received a request or method
     *          invocation from the user associated with this session
     */
    this.getLastAccessTime = function () {
      return this.lastAccessTime;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#getTimeout
     * @methodOf angularShiro.services.Session
     * 
     * @description Returns the time in milliseconds that the session session
     *              may remain idle before expiring
     * 
     * @returns {date} the time in milliseconds the session may remain idle
     *          before expiring
     */
    this.getTimeout = function () {
      return this.timeout;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#setTimeout
     * @methodOf angularShiro.services.Session
     * 
     * @description Sets the time in milliseconds that the session may remain
     *              idle before expiring
     * 
     * @param {long}
     *                the time in milliseconds that the session may remain idle
     *                before expiring
     * 
     */
    this.setTimeout = function (maxIdleTimeInMillis) {
      this.timeout = maxIdleTimeInMillis;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#touch
     * @methodOf angularShiro.services.Session
     * 
     * @description Explicitly updates the `lastAccessTime` of this session to
     *              the current time when this method is invoked. This method
     *              can be used to ensure a session does not time out
     * 
     */
    this.touch = function () {
      this.lastAccessTime = new Date();
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#stop
     * @methodOf angularShiro.services.Session
     * 
     * @description Explicitly stops (invalidates) this session
     * 
     */
    this.stop = function () {
      if (this.stopTimestamp === null) {
        this.stopTimestamp = new Date();
      }
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#isStopped
     * @methodOf angularShiro.services.Session
     * 
     * @description Returns `true` if the session is stopped, `false' otherwise
     * 
     * @returns {boolean} `true` if the session is stopped, `false' otherwise
     */
    this.isStopped = function () {
      return this.stopTimestamp !== null;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#isExpired
     * @methodOf angularShiro.services.Session
     * 
     * @description Returns `true` if the session is expired, `false' otherwise
     * 
     * @returns {boolean} `true` if the session is expired, `false' otherwise
     */
    this.isExpired = function () {
      return this.expired;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#expire
     * @methodOf angularShiro.services.Session
     * 
     * @description Explicitly make the session expires
     * 
     */
    this.expire = function () {
      this.stop();
      this.expired = true;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#isValid
     * @methodOf angularShiro.services.Session
     * 
     * @description Returns `true` if the session is valid (not expired nor
     *              stopped), `false' otherwise
     * 
     * @returns {boolean} `true` if the session is valid, `false' otherwise
     */
    this.isValid = function () {
      return !this.isStopped() && !this.isExpired();
    };
    /**
     * 
     */
    this.validate = function () {
      if (this.isStopped()) {
        var msg = 'Session with id [' + this.getId() + '] has been ' + 'explicitly stopped.  No further interaction under this session is allowed.';
        throw new SessionException(msg);
      }
      if (this.isTimedOut()) {
        this.expire();
        throw new SessionException('Session has expired');
      }
    };
    /**
     * 
     */
    this.isTimedOut = function () {
      var timedOut = this.isExpired();
      if (!timedOut) {
        var timeout = this.getTimeout();
        if (timeout >= 1) {
          var currentTimestamp = new Date().getTime();
          var expireTimeMillis = currentTimestamp - timeout;
          var expireTime = new Date(expireTimeMillis);
          timedOut = this.lastAccessTime <= expireTime;
        }
      }
      return timedOut;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#getAttributeKeys
     * @methodOf angularShiro.services.Session
     * 
     * @description Returns the keys of all the attributes stored under this
     *              session. If there are no attributes, this returns an empty
     *              collection
     * 
     * @returns {array} the keys of all attributes stored under this session, or
     *          an empty collection if there are no session attributes
     * 
     */
    this.getAttributeKeys = function () {
      var keys = [];
      angular.forEach(this.attributes, function (value, key) {
        keys.push(key);
      });
      return keys;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#getAttribute
     * @methodOf angularShiro.services.Session
     * 
     * @description Returns the keys of all the attributes stored under this
     *              session. If there are no attributes, this returns an empty
     *              collection
     * 
     * @param {object}
     *                the unique name of the object bound to this session
     * 
     * @returns {object} the object bound under the specified `key` name or
     *          `null` if there is no object bound under that name
     * 
     */
    this.getAttribute = function (key) {
      return this.attributes[key];
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#setAttribute
     * @methodOf angularShiro.services.Session
     * 
     * @description Returns the keys of all the attributes stored under this
     *              session. If there are no attributes, this returns an empty
     *              collection
     * 
     * @param {object}
     *                `key` the name under which the `value` object will be
     *                bound in this session
     * @param {object}
     *                `value` the object to bind in this session
     * 
     */
    this.setAttribute = function (key, value) {
      this.attributes[key] = value;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name Session#removeAttribute
     * @methodOf angularShiro.services.Session
     * 
     * @description Removes (unbinds) the object bound to this session under the
     *              specified `key` name
     * 
     * @param {object}
     *                `key` the name under which the `value` object will be
     *                bound in this session
     * 
     */
    this.removeAttribute = function (key) {
      delete this.attributes[key];
    };
  }
  /**
 * @ngdoc object
 * @name angularShiro.services.SessionManager
 * 
 * @description `SessionManager` manages the creation, maintenance, and clean-up
 *              of all application Sessions.
 * 
 */
  function SessionManager(sessionDAO) {
    this.sessionDAO = sessionDAO;
    /**
     * 
     * @ngdoc method
     * @function
     * @name SessionManager#start
     * @methodOf angularShiro.services.SessionManager
     * 
     * @description Starts a new session
     * 
     * @return {angularShiro.services.Session} the created `Session` object
     */
    this.start = function () {
      var session = new Session();
      this.sessionDAO.create(session);
      return session;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name SessionManager#getSession
     * @methodOf angularShiro.services.SessionManager
     * 
     * @description Retrieves the session corresponding to the specified ID, or
     *              `null` if no Session is found
     * 
     * @param {string}
     *                session ID
     * @return {angularShiro.services.Session} the `Session` object or `null` if
     *         no session is found
     */
    this.getSession = function (sessionId) {
      var session = this.sessionDAO.readSession(sessionId);
      if (session) {
        session.validate();
      }
      return session;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name SessionManager#update
     * @methodOf angularShiro.services.SessionManager
     * 
     * @description Update the session
     * 
     * @param {angularShiro.services.Session}
     *                session the session to update
     */
    this.update = function (session) {
      this.sessionDAO.update(session);
    };
  }
  /**
 * Function used to generate a unique session id
 * 
 * @returns
 */
  function guid() {
    function s4() {
      return Math.floor((1 + Math.random()) * 65536).toString(16).substring(1);
    }
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4();
  }
  /**
 * @ngdoc object
 * @name angularShiro.services.SessionDAO
 * 
 * @description Data Access Object design pattern specification to enable
 *              {@link Session} access to an EIS (Enterprise Information System)
 * 
 */
  function SessionDAO() {
    /**
     * 
     * @ngdoc method
     * @function
     * @name SessionDAO#create
     * @methodOf angularShiro.services.SessionDAO
     * 
     * @description Inserts a new Session record into the browser session
     *              storage
     * 
     * @param {angularShiro.services.Session}
     *                session the `Session` object
     * @return {object} the unique identifier of the created `Session` object
     */
    this.create = function (session) {
      var sessionId = guid();
      session.setId(sessionId);
      sessionStorage.setItem(sessionId, angular.toJson(session));
      return sessionId;
    };
    /**
     * @ngdoc method
     * @function
     * @name SessionDAO#readSession
     * @methodOf angularShiro.services.SessionDAO
     * 
     * @description Retrieves the session from the EIS uniquely identified by
     *              the specified `sessionId`.
     * 
     * @param {object}
     *                'sessionId' the system-wide unique identifier of the
     *                Session object to retrieve from the EIS.
     * @return {angularShiro.services.Session} the session identified by
     *         `sessionId`.
     */
    this.readSession = function (sessionId) {
      var session = null;
      var obj = angular.fromJson(sessionStorage.getItem(sessionId));
      if (obj) {
        session = new Session();
        angular.extend(session, obj);
      }
      return session;
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name SessionDAO#update
     * @methodOf angularShiro.services.SessionDAO
     * 
     * @description Updates (persists) data from a previously created Session
     *              instance in the the browser session storage identified by
     *              `session.getId()`.
     * 
     * @param {angularShiro.services.Session}
     *                `session` the Session to update
     */
    this.update = function (session) {
      sessionStorage.setItem(session.getId(), angular.toJson(session));
    };
    /**
     * 
     * @ngdoc method
     * @function
     * @name SessionDAO#delete
     * @methodOf angularShiro.services.SessionDAO
     * 
     * @description Removes the `session` identified by `session.getId()` from
     *              the browser session storage
     * 
     * @param {angularShiro.services.Session}
     *                `session` the session to delete.
     */
    this.delete = function (session) {
      sessionStorage.removeItem(session.getId());
    };  // /**
        // *
        // * @ngdoc method
        // * @function
        // * @name SessionDAO#getActiveSessions
        // * @methodOf angularShiro.services.SessionDAO
        // *
        // * @description Returns all sessions in the EIS that are considered active,
        // * meaning all sessions that haven't been stopped/expired
        // *
        // * @return {array} Sessions that are considered active, or an empty
        // * collection or `null` if there are no active sessions.
        // */
        // this.getActiveSessions = function(){
        //	
        // };
  }
  function PathMatcher() {
    this.DEFAULT_PATH_SEPARATOR = '/';
    this.pathSeparator = this.DEFAULT_PATH_SEPARATOR;
    this.setPathSeparator = function (pathSeparator) {
      this.pathSeparator = pathSeparator !== null ? pathSeparator : this.DEFAULT_PATH_SEPARATOR;
    };
    this.match = function (pattern, path) {
      return this.doMatch(pattern, path, true);
    };
    this.doMatch = function (pattern, path, fullMatch) {
      if (path.startsWith(this.pathSeparator) !== pattern.startsWith(this.pathSeparator)) {
        return false;
      }
      var pattDirs = pattern.split(this.pathSeparator);
      var pathDirs = path.split(this.pathSeparator);
      var pattIdxStart = 0;
      var pattIdxEnd = pattDirs.length - 1;
      var pathIdxStart = 0;
      var pathIdxEnd = pathDirs.length - 1;
      // Match all elements up to the first **
      while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
        var patDir = pattDirs[pattIdxStart];
        if ('**' === patDir) {
          break;
        }
        if (!this.matchStrings(patDir, pathDirs[pathIdxStart])) {
          return false;
        }
        pattIdxStart++;
        pathIdxStart++;
      }
      if (pathIdxStart > pathIdxEnd) {
        // Path is exhausted, only match if rest of pattern is * or **'s
        if (pattIdxStart > pattIdxEnd) {
          return pattern.endsWith(this.pathSeparator) ? path.endsWith(this.pathSeparator) : !path.endsWith(this.pathSeparator);
        }
        if (!fullMatch) {
          return true;
        }
        if (pattIdxStart === pattIdxEnd && pattDirs[pattIdxStart] === '*' && path.endsWith(this.pathSeparator)) {
          return true;
        }
        for (var i = pattIdxStart; i <= pattIdxEnd; i++) {
          if (pattDirs[i] !== '**') {
            return false;
          }
        }
        return true;
      } else if (pattIdxStart > pattIdxEnd) {
        // String not exhausted, but pattern is. Failure.
        return false;
      } else if (!fullMatch && '**' === pattDirs[pattIdxStart]) {
        // Path start definitely matches due to '**' part in pattern.
        return true;
      }
      // up to last '**'
      while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
        var parts = pattDirs[pattIdxEnd];
        if (parts === '**') {
          break;
        }
        if (!this.matchStrings(parts, pathDirs[pathIdxEnd])) {
          return false;
        }
        pattIdxEnd--;
        pathIdxEnd--;
      }
      if (pathIdxStart > pathIdxEnd) {
        // String is exhausted
        for (var j = pattIdxStart; j <= pattIdxEnd; j++) {
          if (pattDirs[j] !== '**') {
            return false;
          }
        }
        return true;
      }
      while (pattIdxStart !== pattIdxEnd && pathIdxStart <= pathIdxEnd) {
        var patIdxTmp = -1;
        for (var k = pattIdxStart + 1; k <= pattIdxEnd; k++) {
          if (pattDirs[k] === '**') {
            patIdxTmp = k;
            break;
          }
        }
        if (patIdxTmp === pattIdxStart + 1) {
          // '**/**' situation, so skip one
          pattIdxStart++;
          continue;
        }
        // Find the pattern between padIdxStart & padIdxTmp in str between
        // strIdxStart & strIdxEnd
        var patLength = patIdxTmp - pattIdxStart - 1;
        var strLength = pathIdxEnd - pathIdxStart + 1;
        var foundIdx = -1;
        for (var l = 0; l <= strLength - patLength; l++) {
          for (var m = 0; m < patLength; m++) {
            var subPat = pattDirs[pattIdxStart + m + 1];
            var subStr = pathDirs[pathIdxStart + l + m];
            if (!this.matchStrings(subPat, subStr)) {
            }
          }
          foundIdx = pathIdxStart + l;
          break;
        }
        if (foundIdx === -1) {
          return false;
        }
        pattIdxStart = patIdxTmp;
        pathIdxStart = foundIdx + patLength;
      }
      for (var n = pattIdxStart; n <= pattIdxEnd; n++) {
        if (pattDirs[n] !== '**') {
          return false;
        }
      }
      return true;
    };
    /**
     * 
     */
    this.matchStrings = function (pattern, str) {
      var patArr = pattern.split('');
      var strArr = str.split('');
      var patIdxStart = 0;
      var patIdxEnd = patArr.length - 1;
      var strIdxStart = 0;
      var strIdxEnd = strArr.length - 1;
      var ch;
      var containsStar = this.containsStar(pattern);
      if (!containsStar) {
        // No '*'s, so we make a shortcut
        if (patIdxEnd !== strIdxEnd) {
          return false;  // Pattern and string do not have the
                         // same size
        }
        for (var i = 0; i <= patIdxEnd; i++) {
          ch = patArr[i];
          if (ch !== '?') {
            if (ch !== strArr[i]) {
              return false;  // Character mismatch
            }
          }
        }
        return true;  // String matches against pattern
      }
      if (patIdxEnd === 0) {
        return true;  // Pattern contains only '*', which matches
                      // anything
      }
      // Process characters before first star
      while ((ch = patArr[patIdxStart]) !== '*' && strIdxStart <= strIdxEnd) {
        if (ch !== '?') {
          if (ch !== strArr[strIdxStart]) {
            return false;  // Character mismatch
          }
        }
        patIdxStart++;
        strIdxStart++;
      }
      if (strIdxStart > strIdxEnd) {
        // All characters in the string are used. Check if only '*'s
        // are
        // left in the pattern. If so, we succeeded. Otherwise
        // failure.
        for (var j = patIdxStart; j <= patIdxEnd; j++) {
          if (patArr[j] !== '*') {
            return false;
          }
        }
        return true;
      }
      // Process characters after last star
      while ((ch = patArr[patIdxEnd]) !== '*' && strIdxStart <= strIdxEnd) {
        if (ch !== '?') {
          if (ch !== strArr[strIdxEnd]) {
            return false;  // Character mismatch
          }
        }
        patIdxEnd--;
        strIdxEnd--;
      }
      if (strIdxStart > strIdxEnd) {
        // All characters in the string are used. Check if only '*'s
        // are
        // left in the pattern. If so, we succeeded. Otherwise
        // failure.
        for (var k = patIdxStart; k <= patIdxEnd; k++) {
          if (patArr[k] !== '*') {
            return false;
          }
        }
        return true;
      }
      // process pattern between stars. padIdxStart and patIdxEnd
      // point
      // always to a '*'.
      while (patIdxStart !== patIdxEnd && strIdxStart <= strIdxEnd) {
        var patIdxTmp = -1;
        for (var l = patIdxStart + 1; l <= patIdxEnd; l++) {
          if (patArr[l] === '*') {
            patIdxTmp = l;
            break;
          }
        }
        if (patIdxTmp === patIdxStart + 1) {
          // Two stars next to each other, skip the first one.
          patIdxStart++;
          continue;
        }
        // Find the pattern between padIdxStart & padIdxTmp in str
        // between
        // strIdxStart & strIdxEnd
        var patLength = patIdxTmp - patIdxStart - 1;
        var strLength = strIdxEnd - strIdxStart + 1;
        var foundIdx = -1;
        strLoop:
          for (var m = 0; m <= strLength - patLength; m++) {
            for (var n = 0; n < patLength; n++) {
              ch = patArr[patIdxStart + n + 1];
              if (ch !== '?') {
                if (ch !== strArr[strIdxStart + m + n]) {
                  continue strLoop;
                }
              }
            }
            foundIdx = strIdxStart + m;
            break;
          }
        if (foundIdx === -1) {
          return false;
        }
        patIdxStart = patIdxTmp;
        strIdxStart = foundIdx + patLength;
      }
      // All characters in the string are used. Check if only '*'s are
      // left
      // in the pattern. If so, we succeeded. Otherwise failure.
      for (var p = patIdxStart; p <= patIdxEnd; p++) {
        if (patArr[p] !== '*') {
          return false;
        }
      }
      return true;
    };
    this.containsStar = function (str) {
      var containsStar = false;
      if (str && str !== null) {
        for (var i = 0; i < str.length; i++) {
          if (str.charAt(i) === '*') {
            containsStar = true;
            break;
          }
        }
      }
      return containsStar;
    };
  }
  /**
 * 
 * @param angularShiroConfig
 * @returns
 */
  var filtersResolver = [
      'angularShiroConfig',
      '$injector',
      function FiltersResolver(config, $injector) {
        var matcher = new PathMatcher();
        var urls = config.urls;
        return {
          resolve: function (path) {
            var stringFilters = this.resolveStringFilters(path);
            return this.resolveFilters(stringFilters);
          },
          resolveFilters: function (str) {
            var filters = [];
            if (str) {
              var tokens = str.split(',');
              for (var i = 0; i < tokens.length; i++) {
                var filter = this.resolveFilter(tokens[i]);
                if (filter !== null) {
                  filters.push(filter);
                }
              }
            }
            return filters;
          },
          resolveFilter: function (stringFilter) {
            var filter = null;
            var name = this.resolveFilterName(stringFilter);
            var args = this.resolveFilterArgs(stringFilter);
            filter = $injector.get(name);
            return function () {
              return filter.execute(args);
            };
          },
          resolveFilterName: function (stringFilter) {
            var name = stringFilter;
            if (this.isParametrizedFilter(stringFilter)) {
              name = stringFilter.substr(0, stringFilter.indexOf('['));
            }
            return trim(name);
          },
          resolveFilterArgs: function (stringFilter) {
            var args = null;
            if (this.isParametrizedFilter(stringFilter)) {
              var sIdx = stringFilter.indexOf('[');
              var eIdx = stringFilter.indexOf(']');
              var str = stringFilter.substr(sIdx + 1, eIdx - sIdx - 1);
              var tokens = str.split(',');
              args = [];
              angular.forEach(tokens, function (token) {
                args.push(trim(token));
              });
            }
            return args;
          },
          resolveStringFilters: function (path) {
            var stringFilters = null;
            for (var pattern in urls) {
              if (matcher.match(pattern, path)) {
                stringFilters = urls[pattern];
                break;
              }
            }
            return stringFilters;
          },
          isParametrizedFilter: function (stringFilter) {
            return stringFilter.indexOf('perms') > -1 || stringFilter.indexOf('roles') > -1;
          }
        };
      }
    ];
  var $$onAccessDenied = function ($timeout, $location, config) {
    if (config && config.login && config.login.path) {
      $timeout(function () {
        $location.path(config.login.path);
      });
    }
  };
  /**
 * Filter that allows access to a path immeidately without performing security
 * checks of any kind.
 * 
 * @returns
 */
  var anonymousFilter = [
      'subject',
      '$log',
      function AnonymousFilter(subject, $log) {
        return {
          execute: function () {
            $log.debug('anon::execute');
            return true;
          }
        };
      }
    ];
  /**
 * The Subject must be authenticated for the request to continue, otherwise
 * forces the user to login by redirecting to the configured login.path
 */
  var formAuthenticationFilter = [
      '$rootScope',
      'subject',
      'angularShiroConfig',
      '$location',
      '$timeout',
      '$log',
      function FormAuthenticationFilter($rootScope, subject, config, $location, $timeout, $log) {
        return {
          execute: function () {
            $log.debug('authc::execute');
            return this.isAccessAllowed() || this.onAccessDenied();
          },
          isAccessAllowed: function () {
            return subject.isAuthenticated();
          },
          onAccessDenied: function () {
            $$onAccessDenied($timeout, $location, config);
            return false;
          }
        };
      }
    ];
  /**
 * Simple Filter that, upon location change, will immediately log-out the
 * currently executing <code>subject</code> and then redirect them to a
 * configured <code>redirectUrl</code>
 */
  var logoutFilter = [
      'subject',
      'angularShiroConfig',
      '$location',
      '$timeout',
      '$log',
      function LogoutFilter(subject, config, $location, $timeout, $log) {
        return {
          execute: function () {
            $log.debug('logoutFilter::execute');
            subject.logout();
            $location.search('sessionId', null);
            if (config.logout && config.logout.path) {
              $location.path(config.logout.path);
            }
            return true;
          }
        };
      }
    ];
  /**
 * Filter that allows access if the current user has the permissions specified
 * by the mapped value, or denies access if the user does not have all of the
 * permissions specified.
 */
  var permsFilter = [
      'subject',
      'angularShiroConfig',
      '$location',
      '$timeout',
      '$log',
      function PermsFilter(subject, config, $location, $timeout, $log) {
        return {
          execute: function (permissions) {
            $log.debug('perms::execute');
            return this.isAccessAllowed(permissions) || this.onAccessDenied();
          },
          isAccessAllowed: function (permissions) {
            return subject.isPermittedAll(permissions);
          },
          onAccessDenied: function () {
            $$onAccessDenied($timeout, $location, config);
            return false;
          }
        };
      }
    ];
  /**
 * Filter that allows access if the current user has the roles specified by the
 * mapped value, or denies access if the user does not have all of the roles
 * specified
 */
  var rolesFilter = [
      'subject',
      'angularShiroConfig',
      '$location',
      '$timeout',
      '$log',
      function RolesFilter(subject, config, $location, $timeout, $log) {
        return {
          execute: function (roles) {
            $log.debug('roles::execute');
            return this.isAccessAllowed(roles) || this.onAccessDenied();
          },
          isAccessAllowed: function (roles) {
            return subject.hasAllRoles(roles);
          },
          onAccessDenied: function () {
            $$onAccessDenied($timeout, $location, config);
            return false;
          }
        };
      }
    ];
  /**
 * @ngdoc object
 * @name angularShiro.services.AuthenticationResponseParser
 * 
 * @description `AuthenticationResponseParser` is responsible of validating then
 *              parsing the response received from the authentication service
 *              backend
 * 
 * #Response
 * 
 * The response returned from the backend have to be a `json` object that comply
 * to the following structure :
 * 
 * <pre>
 * {
 *     info : {
 * 	authc : {
 * 	    principal : {
 * 		// the Suject/User principal, for example
 * 		&quot;login&quot; : &quot;edegas&quot;,
 * 		&quot;apiKey&quot; : &quot;*******&quot;
 * 	    },
 * 	    credentials : {
 * 		// the Subject/User credentials, for example
 * 		&quot;name&quot; : &quot;Edgar Degas&quot;,
 * 		&quot;email&quot; : &quot;degas@mail.com&quot;
 * 	    }
 * 	},
 * 	authz : {
 * 	    roles : [
 * 	    // list of the Subject/User roles, for example
 * 	    &quot;GUEST&quot; ],
 * 	    permissions : [
 * 	    // list of the Subject/User permissions, for example
 * 	    &quot;newsletter$read&quot;, &quot;book$*&quot;, ]
 * 	}
 *     }
 * }
 * </pre>
 * 
 * @since 0.0.1
 */
  function AuthenticationResponseParser() {
    /**
     * 
     * @ngdoc method
     * @function
     * @name AuthenticationResponseParser#parse
     * @methodOf angularShiro.services.AuthenticationResponseParser
     * 
     * @description Validates then parse the data received from the backend
     *              authentication service
     * 
     * @param {Object}
     *                data the token encapsulating the subject's principals and
     *                credentials to be passed to the Authentication subsystem
     *                for verification.
     * 
     * @returns {object} the parsed data
     * 
     */
    this.parse = function (data) {
      this.checkValidity(data);
      return {
        authc: this.parseAuthc(data.info.authc),
        authz: this.parseAuthz(data.info.authz)
      };
    };
    this.parseAuthc = function (authc) {
      return new AuthenticationInfo(authc.principal, authc.credentials);
    };
    this.parseAuthz = function (authz) {
      return new AuthorizationInfo(authz.roles, authz.permissions);
    };
    /**
     * 
     */
    this.checkValidity = function (data) {
      if (!angular.isDefined(data) || !angular.isDefined(data.info) || !this.isAuthcValid(data.info) || !this.isAuthzValid(data.info)) {
        var msg = 'Response does not match expected structure.';
        throw {
          'name': 'ParseException',
          'message': msg
        };
      }
    };
    this.isAuthcValid = function (info) {
      var valid = angular.isDefined(info.authc);
      if (valid) {
        var authc = info.authc;
        valid = angular.isDefined(authc.principal) && angular.isDefined(authc.credentials);
      }
      return valid;
    };
    this.isAuthzValid = function (info) {
      var valid = angular.isDefined(info.authz);
      if (valid) {
        var authz = info.authz;
        valid = angular.isDefined(authz.roles) && angular.isDefined(authz.permissions);
      }
      return valid;
    };
  }
  /**
 * 
 * @ngdoc service
 * @name angularShiro.services.Subject
 * @requires angularShiro.services.Authenticator
 * @requires angularShiro.services.Authorizer
 * @requires angularShiro.services.AuthenticationResponseParser
 * 
 * @description A <code>Subject</code> represents state and security
 *              operations for an application user. Operations goes from
 *              authentication (login and logout) to authorization.
 * 
 * @class Subject
 * @constructor
 * 
 * @param {Authenticator}
 *                authenticator instance of <code>Authenticator</code>
 * 
 * @param {Authorizer}
 *                authorizer instance of <code>Authorizer</code>
 * 
 * @param {AuthenticationResponseParser}
 *                authenticationResponseParser instance of
 *                <code>AuthenticationResponseParser</code>
 * 
 * 
 * @since 0.0.1
 */
  function Subject(authenticator, authorizer, authenticationResponseParser) {
    /**
     * @name Subject#authenticated
     * @description flag indicating if the current Subject is authenticated or
     *              not
     * @propertyOf angularShiro.services.Subject
     */
    this.authenticated = false;
    /**
     * 
     */
    this.sessionManager = new SessionManager(new SessionDAO());
    /**
     * @private
     */
    this.session = null;
    /**
     * @name Subject#authorizer
     * @propertyOf angularShiro.services.Subject
     * @description <code>Authorizer</code> instance in charge of
     *              authorization operations
     * 
     */
    this.authorizer = authorizer;
    /**
     * @name Subject#authenticationInfo
     * @propertyOf angularShiro.services.Subject
     * @description this Subject authenticiation infos
     */
    this.authenticationInfo;
    /**
     * @name Subject#remembered
     * @propertyOf angularShiro.services.Subject
     * @description flag indicating if the current subject is to be remembered
     */
    this.remembered = false;
    /**
     * 
     * @ngdoc method
     * @function
     * @name Subject#login
     * @methodOf angularShiro.services.Subject
     * 
     * @description Performs a login attempt for this Subject.
     * 
     * On unsuccessful attempts returned <code>httpPromise</code> is rejected;
     * 
     * On the contrary authentication informations along with with the submitted
     * principals/credentials are associated with this Subject and the method
     * will return quietly. On successful authentication, the Subject instance
     * is considered authenticated so that the isAuthenticated() method will
     * return true and the getPrincipal() method must return a non-null value
     * and .
     * 
     * @param {UsernamePasswordToken}
     *                token the token encapsulating the subject's principals and
     *                credentials to be passed to the Authentication subsystem
     *                for verification.
     * 
     * @returns {Promise} Returns a promise
     * 
     */
    this.login = function (token) {
      var promise = authenticator.authenticate(token);
      var me = this;
      promise.then(function (data, status, headers, config) {
        var infos = authenticationResponseParser.parse(data);
        me.authenticationInfo = infos.authc;
        me.authorizer.setAuthorizationInfo(infos.authz);
        me.authenticated = true;
        if (token.isRememberMe()) {
          // put the token in session to auto login if needed
          var session = me.getSession(true);
          session.setAttribute('token', token);
          me.sessionManager.update(session);
          me.remembered = true;
        }
        token.clear();
      }, function (data, status, headers, config) {
        me.clear();
      });
      return promise;
    };
    /**
     * 
     */
    this.rememberMe = function (sessionId) {
      var output = false;
      var session = this.sessionManager.getSession(sessionId);
      if (session !== null && session.getAttribute('token')) {
        var token = new UsernamePasswordToken();
        token.deserialize(session.getAttribute('token'));
        // auto login to reload authorization infos
        output = this.login(token);
      }
      return output;
    };
    /**
     * @ngdoc method
     * @name Subject#logout
     * @methodOf angularShiro.services.Subject
     * 
     * @description Logs out this Subject and invalidates and/or removes any
     *              associated entities, such as <code>Session</code> and
     *              authorization data. After this method is called, the Subject
     *              is considered 'anonymous' and may continue to be used for
     *              another log-in if desired.
     * 
     * @method logout
     * @public
     */
    this.logout = function () {
      this.clear();
    };
    /**
     * Returns the application <code>Session</code> associated with this
     * Subject/User. If no session exists when this method is called, a new
     * session will be created, associated with this Subject, and then returned.
     * 
     * @return the application <code>Session</code> associated with this
     *         SubjectUser
     */
    this.getSession = function (create) {
      if (this.session === null && create) {
        this.session = this.sessionManager.start();
      }
      return this.session;
    };
    /**
     * @ngdoc method
     * @name Subject#getPrincipal
     * @methodOf angularShiro.services.Subject
     * 
     * @description Returns this Subject's application-wide uniquely identifying
     *              principal, or <code>null</code> if this Subject is
     *              anonymous
     * 
     * @return {*} this Subject's application-specific unique identity
     */
    this.getPrincipal = function () {
      var principal = '';
      if (angular.isDefined(this.authenticationInfo) && angular.isObject(this.authenticationInfo)) {
        principal = this.authenticationInfo.getPrincipal();
      }
      return principal;
    };
    /**
     * @ngdoc method
     * @name Subject#isAuthenticated
     * @methodOf angularShiro.services.Subject
     * 
     * @description Returns <code>true</code> if the <code>Subject</code>
     *              has provided valid credentials, <code>false</code>
     *              otherwise.
     * 
     * @return {boolean} <code>true</code> if the <code>Subject</code> is
     *         authenticated, <code>false</code> otherwise
     */
    this.isAuthenticated = function () {
      return this.authenticated;
    };
    /**
     * @ngdoc method
     * @name Subject#isRemembered
     * @methodOf angularShiro.services.Subject
     * 
     * @description Returns `true` if this Subject is remembered from a
     *              successful authentication.
     * 
     * @return {boolean} Returns `true` if this Subject is remembered from a
     *         successful authentication.
     */
    this.isRemembered = function () {
      return this.remembered;
    };
    /**
     * @ngdoc method
     * @name Subject#hasRole
     * @methodOf angularShiro.services.Subject
     * 
     * @description Returns <code>true</code> if the <code>Subject</code>
     *              has the specified role, <code>false</code> otherwise.
     * 
     * 
     * @param {string}
     *                roleIdentifier the application-specific role identifier
     *                (usually a role id or role name)
     * @return {boolean} <code>true</code> if the <code>Subject</code> has
     *         the specified role, <code>false</code> otherwise
     */
    this.hasRole = function (role) {
      return this.isAuthenticated() && angular.isDefined(this.authorizer) && this.authorizer.hasRole(role);
    };
    /**
     * @ngdoc method
     * @name Subject#hasRoles
     * @methodOf angularShiro.services.Subject
     * 
     * @description Checks if the <code>Subject</code> has the specified
     *              roles, returning a boolean array indicating which roles are
     *              associated
     * 
     * @param {array}
     *                roleIdentifiers the application-specific role identifiers
     *                to check (usually role ids or role names)
     * @return {array} a boolean array where indices correspond to the index of
     *         the roles in the given identifiers. A <code>true</code> value
     *         indicates the <code>Subject</code> has the role at that index.
     *         <code>false</code> indicates the <code>Subject</code> does
     *         not have the role at that index
     */
    this.hasRoles = function (roles) {
      var result = [];
      angular.forEach(roles, function (role) {
        result.push(this.hasRole(role));
      }, this);
      return result;
    };
    /**
     * @ngdoc method
     * @name Subject#hasAllRoles
     * @methodOf angularShiro.services.Subject
     * 
     * @description Returns <code>true</code> if the <code>Subject</code>
     *              has all of the specified roles, <code>false</code>
     *              otherwise.
     * 
     * @param {array}
     *                roleIdentifiers the application-specific role identifiers
     *                to check (usually role ids or role names)
     * 
     * @return {boolean} <code>true</code> if the <code>Subject</code> has
     *         all the roles, <code>false</code> otherwise.
     */
    this.hasAllRoles = function (roles) {
      return this.isAuthenticated() && this.authorizer.hasAllRoles(roles);
    };
    /**
     * @ngdoc method
     * @name Subject#isPermitted
     * @methodOf angularShiro.services.Subject
     * 
     * @description Returns <code>true</code> if the current
     *              <code>Subject</code> has the specified permission(s),
     *              <code>false</code> otherwise.
     * 
     * @param {string |
     *                Permission | array} permission a permission
     * @return {boolean | array} <code>true</code> if the current
     *         <code>Subject</code> has the specified permission(s),
     *         <code>false</code> otherwise.
     * 
     */
    this.isPermitted = function (permissions) {
      return this.isAuthenticated() && this.authorizer.isPermitted(permissions);
    };
    /**
     * @ngdoc method
     * @name Subject#isPermittedAll
     * @methodOf angularShiro.services.Subject
     * 
     * @description Returns <code>true</code> if this Subject implies all of
     *              the specified permissions, <code>false</code> otherwise
     * 
     * @param {array}
     *                permissions the permissions to check
     * 
     * @return {boolean} <code>true</code> if the current <code>Subject</code>
     *         implies all of the specified permissions, <code>false</code>
     *         otherwise
     */
    this.isPermittedAll = function (permissions) {
      return this.isAuthenticated() && this.authorizer.isPermittedAll(permissions);
    };
    /**
     * 
     */
    this.clear = function () {
      this.authenticated = false;
      this.authenticationInfo = null;
      this.authorizer.clear();
      this.remembered = false;
    };
  }
  angular.module('angularShiro.templates', ['templates/usernamePasswordForm.html']);
  angular.module('templates/usernamePasswordForm.html', []).run([
    '$templateCache',
    function ($templateCache) {
      $templateCache.put('templates/usernamePasswordForm.html', '<div>\n' + '\t<div style="padding-top: 10px; padding-bottom: 10px;"\n' + '\t\tdata-ng-show="error">\n' + '\t\t<span class="label label-danger label-important"\n' + '\t\t\tdata-ng-bind="labels[\'connection.denied.message\']"> </span>\n' + '\t</div>\n' + '\t<form name="loginForm" data-role="form" data-ng-submit="submit()"\n' + '\t\tnovalidate>\n' + '\t\t<div class="form-group">\n' + '\t\t\t<input name="username" type="text" class="form-control"\n' + '\t\t\t\tplaceholder="{{labels[\'field.username.placeholder\']}}"\n' + '\t\t\t\tdata-ng-model="token.username" required />\n' + '\t\t</div>\n' + '\t\t<div class="form-group">\n' + '\t\t\t<input name="password" type="password" class="form-control"\n' + '\t\t\t\tplaceholder="{{labels[\'field.password.placeholder\']}}"\n' + '\t\t\t\tdata-ng-model="token.password" required />\n' + '\t\t</div>\n' + '\t\t<div class="form-group">\n' + '\t\t\t<button type="submit" class="btn btn-primary btn-block"\n' + '\t\t\t\tdata-ng-disabled="loginForm.$pristine || loginForm.$invalid">\n' + '\t\t\t\t<span data-ng-bind="labels[\'button.submit.label\']"></span>\n' + '\t\t\t</button>\n' + '\t\t</div>\n' + '\t</form>\n' + '</div>');
    }
  ]);
  /**
 * @ngdoc directive
 * @name angularShiro.directives.authenticated
 * @restrict A
 * 
 * @description Display its content only if the current Subject has successfully authenticated 
 * 			    during their current session.
 * 
 * @element ANY
 * @scope
 * @priority 600
 * 
 */
  var authenticatedDirective = [
      'subject',
      '$animate',
      function (subject, $animate) {
        return {
          transclude: 'element',
          priority: 600,
          terminal: true,
          restrict: 'A',
          $$tlb: true,
          link: function ($scope, $element, $attr, ctrl, $transclude) {
            var block, childScope, previousElements;
            $scope.$watch(function () {
              return subject.authenticated;
            }, function () {
              if (subject.isAuthenticated()) {
                if (!childScope) {
                  childScope = $scope.$new();
                  $transclude(childScope, function (clone) {
                    block = { clone: clone };
                    $animate.enter(clone, $element.parent(), $element);
                  });
                }
              } else {
                if (previousElements) {
                  previousElements.remove();
                  previousElements = null;
                }
                if (childScope) {
                  childScope.$destroy();
                  childScope = null;
                }
                if (block) {
                  previousElements = getBlockElements(block.clone);
                  $animate.leave(previousElements, function () {
                    previousElements = null;
                  });
                  block = null;
                }
              }
            });
          }
        };
      }
    ];
  /**
 * @ngdoc directive
 * @name angularShiro.directives.hasAnyPermission
 * @restrict A
 * 
 * @description Display if the current `Subject` is assigned any of the
 *              specified permissions (for example,
 *              `['newsletter:read','newsletter:edit']`)
 * 
 * @element ANY
 * @scope
 * @priority 600
 * @param {string |
 *                array | expression} hasAnyPermission the specified specified
 *                permissions (for example,
 *                `['newsletter:read','newsletter:edit']`)
 */
  var hasAnyPermissionDirective = [
      'subject',
      '$animate',
      function (subject, $animate) {
        return {
          transclude: 'element',
          priority: 600,
          terminal: true,
          restrict: 'A',
          $$tlb: true,
          link: function ($scope, $element, $attr, ctrl, $transclude) {
            var block, childScope, previousElements;
            $scope.$watch(function () {
              return subject.authenticated;
            }, function () {
              var permissions = $scope.$eval($attr.hasAnyPermission) || $attr.hasAnyPermission;
              permissions = angular.isArray(permissions) ? permissions : [permissions];
              var results = subject.isPermitted(permissions);
              if (angular.isArray(results) && results.indexOf(true) > -1) {
                if (!childScope) {
                  childScope = $scope.$new();
                  $transclude(childScope, function (clone) {
                    block = { clone: clone };
                    $animate.enter(clone, $element.parent(), $element);
                  });
                }
              } else {
                if (previousElements) {
                  previousElements.remove();
                  previousElements = null;
                }
                if (childScope) {
                  childScope.$destroy();
                  childScope = null;
                }
                if (block) {
                  previousElements = getBlockElements(block.clone);
                  $animate.leave(previousElements, function () {
                    previousElements = null;
                  });
                  block = null;
                }
              }
            });
          }
        };
      }
    ];
  /**
 * @ngdoc directive
 * @name angularShiro.directives.hasAnyRole
 * @restrict A
 * 
 * @description Display if the current `Subject` is assigned any of the
 *              specified roles (for example, `'ADMIN'` or
 *              `['ADMIN','DEVELOPPER','GUEST']`)
 * 
 * @element ANY
 * @scope
 * @priority 600
 * @param {string |
 *                array | expression} hasAnyRole the specified role names (for
 *                example, `'ADMIN'` or `['ADMIN','DEVELOPPER','GUEST']`)
 */
  var hasAnyRoleDirective = [
      'subject',
      '$animate',
      function (subject, $animate) {
        return {
          transclude: 'element',
          priority: 600,
          terminal: true,
          restrict: 'A',
          $$tlb: true,
          link: function ($scope, $element, $attr, ctrl, $transclude) {
            var block, childScope, previousElements;
            $scope.$watch(function () {
              return subject.authenticated;
            }, function () {
              var roles = $scope.$eval($attr.hasAnyRole) || $attr.hasAnyRole;
              roles = angular.isArray(roles) ? roles : [roles];
              if (subject.hasRoles(roles).indexOf(true) > -1) {
                if (!childScope) {
                  childScope = $scope.$new();
                  $transclude(childScope, function (clone) {
                    block = { clone: clone };
                    $animate.enter(clone, $element.parent(), $element);
                  });
                }
              } else {
                if (previousElements) {
                  previousElements.remove();
                  previousElements = null;
                }
                if (childScope) {
                  childScope.$destroy();
                  childScope = null;
                }
                if (block) {
                  previousElements = getBlockElements(block.clone);
                  $animate.leave(previousElements, function () {
                    previousElements = null;
                  });
                  block = null;
                }
              }
            });
          }
        };
      }
    ];
  /**
 * @ngdoc directive
 * @name angularShiro.directives.hasPermission
 * @restrict A
 * 
 * @description Display its content only if the current `Subject` 'has'
 *              (implies) the specified permission (for example,
 *              `newletter$edit`)
 * 
 * Angular `$parse` service does not support `:` character so we replaced it by
 * the character `$`
 * 
 * @element ANY
 * @scope
 * @priority 600
 * @param {string |
 *                expression} hasPermission the permission to check (for
 *                example, `newletter$edit`)
 */
  var hasPermissionDirective = [
      'subject',
      '$animate',
      function (subject, $animate) {
        return {
          transclude: 'element',
          priority: 600,
          terminal: true,
          restrict: 'A',
          $$tlb: true,
          link: function ($scope, $element, $attr, ctrl, $transclude) {
            var block, childScope, previousElements;
            $scope.$watch(function () {
              return subject.authenticated;
            }, function (permission) {
              permission = $scope.$eval($attr.hasPermission) || $attr.hasPermission;
              if (subject.isPermitted(permission)) {
                if (!childScope) {
                  childScope = $scope.$new();
                  $transclude(childScope, function (clone) {
                    block = { clone: clone };
                    $animate.enter(clone, $element.parent(), $element);
                  });
                }
              } else {
                if (previousElements) {
                  previousElements.remove();
                  previousElements = null;
                }
                if (childScope) {
                  childScope.$destroy();
                  childScope = null;
                }
                if (block) {
                  previousElements = getBlockElements(block.clone);
                  $animate.leave(previousElements, function () {
                    previousElements = null;
                  });
                  block = null;
                }
              }
            });
          }
        };
      }
    ];
  /**
 * @ngdoc directive
 * @name angularShiro.directives.hasRole
 * @restrict A
 * 
 * @description Display if the current Subject has the specified role (for example, `ADMIN`)
 * 
 * 
 * @element ANY
 * @scope
 * @priority 600
 * @param {string | expression}
 *            hasRole the role to check (for example, `ADMIN`)
 * 
 */
  var hasRoleDirective = [
      'subject',
      '$animate',
      function (subject, $animate) {
        return {
          transclude: 'element',
          priority: 600,
          terminal: true,
          restrict: 'A',
          $$tlb: true,
          link: function ($scope, $element, $attr, ctrl, $transclude) {
            var block, childScope, previousElements;
            $scope.$watch(function () {
              return subject.authenticated;
            }, function hasRoleWatchAction() {
              var role = $attr.hasRole;
              if (subject.hasRole(role)) {
                if (!childScope) {
                  childScope = $scope.$new();
                  $transclude(childScope, function (clone) {
                    block = { clone: clone };
                    $animate.enter(clone, $element.parent(), $element);
                  });
                }
              } else {
                if (previousElements) {
                  previousElements.remove();
                  previousElements = null;
                }
                if (childScope) {
                  childScope.$destroy();
                  childScope = null;
                }
                if (block) {
                  previousElements = getBlockElements(block.clone);
                  $animate.leave(previousElements, function () {
                    previousElements = null;
                  });
                  block = null;
                }
              }
            });
          }
        };
      }
    ];
  /**
 * @ngdoc directive
 * @name angularShiro.directives.lacksPermission
 * @restrict A
 * 
 * @description Display if the current `Subject` 'has' (implies) the specified
 *              permission (for example, `newletter$edit`)
 * 
 * Angular `$parse` service does not support `:` character so we replaced it by
 * the character `$`
 * 
 * @element ANY
 * @scope
 * @priority 600
 * @param {string |
 *                expression} lacksPermission the permission to check (for
 *                example, `newletter$edit`)
 */
  var lacksPermissionDirective = [
      'subject',
      '$animate',
      function (subject, $animate) {
        return {
          transclude: 'element',
          priority: 600,
          terminal: true,
          restrict: 'A',
          $$tlb: true,
          link: function ($scope, $element, $attr, ctrl, $transclude) {
            var block, childScope, previousElements;
            $scope.$watch(function () {
              return subject.authenticated;
            }, function () {
              var permission = $scope.$eval($attr.lacksPermission) || $attr.lacksPermission;
              if (!subject.isPermitted(permission)) {
                if (!childScope) {
                  childScope = $scope.$new();
                  $transclude(childScope, function (clone) {
                    block = { clone: clone };
                    $animate.enter(clone, $element.parent(), $element);
                  });
                }
              } else {
                if (previousElements) {
                  previousElements.remove();
                  previousElements = null;
                }
                if (childScope) {
                  childScope.$destroy();
                  childScope = null;
                }
                if (block) {
                  previousElements = getBlockElements(block.clone);
                  $animate.leave(previousElements, function () {
                    previousElements = null;
                  });
                  block = null;
                }
              }
            });
          }
        };
      }
    ];
  /**
 * @ngdoc directive
 * @name angularShiro.directives.lacksRole
 * @restrict A
 * 
 * @description Display if the current Subject is **NOT** assigned the specified
 *              role (for example, `ADMIN`)
 * 
 * @element ANY
 * @scope
 * @priority 600
 * @param {string |
 *            expression} lacksRole the excluded role (for example, `ADMIN`)
 * 
 */
  var lacksRoleDirective = [
      'subject',
      '$animate',
      function (subject, $animate) {
        return {
          transclude: 'element',
          priority: 600,
          terminal: true,
          restrict: 'A',
          $$tlb: true,
          link: function ($scope, $element, $attr, ctrl, $transclude) {
            var block, childScope, previousElements;
            $scope.$watch(function () {
              return subject.authenticated;
            }, function () {
              var role = $attr.lacksRole;
              if (!subject.hasRole(role)) {
                if (!childScope) {
                  childScope = $scope.$new();
                  $transclude(childScope, function (clone) {
                    block = { clone: clone };
                    $animate.enter(clone, $element.parent(), $element);
                  });
                }
              } else {
                if (previousElements) {
                  previousElements.remove();
                  previousElements = null;
                }
                if (childScope) {
                  childScope.$destroy();
                  childScope = null;
                }
                if (block) {
                  previousElements = getBlockElements(block.clone);
                  $animate.leave(previousElements, function () {
                    previousElements = null;
                  });
                  block = null;
                }
              }
            });
          }
        };
      }
    ];
  /**
 * @ngdoc directive
 * @name angularShiro.directives.notAuthenticated
 * @restrict A
 * 
 * @description Display if the
 *              current Subject has NOT yet successfully authenticated during
 *              the current session.
 * 
 * 
 * @element ANY
 * @scope
 * @priority 600
 * 
 */
  var notAuthenticatedDirective = [
      'subject',
      '$animate',
      function (subject, $animate) {
        return {
          transclude: 'element',
          priority: 600,
          terminal: true,
          restrict: 'A',
          $$tlb: true,
          link: function ($scope, $element, $attr, ctrl, $transclude) {
            var block, childScope, previousElements;
            $scope.$watch(function () {
              return subject.authenticated;
            }, function () {
              if (!subject.isAuthenticated()) {
                if (!childScope) {
                  childScope = $scope.$new();
                  $transclude(childScope, function (clone) {
                    block = { clone: clone };
                    $animate.enter(clone, $element.parent(), $element);
                  });
                }
              } else {
                if (previousElements) {
                  previousElements.remove();
                  previousElements = null;
                }
                if (childScope) {
                  childScope.$destroy();
                  childScope = null;
                }
                if (block) {
                  previousElements = getBlockElements(block.clone);
                  $animate.leave(previousElements, function () {
                    previousElements = null;
                  });
                  block = null;
                }
              }
            });
          }
        };
      }
    ];
  var principalDirective = [
      'subject',
      function (subject) {
        return {
          restrict: 'E',
          replace: true,
          scope: {},
          template: '<span class="principal" ng-bind="getPrincipal()"></span>',
          link: function (scope, element, attr) {
            scope.getPrincipal = function () {
              var text = subject.getPrincipal();
              if (angular.isObject(text)) {
                if (attr.property && angular.isDefined(text[attr.property])) {
                  text = text[attr.property];
                } else {
                  text = angular.toJson(text);
                }
              }
              return text;
            };
          }
        };
      }
    ];
  var usernamePasswordFormDirective = [
      'subject',
      'usernamePasswordToken',
      function (subject, token) {
        var labels = {
            'field.username.placeholder': 'Username',
            'field.password.placeholder': 'Password',
            'button.submit.label': 'Connection',
            'connection.denied.message': 'Username and password do not match'
          };
        return {
          restrict: 'E',
          replace: true,
          templateUrl: 'templates/usernamePasswordForm.html',
          scope: {
            onSuccess: '&',
            onError: '&'
          },
          link: function ($scope, $element, $attr) {
            $scope.error = false;
            $scope.token = token;
            if (angular.isDefined($attr.labels)) {
              $scope.labels = angular.extend(labels, angular.fromJson($attr.labels));
            } else {
              $scope.labels = labels;
            }
            $scope.$watch('token.getPrincipal()', function (value) {
              $scope.error = false;
            });
            $scope.$watch('token.getCredentials()', function (value) {
              $scope.error = false;
            });
            $scope.submit = function () {
              subject.login(token).then(function (data) {
                if (angular.isDefined($scope.onSuccess)) {
                  $scope.onSuccess({ data: data });
                }
                token.username = token.password = null;
              }, function (data) {
                $scope.error = true;
                if (angular.isDefined($scope.onError)) {
                  $scope.onError({ data: data });
                }
              });
            };
          }
        };
      }
    ];
  var angularShiroServicesModule = angular.module('angularShiro.services', []);
  angularShiroServicesModule.provider('authenticator', AuthenticatorProvider);
  angularShiroServicesModule.provider('angularShiroConfig', AngularShiroConfigProvider);
  angularShiroServicesModule.factory('subject', [
    'authenticator',
    'authorizer',
    'authenticationResponseParser',
    function (authenticator, authorizer, authenticationResponseParser) {
      return new Subject(authenticator, authorizer, authenticationResponseParser);
    }
  ]);
  angularShiroServicesModule.factory('usernamePasswordToken', function () {
    return new UsernamePasswordToken();
  });
  angularShiroServicesModule.factory('authorizer', function () {
    return new Authorizer();
  });
  angularShiroServicesModule.factory('authenticationResponseParser', function () {
    return new AuthenticationResponseParser();
  });
  var filters = {
      'anon': anonymousFilter,
      'authc': formAuthenticationFilter,
      'logout': logoutFilter,
      'perms': permsFilter,
      'roles': rolesFilter
    };
  for (var filterName in filters) {
    angularShiroServicesModule.factory(filterName, filters[filterName]);
  }
  angularShiroServicesModule.factory('filtersResolver', filtersResolver);
  var directives = {
      'hasRole': hasRoleDirective,
      'notAuthenticated': notAuthenticatedDirective,
      'authenticated': authenticatedDirective,
      'lacksRole': lacksRoleDirective,
      'hasAnyRole': hasAnyRoleDirective,
      'hasPermission': hasPermissionDirective,
      'lacksPermission': lacksPermissionDirective,
      'hasAnyPermission': hasAnyPermissionDirective,
      'principal': principalDirective,
      'usernamePasswordForm': usernamePasswordFormDirective
    };
  var moduleDirectives = angular.module('angularShiro.directives', []);
  for (var key in directives) {
    moduleDirectives.directive(key, directives[key]);
  }
  angular.module('angularShiro', [
    'angularShiro.services',
    'angularShiro.directives',
    'angularShiro.templates'
  ]).run([
    '$rootScope',
    '$location',
    'subject',
    'angularShiroConfig',
    'filtersResolver',
    '$log',
    function ($rootScope, $location, subject, angularShiroConfig, filtersResolver, $log) {
      var doFilter = function (filtersResolver, $location) {
        var filters = filtersResolver.resolve($location.path());
        for (var i = 0, len = filters.length; i < len; i++) {
          if (!filters[i]()) {
            break;
          }
        }
      };
      $rootScope.$on('$locationChangeStart', function (event, next, current) {
        var params = $location.search();
        if (!subject.isAuthenticated() && params.sessionId) {
          try {
            var output = subject.rememberMe(params.sessionId);
            if (output !== false) {
              output.then(function () {
                doFilter(filtersResolver, $location);
              });
            } else {
              $location.search('sessionId', null);
              $location.path(angularShiroConfig.login.path);
            }
          } catch (e) {
            $log.error(e.message);
            $location.search('sessionId', null);
            $location.path(angularShiroConfig.login.path);
          }
        } else {
          doFilter(filtersResolver, $location);
          if (subject.isRemembered() && !params.sessionId) {
            $location.search('sessionId', subject.getSession(false).getId());
          }
        }
      });
    }
  ]);
  /**
 * Return the DOM siblings between the first and last node in the given array.
 * 
 * @param {Array}
 *                array like object
 * @returns jQlite object containing the elements
 */
  function getBlockElements(nodes) {
    var startNode = nodes[0], endNode = nodes[nodes.length - 1];
    if (startNode === endNode) {
      return angular.element(startNode);
    }
    var element = startNode;
    var elements = [element];
    do {
      element = element.nextSibling;
      if (!element)
        break;
      elements.push(element);
    } while (element !== endNode);
    return angular.element(elements);
  }
  var trim = function () {
      if (!String.prototype.trim) {
        return function (value) {
          return angular.isString(value) ? value.replace(/^\s\s*/, '').replace(/\s\s*$/, '') : value;
        };
      }
      return function (value) {
        return angular.isString(value) ? value.trim() : value;
      };
    }();
}(window, document));