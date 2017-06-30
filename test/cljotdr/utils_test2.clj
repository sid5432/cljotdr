(ns cljotdr.utils-test2
  (:require [clojure.test :refer :all]
            [cljotdr.utils :refer :all]
            [cljotdr.reader :refer [openfile]]
            ))


(deftest test-numbers
  (testing "test writing uint"
    (let [
          fname (java.io.File/createTempFile "utils-test" ".dat")
          raf   {:fh (java.io.RandomAccessFile. fname "rw") :cksum (ref 0xff)}
          ]
      (is (= (cljotdr.utils/to-little-endian 16961 2) '(0x41 0x42)) "conversion to little-endian incorrect")
      
      (cljotdr.utils/write-fixed-string raf "1234")
      (cljotdr.utils/write-string raf "abcdefg")
      (cljotdr.utils/write-hexstring raf "01 02 03 04 ")
      (cljotdr.utils/write-uint raf 1234 4)
      (cljotdr.utils/write-signed raf 1234 4)
      (cljotdr.utils/write-signed raf -1234 4)
      
      ;; now read-back
      (.seek (raf :fh) 0)
      (is (= "1234" (get-fixed-string raf 4)) "get/write fixed-string error")
      ;; do *not* include trailing \0 in "abcdefg"
      (is (= "abcdefg" (get-string raf)) "get/write string error")
      (is (= "01 02 03 04 " (get-hexstring raf 4)) "get/write hexstring error")
      (is (= 1234 (get-uint raf 4)) "get/write uint error")
      (is (= 1234 (get-signed raf 4)) "get/write signed int error")
      (is (= -1234 (get-signed raf 4)) "get/write signed int error")
      (.close (raf :fh))

      (.delete fname)
      )
    )
  )

    
      
