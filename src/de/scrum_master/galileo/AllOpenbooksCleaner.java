package de.scrum_master.galileo;

import java.util.Arrays;

public class AllOpenbooksCleaner
{
	private static final String basePath = "c:/Dokumente und Einstellungen/Robin/Eigene Dateien/Bücher/Galileo Computing/";
	private static final String[] books = {
		"actionscript",                 // OK
		"actionscript_einstieg",        // OK
		"apps_entwickeln_iphone_ipad",  // OK
		"c_von_a_bis_z",                // OK
		"dreamweaver8",                 // OK
		"einstieg_vb_2008",             // TOC links *.4 -> *.3 for appendices
		"excel_2007",                   // OK
		"hdr_fotografie",               // OK
		"it_handbuch",                  // OK
		"java7",                        // OK
		"javainsel",                    // OK
		"javascript_ajax",              // Fix small images
		"joomla15",                     // OK
		"linux",                        // OK
		"linux_unix_programmierung",    // OK
		"microsoft_netzwerk",           // Fix small images
		"oop",                          // OK
		"photoshop_cs2",                // OK
		"photoshop_cs4",                // TOC links *.4 -> *.3 (special naming scheme)
		"php_pear",                     // OK
		"python",                       // OK
		"ruby_on_rails",                // OK
		"shell_programmierung",         // OK
		"ubuntu",                       // OK
		"ubuntu_1004",                  // OK
		"unix_guru",                    // Fix font sizes (too small) => stylesheets
		"visual_csharp",                // OK
		"visual_csharp_2010",           // OK
		"vmware",                       // OK
		"windows_server_2008"           // OK
	};

	public static void main(String[] args) throws Exception
	{
		long startTime = System.currentTimeMillis();
		// Forward command line options to OpenbookCleaner.main, always adding current book's name at the end
		int argv = args.length;
		String[] newArgs = Arrays.copyOf(args, argv + 1);
		for (String book : books) {
			newArgs[argv] = basePath + book;
			OpenbookCleaner.main(newArgs);
			SimpleLogger.echo("");
		}
		SimpleLogger.time("Total duration", System.currentTimeMillis() - startTime);
	}
}
