(ns cljotdr.genparams
  (:require
   [cljotdr.utils :refer :all]
   [cljotdr.mapblock :refer [adjust-block-size]]
   )
  (:gen-class))

(defn- fields
  [fmtno]
  (cond
    (= 1 fmtno)
    (list
     "cable ID",    ;; ........... 0
     "fiber ID",    ;; ........... 1
     "wavelength",  ;; ............2: fixed 2 bytes value
     
     "location A",  ;; ........... 3
     "location B",  ;; ........... 4
     "cable code/fiber type", ;... 5
     "build condition", ;; ....... 6: fixed 2 bytes char/string
     "(unknown 1)", ;; ........... 7: fixed 4 bytes
     "operator",    ;; ........... 8
     "comments",    ;; ........... 9
     )
    (= 2 fmtno)
    (list
     "cable ID",    ;; ........... 0
     "fiber ID",    ;; ........... 1

     "fiber type",  ;; ........... 2: fixed 2 bytes value
     "wavelength",  ;; ............3: fixed 2 bytes value

     "location A", ;; ............ 4
     "location B", ;; ............ 5
     "cable code/fiber type", ;... 6
     "build condition", ;; ....... 7: fixed 2 bytes char/string
     "(unknown 2)", ;; ........... 8: fixed 8 bytes
     "operator",    ;; ........... 9
     "comments",    ;; .......... 10
     )
    )
  )

(defn- fiber-type
  "decode fiber type; 
  REF: http://www.ciscopress.com/articles/article.asp?p=170740&seqNum=7"
  [val]
  (cond
    (= val 651) ; ITU-T G.651
    "G.651 (50um core multimode)"
    (= val 652)  ; standard nondispersion-shifted
    "G.652 (standard SMF)" ; G.652.C low Water Peak Nondispersion-Shifted Fiber
    (= val 653) 
    "G.653 (dispersion-shifted fiber)"
    (= val 654)
    "G.654 (1550nm loss-minimzed fiber)"
    (= val 655)
    "G.655 (nonzero dispersion-shifted fiber)"
    :else
    (format "%d (unknown)" val)
    )
  )

(defn- build-condition
  "decode build condition"
  [bcstr]
  (cond 
    (= bcstr "BC")  (str bcstr " (as-built)")
    (= bcstr "CC")  (str bcstr " (as-current)")
    (= bcstr "RC")  (str bcstr " (as-repaired)")
    (= bcstr "OT")  (str bcstr " (other)")
    :else           (str bcstr " (unknown)")
    )
  )

(defn- read-field
  [raf fmtno field]
  (cond
    (= "build condition" field) (build-condition
                                 (str (char (myread raf)) (char (myread raf)))
                                 )
    (= "fiber type" field) (fiber-type (get-uint raf 2))
    (= "wavelength" field) (format "%d nm" (get-uint raf 2))
    (= "(unknown 1)" field) (format "VALUE %d" (get-uint raf 4))
    (= "(unknown 2)" field) (str "VALUE " (str (get-uint raf 8)))
    :else
    (get-string raf)
    ) ; end cond
  )

(defn- dump
  "dump results to screen"
  [results fmtno]
  (if (get results "debug")
    (let [ block (get results "GenParams") ]
      (println "    :  language:" (get block "language"))
      (doall
       (map-indexed
        (fn [i x]
          (println (format "    : %d. %s:" i x) (get block x))
          )
        (fields fmtno)
        ) ; map
       ) ; doall
      ) ; let
    ) ; if
  )


(defn process
  "process GenParams block"
  [raf fmtno bname pos bsize results]
  (.seek (raf :fh) pos)
  
  (if (get results "debug")
    (do
      (println "")
      (println (format "MAIN:  %s block: %d bytes, start pos 0x%X (%d)"
                       bname bsize pos pos))
      ) ; do
    ) ; if
  ;; get block header
  (if (= fmtno 2)
    (let [ _bname_ (get-string raf)]
      (if (not= bname _bname_)
        (println "!!! Cksum block header does not match! is " _bname_)
        ) ; if
      ) ; let
    ) ; if
  
  ;; process each field
  (loop [
         flist (fields fmtno)
         current (assoc-in results [bname "language"]
                           (str (char (myread raf)) (char (myread raf)))
                           ) ;; get language
         ]
    (if (empty? flist)
      (do
        (dump current fmtno)
        ;; return
        current
        )
      (let [field (first flist)]
        ;;
        (recur
         (rest flist)
         (assoc-in current [bname field]
                   (read-field raf fmtno field)
                   )
         ) ; recur
        ) ; let
      ) ; if
    ) ; loop
  )

;; ===========================================================
(defn- map-fiber-type
  [val]
  (if
      (= "G." (.substring val 0 2)) (read-string (.substring val 2 5))
      (read-string (.substring val))
      )
  )

(defn- real-alter-block
  [bname fmtno old-map new-map input output]

  (println "* Proceesing/altering " bname)
  (let [startpos (.getFilePointer (output :fh))]
    (if (= fmtno 2) ; write header
      (write-string output bname)
      )
    
    (write-fixed-string output (get-in new-map [bname "language"])) ; write language
    
    (loop [
           flist (fields fmtno)
           ]
      (if (empty? flist) nil
          (let [field (first flist)
                oldval (get-in old-map [bname field])
                tmpval (get-in new-map [bname field])
                newval (if (nil? tmpval) oldval tmpval)
                ]
            (cond
              (= "wavelength" field) (write-uint output
                                                 (read-string newval)   2)
              (= "build condition" field) (write-fixed-string output
                                                              (.substring newval 0 2) )
              (= "(unknown 1)" field) (write-uint output
                                                  (read-string (.substring newval 5)) 4)
              (= "(unknown 2)" field) (write-uint output
                                                  (read-string (.substring newval 5)) 8)
              ;; version 2 fields
              (= "fiber type" field) (write-uint output
                                                 (map-fiber-type newval)   2)
              :else (write-string output newval)
              )
            (recur (rest flist))
            ); let
          ); if
      ) ;loop
    
    ;; (println "\tDEBUG: " bname " block: loop finished")
    (let [
          currpos  (.getFilePointer (output :fh))
          newbsize (- currpos startpos)
          mbsize   (get-in old-map ["mapblock" "nbytes"])
          ]
      ;; (println "Old block size " (get-in old-map ["blocks" bname "size"]))
      ;; (println "New block size " newbsize)
      
      (cljotdr.mapblock/adjust-block-size bname newbsize mbsize output)
      
      (.seek (output :fh) currpos) ;; restore file position for next round
      ); let (adjust-block-size)
    
    ); let (startpos)
  )

(defn alter-block
  [bname fmtno old-map new-map input output]
  (if (not= bname "GenParams") (println "! wrong block " bname)
      (real-alter-block bname fmtno old-map new-map input output)
      )
  )
