# <img src="http://ericdscott.com/NaturalLexiconLogo.png" alt="NaturalLexicon logo" :width=100 height=100/> ont-app/vocabulary

Integration between Clojure keywords and URIs, plus support for
RDF-style language-tagged literals.

## Contents
- [Installation](#h2-installation)
- [Motivation](#h2-motivation)
- [Defining keyword Identifiers (KWIs) mapped to URI namespaces](#h2-defining-kwis)
  - [Basic namespace metadata](#h3-basic-metadata)
  - [Working with KWIs](#h3-working-with-kwis)
    - [`uri-for`](#h4-iri-for)
    - [`qname-for`](#h4-qname-for)
    - [`keyword-for`](#h4-keyword-for)
  - [Accessing-namespace-metadata](#h3-accessing-namespace-metadata)
    - [`put-ns-meta!` and `get-ns-meta`](#h4-put-ns-meta)
    - [`prefix-to-ns`](#h4-prefix-to-ns)
    - [`ns-to-namespace`](#h4-ns-to-namespace)
    - [`namespace-to-ns`](#h4-namespace-to-ns)
    - [`ns-to-prefix`](#h4-ns-to-prefix)
    - [`clear-caches!`](#h4-clear-caches)
  - [Support for SPARQL queries](#h3-support-for-sparql)
    - [`sparql-prefixes-for`](#h4-sparql-prefixes-for)
    - [`prepend-prefix-declarations`](#h4-prepend-prefix-declarations)
  - [Common Linked Data namespaces](#h3-linked-data)
    - [Imported with_ont-app.vocabulary.core](#h4-imported-with-voc)
    - [Imported with ont-app.vocabulary.wikidata](#h4-imported-with-wd)
    - [Imported with ont-app.vocabulary.linguistics](#h4-imported-with-ling)
- [Language-tagged strings](#h2-language-tagged-strings)
- [License](#h2-license)

<a name="h2-installation"></a>
## Installation

Available at [clojars](https://clojars.org/ont-app/vocabulary).

[![Clojars Project](https://img.shields.io/clojars/v/ont-app/vocabulary.svg)](https://clojars.org/ont-app/vocabulary)


```
 (defproject .....
  :dependencies 
  [...
   [ont-app/vocabulary "0.1.2"]
   ...
   ])
```   

<a name="h2-motivation"></a>
## Motivation
Clojure provides for the definition of
[keywords](https://clojure.org/reference/data_structures#Keywords),
which function as identifiers within Clojure code, and serve many
useful purposes.  These keywords can be interned within specific
namespaces to avoid collisions. The role played by these keywords is
very similar to the role played by URIs within the [Linked Open
Data](http://linkeddata.org/) (LOD) community, which also has a regime
for providing namespaces.

Ont-app/vocabulary provides mappings between Clojure namespaces and
URI-based namespaces using declarations within Clojure namespace
metadata.

There is also support for a similar arrangement within Clojurescript,
though some things are done a little differently given the fact that
Clojurescript does not implement namespaces as first-class objects.

These mappings set the stage for using Keyword Identifiers (KWIs)
mappable between Clojure code and the larger world through a
correspondence with URIs.

Another construct from RDF that may have application more generally in
graph-based data is that of a language-tagged literal, which tags
strings of natural language with their associated language. For
example we could use such tags to express the differing orthographies
of `"gaol"@en-GB` vs. `"jail"@en-US`. This library defines a custom
reader tag `lstr` for declaring similar language-tagged strings,
e.g. `#lstr "gaol@en-GB"` and `#lstr "jail@en-US"`.

<a name="h2-defining-kwis"></a>
## Defining Keyword Identifiers (KWIs) mapped to URI namespaces

```
(ns ...
 (:require
   ...
   [ont-app.vocabulary.core] 
   ...))
```

This will load function definitions interned in the vocabulary.core
namespace, and also a number of other `ns` declarations, each
dedicated to a commonly occurring namespace in the world of LOD.

<a name="h3-basic-metadata"></a>
### Basic namespace metadata 
Within standard (JVM-based) clojure, the minimal specification to support ont-app/vocabulary functionality for a given namespace requires metadata specification as follows:

```
(ns org.example
  {
    :vann/preferredNamespacePrefix "eg"
    :vann/preferredNamespaceUri "http://example.org/"
  }
  (:require 
  [ont-app.vocabulary.core :as v]
  ...))
```

This expresses an equivalence between the clojure keyword...

```
  :eg/example-var
```
... and the URI ...
```
 <http://example.org/example-var>
```

The `vann` prefix refers to [an existing public
vocabulary](http://vocab.org/vann/) which will be explained in more detail
[below](#h3-accessing-namespace-metadata).

Unfortunately, Clojurescript does not implement namespaces as
first-class objects, and so there is no `ns` object to which we can
attach metadata. So _ont-app/vocabulary_ provides this idiom to
achieve the same effect in both clj and cljs environments:

```
(voc/put-ns-meta!
 'org.example
  {
    :vann/preferredNamespacePrefix "eg"
    :vann/preferredNamespaceUri "http://example.org/"
  })
```

In Clojure, it simply updates the metadata of the named namespace
(which may be automatically created with [_create-ns_](https://clojuredocs.org/clojure.core/create-ns)). In Clojurescript,
this updates a dedicated map from _org.example_ to 'pseudo-metadata'
in a global atom called _cljs-ns-metadata_.

<a name="h3-working-with-kwis"></a>
### Working with KWIs


<a name="h4-iri-for"></a>
#### `uri-for`

We can get the URI string associated with a keyword:

The function `uri-for` works for fully qualified keywords, whose
aliases interned in the local lexical environment (note the
double-colon):

```
> (require [org.example :as eg]) ;; or any other alias
> (v/uri-for ::eg/Example)

"http://example.org/Example"
>
```

We can also usually get away with using a single colon (these are qualified, but not _fully_ qualified ...
```
> (v/uri-for :eg/Example)
"http://example.org/Example"
>
```

... but because the namespace is hard-coded and not bound to clojure's
aliasing system, there is the possibility of a clash in the case where
the same alias was chosen for two different namesspace. This is not
really a problem for well-known prefixes like `rdfs` or `foaf`, but
may become a bigger problem if you choose a generic prefix like
"data".

Also, it's important to note that while _::eg/Example_ and
_:eg/Example_ resolve to the same URI string, the keywords
themselves are not equal in Clojure.


This function is called `uri-for` to reflect common usage, but because any UTF-8 characters can be used, these are actually [IRIs](https://en.wikipedia.org/wiki/Internationalized_Resource_Identifier). The function _iri-for_ function is also defined an alias of _uri-for_.

<a name="h4-qname-for"></a>
#### `qname-for`

We can get the [qname](https://en.wikipedia.org/wiki/QName) for a keyword:

```
> (v/qname-for ::foaf/homepage)
"foaf:homepage"
>
```

<a name="h4-keyword-for"></a>
#### `keyword-for`

We can get a keyword for an URI...

```
> (v/keyword-for "http://xmlns.com/foaf/0.1/homepage")
:foaf/homepage
>
```

If the namespace does not have sufficient metadata to create a
namespaced keyword, the keyword will be interned in an namespace based on the URI scheme:

```
> (v/keyword-for "http://example.com/my/stuff")
:http://example.com/my/stuff))
>
```

Characters which would choke the reader will be %-escaped. These characters differ depending on whether we're using the jvm or cljs platforms.

There is an optional arity-2 version whose first argument is called
when no ns could be resolved:

```
> (v/keyword-for (fn [u k] (log/warn "No namespace metadata found for " u) k)
                  "http://example.com/my/stuff")
WARN: No namespace metadata found for "http://example.com/my/stuff"
:http://example.com/my/stuff
>          
```


<a name="h3-accessing-namespace-metadata"></a>
### Accessing namespace metadata

<a name="h4-put-ns-meta"></a>
#### `put-ns-meta!` and `get-ns-meta`
Let's take another look at the metadata we used above to declare mappings between clojure namespaces and RDF namespaces:

```
(v/put-ns-meta!
 'org.example
  {
    :vann/preferredNamespacePrefix "eg"
    :vann/preferredNamespaceUri "http://example.org/"
  })
```

Note that the metadata for this module includes some qualified
keywords in this format:

```
:<prefix>/<name>
```

The relations
[preferredNamespaceUri](http://vocab.org/vann/#preferredNamespacePrefix)
and
[preferredNamespacePrefix](http://vocab.org/vann/#preferredNamespaceUri)
are part of the public VANN vocabulary, with well-defined usage and
semantics.


The namespace for `vann` is also declared as _ont-app.vocabulary.vann_ in the
`ont_app/vocabulary/core.cljc` file, with this declaration:

```
(v/put-ns-meta!
 'ont-app.vocabulary.vann
 {
   :rdfs/label "VANN"
   :dc/description "A vocabulary for annotating vocabulary descriptions"
   :vann/preferredNamespaceUri "http://purl.org/vocab/vann"
   :vann/peferredNamespacePrefix "vann"
   :foaf/homepage "http://vocab.org/vann/"
 })
```

There is an inverse of _put-ns-meta!_ called _get-ns-meta_:

```
> (v/get-ns-metadata 'ont-app.vocabulary.foaf)
{
 :dc/title "Friend of a Friend (FOAF) vocabulary"
 :dc/description "The Friend of a Friend (FOAF) RDF vocabulary,
 described using W3C RDF Schema and the Web Ontology Language."
 :vann/preferredNamespaceUri "http://xmlns.com/foaf/0.1/"
 :vann/preferredNamespacePrefix "foaf"
 :foaf/homepage "http://xmlns.com/foaf/spec/"
 :dcat/downloadURL "http://xmlns.com/foaf/spec/index.rdf"
 :voc/appendix [["http://xmlns.com/foaf/spec/index.rdf"
                 :dcat:mediaType "application/rdf+xml"]]
 })
>
```

These are much richer descriptions than the minimal example in the
previous section, with metadata encoded using several different public
vocabularies, described [below](#h4-linked-data).

Note that these are all simple key/value declarations except the
`:voc/appendix` declaration which is in the form

```
:voc/appendix [[<subject> <predicate> <object>]....], 
```

This includes triples which elaborate on constructs mentioned in the
key-value paris in the rest of the metadata, in this case describing
the media types of files describing the vocabulary which are available
for download at the URLs given.

<a name="h4-prefix-to-ns"></a>
#### `prefix-to-ns`
We can get a map all the prefixes of namespaces declared within the current lexical environment:

```
> (v/prefix-to-ns)
{"dc" #namespace[ont-app.vocabulary.dc],
 "owl" #namespace[ont-app.vocabulary..owl],
 "ontolex" #namespace[ont-app.vocabulary.ontolex],
 "foaf" #namespace[ont-app.vocabulary.foaf],
 ...
 }
 >
 ```
 
In Clojurescript, since there's no _ns_ object, the results would look like this:

```
> (v/prefix-to-ns)
{"dc" ont-app.vocabulary.dc,
 "owl" ont-app.vocabulary..owl,
 "ontolex" ont-app.vocabulary.ontolex,
 "foaf" ont-app.vocabulary.foaf,
 ...
 }
 >
```

<a name="h4-ns-to-namespace"></a>
#### `ns-to-namespace`
We can get the URI namespace associated with an `ns`

In Clojure:

```
> (v/ns-to-namespace (find-ns 'ont-app.vocabulary.foaf))
"http://xmlns.com/foaf/0.1/"
>
```

In both Clojure and ClojureScript:

```
> (v/ns-to-namespace 'ont-app.vocabulary.foaf)
"http://xmlns.com/foaf/0.1/"
>
```

<a name="h4-namespace-to-ns"></a>
#### `namespace-to-ns`
We can get a map from namespace URIs to their associated clojure namespaces:

```
> (v/namespace-to-ns)
{
 "http://www.w3.org/2002/07/owl#"
 #namespace[org.naturallexicon.lod.owl],
 "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#"
 #namespace[org.naturallexicon.lod.nif],
 "http://purl.org/dc/elements/1.1/"
 #namespace[org.naturallexicion.lod.dc],
 "http://www.w3.org/ns/dcat#"
 #namespace[org.naturallexicon.lod.dcat],
 ...
 }
>
```

With the usual allowance for clojurescript described above.

<a name="h4-ns-to-prefix"></a>
#### `ns-to-prefix`
We can get the prefix associated with an `ns`:
```
> (v/ns-to-prefix (v/cljc-find-ns 'org.naturallexicon.lod.foaf))
"foaf"
>
```

<a name="h4-clear-caches"></a>
#### `clear-caches!`
For performance reasons, these metadata values are all cached. If
you're making changes to the metadata and it's not 'taking', you may
need to clear the caches:

```
> (v/clear-caches!)
```

<a name="h3-support-for-sparql"></a>
### Support for SPARQL queries

RDF is explicitly constructed from URIs, and there is an intimate
relationship between [SPARQL](https://en.wikipedia.org/wiki/SPARQL)
queries and RDF namespaces. `ont-app/vocabulary` provides facilities
for extracting SPARQL prefix declarations from queries containing
qnames.

<a name="h4-sparql-prefixes-for"></a>
#### `sparql-prefixes-for`
We can infer the PREFIX declarations appropriate to a SPARQL query:
```
> (v/sparql-prefixes-for
             "Select * Where{?s foaf:homepage ?homepage}")
("PREFIX foaf: <http://xmlns.com/foaf/0.1/>")
>
```

<a name="h4-prepend-prefix-declarations"></a>
#### `prepend-prefix-declarations`
Or we can just go ahead and prepend the prefixes...

```
> (v/prepend-prefix-declarations
               "Select * Where {?s foaf:homepage ?homepage}")
"PREFIX foaf: <http://xmlns.com/foaf/0.1/>
Select * Where{?s foaf:homepage ?homepage}"
>
```

<a name="h3-linked-data"></a>
### Common Linked Data namespaces

Part of the vision of the `ont-app` project is to provide a medium for
expressing what adherents to Domain-driven Design and Behavior-driven
Design call a "[Ubiquitous
Vocabulary](https://martinfowler.com/bliki/UbiquitousLanguage.html)". It
also shares the vision of the Linked Data community that huge [network
effects](https://en.wikipedia.org/wiki/Network_effect) can emerge when
vocabularies emerge which are shared amongst a community of users
working in the same domain.

There are a large number of public vocabularies dedicated to various
application domains, some of which have gained a good deal of traction
in the Linked Data community. Ont-app/vocabulary includes declarations
of their associated namespaces, packaged within the core module, a
module dedicated to wikidata, and another dedicated to linguistics. 

<a name="h4-imported-with-voc"></a>
#### Imported with _ont-app.vocabulary.core_

Requiring the `ont-app.vocabulary.core` module also loads `ns`
declarations dedicated to some of the most commonly used RDF/Linked
Open Data prefixes: 


| PREFIX | URI | Comments |
| --- | --- | --- |
| [rdf](https://www.w3.org/2001/sw/wiki/RDF) | https://www.w3.org/2001/sw/wiki/RDF | the basic RDF constructs |
| [rdfs](https://www.w3.org/TR/rdf-schema/) | https://www.w3.org/TR/rdf-schema/ | expresses class relations, domain, ranges, etc. |
| [owl](https://www.w3.org/OWL/) | https://www.w3.org/OWL/ | for more elaborate ontologies |
| [vann](http://vocab.org/vann/) | https://vocab.org/vann/ | for annotating vocabulary descriptons |
| [dc](http://purl.org/dc/elements/1.1/) | http://purl.org/dc/elements/1.1/ | elements of [Dublin Core](http://dublincore.org/) metadata initiative |
| [dct](http://purl.org/dc/terms/) | http://purl.org/dc/terms/ | terms for the [Dublin Core](http://dublincore.org/) metadata initiative |
| [sh](https://www.w3.org/TR/shacl/) | https://www.w3.org/TR/shacl/ | for defining well-formedness constraints |
| [dcat](https://www.w3.org/TR/vocab-dcat/) | https://www.w3.org/TR/vocab-dcat/ | Data Catalog vocabulary |
| [foaf](http://xmlns.com/foaf/spec/) | http://xmlns.com/foaf/spec/ | the 'Friend of a Friend' vocabulary |
| [skos](https://www.w3.org/2009/08/skos-reference/skos.html) | http://www.w3.org/2004/02/skos/core# |for thesaurus-type taxonomies |
| [schema.org](https://schema.org/) | https://schema.org/ |  mostly commercial topics, with web-page metadata and search-engine indexes in mind |

<a name="h4-imported-with-wd"></a>
### Imported with _ont-app.vocabulary.wikidata_

Requiring the `ont-app.vocabulary.wikidata` module imports
declarations for the [several namespaces](https://www.mediawiki.org/wiki/Wikibase/Indexing/RDF_Dump_Format#Full_list_of_prefixes)
pertinent to the
[Wikidata](#https://www.wikidata.org/wiki/Wikidata:Main_Page) database.

It also defines the value for [Wikidata's public SPARQL endpoint](https://query.wikidata.org/bigdata/namespace/wdq/sparql) is
as the constant:

`ont-app.vocabulary.wikidata/sparql-endpoint`

<a name="h4-imported-with-ling"></a>
### Imported with _ont-app.vocabulary.linguistics_ 

The `ont-app.vocabulary.linguistics` module declares namespaces for:

| PREFIX | URI | Comments |
| --- | --- | --- |
| [ontolex](https://www.w3.org/2016/05/ontolex/) | http://www.w3.org/ns/lemon/ontolex# | for encoding lexical data | 
| [pmn](http://premon.fbk.eu/ontology/core.html) | http://premon.fbk.eu/ontology/core# | PreMOn - dedicated to describing English verbs |
| [nif](http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html) | http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#  | Natural Language Interchange Format - for annotating corpora |


There are also a set of namespaces particular to my Natural Lexicon
project, which are still under development.
    
<a name="h2-language-tagged-strings"></a>
## Language-tagged strings

RDF entails use of language-tagged strings (e.g. `"gaol"@en-GB`) when
providing natural-language content. Typing this directly in Clojure
code is a bit a bit awkward, since the inner quotes would need to be
escaped.

To enable this language tag, we must require the namespace:

```
(require ...
  [ont-app.vocabulary.lstr :refer [lang]]
  )
```

This library defines a reader macro `#lstr` and accompanying record
_LangStr_ to facilitate wriing language-tagged strings in clojure. The
value above for example would be written: `#lstr "gaol@en-GB"`.

The reader encodes and instance of type LangStr (it is autoiconic):

```
> (def brit-jail #lstr "gaol@en-GB")
brit-jail
> brit-jail
#lstr "gaol@en-GB"
> (type brit-jail)
ont_app.vocabulary.lstr.LangStr
>
```

Rendered as a string, the language tag is dropped

```
> (str #lstr "gaol@en-GB")
"gaol"
>
```

We get the language tag with `lang`:

```
> (lang #lstr "gaol@en-GB")
"en-GB"
>
```

<a name="h2-license"></a>
## License

Copyright © 2019-20 Eric D. Scott

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.


<table>
<tr>
<td width=75>
<img src="http://ericdscott.com/NaturalLexiconLogo.png" alt="Natural Lexicon logo" :width=50 height=50/> </td>
<td>
<p>Natural Lexicon logo - Copyright © 2020 Eric D. Scott. Artwork by Athena M. Scott.</p>
<p>Released under <a href="https://creativecommons.org/licenses/by-sa/4.0/">Creative Commons Attribution-ShareAlike 4.0 International license</a>. Under the terms of this license, if you display this logo or derivates thereof, you must include an attribution to the original source, with a link to https://github.com/ont-app, or  http://ericdscott.com. </p> 
</td>
</tr>
<table>
