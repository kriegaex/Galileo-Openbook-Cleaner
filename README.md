HTML cleaner for Rheinwerk (ex-Galileo) openbooks
=================================================

This is a tool for cleaning up [Rheinwerk openbooks](https://www.rheinwerk-verlag.de/openbook/)
(formerly known as Galileo openbooks) before converting them to EPUB or PDF format.

__Current state of development:__ v1.2.0-SNAPSHOT is feature complete, i.e. it can download, MD5-verify, unpack
and convert all 37 openbooks available at release time.

__History:__ If you want to know details about what has changed in which version, please take a look at the
[change log](https://raw.githubusercontent.com/kriegaex/Galileo-Openbook-Cleaner/master/CHANGELOG).

__Download:__ A precompiled, executable JAR file is available
[here](http://scrum-master.de/download/GalileoOpenbookCleaner/openbook_cleaner-1.2.0-SNAPSHOT.jar).

__Usage:__

    $ java -jar openbook_cleaner-1.2.0-SNAPSHOT.jar --help

    OpenbookCleaner usage: java ... [options] <book_id>*

    Option                       Description
    ------                       -----------
    -?, --help                   Display this help text
    -c, --check-avail            Check Galileo homepage for available books,
                                   compare with known ones
    -d, --download-dir <File>    Download directory for openbooks; must exist
                                   (default: .)
    -l, --log-level <Integer>    Log level (0=normal, 1=verbose, 2=debug, 3=trace)
                                   (default: 0)
    -m, --check-md5              Download all known books without storing them,
                                   verifying their MD5 checksums (slow! >1 Gb
                                   download)
    -t, --threading <Integer>    Threading mode (0=single, 1=multi); single is
                                   slower, but better for diagnostics) (default: 1)
    -w, --write-config           Write editable book list to config.xml, enabling
                                   you to update MD5 checksums or add new books

    book_id1 book_id2 ...        Books to be downloaded & converted

    Legal book IDs:
      all (magic value: all books), actionscript_1_und_2, actionscript_einstieg,
      apps_iphone_ios5, apps_iphone_ios6, asp_net, c_von_a_bis_z, dreamweaver_8,
      excel_2007, hdr_fotografie, it_handbuch, javascript_ajax, java_7, java_insel,
      joomla_1_5, linux, linux_unix_prog, microsoft_netzwerk, oop, photoshop_cs2,
      photoshop_cs4, php_pear, ruby_on_rails_2, shell_prog, ubuntu_10_04,
      ubuntu_11_04, ubuntu_12_04, unix_guru, vb_2008, vb_2008_einstieg,
      vb_2010_einstieg, vb_2012_einstieg, vcsharp_2008, vcsharp_2010, vcsharp_2012,
      vmware, windows_server_2008, windows_server_2012

__Dependencies:__ Openbook cleaner was developed in Java 7. It also uses a few open source libraries:

  * __jsoup 1.8.3__ for parsing the "dirty" openbook HTML, selecting DOM elements and editing them, removing
    navigation elements, ads and other types of clutter, and finally write a clean, pretty-printed HTML
    document back to disk
  * __JOpt Simple 4.9__ for parsing command-line parameters and showing a help page (usage info)
  * __Apache Commons Compress 1.10__ for unzipping downloaded openbook archives. *Note: When Java 7 is
    available on MacOS, this library might be removed again and we can revert to using the built-in Java
    classes.*
  * __XStream 1.4.8__ parsing the *config.xml* file containing openbook meta data
  * __AspectJ 1.8.13__ for cross-cutting concerns like logging, timing, tracing which are not part of the
    main application logic. This helps to keep the core code clean and free from scattered code addressing
    secondary concerns.

__Development environment:__

  * __IDE:__ I originally started developing this project with _Eclipse_ but have switched to _IntelliJ IDEA_
    which for me personally is preferable because of its superior Maven support. OTOH, _Eclipse_ has better
    _AspectJ_ integration. So if you want to change any of the aspect code, you might want to use _Eclipse_
    anyway.
  * __Git__ support is needed in your IDE of choice (or at least from the command line) if you want to
    interact with the source code repository and not just download a ZIP archive from _GitHub_.
  * __Maven__ is used for dependency management and the whole build and packaging cycle. Any Maven 3 version
    should be safe, I recommend using the latest stable version. It is totally up to you if you want to build
    from the command line or via IDE integration. In _IntelliJ IDEA_ you should install the original Maven
    plugins, for _Eclipse_ you need _m2e_ and also the _AspectJ Maven Configurator_ (can be installed from
    http://dist.springsource.org/release/AJDT/configurator/).
  * __AspectJ__ support is available for both _Eclipse_ (AJDT, AspectJ Development Tools) and _IntelliJ IDEA_.
    I do not know about _Netbeans_ or other IDEs though. So please make sure to install the corresponding IDE
    plugins for AspectJ support if you want to edit the aspect code comfortably. But this is optional, because
    Maven can still build the project, fetching all necessary dependencies including AspectJ.

Because later I might want to use this *Git* repository as a refactoring showcase for my developer workshops,
I am going to do any refactoring step by step, documenting progress in small, fine-granular *Git* changesets,
so later on I can review the evolutionary progress with others.

As you can see, I am mostly doing this little project for myself, but I like to share the results and
receive some user feedback. I hope the openbook cleaner is useful to you. Enjoy! :-)

Alexander Kriegisch
