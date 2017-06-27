# cljotdr

<code>cljotdr</code> Simple OTDR SOR file parse written in Clojure

The SOR ("Standard OTDR Record") data format is used to store OTDR
([optical time-domain
reflectometer](http://https://en.wikipedia.org/wiki/Optical_time-domain_reflectometer)
) fiber data.  The format is defined by the Telcordia [SR-4731, issue
2](http://telecom-info.telcordia.com/site-cgi/ido/docs.cgi?ID=SEARCH&DOCUMENT=SR-4731&)
standard.  While it is a standard, it is unfortunately not open, in
that the specifics of the data format are not openly available.  You
can buy the standards document from Telcordia for $750 US (as of this
writing), but this was beyond my budget. (And likely comes with
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
written in Perl (there is also a Python version, [pyOTDR](https://github.com/sid5432/pyOTDR)

(Why Clojure?  Well, I needed a project to practice/learn the language, and
this seems as good as any.  Since Clojure is a hosted language, under the Java Virtual Machine (JVM),
you should be able to use the code from Java also)


## Installation

Add <code>[optical.fiber/cljotdr "0.1.0"]</code> to your <code>project.clj</code> file.

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
<pre>
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

### Bugs
    
The parsing is incomplete; please see <A HREF="https://morethanfootnotes.blogspot.com/2015/07/the-otdr-optical-time-domain.html">my blog post</A> for details.

## License

Copyright © 2017 Sidney Li <sidney.hy.li@gmail.com>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
