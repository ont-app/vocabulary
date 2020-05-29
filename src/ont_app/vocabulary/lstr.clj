(ns ont-app.vocabulary.lstr
  {:doc "Defines LangStr type to inform #lstr custom reader tag"
   :author "Eric D. Scott"
   }
  (:require [cljs.compiler])
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; LANGSTR
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord LangStr [s lang]
  Object
  (toString [_] s))

(defmethod print-method LangStr
  [literal ^java.io.Writer w]
  (.write w (str "#lstr \"" literal "@" (:lang literal) "\"")))

(defmethod print-dup LangStr [o ^java.io.Writer w]
  (print-method o w))

(defn lang [langStr]
  "returns the language tag associated with `langStr`"
  (:lang langStr))

(defn ^LangStr read-LangStr [form]
  (let [langstring-re #"^(.*)@([-a-zA-Z]+)" 
        m (re-matches langstring-re form)
        ]
    (when (not= (count m) 3)
      (throw (ex-info "Bad LangString fomat"
                      {:type ::BadLangstringFormat
                       :regex langstring-re
                       :form form})))
    (let [[_ s lang] m]
      (LangStr. s lang))))


