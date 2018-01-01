(ns cljotdr.misc-test
  (:require [clojure.test :refer :all]
            [cljotdr.fxdparams]
            ))

(deftest test-time
  (testing "check unix time conversion"
    (is (= (cljotdr.fxdparams/convert-date-time "Thu, 05 Feb 1998 08:46:14 +0000") 886668374)
        "error parsing date/time 1")
    (is (= (cljotdr.fxdparams/convert-date-time "1998-02-5T08:46:14+0000") 886668374)
        "error parsing date/time 2")
    
    (is (= (cljotdr.fxdparams/convert-date-time "1998-02-5T08:46:14.333+0000") 886668374)
        "error parsing date/time 3")
    (is (= (cljotdr.fxdparams/unix-time-to-string 886668374) "Thu, 05 Feb 1998 08:46:14 +0000")
        "error converting date/time 1")
    
    )
  
  )
