(ns cljotdr.cksum
  (:require [cljotdr.utils :refer :all])
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
