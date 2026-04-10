(ns ont-app.vocabulary.core
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as str :refer [join]]
   [ont-app.vocabulary.dstr :as dstr]
   [ont-app.vocabulary.format :refer [decode-kw-ns
                                      decode-kw-name
                                      decode-uri-string
                                      escape-slash
                                      unescaped-slash-re
                                      encode-kw-name
                                      encode-uri-string]]
   #?(:cljs [ont-app.vocabulary.lstr :as lstr])
   #?(:clj [clojure.instant :refer [read-instant-date]])
   #?(:cljs [clojure.reader :refer [read-string]])
   ;; #?(:cljs [goog.date.DateTime :as DateTime])
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
        (:voc/mapsTo m)
        (not (or (:vann/preferredNamespacePrefix m)
                 (:vann/preferredNamespaceUri m)
                 (:voc/mapsTo m))))))

(declare match-qname-spec)
(spec/def :voc/qname-spec (fn [s] (match-qname-spec s)))

(declare match-kwi-spec)
(spec/def :voc/kwi-spec (fn [k] (match-kwi-spec k)))

;;;;;;;;;;;;;;;;;
;; CONFIGURATION
;;;;;;;;;;;;;;;;;

(def config
  "A configuration map for ont-app/vocabulary.
  Typical keys are
  - `::operative-resource-context` (optional) a keyword naming the operative
       resource type context, which informs the `resource-type-dispatch` function.
       Resource type contexts should be declared in a taxonomy rooted in
       ::voc/resource-type-context. Left unspecified, it will be set automatically to the
       most specific descendant of ::voc/resource-type-context.
       - See also `most-specific-resource-type-context`
  - `inferred-operative-resource-context`. This will be created automatically in the absence
     of `::operative-resource-context`
  - `::special-uri-str-re` is a regex matching valid URI strings not specified in
       `ordinary-uri-str-re`.
       - default: :~ `^(arn:).*`
  "
  (atom {}))

(def default-config
  "The default value of `voc/config`."
  {::special-uri-str-re #"^(arn:).*"})

;;;;;;;;;;
;; Caches
;;;;;;;;;;

(def ^:private prefix-to-ns-cache (atom nil))
(def ^:private namespace-to-ns-cache (atom nil))
(def ^:private prefix-re-str-cache (atom nil))
(def ^:private namespace-re-cache (atom nil))

(defn clear-caches!
  "Side-effects: resets all caches in voc/ to nil.
NOTE: call this when you may have imported new namespace metadata
"
  []
  (reset! prefix-re-str-cache nil)
  (reset! namespace-to-ns-cache nil)
  (reset! prefix-to-ns-cache nil)
  (reset! namespace-re-cache nil))


;;;;;;;;;;;;;;;;;;;;;;;;;;
;; FUN WITH READER MACROS
;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   #_:clj-kondo/ignore
   (do
     (cljs.reader/register-tag-parser! "lstr" lstr/read-LangStr-cljs)
     (cljs.reader/register-tag-parser! "dstr" dstr/read-DatatypeStr-cljs)
   ))

#?(:cljs (enable-console-print!))

;;;; namespace metadata isn't available at runtime in cljs...
#?(:cljs
   (def cljs-ns-metadata
     "Stores cljs ns `pseudo-metadata`.
  Namespaces in cljs are not proper objects, and there is no metadata
  available at runtime. This atom stores 'pseudo-metadata' declared with
  cljc-put-ns-metadata and accessed with cljc-get-metadata. Clj just uses
  the metadata regime for its ns's"
     (atom {})))

(defn put-ns-meta!
  "Side-effect: ensures that subsequent calls to (cljc-get-ns-meta `ns'` return `m`.
  Where
  - `ns'`  is an ns (clj only) or the name of a namespace, possibly declared for the sole purpose of holding vocabulary metadata (e.g. rdf, foaf, etc)
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
      (let [overwrite-ns-meta (fn [meta' m] (-> (reduce dissoc meta'
                                                        #{:vann/preferredNamespacePrefix
                                                          :vann/preferredNamespaceUri
                                                          :voc/mapsTo})
                                                (merge m)))]
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
         overwrite-ns-meta m))
      (clear-caches!)))
   #?(:clj
      ([m] (put-ns-meta! *ns* m))
      :cljs
      ([_m]
       (put-ns-meta! (namespace ::dummy)))))

(defn get-ns-meta
  "Returns `metadata` assigned to ns named `ns'`.
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
        (when-let [it (find-ns ns')]
          (meta it))
         ;; else not a symbol
        (meta ns'))))

  ([]
   #?(:cljs (throw (ex-info "Cannot infer namespace at runtime in cljs"
                            {:type ::CannotInferNamespace}))
      :clj (get-ns-meta *ns*))))

#?(:cljs
   (def ^:dynamic *alias-map*
     "A map :={`alias` `ns-name`, ...}.
  Where
  `alias` is a symbol
  `ns-name` is a symbol naming an ns in the current lexical env
  NOTE: Informs cljc-ns-aliases in cljs space.
  "
     {}))

(defn cljc-ns-aliases
  "Returns {`alias` `ns`, ...}.
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
  - `ns'` is a symbol which may name a namespace.
NOTE: Implementations involving cljs must use cljs-put/get-ns-meta to declare
      ns metadata."
  [ns']
  #?(:clj (find-ns ns')
     :cljs (when (contains? @cljs-ns-metadata ns')
             ns')
     ))

(defn- cljc-all-ns
  "Returns (`ns-name-or-obj` ...).
Where
  - `ns-name-or-obj` may either be a namespace (in clj) or the name of a namespace (in
     cljs)"
  []
  #?(:clj (all-ns)
     :cljs (keys @cljs-ns-metadata)))

(declare prefix-re-str)
(defn- cljc-find-prefixes
  "Returns #{`prefix`...} for `s` matching `re-str`.
Where
  - `prefix` is a prefix found in `s`, for which some (meta ns) has a
     :vann/preferredNamespacePrefix declaration
  - `re-str` is a suitable argument to `re-pattern`
  - `s` is a string, typically a SPARQL query body for which we want to
    infer prefix declarations."
  [re-str s]
  {:pre [(string? re-str)
         (string? s)]}
  #?(:clj
     (let [prefixes (re-matcher (re-pattern re-str) s)]
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
               (recur acc (subs input 1)))))))))

(def ^:private cljc-ns-map
  "Mimics behavior of `ns-map` on cljs, but returns empty symbol->binding map."
  #?(:clj ns-map
     :cljs (fn [_not-a-real-ns] {})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; platform-specific method dispatch types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(derive #?(:clj java.lang.String :cljs (type ""))
        ::cljc-string-type)

(derive #?(:clj clojure.lang.Keyword :cljs cljs.core/Keyword)
        ::cljc-keyword-type)

#?(:clj (derive java.io.File ::cljc-file-type))

#?(:clj (derive clojure.lang.Seqable ::cljc-seqable-type)
   :cljs (do
           (derive PersistentArrayMap ::cljc-seqable-type)
           (derive List ::cljc-seqable-type)
           (derive EmptyList ::cljc-seqable-type)
           (derive PersistentVector ::cljc-seqable-type)))

;;;;;;;;;;;;;;;;;;;;
;; Parsing instants
;;;;;;;;;;;;;;;;;;;;

(defn- cljc-to-date
  "Returns a string rendering of a date for xsd:dateTime."
  [s]
  #?(:clj (.toInstant s)
     :cljs (.toISOString (js/Date. s))))

(defn- cljc-read-date
  "Reads a date object from xsd:datetime."
  [date]
  #?(:clj (read-instant-date date)
     :cljs (js/Date. date)))

(defn- cljc-get-java-class-tag-name
  "Returns a string naming a Java class for use in a (voc/tag ... :clj/javaClass) method. Clj will accept a class instance or a string naming the class; cljs will only accept a string."
  [obj]
  #?(:clj (if (instance? java.lang.Class obj)
            (.getName obj)
            (str obj))
     :cljs (str obj)))

#?(:clj
(defn as-calendar
  "Returns an instance of `java.util.GregorianCalendar` given `date` (JVM only).
  - Where
    - `date` is a java.util.Date, the standard value of clojure #inst
  - NOTE: this lets you `.get` things like the `java.util.Calendar/YEAR`."
  ([date]
   {:pre [(instance? java.util.Date date)]}
   (let [c (java.util.Calendar/getInstance)]
     (.setTime c date)
     c))))

(defn cljc-resolve
  "Returns the resolved `sym`, if possible.
  - Where
    - `sym` is a symbol, typically acquired from a string value in a DSTR tag.
  - NOTE:
    - Resolving symbols is not straightforward under cljs, and I have yet to need to do
      so in anger, so for now under cljs the symbol will be returned unchanged until
      actual use cases present themselves.
  "
  [sym]
  #?(:clj (resolve sym)
     :cljs sym))

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
  :vann/preferredNamespaceUri "http://rdf.naturallexicon.org/ont-app/vocabulary/" })

(def ^:no-doc terms
  "Describes vocabulary for this namespace in a format that may be read into an IGraph downstream."
  ^{:triples-format :vector-of-vectors}
  [[:voc/appendix
    :rdf/type :rdf:Property
    :rdfs/comment "<ns> :voc/appendix <triples>
 Asserts that <triples> describe a graph that elaborates on other attributes
asserted in usual key-value metadata asserted for <ns>, e.g. asserting a
dcat:mediaType relation for some dcat:downloadURL."]])

;;;;;;;;;;;;;;;;;;;;;;
;; UTILIITY FUNCTIONS
;;;;;;;;;;;;;;;;;;;;;;

(defn- unique
  "Returns the only member of a singleton `coll`, or calls `on-ambiguity`."
  ([coll]
   (unique coll (fn [coll] (throw (ex-info "Ambiguous collection"
                                           {:type ::ambiguous-collection
                                            :coll coll})))))
  ([coll on-ambiguity]
   (if (= (count coll) 1)
     (first coll)
     ;; else ambiguous
     (on-ambiguity coll))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resource Type context and method def
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare operative-resource-context)

(defn resource-type-dispatch
  "Returns [`type-context` (type this)].
  - Where
    - `this` is something which we may want to render as a URI or related
       construct.
    - `type-context` is a keyword naming a context to which dispatch methods are
        keyed. It is the value of (operative-resource-context)
       - defaults to (`most-specific-resource-context` `::voc/resource-type-context`)
  "
  [this]
  [(operative-resource-context)
   (type this)])

(defmulti resource-type
  "Signature [this] -> `resource-type`, dispatched on `resource-type-dispatch`.
  - Where
    - `this` is something renderable as a URI, KWI, qname, or some other
       resource ID.
    - `resource-type` names a resource type on which `as-uri-string`, `as-kwi`,
      `as-qname`, `resource=` and perhaps other methods might be dispatched.
  "
  resource-type-dispatch)

(declare mint-kwi)
(defmethod resource-type :default
  [this]
  (mint-kwi :voc/resource-type this))


(defmethod resource-type [::resource-type-context ::cljc-string-type]
  [this]
  (cond
    (spec/valid? :voc/uri-str-spec this)
    :voc/UriString

    (spec/valid? :voc/qname-spec this)
    :voc/Qname

    :else
    :voc/NonUriString))

(declare prefixed-ns)
(defmethod resource-type [::resource-type-context ::cljc-keyword-type]
  [this]
  (let [prefix (namespace this)
        kw-name (name this)]
    (if prefix
      (let [ns' (or (cljc-find-ns (symbol prefix))
                    (prefixed-ns prefix))]
        (if ns' :voc/Kwi
            ;; else
            :voc/QualifiedNonKwi))
      ;; else no prefix
      (if (spec/valid? :voc/uri-str-spec kw-name)
        :voc/Kwi
        :voc/UnqualifiedKeyword))))


(defmethod resource-type [::resource-type-context ::cljc-file-type]
  [_]
  :voc/LocalFile)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PATTERN MATCHING
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ordinary-uri-str-re
  "A regex matching a commonly occurring standard URI string.
  Arbitrarily chosen from https://www.iana.org/assignments/uri-schemes/uri-schemes.xhtml."
  #"^(http:|https:|file:|urn:|tel:|mailto:|jdbc:|odbc:|ftp:|geo:|git:|gopher:|pop:|telnet:).*")

(defn- match-uri-str-spec
  "Truthy when `s` matches spec `:voc/uri-str-spec`."
  [s]
  (or (re-matches ordinary-uri-str-re s)
      (when-let [r (-> @config ::special-uri-str-re)]
        (re-matches r s))))

(defn- match-kwi-spec
  "Truthy when `k` matches spec `:voc/kwi-spec`."
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

(declare namespace-to-ns)
(declare prefix-to-ns)
(defn namespace-re
  "Returns a regex to recognize strings matching a URI for an ns declared with LOD metadata.
  - Side-effect: manages `namespace-re-cache`
  - NOTE: Groups for namespace and name.
    - i.e. (re-matches (namespace-re) uri-string) -> [uri-string namespace name]"
  []
  (or @namespace-re-cache
      (let [namespace< (fn [a b] ;; match longer first
                         (> (count a)
                            (count b)))]
        (reset! namespace-re-cache
                (re-pattern (str "^("      ;; start first group
                                 ;; any of the namespace URIs, longest first...
                                 (join "|" (sort namespace<
                                                 (keys (namespace-to-ns))))
                                 ")"        ;; end first group
                                 "(.*)"     ;; all following as second group
                                 )))
        @namespace-re-cache)))

(defn- prefix-re-str
  "Returns a regex string that recognizes prefixes declared in ns metadata with `:vann/preferredNamespacePrefix` keys.
  - NOTE: this is a string because the actual re-pattern will differ per clj/cljs."
  []
  (when-not @prefix-re-str-cache
    (reset! prefix-re-str-cache
            (str "\\b"                            ;; word boundary
                 "("                              ;; start group
                 (join "|" (keys (prefix-to-ns))) ;; any of the prefixes
                 ")"                              ;; end group
                 ":"                              ;; colon
                 )))
  @prefix-re-str-cache)

(defn- qname-re "Returns a regex s.t. 'my-ns:my-name' will parse to ['my-ns:my-name' 'my-ns' 'my-name']."
  []
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
  (if-let [[_ _ns' name'] (re-matches (qname-re) s)]
    (not (re-find unescaped-slash-re name'))
    ;; else it's not a standard qname
    (when-let [[_ uri-str] (re-matches #"^[<](.*)[>]$" s)] ;; angle-brackets
      (spec/valid? :voc/uri-str-spec uri-str))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PREFIX <-> NAMESPACE MAPPINGS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- vann-annotated-objects
  "Returns [`obj`, ...] of objects annonated with the vann vocabulary.
  - Where:
    - `obj` bears metadata s.t. (get-ns-meta obj)  includes :vann/... annotations
      - these may be either namespaces or vars."
  []
  (let [has-vann-declarations? (fn [obj]
                                 (when-let [m (get-ns-meta obj)]
                                   (and (:vann/preferredNamespacePrefix m)
                                        (:vann/preferredNamespaceUri m))))]
    (filter has-vann-declarations?
            (reduce conj
                    ;; namespaces...
                    (cljc-all-ns)
                    ;; vars ...
                    (mapcat (comp vals cljc-ns-map) (cljc-all-ns))))))

(defn- collect-prefixes
  "Returns {`prefix` #{`namespace`, ...} ...} s.t. `next-ns` is included.
  Where
  - `acc` := {`prefix` #{`namespace`, ...}, ...}
  - `next-ns` is typically an element in a reduction sequence of ns's
  - `prefix` is a prefix declared in the metadata of `next-ns`
  - `namespace` is a URI namespace declared for `prefix` in metadata of `next-ns`
  - NOTE: We should try very hard to ensure that each prefix maps to a singleton set.
    - Failing that see `disambiguate-prefix-ns` method."
  [acc next-ns]
  {:pre [(map? acc)]}
  (let [nsm (get-ns-meta next-ns)
        add-prefix (fn [acc prefix]
                     (if-let [ns-set (acc prefix)]
                       (assoc acc prefix (conj ns-set next-ns))
                       ;; else this is the first declaration of `prefix` (the norm)
                       (assoc acc prefix #{next-ns})))]
    (if-let [p (:vann/preferredNamespacePrefix nsm)]
      (if (set? p)
        (reduce add-prefix acc p)
        (add-prefix acc p))
      acc)))

(defn prefix-to-ns
  "Returns {`prefix` `ns`, ...}.
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

(defmulti disambiguate-prefix-ns
  "Signature: [kw namespaces] -> winning-ns.
  - Where
    - `kw` is a namespaced keyword
    - `namespaces` is a set containing a plurality of namespaces associated with
       prefix = (namespace kw)
    - `winning-ns` is the member of `namespaces` appropriate to `kw`
  "
  (fn [kw _namespaces] (namespace kw)))

(defmethod disambiguate-prefix-ns :default
  [kw namespaces]
  {:pre [(keyword? kw)
         (coll? namespaces)]}
  (throw (ex-info (str "Prefix `"
                       (namespace kw)
                       "` is being associated with multiple namespaces" namespaces)

                  {:type ::DuplicatePrefix
                   :kw kw
                   :namespaces namespaces
                   :prefix (namespace kw)})))

(defn ns-to-namespace
  "Returns `iri` for `ns`.
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
  "Returns {`namespace` `ns`, ...} for each `ns` with `:vann/preferredNamespaceUri` declaration.
  - Where
    - `namespace` is the URI suitable for for an RDF prefix declaration
    - `ns` is either a clojure ns (clj) or a symbol naming a clojure ns (cljs)."
  []
  (when-not @namespace-to-ns-cache
    (let [collect-mapping (fn [acc ns']
                            (if-let [namespace' (:vann/preferredNamespaceUri
                                                (get-ns-meta ns'))]
                              (assoc acc namespace' ns')
                              ;;else
                              acc))]
      (reset! namespace-to-ns-cache
              (reduce collect-mapping {} (vann-annotated-objects)))))
  @namespace-to-ns-cache)

(defn prefixed-ns
  "Returns nil or #{ns, ...} whose `prefix` was declared in metadata with `:vann/preferredNamespacePrefix`.
  - Where
    - `prefix` is a string, typically parsed from a keyword.
    - #{`ns` ...}` is typically a singleton set containg the namespace associated with
      `prefix`. See also the `disambiguate-prefix-ns` method.
  "
  [prefix]
  {:pre [(string? prefix)]}
  (get (prefix-to-ns) prefix))


(defn ns-to-prefix
  "Returns the prefix associated with `ns'`.
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
  "Returns `namespace` URI associated with `prefix`.
  - Where:
    - `prefix` is a string declared for `ns` with vann/preferredNamespacePrefix
    - `namespace` is a string declared for some `ns` with vann/preferredNamespaceUri"
  [prefix]
  (->> prefix
       (get (prefix-to-ns))
       unique
       (ns-to-namespace)))


;;;;;;;;;;;;;;;;
;; Minting KWIs
;;;;;;;;;;;;;;;;

(defmulti kw-string
  "Signature: [this] -> a string to be used as part of a minted kwi.
  - where
    - `this` is some object
  - Dispatched on (type this)"
  type)

(defmethod kw-string :default
  [this]
  (str/replace (str this) #"\s" "_"))

(defmethod kw-string ::cljc-keyword-type
  [this]
  (name this))

(defmethod kw-string ::cljc-seqable-type
  [this]
  (str (abs (hash this))))

(defn mint-kwi-dispatch
  "Returns `head-kwi` as `dispatch-key` for the `mint-kwi` method.
  Where:
  `head-kwi` is the first argument
  `dispatch-key` is a keyword"
  [head-kwi & _args]
  head-kwi)

(defmulti mint-kwi
  "Signature: [`head-kwi` & `args`] -> `cononical-kwi`, dispatched on `mint-kw-dispatch`.
  Where
  - `canonical-kwi` is a KWI minted systematically based on its arguments.
  - `head-kwi` initiates the KWI (typically the name of an existing class in some
    model).
  - `args` := [`property` `value`, ...], .s.t. the named value is uniquely distinguished.
  E.g: The default method simply joins arguments on _ as follows:
    (mint-kwi :myNs/MyClass :myNs/prop1 'foo' :myNs/prop2 'bar)
    -> :myNs/MyClass_prop1_foo_prop2_bar, but overriding methods will be
    dispatched on `head`
  Compiled arguments are rendered as their hashes.
  "
  mint-kwi-dispatch)

(defmethod mint-kwi :default mint-kwi-default
  [head-kwi & args]
  ;; <head-kwi> + hash of sorted args.
  (assert (not (some #(nil? %) args)))
  (let [ns' (namespace head-kwi)
        name' (name head-kwi)
        kwi (keyword ns' (str name' "_" (str/join "_" (map kw-string args))))]
    kwi))

(defmethod mint-kwi :voc/resource-type mint-kwi-resource-type
  [_ this]
  (keyword "voc"
           (str "resource_type_"
                (kw-string (type this)))))

(def dstr-kwi-name-re "Regex parsing DSTR KWI name to [_ datatype hash]."
  #"^d=(.*).hash=(.*)$")

(declare as-kwi)
(declare as-qname)
(declare untag)
(defmethod mint-kwi :voc/dstr mint-kwi-dstr
  [_ dstr]
  {:pre [(= ont_app.vocabulary.dstr.DatatypeStr (type dstr))]}
  (let [hash-fn (or (-> (@config ::resource-hashing-fn) untag)
                    hash)]
    (-> (str "http://rdf.naturallexicon.com/dstr#d="
             (as-qname (dstr/datatype dstr))
             ".hash="
             (hash-fn (str dstr)))
        as-kwi)))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; RESOURCE TYPE CONTEXTS
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti preferred-child-resource-context
  "Signature: [`parent` `children'`] -> preferred `child` or error.
  - Distpatched on `parent`
  - Where
    - `parent` is the resource context from which each `child` is derived
    - `children'` := #{`child`, ...}
    - `child` is derived from `parent` in the global heirarchy.
  - NOTE: this is necessary to infer the single most-specific resource type context for a
    given application in cases where the taxonomy of contexts has multiple branches.
  "
  (fn [parent _children] parent))

(defmethod preferred-child-resource-context :default
  [parent children]
  (throw (ex-info (str "Ambiguous resource type context taxonomy. Please define `preferred-child-resource-context` for " parent)
                  {:type ::ambiguous-resource-type-context
                   :parent parent
                   :children children})))

(defn most-specific-resource-context
  "Returns the `::resource-type-context` which has no descendents.
  Raises an error if this is not unique."
  [parent-context]
  {:pre [(isa? parent-context ::resource-type-context)]}
  (if-let [descendants' (descendants parent-context)]
    (recur (unique (filter #((or (parents %) #{}) parent-context) descendants')
                   (partial preferred-child-resource-context parent-context)))
    ;; else there are no descendants
    parent-context))

(defn operative-resource-context
  "The resource context on which to dispatch the `resource-type` multimethod."
  []
  (or (-> @config ::operative-resource-context)
      (-> @config ::inferred-operative-resource-context)
      (let [context (most-specific-resource-context ::resource-type-context)]
        (swap! config assoc ::inferred-operative-resource-context context)
        context)))

(defn register-resource-type-context!
  "Side-effects: Derives `child` from `parent` and clears (@context ::voc/operative-resource-context).
  - Where
    - `child` names a resource type context on which methods may be dispatched.
    - `parent` names a resource type context on which methods may be dispatched.
  "
  [child parent]
  {:pre [(isa? parent ::resource-type-context)]
   :post [#(isa? child ::resource-type-context)
          #(isa? child parent)]}
  (derive child parent)
  (swap! config dissoc ::inferred-operative-resource-context))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Methods keyed to resource-type
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare as-uri-string)

(defmulti as-kwi
  "Returns keyword identifier for an instance of Resource.
  - Signature: [this] -> `kwi`.
  - Where:
    - `this` is an instance of Resource
    - `kwi` conforms to `:voc/kwi-spec`"
  resource-type)

(defmethod as-kwi :voc/Kwi
  [this]
  {:post [(spec/assert :voc/kwi-spec %)]}
  (if-let [ns' (some-> this namespace symbol find-ns)]
    ;; .. this is a namespace-qualified keyword starting with ::
    (keyword (ns-to-prefix ns') (name this))
    ;; else
    this))

(defmethod as-kwi :voc/Qname
  [this]
  {:post [(spec/assert :voc/kwi-spec %)]}
  (let [[_ prefix name'] (re-matches (qname-re) this)
           remove-backslash (fn [s] (str/replace s #"\\" ""))]
    (if (empty? name')
      ;; there's nothing but prefix
      (-> prefix
          prefix-to-namespace-uri
          as-kwi)
      ;; else there's a complete match to the namespace regex
      (keyword prefix (-> name' remove-backslash decode-uri-string encode-kw-name)))))

(defmethod as-kwi :voc/KwiInferredFromUriString
  [this]
  {:post [(spec/assert :voc/kwi-spec %)]}
  (as-kwi (as-uri-string this)))

(derive :voc/LocalFile :voc/KwiInferredFromUriString)

(defmethod as-kwi :voc/UriString
  [uri]
  {:post [(spec/assert :voc/kwi-spec %)]}
  (let [namespace-re-match  (re-matches (namespace-re) uri)
        namespace' (and namespace-re-match (namespace-re-match 1))
        value' (and namespace-re-match (namespace-re-match 2))]
    (if (empty? value')
      ;; there's nothing but prefix
      (-> uri
          decode-uri-string
          encode-kw-name
          keyword)
      ;; else there's a match to the namespace regex
      (if (not namespace')
           (-> (decode-uri-string value')
               encode-kw-name
               keyword)
           ;; else we found a namespace for which we have a prefix...
           (keyword
            (-> namespace'
                ((namespace-to-ns))
                ns-to-prefix)
            (-> value' decode-uri-string encode-kw-name))))))

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
  (as-uri-string (as-kwi this)))

(defmethod as-uri-string :voc/Kwi
  [this]
  {:post [(spec/assert :voc/uri-str-spec %)]}
  (let [ns' (namespace this)
        name' (name this)
        get-ns-set (fn [ns-prefix] (or (-> ns-prefix (prefixed-ns))
                                       ;; auto-resolved keyword...
                                       (->> ns-prefix symbol cljc-find-ns (conj #{}))))]
    (if (and ns' name')
      (-> (str (-> (namespace this)
               decode-kw-ns
               (get-ns-set)
               (unique (partial disambiguate-prefix-ns this))
               ns-to-namespace)
           (-> (name this)
               decode-kw-name
               encode-uri-string)))
      ;; else we're either all ns or all name
      (or (and ns'
               (-> ns' decode-kw-ns encode-uri-string))
          (and name'
               (-> name' decode-kw-name encode-uri-string))))))

(defmethod as-uri-string :voc/LocalFile
  [this]
  {:post [(spec/assert :voc/uri-str-spec %)]}
  (let [s (str this)]
    (as-uri-string
     (if (re-matches ordinary-uri-str-re s)
       s
       (spec/conform :voc/uri-str-spec (str "file:/" s))))))

(defmethod as-uri-string :voc/Qname
  [this]
  {:post [(spec/assert :voc/uri-str-spec %)]}
  (if-let [[_ prefix name'] (re-matches (qname-re) this)]
    (str (-> (unique (prefixed-ns prefix)
                     (partial disambiguate-prefix-ns this))
             ns-to-namespace)
         (-> name'
             (str/replace #"\\/" "/")
             ))
    ;; else it's an angle-quoted URI
    (let [[_ uri-str] (re-matches #"^[<](.*)[>]$" this)]
      uri-str)))

(defmethod as-uri-string :default
  [this]
  (throw (ex-info (str "No `as-uri-string` method for " this)
                  {:type ::no-as-kwi-method
                   ::this this
                   ::resource-type (resource-type this)
                   })))

(defmulti as-qname
  "Returns a qname/CURIE for instance of Resource.
  - Signature: [this] -> `qname`
  - Where
    - `this` is an instance of `Resource`
    - `qname` conforms to `:voc/qname-spec`"
  resource-type)

(defmethod as-qname :voc/Qname
  [this]
  {:post [(spec/assert :voc/qname-spec %)]}
  this)

(defmethod as-qname :voc/Kwi
  [this]
  {:post [(spec/assert :voc/qname-spec %)]}
  (let [prefix (namespace this)
        kw-name (name this)]
    (if (not prefix)
      (str "<" (-> (name this) decode-kw-name encode-uri-string) ">")
      ;; else there is a prefix
      (let [ns' (or (cljc-find-ns (symbol prefix))
                    (unique (prefixed-ns prefix)
                            (partial disambiguate-prefix-ns this)))]
        (when-not ns'
          (throw (ex-info (str "Could not resolve prefix " prefix)
                          {:type ::CouldNotResolvePrefix
                           ::this this
                           ::prefix prefix})))
        (let [qname (str (ns-to-prefix ns')
                         ":"
                         (-> kw-name decode-kw-name encode-uri-string
                             (escape-slash)))]
          (if (not (spec/valid? :voc/qname-spec qname))
            (str "<" (prefix-to-namespace-uri prefix) kw-name ">")
            ;;else valid as qname
            qname))))))

(defmethod as-qname :default
  [this]
  {:post [(spec/assert :voc/qname-spec %)]}
  (as-qname (as-kwi this)))

(defmulti resource=
  "Truthy when two different resource identifiers refer to the same resource.
  - Signature: [this that] -> truthy
  - Where
    - `this` is a Resource
    - `that` is a Resource"
  (fn [this that] [(resource-type this) (resource-type that)]))

(defmethod resource= :default
  [this that]
  (= (as-uri-string this) (as-uri-string that)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PREFIXES FOR SPARQL AND TURTLE SOURCE
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sparql-prefix-declaration
  "Returns PREFIX `prefix`: <`uri`>.
  - Where
    - `prefix` is a prefix in the metadata
    - `uri` is the uri string associated with `prefix` in the metadata"
  [prefix]
  (str "PREFIX "
       prefix
       ": <"
       (prefix-to-namespace-uri prefix)
       ">"))

(defn- turtle-prefix-declaration
  "Returns  @prefix `prefix`: <`uri`>.
  - Where
    - `prefix` is a prefix in the metadata
    - `uri` is the uri string associated with `prefix` in the metadata
  "
  [prefix]
  (str "@prefix "
       prefix
       ": <"
       (prefix-to-namespace-uri prefix)
       ">."))

(defn prefixes-for
  "Returns [`prefix-string`...] for each prefix identified in `content-string`.
  - Where
    - `content-string` is a string of SPARQL, typically without prefixes
    - `prefix-string` := PREFIX `prefix`: `namespace`\n
    - `prefix` is a prefix defined for `namespace` in metadata of some ns with
       `:vann/preferredNamespacePrefix`
    - `namespace` is a namespace defined in the metadata for some ns with
      `:vann/preferredNamespaceUri`"
  ([sparql-string]
   (prefixes-for sparql-prefix-declaration sparql-string))
  ([prefix-fn content-string]
   (map prefix-fn (cljc-find-prefixes (prefix-re-str) content-string))))

(defn sparql-prefixes-for "Gets SPARQL prefixes from `sparql-string`."
  [sparql-string]
  (prefixes-for sparql-prefix-declaration sparql-string))

(defn turtle-prefixes-for "Gets Turtle prefixes from `ttl-string`."
  [ttl-string]
  (prefixes-for turtle-prefix-declaration ttl-string))

(defn prepend-prefix-declarations
  "Returns `content-string`, prepended with appropriate PREFIX decls.
  - Where
    - `content-string` is a string of SPARQL or Turtle/n3, typically without prefixes.
       - default is SPARQL
    - `prefixes-for` := fn [content-string] -> prefixes-string.
      - in practice this would only be needed for `turtle-prefixes-for`
  "
  ([content-string] ;; default content is sparql
   (prepend-prefix-declarations sparql-prefixes-for content-string))

  ([prefixes-for-fn content-string]
   (join "\n" (conj (vec (prefixes-for-fn content-string))
                    content-string))))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Typed Literal support
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tag-dispatch
  "Returns `tag` for `obj` with optional `tag-spec`.
  - Where
    - `obj` is something to be tagged
    - `tag-spec` is a resource, or (@dstr/default-tags (type `obj`))
    - `tag` := (as-kwi `tag-spec`)
  - NOTE: this is the dispatch value for the `tag` method
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
  ([_obj tag-spec]
   (as-kwi tag-spec)))

(defmulti tag
  "Signature [`obj` & maybe `tag-spec`] -> `dstr`.
  - Where
    - `obj` is something to be tagged
    - `tag-spec` is a resource, or (@lstr/default-tags (type `obj`))
    - `dstr` :~ #voc/dstr `obj`^^`tag`
    - `tag` := (as-kwi `tag-spec`)"
  tag-dispatch)

(defmethod tag :xsd/dateTime
  [obj & _]
  (let [obj' (if (string? obj)
               (cljc-read-date obj)
               obj)]
    (dstr/->DatatypeStr (-> obj' cljc-to-date str)
                        "xsd:dateTime")))

(defmethod tag :clj/var
  [obj & _]
  (dstr/->DatatypeStr (if (var? obj)
                        (str (.toSymbol obj))
                        ;; else not a var
                        (str obj))
                      (as-qname :clj/var)))

(defmethod tag :clj/symbol
  [obj & _]
  (dstr/->DatatypeStr (str obj) (as-qname :clj/symbol)))

(defmethod tag :clj/javaClass
  [obj & _]
  (dstr/->DatatypeStr (cljc-get-java-class-tag-name obj) (as-qname :clj/javaClass)))

(defmethod tag :default
  ([obj]
   (tag obj (@dstr/default-tags (type obj))))
  ([obj tag']
   (dstr/->DatatypeStr (str obj) (as-qname tag'))))

(defn untag-dispatch
  "Returns (as-kwi `tag`) for `dstr`.
  - Where
    - `dstr` :~ #voc/dstr `obj`^^`tag`
    - `tag` := (as-qname `tag-spec`)
    "
  [dstr & _]
  (as-kwi (dstr/datatype dstr)))

(defmulti untag
  "Signature: [`dstr` & maybe `on-not-found-fn`?].
  - returns: clojure value appropriate to (as-kwi `tag`)
  - Where
    - `dstr` :~ #voc/dstr `obj`^^`tag`
    - `on-not-found-fn` := fn [dstr] -> clojure value or error.
       - Default is `error-on-no-untag-found`.
  "
  untag-dispatch)

(defmethod untag :clj/javaClass [obj & _] (-> obj str symbol cljc-resolve))
(defmethod untag :clj/symbol    [obj & _] (-> obj str symbol))
(defmethod untag :clj/var       [obj & _] (-> obj str symbol cljc-resolve))
(defmethod untag :xsd/boolean   [obj & _] {:post [(boolean? %)]} (-> obj str read-string))
(defmethod untag :xsd/byte      [obj & _] (-> obj str read-string byte))
(defmethod untag :xsd/dateTime  [obj & _] (-> obj str cljc-read-date))
(defmethod untag :xsd/double    [obj & _] (-> obj str read-string))
(defmethod untag :xsd/float     [obj & _] (-> obj str read-string float))
(defmethod untag :xsd/int       [obj & _] (-> obj str read-string))
(defmethod untag :xsd/integer   [obj & _] (-> obj str read-string))
(defmethod untag :xsd/long      [obj & _] (-> obj str read-string))
(defmethod untag :xsd/short     [obj & _] (-> obj str read-string short))
(defmethod untag :xsd/string    [obj & _] (-> obj str))


(derive :xsd/decimal :xsd/double)


(defn- error-on-no-untag-found
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Establish initial @config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(when (empty? @config)
  (reset! config default-config))

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
     })

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
     })

(put-ns-meta!
 'ont-app.vocabulary.dct
    {
     :dc/title "DCMI Metadata Terms - other"
     :vann/preferredNamespaceUri "http://purl.org/dc/terms/"
     :vann/preferredNamespacePrefix "dct"
     :dcat/downloadURL "http://purl.org/dc/terms/1.1/"
     :voc/appendix [["http://purl.org/dc/elements/1.1/"
                     :dcat/mediaType "text/turtle"]]
     })

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
     })

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
  })

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
     })

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
 'ont-app.vocabulary.void
    {
     :vann/preferredNamespaceUri "http://rdfs.org/ns/void#"
     :vann/preferredNamespacePrefix "void"
     :dc/description "The Vocabulary of Interlinked Datasets (VoID) is an RDF Schema vocabulary for expressing metadata about RDF datasets. It is intended as a bridge between the publishers and users of RDF data, with applications ranging from data discovery to cataloging and archiving of datasets. This document provides a formal definition of the new RDF classes and properties introduced for VoID. It is a companion to the main specification document for VoID, Describing Linked Datasets with the VoID Vocabulary."
     :foaf/homepage "https://www.w3.org/TR/void/"
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

(def ^:deprecated ^:no-doc cljc-put-ns-meta! "Deprecated. Use put-ns-meta!" put-ns-meta!)
(def ^:deprecated ^:no-doc cljc-get-ns-meta "Deprecated. Use get-ns-meta." get-ns-meta)

^:deprecated ^:no-doc
(defmulti uri-str-for
  "Deprecated. Use Resource protocol and `as-uri-string` instead."
  type)

^:deprecated ^:no-doc
(defmethod uri-str-for :default
  [x]
  (as-uri-string x))

(defprotocol ^:deprecated ^:no-doc Resource
  "Deprecated. Use resource-type multimethod instead."
  :extend-via-metadata true
  (resource-class [this]))

(def ^:deprecated ^:no-doc resource-types
  "Deprecated. Use (@voc/config ::voc/resource-types instead)."
  (let [err (ex-info "@Resource-types is no longer used. Use (@voc/config ::voc/resource-types) instead."
                     {:type ::ResourcetypesAtomIsDeprecated})]
    (atom {::context-fn (fn []
                          (throw err))})))

(defn ^:deprecated ^:no-doc qname-for
  "Deprecated. Use `as-qname` instead."
  [kw]
  {:pre [(keyword? kw)]}
  (as-qname kw))

(defn ^:deprecated ^:no-doc keyword-for
  "Deprecated. use `as-kwi` instead."
  ([uri]
   (as-kwi uri)))

(defn ^:deprecated ^:no-doc uri-for
  "Deprecated. Use `as-uri-string` instead."
  [this]
  (as-uri-string this))

#_:clj-kondo/ignore
(def ^:deprecated ^:no-doc iri-for "Deprecated. Use `as-uri-string`." uri-for)
