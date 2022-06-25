(ns ont-app.vocabulary.format
  "Logic to handle all the various escpapes and en/decoding required for URIs
  and keywords"
  (:require
   [clojure.string :as s]
   [clojure.set :as set]
   [clojure.edn :as edn]
   #?(:clj [clojure.java.io :as io])
   #?(:cljs [cljs.reader :refer [read-string]])
   ))



;; Java-specific URI utilities
(defn escape-utf-8
  "Returns a string for `c`, escaped as its hex equivalent."
  [c]
  #?(:clj
     (str "%"
          (->> (str (char c)) .getBytes (map #(format "%X" %)) (s/join "%")))
     :cljs
     (throw (ex-info "Not supported in cljs: escape-utf-8"
                     {:type :not-supported-in-cljs
                      :fn "escape-utf-8"
                      :c c}))))

(defn uri-test
    "True when `c` is problem-free in java URIs.
  Typically used to create an edn file to inform encoding/decoding URI strings"
  [c]
  #?(:clj
     (let [s (str "http://blah.com/" c)]
       (= (str (java.net.URI. s)) s))
     :cljs
     (throw (ex-info "Not supported in cljs: uri-test"
                     {:type :not-supported-in-cljs
                      :fn "uri-test"
                      :c c}))))


#?(:clj
   (defn get-escapes
  "Returns {`test-breaker` `escaped`, ...} for `char-test` and `escape-fn`
  Where
  - `test-breaker` is a char that breaks `char-test`
  - `char-test` := fn [c] -> true if the char does not need escaping
  - `escape-fn` := fn [c] -> `escaped`
  "
  ([char-test escape-fn]
   (get-escapes char-test escape-fn identity))
  
  ([char-test escape-fn key-fn]
  (let [max-char 65535
        collect-test-breaker (fn [sacc c]
                             (try
                               (if (not (char-test c))
                                 (conj sacc c)
                                 sacc)
                               (catch Throwable e
                                 (conj sacc c))))
        test-breakers (reduce collect-test-breaker
                              #{}
                              (map char (range max-char)))
        collect-escape (fn [macc c] (assoc macc
                                           (key-fn c)
                                           (escape-fn (int c))))
        ]
    (reduce collect-escape {} test-breakers)
    ))))

(def invert-escape-map #(reduce-kv (fn [macc c v] (assoc macc v (str c))) {} %))

(defn escapes-re
  "Returns a regex to recognize escape patterns in an encoded string per `inverted-escpaes-map`
  Where
  - `inverted-escapes-map` := {`escape-pattern` `original`, ...}
  "
  [inverted-escapes-map]
  (re-pattern (s/join "|"
                      (sort (fn [a b] (> (count a) (count b)))
                            ;; ... longer patterns first
                            (keys inverted-escapes-map)))))
#?(:clj
   (defn generate-uri-escapes
  "Side-effects: writes uri-escapes.edn and uri-escapes-inverted.edn
  These are used to cache values used in clj/s to escape URIs
  Note: typically used once to populate the resources.
"
     []
     (spit "uri-escapes.edn" (get-escapes uri-test escape-utf-8))))


(def cljs-uri-escapes
  "A direct copy of (io/resource 'uri-escapes.edn')"
  (merge
   ;; A direct copy of (io/resource "uri-escapes.edn")"
   {
    (char 0) "%0",
    (char 1) "%1",
    (char 2) "%2",
    (char 3) "%3",
    (char 4) "%4",
    (char 5) "%5",
    (char 6) "%6",
    (char 7) "%7",
    (char 8) "%8",
    (char 9) "%9",
    (char 10) "%A",
    (char 11) "%B",
    (char 12) "%C",
    (char 13) "%D",
    (char 14) "%E",
    (char 15) "%F",
    (char 16) "%10",
    (char 17) "%11",
    (char 18) "%12",
    (char 19) "%13",
    (char 20) "%14",
    (char 21) "%15",
    (char 22) "%16",
    (char 23) "%17",
    (char 24) "%18",
    (char 25) "%19",
    (char 26) "%1A",
    (char 27) "%1B",
    (char 28) "%1C",
    (char 29) "%1D",
    (char 30) "%1E",
    (char 31) "%1F",
    (char 32) "%20",
    (char 34) "%22",
    (char 37) "%25",
    (char 60) "%3C",
    (char 62) "%3E",
    (char 91) "%5B",
    (char 92) "%5C",
    (char 93) "%5D",
    (char 94) "%5E",
    (char 96) "%60",
    (char 123) "%7B",
    (char 124) "%7C",
    (char 125) "%7D",
    (char 127) "%7F",
    (char 128) "%C2%80",
    (char 129) "%C2%81",
    (char 130) "%C2%82",
    (char 131) "%C2%83",
    (char 132) "%C2%84",
    (char 133) "%C2%85",
    (char 134) "%C2%86",
    (char 135) "%C2%87",
    (char 136) "%C2%88",
    (char 137) "%C2%89",
    (char 138) "%C2%8A",
    (char 139) "%C2%8B",
    (char 140) "%C2%8C",
    (char 141) "%C2%8D",
    (char 142) "%C2%8E",
    (char 143) "%C2%8F"
    (char 144) "%C2%90",
    (char 145) "%C2%91",
    (char 146) "%C2%92",
    (char 147) "%C2%93",
    (char 148) "%C2%94",
    (char 149) "%C2%95",
    (char 150) "%C2%96",
    (char 151) "%C2%97",
    (char 152) "%C2%98",
    (char 153) "%C2%99",
    (char 154) "%C2%9A",
    (char 155) "%C2%9B",
    (char 156) "%C2%9C",
    (char 157) "%C2%9D",
    (char 158) "%C2%9E",
    (char 159) "%C2%9F",
    (char 160) "%C2%A0",
    (char 5760) "%E1%9A%80",
    (char 6158) "%E1%A0%8E",
    (char 8192) "%E2%80%80",
    (char 8193) "%E2%80%81",
    (char 8194) "%E2%80%82",
    (char 8195) "%E2%80%83",
    (char 8196) "%E2%80%84",
    (char 8197) "%E2%80%85",
    (char 8198) "%E2%80%86",
    (char 8199) "%E2%80%87",
    (char 8200) "%E2%80%88",
    (char 8201) "%E2%80%89",
    (char 8202) "%E2%80%8A",
    (char 8232) "%E2%80%A8",
    (char 8233) "%E2%80%A9",
    (char 8239) "%E2%80%AF",
    (char 8287) "%E2%81%9F",
    (char 12288) "%E3%80%80",
    }
   ;; characters that break the cljs reader
   {}))


(def uri-escapes #?(:clj (edn/read-string
                          (slurp (io/resource "uri-escapes.edn")))
                    :cljs cljs-uri-escapes))

(def uri-escapes-inverted (invert-escape-map uri-escapes))

#?(:clj
   (defn uri-string?
     "True iff `s` is a valid URI string"
     [s]
     (try
       (not (nil? (some-> s
                          (java.net.URI.)
                          (#(and (.getScheme %)(.getAuthority %))))))
       (catch Throwable e
         false))))

(defn encode-uri-string
  "Renders `s` in a form that can be parsed as a URI"
  [s]
  (s/escape s uri-escapes))

(def uri-escapes-re (escapes-re uri-escapes-inverted))

(defn decode-uri-string
  "Inverts URI escapes in `s`. Inverse of encode-uri-string."
  [s]
  (s/replace s uri-escapes-re (fn [esc] (uri-escapes-inverted esc))))

(defn kw-test
  "True when `c` is problem-free in keywords.
  Typically used in creataing edn files to inform encoding/decoding keywords"
  [c]
  (let [s (str "a" c "b")]
    (= (keyword s)
       (read-string (str ":" s)))))

#?(:clj
   (defn generate-kwi-escapes
  "Side-effects: writes uri-escapes.edn and uri-escapes-inverted.edn
  These are used to cache values used in clj/s to escape URIs
  Note: typically used once to populate the resources.
"
  []
  (spit "resources/kw-escapes.edn" (get-escapes kw-test escape-utf-8))))

(def cljs-kw-escapes
  (merge
   ;; copy of kw-escapes.edn
   {\  "%E2%80%80", \　 "%E3%80%80", \space "%20", \@ "%40", \` "%60", \  "%E1%9A%80", \  "%E2%80%81", \  "%E2%80%82", \" "%22", \  "%E2%80%83", \  "%E2%80%84", \  "%E2%80%85", \  "%E2%80%86", \  "%E2%80%88", \( "%28", \  "%E2%80%A8", \tab "%9", \  "%E2%80%89", \) "%29", \  "%E2%80%A9", \newline "%A", \  "%E2%80%8A", \ "%B", \formfeed "%C", \, "%2C", \return "%D", \᠎ "%E1%A0%8E", \; "%3B", \[ "%5B", \{ "%7B", \ "%1C", \\ "%5C", \ "%1D", \] "%5D", \} "%7D", \ "%1E", \^ "%5E", \~ "%7E", \ "%1F", \  "%E2%81%9F"}
   ;; clojurescript-specific escapes for chars 
   {(char 47) "%2F" ;; forward slash
    (char 58) "%3A"})) ;; colon

(def kw-escapes "Escapes map for keywords"
    #?(:clj (edn/read-string (slurp (io/resource "kw-escapes.edn")))
       :cljs cljs-kw-escapes))


(def kw-terminal-escapes
  "Escapes for characters forbidden a the end of a keyword"
  {(char 47) "%2F", ;; forward slash
   (char 58) "%3A"}) ;; colon

(def kw-escapes-inverted "Maps escaped charaters to the originals"
  (invert-escape-map (merge kw-escapes
                            kw-terminal-escapes)))

(def kw-escapes-re "A regex to recognize when a string contains escapes"
    (escapes-re kw-escapes-inverted))

(defn encode-kw-ns
  "Returns modified `kw-ns`, derived s.t. when used as the namespace component of
   some   `kw`, `kw` will not choke the reader.
  Inverse of `decode-kw-name`
  Where
  - `kw-ns`` is a string
  - `kw` is a keyword := :`ns`/`s`
  "
  [kw-ns]
  (s/escape kw-ns kw-escapes))


(defn decode-kw-ns
  "Returns `kw-ns` with any escapes translated"
  [kw-ns]
  (-> kw-ns (s/replace kw-escapes-re (fn [esc] (kw-escapes-inverted esc)))))

(defn encode-kw-name
  "Returns modified `kw-name`, derived s.t. when used as the name component of
   some   `kw`, `kw` will not choke the reader.
  Inverse of `decode-kw-name`
  Where
  - `kw-name`` is a string
  - `kw` is a keyword := :`ns`/`s`
  "
  [kw-name]
  (let [maybe-prepend+n+ (fn [s]
                        (if (re-matches #"^[0-9]+.*" s)
                          (str "+n+" s)
                          s))
        maybe-escape-last (fn [s]
                            (if (contains? kw-terminal-escapes (last s))
                              (str (subs s 0 (dec (count s)))
                                   (str (kw-terminal-escapes (last s))))
                              s))
        ]
  (-> kw-name
      (s/escape kw-escapes)
      (maybe-escape-last)
      (maybe-prepend+n+))))


(defn decode-kw-name
  "Inverse of `encode-kw-name`. Returns original value of `kw-name`
  Where
  `kw-name` is a string, typically the name string of a KWI."
  [kw-name]
  (-> kw-name
      (s/replace #"^\+n\+" "")
      (s/replace kw-escapes-re (fn [esc] (kw-escapes-inverted esc)))))

(defn ensure-readable-keywords
  "Returns `edn'`, replacing keywords properly encoded for clj or cljs
  Where
  - `edn` is a string of edn
  NOTE: this would typically be used to translate clj <-> cljs
  "
  [edn]
  (let [keyword-recognizer #":([^/\s]+)((/?)(\S+))?"
        categorize (fn [elts]
                     ;; `elts` are parse elements from kw recognizer
                     (cond
                       (and (some? (nth elts 1))
                            (nil? (nth elts 2))
                            (nil? (nth elts 3))
                            (nil? (nth elts 4))) :simple
                       (and (some? (nth elts 1))
                            (some? (nth elts 2))
                            (some? (nth elts 3))
                            (some? (nth elts 4))) :namespaced

                       ))
        ]
    (clojure.string/replace
     edn
     keyword-recognizer
     (fn [elts]
       (str ":"
            (case (categorize elts)
              :simple (encode-kw-name (nth elts 1))
              :namespaced (str (encode-kw-ns (nth elts 1))
                               "/"
                               (encode-kw-name (nth elts 4)))))))))


