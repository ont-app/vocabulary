(ns ont-app.vocabulary.core
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :refer [join]]
   [ont-app.vocabulary.format :as fmt :refer [decode-kw-ns
                                              decode-kw-name
                                              decode-uri-string
                                              encode-kw-name
                                              encode-uri-string]]
   [ont-app.vocabulary.lstr :as lstr]
   [ont-app.vocabulary.dstr :as dstr]
   #?(:clj [clojure.instant :refer [read-instant-date]])
   #?(:cljs [clojure.reader :as cljs-reader :refer [read-string]])
   #?(:cljs [goog.date.DateTime :as DateTime])
   ))

;;;;;;;;;
;; SPEX
;;;;;;;;;

(declare match-uri-str-spec)
(spec/def :voc/uri-str-spec (fn [s] (match-uri-str-spec s)))

(spec/def :vann/preferredNamespaceUri :voc/uri-str-spec)

(spec/def :vann/preferredNamespacePrefix string?)

(spec/def :voc/triple (spec/coll-of (spec/or :uri string? :kwi keyword?) :kind vector?))

(spec/def :voc/appendix (spec/coll-of :voc/triple :kind vector?))

(spec/def :voc/ns-meta-spec
  (fn [m]
    (or (and (:vann/preferredNamespacePrefix m)
             (:vann/preferredNamespaceUri m))
        (:voc/mapsTo m))))

(declare match-qname-spec)
(spec/def :voc/qname-spec (fn [s] (match-qname-spec s)))

(declare match-kwi-spec)
(spec/def :voc/kwi-spec (fn [k] (match-kwi-spec k)))

;;;;;;;;;;
;; Caches
;;;;;;;;;;

(def ^:private prefix-to-ns-cache (atom nil))
(def ^:private namespace-to-ns-cache (atom nil))
(def ^:private prefix-re-str-cache (atom nil))
(def ^:private namespace-re-cache (atom nil))

(defn clear-caches! 
  "Side-effects: resets all caches in voc/ to nil
NOTE: call this when you may have imported new namespace metadata
"
  []
  (reset! prefix-re-str-cache nil)
  (reset! namespace-to-ns-cache nil)
  (reset! prefix-to-ns-cache nil)
  (reset! namespace-re-cache nil))

(def resource-type-context
  "Atom naming the operative resource-type context. Informs `resource-type-dispatch`."
  (atom ::resource-type-context))

(defn resource-type-dispatch
  "Returns [@resoruce-type-context (type this)]
  - Where
    - `this` is something which we may want to render as a URI or related construct.
  "
  [this]
  (tap> {:type ::resource-type-dispatch
         ::this this
         ::resource-type-context @resource-type-context})
  [@resource-type-context (type this)])

(defmulti resource-type
  "- Signature [this] -> `resource-type`, informed by @`resource-type-context`
  - Where
    - `this` is something renderable as a URI, KWI, qname, or some other resource ID.
    - `resource-type` names a resource type on which `as-uri-string`, `as-kwi`, `as-qname` or other methods might be dispatched.
    - @`resource-type-context` names a context. Default is ::resource-type-context.
  - NOTE: `resource-type-dispatch` := [this] -> [@resource-type-context (type this)]
  "
  resource-type-dispatch)

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; FUN WITH READER MACROS
;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (do
     (cljs.reader/register-tag-parser! "lstr" lstr/read-LangStr-cljs)
     (cljs.reader/register-tag-parser! "dstr" dstr/read-DatatypeStr-cljs)
   ))

#?(:cljs (enable-console-print!))

;;;; namespace metadata isn't available at runtime in cljs...
#?(:cljs
   (def cljs-ns-metadata
     "Namespaces in cljs are not proper objects, and there is no metadata
  available at runtime. This atom stores 'pseudo-metadata' declared with
  cljc-put-ns-metadata and accessed with cljc-get-metadata. Clj just uses
  the metadata regime for its ns's"
     (atom {})))

(defn put-ns-meta!
  "Side-effect: ensures that subsequent calls to (cljc-get-ns-meta `ns'` return `m`
  Where
  - `ns'`  is an ns(clj only) or the name of a namespace, possibly declared for the sole purpose of holding vocabulary metadata (e.g. rdf, foaf, etc)
  - `m` := {`key` `value`, ...}, metadata (clj) or 'pseudo-metadata' (cljs)
  - `key` is a keyword containing vocabulary metadata, e.g.
    `::vann/preferredNamespacePrefix`
  NOTE: In cljs, ns's are not available at runtime, so the metadata is stored
    in an atom called 'voc/cljs-ns-metadata'
  See also declarations for ont-app.vocabulary.rdf, ont-app.vocabulary.foaf, etc.
  for examples of namespaces declared solely to hold vocabulary metadata."
  ([ns' m]
   (spec/assert :voc/ns-meta-spec m)
   #?(:cljs
      (swap! cljs-ns-metadata
             assoc ns' m)
      :clj
      (alter-meta!
       (if (symbol? ns')
         (if-let [found (find-ns ns')]
           found
           ;; else no proper namespace found
           (or (try (eval `(var ~ns'))
                    (catch Exception _))
               (create-ns ns')))
                             
         ;; else not a symbol
         (let []
           (assert (= (type (find-ns 'user)) clojure.lang.Namespace))
           ns'))
       merge m))
   (clear-caches!))

  ([m]
   #?(:cljs (put-ns-meta! (namespace ::dummy)) 
      :clj (put-ns-meta! *ns* m))))

(defn get-ns-meta
  "Returns `metadata` assigned to ns named `ns'`
  Where
  - `ns'` names a namespace or a 'dummy' namespace whose sole purpose is to hold metadata.
  - `metadata` := {`key` `value`, ...}
  - `key` is a keyword containing vocabulary metadata, e.g. :vann/preferredNamespacePrefix"
  ([ns']
   #?(:cljs
      (do
        (assert (symbol? ns'))
        (get @cljs-ns-metadata ns'))
      :clj
      (if (symbol? ns')
        (if-let [it (find-ns ns')]
          (meta it))
         ;; else not a symbol
        (meta ns'))))

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
as some symbol other than the preferred prefix."
  []
  #?(:clj (ns-aliases *ns*)
     :cljs *alias-map*))

(defn cljc-find-ns 
  "Returns `ns-name-or-obj` for `ns'`, or nil.
Where 
  - `ns-name-or-obj` may either be a namespace (in clj) 
    or the name of a namespace (in cljs)
  - `_ns` is a symbol which may name a namespace.
NOTE: Implementations involving cljs must use cljs-put/get-ns-meta to declare
  ns metadata."
  [ns']
  #?(:clj (find-ns ns')
     :cljs (if (contains? @cljs-ns-metadata ns')
             ns')
     ))

(defn cljc-all-ns 
  "Returns (`ns-name-or-obj` ...)
Where
  - `ns-name-or-obj` may either be a namespace (in clj) 
     or the name of a namespace (in cljs)"
  []
  #?(:clj (all-ns)
     :cljs (keys @cljs-ns-metadata)))

(declare prefix-re-str)
(defn cljc-find-prefixes 
  "Returns #{`prefix`...} for `s` matching `re-str`
Where
  - `prefix` is a prefix found in `s`, for which some (meta ns) has a 
     :vann/preferredNamespacePrefix declaration
  - `re-str` is a regex string
  - `s` is a string, typically a SPARQL query body for which we want to 
    infer prefix declarations."
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

;;;;;;;;;;;;;;;;;;
;; resource types
;;;;;;;;;;;;;;;;;;
(declare prefixed-ns)

(defmethod resource-type [::resource-type-context #?(:clj java.lang.String :cljs (type ""))]
  [this]
  (cond
    (spec/valid? :voc/uri-str-spec this)
    :voc/UriString

    (spec/valid? :voc/qname-spec this)
    :voc/Qname

    :else
    :voc/NonUriString))

(defmethod resource-type [::resource-type-context
                          #?(:clj clojure.lang.Keyword :cljs cljs.core/Keyword)]
  [this]
  (let [prefix (namespace this)
        kw-name (name this)
        ]
    (if prefix
      (let [ns' (or (cljc-find-ns (symbol prefix))
                    (prefixed-ns prefix))
            ]
        (if ns' :voc/Kwi
            ;; else
            :voc/QualifiedNonKwi))
      ;; else no prefix
      (if (spec/valid? :voc/uri-str-spec kw-name)
        :voc/Kwi
        :voc/UnqualifiedKeyword))))

#?(:clj
   (defmethod resource-type [::resource-type-context java.io.File]
     [_]
     :voc/LocalFile))
       
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Differing escaping semantics
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:private true} unescaped-slash-re
  "Matches a string preceded by a backslash escape"
  #?(:clj #"[^\\][\/]"
     :cljs #"[^\\][/]"))

(defn escape-slash
  "Replaces a slash in `s` with a backslash-escaped slash"
  [s]
  #?(:clj (clojure.string/replace s #"/" "\\\\/")
     :cljs (clojure.string/replace s #"/" "\\/")))
;;;;;;;;;;;;;;;;;;;;
;; Parsing instants
;;;;;;;;;;;;;;;;;;;;

(defn cljc-to-date
  "returns a string rendering of a date for xsd:dateTime."
  [s]
  #?(:clj (.toInstant s)
     :cljs (.toISOString (js/Date. s))))

(defn cljc-read-date
  "Reads a date object from xsd:datetime."
  [date]
  #?(:clj (read-instant-date date)
     :cljs (js/Date. date)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; NO READER MACROS BEYOND THIS POINT
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;; SCHEMA

(put-ns-meta!
 'ont-app.vocabulary.core
 {:doc "Defines utilities and a set of namespaces for commonly used linked data 
constructs, metadata of which specifies RDF namespaces, prefixes and other 
details."
  :vann/preferredNamespacePrefix "voc"
  :vann/preferredNamespaceUri "http://rdf.naturallexicon.org/ont-app/vocabulary/"
  })

(def terms
  "Describes vocabulary for this namespace. May be read into an IGraph downstream."
  ^{:triples-format :vector-of-vectors}
  [[:voc/appendix
    :rdf/type :rdf:Property
    :rdfs/comment "<ns> :voc/appendix <triples>
 Asserts that <triples> describe a graph that elaborates on other attributes 
asserted in usual key-value metadata asserted for <ns>, e.g. asserting a 
dcat:mediaType relation for some dcat:downloadURL."]])

;;;; FUNCTIONS
(defn on-duplicate-prefix
  "Throws an error if a prefix is bound to more than one namespace.
  Reduces into `prefix` argument
  - Where
    - `prefixes` := {`prefix` `ns`, ...}
    - `prefix` := is a string naming the `vann:preferredNamespaceUri` for `ns'`
    - `ns'` refers to a namespace
  NOTE: Can be overridden with `with-redefs`."
  [prefixes prefix ns']
  (throw (ex-info (str "Prefix `" prefix "` is being associated with both "
                       (prefixes prefix) " and " ns')
                  {:type ::DuplicatePrefix
                   :prefixes prefixes
                   :prefix prefix
                   :ns ns'})))

(defn vann-annotated-objects 
  "Returns `[obj, ...]
  - Where:
    - `obj` bears metadata s.t. (get-ns-meta obj)  includes :vann/... annotations
      - these may be either namespaces or vars"
  []
  (let [has-vann-declarations? (fn [obj]
                                 (if-let [m (get-ns-meta obj)]
                                   (and (:vann/preferredNamespacePrefix m)
                                        (:vann/preferredNamespaceUri m))))]
    (filter has-vann-declarations?
            (reduce conj
                    ;; namespaces...
                    (cljc-all-ns)
                    ;; vars ...
                    (mapcat (comp vals cljc-ns-map) (cljc-all-ns))))))

(defn collect-prefixes 
  "Returns {`prefix` `namespace` ...} s.t. `next-ns` is included
  Where
  - `acc` := {`prefix` `namespace` ...}
  - `next-ns` is typically an element in a reduction sequence of ns's
  - `prefix` is a prefix declared in the metadata of `next-ns`
  - `namespace` is a URI namespace declared for `prefix` in metadata of `next-ns`"
  [acc next-ns]
  {:pre [(map? acc)]
   }
  (let [nsm (get-ns-meta next-ns)
        add-prefix (fn [acc prefix]
                     (if (acc prefix)
                       (on-duplicate-prefix acc prefix next-ns)
                       (assoc acc prefix next-ns)))]
    (if-let [p (:vann/preferredNamespacePrefix nsm)]
      (if (set? p)
        (reduce add-prefix acc p)
        (add-prefix acc p))
      acc)))

(defn prefix-to-ns 
  "Returns {`prefix` `ns` ...}
  - Where
    - `prefix` is declared in metadata for some `ns` with
       :vann/preferredNamespacePrefix
    - `ns` is an instance of clojure.lang.ns available within the lexical
       context in which the  call was made."
  []
  (when-not @prefix-to-ns-cache
    (reset! prefix-to-ns-cache
            (reduce collect-prefixes
                    {}
                    (vann-annotated-objects))))
  @prefix-to-ns-cache)

(defn ns-to-namespace 
  "Returns `iri` for `ns`
- Where
  - `ns'` is an instance of clojure.lang.Namespace (in clj) or a symbol-name for ns (cljs)
  - `iri` is an iri declared with :vann/preferredNamespaceUri in the metadata for 
    `ns'`, or nil"
  [ns']
  (or
   (:vann/preferredNamespaceUri (get-ns-meta ns'))
   (-> ns'
       get-ns-meta
       :voc/mapsTo
       get-ns-meta
       :vann/preferredNamespaceUri)))

(defn namespace-to-ns 
  "returns {`namespace` `ns` ...} for each `ns` with :vann/preferredNamespaceUri
  declaration
  - Where
    - `namespace` is the URI suitable for for an RDF prefix declaration
    - `ns` is either a clojure ns (clj) or a symbol naming a clojure ns (cljs)."
  []
  (when-not @namespace-to-ns-cache
    (let [collect-mapping (fn [acc ns']
                            (if-let [namespace (:vann/preferredNamespaceUri
                                                (get-ns-meta ns'))
                                     ]
                              (assoc acc namespace ns')
                              ;;else
                              acc))]
      (reset! namespace-to-ns-cache
              (reduce collect-mapping {} (vann-annotated-objects)))))
  @namespace-to-ns-cache)

(defn prefixed-ns 
  "Returns nil or the ns whose `prefix` was declared in metadata with `:vann/preferredNamespacePrefix`.
  - Where
    - `prefix` is a string, typically parsed from a keyword."
  [prefix]
  {:pre [(string? prefix)]
   }
  (or (get (prefix-to-ns) prefix)
      #_(get (cljc-ns-aliases) (symbol prefix))))

(def ordinary-iri-str-re
  "A regex matching a standard IRI string."
  #"^(http:|https:|file:).*")
  
(def ^:dynamic *exceptional-iri-str-re*
  "A regex matching an IRI string which doesn't match the usual http//-ish scheme, such as `urn:`."
  #"^(urn:|arn:).*")

(defn- match-uri-str-spec
  "Truthy when `s` matches spec `:voc/uri-str-spec`."
  [s]
  (or (re-matches ordinary-iri-str-re s)
      (re-matches *exceptional-iri-str-re* s)))

(defn- match-kwi-spec
  "Truthy when `k` matches spec `:voc/kwi-spec`"
  [k]
  (and (keyword? k)
       (let [prefix (namespace k)
             kw-name (name k)]
         (or 
          (and prefix
               (seq kw-name) ;; empty name is not a valid keyword
               (or (cljc-find-ns (symbol prefix))
                   (prefixed-ns prefix)
                   ))
          (spec/valid? :voc/uri-str-spec kw-name)))))


(defn kwi-missing-namespace-if-not-urn-or-arn
  "Returns the name-stiring of `kw`, or throws ::NoIRIForKw if `kw` is incorrectly missing a namespace."
  [kw]
  {:pre [(keyword? kw)
         (empty? (namespace kw))]
   }
  (if (re-matches *exceptional-iri-str-re* (name kw))
    (name kw)
    (throw (ex-info (str "Could not find IRI for " kw)
                  {:type ::NoIRIForKw
                   ::kw kw
                   }))))

(defn default-on-no-kwi-ns
  "Returns the name-string of `kw` if its name string is a typical URI or URN, otherwise throws a :NoIRIForKw error.
  - Where:
    - `kw` is a keyword with no namespace."
  [kw]
  {:pre [(keyword? kw)
         (empty? (namespace kw))]
   }
  (let [kw-name (name kw)
        ]
    (if (spec/valid? :voc/uri-str-spec kw-name)
      (-> kw-name
          decode-kw-name
          encode-uri-string)
      (throw (ex-info (str "Could not find IRI for " kw)
                      {:type ::NoIRIForKw
                       ::kw kw
                       })))))
  
(defn uri-for
  "Returns `iri` for `kw` based on metadata attached to `ns` Alias of `iri-for` or `on-no-prefix (kw) if the keyword is not namespaced.
  - Where
    - `kw` is a keyword of the form `prefix`:`value`
    - `on-no-kwi-ns` := fn [kw] -> uri, for cases where `kw` is not namespaced
      default is `default-on-no-kwi-ns`
    - `iri` is of the form `namespace``value`
    - `ns` is an instance of clojure.lang.ns
    - `prefix` is declared with :vann/preferredNamespacePrefix in metadata of `ns`
    - `namespace` is typically of the form http://...., declared with 
      `:vann/preferredNamespaceUri` in metadata of `ns`"
  ([kw]
   (uri-for default-on-no-kwi-ns kw))

  ([on-no-kwi-ns kw]
   {:pre [(keyword? kw)]
    }
  (let [prefix (namespace kw)
        kw-name (name kw)]
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
  "Returns the prefix associated with `ns'`
  - Where
    - `ns'` is a clojure namespace, which may have :vann/preferredNamespacePrefix
      declaration in its metadata."
  [ns']
  (or 
   (:vann/preferredNamespacePrefix (get-ns-meta ns'))
   (-> ns'
       (get-ns-meta)
       :voc/mapsTo
       (get-ns-meta)
       :vann/preferredNamespacePrefix)
   (str (ns-name ns'))))

(defn prefix-to-namespace-uri
  "returns `namespace` URI associated with `prefix`
  - Where:
    - `prefix` is a string declared for `ns` with vann/preferredNamespacePrefix
    - `namespace` is a string declared for some `ns` with vann/preferredNamespaceUri"
  [prefix]
  (->> prefix
       (get (prefix-to-ns))
       (ns-to-namespace)))

(defn namespace-re 
  "Returns a regex to recognize substrings matching a URI for an ns declared with LOD metadata.
  - Note: Groups for namespace and value."
  []
  (or @namespace-re-cache
      (let [namespace< (fn [a b] ;; match longer first
                         (> (count a)
                            (count b)))
            ]
        (reset! namespace-re-cache
                (re-pattern (str "^("
                                 (join "|" (sort namespace<
                                                   (keys (namespace-to-ns))))
                                 ")(.*)")))
        @namespace-re-cache)))

(defn qname-for 
  "Returns the 'qname' URI for `kw`, or <...>'d full URI if no valid qname could be found.
  - Throws an error if the prefix is specified, but can't be mapped to metadata.
  - Where
    - `kw` is a keyword, in a namespace with LOD declarations in its metadata."
  [kw]
  {:pre [(keyword? kw)]
   }
  (let [prefix (namespace kw)
        kw-name (name kw)]
    (if (#{"https:" "http:" "file:" "https%3A" "http%3A" "file%3A"} prefix)
      ;; scheme was parsed as namespace of kw
      (let [uri-str (str (-> prefix decode-kw-name)
                         "/"
                         (-> kw-name decode-kw-name encode-uri-string))]
        (if-let [rem (re-matches (namespace-re) uri-str)]
          (let [[_ namespace-uri kw-name] rem]
            (if (not (spec/valid? :voc/qname-spec kw-name))
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
        (let [ns' (or (cljc-find-ns (symbol prefix))
                      (prefixed-ns prefix))]
          (when-not ns'
            (throw (ex-info (str "Could not resolve prefix " prefix)
                            {:type ::CouldNotResolvePrefix
                             ::kw kw
                             ::prefix prefix
                             })))
          (let [qname (str (ns-to-prefix ns')
                           ":"
                           (-> kw-name decode-kw-name encode-uri-string
                               (escape-slash)
                               )
                           )]
            (if (not (spec/valid? :voc/qname-spec qname))
              (str "<" (prefix-to-namespace-uri prefix) kw-name ">")
              ;;else valid as qname
              qname)))
        ;; else no namespace in keyword
        (str "<" (-> kw-name decode-kw-name encode-uri-string) ">")))))

(defn prefix-re-str 
  "Returns a regex string that recognizes prefixes declared in ns metadata with `:vann/preferredNamespacePrefix` keys. 
  - NOTE: this is a string because the actual re-pattern will differ per clj/cljs."
  []
  (when-not @prefix-re-str-cache
    (reset! prefix-re-str-cache
            (str "\\b(" ;; word boundary
                 (join "|" (keys (prefix-to-ns)))
                 "):")))
  @prefix-re-str-cache)

(defn qname-re []
  "Returns a regex s.t. 'my-ns:my-name' will parse to ['my-ns:my-name' 'my-ns' 'my-name']"
  (let [name-pattern (str "("            ;; start group
                          ""             ;; either nothing
                          "|"            ;; or
                          "[^#:].*"      ;; doesn't start with invalid char
                          ")"            ;; end group
                          "$"            ;; end of string
                          )]
    (re-pattern (str (prefix-re-str) name-pattern))))

(defn- match-qname-spec
  "Truthy when `s` conforms to spec :voc/qname-spec."
  [s]
  (if-let [[_ ns' name'] (re-matches (qname-re) s)]
    (not (re-find unescaped-slash-re name'))
    ;; else it's not a standard qname
    (if-let [[_ uri-str] (re-matches #"^[<](.*)[>]$" s)] ;; angle-brackets
      (spec/valid? :voc/uri-str-spec uri-str))))


(defn default-on-no-ns
  "Returns the kwi normally appropriate for `kw` in cases where no ns can be matched, as is the case with say http://...
  - Where
    - _uri is a dummy provided to conform to the expected function signature.
    - `kw` is either a keyword or a string (which will be read into a keyword)"
  [_uri kw]
  (if (keyword? kw)
            kw
            (keyword (str kw))))

(defn keyword-for 
  "Returns a keyword equivalent of `uri`, properly prefixed if Vann declarations exist in some ns in the current lexical environment.
  - Side effects per `on-no-ns`
  - Where
    - `uri` is a string representing a URI
    - `on-no-ns` (optional) := fn [uri kw] -> kwi',
       possibly with side-effects in response to the fact that no qname was found for
       `uri` (default returns `kw`)
  - NOTE: typically `on-no-ns` would log a warning or make an assertion."
  ([uri]
   (keyword-for default-on-no-ns uri))
  ([on-no-ns uri]
   {:pre [(string? uri)]
    :post [(keyword? %)]
    }
   (if-let [prefix-re-match  (re-matches (qname-re) uri)]
     ;; ... this is a qname...
     (let [[_ prefix _name] prefix-re-match
           remove-backslash (fn [s] (clojure.string/replace s #"\\" ""))
           ]
       (spec/assert :voc/qname-spec uri)
       (keyword prefix (-> _name remove-backslash decode-uri-string encode-kw-name)))
     ;;else this isn't a qname. Maybe it's a full URI we have a prefix for...
     ;; namespace re match returns [s namepace value] or nil
     (let [namespace-re-match  (re-matches (namespace-re) uri)
           _namespace (and namespace-re-match (namespace-re-match 1))
           _value (and namespace-re-match (namespace-re-match 2))]
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
                    (-> _value decode-uri-string encode-kw-name))))))))

(defn sparql-prefixes-for 
  "Returns [`prefix-string`...] for each prefix identified in `sparql-string`.
  - Where
    - `sparql-string` is a string of SPARQL, typically without prefixes
    - `prefix-string` := PREFIX `prefix`: `namespace`\n
    - `prefix` is a prefix defined for `namespace` in metadata of some ns with 
       `:vann/preferredNamespacePrefix`
    - `namespace` is a namespace defined in the metadata for some ns with 
      `:vann/preferredNamespaceUri`"
  [sparql-string]
  (let [sparql-prefix-for (fn [prefix]
                            (str "PREFIX "
                                 prefix
                                 ": <"
                                 (prefix-to-namespace-uri prefix)
                                 ">"))]
    (map sparql-prefix-for (cljc-find-prefixes (prefix-re-str)
                                               sparql-string))))

(defn prepend-prefix-declarations 
  "Returns `sparql-string`, prepended with appropriate PREFIX decls.
  - Where
    - `sparql-string` is a string of SPARQL, typically without prefixes."
  [sparql-string]
  (join "\n" (conj (vec (sparql-prefixes-for sparql-string))
                     sparql-string)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Methods keyed to resource-type
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare as-uri-string)

(defmulti as-kwi
  "Returns keyword identifier for an instance of Resource.
  - Signature: [this] -> `kwi`.
  - Where:
    - `this` is an instance of RESOURCE
    - `kwi` conforms to :voc/kwi-spec"
  resource-type)

(defmethod as-kwi :voc/Kwi
  [this]
  {:post [(spec/assert :voc/kwi-spec %)]}  
  this)

(defmethod as-kwi :voc/Qname
  [this]
  {:post [(spec/assert :voc/kwi-spec %)]}
  (keyword-for (as-uri-string this)))

(defmethod as-kwi :voc/KwiInferredFromUriString
  [this]
  {:post [(spec/assert :voc/kwi-spec %)]}    
  (keyword-for (as-uri-string this))
  )

(derive :voc/LocalFile :voc/KwiInferredFromUriString)

(defmethod as-kwi :voc/UriString
  [this]
  {:post [(spec/assert :voc/kwi-spec %)]}
  (keyword-for this))

(defmethod as-kwi :default
  [this]
  (throw (ex-info (str "No `as-kwi` method for " this)
                  {:type ::no-as-kwi-method
                   ::this this
                   ::resource-type (resource-type this)
                   })))

(defmulti as-uri-string
  "Returns URI string for instance of Resource.
  - Signature: [this] -> `uri-string`
  - Where
    - `this` is a Resource
    - `uri-string` conforms to `:voc/uri-str-spec`"
  resource-type)

(defmethod as-uri-string :voc/UriString
  [this]
  {:post [(spec/assert :voc/uri-str-spec %)]}
  this)

(defmethod as-uri-string :voc/UriStringInferredFromKwi
  [this]
  {:post [(spec/assert :voc/uri-str-spec %)]}  
  (uri-for (as-kwi this)))

(defmethod as-uri-string :voc/Kwi
  [this]
  {:post [(spec/assert :voc/uri-str-spec %)]}  
  (uri-for this))

(defmethod as-uri-string :voc/LocalFile
  [this]
  {:post [(spec/assert :voc/uri-str-spec %)]}  
  (let [s (str this)]
    (as-uri-string
     (if (re-matches ordinary-iri-str-re s)
       s
       (spec/conform :voc/uri-str-spec (str "file:/" s))))))

(defmethod as-uri-string :voc/Qname
  [this]
  {:post [(spec/assert :voc/uri-str-spec %)]}    
  (uri-for (keyword-for this)))

(defmethod as-uri-string :default
  [this]
  (throw (ex-info (str "No `as-uri-string` method for " this)
                  {:type ::no-as-kwi-method
                   ::this this
                   ::resource-type (resource-type this)
                   })))

(defmulti as-qname
  "Returns a qname for instance of Resource.
  - Signature: [this] -> `qname`
  - Where
    - `this` is an instance of `Resource`
    - `qname` conforms to `:voc/qname-spec`"
  resource-type)

(defmethod as-qname :voc/Qname
  [this]
  {:post [(spec/assert :voc/qname-spec %)]}
  this)

(defmethod as-qname :default
  [this]
  {:post [(spec/assert :voc/qname-spec %)]}      
  (qname-for (as-kwi this)))

(defmulti resource=
  "Truthy when two different resource identifiers refer to the same resource
  - Signature: [this that] -> truthy
  - Where
    - `this` is a Resource
    - `that` is a Resource
  "
  (fn [this that] [(resource-type this) (resource-type that)]))

(defmethod resource= :default
  [this that]
  (= (as-uri-string this) (as-uri-string that)))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Typed Literal support
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tag-dispatch
  "Returns `tag` for `obj` with optional `tag-spec`
  - Where
    - `obj` is something to be tagged
    - `tag-spec` is a resource, or (@lstr/default-tags (type `obj`))
    - `tag` := (as-kwi `tag-spec`)
  "
  ([obj]
   (if-let [tag-spec (@dstr/default-tags (type obj))]
     (tag-dispatch obj tag-spec)
     ;; else no entry in default tags
     (throw (ex-info (str "No tag found for type " (type obj) " of `obj`")
                     {:type ::no-tag-found
                      ::obj obj
                      ::obj-type (type obj)
                      ::tag-keys (keys @dstr/default-tags)}))))
  ([obj tag-spec]
   (as-kwi tag-spec)))

(defmulti tag
  "Signature [`obj` & maybe `tag-spec`] -> `dstr`
  - Where
    - `obj` is something to be tagged
    - `tag-spec` is a resource, or (@lstr/default-tags (type `obj`))
    - `dstr` :~ #voc/dstr `obj`^^`tag`
    - `tag` := (as-kwi `tag-spec`)
  "
  tag-dispatch)

(defmethod tag :xsd/dateTime
  [obj & _]
  (dstr/->DatatypeStr (-> obj cljc-to-date str)
                      "xsd:dateTime"))

(defmethod tag :default
  ([obj]
   (tag obj (@dstr/default-tags (type obj))))
  ([obj tag]
   (dstr/->DatatypeStr (str obj) (as-qname tag))))

(defn untag-dispatch
  "Returns (as-kwi `tag`) for `dstr`
  - Where
    - `dstr` :~ #voc/dstr `obj`^^`tag`
    - `tag` := (as-qname `tag-spec`)
    "
  [dstr & _]
  (as-kwi (dstr/datatype dstr)))

(defmulti untag
  "Signature: [`dstr` & maybe `on-not-found-fn`?]
  - returns: clojure value appropriate to (as-kwi `tag`)
  - Where
    - `dstr` :~ #voc/dstr `obj`^^`tag`
    - `on-not-found-fn` := fn [dstr] -> clojure value or error.
       - Default is `error-on-no-untag-found`.
  "
  untag-dispatch)

(defmethod untag :xsd/Boolean  [obj] {:post [(boolean? %)]} (read-string (str obj)))
(defmethod untag :xsd/dateTime [obj] (cljc-read-date (str obj)))
(defmethod untag :xsd/long     [obj] (read-string (str obj)))
(defmethod untag :xsd/int      [obj] (read-string (str obj)))
(defmethod untag :xsd/integer  [obj] (read-string (str obj)))
(defmethod untag :xsd/double   [obj] (read-string (str obj)))
(defmethod untag :xsd/string   [obj] (str obj))
(defmethod untag :xsd/short    [obj] (short (read-string (str obj))))
(defmethod untag :xsd/float    [obj] (float (read-string (str obj))))
(defmethod untag :xsd/byte     [obj] (byte (read-string (str obj))))

(defn error-on-no-untag-found
  "Default response to a case where no `untag` method was found."
  [dstr]
  (throw (ex-info (str "No untag method found for " dstr "^^" (dstr/datatype dstr))
                  {:type ::no-untag-method-found
                   ::dstr dstr})))

(defmethod untag :default
   ([dstr]
    (untag dstr error-on-no-untag-found))
   ([dstr on-not-found]
    (on-not-found dstr)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;;; NAMESPACE DECLARATIONS
;; ;;; These are commonly used RDF namespaces.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(put-ns-meta! 'qudt
   {:vann/preferredNamespacePrefix "qudt"
    :vann/preferredNamespaceUri "http://qudt.org/schema/qudt/"
    :doc "Quantities, Units, Dimenstions and Datatypes vocabulary. Main vocabulary."
    })

(put-ns-meta! 'unit
   {:vann/preferredNamespacePrefix "unit"
    :vann/preferredNamespaceUri "http://qudt.org/vocab/unit#"
    :doc "Quantities, Units, Dimenstions and Datatypes vocabulary (units module)."
    })

;;;;;;;;;;;;;;;
;; deprecated
;;;;;;;;;;;;;;;

(def ^:deprecated cljc-put-ns-meta! "Deprecated. Use put-ns-meta!" put-ns-meta!)
(def ^:deprecated cljc-get-ns-meta "Deprecated. Use get-ns-meta" get-ns-meta)

^:deprecated
(defmulti uri-str-for
  "Deprecated. Use Resource protocol and `as-uri-string` instead."
  type)

^:deprecated
(defmethod uri-str-for :default
  [x]
  (as-uri-string x))

^:deprecated
(def exceptional-iri-str-re
  "deprecated alias for dynamic var *exceptional-iri-str-re*"
  *exceptional-iri-str-re*)

^:deprecated
(defprotocol Resource
  "Deprecated. Use resource-type multimethod instead."
  :extend-via-metadata true
  (resource-class [this]))
