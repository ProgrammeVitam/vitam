imagemagick
-----------
What it is?
-----------
Image magick is a took that can analyze, generate, or extract data from a picture.

How to build
------------
Execute the following command:
- `mvn clean install` it will build the jar and execute the test.

In order to build the RPM package you must have `rpmbuild` tool.

How to run
----------
On a shell run `griffon-imagemagick path/to/batch/directory`, it can also be run with `java -jar target/imagemagick-jar-with-dependencies.jar path/to/batch/directory`. A path to the work directory can be specify or nothing if the tool is executed directly in the right place.

How to install
--------------
On centos run `dnf install target/imagemagick-griffon-vitam-1.11.0-SNAPSHOT.rpm` and on debian `dpkg -i target/imagemagick-griffon-vitam-1.11.0-SNAPSHOT.deb`.
