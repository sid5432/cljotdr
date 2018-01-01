# cljotdr: a simple OTDR SOR file parse written in Clojure

[![Clojars Project](https://img.shields.io/clojars/v/optical.fiber/cljotdr.svg)](https://clojars.org/optical.fiber/cljotdr)

The SOR ("Standard OTDR Record") data format is used to store OTDR
([optical time-domain
reflectometer](http://https://en.wikipedia.org/wiki/Optical_time-domain_reflectometer)
) fiber data.  The format is defined by the Telcordia [SR-4731, issue
2](http://telecom-info.telcordia.com/site-cgi/ido/docs.cgi?ID=SEARCH&DOCUMENT=SR-4731&)
standard.  While it is a standard, it is unfortunately not open, in
that the specifics of the data format are not openly available.  You
can buy the standards document from Telcordia for $750 US (last I checked), 
but this was beyond my budget. (And likely comes with
all sorts of licensing restrictions. I wouldn't know; I have never
seen the document!)


There are several freely available OTDR trace readers available for
download on the web, but most do not allow exporting the trace curve
into, say, a CSV file for further analysis, and only one that I've
found that runs natively on Linux (but without source code; although
some of these do work in the Wine emulator).  There have been requests
on various Internet forums asking for information on how to extract
the trace data, but I am not aware of anyone providing any answers
beyond pointing to the free readers and the Telcordia standard.


Fortunately the data format is not particularly hard to decipher.  The
table of contents on the Telcordia [SR-4731, issue
2](http://telecom-info.telcordia.com/site-cgi/ido/docs.cgi?ID=SEARCH&DOCUMENT=SR-4731&)
page provides several clues, as does the Wikipedia page on [optical
time-domain
reflectometer](http://https://en.wikipedia.org/wiki/Optical_time-domain_reflectometer).


Using a binary-file editor/viewer and comparing the outputs from some
free OTDR SOR file readers, I was able to piece together most of the
encoding in the SOR data format and written yet another simple program
(in [Clojure](https://clojure.org)) that parses the SOR file and dumps the trace data into a
file.  (For a more detailed description, other than reading the source
code, see [my blog
post](http://morethanfootnotes.blogspot.com/2015/07/the-otdr-optical-time-domain.html?view=sidebar)).

Presented here for your entertainment are my findings, in the hope
that it will be useful to other people.  But be aware that the
information provided here is based on guess work from looking at a
limited number of sample files.  I can not guarantee that there are no
mistakes, or that I have uncovered all possible exceptions to the
rules that I have deduced from the sample files.  **use it at your own
risk! You have been warned!**

The program was ported over from my original [pubOTDR](https://github.com/sid5432/pubOTDR)
written in Perl (there is also a Python version, [pyOTDR](https://github.com/sid5432/pyOTDR)).

(<i>Why Clojure?  Well, I needed a project to practice/learn the language, and
this seems as good as any.  Since Clojure is a hosted language, under the Java Virtual Machine (JVM),
you should be able to use the code from Java also.</i>)


## Installation

Add <code>[optical.fiber/cljotdr "0.1.2"]</code> to your <code>project.clj</code> file.

Uses several other modules:

* [org.clojure/tools.cli "0.3.5"]
* [biscuit "1.0.0"] (for CRC-16)
* [me.raynes/fs "1.4.6"] (file system utilities)
* [bytebuffer "0.2.0"] (for handling binary data)
* [nio "1.0.4"] (for handling binary data)
* [cheshire "5.7.1"] (for JSON and SMILE formats)
* [clj-time "0.13.0"] (date/time utilities)
* [digest "1.4.5"] (various digests; only for testing)

## Usage

In the top level directory, the command "lein run" to see the options.  To parse a file, run
the command like this:

<pre>
% lein run --file myfile.sor --dump yes
</pre>

This should produce two files: <code>myfile.json</code> and <code>myfile-dump.dat</code>.
The first file is a JSON dump of the parse results.  The second file is the OTDR trace
(tab delimited x and y values).

To run the program interactively, start up "lein repl" and run

<pre>
   % lein repl
   nREPL server started on port 32927 on host 127.0.0.1 - nrepl://127.0.0.1:32927
   REPL-y 0.3.7, nREPL 0.2.12
   
   ...(<i>omitted</i>)....
   
   user=> (use '[cljotdr.core])
   nil
   user=> (def file-name "mydata.sor")
   #'user/file-name   (<i>your SOR file</i>)
   user=> (def trace-file "otdr-trace.dat")
   #'user/trace-file  (<i>where the OTDR trace should be written to; use nil to avoid writing to file</i>)
   user=> (def debug? false)
   #'user/debug?      (<i>whether to show debugging information on screen</i>)
   user=> (def results (cljotdr.parse/sorparse file-name trace-file debug?))
   #'user/results     (<i>hash-map of parsing results</i>)
   user=> (def output-type 1)
   #'user/output-type (<i>1 for JSON, 2 for SMILE</i>)
   user=> (cljotdr.dump/save-file results "output.json" output-type) 
</pre>

To run the unit tests, run this at the top level directory:

<pre>
   % lein test
</pre>

## Using in Java

Here is a sample Java program to call the two main functions, <code>sorparse</code> and
<code>save_file</code>:

<pre>
import cljotdr.parse;
import cljotdr.dump;
import clojure.lang.PersistentHashMap;

public class testOTDR {
	public static void main(String[] args) {
		clojure.lang.PersistentHashMap results;
		
		Boolean verbose = true; // display results on screen
		results = cljotdr.parse.sorparse("demo_ab.sor", "trace.dat", verbose);
		// save result in JSON format
		cljotdr.dump.save_file(results,"testout.json", 1);
		// save result in SMILE format
		cljotdr.dump.save_file(results,"testout.sml", 2);
		
		double version = (double) results.valAt("version");
                System.out.format("\n* Version = %.1f\n", version);
		
		clojure.lang.PersistentHashMap GenParams = (clojure.lang.PersistentHashMap) results.valAt("GenParams");

                System.out.format("\n* GenParams listing : %s\n", GenParams);
                System.out.format("\n* wavelength = %s\n", GenParams.valAt("wavelength"));


                // Changing an SOR file: generate the JSON file from parsing the original SOR file
                // then edit the JSON to make changes and run the change_sor() function.
                // Only a few fields (such as fiber ID) can be changed; most are ignored.
                String original = "demo_ab.sor";
                String jsonfile = "demo_ab-replacement.json";
                String newsor   = "testout.sor";
                cljotdr.alter.change_sor(original, jsonfile, newsor);

		System.out.println("Bye!");
	}
}
</pre>

You will need to have (generate) the jar file from the <code>cljotdr</code> source code first (note that the jar file from clojars does not include the class files).  Grab the source code for <code>cljotdr</code>, and run [Leiningen](https://leiningen.org) in the top level folder:

<pre>
% lein uberjar
</pre>

This should generate two jar files in the folder <code>target/uberjar/</code>: <code>uberjar/cljotdr-0.1.2.jar</code> and 
<code>cljotdr-0.1.2-standalone.jar</code>.  To generating the class file, you will need to set the classpath to include the necessary jar files.  If you are using <code>javac</code> compile the class file as follows (adjust the path to the jar file according to where you place it in your file system):

<pre>
% javac -cp cljotdr-0.1.2-standalone.jar:. testOTDR.java
</pre>

(where <code>testOTDR.java</code> contains the Java code listed above).  This will generate the file <code>testOTDR.class</code>.  Now you can run the code:

<pre>
% java -cp cljotdr-0.1.2-standalone.jar:. testOTDR
</pre>


### Bugs
    
The parsing is "complete", to the extent that all parameters have been identified (thanks
to the help of several readers of my blog).  However the current program does not handle more than
one trace (even though the standard allows this).  If there is more than one trace, the program
will simply abort!


## License

Copyright © 2017 Sidney Li <sidney.hy.li@gmail.com>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

<i>(Last Revised 2017-12-31)</i>
