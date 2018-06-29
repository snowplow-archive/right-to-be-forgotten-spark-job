# Right To Be Forgotten (R2F) Spark Job

[![Codacy Badge][codacy-badge]][codacy-dashboard]
[![Build Status][travis-image]][travis]
[![Release][release-image]][releases]
[![License][license-image]][license]

## Introduction

This is the Snowplow Right To Be Forgotten (R2F) Spark Job.

## Technical overview

Right To Be Forgotten is written in Scala and uses [Spark][spark].

The process can be run on [Amazon Elastic MapReduce][emr].

## Building

Assuming you already have SBT installed:

    $ git clone git://github.com/snowplow-incubator/right-to-be-forgotten-spark-job.git
    $ sbt assembly

The 'fat jar' is now available as:

    target/snowplow-right-to-be-forgotten-job-x.x.x.jar

## Running

To run the job in your spark cluster, you will need the above mentoned jar to be uploaded to your master node (or other node that has access) of your cluster and you can simply run it using `spark-submit`.

Here is one example of using spark submit to run that job:

```bash
spark-submit --master yarn --deploy-mode client ./snowplow-right-to-be-forgotten-job-0.1.0.jar --r2f-data-file s3://snowplow-data-<mycompany>/config/to_be_forgotten.json --input-directory s3://snowplow-data-<mycompany>/enriched/archive/ --non-matching-output-directory s3://snowplow-data-<mycompany>/r2f-test/non-matching/runid=<yyyy-mm-dd-HH-MM-SS> --matching-output-directory s3://snowplow-data-<mycompany>/r2f-test/matching/runid=<yyyy-mm-dd-HH-MM-SS> --maximum-matching-proportion 0.01
```

This process does not preserve the directory structure under the `enrihed archive` (namely the `run=<runid>` subfolders).

## Find out more

| Technical Docs              | Setup Guide           |
|-----------------------------|-----------------------|

## Copyright and license

Copyright 2018 Snowplow Analytics Ltd.

Licensed under the [Apache License, Version 2.0][license] (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[spark]: http://spark.apache.org/
[snowplow]: http://snowplowanalytics.com
[emr]: http://aws.amazon.com/elasticmapreduce/

[license]: http://www.apache.org/licenses/LICENSE-2.0

[codacy-badge]: https://api.codacy.com/project/badge/Grade/422d7981055243a4abb8530306904dc2
[codacy-dashboard]: https://www.codacy.com/project/snowplow/right-to-be-forgotten-spark-job/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=snowplow-incubator/right-to-be-forgotten-spark-job&amp;utm_campaign=Badge_Grade_Dashboard

[travis-image]: https://travis-ci.org/snowplow-incubator/snowplow-right-to-be-forgotten-job.svg?branch=master
[travis]: https://travis-ci.org/snowplow-incubator/snowplow-right-to-be-forgotten-job

[release-image]: https://img.shields.io/badge/release-0.1.0-orange.svg?style=flat
[releases]: https://github.com/snowplow-incubator/snowplow-right-to-be-forgotten-job/releases

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0