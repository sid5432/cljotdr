(ns cljotdr.parse
  (:require [cljotdr.reader :refer [openfile]]
            [cljotdr.crc :refer [update-cksum initial-cksum]]
            [cljotdr.utils :refer :all]
            [cljotdr.crc]
            [cljotdr.mapblock]
            [cljotdr.bypass]
            [cljotdr.genparams]
            [cljotdr.supparams]
            [cljotdr.fxdparams]
            [cljotdr.keyevents]
            [cljotdr.datapts]
            [cljotdr.cksum]
            )
  (:gen-class
   :name cljotdr.parse
   :methods [^:static [sorparse [String String Boolean] clojure.lang.PersistentHashMap] ]
   ))

(use '[clojure.pprint :only (pp pprint)])

(defn- block-seq
  "generate sequence of blocks in the correct order"
  [results]
  (sort-by #(get % "order") (-> results (get "blocks") vals))
  )

(defn- process-block
  [raf item results]
  ;; dummy
  (let [
        fmtno (get results "format")
        bname (get item "name")
        bsize (get item "size")
        pos (get item "pos")
        ]
    (assoc-in results ["blocks" bname "processed"] true)
    (.seek (raf :fh) pos)
    (cond
      (= bname "GenParams") (cljotdr.genparams/process raf fmtno bname pos bsize results)
      (= bname "SupParams") (cljotdr.supparams/process raf fmtno bname pos bsize results)
      (= bname "FxdParams") (cljotdr.fxdparams/process raf fmtno bname pos bsize results)
      (= bname "KeyEvents") (cljotdr.keyevents/process raf fmtno bname pos bsize results)
      (= bname "DataPts")     (cljotdr.datapts/process raf fmtno bname pos bsize results)
      (= bname "Cksum")         (cljotdr.cksum/process raf fmtno bname pos bsize results)
      :else
      (cljotdr.bypass/process raf fmtno bname pos bsize results)
      ) ;; end cond
    ) ;; end let
  )
  
(defn- real-sor-parse
  [fname raf tracefile debug?]
  ;; (println "* File size " (.length (raf .fh)))
  ;; reset checksum in case this was called before!
  (reset-cksum raf)
  
  ;; process map block
  (loop [
         results (cljotdr.mapblock/process raf {"filename" fname, "debug" debug?, "dump" tracefile})
         blocks (block-seq results)
         ]
    (if (empty? blocks)
      (do
        ;; (println "-------- final list ---------------")
        ;; (pprint results)
        ;; (println "-------- final list end -----------")
        (.close (raf :fh))
        ;; the last form to return is results; we need to close raf here instead of
        ;; in sorparse because we want the return value of sorparse to be results
        results
        )
      (do
        ;; (println "- processing " (get item "name") "block")
        ;; (println "item is" (first blocks))
        (recur
         (process-block raf (first blocks) results)
         (rest blocks)
         ) ;; end recur
        ) ;; end do
      ) ;; end if
    ) ;; end loop
  )

(defn sorparse
  "Parse a OTDR file"
  [fname tracefile debug?]
  (let [ raf (openfile fname)
        ]
    ;; check if file exists -------------------------
    (if (= raf nil)
      (println "* Error opening file; aborting")
      ;; else....
      ;; we close raf in real-sor-parse
      (real-sor-parse fname raf tracefile debug?)
      ) ; end if
    ) ; end let
  )

(defn -sorparse
  "A Java-callable wrapper around the 'sorparse' function"
  [fname tracefile debug?]
  (sorparse fname tracefile debug?)
  )
