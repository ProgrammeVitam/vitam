esapi utilisation
#################


.. code-block:: xml
 
  <?xml version="1.0" encoding="UTF-8"?>
  <policy>
	<settings>
		<mode>redirect</mode>
		<error-handling>
			<default-redirect-page>/security/error.jsp</default-redirect-page>
			<block-status>403</block-status>
		</error-handling>
	</settings>
	<outbound-rules>
		<add-header name="FOO" value="BAR" path="/.*">
			<path-exception type="regex">/marketing/.*</path-exception>
		</add-header>
	</outbound-rules>
  </policy>


