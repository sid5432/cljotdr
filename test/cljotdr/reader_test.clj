(ns cljotdr.reader-test
  (:require [clojure.test :refer :all]
            [cljotdr.reader :refer [openfile]]
            ))

(deftest test-file1
  (testing "checksum on file 1"
    (let [ fname "resources/samples/demo_ab.sor"
          raf (openfile fname)
          fsize (.length (raf :fh))
          ]
      (.close (raf :fh))
      (is (= fsize 25708))
      )
    )
  )

(deftest test-file2
  (testing "checksum on file 2"
    (let [ fname "resources/samples/sample1310_lowDR.sor"
          raf (openfile fname)
          fsize (.length (raf :fh))
          ]
      (.close (raf :fh))
      (is (= fsize 32133))
      )
    )
  )
