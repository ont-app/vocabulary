(ns vocabulary.core
  {:doc "Defines utilities and a set of namespaces for commonly used linked data constructs, metadata of which specifies RDF namespaces, prefixes and other details."
   :vann/preferredNamespacePrefix "voc"
   :vann/preferredNamespaceUri
   "http://rdf.naturallexicon.org/ont-app/vocabulary/"
   }
  (:require
   [clojure.string :as s]
   [clojure.set :as set]
   ))       


(def ontology
  "I'm still kinda spitballing here"
  {:voc/appendix
   {:rdf/type #{:rdf:Property}
    :rdfs/comment #{"<ns> :voc/appendix <triples>
 Asserts that <triples> describe a graph that elaborates on other attributes asserted in usual key-value metadata asserted for <ns>, e.g. asserting a dcat:mediaType relation for some dcat:downloadURL.
 "}
    }})

(defn- collect-prefixes
  "Returns {<prefix> <namespace> ...} s.t. <next-ns> is included
Where
<prefix> is a prefix declared in the metadata of <next-ns>
<namespace> is a URI namespace declared for <prefix> in metadata of <next-ns>
<next-ns> is typically an element in a reduction sequence of ns's 
"
  {:test #(assert
           (= (collect-prefixes {}
                                (find-ns 'org.naturallexicon.lod.foaf))
              {"foaf" (find-ns 'org.naturallexicon.lod.foaf)}))
   }
  [acc next-ns]
  {:pre (map? acc)
   }
  (let [nsm (meta next-ns)
        ]
    (if-let [p (:vann/preferredNamespacePrefix nsm)]
      (if (set? p)
        (reduce (fn [acc v] (assoc acc v next-ns)) acc p)
        (assoc acc p next-ns))
      acc)))


(defn prefix-to-ns []
  "Returns {<prefix> <ns> ...}
Where 
<prefix> is declared in metadata for some <ns> with 
  :vann/preferredNamespacePrefix 
<ns> is an instance of clojure.lang.ns available within the lexical 
  context in which the  call was made.
"
  (reduce collect-prefixes {} (all-ns)))


(defn ns-to-namespace 
  "Returns <iri> for <ns>
Where
<iri> is an iri declared with :vann/preferredNamespaceUri in the metadata for 
  <ns>, or nil
<ns> is an instance of clojure.lang.Namespace
"
  {:test #(assert
           (= (ns-to-namespace (find-ns 'org.naturallexicon.lod.foaf))
              "http://xmlns.com/foaf/0.1/"))
   }
  [ns]
  (:vann/preferredNamespaceUri (meta ns)))

(defn namespace-to-ns  []
  "returns {<namespace> <ns> ...} for each ns with :vann/preferredNamespaceUri
declaration
"
  (let [maybe-mapping (fn [_ns]
                        (if-let [namespace (:vann/preferredNamespaceUri
                                            (meta _ns))
                                 ]
                          [namespace _ns]))
        ]
    (into {}
          (filter identity
                  (map maybe-mapping
                       (all-ns))))))

(defn- aliased-ns [prefix]
  "Returns nil or the ns whose aliase is `prefix`
Where
<prefix> is a string, typically parsed from a keyword.
"
  {:pre [(string? prefix)]
   }
  (->> prefix
       (symbol)
       (get (ns-aliases *ns*))))

(defn- prefixed-ns [prefix]
  "Returns nil or the ns whose `prefix` was declared in metadata with :vann/preferredNamespacePrefix
Where
<prefix> is a string, typically parsed from a keyword.
"
  {:pre [(string? prefix)]
   }
  (get (prefix-to-ns) prefix))


(defn iri-for 
  "Returns <iri>  for `kw` based on metadata attached to <ns>
Where
<iri> is of the form <namespace><value>
<kw> is a keyword of the form <prefix>:<value>
<ns> is an instance of clojure.lang.ns
<prefix> is declared with :vann/preferredNamespacePrefix in metadata of <ns>
<namespace> is typically of the form http://...., declared with 
  :vann/preferredNamespaceUri in metadata of <ns>
"
  {:test #(do (assert
               (= (iri-for :foaf/homepage)
                  "http://xmlns.com/foaf/0.1/homepage"))
              (assert
               (= (iri-for ::blah)
                  "http://rdf.naturallexicon.org/ont-app/vocabulary/blah"))
              (assert
               (= (iri-for :http://blah)
                  "http://blah")))
   }
  [kw]
   {:pre [(keyword? kw)]
   }
  (if (#{"http:" "https:" "file:"} (namespace kw)) ;; uri scheme http://...
    (str (namespace kw) "/" (name kw))
    (if-let [prefix (namespace kw)
             ]
      (let [_ns (or (find-ns (symbol prefix)) ;; ::keyword
                    (aliased-ns prefix)
                    (prefixed-ns prefix))]
        (if-not _ns
          (throw (Exception. (str "No URI declared for prefix '" prefix "'")))
          (str (-> _ns
                   (ns-to-namespace))
               (name kw))))
      (throw (Exception. (str "Could not find IRI for " kw))))))


(defn ns-to-prefix 
  "Returns the prefix associated with `_ns`
Where
<_ns> is a clojure namespace, which may have :vann/preferredNamespacePrefix
  declaration in its metadata.   
"
  {:test #(assert
           (= (ns-to-prefix (find-ns 'org.naturallexicon.lod.foaf))
              "foaf"))
   }
  [_ns]
  (:vann/preferredNamespacePrefix (meta _ns)))

(defn qname-for 
  "Returns the 'qname' URI for `kw`, or nil if ns metadata is missing. 
Where
  <kw> is a keyword, in a namespace with LOD declarations in its metadata.
"
  {:test #(do
            (assert
             (or (not= *ns* (find-ns 'vocabulary.core))
                 (= (qname-for ::blah)
                    "voc:blah")))
            (assert
             (= (qname-for :foaf/homepage)
                "foaf:homepage")))
   }
  [kw]
  {:pre [(keyword? kw)
         ]
   }
  (if-let [prefix (namespace kw)
           ]
    (if (#{"http:" "https:" "file:"} prefix) ;; this is a scheme, not a namespace
      (str "<" prefix "/" (name kw) ">")
      (let [_ns (or (find-ns (symbol prefix))
                    (aliased-ns prefix)
                    (prefixed-ns prefix))
            ]
        (if-not _ns
          (throw (Exception. (str "Could not resolve prefix " prefix))))
        
        (str (ns-to-prefix _ns)
             ":"
             (name kw))))
    ;; else no namespace
    (str "<" (name kw) ">")))

  

(defn namespace-re []
  "Returns a regex to recognize substrings matching a URI for an ns 
  declared with LOD metadata. Groups for namespace and value.
"
  (let [namespace< (fn [a b] ;; match longer first
                     (> (count a)
                        (count b)))
        ]
    (re-pattern (str "^("
                     (s/join "|" (sort namespace<
                                       (keys (namespace-to-ns))))
                     ")(.*)"))))

(defn keyword-for 
  "Returns a keyword equivalent of <uri>, properly prefixed if LOD declarations
  exist in some ns in the current lexical environment.
"
  {:test #(assert
           (= (keyword-for "http://xmlns.com/foaf/0.1/homepage")
              :foaf/homepage))
   }
  [uri]
  {:pre [(string? uri)]
   }
  (let [[_ namespace value] (re-matches (namespace-re) uri)
        ]
    (if (not value)
      (keyword uri)
      (if (not namespace)
        (keyword value)
        (keyword (-> namespace
                     ((namespace-to-ns))
                     meta
                     :vann/preferredNamespacePrefix)
                 value
                 )))))

(defn- prefix-re []
  "Returns a regex that recognizes prefixes declared in ns metadata with 
  :vann/preferredNamespacePrefix keys
"
  (re-pattern
   (str "[^a-zA-Z]+("
        (s/join "|" (keys (prefix-to-ns)))
        "):")))

(defn- find-prefixes 
  "Returns #{<prefix>...} for `s`
Where
<prefix> is a prefix found in <s>, for which some (meta ns) has a 
  :vann/preferredNamespacePrefix declaration
<s> is a string, typically a SPARQL query body for which we want to 
  infer prefix declarations.
"
  {:test #(assert
           (= (find-prefixes "Select * Where{?s foaf:homepage ?homepage}")
              #{"foaf"}))
   }
  
  [s]
  (let [prefixes (re-matcher (prefix-re) s)
        ]
    (loop [acc #{}
           next-match (re-find prefixes)]
      (if (not next-match)
        acc
        (let [[_ prefix] next-match]
          (recur (conj acc prefix)
                 (re-find prefixes)))))))

(defn sparql-prefixes-for 
  "Returns [<prefix-string>...] for each prefix identified in <sparql-string>
Where
<prefix-string> := PREFIX <prefix>: <namespace>\n
<prefix> is a prefix defined for <namespace> in metadata of some ns with 
  :vann/preferredNamespacePrefix
<namespace> is a namespace defined in the metadata for some ns with 
  :vann/preferredNamespaceUri
"
  {:test #(assert
           (=
            (sparql-prefixes-for
             "Select * Where{?s foaf:homepage ?homepage}")
            (list "PREFIX foaf: <http://xmlns.com/foaf/0.1/>")))
   }
  [sparql-string]
  (let [sparql-prefix-for (fn [prefix]
                            (str "PREFIX "
                                 prefix
                                 ": <"
                                 (ns-to-namespace
                                  ((prefix-to-ns) prefix))
                                 ">"))
        ]
    (map sparql-prefix-for (find-prefixes sparql-string))))

(defn prepend-prefix-declarations 
  "Returns <sparql-string>, prepended with appropriate PREFIX decls.
"
  {:test #(assert
           (= (prepend-prefix-declarations
               "Select * Where{?s foaf:homepage ?homepage}")
              "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\nSelect * Where{?s foaf:homepage ?homepage}"))
               
   }
  [sparql-string]
  
  (s/join "\n" (conj (vec (sparql-prefixes-for sparql-string))
                     sparql-string)))


;;; NAMESPACE DECLARATIONS
;;; These are commonly used RDF namespaces.

(ns org.naturallexicon.lod.rdf
  {
  :dc/title "The RDF Concepts Vocabulary (RDF)" 
  :dc/description "This is the RDF Schema for the RDF vocabulary terms
  in the RDF Namespace, defined in RDF 1.1 Concepts."
   :vann/preferredNamespaceUri "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
   :vann/preferredNamespacePrefix "rdf"
   :foaf/homepage "https://www.w3.org/2001/sw/wiki/RDF"
   :dcat/downloadURL "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
   :voc/appendix [["http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                   :dcat/mediaType "text/turtle"]]
   }
  )

(ns org.naturallexicon.lod.rdf-schema
  {
   :dc/title "The RDF Schema vocabulary (RDFS)"
   :vann/preferredNamespaceUri "http://www.w3.org/2000/01/rdf-schema#"
   :vann/preferredNamespacePrefix "rdfs"
   :foaf/homepage "https://www.w3.org/TR/rdf-schema/"
   :dcat/downloadURL "http://www.w3.org/2000/01/rdf-schema#"
   :voc/appendix [["http://www.w3.org/2000/01/rdf-schema#"
                   :dcat/mediaType "text/turtle"]]
   }
  )

(ns org.naturallexicon.lod.owl  
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

(ns org.naturallexicon.lod.vann
{
  :rdfs/label "VANN"
  :dc/description "A vocabulary for annotating vocabulary descriptions"
  :vann/preferredNamespaceUri "http://purl.org/vocab/vann"
  :vann/peferredNamespacePrefix "vann"
  :foaf/homepage "http://vocab.org/vann/"
})

(ns org.naturallexicion.lod.dc
  {
   :dc/title "Dublin Core Metadata Element Set, Version 1.1"
   :vann/preferredNamespaceUri "http://purl.org/dc/elements/1.1/"
   :vann/preferredNamespacePrefix "dc"
   :dcat/downloadURL "http://purl.org/dc/elements/1.1/"
   :voc/appendix [["http://purl.org/dc/elements/1.1/"
                   :dcat/mediaType "text/turtle"]]
   }
  )

(ns org.naturallexicon.lod.dct
  {
   :dc/title "DCMI Metadata Terms - other"
   :vann/preferredNamespaceUri "http://purl.org/dc/elements/1.1/"
   :vann/preferredNamespacePrefix "dct"
   :dcat/downloadURL "http://purl.org/dc/terms/1.1/"
   :voc/appendix [["http://purl.org/dc/elements/1.1/"
                   :dcat/mediaType "text/turtle"]]
   }
  )

(ns org.naturallexicon.lod.shacl
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


(ns org.naturallexicon.lod.dcat
  {
   :dc/title "Data Catalog vocabulary"
   :foaf/homepage "https://www.w3.org/TR/vocab-dcat/"
   :dcat/downloadURL "https://www.w3.org/ns/dcat.ttl"
   :vann/preferredNamespacePrefix "dcat"
   :vann/preferredNamespaceUri "http://www.w3.org/ns/dcat#"
   }
  )


(ns org.naturallexicon.lod.foaf
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

(ns org.naturallexicon.lod.skos
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


(ns org.naturallexicon.lod.schema
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

(ns org.naturallexicon.lod.xsd
  {
   :dc/description "Offers facilities for describing the structure and
   constraining the contents of XML and RDF documents"
   :vann/preferredNamespaceUri "http://www.w3.org/2001/XMLSchema#"
   :vann/preferredNamespacePrefix "xsd"
   :foaf/homepage "https://www.w3.org/2001/XMLSchema"
   })
