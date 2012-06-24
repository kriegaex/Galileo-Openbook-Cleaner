package de.scrum_master.galileo;

public enum BookInfo
{
	ACTIONSCRIPT_1_UND_2  ("design_actionscript",                            "9783898422215"),
	ACTIONSCRIPT_EINSTIEG ("design_actionscript_einstieg",                   "9783898427746"),
	APPS_IPHONE           ("computing_apps_entwickeln_fuer_iphone_und_ipad", "9783836214636"),
	C_VON_A_BIS_Z         ("computing_c_von_a_bis_z",                        "9783836214117"),
	DREAMWEAVER_8         ("design_dreamweaver8",                            "9783898427395"),
	EXCEL_2007            ("computing_excel_2007",                           "9783898428644"),
	HDR_FOTOGRAFIE        ("design_hdr_fotografie",                          "9783836211031"),
	IT_HANDBUCH           ("computing_it_handbuch",                          "9783836217446"),
	JAVA_7                ("computing_java7",                                "9783836215077"),
	JAVA_INSEL            ("computing_javainsel",                            "9783836218023"),
	JAVASCRIPT_AJAX       ("computing_javascript_ajax",                      "9783898428590"),
	JOOMLA_1_5            ("computing_joomla15",                             "9783898428811"),
	LINUX                 ("computing_linux",                                "9783836218221"),
	LINUX_UNIX_PROG       ("computing_linux_unix_programmierung",            "9783836213660"),
	MICROSOFT_NETZWERK    ("computing_microsoft_netzwerk",                   "9783898428477"),
	OOP                   ("computing_oop",                                  "9783836214018"),
	PHOTOSHOP_CS2         ("design_photoshop_cs2",                           "9783898427005"),
	PHOTOSHOP_CS4         ("design_photoshop_cs4",                           "9783836212373"),
	PHP_PEAR              ("computing_php_pear",                             "9783898425803"),
	PYTHON                ("computing_python",                               "9783836211109"),
	RUBY_ON_RAILS_2       ("computing_ruby_on_rails",                        "9783898427791"),
	SHELL_PROG            ("computing_shell_programmierung",                 "9783898426831"),
	UBUNTU_10_04          ("computing_ubuntu_1004",                          "9783836216548"),
	UBUNTU_11_04          ("computing_ubuntu",                               "9783836217651"),
	UNIX_GURU             ("computing_unix_guru",                            "9783898422406"),
	VB_2008               ("computing_visualbasic_2008",                     "9783836211710"),
	VB_2008_EINSTIEG      ("computing_einstieg_vb_2008",                     "9783836211925"),
	VB_2010_EINSTIEG      ("computing_einstieg_vb_2010",                     "9783836215411"),
	VCSHARP_2008          ("computing_visual_csharp",                        "9783836211727"),
	VCSHARP_2010          ("computing_visual_csharp_2010",                   "9783836215527"),
	VMWARE                ("computing_vmware",                               "9783898427012"),
	WINDOWS_SERVER_2008   ("computing_windows_server_2008",                  "9783836215282"),
	;

	final String unpackDirectory;
	final String downloadArchive;
	final String coverImage;

	private final static String BOOK_URL       = "http://download2.galileo-press.de/openbook/"; 
	private final static String COVER_URL      = "http://cover.galileo-press.de/";
	private final static String ARCHIVE_PREFIX = "galileo"; 

	private BookInfo(String downloadArchive, String coverImage)
	{
		this.unpackDirectory = name().toLowerCase();
		this.downloadArchive = BOOK_URL + ARCHIVE_PREFIX + downloadArchive + ".zip";
		this.coverImage = COVER_URL + coverImage + ".jpg";
	}

	public static void main(String[] args)
	{
		// Iterate over enum and access members
		for (BookInfo bookInfo : BookInfo.values()) {
			System.out.println(bookInfo.name());  // or just 'bookInfo'
			System.out.println("  " + bookInfo.unpackDirectory);
			System.out.println("  " + bookInfo.downloadArchive);
			System.out.println("  " + bookInfo.coverImage);
		}
		System.out.println();

		// Convert string into enum identifier
		System.out.println(BookInfo.valueOf("LINUX"));

		// Handle illegal identifier + get its name from exception message 
		try {
			System.out.println(BookInfo.valueOf("FOOBAR_BOOK"));
		}
		catch (IllegalArgumentException e) {
			System.err.println(
				"Error: book info element " + 
				e.getMessage().replaceFirst(".*[.]", "") +
				" not found"
			);
		}
	}
}
