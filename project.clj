(defproject ont-app/vocabulary "0.1.0-SNAPSHOT"
  :description "Utilities to map between namespaced keywords and URIs"
  :url "https://github.com/ont-app/vocabulary"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.7.1"
  :dependencies [
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async  "0.4.474"]
                 [lein-doo "0.1.11"]
                 ]
  :plugins [[lein-codox "0.10.6"]
            [lein-cljsbuild "1.1.7"
             :exclusions [[org.clojure/clojure]]]
            [lein-doo "0.1.11"]
            ]
  ;; CLJC
  :source-paths ["src"]
  ;; CLJ
  :target-path "target/%s"
  :test-paths ["test/cljc"]
  ;; CLJS
  :cljsbuild
  {:test-commands {"test" ["lein" "doo" "node" "test" "once"]}
   :builds
   {
    :test {:source-paths ["src" "test"]
           :compiler {:output-to "resources/test/compiled.js"
                      :output-dir "resources/test/js/compiled/out"
                      ;; entry point for doo-runner:
                      :main ont-app.vocabulary.doo
                      :target :nodejs
                      :optimizations :advanced ;; none
                      :warnings {:bad-method-signature false}
                      }}
   }}
  :codox {:output-path "doc"}
  :profiles {:uberjar {:aot :all}}
  :clean-targets
  ^{:protect false}
  ["resources/dev/js/compiled"
   "resources/test"
   :target-path
   ]
   )
