imagemagick
-----------
What it is?
-----------
Image magick is a took that can analyze, generate, or extract data from a picture.

How to build
------------
Execute the following command:
- `mvn clean install` it will build the jar and execute the test.
- `mkdir imagemagick-griffon-vitam`.
- `cp griffon-imagemagick imagemagick-griffon-vitam/`.
- `cp target/imagemagick-jar-with-dependencies.jar imagemagick-griffon-vitam/`.
- `fpm -s dir -t deb -d 'imagemagick' -n vitam-imagemagick-griffon ./imagemagick-griffon-vitam=/vitam/griffon`.
- `fpm -s dir -t rpm -d 'ImageMagick' -n vitam-imagemagick-griffon ./imagemagick-griffon-vitam=/vitam/griffon`.

In order to build the RPM package you must have `rpmbuild` tool.

How to run
----------
On a shell run `griffon-imagemagick path/to/batch/directory`, it can also be run with `java -jar target/imagemagick-jar-with-dependencies.jar path/to/batch/directory`. A path to the work directory can be specify or nothing if the tool is executed directly in the right place.

How to install
--------------
On centos run `dnf install vitam-imagemagick-griffon-1.0-1.x86_64.rpm` and on debian `apt-get install vitam-imagemagick-griffon_1.0_amd64.deb`.
