(ns ont-app.vocabulary.core-test
  (:require
   [cljs.test :refer [testing is deftest]]
   [ont-app.vocabulary.core :as v]
   ))


(deftest ns-test
  (testing "namespace access"
    (is (= (v/cljc-find-ns 'ont-app.vocabulary.core)
           'ont-app.vocabulary.core))))


(deftest attr-map-test
  (testing "Tests the conditions found in attr-map tests"
     (is (= (v/ns-to-namespace (v/cljc-find-ns 'ont-app.vocabulary.foaf))
            "http://xmlns.com/foaf/0.1/"))
     (is (= (v/ns-to-prefix 'ont-app.vocabulary.foaf)
            "foaf"))
     (is (= (v/iri-for :foaf/homepage)
           "http://xmlns.com/foaf/0.1/homepage"))
    (is (thrown? js/Object 
                 (v/iri-for ::blah))) ;; no metadata in this file
    (is (= (v/iri-for (keyword "http://blah"))
           "http://blah"))
    (is (= (v/iri-for ::v/blah)
           "http://rdf.naturallexicon.org/ont-app/vocabulary/blah"))
    (is (= (v/ns-to-prefix (v/cljc-find-ns 'ont-app.vocabulary.foaf))
              "foaf"))
    (is (= (v/qname-for ::v/blah)
           "voc:blah"))
    (is (= (v/qname-for :foaf/homepage)
           "foaf:homepage"))
    (is (= (v/keyword-for "http://xmlns.com/foaf/0.1/homepage")
           :foaf/homepage))
    (is (= (v/cljc-find-prefixes (v/prefix-re-str)
                                 "Select * Where{?s foaf:homepage ?homepage}")
           #{"foaf"}))
    (is (=
            (v/sparql-prefixes-for
             "Select * Where{?s foaf:homepage ?homepage}")
            (list "PREFIX foaf: <http://xmlns.com/foaf/0.1/>")))
    (is (= (v/prepend-prefix-declarations
               "Select * Where{?s foaf:homepage ?homepage}")
           "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\nSelect * Where{?s foaf:homepage ?homepage}"))
    

     ))
