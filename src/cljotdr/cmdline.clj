(ns cljotdr.cmdline
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            )
  (:gen-class))

(def cli-options
  [["-f" "--file filename" "File name"]
   ["-t" "--test type"
    "Run test: 1: SOR version 1 (default), 2: SOR version 2 "
    ;; :default 2
    :parse-fn #(Integer/parseInt %)
    :validate [#(<= 1 % 2) "Either 1 or 2"]
    ]
   ["-v" "--verbose yes_or_no" "print debug message"
    :default "yes"
    :validate [#(contains? #{"yes" "no"} %) "yes or no"]    
    ]
   ["-d" "--dump yes_or_no" "dump data points to a file"
    :default "yes"
    :validate [#(contains? #{"yes" "no"} %) "yes or no"]
    ]
   ["-h" "--help" ]
   ["-j" "--json OutputType" "Output type: 1: JSON (default) or 2: SMILE"
    :default 1
    :parse-fn #(Integer/parseInt %)
    :validate [#(<= 1 % 2) "Either 1 or 2"]
    ]
   ])

(defn- usage [options-summary]
  (->> ["Usage: cljotdr [options]"
  ""
  "Options:"
  options-summary
  ""
  ]
  (string/join \newline)))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn- error-msg [errors]
  (str "Error occurred:\n\n"
       (string/join \newline errors)))

(defn validate-args [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
        testfile1 "test/data/demo_ab.sor"
        testfile2 "test/data/sample1310_lowDR.sor"
        returnval  {:sorfile (options :file)
                    :json (options :json)
                    :dump (options :dump)
                    :verbose (options :verbose)
                    }
        ]
    (cond
      ;; help => exit OK with usage summary
      (:help options) {:exit-message (usage summary) :ok? true}
      ;; file given; override :test (if applicable)
      (:file options) returnval
      ;; file not given; check :test option
      (:test options) ;; which version to test
      (let [ver (options :test)]
        (cond
          (= ver 1) (assoc returnval :sorfile testfile1)
          (= ver 2) (assoc returnval :sorfile testfile2)
          :else {:exit-message (usage summary)}))
      ;; failed => exit with usage summary
      :else {:exit-message (usage summary)})))

;;============================================================
(defn filename_etc
  "Get filename and options from command line"
  [& args]
  (let [{:keys [exit-message sorfile dump json verbose ok?]} (validate-args args)]
    (cond
      exit-message (println exit-message)
      sorfile      (list sorfile json dump verbose)
      :else        (println exit-message verbose)
      )
    )
)
