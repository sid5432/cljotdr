(ns cljotdr.keyevents
  (:require [cljotdr.utils :refer :all])
  (:gen-class))

(defn- fields
  [fmtno]
  (cond
    (= 1 fmtno)
    (list
     )
    (= 2 fmtno)
    (list
     )
    )
  )

(defn- get-v2
  [raf fmtno factor]
  (if (= fmtno 1)
    (repeat 5 nil)
    ;; else
    (map (fn [_] (* factor (get-uint raf 4))) (range 5))
    ); if
  )

(defn- fill-v2
  [results fmtno blkid end-prev start-curr end-curr start-next pkpos]
  (if (= fmtno 1)
    results
    (-> results
        ;; bellcore 2.x
        (assoc-in ["KeyEvents" blkid "end of prev"] (read-string (format "%.3f" end-prev)))
        (assoc-in ["KeyEvents" blkid "start of curr"] (read-string (format "%.3f" start-curr)))
        (assoc-in ["KeyEvents" blkid "end of curr"] (read-string (format "%.3f" end-curr)))
        (assoc-in ["KeyEvents" blkid "start of next"] (read-string (format "%.3f" start-next)))
        (assoc-in ["KeyEvents" blkid "peak"] (read-string (format "%.3f" pkpos)))
        )
    )
  )

(defn- decode-subtype
  [subtype]
  (cond
    (= "1" subtype) " reflection"
    (= "0" subtype) " loss/drop/gain"
    (= "2" subtype) " multiple"
    :else           " unknown"
    )
  )

(defn- decode-manual-auto
  [manual]
  (cond
    (= manual "A") " {manual}"
    :else          " {auto}"
    )
  )

(defn- event-type
  "decode event type"
  [xtype]
  (let [
        subtype (decode-subtype (.substring xtype 0 1))
        manual  (decode-manual-auto (.substring xtype 1 2))
        remain  (.substring xtype 2)
        ]
    (cond
      (not= remain "9999LS") (str xtype " [unknown type]")
      :else
      (str xtype manual subtype)
    )
    )
  )

(defn- read-an-event
  [raf result evnum fmtno factor]
  (let [
        xid (get-uint raf 2)
        blkid (str "event " xid)
        dist (* (get-uint raf 4) factor)
        slope (* 0.001 (get-signed raf 2))
        splice (* 0.001 (get-signed raf 2))
        refl (* 0.001 (get-signed raf 4))
        xtype (event-type (get-fixed-string raf 8))
        [end-prev start-curr end-curr start-next pkpos] (get-v2 raf fmtno factor)
        comments (get-string raf)
        ]
    ;; (println "\tDEBUG: event" xid " pos " (format "0x%X" (.getFilePointer raf)))
    (-> result
     (assoc-in ["KeyEvents" blkid] {})
     (assoc-in ["KeyEvents" blkid "type"] xtype)
     (assoc-in ["KeyEvents" blkid "distance"] (read-string (format "%.3f" dist)))
     (assoc-in ["KeyEvents" blkid "slope"] (read-string (format "%.3f" slope)))
     (assoc-in ["KeyEvents" blkid "splice loss"] (read-string (format "%.3f" splice)))
     (assoc-in ["KeyEvents" blkid "refl loss"] (read-string (format "%.3f" refl)))
     (assoc-in ["KeyEvents" blkid "comments"] comments)
     (fill-v2 fmtno blkid end-prev start-curr end-curr start-next pkpos)
     )
    )
  )

(defn- read-summary
  [raf result factor]
  (let [
        total       (* (get-signed raf 4) 0.001)
        loss-start  (* (get-signed raf 4) factor)
        loss-finish (* (get-signed raf 4) factor)
        orl         (* (get-signed raf 2) 0.001)
        orl-start   (* (get-signed raf 4) factor)
        orl-finish  (* (get-signed raf 4) factor)
        ]
    (-> result
        (assoc-in ["KeyEvents" "Summary" "total loss"] (read-string (format "%.3f" total)))
        (assoc-in ["KeyEvents" "Summary" "ORL"] (read-string (format "%.3f" orl)))
        (assoc-in ["KeyEvents" "Summary" "loss start"] (read-string (format "%.6f" loss-start)))
        (assoc-in ["KeyEvents" "Summary" "loss end"] (read-string (format "%.6f" loss-finish)))
        (assoc-in ["KeyEvents" "Summary" "ORL start"] (read-string (format "%.6f" orl-start)))
        (assoc-in ["KeyEvents" "Summary" "ORL end"] (read-string (format "%.6f" orl-finish)))
        )
    ) ; let
  )

(defn- dump
  "dump results to screen"
  [results fmtno]
  (if (get results "debug")
    (let [block (get results "KeyEvents")
          nev (get block "num events")
          ]
      (println (format "    : %d events" nev))
      ;; loop through events
      (loop [
             evnum 1
             ]
        (if (<= evnum nev)
          (let [
                blkid (format "event %d" evnum)
                dist (get-in results ["KeyEvents" blkid "distance"])
                slope (get-in results ["KeyEvents" blkid "slope"])
                splice (get-in results ["KeyEvents" blkid "splice loss"])
                refl (get-in results ["KeyEvents" blkid "refl loss"])
                xtype (get-in results ["KeyEvents" blkid "type"])
                ;; bellcore 2.x                
                comments (get-in results ["KeyEvents" blkid "comments"])
                end-prev (get-in results ["KeyEvents" blkid "end of prev"])
                end-curr (get-in results ["KeyEvents" blkid "end of curr"])
                start-curr (get-in results ["KeyEvents" blkid "start of curr"])
                start-next (get-in results ["KeyEvents" blkid "start of next"])
                pkpos (get-in results ["KeyEvents" blkid "peak"])
                ]
            (println "    : Event" evnum ": type" xtype)
            (println "    :    : distance:" (format "%.3f km" dist))
            (println "    :    : slope:" (format "%.3f dB/km" slope))
            (println "    :    : splice loss:" (format "%.3f dB" splice))
            (println "    :    : refl loss:" (format "%.3f dB" refl))
            (if (= fmtno 2)
              (do
                (println "    :    : end of previous event:" (format "%.3f km" end-prev))
                (println "    :    : start of current event:" (format "%.3f km" start-curr))
                (println "    :    : end of current event:" (format "%.3f km" end-curr))
                (println "    :    : start of next event:" (format "%.3f km" start-next))
                (println "    :    : peak point of event:" (format "%.3f km" pkpos))                
                );do
              ) ; if
            (println "    :    : comments:" comments)
            (recur (inc evnum))
            ); let
          ) ; if
        ); loop (events)
      ;; summary report
      (let [
            total (get-in results ["KeyEvents" "Summary" "total loss"])
            orl (get-in results ["KeyEvents" "Summary" "ORL"])
            loss-start (get-in results ["KeyEvents" "Summary" "loss start"])
            loss-end (get-in results ["KeyEvents" "Summary" "loss end"])
            orl-start (get-in results ["KeyEvents" "Summary" "ORL start"])
            orl-end (get-in results ["KeyEvents" "Summary" "ORL end"])
            ]
        (println "    : Summary:")
        (println "    :    : total loss:" (format "%.3f dB" total))
        (println "    :    : ORL:" (format "%.3f dB" orl))
        (println "    :    : loss start:" (format "%.6f km" loss-start))
        (println "    :    : loss end:" (format "%.6f km" loss-end))
        (println "    :    : ORL start:" (format "%.6f km" orl-start))
        (println "    :    : ORL finish:" (format "%.6f km" orl-end))
        ) ; let (summary)
      ) ; let (all key events)
    ) ; if
  )

(defn- read-events
  [raf results bname nev fmtno factor]
  (loop [
         evnum 1
         current (assoc-in results [bname "num events"] nev)
         ]
    (if (<= evnum nev)
      (recur
       (inc evnum)
       (read-an-event raf current evnum fmtno factor)
       ) ; recur
      (do
        ;; (println (format "\tDEBUG: at end pos 0x%X" (.getFilePointer (raf :fh))))
        ;; return
        current
        )
      ) ; if
    ) ; loop
  )

(defn process
  "process KeyEvents block"
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
        ) ; if
      ) ; let
    ) ; if
  ;; process each field
  (let [nev (get-uint raf 2)
        rindx (get-in results ["FxdParams" "index"])
        factor (-> 1e-4 (* sol) (/ rindx))
        stage1 (read-events raf results bname nev fmtno factor)
        final  (read-summary raf stage1 factor)
        ]
    (dump final fmtno)
    final
    ); let
  ) ; eofn
