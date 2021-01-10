# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

v 0.1.3
- cljs.compiler requirment conditional on :cljs (bug fix)

v 0.1.2
- (Breaking) Changed LangStr from record to type
- There's now a format module to handle en/de-coding of URIs and KWIs for clj(s)
- cljc-(get/put)-ns-meta! is deprecated in favor of just (get/put)-ns-meta!
- duplicate prefix mappings throw an error

v 0.1.1
- keyword-for now allows you to respond to the case where no ns
  metadata could be found for the given uri.
- Added the lstr module for language-tagged strings
- cljs-put-ns-meta! has alias put-ns-meta!
- cljs-get-ns-meta has alias get-ns-meta
- iri-for has alias uri-for
- there's an optional on-no-ns argument in keyword-for

