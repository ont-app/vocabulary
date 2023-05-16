(ns ont-app.vocabulary.lstr
  {:doc "Defines LangStr type to inform #voc/lstr custom reader tag"
   :author "Eric D. Scott"
   }
  (:require
   [clojure.spec.alpha :as spec]
   #?(:cljs [cljs.compiler])))


;;;;;;;;
;; spec
;;;;;;;;;
(declare langstring-re)

(spec/def :lstr/valid-string langstring-re)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; LANGSTR
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
     [^LangStr literal ^java.io.Writer w]
     (.write w (pr-str (tagged-literal 'voc/lstr  (str literal "@" (.lang literal)))))))

#?(:clj
   (defmethod print-dup LangStr [o ^java.io.Writer w]
     (print-method o w)))

;; for cljs ...
#?(:cljs
   (extend-protocol IPrintWithWriter
     LangStr
     (-pr-writer [this writer _]
       (write-all writer "#voc/lstr \"" (.toString this) "@" (.-lang this) "\""))))

#?(:cljs
   (extend-protocol IEquiv
     LangStr
     (-equiv [this that]
             (and (instance? LangStr that)
                  (= (.-s this) (.-s ^LangStr that))
                  (= (.-lang this) (.-lang ^LangStr that))))))


#?(:cljs
   (defmethod cljs.compiler/emit-constant* ont_app.vocabulary.lstr.LangStr
     ;; Emits a string of js instantiating a LangStr
     [^LangStr x]
     (apply cljs.compiler/emits [(str "new LangStr (\""
                                      (#?(:clj .s :cljs .-s) x)
                                      "\" , \""
                                      (#?(:clj .lang :cljs .-lang) x)
                                   "\")")])))

(defn lang 
  "returns the language tag associated with `langStr`"
  [^LangStr langStr]
  (#?(:clj .lang
      :cljs .-lang) langStr))

(def ^:private langstring-re
  "A regex matching and destructuring valid LangStr format."
  #?(:clj #"(?s)^(.*)@([-a-zA-Z]+)"
     ;; (?s) Dot matches all (including newline)
     ;; only supported for ECMASCRIPT_2018 mode or better.
     :cljs #"^((?:.|\s)*)@([-a-zA-Z]+)"
     ))
;; END READER MACROS

(defn read-LangStr
  "Returns an instance of LangStr parsed from `form`
Where:
- `form` :- `str`@`lang`"
  ^LangStr [form]
  (let [m (re-matches langstring-re form)
        ]
    (when (not= (count m) 3)
      (throw (ex-info "Bad LangString fomat"
                      {:type ::BadLangstringFormat
                       :regex langstring-re
                       :form form})))
    (let [[_ s lang] m]
      (LangStr. s lang))))

(defn  read-LangStr-cljs
  "Returns a macro expression for read-LangStr suitable for insertion and interpretation in cljs source."
  ^LangStr [form]
  `(read-LangStr ~form))
