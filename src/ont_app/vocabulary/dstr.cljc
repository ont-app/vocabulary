(ns ont-app.vocabulary.dstr
  {:doc "Defines DatatypeStr type to inform #voc/dstr custom reader tag"
   :author "Eric D. Scott"
   }
  (:require
   [clojure.spec.alpha :as spec]
   #?(:cljs [cljs.compiler])))

;;;;;;;;
;; spec
;;;;;;;;;
(declare datatypestring-re)

(spec/def :dstr/valid-string (fn [s] (re-matches datatypestring-re s)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DATATYPE STR
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftype DatatypeStr [s datatype]
   Object
   (hashCode [this] (hash (str (.s this) "^^" (.datatype this))))
   (toString [_] s)
   #?(:clj
     (equals [this that]
       (and (instance? DatatypeStr that)
            (= (.s this) (.s that))
            (= (.datatype this) (.datatype that))))))

;; for clj...
#?(:clj
   (defmethod print-method DatatypeStr
     [^DatatypeStr literal ^java.io.Writer w]
     (.write w (pr-str (tagged-literal 'voc/dstr (str literal "^^" (.datatype literal)))))))

#?(:clj
   (defmethod print-dup DatatypeStr [o ^java.io.Writer w]
     (print-method o w)))

;; for cljs ...
#?(:cljs
   (extend-protocol IPrintWithWriter
     DatatypeStr
     (-pr-writer [this writer _]
       (write-all writer "#voc/dstr \"" (.toString this) "^^" (.-datatype this) "\""))))

#?(:cljs
   (extend-protocol IEquiv
     DatatypeStr
     (-equiv [this that]
       (and (instance? DatatypeStr that)
            (= (str (.-s this)) (str (.-s ^DatatypeStr that)))
            (= (str (.-datatype this)) (str (.-datatype ^DatatypeStr that)))))))


#?(:cljs
   (defmethod cljs.compiler/emit-constant* ont_app.vocabulary.dstr.DatatypeStr
     ;; Emits a string of js instantiating a DatatypeStr
     [^DatatypeStr x]
     (apply cljs.compiler/emits [(str "new DatatypeStr (\""
                                      (#?(:clj .s :cljs .-s) x)
                                      "\" , \""
                                      (#?(:clj .datatype :cljs .-datatype) x)
                                   "\")")])))

(defn datatype 
  "returns the datatype tag associated with `datatypeStr`"
  [^DatatypeStr datatypeStr]
  (#?(:clj .datatype
      :cljs .-datatype) datatypeStr))

(def ^:private datatypestring-re
  "A regex matching and destructuring DatatypeStr format.
  - Match := [_ `value` `datatype`]"
  #?(:clj (re-pattern (str "(?s)"   ;; match all including newline
                           "("      ;; start group 1
                           ".+"    ;;   at least one of anything
                           ")"      ;; end group 1
                           "\\^\\^" ;; ^^
                           "("      ;; start group 2
                           ".+"     ;;   at least one of anything
                           ")"      ;; end group 2
                           "$"
                           ))

     :cljs (re-pattern (str "^"      ;; start
                            "("      ;; start group 1
                            "(?:"      ;; start non-capturing group
                            "."           ;; single char
                            "|"           ;; or
                            "\\s"          ;; space
                            ")"        ;; end non-capturing group
                            "*"        ;; 0 or more of the non-capturing group
                            ")"      ;; end group 1
                            "\\^\\^" ;; ^^
                            "("      ;; start group 2
                            ".+"     ;;   at least one char
                            ")"      ;; end group 2
                            ))
     
     ))

;; END READER MACROS

(defn parse
  "Returns [`datum` `datatype`] for `s`, or nil
  - Where
    -`s` is a string :~ `datum`^^`datatype`
    - `datum` is a string
    - `datatype` is is a string
  "
  [form]
  (when-let [[_ datum datatype] (re-matches datatypestring-re form)]
    [datum datatype]))

(defn read-DatatypeStr
  "Returns an instance of DatatypeStr parsed from `form`
  - Where:
  - `form` :- `datum`^^`datatype`"
  ^DatatypeStr [form]
  (if-let [[datum datatype] (parse form)]
    (DatatypeStr. datum datatype)
    ;; else no parse
    (throw (ex-info "Bad DatatypeString format"
                    {:type ::BadDatatypestringFormat
                     :regex datatypestring-re
                     :form form}))))


(defn read-DatatypeStr-cljs
  "Returns a macro expression for read-DatatypeStr suitable for insertion and interpretation in cljs source."
  ^DatatypeStr [form]
  `(read-DatatypeStr ~form))

(def default-tags
  "A map := {(type `obj`) `tag`, ...}
  - Where
    -`obj` is a clojure value
    -`tag` is a qname for the resource tagging the datatype of `obj`
  - NOTE: this informs the `voc/tag` method's 1-arg version
  "
  (atom {(type 0) "xsd:long"
         (type 0.0) "xsd:double"
         (type true) "xsd:Boolean"
         (type #inst "2000") "xsd:dateTime"
         (type "") "xsd:string"
         (type (short 0)) "xsd:short"
         (type (byte 0)) "xsd:byte"
         (type (float 0)) "xsd:float"
         }))
