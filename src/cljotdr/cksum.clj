(ns cljotdr.cksum
  (:require [cljotdr.utils :refer :all]
            [cljotdr.crc :refer [mycrc16]]
            )
  (:gen-class))

(defn process
  "process Cksum block"
  [raf fmtno bname pos bsize results]
  (.seek (raf :fh) pos)

  (if (get results "debug")
    (do
      (println "")
      (println (format "MAIN:  %s block: %d bytes, start pos 0x%X (%d)"
                       bname bsize pos pos))
      )
    )
  ;; get block header
  (if (= fmtno 2)
    (let [ _bname_ (get-string raf)]
      (if (not= bname _bname_)
        (println "!!! Cksum block header does not match! is " _bname_)
        )
      ) ; end let
    ) ; end if
  (let [ours (get-curr-cksum raf)
        theirs (get-uint raf 2)
        match (= ours theirs)
        ]
    (if (get results "debug")
      (do
        (println (format "    : checksum from file %d (0x%X)" theirs theirs))
        (println (format "    : checksum calculated %d (0x%X)" ours ours)
                 (if match "MATCHES!" "DOES NOT MATCH!")
                 )
        )
      )
    (-> results
        (assoc-in ["Cksum" "checksum_ours"] ours)
        (assoc-in ["Cksum" "checksum"] theirs)
        (assoc-in ["Cksum" "match"] match)
        )
    ); end let
  )

;; ========================================================================
(defn- real-alter-block
  [bname fmtno old-map new-map input output]
  
  (if (= fmtno 2) ; write header
    (write-string output bname)
    )
  
  (let [
        currpos (.getFilePointer (output :fh)) ; save current position
        ]
    ;; NOTE: if output file already exists, the (.length (output :fh)) value
    ;; will be incorrect!
    ;; (println "DEBUG: curr pos " currpos)
    (println "* Recalculating checksum")
    (.seek (output :fh) 0) ;; rewind and calculate new checksum
    (reset-cksum output)
    
    ;; re-read the whole file (up to the checksum header) to calculate
    ;; the checksum
    (doall
     (map (fn [_] (myread output))
          (range currpos))
     )
    
    ;; restore position
    (.seek (output :fh) currpos)
    ;; dummy write
    (write-uint output (get-curr-cksum output) 2)
    ); let
  )

(defn alter-block
  [bname fmtno old-map new-map input output]
  (if (not= bname "Cksum") (println "! wrong block " bname)
      (real-alter-block bname fmtno old-map new-map input output)
      )
  )
