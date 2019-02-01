(ns vocabulary.wikidata
  {:doc "Wikidata-related vocabulary"
   })


(ns iri.org.wikidata.www.entity
  {
   :dc:title "Wikibase/EntityData"
   :foaf:homepage "https://www.mediawiki.org/wiki/Wikibase/EntityData"
   :sh:namespace "http://www.wikidata.org/entity/"
   :sh:prefix "wd"
   }
  )

(ns iri.org.wikidata.www.prop.direct
  {
   :dc:description "Direct properties in wikibase"
   :sh:namespace "http://www.wikidata.org/prop/direct/"
   :sh:prefix "wdt"
   :rdfs:seeAlso :wikibase/directClaim
   }
  )

(ns iri.se.wikiba.ontology
  {
   :rdfs:label "Wikibase system ontology"
   :sh:namespace "http://wikiba.se/ontology#"
   :sh:prefix "wikibase"
   :rdfs:isDefinedBy "http://wikiba.se/ontology-1.0.owl"
   }
  )

(ns iri.org.wikidata.www.prop
  {
   :rdfs:comment "Reifies wikibase properties"
   :sh:namespace "http://www.wikidata.org/prop/"
   :sh:prefix "p"
   :foaf:homepage "https://www.wikidata.org/wiki/Help:Properties"
   }
  )

(ns iri.org.wikidata.www.statement
  {
   :sh:namespace "http://www.wikidata.org/prop/statement/"
   :sh:prefix #{"v" "ps"}
   :foaf:homepage "https://www.wikidata.org/wiki/Help:Statements"
   }
  )

(ns iri.org.wikidata.www.qualifier
  {
   :sh:namespace "http://www.wikidata.org/prop/qualifier"
   :sh:prefix "q"
   :foaf:homepage "https://www.wikidata.org/wiki/Help:Qualifiers"
   }
  )

;; THESE NAMESPACES ARE RELATIVELY RARE
;; BUT SHOW UP IN https://www.mediawiki.org/wiki/Wikibase/Indexing/RDF_Dump_Format#Full_list_of_prefixes

;; PREFIX wdtn: <http://www.wikidata.org/prop/direct-normalized/>
(ns iri.org.wikidata.www.prop.direct-normalized
  {
   :sh:namespace "http://www.wikidata.org/prop/direct-normalized/"
   :sh:prefix "wdtn"
   }
  )
;;  PREFIX wds: <http://www.wikidata.org/entity/statement/>
(ns iri.org.wikidata.www.entity.statement
  {
   :sh:namespace "http://www.wikidata.org/entity/statement/"
   :sh:prefix "wds"
   }
  )

;;  PREFIX wdref: <http://www.wikidata.org/reference/>
(ns iri.org.wikidata.www.reference
  {
   :sh:namespace "http://www.wikidata.org/reference/"
   :sh:prefix "wdref"
   }
   )
  
;; PREFIX wdv: <http://www.wikidata.org/value/>
(ns iri.org.wikidata.www.value
  {
   :sh:namespace "http://www.wikidata.org/value/"
   :sh:prefix "wdv"
   }
  )

;; PREFIX psv: <http://www.wikidata.org/prop/statement/value/>
(ns iri.org.wikidata.www.prop.statement.value
  {
   :sh:namespace "http://www.wikidata.org/prop/statement/value/"
   :sh:prefix "psv"
   }
  )

;; PREFIX psn: <http://www.wikidata.org/prop/statement/value-normalized/>
(ns iri.org.wikidata.www.prop.statement.value-normalized
  {
   :sh:namespace "http://www.wikidata.org/prop/statement/value-normalized/"
   :sh:prefix "psn"
   }
  )

;; PREFIX pq: <http://www.wikidata.org/prop/qualifier/>
(ns iri.org.wikidata.www.prop.qualifier
  {
   :sh:namespace "http://www.wikidata.org/prop/qualifier/"
   :sh:prefix "pq"
   }
  )

;; PREFIX pqv: <http://www.wikidata.org/prop/qualifier/value/>
(ns iri.org.wikidata.www.prop.qualifier.value
  {
   :sh:namespace "http://www.wikidata.org/prop/qualifier/value/"
   :sh:prefix "pqv"
   }
  )

;; PREFIX pqn: <http://www.wikidata.org/prop/qualifier/value-normalized/>
(ns iri.org.wikidata.www.prop.qualifier.value-normalized
  {
   :sh:namespace "http://www.wikidata.org/prop/qualifier/value-normalized/"
   :sh:prefix "pqn"
   }
  )

;; PREFIX pr: <http://www.wikidata.org/prop/reference/>
(ns iri.org.wikidata.www.prop.reference
  {
   :sh:namespace "http://www.wikidata.org/prop/reference/"
   :sh:prefix "pr"
   }
  )

;; PREFIX prv: <http://www.wikidata.org/prop/reference/value/>
(ns iri.org.wikidata.www.prop.reference.value
  {
   :sh:namespace "http://www.wikidata.org/prop/reference/value/"
   :sh:prefix "prv"
   }
  )

;; PREFIX prn: <http://www.wikidata.org/prop/reference/value-normalized/>
(ns iri.org.wikidata.www.prop.reference.value-normalized
  {
   :sh:namespace "http://www.wikidata.org/prop/reference/value-normalized/"
   :sh:prefix "prn"
   }
  )

 ;; PREFIX wdno: <http://www.wikidata.org/prop/novalue/>
(ns iri.org.wikidata.www.prop.novalue
  {
   :sh:namespace "http://www.wikidata.org/prop/novalue/"
   :sh:prefix "wdno"
   }
  )
