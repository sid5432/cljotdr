(ns cljotdr.core
  (:require [me.raynes.fs]
            [cljotdr.cmdline :refer [filename_etc]]
            [cljotdr.parse :refer :all]
            [cljotdr.dump :refer [save-file]]
            [cljotdr.mapblock]
            [cljotdr.alter]
            )
  (:gen-class))

(use '[clojure.pprint :only (pp pprint)])

(defn- opformat
  [optype]
  (cond
    (= optype 1) "JSON"
    (= optype 2) "SMILE"
    :else
    "This shouldn't happen!"
    ;; exit?
    )  
  )

(defn- gen-tracefile
  "generate a OTDR trace file name (for the data points)"
  [fname dump?]
  (if (or (not dump?) (nil? fname))
    nil
    (str (me.raynes.fs/name fname) "-dump.dat")
    )
  )

(defn -main
  "OTDR parser"
  [& args]
  (let [ [fname optype dump? xdebug?] (apply filename_etc args)
        debug? (= xdebug? "yes")
        tracefile (gen-tracefile fname dump?)
        ]
    (if (not (nil? fname))
      (do
        (println "")
        (println "process file" fname ", output format " (opformat optype)
                 ", dump data to file? " dump?
                 )
        (-> fname
            (cljotdr.parse/sorparse tracefile debug?)
            (save-file (cljotdr.dump/get-opname fname optype) optype)
            )
        (println "* All done; bye!")
        ) ; do
      ) ; if
    ) ; let
  )


