(ns ont-app.vocabulary.linguistics
  {:doc "Defines a set of namespaces relating to linguistics"
   }
  (:require
   [clojure.string :as s]
   [clojure.set :as set]
   [ont-app.vocabulary.core :as voc]
   ))       


;; LEMON/ONTOLEX
(voc/put-ns-meta!
 'ont-app.vocabulary.linguistics.ontolex
  {:dc/title "ontolex"
   :dc/description "A model for the representation of lexical information relative to ontologies. Core module."
   :vann/preferredNamespacePrefix "ontolex"
   :vann/preferredNamespaceUri "http://www.w3.org/ns/lemon/ontolex#"
   :foaf/homepage "https://www.w3.org/2016/05/ontolex/"
   :dcat/downloadURL "http://www.w3.org/ns/lemon/ontolex#"
   :voc/appendix [["http://www.w3.org/ns/lemon/ontolex#"
                   :dcat/mediaType "application/rdf+xml"]]
   }
  )

(voc/put-ns-meta!
 'ont-app.vocabulary.linguistices.iso639
  {:dc/title "iso 639"
   :dc/description "Identifies the various language tags"
   :vann/preferredNamespacePrefix "iso639"
   :vann/preferredNamespaceUri "http://www.lexvo.org/page/iso639-3/"
   :foaf/homepage "http://www.lexvo.org/"
   })

(voc/put-ns-meta!
 'ont-app.vocabulary.linguistics.pmn
  {
   :dc/title "Predicate Model for Ontologies (PreMOn)"
   :dc/description "The PreMOn Ontology is an extension of lemon (W3C Ontology Lexicon Community Group, 2015) for representing predicate models and their mappings. The Core Module of the PreMOn Ontology defines the main abstractions for modelling semantic classes with their semantic roles, mappings between different predicate models, and annotations."
   :vann/preferredNamespacePrefix "pmn"
   :vann/preferredNamespaceUri "http://premon.fbk.eu/ontology/core#"
   :foaf/homepage "http://premon.fbk.eu/ontology/core.html"
   :owl/seeAlso "http://www.w3.org/ns/lemon/ontolex#"
   :dcat/downloadURL #{"http://premon.fbk.eu/ontology/core.ttl"
                       "http://premon.fbk.eu/ontology/core.owl"
                       "http://premon.fbk.eu/ontology/core.nt"
                       }
   :voc/appendix [["http://premon.fbk.eu/ontology/core.ttl"
                   :dcat/mediaType "text/tutle"
                   ]
                  ["http://premon.fbk.eu/ontology/core.owl"
                   :dcat/mediaType "application/owl+xml"
                   ]
                  ["http://premon.fbk.eu/ontology/core.nt"
                   :dcat/mediaType "application/n-triples"
                   ]]
   }
  )

;; NATURAL LANGUAGE INTERCHANGE FORMAT (NIF)

(voc/put-ns-meta!
 'ont-app.vocabulary.linguistics.nif
  {
   :dc/title "Natural Language Interchange Format (NIF)"
   :dc/description "The NLP Interchange Format (NIF) is an RDF/OWL-based format that aims to achieve interoperability between Natural Language Processing (NLP) tools, language resources and annotations. NIF consists of specifications, ontologies and software, which are combined under the version identifier "2.0", but are versioned individually. This ontology is developed by the NLP2RDF project (http://nlp2rdf.org) and provided as part of NIF 2.0 under CC-BY license as well as Apache 2.0. The ontology contains seven core URIs (String, RFC5147String, Context, isString, referenceContext, beginIndex, endIndex) that provide the foundation to express NLP annotations effectively in RDF."
   :vann/preferredNamespacePrefix "nif"
   :vann/preferredNamespaceUri "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#"
   :foaf/homepage #{"https://github.com/NLP2RDF",
                    "http://persistence.uni-leipzig.org/nlp2rdf/specification/version/version-1.0.0-rc1.html"
                    "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html"
                    }
   }
  )


;; NATURAL LEXICON
;; Namespaces native to naturallexicon.org and the ont-app project

(voc/put-ns-meta!
 'ont-app.vocabulary.linguistics.natlex.ont
  {
   :dc/description "The home of ontological constructs that apply to no particular language."
   :vann/preferredNamespacePrefix "natlex"
   :vann/preferredNamespaceUri
   "http://rdf.naturallexicon.org/ont#"
   
   })

;; ENGLISH

(voc/put-ns-meta!
 'ont-app.vocabulary.linguistics.natlex.en.ont
  {
   :dc/description "The home of ontological constructs relating to English"
   :vann/preferredNamespacePrefix "en"
   :vann/preferredNamespaceUri
   "http://rdf.naturallexicon.org/en/ont#"
   }
  )

(voc/put-ns-meta!
 'ont-app.vocabulary.linguistics.natlex.en.form
  {
   :dc/description "A container for English word forms"
   :vann/preferredNamespacePrefix "enForm"
   :vann/preferredNamespaceUri
   "http://rdf.naturallexicon.org/en/form/"
   }
  )

;; CHINESE

(voc/put-ns-meta!
 'ont-app.vocabulary.linguistics.natlex.zh.ont
  {
   :dc/description "The home of ontological constructs relating to Chinese"
   :vann/preferredNamespacePrefix "zh"
   :vann/preferredNamespaceUri
   "http://rdf.naturallexicon.org/zh/ont#"
   }
  )

(voc/put-ns-meta!
 'ont-app.vocabulary.linguistics.natlex.zh.written
  {
   :dc/description "A container for Chinese written forms"
   :vann/preferredNamespacePrefix "hanzi"
   :vann/preferredNamespaceUri
   "http://rdf.naturallexicon.org/zh/written/"
   }
  )
    

(voc/put-ns-meta!
 'ont-app.vocabulary.linguistics.natlex.zh.cmn
  {
   :dc/description "A container for Mandarin forms"
   :vann/preferredNamespacePrefix "cmn"
   :vann/preferredNamespaceUri
   "http://rdf.naturallexicon.org/zh/cmn/"
   }
  )


