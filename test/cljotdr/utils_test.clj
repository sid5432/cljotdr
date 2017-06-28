(ns cljotdr.utils-test
  (:require [clojure.test :refer :all]
            [cljotdr.utils :refer :all]
            [cljotdr.reader :refer [openfile]]
            ))

(deftest test-uint
  (testing "test reading uint"
    (let [ fname "test/data/demo_ab.sor"
          raf (openfile fname)
          ]
      ;; test default
      (.seek (raf :fh) 0)
      (is (= (get-uint raf) 100))
      ;; test short(2)
      (.seek (raf :fh) 0)
      (is (= (get-uint raf 2) 100))
      ;; test int(2)
      (.seek (raf :fh) 0)
      (is (= (get-uint raf 4) 9699428))

      (.close (raf :fh))
      )
    )
  )

(deftest test-get-string
  (testing "test reading string"
    (let [ fname "test/data/sample1310_lowDR.sor"
          raf (openfile fname)
          mstr (get-string raf)
          ]
      (is (= mstr "Map"))
      )
    )
  )

(deftest test-get-hexstring
  (testing "test reading hexidecimal"
    (let [ fname "test/data/demo_ab.sor"
          raf (openfile fname)
          mstr (get-hexstring raf 4)
          ]
      (is (= mstr "64 00 94 00 "))
      )
    )
  )

(deftest test-signed
  (testing "test reading signed"
    (let [ fname "test/data/demo_ab.sor"
          raf (openfile fname)
          ]

      ;; test short(2)
      (.seek (raf :fh) 0x51)
      (is (= (get-signed raf 2) -28672))
      ;; test int(4)
      (.seek (raf :fh) 0x51)
      (is (= (get-signed raf 4) 36864))
      (.close (raf :fh))
      )
    )
  )

(deftest test-file1
  (testing "test reading whole file"
    (let [ fname "test/data/demo_ab.sor"
          raf (openfile fname)
          flength (- (.length (raf :fh)) 2)
          ]
      (.seek (raf :fh) 0)
      (reset-cksum raf)
      (loop [ii 0]
        (if (= ii flength) ()
            (do
              (myread raf)
              (recur (inc ii))
              )
            )
        )
      (.close (raf :fh))
      (is (= (get-curr-cksum raf) 38827))
      (reset-cksum raf)
      )
    )
  )

(deftest test-file2
  (testing "test reading whole "
    (let [ fname "test/data/sample1310_lowDR.sor"
          raf (openfile fname)
          flength (- (.length (raf :fh)) 2)
          ]
      (.seek (raf :fh) 0)
      (reset-cksum raf)
      (loop [ii 0]
        (if (= ii flength) ()
            (do
              (myread raf)
              (recur (inc ii))
              )
            )
        )
      (.close (raf :fh))
      (is (= (get-curr-cksum raf) 62998))
      (reset-cksum raf)
      )
    )
  )
