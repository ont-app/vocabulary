(ns ont-app.vocabulary.lstr
  {:doc "Defines LangStr type to inform #lstr custom reader tag"
   :author "Eric D. Scott"
   }
  (:require
   [cljs.compiler])
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; LANGSTR
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord LangStr [s lang])

(extend-protocol Object
  LangStr
  (toString [this] (:s this)))

(extend-protocol  IPrintWithWriter
  LangStr
  (-pr-writer [this writer opts]
    (write-all writer "#lstr \"" (str this) "@" (:lang this) "\"")))

(defn lang [langStr]
  "returns the language tag associated with `langStr`"
  (:lang langStr))

(defn read-LangStr [form]
  (let [langstring-re #"^(.*)@([-a-zA-Z]+)" 
        m (re-matches langstring-re form)
        ]
    (when (not= (count m) 3)
      (throw (ex-info "Bad LangString fomat"
                      {:type ::BadLangstringFormat
                       :regex langstring-re
                       :form form})))
    (let [[_ s lang] m]
      (->LangStr s lang))))

