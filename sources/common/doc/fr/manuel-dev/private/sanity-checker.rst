Présentation
************

La classe SanityChecker est une classe utilisée pour nettoyer les fichiers à importer dans la solution logicielle Vitam (XML, JSON, ...),
en supprimant les balises HTML afin de renforcer la sécurité du système.


Utilisation
###########

1. Rejet d'un référentiel CSV contenant une injection

.. code-block:: java

    public static final void checkHTMLFile(File file) throws InvalidParseOperationException, IOException {
        try (final Reader fileReader = new FileReader(file)) {
            try (final BufferedReader bufReader = new BufferedReader(fileReader)) {
                String line = null;
                while ((line = bufReader.readLine()) != null) {
                    checkParameter(line.split(","));
                }
            }
        }
    }

2. Rejet d'un référentiel Json contenant une injection

.. code-block:: java

    if (json.isArray()) {
            ArrayNode nodes = (ArrayNode) json;
            for (JsonNode element : nodes) {
                checkJsonSanity(element);
            }
        } else {
            final Iterator<Map.Entry<String, JsonNode>> fields = json.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> entry = fields.next();
                final String key = entry.getKey();
                checkSanityTags(key, getLimitFieldSize());
                final JsonNode value = entry.getValue();

                if (value.isArray()) {
                    ArrayNode nodes = (ArrayNode) value;
                    for (JsonNode jsonNode : nodes) {
                        if (!jsonNode.isValueNode()) {
                            checkJsonSanity(jsonNode);
                        } else {
                            validateJSONField(value);
                        }
                    }
                } else if (!value.isValueNode()) {
                    checkJsonSanity(value);
                } else {
                    validateJSONField(value);
                }
            }
        }