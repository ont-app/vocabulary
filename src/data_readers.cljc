{
    voc/lstr #?(:clj ont-app.vocabulary.lstr/read-LangStr
                :cljs  ont-app.vocabulary.lstr/read-LangStr-cljs
                :default ont-app.vocabulary.lstr/read-LangStr
               )
    voc/dstr #?(:clj ont-app.vocabulary.dstr/read-DatatypeStr
                :cljs  ont-app.vocabulary.dstr/read-DatatypeStr-cljs
                :default ont-app.vocabulary.dstr/read-DatatypeStr
               )
 ;; tagged literal spec indicates that:
 ;; "Reader tags without namespace qualifiers are reserved for Clojure"
 ;; See https://clojure.org/reference/reader#tagged_literals
 ;; Including for backward compatiility...
 ^:deprecated
 lstr #?(:clj ont-app.vocabulary.lstr/read-LangStr
         :cljs  ont-app.vocabulary.lstr/read-LangStr-cljs
         :default ont-app.vocabulary.lstr/read-LangStr
         )
 }
