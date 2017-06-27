(defproject org.clojars.sidneyli/cljotdr "0.1.0-SNAPSHOT"
  :description "OTDR SOR file parse"
  :url "https://github.com/sid5432/cljotdr"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [biscuit "1.0.0"]
                 [me.raynes/fs "1.4.6"]
                 [bytebuffer "0.2.0"]
                 [nio "1.0.4"]
                 [cheshire "5.7.1"]
                 [clj-time "0.13.0"]
                 [digest "1.4.5"]
                 ]
  :main ^:skip-aot cljotdr.core
  :target-path "target/%s"
  :profiles {
             :uberjar {:aot :all}
             }
  )
