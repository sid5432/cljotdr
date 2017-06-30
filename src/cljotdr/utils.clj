(ns cljotdr.utils
  (:require [nio.core]
            [bytebuffer.buff :refer :all]
            [cljotdr.crc :refer [update-cksum initial-cksum]]
            )
  (:gen-class))

(def sol
  "speed of light: 0.299792458 km/usec"
  ;; (/ 299792.458 1.0e6)
  0.299792458
  )


(defn get-curr-cksum
  "get checksum associated with file-object gotten from
  cljotdr.reader/openfile"

  [raf]
  (deref (raf :cksum))
  )

(defn reset-cksum
  "reset checksum associated with file-object raf gotten from
  cljotdr.reader/openfile"

  [raf]
  (dosync (ref-set (raf :cksum) initial-cksum))
  )

(defn myread
  "read one byte from 'raf' and update checksum"
  [raf]
  
  (let [
        bt (.read (raf :fh))
        newsum (update-cksum (get-curr-cksum raf) bt)
        ]
    (dosync (ref-set (raf :cksum) newsum))
    bt
    )
  )

;; ========================================================
(def slimit 1024)

(defn- check-nbytes [xnn]
  (cond
    (contains? #{2 4 8} xnn) xnn
    :else (int 2)
    )
  )

(defn get-string
  "read in a string (stop when reaching '0')"
  [raf]
  (reduce (fn [sumstr _]
            (let [bt (myread raf)]
              (if (= bt 0) (reduced sumstr)
                  (str sumstr (char bt))
                  )
              )
            )
          "" (range slimit))
  )

(defn get-string-alt
  "read in a string (stop when reaching '0')"
  [raf]
  (loop [mystr ""
         bcount 0
         ]
    (if (> bcount slimit)
      (do
        (println "get-string exceed limit " slimit)
        mystr
        )
      (let [bt (myread raf)]
        (if (= bt 0)
          mystr
          (recur (str mystr (char bt)) (inc bcount) )
          )
        )
      )
    )
  )

;; ========================================================
(defn get-hexstring
  "read in specified number of bytes and display as hexidecimals"
  [raf nn]
  (reduce (fn [x _] (str x (format "%02X " (myread raf)))) "" (range nn))
  )

;; ========================================================
(defn get-fixed-string
  "read in specified number of bytes as string"
  [raf nn]
  (reduce (fn [x _] (str x (char (myread raf)))) "" (range nn))
  )

;; ========================================================
(defn get-uint
  "read unsigned int, 2, 4, or 8 bytes; little endian"
  [raf & args]
  (let [
        nn (check-nbytes (first args))
        plist (map (fn [_] (myread raf)) (range nn))
        ]
    (reduce (fn [prev x] (+ x (bit-shift-left prev 8))) 0 (reverse plist))
    )
  )

;; ========================================================
(defn get-signed
  "read signed int, 2, 4, or 8 bytes; little endian"
  [raf & args]
  (let [
        nn (check-nbytes (first args))
        buffer (byte-buffer nn)
        ]
    (nio.core/set-byte-order! buffer :little-endian)
    ;; force the map/lazy-sequence to run!
    (doall
     (map (fn [x] (put-byte buffer (myread raf))) (range nn))
     )
    (.position buffer 0)
    (cond
      (= nn 2) (take-short buffer)
      (= nn 4) (take-int buffer)
      (= nn 8) (take-long buffer)
      )
    )
  )

;; ========================================================
(defn get-signed-alt
  "read signed int, 2, 4, or 8 bytes; little endian"
  [raf & args]
  (let [
        nn (check-nbytes (first args))
        plist (map (fn [_] (myread raf)) (range nn))
        xx (nio.core/byte-buffer (byte-array plist))
        ]
    (nio.core/set-byte-order! xx :little-endian)
    (nio.core/buffer-nth 
     (cond
       (= nn 2) (nio.core/short-buffer xx)
       (= nn 4) (nio.core/int-buffer xx)
       (= nn 8) (nio.core/long-buffer xx)
       ) 0)
    )
  )

;; ========================================================
(defn write-fixed-string
  "write string without trailing \0"
  [raf text]
  ;; (println (map (fn [x] (int x)) (.getBytes text)))
  (doall
   (map (fn [x] (.write (raf :fh) (int x))) (.getBytes text))
   )
  )

(defn write-string
  "write string with trailing \0"
  [raf text]
  (write-fixed-string raf text)
  (.write (raf :fh) 0)
  )

(defn write-hexstring
  [raf hexstring]
  ;(println
  ; (map (fn [[x y _]] (str x y)) (partition 3 hexstring)) )
  
  (doall
   (map (fn [[x y _]] (.write (raf :fh)
                              (Integer/parseInt (str x y) 16))) (partition 3 hexstring))
   )
  )

(defn to-little-endian
  "create a little-endian byte representation of val as an nn element list"
  [val nn]
  (loop [
         i     nn
         plist ()
         xx    val
         ]
    (if (= i 0) (reverse plist)
        (do
          (recur
           (dec i)
           (conj plist (bit-and xx 0xff))
           (bit-shift-right xx 8)
           ); recur
          ); do
        ); if
    ); loop
  )

(defn write-uint
  "write val as nn byte unsigned int"
  [raf val xnn]
  (let [
        nn     (check-nbytes xnn)
        ]
    (doall
     (map (fn [x] (.write (raf :fh) x)) (to-little-endian val nn))
     ); doall
    ); let
  )

(defn write-signed
  "write val as nn byte unsigned int"
  [raf val xnn]
  (let [
        nn     (check-nbytes xnn)
        buffer (byte-buffer nn)
        ]
    (nio.core/set-byte-order! buffer :little-endian)
    (.position buffer 0)
    (cond
      (= nn 2) (put-short buffer val)
      (= nn 4) (put-int   buffer val)
      (= nn 8) (put-long  buffer val)
      )
    (.position buffer 0)
    (doall
     (map (fn [_] (.write (raf :fh) (take-byte buffer)))  (range nn))
     ); doall
    
    ); let
  )


