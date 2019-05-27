(defproject ont-app/vocabulary "0.1.0-SNAPSHOT"
  :description "Namespace descriptions of common LD vocabularies"
  :url "https://github.com/ont-app/vocabulary"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.7.1"
  :dependencies [
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async  "0.4.474"]
                 [lein-doo "0.1.10"]
                 ]
  :plugins [
            [lein-cljsbuild "1.1.7"
             :exclusions [[org.clojure/clojure]]]
            [lein-figwheel "0.5.16"]
            [lein-doo "0.1.10"]
            ]
  :cljsbuild
  {:builds
   {
    ;; DEVELOPMENT PROFILE
    :dev {:source-paths ["src"]
           ;; The presence of a :figwheel configuration here
           ;; will cause figwheel to inject the figwheel client
           ;; into your build
           :figwheel {:on-jsload "vocabulary.core/on-js-reload"
                      ;; :open-urls will pop open your application
                      ;; in the default browser once Figwheel has
                      ;; started and compiled your application.
                      ;; Comment this out once it no longer serves you.
                      :open-urls ["http://localhost:3449/index.html"]}
           :compiler {:main vocabulary.core
                      :asset-path "js/compiled/out"
                      :output-to "resources/public/js/compiled/vocabulary.js"
                      :output-dir "resources/public/js/compiled/out"
                      :source-map-timestamp true
                      ;; To console.log CLJS data-structures make sure you enable devtools in Chrome
                      ;; https://github.com/binaryage/cljs-devtools
                      :preloads [devtools.preload]}}
    
    ;; TEST PROFILE
    ;; for testing the cljs incarnation
    ;; run with 'lein doo firefox test once', or swap in some other browser
    :test {:source-paths ["src" "test/cljs"]
            :compiler {:output-to "resources/test/compiled.js"
                       ;; entry point for doo-runner:
                       :main vocabulary.browser ;; at test/cljs/vocabulary/browser.cljs
                       :optimizations :none
                       ;; :warnings {:bad-method-signature false}
                       }}
    ;; DEPLOYMENT PROFILE
    ;; :min
    ;; {
    ;;  :source-paths ["src"]
    ;;  :compiler {:output-to "resources/public/js/compiled/vocabulary.js"
    ;;             :main vocabulary.core
    ;;             :optimizations :advanced
    ;;             :pretty-print false}
    ;;  }
   }}
   :target-path "target/%s"
   :test-paths ["test/clj"]
   :figwheel {;; :http-server-root "public" ;; default and assumes "resources"
              ;; :server-port 3449 ;; default
              ;; :server-ip "127.0.0.1"
              :css-dirs ["resources/public/css"] ;; watch and update CSS
              ;; Start an nREPL server into the running figwheel process
              ;; :nrepl-port 7888
              ;; To be able to open files in your editor from the heads up display
              ;; you will need to put a script on your path.
              ;; that script will have to take a file path and a line number
              ;; ie. in  ~/bin/myfile-opener
              ;; #! /bin/sh
              ;; emacsclient -n +$2 $1
              ;;
              ;; :open-file-command "myfile-opener"
              ;; if you are using emacsclient you can just use
              ;; :open-file-command "emacsclient"
              ;; if you want to disable the REPL
              ;; :repl false
              ;; to configure a different figwheel logfile path
              ;; :server-logfile "tmp/logs/figwheel-logfile.log"
              ;; to pipe all the output to the repl
              :server-logfile false
              }

  ;; :profiles {:uberjar {:aot :all}}
   ;;:profiles {:dev {:dependencies [[figwheel-sidecar "0.5.4-6"]]}}
   :profiles {:dev {:dependencies [[binaryage/devtools "0.9.9"]
                                   [figwheel-sidecar "0.5.16"]
                                   [cider/piggieback "0.3.1"]]
                    ;; need to add dev source path here to get user.clj loaded
                    :source-paths ["src" "dev"]
                    ;; for CIDER
                    ;; :plugins [[cider/cider-nrepl "0.12.0"]]
                    :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                    ;; need to add the compliled assets to the :clean-targets
                    :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                      :target-path]}}
   )
