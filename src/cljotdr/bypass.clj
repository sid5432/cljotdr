(ns cljotdr.bypass
  (:require [cljotdr.utils :refer :all])
  (:gen-class))

(defn process
  "process TBD block; use this as a template as a starting point 
  to write a new block readers"
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
        (println "!!!" bname " block header does not match! is " _bname_)
        )
      ) ; end let
    ) ; end if
  ;; read to end (to get checksum correct)
  (let [
        curr-pos (.getFilePointer (raf :fh))
        readsize (- curr-pos pos)
        bsize2 (- bsize readsize)
        ]
    ;; (println "DEBUG: read size is " readsize)
    (doall
     (map (fn [_] (myread raf)) (range bsize2))
     )
    )
  (let [
        status true
        ]
    (if (get results "debug")
      (do
        (println (format "    : nothing to process yet"))
        )
      )
    (-> results
        (assoc-in [bname "ignore"] status)
        )
    ); end let
  )
