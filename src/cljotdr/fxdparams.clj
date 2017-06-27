(ns cljotdr.fxdparams
  (:require
   [cljotdr.utils :refer :all]
   [clj-time.coerce :as ct]
   [clj-time.format :as f]
   )
  (:gen-class))

(defn fields
  [fmtno]
  (cond
    (= 1 fmtno)
    (list ; name, start-pos, length (bytes), type, multiplier, precision, units
     ; type: display type: 'v' (value) or 'h' (hexidecimal) or 's' (string)
     ["date/time",0,4,"v","","",""], ; ............... 0-3 seconds in Unix time
     ["unit",4,2,"s","","",""], ; .................... 4-5 distance units, 2 char (km,mt,$
     ["wavelength",6,2,"v",0.1,1,"nm"], ; ............ 6-7 wavelength (nm)
     ["unknown 1",8,6,"h","","",""], ; ............... 8-13 ???
     ["pulse width",14,2,"v","",0,"ns"],  ; .......... 14-15 pulse width (ns)
     ["sample spacing", 16,4,"v",1e-8,"","usec"], ; .. 16-19 sample spacing (in usec)
     ["num data points", 20,4,"v","","",""], ; ....... 20-23 number of data points
     ["index", 24,4,"v",1e-5,6,""], ; ................ 24-27 index of refraction
     ["BC", 28,2,"v",-0.1,2,"dB"], ; ................. 28-29 backscattering coeff
     ["num averages", 30,4,"v","","",""], ; .......... 30-33 number of averages
     ["range", 34,4,"v",2e-5,6,"km"], ; .............. 34-37 range (km)
     ["unknown 2",38,10,"h","","",""], ; ............. 38-47 ???
     ["loss thr", 48,2,"v",0.001,3,"dB"], ; .......... 48-49 loss threshold
     ["refl thr", 50,2,"v",-0.001,3,"dB"], ; ......... 50-51 reflection threshold
     ["EOT thr",52,2,"v",0.001,3,"dB"], ; ............ 52-53 end-of-transmission threshol$
     )
    (= 2 fmtno)
    (list ; name, start-pos, length (bytes), type, multiplier, precision, units
     ; type: display type: "v" (value) or "h" (hexidecimal) or "s" (string)
     ["date/time",0,4,"v","","",""], ; ............... 0-3 seconds in Unix time
     ["unit",4,2,"s","","",""], ; .................... 4-5 distance units, 2 char (km,mt,$
     ["wavelength",6,2,"v",0.1,1,"nm"], ; ............ 6-7 wavelength (nm)
     ["unknown 1",8,10,"h","","",""], ; .............. 8-17 ???
     ["pulse width",18,2,"v","",0,"ns"],  ; .......... 18-19 pulse width (ns)
     ["sample spacing", 20,4,"v",1e-8,"","usec"], ; .. 20-23 sample spacing (usec)
     ["num data points", 24,4,"v","","",""], ; ....... 24-27 number of data points
     ["index", 28,4,"v",1e-5,6,""], ; ................ 28-31 index of refraction
     ["BC", 32,2,"v",-0.1,2,"dB"], ; ................. 32-33 backscattering coeff
     
     ["num averages", 34,4,"v","","",""], ; .......... 34-37 number of averages
     
     ; from Dmitry Vaygant:
     ["averaging time", 38,2,"v",0.1,0,"sec"], ; ..... 38-39 averaging time in seconds
     
     ["range", 40,4,"v",2e-5,6,"km"], ; .............. 40-43 range (km); note x2
     ["unknown 2",44,14,"h","","",""], ; ............. 44-57 ???
     
     ["loss thr", 58,2,"v",0.001,3,"dB"], ; .......... 58-59 loss threshold
     ["refl thr", 60,2,"v",-0.001,3,"dB"], ; ......... 60-61 reflection threshold
     ["EOT thr",62,2,"v",0.001,3,"dB"], ; ............ 62-63 end-of-transmission threshol$
     ["trace type",64,2,"s","","",""], ; ............. 64-65 trace type (ST,RT,DT, or RF)
     ["unknown 3",66,16,"h","","",""], ; ............. 66-81 ???
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
    (str " (unknown unit" val ")")
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
    :else (get-hexstring raf fsize)    
    ) ; cond
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
                (= "date/time" field) (str (f/unparse (f/formatters :rfc822)
                                                      (ct/from-long (* 1000 val))
                                                      )
                                           " (" val " sec)")
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
