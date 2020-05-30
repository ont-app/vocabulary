(ns ont-app.vocabulary.lstr
  {:doc "Defines LangStr type to inform #lstr custom reader tag"
   :author "Eric D. Scott"
   }
  #_(:require [cljs.compiler])
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; LANGSTR
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; defrecord
#?(:clj
    (defrecord LangStr [s lang]
          Object
          (toString [_] s))
   :cljs
   (defrecord LangStr [s lang]))

;; printing methods
;; .. for clj...
#?(:clj
   (defmethod print-method LangStr
     [literal ^java.io.Writer w]
     (.write w (str "#lstr \"" literal "@" (:lang literal) "\""))))

#?(:clj
   (defmethod print-dup LangStr [o ^java.io.Writer w]
     (print-method o w)))
;; .. for cljs ...
#?(:cljs
   (extend-protocol Object
     LangStr
     (toString [this] (:s this))))
#?(:cljs
   (extend-protocol  IPrintWithWriter
     LangStr
     (-pr-writer [this writer opts]
       (write-all writer "#lstr \"" (str this) "@" (:lang this) "\""))))


(defn lang [langStr]
  "returns the language tag associated with `langStr`"
  (:lang langStr))

(def langstring-re #"^(.*)@([-a-zA-Z]+)")

(defn ^LangStr read-LangStr [form]
  (let [m (re-matches langstring-re form)
        ]
    (when (not= (count m) 3)
      (throw (ex-info "Bad LangString fomat"
                      {:type ::BadLangstringFormat
                       :regex langstring-re
                       :form form})))
    (let [[_ s lang] m]
      (LangStr. s lang))))

