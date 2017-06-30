(ns cljotdr.gensor
  (:require [cljotdr.utils :refer :all]
            [cljotdr.parse]
            [cheshire.core :as json]
            )
  (:gen-class
   :name cljotdr.gensor
   :methods [^:static [gensor [String String String] void] ]
   ))

(use '[clojure.pprint :only (pp pprint)])

(defn- get-new-map
  [json-file]
  (with-open [in (clojure.java.io/input-stream json-file)]
    (json/parse-string (slurp in))
    )
  )

(defn- block-seq
  "generate sequence of blocks in the correct order"
  [results]
  (sort-by #(get % "order") (-> results (get "blocks") vals))
  )

(defn- process-block
  "process one block"
  [block results]
  (let [
        bname (get block "name")
        pos   (get block "pos")
        ]
    (println "process" bname " pos " pos)
    )
  )

(defn- process
  "loop through all the blocks"
  [sor-file results new-map output]
  (println "SOR file " sor-file)
  (pprint results)
  (println "END RESULTS\n")
  (pprint new-map)
  (println "END JSON MAP\n")
  
  (let [raf (java.io.RandomAccessFile. sor-file "r")
        ]
    (loop [blocks (block-seq results)]
      (if (empty? blocks)
        (do
          nil ; exit loop
          ); do
        (do
          (process-block (first blocks) results)
          (recur (rest blocks))
          );
        ) ; if
      ); loop
    ) ; let
  )


; ===================================================================
(defn- check-parse
  [results source]
  (if (nil? source)
    (do
      (println "Error parsing " source)
      false
      )
    true
    )
  )

(defn- check-writable
  [fh opfile]
  (if (nil? fh)
    (do
      (println "Error writing to " opfile )
      false
      )
    true
    )
  )

  
(defn gensor
  "read the SOR file and a JSON file, and replace the SOR with
  the data from the JSON file (keeping the same the DataPts block)."
  [sor-file json-file new-sor-file]
  (let [results  (cljotdr.parse/sorparse sor-file nil false)
        new-map  (get-new-map json-file)
        output   (java.io.RandomAccessFile. new-sor-file "rw")
        ]
    (if (and (check-parse results sor-file) (check-parse new-map json-file) (check-writable output new-sor-file))
      (process sor-file results new-map output)
      (println "Exiting program")
      )
    )
  )

(defn -gensor
  "Java wrapper for gensor"
  [sor-file json-file new-sor-file]
  (gensor sor-file json-file new-sor-file)
  )
