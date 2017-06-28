(ns cljotdr.file-test
  (:require [clojure.test :refer :all]
            [cljotdr.parse :refer :all]
            [cheshire.core :as json]
            [digest]
            ))

(use '[clojure.pprint :only (pp pprint)])

(deftest test-format1
  (testing "check JSON results with bellcore 1.x file"
    (let [
          tracefile (java.io.File/createTempFile "file1" ".dat")
          debug?    false ; quiet
          fname     "test/data/demo_ab.sor"
          expected  "test/data/demo_ab-expected.json"
          opfile    "/tmp/demo_ab.json"
          results-map  (cljotdr.parse/sorparse fname tracefile debug?)
          ]
      (with-open [in (clojure.java.io/input-stream expected)]
        (let [
              expmap (json/parse-string (slurp in))
              ]
          ;; comparison
          (is (= (get expmap "mapblock")    (get results-map "mapblock") )  "Map blocks do not match!")
          (is (= (get expmap "blocks")      (get results-map "blocks") )    "Blocks do not match!")
          
          (is (= (get expmap "GenParams")   (get results-map "GenParams") ) "GenParams blocks do not match!")
          (is (= (get expmap "FxdParams")   (get results-map "FxdParams") ) "FxdParams blocks do not match!")
          (is (= (get expmap "SupParams")   (get results-map "SupParams") ) "SupParams blocks do not match!")
          (is (= (get expmap "DataPts")     (get results-map "DataPts") )   "DataPts blocks do not match!")

          (is (= (get expmap "KeyEvents")     (get results-map "KeyEvents") )   "DataPts blocks do not match!")
          
          (is (= (get-in expmap      ["KeyEvents" "event 1"])
                 (get-in results-map ["KeyEvents" "event 1"]) ) "KeyEvents e1 blocks do not match!")
          (is (= (get-in expmap      ["KeyEvents" "event 2"])
                 (get-in results-map ["KeyEvents" "event 2"]) ) "KeyEvents e2 blocks do not match!")
          (is (= (get-in expmap      ["KeyEvents" "event 3"])
                 (get-in results-map ["KeyEvents" "event 3"]) ) "KeyEvents e3 blocks do not match!")
          (is (= (get-in expmap      ["KeyEvents" "event 4"])
                 (get-in results-map ["KeyEvents" "event 4"]) ) "KeyEvents e4 blocks do not match!")
          (is (= (get-in expmap      ["KeyEvents" "event 5"])
                 (get-in results-map ["KeyEvents" "event 5"]) ) "KeyEvents e5 blocks do not match!")
          (is (= (get-in expmap      ["KeyEvents" "event 5"])
                 (get-in results-map ["KeyEvents" "event 5"]) ) "KeyEvents e5 blocks do not match!")
          
          (is (= (get expmap "Cksum") (get results-map "Cksum") )     "Cksum blocks do not match!")
          
          (is (= (digest/sha-256 (clojure.java.io/as-file tracefile))
                 "e0a7df057a6ad91f7daa2aacaab8ede5b4b1b72e0bcdcfa439f9f2068c7875a1"
                 )
              "trace data sha256 digests do not match"
              ) ; is
          (.delete tracefile)
          
          ) ; let 
        ) ; with-open
      ) ; let
    ) ; testing
  ) ;deftest

(deftest test-format2
  (testing "check JSON results with bellcore 2.x file"
    (let [
          tracefile (java.io.File/createTempFile "file2" ".dat")
          debug?    false ; quiet
          fname     "test/data/sample1310_lowDR.sor"
          expected  "test/data/sample1310_lowDR-expected.json"
          opfile    "/tmp/sample1310_lowDR.json"
          results-map  (cljotdr.parse/sorparse fname tracefile debug?)
          ]
      (with-open [in (clojure.java.io/input-stream expected)]
        (let [
              expmap (json/parse-string (slurp in))
              ]
          ;; comparison
          (is (= (get expmap "mapblock")    (get results-map "mapblock") )  "Map blocks do not match!")
          (is (= (get expmap "blocks")      (get results-map "blocks") )    "Blocks do not match!")
          
          (is (= (get expmap "GenParams")   (get results-map "GenParams") ) "GenParams blocks do not match!")
          (is (= (get expmap "FxdParams")   (get results-map "FxdParams") ) "FxdParams blocks do not match!")
          (is (= (get expmap "SupParams")   (get results-map "SupParams") ) "SupParams blocks do not match!")
          (is (= (get expmap "DataPts")     (get results-map "DataPts") )   "DataPts blocks do not match!")

          (is (= (get expmap "KeyEvents")     (get results-map "KeyEvents") )   "DataPts blocks do not match!")
          
          (is (= (get-in expmap      ["KeyEvents" "event 1"])
                 (get-in results-map ["KeyEvents" "event 1"]) ) "KeyEvents e1 blocks do not match!")
          (is (= (get-in expmap      ["KeyEvents" "event 2"])
                 (get-in results-map ["KeyEvents" "event 2"]) ) "KeyEvents e2 blocks do not match!")
          (is (= (get-in expmap      ["KeyEvents" "event 3"])
                 (get-in results-map ["KeyEvents" "event 3"]) ) "KeyEvents e3 blocks do not match!")
          
          (is (= (get expmap "Cksum")       (get results-map "Cksum") )     "Cksum blocks do not match!")

          (is (= (digest/sha-256 (clojure.java.io/as-file tracefile))
                 "69c83257bcdcb5db446a0c01bb5a3fa594254b589ebf0a5e2ecb9731543eefdb"
                 )
              "trace data sha256 digests do not match"
              ) ; is
          (.delete tracefile)

          ) ; let
        ) ; with-open
      ) ;  let
    ) ; testing
  ) ; deftest
