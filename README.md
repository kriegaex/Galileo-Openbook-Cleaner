HTML cleaner for Galileo openbooks
==================================

This is a tool for cleaning up [Galileo Computing openbooks](http://www.galileocomputing.de/openbook)
before converting them to EPUB or PDF format.

__Current state of development:__ v1.0 is feature complete, i.e. it can download, MD5-verify, unpack
and convert all 32 openbooks available at release time.

__Attention:__ If you are still using the previous release v0.9.1.1, please read the
[description for the old version](https://github.com/kriegaex/Galileo-Openbook-Cleaner/tree/v0.9.1.1),
not this one.

__Usage:__

    $ java -jar galileo_openbook_cleaner-1.0.jar --help

    OpenbookCleaner usage: java ... [options] <book_id>*

    Option                                  Description
    ------                                  -----------
    -?, --help                              Display this help text
    -d, --download-dir <File>               Download directory for openbooks; must
                                              exist (default: .)
    -l, --log-level <Integer>               Log level (0=normal, 1=verbose,
                                              2=debug, 3=trace) (default: 0)
    -t, --threading <Integer>               Threading mode (0=single, 1=multi);
                                              single is slower, but better for
                                              diagnostics) (default: 1)
    book_id1 book_id2 ...                   Books to be downloaded & converted

    Legal book IDs:
      all (magic value: all books), actionscript_1_und_2, actionscript_einstieg,
      apps_iphone, c_von_a_bis_z, dreamweaver_8, excel_2007, hdr_fotografie,
      it_handbuch, java_7, java_insel, javascript_ajax, joomla_1_5, linux,
      linux_unix_prog, microsoft_netzwerk, oop, photoshop_cs2, photoshop_cs4,
      php_pear, python, ruby_on_rails_2, shell_prog, ubuntu_10_04, ubuntu_11_04,
      unix_guru, vb_2008, vb_2008_einstieg, vb_2010_einstieg, vcsharp_2008,
      vcsharp_2010, vmware, windows_server_2008

__Dependencies:__ Openbook cleaner was developed in Java 7 with compiler compliance level 1.6 (Java 6). So you
should be fine with a JRE or JDK for Java 6. It also uses a few open source libraries (see
[readme.txt](https://github.com/kriegaex/Galileo-Openbook-Cleaner/tree/master/galileo_openbook_cleaner/lib)
for download links and installation instructions):

  * __jsoup 1.6.3__ for parsing the "dirty" openbook HTML, selecting DOM elements and editing them, removing
    navigation elements, ads and other types of clutter, and finally write a clean, pretty-printed HTML
    document back to disk
  * __JOpt Simple 4.3__ for parsing command-line parameters and showing a help page (usage info)
  * __Apache Commons Compress 1.4.1__ for unzipping downloaded openbook archives. *Note: When Java 7 is
    available on MacOS, this library might be removed again and we can revert to using the built-in Java
    classes.*
  * __AspectJ 1.7.0__ for cross-cutting concerns like logging, timing, tracing which are not part of the
    main application logic. This helps to keep the core code clean and free from scattered code addressing
    secondary concerns. The AspectJ runtime is part of the pre-packaged JAR available for [download]
    (https://github.com/kriegaex/Galileo-Openbook-Cleaner/downloads), so you only need to install AspectJ if
    you want to build the application by yourself.

Because later I might want to use this *Git* repository as a refactoring showcase for my developer workshops,
I am going to do any refactoring step by step, documenting progress in small, fine-granular *Git* changesets,
so later on I can review the evolutionary progress with others.

As you can see, I am mostly doing this little project for myself, but I like to share the results and
receive some user feedback. I hope the openbook cleaner is useful to you. Enjoy! :-)

Alexander Kriegisch
