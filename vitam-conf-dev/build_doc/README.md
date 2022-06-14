1. Build a container with all dependencies

   ```
   docker build -t vitam/doc_builder_debian10 .
   ```

2. Start a container with current user

   ```
   docker run --rm -it -v /path/to/vitam/:/code -v ~/.m2/:/home/$USER/.m2 --user $(id -u):$(id -g) -v /etc/passwd:/etc/passwd -v /etc/shadow:/etc/shadow vitam/doc_builder_debian10:latest /bin/bash
   ```

3. Compile core vitam (required for javadocs)

   ```bash
   cd /code
   mvn package javadoc:aggregate-jar install -f sources/pom.xml -P-vitam -DskipTests=true
   ```

   Alternatively, if you need RPM & DEB packages also :

   ```bash
   cd /code
   mvn package javadoc:aggregate-jar install rpm:attached-rpm jdeb:jdeb -f sources/pom.xml -P-vitam -DskipTests=true
   ```

4. Compile documentation

   ```bash
   cd /code
   mvn package install -f doc/pom.xml -P-vitam -DskipTests=true
   ```

   Alternatively, if you need RPM & DEB packages also :

   ```bash
   cd /code
   mvn package install rpm:attached-rpm jdeb:jdeb -f doc/pom.xml -P-vitam -DskipTests=true
   ```
