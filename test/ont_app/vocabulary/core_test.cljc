(ns ont-app.vocabulary.core-test
  (:require
   [clojure.spec.alpha :as spec]
   [ont-app.vocabulary.core :as voc :refer [resource-type]]
   [ont-app.vocabulary.lstr :as lstr]
   [ont-app.vocabulary.dstr :as dstr]
   [ont-app.vocabulary.format :as fmt]
   ;; platform-specific...
   #?(:clj [clojure.core :refer [read-string]]
      :cljs [cljs.reader :refer [read-string]])
   #?(:clj [clojure.repl :refer [apropos]])
   #?(:clj [clojure.test :as test :refer :all]
      :cljs [cljs.test :as test :refer-macros [testing is deftest]]
      )
   #?(:clj [clojure.reflect :refer [reflect]])
   ))


(spec/check-asserts true)

(voc/put-ns-meta!
 'ont-app.vocabulary.core-test
 {
  :voc/mapsTo 'ont-app.vocabulary.core ;; <- part of the test
  })

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; FUN WITH READER MACROS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:dynamic *in-cider-cljs-repl*
  "There's a funny bug with reader macros that only happens in cider/cljs. Set this to true to keep the repl from barfing on you."
  false)

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

(defn- cljc-data-s
  [^ont_app.vocabulary.dstr.DatatypeStr x]
  #?(:clj (.s x)
     :cljs (.-s x)))

(defn- cljc-datatype
  [^ont_app.vocabulary.dstr.DatatypeStr x]
  #?(:clj (.datatype x)
     :cljs (.-datatype x)))

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
                    "Select * Where{?s issue21:testing ?whatever}"))))))


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
    (is (= (list "PREFIX foaf: <http://xmlns.com/foaf/0.1/>")
           (voc/sparql-prefixes-for
            "Select * Where{?s foaf:homepage ?homepage}")
            ))
    (is (= "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\nSelect * Where{?s foaf:homepage ?homepage}"
           (voc/prepend-prefix-declarations
            "Select * Where{?s foaf:homepage ?homepage}")
           ))))

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
           (voc/keyword-for "http://xmlns.com/foaf/0.1/blah/")))))

(deftest maps-to-test
  (testing ":voc/mapsTo ns metadata should resolve prefixes properly"
    ;; note that the local ns  maps to vocabulary.core
    (is (= "http://rdf.naturallexicon.org/ont-app/vocabulary/blah"
           (voc/iri-for ::blah)
           ))
    (is (= "voc:blah"
           (voc/qname-for ::blah)
           ))))

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
      (is (= x (lstr/read-LangStr "asdf@en"))))))

(deftest issue-12-language-tagged-strings-in-cljs-source
  (testing "read lstr tag"
    (let [x #voc/lstr "dog@en"
          y (lstr/read-LangStr "dog@en")
          ]
      (is (= (str x) "dog"))
      (is (= (lstr/lang x) "en"))
      (is (= x y))
      (is (= #{x} (into #{} [x y]))) ;; identical hashes
      (is (= ont_app.vocabulary.lstr.LangStr (type x))))))

(deftest issue-15-lstr-should-accommodate-newlines
  (testing "lstr newlines"
    (let [x #voc/lstr "line1\nline2@en"
          ]
      (is (= (str x)
             "line1\nline2")))))

(voc/put-ns-meta! 'example-ns
                  {:vann/preferredNamespacePrefix "eg"
                   :vann/preferredNamespaceUri "http://rdf.example.com/"
                   })

(deftest issue-18-ttl-prefixes
  (let [ttl-string "eg:SomeGuy foaf:homepage eg:SomeWebPage."
        ]
  (is (= (into #{} (voc/turtle-prefixes-for ttl-string))
         #{"@prefix eg: <http://rdf.example.com/>."
           "@prefix foaf: <http://xmlns.com/foaf/0.1/>."}))
  (let [prefixed-ttl (voc/prepend-prefix-declarations
                      voc/turtle-prefixes-for
                      "eg:SomeGuy foaf:homepage eg:SomeWebPage.")]
    (is (re-matches #"(?s).*@prefix eg: <http://rdf.example.com/>.*" prefixed-ttl))
    (is (re-matches #"(?s).*@prefix foaf: <http://xmlns.com/foaf/0.1/>.*" prefixed-ttl))
    )
  ))

(voc/put-ns-meta! 'issue-19-urns-should-be-accommodated
                  {:vann/preferredNamespacePrefix "test-urn"
                   :vann/preferredNamespaceUri "urn:testing:issue:"
                   })
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
         (binding [voc/*exceptional-iri-str-re* #"^(urn:|arn:|urx).*"]
           (voc/uri-for (voc/keyword-for "urx:blah:blah:blah"))))))

(deftest issue-20-validity-of-full-uri-keywords
  (is (= :https:%2F%2Fw3id.org%2Fschematransform%2FExampleShape#BShape
         (voc/keyword-for "https://w3id.org/schematransform/ExampleShape#BShape"))))

(defrecord Employee [name eid])

(defmethod resource-type [::voc/resource-type-context Employee]
  [_] ::employee-urn)

(defmethod voc/as-uri-string ::employee-urn
  [this]
  (str "urn:acme:employee:" (:eid this)))

(derive ::employee-urn :voc/KwiInferredFromUriString)

(voc/put-ns-meta! 'resource-protocol-and-methods
                  {:vann/preferredNamespacePrefix "tmp"
                   :vann/preferredNamespaceUri "file://tmp/"
                   :dc/description "Used to test issue 26."
                   })

(deftest issue-26-resource-protocol-and-methods
  (is (= "http://www.w3.org/2000/01/rdf-schema#comment"
         (voc/as-uri-string :rdfs/comment)))
  (let [e (->Employee "George" 42)]
    (is (= ::employee-urn (voc/resource-type e)))
    (is (= "urn:acme:employee:42" (voc/as-uri-string e)))
    (is (= :urn:acme:employee:42 (voc/as-kwi e)))
    (is (= "<urn:acme:employee:42>" (voc/as-qname e))))
  #?(:clj
     (let [f (clojure.java.io/file "/tmp/test-resource-protocol.txt")
           ]
       (is (= "file://tmp/test-resource-protocol.txt" (voc/as-uri-string f)))
       (is (= :tmp/test-resource-protocol.txt (voc/as-kwi f)))
       (is (= "tmp:test-resource-protocol.txt" (voc/as-qname f))))))

(deftest issue-27-backslashes-in-qnames
  (let [uri-string "http://www.w3.org/2000/01/rdf-schema#blah/blah"
        kwi (voc/as-kwi uri-string)
        qname (voc/as-qname uri-string)
        ]
    (is (= :rdfs/blah%2Fblah kwi))
    (is (= "rdfs:blah\\/blah" (voc/as-qname uri-string)))
    (is (= uri-string (voc/as-uri-string (voc/as-qname uri-string))))))


(deftest issue-29-resource=
  (is (voc/resource=  "rdfs:subClassOf"
                      :rdfs/subClassOf))

  (is (not (voc/resource= "rdfs:subClassOf"
                          :rdfs/subPropertyOf))))

(deftest issue-31-invalid-keywords-when-only-prefix-is-provided
  (is (= :http:%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema#
         (voc/as-kwi "rdfs:")))
  (is (spec/valid? :voc/kwi-spec (voc/as-kwi "rdfs:")))
  (is (= (voc/resource-type (voc/as-kwi "rdfs:"))
         :voc/Kwi)))

(defn tag-round-trip
  "True when (tag (untag val)) == val"
  [val]
  (= (voc/untag (voc/tag val)) val))

(deftest datatype-tagged-strings
  (testing "datatype str dispatch"
    (let [x (dstr/read-DatatypeStr "1000^^unit:Meter")]
      (is (= ont_app.vocabulary.dstr.DatatypeStr (type x) ))
      (is (= "1000"
             (cljc-data-s x)
             ))
      (is (= "unit:Meter"
             (cljc-datatype x)
             ))
      (is (= "1000" (str x) ))
      (is (= "unit:Meter" (dstr/datatype x) ))
      (is (= x (dstr/read-DatatypeStr "1000^^unit:Meter")))))

  (testing "tag/untag"
    ;;(is (= true (voc/untag (voc/tag true))))
    (is (tag-round-trip true))
    (is (tag-round-trip true))
    (is (tag-round-trip 1))
    (is (tag-round-trip (short 1)))
    (is (tag-round-trip "yowsa"))
    (is (tag-round-trip 0.0))
    (is (tag-round-trip (float 0.0)))
    (is (tag-round-trip (byte 0)))
    (is (tag-round-trip #inst "2000"))
    (is (= (dstr/->DatatypeStr (str 1) (voc/as-qname :unit/Meter))
           (voc/tag 1 :unit/Meter)))
    (when (not *in-cider-cljs-repl*)
      (is (= (voc/untag (voc/tag 1 :unit/Meter) identity)
             #voc/dstr "1^^unit:Meter")))
    ))

(deftest test-register-resource-types
  (let [original-resource-types @voc/resource-types]
    (try
      (do
        ;; Declare a new context to supersede the default...
        (voc/register-resource-type-context! ::test-context-1
                                             ::voc/resource-type-context)
        ;; This should result in a new most-specific context set (a singleton)
        (is (= (::voc/most-specific-context #{::test-context-1})))
        ;; ... which is now the operative context...
        (let [context-fn (-> @voc/resource-types ::voc/context-fn)]
          (is (= (context-fn) ::test-context-1)))

        ;; Registering a competing lineage...
        (voc/register-resource-type-context! ::test-context-2
                                             ::voc/resource-type-context)

        ;; ... introduces an ambiguity...
        (is (= (::voc/most-specific-context #{::test-context-1 ::test-context-2})))

        ;; ... which is an error by default ...
        (is (thrown-with-msg?
             #?(:clj Exception :cljs js/Error)
             #"Ambiguous resource type context.*"
             (voc/resource-type "blah")))

        ;; ... but we can fix it ...
        (swap! voc/resource-types #(-> % (assoc ::voc/on-ambiguity-fn
                                                (fn [_] ::test-context-1))))
        (let [context-fn (-> @voc/resource-types ::voc/context-fn)]
          (is (= (context-fn) ::test-context-1))))

    (finally
      (reset! voc/resource-types original-resource-types)))))

(comment
(defn describe-api ;; todo: move this into a utilities lib
  "Returns [`member`, ...] for `obj`, for public members of `obj`, sorted by :name,  possibly filtering on `name-re`
  - Where
    - `obj` is an object subject to reflection
    - `name-re` is a regular expression to match against (:name `member`)
    - `member` := m, s.t. (keys m) = #{:name, :parameter-types, :return-type}
  "
  ([obj]
   (let [collect-public-member (fn [acc member]
                                (if (not
                                     (empty?
                                      (clojure.set/intersection #{:public}
                                                                (:flags member))))
                                  (conj acc (select-keys member
                                                         [:name
                                                          :parameter-types
                                                          :return-type]))
                                  ;;else member is not public
                                  acc))]
     (sort (fn compare-names [this that] (compare (:name this) (:name that)))
           (reduce collect-public-member [] (:members (reflect obj))))))
  ([obj name-re]
   (filter (fn [member]
             (re-matches name-re (str (:name member))))
           (describe-api obj))))
);; end comment

;;;;;;;;;;;;;
;; BONE-YARD
;;;;;;;;;;;;;

;; Made moot by issue 34
;; #?(:clj
;;    (deftest
;;      ^{`voc/resource-type (fn [_] :issue-30)} ;; metadata is bound to the test name won't work under cljs
;;      clj-issue-30-extend-via-metadata
;;      (is (= (voc/resource-type #'clj-issue-30-extend-via-metadata)
;;             :issue-30))))

;; Made moot by issue 34
;; (deftest issue-30-extend-via-metadata
;;   ;; see also clj-issue-30-extend-via-metadata which binds to a var (clj only)
;;   (let [my-thing ^{`voc/resource-type (fn [_] :issue-30)} {:name :issue-30-test}
;;         ]
;;     (is (= :issue-30
;;            (voc/resource-type my-thing)))))

