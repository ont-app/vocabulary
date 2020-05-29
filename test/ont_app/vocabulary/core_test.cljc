(ns ont-app.vocabulary.core-test
  (:require
   #?(:clj [clojure.test :refer :all]
      :cljs [cljs.test :refer [testing is deftest]]
      )
   #?(:clj [clojure.core :refer [read-string]]
      :cljs [cljs.reader :refer [read-string]])
   [ont-app.vocabulary.core :as v]
   [ont-app.vocabulary.lstr :as lstr]
   ))

(v/cljc-put-ns-meta!
 'ont-app.vocabulary.core-test
 {
  :voc/mapsTo 'ont-app.vocabulary.core ;; <- part of the test
  })

;; FUN WITH READER MACROS

#_(def ErrObject
  #?(:clj Exception
     :cljs js/Object
     ))

(deftest platform-specific-tests
  (testing "namespace access"
    (let [get-sym #?(:cljs identity
                     :clj ns-name)
          ]
      (is (= (get-sym (v/cljc-find-ns 'ont-app.vocabulary.core))
             'ont-app.vocabulary.core)))))
              
  
;; NO READER MACROS BELOW THIS POINT


(deftest attr-map-test
  (testing "Tests the conditions found in attr-map tests"
     (is (= (v/ns-to-namespace (v/cljc-find-ns 'ont-app.vocabulary.foaf))
            "http://xmlns.com/foaf/0.1/"))
     (is (= (v/ns-to-prefix 'ont-app.vocabulary.foaf)
            "foaf"))
     (is (= (v/iri-for :foaf/homepage)
           "http://xmlns.com/foaf/0.1/homepage"))
    (is (= (v/iri-for (v/keyword-for "http://blah"))
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
    (is (= (v/keyword-for "http://example.com/my/stuff")
           :http+58++47++47+example.com+47+my+47+stuff))
    (is (= (v/keyword-for (fn [u k] :no-qname-found)
                          "http://example.com/my/stuff")
           :no-qname-found))
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

(deftest maps-to-test
  (testing ":voc/mapsTo ns metadata should resolve prefixes properly"
    ;; note that the local ns  maps to vocabulary.core
    (is (= (v/iri-for ::blah)
           "http://rdf.naturallexicon.org/ont-app/vocabulary/blah"))
    (is (= (v/qname-for ::blah)
           "voc:blah"))
    ))


(deftest language-tagged-strings
  (testing "langstr dispatch"
    (let [x (read-string "#lstr \"asdf@en\"")]
      ;; ... defer invoking reader macro directly during compilation.
      (is (= (type x) ont_app.vocabulary.lstr.LangStr))
      (is (= (:s x) "asdf"))
      (is (= (:lang x) "en"))
      (is (= (str x) "asdf"))
      (is (= (lstr/lang x) "en"))

      )))
