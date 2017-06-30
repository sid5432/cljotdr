(ns cljotdr.alter
  (:require [cljotdr.utils :refer :all]
            [cljotdr.parse]
            [cljotdr.crc :refer [initial-cksum]]
            [cheshire.core :as json]
            [cljotdr.mapblock]
            [cljotdr.genparams]
            [cljotdr.supparams]
            [cljotdr.fxdparams]
            [cljotdr.cksum]
            )
  (:gen-class
   :name cljotdr.alter
   :methods [^:static [change_sor [String String String] void] ]
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
  [block old-map new-map input output]

  (let [
        bname (get block "name")
        pos   (get block "pos")
        fmtno (get old-map "format")
        ]
    ;; (println "* process" bname " pos " pos)
    (cond
      (= bname "GenParams") (cljotdr.genparams/alter-block bname fmtno old-map new-map input output)
      (= bname "SupParams") (cljotdr.supparams/alter-block bname fmtno old-map new-map input output)
      (= bname "FxdParams") (cljotdr.fxdparams/alter-block bname fmtno old-map new-map input output)
      (= bname "KeyEvents") (cljotdr.bypass/copy-block bname old-map input output)
      (= bname "DataPts")   (cljotdr.bypass/copy-block bname old-map input output)
      (= bname "Cksum")     (cljotdr.cksum/alter-block bname fmtno old-map new-map input output)
      :else
      (cljotdr.bypass/copy-block bname old-map input output)
      ); cond
    ); let
  )

(defn- process
  "loop through all the blocks"
  [old-map new-map input output]
  ;; (println "OLD MAP")
  ;; (pprint old-map)
  ;; (println "NEW MAP\n")
  ;; (pprint new-map)
  ;; (println "END MAPS\n")
  
  ;; copy mapblock first (will adjust later)
  (println "* Copying Map block")
  (cljotdr.mapblock/copy-map-block old-map input output)
     
  (loop [blocks (block-seq old-map)]
    (if (empty? blocks)
      (do
        nil ; exit loop
        ); do
      (do
        (process-block (first blocks) old-map new-map input output)
        (recur (rest blocks))
        );
      ) ; if
    ); loop
  
  ;; finish
  (.close (input :fh))
  (.close (output :fh))
  (println "All done; bye!")
  )


; ===================================================================
(defn- check-parse
  [results-map source]
  (if (nil? results-map)
    (do
      (println "Error parsing " source)
      false
      )
    true
    )
  )

(defn- check-writable
  [fh filename]
  (if (nil? fh)
    (do
      (println "Error writing to " filename )
      false
      )
    true
    )
  )

  
(defn change-sor
  "read the SOR file and a JSON file, and replace the SOR with
  the data from the JSON file (keeping the same the DataPts block)."
  [sor-file json-file new-sor-file]
  (let [old-map  (cljotdr.parse/sorparse sor-file nil false)
        new-map  (get-new-map json-file)
        ; don't really care about cksum or use it, but need it to reuse existing routines
        input    {:fh (java.io.RandomAccessFile. sor-file "r") :cksum (ref initial-cksum)}
        output   {:fh (java.io.RandomAccessFile. new-sor-file "rw")  :cksum (ref initial-cksum)}
        ]
    (if (and (check-parse old-map sor-file) (check-parse new-map json-file) (check-writable output new-sor-file))
      (process old-map new-map input output)
      (println "Error; exiting program")
      )
    )
  )

(defn -change_sor
  "Java wrapper for gensor"
  [sor-file json-file new-sor-file]
  (change-sor sor-file json-file new-sor-file)
  )
