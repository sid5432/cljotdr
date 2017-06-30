(ns cljotdr.alter-test
  (:require [clojure.test :refer :all]
            [cljotdr.alter :refer :all]
            [cljotdr.parse :refer :all]
            [cheshire.core :as json]
            [digest]
            ))

(use '[clojure.pprint :only (pp pprint)])

(deftest test-format1
  (testing "test changing bellcore 1.x file"
    (let [
          oldfile     "test/data/demo_ab.sor"
          newjson     "test/data/demo_ab-replacement.json"
          newsor      (java.io.File/createTempFile "file1" ".sor")
          _           (cljotdr.alter/change-sor oldfile newjson newsor)
          tracefile   (java.io.File/createTempFile "trace1" ".dat")
          results-map (cljotdr.parse/sorparse newsor tracefile false)
          ]
      (is (= (get results-map "Cksum") {"checksum_ours" 20600, "checksum" 20600, "match" true}) "Cksum not expected")
      (is (= (get-in results-map ["GenParams" "fiber ID"]) "BIF733-D1-S0 32km") "Fiber ID change failed")
      (is (= (get-in results-map ["FxdParams" "date/time"]) "Fri, 30 Jun 2017 09:04:17 +0000 (1498813457 sec)") "date/time not expected")
      (is (= (digest/sha-256 (clojure.java.io/as-file tracefile))
             "e0a7df057a6ad91f7daa2aacaab8ede5b4b1b72e0bcdcfa439f9f2068c7875a1"
             )
          "trace data sha256 digests do not match")
      
      (.delete newsor)
      (.delete tracefile)
      )
    )
  )

(deftest test-format2
  (testing "test changing bellcore 2.x file"
    (let [
          oldfile     "test/data/sample1310_lowDR.sor"
          newjson     "test/data/sample1310_lowDR-replacement.json"
          newsor      (java.io.File/createTempFile "file2" ".sor")
          _           (cljotdr.alter/change-sor oldfile newjson newsor)
          tracefile   (java.io.File/createTempFile "trace2" ".dat")
          results-map (cljotdr.parse/sorparse newsor tracefile false)
          ]
      (is (= (get results-map "Cksum") {"checksum_ours" 15651, "checksum" 15651, "match" true}) "Cksum not expected")
      (is (= (get-in results-map ["GenParams" "fiber ID"]) "New SMF-28") "Fiber ID change failed")
      (is (= (get-in results-map ["FxdParams" "date/time"]) "Fri, 30 Jun 2017 10:05:45 +0000 (1498817145 sec)") "date/time not expected")
      (is (= (digest/sha-256 (clojure.java.io/as-file tracefile))
             "69c83257bcdcb5db446a0c01bb5a3fa594254b589ebf0a5e2ecb9731543eefdb"
             )
          "trace data sha256 digests do not match")
      
      (.delete newsor)
      (.delete tracefile)
      )
    )
  )
