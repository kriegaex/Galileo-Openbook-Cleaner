You need to add the following JAR files here in order to make the Eclipse build work:

  JTidy r938
    http://sourceforge.net/projects/jtidy/files/JTidy/r938/jtidy-r938.jar/download

  XOM 1.2.8
    http://www.cafeconleche.org/XOM/xom-1.2.8.jar

  TagSoup 1.2.1
    http://ccil.org/~cowan/XML/tagsoup/tagsoup-1.2.1.jar

  JOpt Simple 4.3
    http://central.maven.org/maven2/net/sf/jopt-simple/jopt-simple/4.3/jopt-simple-4.3.jar

  Apache Commons Compress 1.4.1
    http://mirror3.layerjet.com/apache//commons/compress/binaries/commons-compress-1.4.1-bin.zip
    Unpack and use commons-compress-1.4.1.jar

  AspectJ 1.7.0
    http://www.eclipse.org/downloads/download.php?file=/tools/aspectj/aspectj-1.7.0.jar
    Install AspectJ by double-clicking the JAR installer. After installation make sure that
    <aspectj_path>/lib/aspectjrt.jar ist in your classpath.

    If you really want to build the AspectJ + Java project in Eclipse, you should also install
    the AJDT (AspectJ Development Tools) available at http://www.eclipse.org/ajdt/downloads/
    for your Eclipse version of choice. I am still using Indigo (3.7, released in June 2011),
    but Juno (3.8 and 4.2) is already available and should probably be your IDE version of
    choice if you do a fresh install. 4.2 is the new reference platform, so maybe you want
    to choose 4.2.
