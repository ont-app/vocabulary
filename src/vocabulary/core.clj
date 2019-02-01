(ns vocabulary.core
  {:doc "Defines a set of namespaces for commonly used linked data constructs, metadata of which specifies RDF namespaces, prefixes and other details."
   :sh:prefix "voc"
   :sh:namespace "http://rdf.naturallexicon.org/ont-app/vocabulary/"
   }
  (:require
   [clojure.string :as s]
   [clojure.set :as set]
   ))       


;; (def ontology (add (make-graph)
;;                    {:voc/appendix {:rdf/type :rdf:Property
;;                                    :rdfs/comment "<ns> :voc/appendix <triples>
;; Asserts that <triples> describe a graph that elaborates on other attributes asserted in usual key-value metadata asserted for <ns>, e.g. asserting a dcat:mediaType relation for some dcat:downloadURL.
;; "
;;                                    }}))


(def appendix {:rdf:type :rdf:Property
               :rdfs:comment "<ns> :voc:appendix <triples>
Asserts that <triples> describe a graph that elaborates on other attributes asserted in usual key-value metadata asserted for <ns>, e.g. asserting a dcat:mediaType relation for some dcat:downloadURL.
"
               })

(defn- collect-prefixes [acc next-ns]
  "Returns {<prefix> <namespace> ...} for <next-ns>
Where
<prefix> is a prefix declared in the metadata of <next-ns>
<namespace> is a URI namespace declared for <prefix> in metadata of <next-ns>
<next-ns> is typically an element in a reduction sequence of ns's 
"
  {:pre (map? acc)
   }
  (let [nsm (meta next-ns)
        ]
    (if-let [p (:sh:prefix nsm)]
      (if (set? p)
        (reduce (fn [acc v] (assoc acc v next-ns)) acc p)
        (assoc acc p next-ns))
      acc)))


(defn prefix-to-ns []
  "Returns {<prefix> <ns> ...}
Where 
<prefix> is declared in metadata for some <ns> with :sh:prefix 
<ns> is an instance of clojure.lang.ns available within the lexical 
  context in which the  call was made.
"
  (reduce collect-prefixes {} (all-ns)))


(defn ns-to-namespace [ns]
  "Returns <iri> for <ns>
Where
<iri> is an iri declared with :sh:namespace in the metadata for <ns>, or nil
<ns> is an instance of clojure.lang.Namespace
"
  (:sh:namespace (meta ns)))

(defn namespace-to-ns []
  "returns {<namespace> <ns> ...}
"
  (let [maybe-mapping (fn [_ns]
                        (if-let [namespace (:sh:namespace (meta _ns))
                                 ]
                          [namespace _ns]))
        ]
    (into {}
          (filter identity
                  (map maybe-mapping
                       (all-ns))))))

(def voc-re #"^([a-zA-Z]+)[:](.*)")

(defn iri-for [kw]
  "Returns <iri>  for `kw` based on metadata attached to <ns>
Where
<iri> is of the form <namespace><value>
<kw> is a keyword of the form <prefix>:<value>
<ns> is an instance of clojure.lang.ns
<prefix> is declared with :sh:prefix in metadata of <ns>
<namespace> is of the form http://...., declared with :sh:namespace in 
  metadata of <ns>
"
  {:pre (keyword? kw)
   }
  (if-let [[_ prefix value] (re-matches voc-re (name kw))
           ]
    (let [iri (-> prefix
                  ((prefix-to-ns))
                  ns-to-namespace)
          ]
      (if-not iri
        (throw (Exception. (str "No URI declared for prefix '" prefix "'")))
        (str iri
             value)))
    ;; else we did not match voc-re, check the aliases
    (if-let [_ns (namespace kw)
             ]
      (let [iri (->> _ns
                     (symbol)
                     (get (ns-aliases *ns*))
                     (ns-to-namespace))
            ]
        (str iri (name kw))))))

(defn ns-to-prefix [_ns]
  (:sh:prefix (meta _ns)))

(defn qname-for [kw]
  (if-let [[_ prefix value] (re-matches voc-re (name kw))
           ]
    (do 
      (if (not (contains? (prefix-to-ns) prefix))
        (throw (Exception.
                (str "No URI declared for prefix '"
                     prefix
                     "' in any namespace."))))
      (str prefix ":" value))
    ;; else we did not match voc-re, check the aliases
    (if-let [_ns (namespace kw)
             ]
      (str (->> _ns
                (symbol)
                (get (ns-aliases *ns*))
                (ns-to-prefix))
           ":"
           (name kw)))))
  
  

(defn namespace-re []
  (let [namespace< (fn [a b] ;; match longer first
                     (def ab [a b])
                     (> (count a)
                        (count b)))
        ]
    (re-pattern (str "^("
                     (s/join "|" (sort namespace<
                                       (keys (namespace-to-ns))))
                     ")(.*)"))))

(defn keyword-for [uri]
  {:pre [(string? (let [_uri uri] (def __uri _uri) uri))]
   }
  (let [[_ namespace value] (re-matches (namespace-re) uri)
        ]
    (if (not namespace)
      (keyword value)
      (keyword (str (-> namespace
                        ((namespace-to-ns))
                        meta
                        :sh:prefix)
                    ":"
                    value
                    )))))

(defn- prefix-re []
  "Returns a regex that recognizes prefixes declared in ns metadata with :sh:prefix keys.
"
  (re-pattern
   (str "[^a-zA-Z]+("
        (s/join "|" (keys (prefix-to-ns)))
        "):")))

(defn- find-prefixes [s]
  "Returns #{<prefix>...} for <s>
Where
<prefix> is a prefix found in <s>, for which some (meta ns) has a 
  :sh:prefix declaration
<s> is a string, typically a SPARQL query body for which we want to 
  infer prefix declarations.
"
  (let [prefixes (re-matcher (prefix-re) s)
        ]
    (loop [acc #{}
           next-match (re-find prefixes)]
      (if (not next-match)
        acc
        (let [[_ prefix] next-match]
          (recur (conj acc prefix)
                 (re-find prefixes)))))))

(defn sparql-prefixes-for [sparql-string]
  "Returns [<prefix-string>...] for each prefix identified in <sparql-string>
Where
<prefix-string> := PREFIX <prefix>: <namespace>\n
<prefix> is a prefix defined for <namespace> in metadata of some ns with :sh:prefix
<namespace> is a namespace defined in the metadata for some ns with :sh:namespace
"
  (let [sparql-prefix-for (fn [prefix]
                            (str "PREFIX "
                                 prefix
                                 ": <"
                                 (ns-to-namespace
                                  ((prefix-to-ns) prefix))
                                 ">"))
        ]
    (map sparql-prefix-for (find-prefixes sparql-string))))

(defn prepend-prefix-declarations [sparql-string]
  (s/join "\n" (conj (vec (sparql-prefixes-for sparql-string))
                     sparql-string)))


;;; NAMESPACE DECLARATIONS

(ns iri.org.w3.www.1999.02.22-rdf-syntax-ns
  {
  :dc:title "The RDF Concepts Vocabulary (RDF)" 
  :dc:description "This is the RDF Schema for the RDF vocabulary terms in the RDF Namespace, defined in RDF 1.1 Concepts." 
   :sh:namespace "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
   :sh:prefix "rdf"
   :dcat:downloadURL "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
   :voc:appendix [["http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                   :dcat:mediaType "text/turtle"]]
   }
  )

(ns iri.org.w3.www.2000.01.rdf-schema
  {
   :dc:title "The RDF Schema vocabulary (RDFS)"
   :sh:namespace "http://www.w3.org/2000/01/rdf-schema#"
   :sh:prefix "rdfs"
   :dcat:downloadURL "http://www.w3.org/2000/01/rdf-schema#"
   :voc:appendix [["http://www.w3.org/2000/01/rdf-schema#"
                   :dcat:mediaType "text/turtle"]]
   }
  )

(ns iri.org.purl.dc.elements.1.1
  {
   :dc:title "Dublin Core Metadata Element Set, Version 1.1"
   :sh:namespace "http://purl.org/dc/elements/1.1/"
   :sh:prefix "dc"
   :dcat:downloadURL "http://purl.org/dc/elements/1.1/"
   :voc:appendix [["http://purl.org/dc/elements/1.1/"
                   :dcat:mediaType "text/turtle"]]
   }
  )

(ns iri.org.purl.dc.terms.1.1
  {
   :dc:title "DCMI Metadata Terms - other"
   :sh:namespace "http://purl.org/dc/elements/1.1/"
   :sh:prefix "dct"
   :dcat:downloadURL "http://purl.org/dc/terms/1.1/"
   :voc:appendix [["http://purl.org/dc/elements/1.1/"
                   :dcat:mediaType "text/turtle"]]
   }
  )

(ns iri.org.w3.www.ns.shacl
  {
   :rdfs:label "W3C Shapes Constraint Language (SHACL) Vocabulary"
   :rdfs:comment "This vocabulary defines terms used in SHACL, the W3C Shapes Constraint Language."
   :sh:namespace "http://www.w3.org/ns/shacl#"
   :sh:prefix "sh"
   :dcat:downloadURL "https://www.w3.org/ns/shacl.ttl"
   })


(ns iri.org.w3.www.ns.dcat
  {
   :dc:title "Data Catalog vocabulary"
   :dcat:downloadURL "https://www.w3.org/ns/dcat.ttl"
   :sh:prefix "dcat"
   :sh:namespace "http://www.w3.org/ns/dcat#"
   }
  )
(ns iri.org.w3.www.2002.07.owl  
{
 :dc:title "The OWL 2 Schema vocabulary (OWL 2)"
 :rdfs:comment "This ontology partially describes the built-in classes and
  properties that together form the basis of the RDF/XML syntax of OWL 2.
  The content of this ontology is based on Tables 6.1 and 6.2
  in Section 6.4 of the OWL 2 RDF-Based Semantics specification,
  available at http://www.w3.org/TR/owl2-rdf-based-semantics/.
  Please note that those tables do not include the different annotations
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
 :sh:namespace "http://www.w3.org/2002/07/owl#"
 :sh:prefix "owl"
 :dcat:downloadURL "http://www.w3.org/2002/07/owl"
 :voc:appendix [["http://www.w3.org/2002/07/owl"
                 :dcat:mediaType "text/turtle"]]
 }
)

(ns iri.com.xmlns.foaf.0.1
{
 :dc:title "Friend of a Friend (FOAF) vocabulary"
 :dc:description "The Friend of a Friend (FOAF) RDF vocabulary, described using W3C RDF Schema and the Web Ontology Language."
 :sh:namespace "http://xmlns.com/foaf/0.1/"
 :sh:prefix "foaf"
 :dcat:downloadURL "http://xmlns.com/foaf/spec/index.rdf"
 :voc:appendix [["http://xmlns.com/foaf/spec/index.rdf"
                 :dcat:mediaType "application/rdf+xml"]]
 }
)

(ns iri.org.w3.www.2004.02.skos.core
  {
   :dc:title "SKOS Vocabulary"
   :dc:description "An RDF vocabulary for describing the basic structure and content of concept schemes such as thesauri, classification schemes, subject heading lists, taxonomies, 'folksonomies', other types of controlled vocabulary, and also concept schemes embedded in glossaries and terminologies."
   :sh:namespace "http://www.w3.org/2004/02/skos/core#"
   :sh:prefix "skos"
   :foaf:homepage "https://www.w3.org/2009/08/skos-reference/skos.html"
   :dcat:downloadURL "https://www.w3.org/2009/08/skos-reference/skos.rdf"
   :voc:appendix [["https://www.w3.org/2009/08/skos-reference/skos.rdf"
                   :dcat:mediaType "application/rdf+xml"]]   
   }
  )

;; PREFIX schema: <http://schema.org/>

(ns iri.org.schema
  {
   :sh:namespace "http://schema.org/"
   :sh:prefix "schema"
   :dc:description "Schema.org is a collaborative, community activity with a mission to create, maintain, and promote schemas for structured data on the Internet, on web pages, in email messages, and beyond. "
   :foaf:homepage "https://schema.org/"
   :dcat:downloadURL #{"http://schema.org/version/latest/schema.ttl"
                       "http://schema.org/version/latest/schema.jsonld"}
   :voc:appendix [["http://schema.org/version/latest/schema.ttl"
                   :dcat:mediaType "text/turtle"]
                  ["http://schema.org/version/latest/schema.jsonld"
                   :dcat:mediaType "application/ld+json"]]
   })
