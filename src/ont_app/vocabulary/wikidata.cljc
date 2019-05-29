(ns ont-app.vocabulary.wikidata
  {:doc "Wikidata-related vocabulary. Requiring this file should bring in all ns assocated with wikidata."
   }
  (:require
   [ont-app.vocabulary.core :as voc]
   ))
  
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.wd
    {
     :dc/title "Wikibase/EntityData"
     :foaf/homepage "https://www.mediawiki.org/wiki/Wikibase/EntityData"
     :vann/preferredNamespaceUri "http://www.wikidata.org/entity/"
     :vann/preferredNamespacePrefix "wd"
     }
    )

(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.wdt
    {
     :dc/description "Direct properties in wikibase"
     :vann/preferredNamespaceUri "http://www.wikidata.org/prop/direct/"
     :vann/preferredNamespacePrefix "wdt"
     :rdfs/seeAlso :wikibase:directClaim
     }
    )
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.wikibase
    {
     :rdfs/label "Wikibase system ontology"
     :vann/preferredNamespaceUri "http://wikiba.se/ontology#"
     :vann/preferredNamespacePrefix "wikibase"
     :rdfs/isDefinedBy "http://wikiba.se/ontology-1.0.owl"
     }
    )

(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.p
    {
     :rdfs/comment "Reifies wikibase properties"
     :vann/preferredNamespaceUri "http://www.wikidata.org/prop/"
     :vann/preferredNamespacePrefix "p"
     :foaf/homepage "https://www.wikidata.org/wiki/Help:Properties"
     }
    )
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.ps
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/prop/statement/"
     :vann/preferredNamespacePrefix #{"v" "ps"}
     :foaf/homepage "https://www.wikidata.org/wiki/Help:Statements"
     }
    )
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.q
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/prop/qualifier"
     :vann/preferredNamespacePrefix "q"
     :foaf/homepage "https://www.wikidata.org/wiki/Help:Qualifiers"
     }
    )

;; THESE NAMESPACES ARE RELATIVELY RARE
;; BUT SHOW UP IN https://www.mediawiki.org/wiki/Wikibase/Indexing/RDF_Dump_Format#Full_list_of_prefixes

;; PREFIX wdtn: <http://www.wikidata.org/prop/direct-normalized/>
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.wdtn
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/prop/direct-normalized/"
     :vann/preferredNamespacePrefix "wdtn"
     }
    )
;;  PREFIX wds: <http://www.wikidata.org/entity/statement/>

(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.wds
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/entity/statement/"
     :vann/preferredNamespacePrefix "wds"
     }
    )

;;  PREFIX wdref: <http://www.wikidata.org/reference/>
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.wdref
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/reference/"
     :vann/preferredNamespacePrefix "wdref"
     }
    )

;; PREFIX wdv: <http://www.wikidata.org/value/>
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.wdv
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/value/"
     :vann/preferredNamespacePrefix "wdv"
     }
    )

;; PREFIX psv: <http://www.wikidata.org/prop/statement/value/>
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.psv
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/prop/statement/value/"
     :vann/preferredNamespacePrefix "psv"
     }
    )

;; PREFIX psn: <http://www.wikidata.org/prop/statement/value-normalized/>
(voc/cljc-put-ns-meta!
 'iri.org.wikidata.www.prop.statement.value-normalized
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/prop/statement/value-normalized/"
     :vann/preferredNamespacePrefix "psn"
     }
    )

;; PREFIX pq: <http://www.wikidata.org/prop/qualifier/>
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.pq
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/prop/qualifier/"
     :vann/preferredNamespacePrefix "pq"
     }
    )

;; PREFIX pqv: <http://www.wikidata.org/prop/qualifier/value/>
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.pqv
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/prop/qualifier/value/"
     :vann/preferredNamespacePrefix "pqv"
     }
    )

;; PREFIX pqn: <http://www.wikidata.org/prop/qualifier/value-normalized/>
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.pqn
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/prop/qualifier/value-normalized/"
     :vann/preferredNamespacePrefix "pqn"
     }
    )

;; PREFIX pr: <http://www.wikidata.org/prop/reference/>
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.pr
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/prop/reference/"
     :vann/preferredNamespacePrefix "pr"
     }
    )

;; PREFIX prv: <http://www.wikidata.org/prop/reference/value/>
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.prv
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/prop/reference/value/"
     :vann/preferredNamespacePrefix "prv"
     }
    )

;; PREFIX prn: <http://www.wikidata.org/prop/reference/value-normalized/>
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.prn
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/prop/reference/value-normalized/"
     :vann/preferredNamespacePrefix "prn"
     }
    )

;; PREFIX wdno: <http://www.wikidata.org/prop/novalue/>
(voc/cljc-put-ns-meta!
 'ont-app.vocabulary.wikidata.wdno
    {
     :vann/preferredNamespaceUri "http://www.wikidata.org/prop/novalue/"
     :vann/preferredNamespacePrefix "wdno"
     }
    )

  

