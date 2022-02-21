(ns ont-app.vocabulary.lstr
  {:doc "Defines LangStr type to inform #lstr custom reader tag"
   :author "Eric D. Scott"
   }
  #?(:cljs
     (:require [cljs.compiler]))
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; LANGSTR
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; defrecord
(deftype LangStr [s lang]
  Object
  (hashCode [this] (hash (str (.s this) "@" (.lang this))))
  (toString [_] s)
  #?(:clj
     (equals [this that]
       (and (instance? LangStr that)
            (= (.s this) (.s that))
            (= (.lang this) (.lang that)))))
  )

  
;; for clj...
#?(:clj
   (defmethod print-method LangStr
     [literal ^java.io.Writer w]
     (.write w (str "#lstr \"" literal "@" (.lang literal) "\""))))

#?(:clj
   (defmethod print-dup LangStr [o ^java.io.Writer w]
     (print-method o w)))

;; for cljs ...
#?(:cljs
   (extend-protocol IPrintWithWriter
     LangStr
     (-pr-writer [this writer opts]
       (write-all writer "#lstr \"" (.toString this) "@" (.-lang this) "\""))))



#?(:cljs
   (extend-protocol IEquiv
     LangStr
     (-equiv [this that]
             (and (instance? LangStr that)
                  (= (.-s this) (.-s that))
                  (= (.-lang this) (.-lang that))))))


#?(:cljs
   (defmethod cljs.compiler/emit-constant* ont_app.vocabulary.lstr.LangStr
     ;; Emits a string of js instantiating a LangStr
     [x]
     (apply cljs.compiler/emits [(str "new ont_app.vocabulary.lstr.LangStr (\""
                                      (#?(:clj .s :cljs .-s) x)
                                      "\" , \""
                                      (#?(:clj .lang :cljs .-lang) x)
                                   "\")")])))

(defn lang 
  "returns the language tag associated with `langStr`"
  [^LangStr langStr]
  (#?(:clj .lang :cljs .-lang) langStr))

(def langstring-re  #?(:clj #"(?s)^(.*)@([-a-zA-Z]+)"
                       ;; (?s) Dot matches all (including newline)
                       ;; only supported for ECMASCRIPT_2018 mode or better.
                       :cljs #"^((?:.|\s)*)@([-a-zA-Z]+)"
                       ))
    
;; END READER MACROS

(defn ^LangStr read-LangStr
  "Returns an instance of LangStr parsed from `form`
Where:
- `form` :- `str`@`lang`"
  [form]
  (let [m (re-matches langstring-re form)
        ]
    (when (not= (count m) 3)
      (throw (ex-info "Bad LangString fomat"
                      {:type ::BadLangstringFormat
                       :regex langstring-re
                       :form form})))
    (let [[_ s lang] m]
      (LangStr. s lang))))

