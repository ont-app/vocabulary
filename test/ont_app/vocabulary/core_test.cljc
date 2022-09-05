(ns ont-app.vocabulary.core-test
  (:require
   #?(:clj [clojure.test :refer :all]
      :cljs [cljs.test :refer [testing is deftest]]
      )
   #?(:clj [clojure.core :refer [read-string]]
      :cljs [cljs.reader :refer [read-string]])
   #?(:clj [ont-app.vocabulary.core :as v :refer :all]
      :cljs [ont-app.vocabulary.core :as v]
      )
   [ont-app.vocabulary.lstr :as lstr]
   [ont-app.vocabulary.format :as fmt]
   ;;#?(:cljs [cljs.reader :as edn])
   ;;#?(:clj [ont-app.vocabulary.issue21])
   ))


(v/put-ns-meta!
 'ont-app.vocabulary.core-test
 {
  :voc/mapsTo 'ont-app.vocabulary.core ;; <- part of the test
  })


#?(:cljs
  (cljs.reader/register-tag-parser! "lstr" lstr/read-LangStr))
   
;; FUN WITH READER MACROS

(deftest platform-specific-tests
  (testing "namespace access"
    (let [get-sym #?(:cljs identity
                     :clj ns-name)
          ]
      (is (= (get-sym (v/cljc-find-ns 'ont-app.vocabulary.core))
             'ont-app.vocabulary.core)))))

(defn- cljc-s
  [x]
  #?(:clj (.s x)
     :cljs (.-s x)))

(defn- cljc-lang
  [x]
  #?(:clj (.lang x)
     :cljs (.-lang x)))


#?(:cljs
   (deftest test-kw-escape-coverage
      (let [max-char 65535
           ]
       (doseq [c (range 0 max-char)]
         (if (not (contains? fmt/kw-escapes (char c)))
           (is (= true (fmt/kw-test c))))))))


#?(:clj
   (deftest
     ^{:vann/preferredNamespacePrefix "issue21"
       :vann/preferredNamespaceUri "http://rdf.naturallexicon.com/issue21/"
       }
     issue-21-uncouple-voc-from-ns
     (is (=  "http://rdf.naturallexicon.com/issue21/uri-for"
             (v/uri-for :issue21/uri-for)))
     (is (= "issue21:uri-for"
            (v/qname-for :issue21/uri-for)))
     (is (= :issue21/uri-for
            (v/keyword-for "http://rdf.naturallexicon.com/issue21/uri-for")))
     (is (= #{"PREFIX issue21: <http://rdf.naturallexicon.com/issue21/>"}
         (into #{} (v/sparql-prefixes-for
                    "Select * Where{?s issue21:testing ?whatever}"))))
     ))


;; NO READER MACROS BELOW THIS POINT

(deftest attr-map-test
  (testing "Tests the conditions found in attr-map tests"
    (is (= "http://xmlns.com/foaf/0.1/"
           (v/ns-to-namespace (v/cljc-find-ns 'ont-app.vocabulary.foaf))
            ))
    (is (= "foaf"
           (v/ns-to-prefix 'ont-app.vocabulary.foaf)
            ))
    (is (= "http://xmlns.com/foaf/0.1/homepage"
           (v/iri-for :foaf/homepage)
           ))
    (is (= "http://blah"
           (v/iri-for (v/keyword-for "http://blah"))
           ))
    (is (= "http://rdf.naturallexicon.org/ont-app/vocabulary/blah"
           (v/iri-for ::v/blah)
           ))
    (is (= "foaf"
           (v/ns-to-prefix (v/cljc-find-ns 'ont-app.vocabulary.foaf))
           ))
    (is (= "voc:blah"
           (v/qname-for ::v/blah)
           ))
    (is (= "foaf:homepage"
           (v/qname-for :foaf/homepage)
           ))
    ;; 
    (is (= :foaf/homepage
           (v/keyword-for "http://xmlns.com/foaf/0.1/homepage")
           ))
    (is (= :http:%2F%2Fexample.com%2Fmy%2Fstuff
           (v/keyword-for "http://example.com/my/stuff")
           ))
    (is (= :no-prefix-found
           (v/keyword-for (fn [u k] :no-prefix-found)
                          "example.com/my/stuff")
           ))
    (is (= #{"foaf"}
           (v/cljc-find-prefixes (v/prefix-re-str)
                                 "Select * Where{?s foaf:homepage ?homepage}")
           ))
    (is (= {"foaf" #?(:clj (v/cljc-find-ns 'ont-app.vocabulary.foaf)
                      :cljs 'ont-app.vocabulary.foaf)
            }
           (v/collect-prefixes {}
                               #?(:clj (find-ns 'ont-app.vocabulary.foaf)
                                  :cljs 'ont-app.vocabulary.foaf
                                  ))
           ))
    (is (= (list "PREFIX foaf: <http://xmlns.com/foaf/0.1/>")
           (v/sparql-prefixes-for
            "Select * Where{?s foaf:homepage ?homepage}")
            ))
    (is (= "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\nSelect * Where{?s foaf:homepage ?homepage}"
           (v/prepend-prefix-declarations
            "Select * Where{?s foaf:homepage ?homepage}")
           ))
     ))

(deftest encode-and-decode-kw-names
  (testing "keywords should not choke the reader. URI strings  should be parsable as URIs"
    (is (= "+n+123"
           (fmt/encode-kw-name "123")
           ))
    (is (= "123"
           (fmt/decode-kw-name (fmt/encode-kw-name "123"))
           ))
    (is (= "http://xmlns.com/foaf/0.1/123"
           (v/iri-for :foaf/+n+123)
           ))
    (is (= :foaf/+n+123
           (v/keyword-for "http://xmlns.com/foaf/0.1/123")
           ))
    (is (= :foaf/+n+123
           (v/keyword-for "foaf:123")
           ))
    (is (= "foaf:123"
           (v/qname-for (v/keyword-for "foaf:123"))
           ))
    (is (= :foaf/Subtopic%2Fx
           (v/keyword-for "http://xmlns.com/foaf/0.1/Subtopic/x")
           ))
    (is (= "http://xmlns.com/foaf/0.1/Subtopic/x"
           (v/iri-for (v/keyword-for "http://xmlns.com/foaf/0.1/Subtopic/x"))
           ))
    (is (= :foaf/blah%2F
           (v/keyword-for "http://xmlns.com/foaf/0.1/blah/")))
    ))

(deftest maps-to-test
  (testing ":voc/mapsTo ns metadata should resolve prefixes properly"
    ;; note that the local ns  maps to vocabulary.core
    (is (= "http://rdf.naturallexicon.org/ont-app/vocabulary/blah"
           (v/iri-for ::blah)
           ))
    (is (= "voc:blah"
           (v/qname-for ::blah)
           ))
    ))


(deftest language-tagged-strings
  ;; the actual reader macro won't compile with actual #lstr tag
  ;; due to a race condition in compilation which seems to be
  ;; resolved in dependent modules.
  ;; see test in ont-app/igraph-vocabulary to test the actual tag
  (testing "langstr dispatch"
    (let [x (lstr/read-LangStr "asdf@en")]
      (is (= ont_app.vocabulary.lstr.LangStr (type x) ))
      (is (= "asdf"
             (cljc-s x)
             ))
      (is (= "en"
             (cljc-lang x)
             ))
      (is (= "asdf" (str x) ))
      (is (= "en" (lstr/lang x) ))
      (is (= x (lstr/read-LangStr "asdf@en")))
      )))


#?(:clj (def x #lstr "asdf@en"))

(deftest issue-12-language-tagged-strings-in-cljs
  ;; the actual reader macro won't compile with actual #lstr tag
  ;; due to a race condition in compilation which seems to be
  ;; resolved in dependent modules.
  ;; see test in ont-app/igraph-vocabulary to test the actual tag
  (testing "read lstr tag"
    (let [x (read-string "#lstr \"dog@en\"")
          ;; 'We'd like to read this directly in source
          ]
      (is (= ont_app.vocabulary.lstr.LangStr (type x) )))))


(deftest issue-15-lstr-should-accommodate-newlines
  (testing "lstr newlines"
    (let [x (read-string "#lstr \"line1\nline2@en\"")
          ]
      (is (= (str x)
             "line1\nline2")))))



  
