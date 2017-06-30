(ns cljotdr.mapblock
  (:require [cljotdr.utils :refer :all])
  (:gen-class))

(defn- step1
  "get preamble from mapblock"
  [datain raf version]
  (let
      [
       vnum (* 0.01 (get-uint raf 2))
       nbytes (get-uint raf 4)
       nblocks (dec (get-uint raf 2))
       ]
    (-> datain
        (assoc "format" version)
        (assoc "version" vnum)
        (assoc-in ["mapblock" "nbytes"] nbytes)
        (assoc-in ["mapblock" "nblocks"] nblocks)
        )
    )
  )

(defn- v1
  "version 1: rewind file and spit out 1"
  [raf]
  (.seek (raf :fh) 0)
  (reset-cksum raf)
  1
  )

(defn- stuff-block
  "add block information"
  [datain bname bver bsize order pos]
  (-> datain
      (assoc-in ["blocks" bname] {})
      (assoc-in ["blocks" bname "name"] bname)
      (assoc-in ["blocks" bname "order"] order)
      (assoc-in ["blocks" bname "pos"] pos)
      (assoc-in ["blocks" bname "size"] bsize)
      (assoc-in ["blocks" bname "version"] (format "%.2f" bver))
      )
  )

(defn process
  "process map block"
  [raf datain]
  
  ;; get first four bytes
  (let [
        startstr (get-fixed-string raf 4) ; (reduce (fn [x _] (str x (char (myread raf)))) "" (range 4))
        fmtno (if (= startstr "Map\0") 2 (v1 raf))
        results (step1 datain raf fmtno)
        version (get results "version")
        nbytes (get-in results ["mapblock" "nbytes"])
        nblocks (get-in results ["mapblock" "nblocks"])
        debug? (get datain "debug")
        ]
    (if debug?
      (do
        (println (format "MAIN: bellcore %d.x version" fmtno))
        (println (format "MAIN: version %.2f, block size %d " version nbytes))
        (println (format "MAIN: %d blocks to follow" nblocks))
        (println "---------------------------------------")
        )
      )
    ;; get data for each block
    (loop [bcount 1
           fpos nbytes
           data2 results
           ]
      (if (<= bcount nblocks)
        ;; process block
        (let [
              bname (get-string raf)
              bver (* 0.01 (get-uint raf 2))
              bsize (get-uint raf 4)
              ]
          (if debug?
            (println (format "MAIN: %s block: version %.2f, block size %d bytes, start at 0x%X"
                             bname bver bsize fpos))
            )
          (recur (inc bcount)
                 (+ fpos bsize)
                 (stuff-block data2 bname bver bsize bcount fpos)
                 )
          ); end let
        ;; else: done with processing map block
        (do
          (if debug?
            (println "---------------------------------------")
            )
          data2
          ); end do
        ); end if
      ) ; end loop
    )
  )

;;; ========================================================================================
(defn copy-map-block
  [results-map input output]

  (.seek (input :fh) 0)
  (.seek (output :fh) 0)
  
  (let [
        bsize (get-in results-map ["mapblock" "nbytes"])
        ]
    (doall
     (map (fn [x] (.write (output :fh) (.read (input :fh))))
          (range bsize))
     );do all
    ); let
  )

(defn- initial-search
  [bname mbsize output]
  (.seek (output :fh) 0) ;; start from beginning of mapblock
  (let [
        slength  (inc (.length bname))
        search-n (- mbsize slength)
        fchar    (.substring bname 0 1)
        ]
        ; find first occurance of first character of bname string
    (reduce (fn [x y]
              (let [c (.read (output :fh))]
                (if (= (str (char c)) fchar)
                  (
                   reduced (.getFilePointer (output :fh))
                                        ; (println y c (char c))
                   ))))
            (range search-n))
    )
  )

(defn- final-and-set
  [bname newbsize output start search-n slength]
  (loop [
         spos start
         ]
    (if (>= spos search-n)
      (do
        (println "ERROR: did not find " bname "block!")
        )
      (let [ 
            _ (.seek (output :fh) (dec spos)) ;; one step back in file position!
            candidate (get-fixed-string output (dec slength))
            ]
        ;; (println "\tDEBUG: candidate " candidate bname " now at " spos)
        (if (= bname candidate)
          (do ; match
            ;; (println "\tconfirmed " bname " at pos " (format "0x%X" spos) " now at " (format "0x%X" (.getFilePointer (output :fh)))  )
            (.seek (output :fh) (+ spos slength 1)) ;; skip 2 bytes (version num) to block size location
            ;; (println "\tadvance pos to " (format "0x%X" (.getFilePointer (output :fh))) )
            ;; (println "\tnewblock size " newbsize)
            (write-uint output newbsize 4) ; write new block size
            ;; (println "\tpos now at " (format "0x%X" (.getFilePointer (output :fh))) )
            ) ; do
          
          (recur (inc spos))
          );if
        ); let
      ); if
    ); loop
  )

(defn adjust-block-size
  [bname newbsize mbsize output]
  
  (.seek (output :fh) 0) ;; start from beginning of mapblock
  (let [
        slength  (inc (.length bname))
        search-n (- mbsize slength)
        fchar    (.substring bname 0 1)
        ; find first occurance of first character of bname string
        start    (initial-search bname mbsize output)
        ]
    ; (println "found at post " start)
    ; (println "search to " search-n)
    ;; continue search to confirm
    (final-and-set bname newbsize output start search-n slength)
    )
  )

