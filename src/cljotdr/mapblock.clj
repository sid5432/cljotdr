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

