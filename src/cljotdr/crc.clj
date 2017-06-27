(ns cljotdr.crc
  (:require [biscuit.tables :as lookup])
  (:gen-class))

;; =========================================
;; copied from biscuit.core
(def ltable lookup/crc16-ccitt)
(defn lshift [x] (bit-shift-right x 8))
(defn xshift [x] (bit-shift-left x 8))
(def amask 0x00ffff)
(def xmask 0x00)
(def csum 0xffff)
(def initial-cksum 0xffff)

(defn- mydigest-byte
  "Returns an updated checksum given a previous checksum and a byte"
  [lookup-table lookup-shift xor-shift and-mask checksum byte]
  (-> checksum
      lookup-shift
      (bit-xor byte)
      (bit-and 0xff)
      lookup-table
      (bit-xor (-> checksum
                   xor-shift
                   (bit-and and-mask)))))

(defn- mydmessage
  "Digests the message bytes into a checksum"
  [lookup-table lookup-shift xor-shift and-mask xor-mask checksum message]
  (let [bytes (.getBytes message)]
    (bit-xor
     (reduce
      (partial mydigest-byte lookup-table lookup-shift xor-shift and-mask)
      checksum bytes)
     xor-mask)))

(defn mycrc16 [message]
  (mydmessage ltable lshift xshift amask xmask csum message)
  )

(defn update-cksum
  "take initial/current checksum value and update with a new byte"
  [initcksum byte]
  (mydigest-byte ltable lshift xshift amask initcksum byte))

