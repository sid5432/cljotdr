(ns cljotdr.fxdparams
  (:require
   [cljotdr.utils :refer :all]
   [clj-time.core :as cc]
   [clj-time.coerce :as ct]
   [clj-time.format :as f]
   [cljotdr.mapblock]
   )
  (:gen-class))

(defn fields
  [fmtno]
  (cond
    (= 1 fmtno)
    (list ; name, start-pos, length (bytes), type, multiplier/scale, precision, units, replace-or-not(when altered)
     ; type: display type: 'v' (value) or 'h' (hexidecimal) or 's' (string)
     ["date/time",0,4,"v","","","", true], ; ................ 0-3 seconds in Unix time
     ["unit",4,2,"s","","","", false], ; .................... 4-5 distance units, 2 char (km,mt,..)
     ["wavelength",6,2,"v",0.1,1,"nm", false], ; ............ 6-7 wavelength (nm)
     
     ;; from Andrew Jones
     ["acquisition offset",8,4,"i","","","", false], ; .............8-11 acqusition offset; units?
     ["number of pulse width entries",12,2,"v","","","", false], ; 12-13 number of pulse width entries
     
     ["pulse width",14,2,"v","",0,"ns", false],  ; .......... 14-15 pulse width (ns)
     ["sample spacing", 16,4,"v",1e-8,"","usec", false], ; .. 16-19 sample spacing (in usec)
     ["num data points", 20,4,"v","","","", false], ; ....... 20-23 number of data points
     ["index", 24,4,"v",1e-5,6,"", false], ; ................ 24-27 index of refraction
     ["BC", 28,2,"v",-0.1,2,"dB", false], ; ................. 28-29 backscattering coeff
     ["num averages", 30,4,"v","","","", false], ; .......... 30-33 number of averages
     ["range", 34,4,"v",2e-5,6,"km", false], ; .............. 34-37 range (km)

     ;; from Andrew Jones
     ["front panel offset",38,4,"i","","","", false], ;........ 38-41
     ["noise floor level",42,2,"v","","","", false], ; ........ 42-43 unsigned
     ["noise floor scaling factor",44,2,"i","","","", false], ; 44-45
     ["power offset first point",46,2,"v","","","", false], ;.. 46-47 unsigned
     
     ["loss thr", 48,2,"v",0.001,3,"dB", false], ; .......... 48-49 loss threshold
     ["refl thr", 50,2,"v",-0.001,3,"dB", false], ; ......... 50-51 reflection threshold
     ["EOT thr",52,2,"v",0.001,3,"dB", false], ; ............ 52-53 end-of-transmission threshold
     )
    (= 2 fmtno)
    (list ; name, start-pos, length (bytes), type, multiplier, precision, units
     ; type: display type: "v" (value) or "h" (hexidecimal) or "s" (string)
     ["date/time",0,4,"v","","","", true], ; ................ 0-3 seconds in Unix time
     ["unit",4,2,"s","","","", false], ; .................... 4-5 distance units, 2 char (km,mt,...)
     ["wavelength",6,2,"v",0.1,1,"nm", false], ; ............ 6-7 wavelength (nm)

     ;; from Andrew Jones
     ["acquisition offset",8,4,"i","","","", false], ; .............. 8-11 acquisition offset; units?
     ["acquisition offset distance",12,4,"i","","","", false], ;.... 12-15 acquisition offset distance; units?
     ["number of pulse width entries",16,2,"v","","","", false], ;.. 16-17 number of pulse width entries
     
     ["pulse width",18,2,"v","",0,"ns", false],  ; .......... 18-19 pulse width (ns)
     ["sample spacing", 20,4,"v",1e-8,"","usec", false], ; .. 20-23 sample spacing (usec)
     ["num data points", 24,4,"v","","","", false], ; ....... 24-27 number of data points
     ["index", 28,4,"v",1e-5,6,"", false], ; ................ 28-31 index of refraction
     ["BC", 32,2,"v",-0.1,2,"dB", false], ; ................. 32-33 backscattering coeff
     
     ["num averages", 34,4,"v","","","", false], ; .......... 34-37 number of averages
     
     ; from Dmitry Vaygant:
     ["averaging time", 38,2,"v",0.1,0,"sec", false], ; ..... 38-39 averaging time in seconds
     
     ["range", 40,4,"v",2e-5,6,"km", false], ; .............. 40-43 range (km); note x2

     ;; from Andrew Jones
     ["acquisition range distance",44,4,"i","","","", false], ; ... 44-47
     ["front panel offset",48,4,"i","","","", false], ; ........... 48-51
     ["noise floor level",52,2,"v","","","", false], ; ............ 52-53 unsigned
     ["noise floor scaling factor",54,2,"i","","","", false], ; ... 54-55
     ["power offset first point",56,2,"v","","","", false], ; ..... 56-57 unsigned
     
     ["loss thr", 58,2,"v",0.001,3,"dB", false], ; .......... 58-59 loss threshold
     ["refl thr", 60,2,"v",-0.001,3,"dB", false], ; ......... 60-61 reflection threshold
     ["EOT thr",62,2,"v",0.001,3,"dB", false], ; ............ 62-63 end-of-transmission threshold
     ["trace type",64,2,"s","","","", true], ; .............. 64-65 trace type (ST,RT,DT, or RF)

     ;; from Andrew Jones
     ["X1",66,4,"i","","","", false], ; ............. 66-69
     ["Y1",70,4,"i","","","", false], ; ............. 70-73
     ["X2",74,4,"i","","","", false], ; ............. 74-77
     ["Y2",78,4,"i","","","", false], ; ............. 78-81
     )
    )
  )

(defn unit-map
  [val]
  (cond
    (= "mt" val) " (meters)"
    (= "km" val) " (kilometers)"
    (= "mi" val) " (miles)"
    (= "kf" val) " (kilo-ft)"
    :else
    (str val " (unknown unit)")
    )
  )

(defn tracetype
  [val]
  (cond
    (= "ST" val) (str val "[standard trace]")
    (= "RT" val) (str val "[reverse trace]")
    (= "DT" val) (str val "[difference trace]")
    (= "RF" val) (str val "[reference]")
    :else (str val "[unknown]")
    )
  )

(defn fixscale
  [x scale]
  (if (= scale "") x (* x scale))
  )

(defn setprec
  [x dgt]
  (if (= dgt "") x
      ;; hack to force into float; is there a better way?
      ;; also: use read-string to force formatted numbers back into number types
      (read-string (format (str "%." dgt "f") (+ 0.0 x)))
      )
  )
(defn read-by-type
  [raf fsize ftype scale dgt]
  ;; (println "\t\tget " ftype "pos" (format "0x%X" (.getFilePointer (raf :fh))))
  (cond
    (= ftype "h") (get-hexstring raf fsize)
    (= ftype "s") (get-fixed-string raf fsize) ; (reduce (fn [x _] (str x (char (myread raf)))) "" (range fsize))
    (= ftype "v") (-> (get-uint raf fsize)
                      (fixscale scale)
                      (setprec dgt)
                      )
    (= ftype "i") (-> (get-signed raf fsize)
                      (fixscale scale)
                      (setprec dgt)
                      )
    :else (get-hexstring raf fsize)    
    ) ; cond
  )

(defn unix-time-to-string
  [val]
  ;; NOTE: return time will be UTC!
  ;; (clj-time.coerce/to-string (clj-time.core/from-time-zone (clj-time.core/now) (clj-time.core/time-zone-for-offset 4)))

  ;; alterative:
  ;; (ct/to-string    (cc/fromt-time-zone    (ct/from-long (* 1000 val))   (cc/time-zone-for-offset 4))    )
  
  (str (f/unparse (f/formatters :rfc822)
                  (ct/from-long (* 1000 val))
                  ))
  )

(defn read-field
  [raf fmtno field fspec]
  (let [
        fsize (get fspec 2)
        ftype (get fspec 3)
        scale (get fspec 4)
        dgt   (get fspec 5)
        unit  (get fspec 6)
        val (read-by-type raf fsize ftype scale dgt)
        inter (cond
                (= "number of pulse width entries" field) (if (> val 1)
                                                            (do
                                                              (println "!!!Cannot handle more than one pulse width entry; aborting")
                                                              (System/exit 0)
                                                              )
                                                            val)
                (= "date/time" field) (str (unix-time-to-string val) " (" val " sec)")
                (= "unit" field) (str val (unit-map val))
                (= "trace type" field) (tracetype val)
                :else val
                ) ; cond
        ]
    (if (= unit "")
      inter
      (str inter " " unit) ;; add unit
      ); if
    ) ; let
  )

(defn- dump
  "dump results to screen"
  [results fmtno]
  (if (get results "debug")
    (let [ block (get results "FxdParams") ]
      (doall
       (map-indexed
        (fn [i x]
          (println (format "    : %d. %s:" i (get x 0)) (get block (get x 0)))
          )
        (fields fmtno)
        ) ; map
       ) ; doall
      ) ; let
    ) ; if
  )

(defn process
  "process FxdParams block"
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
        (println "!!!" bname "block header does not match! is " _bname_)
        ;; (println "DEBUG: header okay" bname)
        ) ; if
      ) ; let
    ) ; if

  ;; process each field
  (let [ final
        (loop [
               flist (fields fmtno)
               current results
               ]
          (if (empty? flist)
            (do
              (dump current fmtno)
              ;; return
              current
              )
            (let [fspec (first flist)
                  field (get fspec 0)
                  ]
              ;;
              (recur
               (rest flist)
               (assoc-in current [bname field] 
                         (read-field raf fmtno field fspec)
                         )
               ) ; recur
              ) ; let
            ) ; if
          ) ; loop
        ]
    ;; some post-processing needed: corrections/adjustments
    (let [
          numpt (get-in final [bname "num data points"])
          ior (get-in final [bname "index"])
          dx  (-> (get-in final [bname "sample spacing"])
                  (read-string)
                  (* sol)
                  (/ ior)
                  )
          range (* dx numpt)
          resolution (* dx 1000.0)
          ]
      (if (get results "debug")
        (do
          (println "")
          (println "    : [adjusted for refractive index]")
          (println (format "    : resolution = %.14f m" resolution))
          (println (format "    : range      = %.13f km" range))
          ))
      (->
       final
       (assoc-in [bname "range"] range)
       (assoc-in [bname "resolution"] resolution) ; in meters
       )
      ) ; let (initial)
    ); let (post-processing)
  )

;; ==============================================================
(defn convert-date-time
  "convert to unix time; accepted formats:
  example 1: Thu, 05 Feb 1998 08:46:14 +0000
  example 2: 2017-06-30T09:07:16
  example 3: 2017-06-30 09:07:16
  example 4: 2013-04-16T15:52:00.000-00:00
  "
  [val]
  (let [
        newval (ct/to-long val)
        unrec? (nil? newval)
        useval (if unrec? (ct/to-long (clj-time.core/now)) newval)
        ]
    (if unrec?
      (println "!!! Warning: unrecognized time format " val "; using now as time stamp")
      )
    
    (read-string (format "%.0f" (* 0.001 useval)))
    
    ); let
  )

(defn- convert-num
  [inval scale]
  ;; coerce into float then into int ....
  ;; is there a better way?
  (let [val (read-string (str inval))
        ascale (if (not= scale "") scale 1.0)
        wl  (read-string (format "%.0f" (+ 0.0 (/ val ascale))))
        ]
    wl
    ); let
  )

(defn- real-alter-block
  [bname fmtno old-map new-map input output]

  (println "* Proceesing/altering " bname)
  (let [startpos (.getFilePointer (output :fh))]
    (if (= fmtno 2) ; write header
      (write-string output bname)
      )
    
    (loop [
           flist (fields fmtno)
           ]
      (if (empty? flist) nil
          ;; ..... not empty; need to do all this....
          (let [fspec    (first flist)
                field    (get fspec 0)
                fsize    (get fspec 2)
                ftype    (get fspec 3)
                scale    (get fspec 4)
                replace? (get fspec 7)
                ;; 
                oldval   (get-in old-map [bname field])
                newval   (get-in new-map [bname field])
                ;;
                nonew?   (nil? newval)
                same?    (= newval oldval)
                useval   (if (or nonew? same?) oldval newval)
                ]
            (println "\t- processing: " field)
            (if (or nonew? same?)
              (println "\t\tnot changed or not provided")
              (println "\t\treplace '" oldval "' with '" newval "'")
              )
            
            (cond
              (= "date/time" field) (write-uint output (convert-date-time useval) fsize)
              (= "trace type" field) (write-fixed-string output (.substring useval 0 2))
              (= "unit" field) (write-fixed-string output (.substring useval 0 2))
              ;; (= "wavelength" field) (write-uint output (convert-num useval scale) fsize)
              ;; (= "pulse width" field) (write-uint output (convert-num useval scale) fsize)
              ;; (= "index" field) (write-uint output (convert-num useval scale) fsize)
              ;; (= "num averages" field) (write-uint output (convert-num useval scale) fsize)
              ;; (= "BC" field) (write-uint output (convert-num useval scale) fsize)
              :else (do
                      (cond
                        (= ftype "h") (write-hexstring output useval)
                        (= ftype "s") (write-fixed-string output useval)
                        (= ftype "v") (write-uint output (convert-num useval scale) fsize)
                        (= ftype "i") (write-signed output (convert-num useval scale) fsize)
                        :else nil
                        ) ; cond inner
                      ); do
              ) ;cond outer
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
  (if (not= bname "FxdParams") (println "! wrong block " bname)
      (real-alter-block bname fmtno old-map new-map input output)
      )
  )
