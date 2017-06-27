(ns cljotdr.datapts
  (:require
   [cljotdr.utils :refer :all]
   [me.raynes.fs]
   )
  (:gen-class))


(def offset-type
  ; method used by STV: minimum reading shifted to zero
  ; method used by AFL/Noyes Trace.Net: maximum reading shifted to zero (approx)
  "STV" ; "AFL" or "STV"
  )

(defn- xscale
  "use x-axis scale of 0.1 for OFL250, 1.0 for everything else"
  [model]
  (if (= model "OFL250") 0.1
      1.0)
  )

(defn- sanity-check
  [numpts numpts2 results]
  (let [ fnumpts (get-in results ["FxdParams" "num data points"]) ]
    (if (not= numpts fnumpts)
      (println "!!! WARNING !!! DataPts block says number of data points is " numpts " instead of " fnumpts)
      )
    )
  (if (not= numpts2 numpts)
    (println "!!! WARNING !!! DataPts block 2nd num data points is " numpts2 ", not the same as " numpts)
    )
  )

(defn- write-file
  [results tracefile ylist]

  (let [
        dx (get-in results ["FxdParams" "resolution"])
        xscaling (get-in results ["DataPts" "_datapts_params" "xscaling"])
        fac (* dx xscaling 0.001)
        ]
    (with-open [w (clojure.java.io/writer tracefile) ]
      (doall
       (map-indexed
        (fn [i y] (.write w (format "%f\t%f\n" (* i fac) y)))
        ylist)) ; doall
      ) ; with-open
    ) ; let
  )

(defn- dump-data
  [results dlist ymin ymax scale tracefile]
  (if (not (nil? tracefile))
    (let [
          offset-t (get-in results ["DataPts" "_datapts_params" "offset type"])
          fs (* 0.001 scale)
          ]
      (cond
        (= "STV" offset-t) (write-file results tracefile
                                       (map #(* fs (- ymax %)) dlist))
        (= "AFL" offset-t) (write-file results tracefile
                                       (map #(* fs (- ymin %)) dlist))
        :else ; invert
        (write-file results tracefile
                    (map #(* fs (- %)) dlist))
        ); cond
      ); let
    ); if dump
  )

(defn- loop-datapoints
  "loop through data points"
  [results raf numpts scale tracefile]
  (loop [dcount 1
         dlist []
         ymin 1e8
         ymax -1e8
         ]
    (if (<= dcount numpts)
      
      (do ; read one data point
        (let [ yval (get-uint raf 2)
              newmax (if (> yval ymax) yval ymax)
              newmin (if (< yval ymin) yval ymin)
              ]
          (recur
           (inc dcount)
           (conj dlist yval)
           newmin
           newmax
           ) ;recur
          ); let
        ); do
      
      (do
        (dump-data results dlist ymin ymax scale tracefile)
        (->
         results
         (assoc-in ["DataPts" "max before offset"] (read-string (format "%.3f" (* ymax scale 0.001))))
         (assoc-in ["DataPts" "min before offset"] (read-string (format "%.3f" (* ymin scale 0.001))))
         )
        )
      
      ); if
    ); loop
  ) ; defn-


(defn- dump-to-screen
  [results]
  (if (get results "debug")
    (do
      (println "    : num data points =" (get-in results ["DataPts" "num data points"]))
      (println "    : unknown =" (get-in results ["DataPts" "unknown"]))
      (println "    : num data points again =" (get-in results ["DataPts" "num data points 2"]))
      (println "    : scaling factor =" (get-in results ["DataPts" "scaling factor"]))
      (println "    : before applying offset: "
               (format "max %.3f dB," (get-in results ["DataPts" "max before offset"]))
               (format "min %.3f dB"  (get-in results ["DataPts" "min before offset"]))
               )
      )
    )
  ;; pass back
  results
  )

(defn process
  "process DataPts block"
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
        (println "!!!  block header does not match! is " _bname_)
        )
      ) ; end let
    ) ; end if
  
  ;; --- main parsing ------
  (let [
        tracefile (get results "dump")
        model     (get-in results ["SupParams" "OTDR"])
        numpts    (get-uint raf 4)
        unknown   (get-uint raf 2)
        numpts2   (get-uint raf 4)
        scale     (* (get-uint raf 2) 0.001)
        ]
    (sanity-check numpts numpts2 results)
    (->
     results
     (assoc-in ["DataPts" "_datapts_params" "xscaling"] (xscale model))
     (assoc-in ["DataPts" "_datapts_params" "offset type"] offset-type)
     (assoc-in ["DataPts" "num data points"] numpts)
     (assoc-in ["DataPts" "unknown"] unknown)
     (assoc-in ["DataPts" "num data points 2"] numpts2)
     (assoc-in ["DataPts" "scaling factor"] scale)
     (loop-datapoints raf numpts scale tracefile)
     (dump-to-screen)
     )
    ); let
  )
