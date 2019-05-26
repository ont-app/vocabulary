(defproject ont-app/vocabulary "0.1.0-SNAPSHOT"
  :description "Namespace descriptions of common LD vocabularies"
  :url "https://github.com/ont-app/vocabulary"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.227"]
                 [lein-doo "0.1.10"]
                 ]
  :plugins [[lein-cljsbuild "1.1.7"
             :exclusions [[org.clojure/clojure]]]
            [lein-figwheel "0.5.16"]
            [lein-doo "0.1.10"]
            ]
  :cljsbuild
  {:builds
   ;; for testing the cljs incarnation
   ;; run with 'lein doo firefox test once', or swap in some other browser
   {:test {:source-paths ["src" "test/cljs"]
           :compiler {:output-to "resources/test/compiled.js"
                      ;; entry point for doo-runner:
                      :main vocabulary.browser ;; at test/cljs/vocabulary/browser.cljs
                      :optimizations :none
                      ;; :warnings {:bad-method-signature false}
                      }}}
   }
  :target-path "target/%s"
  :test-paths ["test/clj"]
  ;; :profiles {:uberjar {:aot :all}}
  :profiles {:dev {:dependencies [[figwheel-sidecar "0.5.4-6"]]}}
  )
