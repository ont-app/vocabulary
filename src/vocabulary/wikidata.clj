(ns vocabulary.wikidata
  {:doc "Wikidata-related vocabulary"
   })


(ns org.naturallexicon.lod.wikidata.wd
  {
   :dc/title "Wikibase/EntityData"
   :foaf/homepage "https://www.mediawiki.org/wiki/Wikibase/EntityData"
   :vann/preferredNamespaceUri "http://www.wikidata.org/entity/"
   :vann/preferredNamespacePrefix "wd"
   }
  )

(ns org.naturallexicon.lod.wikidata.wdt
  {
   :dc/description "Direct properties in wikibase"
   :vann/preferredNamespaceUri "http://www.wikidata.org/prop/direct/"
   :vann/preferredNamespacePrefix "wdt"
   :rdfs/seeAlso :wikibase:directClaim
   }
  )

(ns org.naturallexicon.lod.wikidata.wikibase
  {
   :rdfs/label "Wikibase system ontology"
   :vann/preferredNamespaceUri "http://wikiba.se/ontology#"
   :vann/preferredNamespacePrefix "wikibase"
   :rdfs/isDefinedBy "http://wikiba.se/ontology-1.0.owl"
   }
  )

(ns org.naturallexicon.lod.wikidata.p
  {
   :rdfs/comment "Reifies wikibase properties"
   :vann/preferredNamespaceUri "http://www.wikidata.org/prop/"
   :vann/preferredNamespacePrefix "p"
   :foaf/homepage "https://www.wikidata.org/wiki/Help:Properties"
   }
  )

(ns org.naturallexicon.lod.wikidata.ps
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/prop/statement/"
   :vann/preferredNamespacePrefix #{"v" "ps"}
   :foaf/homepage "https://www.wikidata.org/wiki/Help:Statements"
   }
  )

(ns org.naturallexicon.lod.wikidata.q
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/prop/qualifier"
   :vann/preferredNamespacePrefix "q"
   :foaf/homepage "https://www.wikidata.org/wiki/Help:Qualifiers"
   }
  )

;; THESE NAMESPACES ARE RELATIVELY RARE
;; BUT SHOW UP IN https://www.mediawiki.org/wiki/Wikibase/Indexing/RDF_Dump_Format#Full_list_of_prefixes

;; PREFIX wdtn: <http://www.wikidata.org/prop/direct-normalized/>
(ns org.naturallexicon.lod.wikidata.wdtn
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/prop/direct-normalized/"
   :vann/preferredNamespacePrefix "wdtn"
   }
  )
;;  PREFIX wds: <http://www.wikidata.org/entity/statement/>
(ns org.naturallexicon.lod.wikidata.wds
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/entity/statement/"
   :vann/preferredNamespacePrefix "wds"
   }
  )

;;  PREFIX wdref: <http://www.wikidata.org/reference/>
(ns org.naturallexicon.lod.wikidata.wdref
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/reference/"
   :vann/preferredNamespacePrefix "wdref"
   }
   )
  
;; PREFIX wdv: <http://www.wikidata.org/value/>
(ns org.naturallexicon.lod.wikidata.wdv
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/value/"
   :vann/preferredNamespacePrefix "wdv"
   }
  )

;; PREFIX psv: <http://www.wikidata.org/prop/statement/value/>
(ns org.naturallexicon.lod.wikidata.psv
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/prop/statement/value/"
   :vann/preferredNamespacePrefix "psv"
   }
  )

;; PREFIX psn: <http://www.wikidata.org/prop/statement/value-normalized/>
(ns iri.org.wikidata.www.prop.statement.value-normalized
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/prop/statement/value-normalized/"
   :vann/preferredNamespacePrefix "psn"
   }
  )

;; PREFIX pq: <http://www.wikidata.org/prop/qualifier/>
(ns org.naturallexicon.lod.wikidata.pq
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/prop/qualifier/"
   :vann/preferredNamespacePrefix "pq"
   }
  )

;; PREFIX pqv: <http://www.wikidata.org/prop/qualifier/value/>
(ns org.naturallexicon.lod.wikidata.pqv
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/prop/qualifier/value/"
   :vann/preferredNamespacePrefix "pqv"
   }
  )

;; PREFIX pqn: <http://www.wikidata.org/prop/qualifier/value-normalized/>
(ns org.naturallexicon.lod.wikidata.pqn
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/prop/qualifier/value-normalized/"
   :vann/preferredNamespacePrefix "pqn"
   }
  )

;; PREFIX pr: <http://www.wikidata.org/prop/reference/>
(ns org.naturallexicon.lod.wikidata.pr
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/prop/reference/"
   :vann/preferredNamespacePrefix "pr"
   }
  )

;; PREFIX prv: <http://www.wikidata.org/prop/reference/value/>
(ns org.naturallexicon.lod.wikidata.prv
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/prop/reference/value/"
   :vann/preferredNamespacePrefix "prv"
   }
  )

;; PREFIX prn: <http://www.wikidata.org/prop/reference/value-normalized/>
(ns org.naturallexicon.lod.wikidata.prn
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/prop/reference/value-normalized/"
   :vann/preferredNamespacePrefix "prn"
   }
  )

 ;; PREFIX wdno: <http://www.wikidata.org/prop/novalue/>
(ns org.naturallexicon.lod.wikidata.wdno
  {
   :vann/preferredNamespaceUri "http://www.wikidata.org/prop/novalue/"
   :vann/preferredNamespacePrefix "wdno"
   }
  )
