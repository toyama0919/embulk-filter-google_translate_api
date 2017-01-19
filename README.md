# Google Translate Api filter plugin for Embulk

Google Translate Api filter plugin for Embulk.

see support language. [Google Language Codes \- tomihasa](https://sites.google.com/site/tomihasa/google-language-codes)

## Overview

* **Plugin type**: filter

## Configuration

- **key_names**: target key names (array, required)
- **out_key_name_suffix**: translated target key names suffix (string, required)
- **source_lang**: source language (string, default: `null`)
- **target_lang**: target language (string, required)
- **model**: if premium edition can use. nmt(neural machine translation) or base. (string, default: `null`)
- **sleep**: delay per record, define milliseconds. (integer, default: 0)
- **google_api_key**: google_api_key. support environment variable. please `export GOOGLE_API_KEY`(string, default: `null`)

## Example

#### input
```yaml
- {
    sentence1: 'Embulk supports plugins to add functions',
    sentence2: 'Embulk is a parallel bulk data loader that helps data transfer between various storages, databases, NoSQL and cloud services.',
    sentence3: 'You can share the plugins to keep your custom scripts readable, maintainable, and reusable.',
    json_column: ['aaa', 'bbbb', 'cccc']
  }
- {
    sentence1: 'Automatic guessing of input file formats',
    sentence2: 'Parallel & distributed execution to deal with big data sets',
    json_column: ['aaa', 'bbbb', 'cccc']
  }

```

#### setting
```yaml
filters:
  - type: google_translate_api
    key_names:
     - sentence1
     - sentence2
     - sentence3
    out_key_name_suffix: _translated
    source_lang: en
    target_lang: ja
    sleep: 1000
    google_api_key: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

#### output
```
*************************** 1 ***************************
           sentence1 (string) : Embulk supports plugins to add functions
           sentence2 (string) : Embulk is a parallel bulk data loader that helps data transfer between various storages, databases, NoSQL and cloud services.
           sentence3 (string) : You can share the plugins to keep your custom scripts readable, maintainable, and reusable.
         json_column (  json) : ["aaa","bbbb","cccc"]
sentence1_translated (string) : Embulkは、機能を追加するためのプラグインをサポートしています
sentence2_translated (string) : Embulkは、さまざまなストレージ、データベース、NoSQLのとクラウドサービス間のデータ転送を助けるパラレル・バルク・データ・ローダーです。
sentence3_translated (string) : あなたは、読み込み可能な保守性、および再利用可能なカスタムスクリプトを維持するためのプラグインを共有することができます。
*************************** 2 ***************************
           sentence1 (string) : Automatic guessing of input file formats
           sentence2 (string) : Parallel & distributed execution to deal with big data sets
           sentence3 (string) :
         json_column (  json) : ["aaa","bbbb","cccc"]
sentence1_translated (string) : 入力ファイル形式の自動推測
sentence2_translated (string) : ビッグデータ・セットに対処するための並列分散実行
sentence3_translated (string) :
embulk preview -G -b embulk_bundle -I  tmp/test_translate.yml.liquid  10.86s user 0.68s system 115% cpu 9.991 total
```

## Example(Multi language combined)

#### input
```yaml
- {
    sentence1: 'Embulk is a Java application.',
    sentence2: 'Embulk ist eine Java-Anwendung.',
    sentence3: 'Embulk是Java应用程序。',
    json_column: ['aaa', 'bbbb', 'cccc']
  }

```

#### setting
```yaml
filters:
  - type: google_translate_api
    key_names:
     - sentence1
     - sentence2
     - sentence3
    out_key_name_suffix: _translated
    target_lang: ja
    sleep: 1000
    google_api_key: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

* If not define source_lang, auto detect language.

#### output
```
*************************** 1 ***************************
           sentence1 (string) : Embulk is a Java application.
           sentence2 (string) : Embulk ist eine Java-Anwendung.
           sentence3 (string) : Embulk是Java应用程序。
         json_column (  json) : ["aaa","bbbb","cccc"]
sentence1_translated (string) : Embulkは、Javaアプリケーションです。
sentence2_translated (string) : Embulkは、Javaアプリケーションです。
sentence3_translated (string) : Embulkは、Javaアプリケーションです。
```


## Build

```
$ ./gradlew gem  # -t to watch change of files and rebuild continuously
```
