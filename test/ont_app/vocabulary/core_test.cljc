(ns ont-app.vocabulary.core-test
  (:require
   #?(:clj [clojure.test :as test :refer :all]
      :cljs [cljs.test :as test :refer-macros [testing is deftest]]
      )
   #?(:clj [clojure.core :refer [read-string]]
      :cljs [cljs.reader :refer [read-string]])
   #?(:clj [ont-app.vocabulary.core :as voc :refer :all]
      :cljs [ont-app.vocabulary.core :as voc]
      )
   [ont-app.vocabulary.lstr :as lstr]
   [ont-app.vocabulary.format :as fmt]
   )
  )


(voc/put-ns-meta!
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
      (is (= (get-sym (voc/cljc-find-ns 'ont-app.vocabulary.core))
             'ont-app.vocabulary.core)))))

(def cljc-error
  #?(:clj Exception
     :cljs js/Error))

(defn- cljc-s
  [^ont_app.vocabulary.lstr.LangStr x]
  #?(:clj (.s x)
     :cljs (.-s x)))

(defn- cljc-lang
  [^ont_app.vocabulary.lstr.LangStr x]
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
             (voc/uri-for :issue21/uri-for)))
     (is (= "issue21:uri-for"
            (voc/qname-for :issue21/uri-for)))
     (is (= :issue21/uri-for
            (voc/keyword-for "http://rdf.naturallexicon.com/issue21/uri-for")))
     (is (= #{"PREFIX issue21: <http://rdf.naturallexicon.com/issue21/>"}
         (into #{} (voc/sparql-prefixes-for
                    "Select * Where{?s issue21:testing ?whatever}"))))
     ))


;; NO READER MACROS BELOW THIS POINT

(deftest attr-map-test
  (testing "Tests the conditions found in attr-map tests"
    (is (= "http://xmlns.com/foaf/0.1/"
           (voc/ns-to-namespace (voc/cljc-find-ns 'ont-app.vocabulary.foaf))
            ))
    (is (= "foaf"
           (voc/ns-to-prefix 'ont-app.vocabulary.foaf)
            ))
    (is (= "http://xmlns.com/foaf/0.1/homepage"
           (voc/iri-for :foaf/homepage)
           ))
    (is (= "http://blah"
           (voc/iri-for (voc/keyword-for "http://blah"))
           ))
    (is (= "http://rdf.naturallexicon.org/ont-app/vocabulary/blah"
           (voc/iri-for ::voc/blah)
           ))
    (is (= "foaf"
           (voc/ns-to-prefix (voc/cljc-find-ns 'ont-app.vocabulary.foaf))
           ))
    (is (= "voc:blah"
           (voc/qname-for ::voc/blah)
           ))
    (is (= "foaf:homepage"
           (voc/qname-for :foaf/homepage)
           ))
    ;; 
    (is (= :foaf/homepage
           (voc/keyword-for "http://xmlns.com/foaf/0.1/homepage")
           ))
    (is (= :http:%2F%2Fexample.com%2Fmy%2Fstuff
           (voc/keyword-for "http://example.com/my/stuff")
           ))
    (is (= :no-prefix-found
           (voc/keyword-for (fn [u k] :no-prefix-found)
                          "example.com/my/stuff")
           ))
    (is (= #{"foaf"}
           (voc/cljc-find-prefixes (voc/prefix-re-str)
                                 "Select * Where{?s foaf:homepage ?homepage}")
           ))
    (is (= {"foaf" #?(:clj (voc/cljc-find-ns 'ont-app.vocabulary.foaf)
                      :cljs 'ont-app.vocabulary.foaf)
            }
           (voc/collect-prefixes {}
                               #?(:clj (find-ns 'ont-app.vocabulary.foaf)
                                  :cljs 'ont-app.vocabulary.foaf
                                  ))
           ))
    (is (= (list "PREFIX foaf: <http://xmlns.com/foaf/0.1/>")
           (voc/sparql-prefixes-for
            "Select * Where{?s foaf:homepage ?homepage}")
            ))
    (is (= "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\nSelect * Where{?s foaf:homepage ?homepage}"
           (voc/prepend-prefix-declarations
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
           (voc/iri-for :foaf/+n+123)
           ))
    (is (= :foaf/+n+123
           (voc/keyword-for "http://xmlns.com/foaf/0.1/123")
           ))
    (is (= :foaf/+n+123
           (voc/keyword-for "foaf:123")
           ))
    (is (= "foaf:123"
           (voc/qname-for (voc/keyword-for "foaf:123"))
           ))
    (is (= :foaf/Subtopic%2Fx
           (voc/keyword-for "http://xmlns.com/foaf/0.1/Subtopic/x")
           ))
    (is (= "http://xmlns.com/foaf/0.1/Subtopic/x"
           (voc/iri-for (voc/keyword-for "http://xmlns.com/foaf/0.1/Subtopic/x"))
           ))
    (is (= :foaf/blah%2F
           (voc/keyword-for "http://xmlns.com/foaf/0.1/blah/")))
    ))

(deftest maps-to-test
  (testing ":voc/mapsTo ns metadata should resolve prefixes properly"
    ;; note that the local ns  maps to vocabulary.core
    (is (= "http://rdf.naturallexicon.org/ont-app/vocabulary/blah"
           (voc/iri-for ::blah)
           ))
    (is (= "voc:blah"
           (voc/qname-for ::blah)
           ))
    ))


(deftest language-tagged-strings
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

(deftest issue-12-language-tagged-strings-in-cljs-source
  (testing "read lstr tag"
    (let [x #voc/lstr "dog@en"
          y (lstr/read-LangStr "dog@en")
          ]
      (is (= (str x) "dog"))
      (is (= (lstr/lang x) "en"))
      (is (= x y))
      (is (= #{x} (into #{} [x y]))) ;; identical hashes
      (is (= ont_app.vocabulary.lstr.LangStr (type x)))
      )))

(deftest issue-15-lstr-should-accommodate-newlines
  (testing "lstr newlines"
    (let [x #voc/lstr "line1\nline2@en"
          ]
      (is (= (str x)
             "line1\nline2")))))


(voc/put-ns-meta! 'issue-19-urns-should-be-accommodated
                  {:vann/preferredNamespacePrefix "test-urn"
                   :vann/preferredNamespaceUri "urn:testing:issue:"
                   }
                  )
(deftest
  issue-19-urns-should-be-accommodated
  (is (= "test-urn:19"
         (voc/qname-for (voc/keyword-for "urn:testing:issue:19"))))
  (is (= :test-urn/+n+19
         (voc/keyword-for "test-urn:19")))
  (is (= :test-urn/+n+19
         (voc/keyword-for "urn:testing:issue:19")))
  (is (= "urn:blah:blah:blah"
         (voc/uri-for (voc/keyword-for "urn:blah:blah:blah"))))
  (is (thrown-with-msg?
       #?(:clj Exception :cljs js/Error)
       #"Could not find IRI for :urx:blah:blah:blah"
       (voc/uri-for (voc/keyword-for "urx:blah:blah:blah"))))
  (is (= "urx:blah:blah:blah"
         (binding [voc/exceptional-iri-str-re #"^(urn:|arn:|urx).*"]
           (voc/uri-for (voc/keyword-for "urx:blah:blah:blah")))))

  )

(deftest issue-20-validity-of-full-uri-keywords
  (is (= :https:%2F%2Fw3id.org%2Fschematransform%2FExampleShape#BShape
         (voc/keyword-for "https://w3id.org/schematransform/ExampleShape#BShape")))
  )





