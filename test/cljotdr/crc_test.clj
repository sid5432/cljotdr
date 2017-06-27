(ns cljotdr.crc-test
  (:require [clojure.test :refer :all]
            [cljotdr.crc :refer :all]
            [cljotdr.reader :refer [openfile]]
            ))

(deftest test1
  (testing "test CRC on standard string"
    (let [mstring "123456789"
          mlist (take 10 (.getBytes mstring))
          expected 0x29B1
          ]
      ;; put back together as string
      ;; (println "origial string is " (apply str (map char mlist)))
      (is
       (= (mycrc16 mstring) expected)
       )
      (is
       (= (reduce update-cksum initial-cksum mlist) expected)
       )
      (defn- finalsum []
        (loop [ii 0 psum
               initial-cksum
               ]
          (if (= ii 9) psum
              (recur (inc ii) (update-cksum psum (nth mlist ii)))
              )
          )
        )
      (is (= (finalsum) expected))
      )
    )
  )

(deftest test-file1
  (testing "checksum on file 1"
    (let [ fname "resources/samples/demo_ab.sor"
          raf (openfile fname)
          flength (- (.length (raf :fh)) 2)
          ]
      (defn- finalsum []
        (loop [ii 0 psum initial-cksum]
          (if (= ii flength) psum
              (recur (inc ii) (update-cksum psum (.read (raf :fh))))
              )
          ))
      (is (= (finalsum) 38827))
      (.close (raf :fh))
      )
    )
  )

(deftest test-file2
  (testing "checksum on file 2"
    (let [ fname "resources/samples/sample1310_lowDR.sor"
          raf (openfile fname)
          flength (- (.length (raf :fh)) 2)
          ]
      (defn- finalsum []
        (loop [ii 0 psum initial-cksum]
          (if (= ii flength) psum
              (recur (inc ii) (update-cksum psum (.read (raf :fh))))
              )
          ))
      (is (= (finalsum) 62998))
      (.close (raf :fh))
      )
    )
  )
