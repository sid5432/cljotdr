(ns cljotdr.dump
  (:require [cljotdr.utils :refer :all]
            [me.raynes.fs]
            [cheshire.core :as json]
            )
  (:gen-class
   :name cljotdr.dump
   :methods [#^{:static true} [save_file [clojure.lang.PersistentHashMap String Integer] void]]
   ))

(defn get-opname
  "generate an outputname"
  [fname optype]
  (let [
        basename (me.raynes.fs/name fname)
        ]
    (if (= optype 1) (str basename ".json")
        (str basename ".sml")
        )
    )
  )

(defn- save-to-json
  "save results to file in JSON format"
  [results opname]
  (json/generate-stream results (clojure.java.io/writer opname) {:pretty true})
  nil
  )

(defn- save-to-sml
  "save results to file in SMILE format"
  [results opname]
  (with-open [w (clojure.java.io/output-stream opname)]
    (.write w
            (json/generate-smile results)
            )
    )
  )

(defn save-file
  "save results to file (based on fname), in JSON/SMILE format"
  [results opname optype]
  (let [
        jstring (json/generate-string results {:pretty true})
        ]
    (if (= 1 optype)
      (save-to-json results opname)
      (save-to-sml results opname)
      )
    )
  )

(defn -save_file
  "A Java-callable wrapper around the 'save-file' function"
  [results opname optype]
  (save-file results opname optype)
  )
