# cljotdr

<code>cljotdr</code> - parser for SOR ("Standard OTDR Record") data files, used to store
OTDR (optical time-domain reflectometer) data.

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
