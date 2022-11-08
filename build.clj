(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b] ; for b/git-count-revs
            [org.corfield.build :as bb]))

(def lib 'ont-app/vocabulary')
(def version "0.1.0-SNAPSHOT")
#_ ; alternatively, use MAJOR.MINOR.COMMITS:
(def version (format "1.0.%s" (b/git-count-revs nil)))

(defn test "Run the tests." [opts]
  (bb/run-tests opts))

(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/run-tests)
      (bb/clean)
      (bb/jar)))

  (defn clean "Cleans any clj/s compilation output.
  Where:
  `opts` := m s.t. (keys m) #~ #{:clear-caches?, ...}
  `clear-caches?` when `true` indicates to clear .cpcache and .shadow-cljs directories.
  "
  [opts]
  (println (str "Cleaning with opts:" opts "."))
  (bb/clean opts)
  (b/delete {:path "./out"})  
  (b/delete {:path "./cljs-test-runner-out"})
  (when (= (:clear-caches? opts) true)
    (b/delete {:path "./.cpcache"})  
    (b/delete {:path "./.shadow-cljs"}))
  opts)

(defn install "Install the JAR locally." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/install)))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/deploy)))

