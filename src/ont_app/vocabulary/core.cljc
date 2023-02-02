(ns ont-app.vocabulary.core
  (:require
   [clojure.string :as s]
   [ont-app.vocabulary.format :as fmt
    :refer
    [decode-kw-ns
     decode-kw-name
     decode-uri-string
     encode-kw-name
     encode-uri-string
     ]]
   [ont-app.vocabulary.lstr :as lstr]
   ;;#?(:cljs [cljs.reader :as edn])

))

(def ^:private prefix-to-ns-cache (atom nil))
(def ^:private namespace-to-ns-cache (atom nil))
(def ^:private prefix-re-str-cache (atom nil))

(defn clear-caches! 
  "Side-effects: resets all caches in voc/ to nil
NOTE: call this when you may have imported new namespace metadata
"
  []
  (reset! prefix-re-str-cache nil)
  (reset! namespace-to-ns-cache nil)
  (reset! prefix-to-ns-cache nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; FUN WITH READER MACROS
;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (cljs.reader/register-tag-parser! "lstr" lstr/read-LangStr)
   )

#?(:cljs (enable-console-print!))

;; namespace metadata isn't available at runtime in cljs...
#?(:cljs
   (def cljs-ns-metadata
     "Namespaces in cljs are not proper objects, and there is no metadata
  available at runtime. This atom stores 'pseudo-metadata' declared with
  cljc-put-ns-metadata and accessed with cljc-get-metadata. Clj just uses
  the metadata regime for its ns's"
     (atom {})))

(defn put-ns-meta!
  "Side-effect: ensures that subsequent calls to (cljc-get-ns-meta `_ns` return `m`
  Where
  - `_ns`  is an ns(clj only) or the name of a namespace, possibly declared for the sole purpose of holding vocabulary metadata (e.g. rdf, foaf, etc)
  - `m` := {`key` `value`, ...}, metadata (clj) or 'pseudo-metadata' (cljs)
  - `key` is a keyword containing vocabulary metadata, e.g.
    `::vann/preferredNamespacePrefix`
  NOTE: In cljs, ns's are not available at runtime, so the metadata is stored
    in an atom called 'voc/cljs-ns-metadata'
  See also declarations for ont-app.vocabulary.rdf, ont-app.vocabulary.foaf, etc.
  for examples of namespaces declared solely to hold vocabulary metadata.
  "
  ([_ns m]
   #?(:cljs
      (swap! cljs-ns-metadata
             assoc _ns m)
      :clj
      (alter-meta!
       (if (symbol? _ns)
         (if-let [found (find-ns _ns)]
           found
           ;; else no proper namespace found
           (or (try (eval `(var ~_ns))
                    (catch Exception _))
               (create-ns _ns)))
                             
         ;; else not a symbol
         (let []
           (assert (= (type (find-ns 'user)) clojure.lang.Namespace))
           _ns))
       merge m))
   (clear-caches!))

  ([m]
   #?(:cljs (put-ns-meta! (namespace ::dummy)) 
      :clj (put-ns-meta! *ns* m))))

(defn get-ns-meta
  "Returns `metadata` assigned to ns named `_ns`
  Where
  - `_ns` names a namespace or a 'dummy' namespace whose sole purpose is to hold metadata.
  - `metadata` := {`key` `value`, ...}
  - `key` is a keyword containing vocabulary metadata, e.g. :vann/preferredNamespacePrefix
  "
  ([_ns]
   #?(:cljs
      (do
        (assert (symbol? _ns))
        (get @cljs-ns-metadata _ns))
      :clj
      (if (symbol? _ns)
        (if-let [it (find-ns _ns)]
          (meta it))
         ;; else not a symbol
        (meta _ns))))

  ([]
   #?(:cljs (throw (ex-info "Cannot infer namespace at runtime in cljs"
                            {:type ::CannotInferNamespace}))
      :clj (get-ns-meta *ns*))))

#?(:cljs
   (def ^:dynamic *alias-map*
     "{`alias` `ns-name`, ...}
  Where
  `alias` is a symbol
  `ns-name` is a symbol naming an ns in the current lexical env
  NOTE: Informs cljc-ns-aliases in cljs space.
  "
     {}))

(defn cljc-ns-aliases 
  "Returns {`alias` `ns`, ...}
Where
  - `alias` is a symbol
  - `ns` is its associated ns in the current lexical environment.
NOTE: cljs will require explicit maintenance of *alias-map*
This is really only necessary if you're importing a package
as some symbol other than the preferred prefix.
"
  []
  #?(:clj (ns-aliases *ns*)
     :cljs *alias-map*))

(defn cljc-find-ns 
  "Returns `ns-name-or-obj` for `_ns`, or nil.
Where 
  - `ns-name-or-obj` may either be a namespace (in clj) 
    or the name of a namespace (in cljs)
  - `_ns` is a symbol which may name a namespace.
NOTE: Implementations involving cljs must use cljs-put/get-ns-meta to declare
  ns metadata.
"
  [_ns]
  #?(:clj (find-ns _ns)
     :cljs (if (contains? @cljs-ns-metadata _ns)
             _ns)
     ))

(defn cljc-all-ns 
  "Returns (`ns-name-or-obj` ...)
Where
  - `ns-name-or-obj` may either be a namespace (in clj) 
     or the name of a namespace (in cljs)
"
  []
  #?(:clj (all-ns)
     :cljs (keys @cljs-ns-metadata)))

(declare prefix-re-str)
(defn cljc-find-prefixes 
  "Returns #{`prefix`...} for `s`
Where
  - `prefix` is a prefix found in `s`, for which some (meta ns) has a 
     :vann/preferredNamespacePrefix declaration
  - `s` is a string, typically a SPARQL query body for which we want to 
    infer prefix declarations.
"
  [re-str s]
  {:pre [(string? re-str)
         (string? s)]
   }
  #?(:clj
     (let [prefixes (re-matcher (re-pattern re-str) s) 
           ]
       (loop [acc #{}
              next-match (re-find prefixes)]
         (if (not next-match)
           acc
           (let [[_ prefix] next-match]
             (recur (conj acc prefix)
                    (re-find prefixes))))))
     :cljs
     (let [prefix-re (re-pattern (str "^(" re-str ")(.*)"))
           ;; ^(<spaces>(<prefix1>|<prefix2>|...):)(<unparsed>) 
           ]
       (loop [acc #{}
              input s]
         (assert (string? input))
         (if (or (not input) (empty? input))
           acc
           (let [next-match (re-matches prefix-re input)]
             (if next-match
               (let [[_ _ prefix unparsed] next-match]
                 (recur (conj acc prefix)
                        unparsed))
               ;; else there's no match
               ;; TODO: make this less ugly
               ;; Should be OK for shortish strings like SPARQL queries
               ;; for now
               (recur acc (subs input 1)))))))
     ))

(def cljc-ns-map
  "mimics behavior of `ns-map` on cljs, but returns empty symbol->binding map"
  #?(:clj ns-map
     :cljs (fn [not-a-real-ns] {})))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;;; NO READER MACROS BEYOND THIS POINT
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; ;; SCHEMA

(put-ns-meta!
 'ont-app.vocabulary.core
 {:doc "Defines utilities and a set of namespaces for commonly used linked data 
constructs, metadata of which specifies RDF namespaces, prefixes and other 
details."
  :vann/preferredNamespacePrefix "voc"
  :vann/preferredNamespaceUri
  "http://rdf.naturallexicon.org/ont-app/vocabulary/"
  })

(def terms
  "Describes vocabulary for this namespace. May be read into an IGraph downstream."
  ^{:triples-format :vector-of-vectors}
  [[:voc/appendix
    :rdf/type :rdf:Property
    :rdfs/comment "<ns> :voc/appendix <triples>
 Asserts that <triples> describe a graph that elaborates on other attributes 
asserted in usual key-value metadata asserted for <ns>, e.g. asserting a 
dcat:mediaType relation for some dcat:downloadURL.
 "
    ]
   ])

;; ;; FUNCTIONS
(defn on-duplicate-prefix
  "Throws an error if a prefix is bound to more than one namespace.
  Reduces into `prefix` argument
  NOTE: Can be overridden with `with-redefs`."
  [prefixes prefix _ns]
  (throw (ex-info (str "Prefix `" prefix "` is being associated with both " (prefixes prefix) " and " _ns)
                  {:type ::DuplicatePrefix
                   :prefixes prefixes
                   :prefix prefix
                   :ns _ns})))

(defn vann-annotated-objects 
  "Returns `[obj, ...]
Where:
  - `obj` bears metadata s.t. (get-ns-meta obj)  includes :vann/... annotations
    - these may be either namespaces or vars
"
  []
  (let [has-vann-declarations? (fn [obj]
                                 (if-let [m (get-ns-meta obj)]
                                   (and (:vann/preferredNamespacePrefix m)
                                        (:vann/preferredNamespaceUri m))))
        ]
    (filter has-vann-declarations?
            (reduce conj
                    ;; namespaces...
                    (cljc-all-ns)
                    ;; vars ...
                    (mapcat (comp vals cljc-ns-map) (cljc-all-ns))))))

(defn collect-prefixes 
  "Returns {`prefix` `namespace` ...} s.t. `next-ns` is included
Where
  - `prefix` is a prefix declared in the metadata of `next-ns`
  - `namespace` is a URI namespace declared for `prefix` in metadata of `next-ns`
  - `next-ns` is typically an element in a reduction sequence of ns's 
"
  [acc next-ns]
  {:pre [(map? acc)]
   }
  (let [nsm (get-ns-meta next-ns)
        add-prefix (fn [acc prefix]
                     (if (acc prefix)
                       (on-duplicate-prefix acc prefix next-ns)
                       (assoc acc prefix next-ns)))
        ]
    (if-let [p (:vann/preferredNamespacePrefix nsm)]
      (if (set? p)
        (reduce add-prefix acc p)
        (add-prefix acc p))
      acc)))

(defn prefix-to-ns 
  "Returns {`prefix` `ns` ...}
Where 
  - `prefix` is declared in metadata for some `ns` with 
  :vann/preferredNamespacePrefix 
  - `ns` is an instance of clojure.lang.ns available within the lexical 
  context in which the  call was made.
"
  []
  (when-not @prefix-to-ns-cache
    (reset! prefix-to-ns-cache
            (reduce collect-prefixes
                    {}
                    (vann-annotated-objects))))
  @prefix-to-ns-cache)

(defn ns-to-namespace 
  "Returns `iri` for `ns`
Where
  - `iri` is an iri declared with :vann/preferredNamespaceUri in the metadata for 
    `ns`, or nil
  - `ns` is an instance of clojure.lang.Namespace (in clj) or a symbol-name for ns (cljs)
"
  [_ns]
  (or
   (:vann/preferredNamespaceUri (get-ns-meta _ns))
   (-> _ns
       (get-ns-meta)
       :voc/mapsTo
       (get-ns-meta)
       :vann/preferredNamespaceUri)))

(defn namespace-to-ns 
  "returns {`namespace` `ns` ...} for each `ns` with :vann/preferredNamespaceUri
  declaration
  Where
  - `namespace` is the URI suitable for for an RDF prefix declaration
  - `ns` is either a clojure ns (clj) or a symbol naming a clojure ns (cljs).
"
  []
  (when-not @namespace-to-ns-cache
    (let [collect-mapping (fn [acc _ns]
                            (if-let [namespace (:vann/preferredNamespaceUri
                                                (get-ns-meta _ns))
                                     ]
                              (assoc acc namespace _ns)
                              ;;else
                              acc))
          
          ]
      (reset! namespace-to-ns-cache
              (reduce collect-mapping {} (vann-annotated-objects)))))
  @namespace-to-ns-cache)

(defn prefixed-ns 
  "Returns nil or the ns whose `prefix` was declared in metadata with
  :vann/preferredNamespacePrefix
Where
  - `prefix` is a string, typically parsed from a keyword.
"
  [prefix]
  {:pre [(string? prefix)]
   }
  (or (get (prefix-to-ns) prefix)
      (get (cljc-ns-aliases) (symbol prefix))
      ))

(def ^:dynamic ordinary-iri-str-re
  "An regex matching a standard IRI string like http://"
  #"^(http:|https:|file:).*"
  )
  
(def ^:dynamic exceptional-iri-str-re
  "A regex matching an IRI string which doesn't match the usual http//-ish scheme, such as urn:"
  #"^(urn:|arn:).*"
  )

(defn kwi-missing-namespace-if-not-urn-or-arn
  "Returns the name-stiring of `kw`, or throws ::NoIRIForKw if `kw` is incorrectly missing a namespace"
  [kw]
  {:pre [(keyword? kw)
         (empty? (namespace kw))
         ]
   }
  (if (re-matches exceptional-iri-str-re (name kw))
    (name kw)
    (throw (ex-info (str "Could not find IRI for " kw)
                  {:type ::NoIRIForKw
                   ::kw kw
                   }))))

(defn default-on-no-kwi-ns
  "Returns the name-string of `kw` if its name string is a typical URI or URN, otherwise throws a :NoIRIForKw error
  Where
  - `kw` is a keyword with no namespace.
"
  [kw]
  {:pre [(keyword? kw)
         (empty? (namespace kw))
         ]
   }
  (let [kw-name (name kw)
        ]
    (if (or (re-matches ordinary-iri-str-re kw-name)
            (re-matches exceptional-iri-str-re kw-name))
      (-> kw-name
          decode-kw-name
          encode-uri-string)
      (throw (ex-info (str "Could not find IRI for " kw)
                      {:type ::NoIRIForKw
                       ::kw kw
                       }))
      )))
  
(defn uri-for
  "Returns `iri`  for `kw` based on metadata attached to `ns` Alias of `iri-for` or `on-no-prefix (kw) if the keyword is not namespaced.
Where
  - `iri` is of the form `namespace``value`
  - `kw` is a keyword of the form `prefix`:`value`
  - `ns` is an instance of clojure.lang.ns
  - `on-no-kwi-ns` := fn [kw] -> uri, for cases where `kw` is not namespaced
    default is `default-on-no-kwi-ns`
  - `prefix` is declared with :vann/preferredNamespacePrefix in metadata of `ns`
  - `namespace` is typically of the form http://...., declared with 
    `:vann/preferredNamespaceUri` in metadata of `ns`
"
  ([kw]
   (uri-for default-on-no-kwi-ns kw)
   )
  ([on-no-kwi-ns kw]
   {:pre [(keyword? kw)]
    }
  (let [prefix (namespace kw)
        kw-name (name kw)
        ]
    (if prefix
      (if (#{"http:" "https:" "file:"} (decode-kw-ns prefix))
        ;; this doesn't happen much anymore. Retained for back-compatibility
        (str (-> prefix decode-kw-name encode-uri-string)
             "/"
             (-> kw-name decode-kw-name encode-uri-string))
      ;; else not a standard scheme....
        (let [_ns (or (cljc-find-ns (symbol prefix))
                      (prefixed-ns prefix))]
          (if-not _ns
            (throw (ex-info (str "No URI declared for prefix '" prefix "'")
                            {:type ::NoUriDeclaredForPrefix
                             ::kw kw
                             ::prefix prefix
                             }))
            
            (str (-> _ns (ns-to-namespace))
                 (-> kw-name decode-kw-name encode-uri-string)))))
      ;; else no prefix
      (on-no-kwi-ns kw)))))

(def iri-for "Alias of uri-for" uri-for)

(defn ns-to-prefix 
  "Returns the prefix associated with `_ns`
Where
  - `_ns` is a clojure namespace, which may have :vann/preferredNamespacePrefix
  declaration in its metadata.   
"
  [_ns]
  (or 
   (:vann/preferredNamespacePrefix (get-ns-meta _ns))
   (-> _ns
       (get-ns-meta)
       :voc/mapsTo
       (get-ns-meta)
       :vann/preferredNamespacePrefix)
   (str (ns-name _ns))))

(defn prefix-to-namespace-uri
  "returns `namespace` URI associated with `prefix`
  Where:
  - `namespace` is a string declared for some `ns` with vann/preferredNamespaceUri
  - `prefix` is a string declared for `ns` with vann/preferredNamespacePrefix
  "
  [prefix]
  (->> prefix
       (get (prefix-to-ns))
       (ns-to-namespace)))

(defn namespace-re 
  "Returns a regex to recognize substrings matching a URI for an ns 
  declared with LOD metadata. Groups for namespace and value.
"
  []
  (let [namespace< (fn [a b] ;; match longer first
                     (> (count a)
                        (count b)))
        ]
    (re-pattern (str "^("
                     (s/join "|" (sort namespace<
                                       (keys (namespace-to-ns))))
                     ")(.*)"))))

(def invalid-qname-name
  "fn [qname-name] -> truthy if `qname-name` is not valid
  Where
  - `qname-name` is a string"
  (partial re-find #"/" ))

(defn qname-for 
  "Returns the 'qname' URI for `kw`, or <...>'d full URI if no valid qname
  could be found. Throws an error if the prefix is specified, but can't be
  mapped to metadata.
Where
  - `kw` is a keyword, in a namespace with LOD declarations in its metadata.
"
  [kw]
  {:pre [(keyword? kw)
         ]
   }
  (let [prefix (namespace kw)
        kw-name (name kw)]
    (if (#{"https:" "http:" "file:"
           "https%3A" "http%3A" "file%3A"
           } prefix)
      ;; scheme was parsed as namespace of kw
      (let [uri-str (str (-> prefix decode-kw-name)
                         "/"
                         (-> kw-name decode-kw-name encode-uri-string))]
        (if-let [rem (re-matches (namespace-re) uri-str)]
          (let [[_ namespace-uri kw-name] rem]
            (if (not (invalid-qname-name kw-name))
              (str
               (->> namespace-uri
                    (get (namespace-to-ns))
                    (ns-to-prefix))
               ":"
               kw-name)))
          ;;else no namespace match
          (str "<" uri-str ">")))
      ;; else this is not scheme-based URI
      (if prefix
        (let [_ns (or (cljc-find-ns (symbol prefix))
                      (prefixed-ns prefix))
              ]
          (when-not _ns
            (throw (ex-info (str "Could not resolve prefix " prefix)
                            {:type ::CouldNotResolvePrefix
                             ::kw kw
                             ::prefix prefix
                             })))
          (if (invalid-qname-name kw-name) ;; invalid as qname
            (str "<" (prefix-to-namespace-uri prefix) kw-name ">")
            ;;else valid as qname
            (str (ns-to-prefix _ns)
                 ":"
                 (-> kw-name decode-kw-name encode-uri-string))))
        ;; else no namespace in keyword
        (str "<" (-> kw-name decode-kw-name encode-uri-string) ">")))))

(defn prefix-re-str 
  "Returns a regex string that recognizes prefixes declared in ns metadata with 
  `:vann/preferredNamespacePrefix` keys. 
NOTE: this is a string because the actual re-pattern will differ per clj/cljs.
"
  []
  (when-not @prefix-re-str-cache
    (reset! prefix-re-str-cache
            (str "\\b(" ;; word boundary
                 (s/join "|" (keys (prefix-to-ns)))
                 "):")))
  @prefix-re-str-cache)

(defn default-on-no-ns
  "Returns the kwi normally appropriate for `_keyword` in cases where no ns can be matched, as is the case with say http://.....
  "
  [_uri _keyword]
  (if (keyword? _keyword)
            _keyword
            (keyword (str _keyword))))

(defn keyword-for 
  "Returns a keyword equivalent of `uri`, properly prefixed if Vann declarations
  exist in some ns in the current lexical environment.
  Side effects per `on-no-ns`
  Where
  - `uri` is a string representing a URI
  - `on-no-ns` (optional) := fn [uri kw] -> kwi',
     possibly with side-effects in response to the fact that no qname was found for
     `uri` (default returns `kw`)
  NOTE: typically `on-no-ns` would log a warning or make an assertion.
  "
  ([uri]
   (keyword-for default-on-no-ns uri))
  ([on-no-ns uri]
   {:pre [(string? uri)]
    }
   #dbg
   (if-let [prefix-re-match  (re-matches
                              (re-pattern
                               (str (prefix-re-str) "(.*)"))
                              uri)]
     ;; ... this is a qname...
     (let [[_ prefix _name] prefix-re-match]
       (keyword prefix (-> _name decode-uri-string encode-kw-name)))
     ;;else this isn't a qname. Maybe it's a full URI we have a prefix for...
     ;; namespace re match returns [s namepace value] or nil
     (let [namespace-re-match  (re-matches (namespace-re) uri)
           _namespace (and namespace-re-match (namespace-re-match 1))
           _value (and namespace-re-match (namespace-re-match 2))
           ]
       (if (empty? _value)
         ;; there's nothing but prefix
         (on-no-ns uri (-> uri
                           decode-uri-string
                           encode-kw-name
                           ))
         ;; else there's a match to the namespace regex
         (if (not _namespace)
           (on-no-ns uri (keyword (decode-uri-string _value)))
           ;; we found a namespace for which we have a prefix...
           (keyword (-> _namespace
                        ((namespace-to-ns))
                        (ns-to-prefix))
                    (-> _value decode-uri-string encode-kw-name)
                    )))))))

(defn sparql-prefixes-for 
  "Returns [`prefix-string`...] for each prefix identified in `sparql-string`
Where
  - `prefix-string` := PREFIX `prefix`: `namespace`\n
  - `prefix` is a prefix defined for `namespace` in metadata of some ns with 
     `:vann/preferredNamespacePrefix`
  - `namespace` is a namespace defined in the metadata for some ns with 
    `:vann/preferredNamespaceUri`
"
  [sparql-string]
  (let [sparql-prefix-for (fn [prefix]
                            (str "PREFIX "
                                 prefix
                                 ": <"
                                 (prefix-to-namespace-uri prefix)
                                 ">"))
        ]
    (map sparql-prefix-for (cljc-find-prefixes (prefix-re-str)
                                               sparql-string))))

(defn prepend-prefix-declarations 
  "Returns `sparql-string`, prepended with appropriate PREFIX decls.
"
  [sparql-string]
  (s/join "\n" (conj (vec (sparql-prefixes-for sparql-string))
                     sparql-string)))

;; ;;; NAMESPACE DECLARATIONS
;; ;;; These are commonly used RDF namespaces.

(put-ns-meta!
 'clojure.core
 {
  :vann/preferredNamespacePrefix "clj"
  :vann/preferredNamespaceUri "http://rdf.naturallexicon.org/clojure/ont#"
  })

(put-ns-meta!
 'ont-app.vocabulary.rdf
 {
  :rdfs/comment "The core rdf vocabulary"
  :vann/preferredNamespacePrefix "rdf"
  :vann/preferredNamespaceUri "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  })

(put-ns-meta!
 'ont-app.vocabulary.rdfs
 {
     :dc/title "The RDF Schema vocabulary (RDFS)"
     :vann/preferredNamespaceUri "http://www.w3.org/2000/01/rdf-schema#"
     :vann/preferredNamespacePrefix "rdfs"
     :foaf/homepage "https://www.w3.org/TR/rdf-schema/"
     :dcat/downloadURL "http://www.w3.org/2000/01/rdf-schema#"
     :voc/appendix [["http://www.w3.org/2000/01/rdf-schema#"
                     :dcat/mediaType "text/turtle"]]
  })

(put-ns-meta!
 'ont-app.vocabulary.owl  
    {
     :dc/title "The OWL 2 Schema vocabulary (OWL 2)"
     :dc/description
     "This ontology partially describes the built-in classes and
  properties that together form the basis of the RDF/XML syntax of OWL
  2.  The content of this ontology is based on Tables 6.1 and 6.2 in
  Section 6.4 of the OWL 2 RDF-Based Semantics specification,
  available at http://www.w3.org/TR/owl2-rdf-based-semantics/.  Please
  note that those tables do not include the different annotations
  (labels, comments and rdfs:isDefinedBy links) used in this file.
  Also note that the descriptions provided in this ontology do not
  provide a complete and correct formal description of either the syntax
  or the semantics of the introduced terms (please see the OWL 2
  recommendations for the complete and normative specifications).
  Furthermore, the information provided by this ontology may be
  misleading if not used with care. This ontology SHOULD NOT be imported
  into OWL ontologies. Importing this file into an OWL 2 DL ontology
  will cause it to become an OWL 2 Full ontology and may have other,
  unexpected, consequences."
     :vann/preferredNamespaceUri "http://www.w3.org/2002/07/owl#"
     :vann/preferredNamespacePrefix "owl"
     :foaf/homepage "https://www.w3.org/OWL/"
     :dcat/downloadURL "http://www.w3.org/2002/07/owl"
     :voc/appendix [["http://www.w3.org/2002/07/owl"
                     :dcat/mediaType "text/turtle"]]
     }
    )
 
(put-ns-meta!
 'ont-app.vocabulary.vann
    {
     :rdfs/label "VANN"
     :dc/description "A vocabulary for annotating vocabulary descriptions"
     :vann/preferredNamespaceUri "http://purl.org/vocab/vann/"
     :vann/preferredNamespacePrefix "vann"
     :foaf/homepage "http://vocab.org/vann/"
     })

(put-ns-meta!
 'ont-app.vocabulary.dc
    {
     :dc/title "Dublin Core Metadata Element Set, Version 1.1"
     :vann/preferredNamespaceUri "http://purl.org/dc/elements/1.1/"
     :vann/preferredNamespacePrefix "dc"
     :dcat/downloadURL "http://purl.org/dc/elements/1.1/"
     :voc/appendix [["http://purl.org/dc/elements/1.1/"
                     :dcat/mediaType "text/turtle"]]
     }
    )

(put-ns-meta!
 'ont-app.vocabulary.dct
    {
     :dc/title "DCMI Metadata Terms - other"
     :vann/preferredNamespaceUri "http://purl.org/dc/terms/"
     :vann/preferredNamespacePrefix "dct"
     :dcat/downloadURL "http://purl.org/dc/terms/1.1/"
     :voc/appendix [["http://purl.org/dc/elements/1.1/"
                     :dcat/mediaType "text/turtle"]]
     }
    )

(put-ns-meta!
 'ont-app.vocabulary.shacl
    {
     :rdfs/label "W3C Shapes Constraint Language (SHACL) Vocabulary"
     :rdfs/comment
     "This vocabulary defines terms used in SHACL, the W3C Shapes
   Constraint Language."
     :vann/preferredNamespaceUri "http://www.w3.org/ns/shacl#"
     :vann/preferredNamespacePrefix "sh"
     :foaf/homepage "https://www.w3.org/TR/shacl/"
     :dcat/downloadURL "https://www.w3.org/ns/shacl.ttl"
     })

(put-ns-meta!
 'ont-app.vocabulary.dcat
    {
     :dc/title "Data Catalog vocabulary"
     :foaf/homepage "https://www.w3.org/TR/vocab-dcat/"
     :dcat/downloadURL "https://www.w3.org/ns/dcat.ttl"
     :vann/preferredNamespacePrefix "dcat"
     :vann/preferredNamespaceUri "http://www.w3.org/ns/dcat#"
     }
    )
   
(put-ns-meta!
 'ont-app.vocabulary.foaf
 {
  :dc/title "Friend of a Friend (FOAF) vocabulary"
  :dc/description "The Friend of a Friend (FOAF) RDF vocabulary,
 described using W3C RDF Schema and the Web Ontology Language."
  :vann/preferredNamespaceUri "http://xmlns.com/foaf/0.1/"
  :vann/preferredNamespacePrefix "foaf"
  :foaf/homepage "http://xmlns.com/foaf/spec/"
  :dcat/downloadURL "http://xmlns.com/foaf/spec/index.rdf"
  :voc/appendix [["http://xmlns.com/foaf/spec/index.rdf"
                  :dcat/mediaType "application/rdf+xml"]]
  }
 )

(put-ns-meta!
 'ont-app.vocabulary.skos
    {
     :dc/title "SKOS Vocabulary"
     :dc/description "An RDF vocabulary for describing the basic
   structure and content of concept schemes such as thesauri,
   classification schemes, subject heading lists, taxonomies,
   'folksonomies', other types of controlled vocabulary, and also
   concept schemes embedded in glossaries and terminologies."
     :vann/preferredNamespaceUri "http://www.w3.org/2004/02/skos/core#"
     :vann/preferredNamespacePrefix "skos"
     :foaf/homepage "https://www.w3.org/2009/08/skos-reference/skos.html"
     :dcat/downloadURL "https://www.w3.org/2009/08/skos-reference/skos.rdf"
     :voc/appendix [["https://www.w3.org/2009/08/skos-reference/skos.rdf"
                     :dcat/mediaType "application/rdf+xml"]]   
     }
    )

(put-ns-meta!
 'ont-app.vocabulary.schema
    {
     :vann/preferredNamespaceUri "http://schema.org/"
     :vann/preferredNamespacePrefix "schema"
     :dc/description "Schema.org is a collaborative, community activity
   with a mission to create, maintain, and promote schemas for
   structured data on the Internet, on web pages, in email messages,
   and beyond. "
     :foaf/homepage "https://schema.org/"
     :dcat/downloadURL #{"http://schema.org/version/latest/schema.ttl"
                         "http://schema.org/version/latest/schema.jsonld"}
     :voc/appendix [["http://schema.org/version/latest/schema.ttl"
                     :dcat/mediaType "text/turtle"]
                    ["http://schema.org/version/latest/schema.jsonld"
                     :dcat/mediaType "application/ld+json"]]
     })

(put-ns-meta!
 'ont-app.vocabulary.xsd
    {
     :dc/description "Offers facilities for describing the structure and
   constraining the contents of XML and RDF documents"
     :vann/preferredNamespaceUri "http://www.w3.org/2001/XMLSchema#"
     :vann/preferredNamespacePrefix "xsd"
     :foaf/homepage "https://www.w3.org/2001/XMLSchema"
     })

;; deprecated
(def ^:deprecated cljc-put-ns-meta! "Deprecated. Use put-ns-meta!" put-ns-meta!)
(def ^:deprecated cljc-get-ns-meta "Deprecated. Use get-ns-meta" get-ns-meta)

