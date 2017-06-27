(ns cljotdr.reader
  (:require [biscuit.tables :as lookup]
            [clojure.java.io :refer [file output-stream input-stream]]
            ;; [nio.core]
            [cljotdr.crc :refer [initial-cksum]]
            )
  (:gen-class))

(import '[java.io RandomAccessFile])

(defn openfile
  "get length of file and return the file handle (as a random-access file)"
  [filename]
  (try
    (let [raf (RandomAccessFile. filename "r")]
      ;; 
      {:fh raf
       :cksum (ref initial-cksum)
       }
      )
    (catch Exception e
      nil
      )
    )
  )



