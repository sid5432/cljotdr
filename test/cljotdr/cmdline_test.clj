(ns cljotdr.cmdline-test
  (:require [clojure.test :refer :all]
            [cljotdr.cmdline :refer :all]
            ))

(deftest test1
  (testing "Command line parsing (normal)"
    (let [expected {:verbose "no", :test 1, :dump "yes", :json 2}
          response ((clojure.tools.cli/parse-opts
                    (list "--test" "1" "--json" "2" "--dump" "what?" "-v" "no") cli-options) :options)
          ]
      (is (= response expected))
      )
    )
  )

(deftest test2
  (testing "Command line parsing (check --json default)"
    (let [expected {:verbose "yes", :test 1, :dump "no", :json 1}
          response ((clojure.tools.cli/parse-opts
                    (list "--test" "1" "--json" "3" "--dump" "no") cli-options) :options)
          ]
      (is (= response expected))
      )
    )
  )

(deftest test3
  (testing "Command line parsing (check --test default)"
    (let [expected {:verbose "yes", :dump "yes", :json 2}
          response ((clojure.tools.cli/parse-opts
                    (list "--test" "-2" "--json" "2") cli-options) :options)
          ]
      (is (= response expected) "failed to catch bogus --test arg")
      )
    )
  )

(deftest test4
  (testing "Command line parsing (return values)"
    (let [ [fname optype dump?] (apply filename_etc
                                 (list "--file" "dm.sor" "-t" "1" "-j" "3"))
          ]
      (is (= fname "dm.sor"))
      (is (= optype 1))
      )
    )
  )

(deftest test5
  (testing "Command line parsing (return values - defaults)"
    (let [ [fname optype dump?] (apply filename_etc
                                 (list "--test" "1" "-j" "1"))
          ]
      (is (= fname "resources/samples/demo_ab.sor"))
      (is (= optype 1))
      )
    )
  )

(deftest test6
  (testing "Command line parsing (return values - defaults)"
    (let [ [fname optype dump?] (apply filename_etc
                                 (list "--test" "2" "-j" "2"))
          ]
      (is (= fname "resources/samples/sample1310_lowDR.sor"))
      (is (= optype 2))
      )
    )
  )
