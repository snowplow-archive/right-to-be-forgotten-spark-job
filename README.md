# Right To Be Forgotten (R2F) Spark Job

[![Codacy Badge][codacy-badge]][codacy-dashboard]
[![Build Status][travis-image]][travis]
[![Release][release-image]][releases]
[![License][license-image]][license]

## Introduction

This is the Snowplow Right To Be Forgotten (R2F) Spark Job. It filters out snowplow events containing the personal identifiers specified in the "removal criteria" file.

Optionally it can also output the events that are filtered out in a separate directory.

It also contains a safeguard so that you don't accidentally filter out more data than you actually want.

## Technical overview

Right To Be Forgotten is written in Scala and uses [Spark][spark].

It is implemented as a simple spark job that reads all input and filters it based on a UDF filter using data (called *removal criteria*) that is loaded on the driver. 

Currently this is very efficient as we expect the remova criteria data to remain small.

The process can be run on [Amazon Elastic MapReduce][emr] or any other spark installation.

## Building

Assuming you already have SBT installed:

    $ git clone git://github.com/snowplow-incubator/right-to-be-forgotten-spark-job.git
    ..
    $ sbt assembly
    ..

The 'fat jar' is now available as:

    target/snowplow-right-to-be-forgotten-job-x.x.x.jar

## Running

To run the job in your spark cluster, you will need the above mentoned jar to be uploaded to your master node (or other node that has access) of your cluster and you can simply run it using `spark-submit`.

Here is one example of using spark submit to run that job:

```bash
spark-submit \
    --master yarn \
    --deploy-mode client ./snowplow-right-to-be-forgotten-job-0.1.0.jar \
    --removal-criteria s3://snowplow-data-<mycompany>/config/to_be_forgotten.json \
    --input-directory s3://snowplow-data-<mycompany>/enriched/archive/ \
    --non-matching-output-directory s3://snowplow-data-<mycompany>/r2f-test/non-matching/runid=<yyyy-mm-dd-HH-MM-SS> \
    --matching-output-directory s3://snowplow-data-<mycompany>/r2f-test/matching/runid=<yyyy-mm-dd-HH-MM-SS> \
    --maximum-matching-proportion 0.01
```

The R2F arguments are:

* `--removal-criteria` (in this example `s3://snowplow-data-<mycompany>/config/to_be_forgotten.json`):
    This is the URL of the removal criteria file containing the criteria for which an event will be removed form the archive.
* `--input-directory` (in this example `s3://snowplow-data-<mycompany>/enriched/archive/`):
    The directory that contains the snowplow data input
* `--non-matching-output-directory` (in this case `s3://snowplow-data-<mycompany>/r2f-test/non-matching/runid=<yyyy-mm-dd-HH-MM-SS>`):
    The directory that contains all data that do not match the criteria
* (Optional) `--matching-output-directory` (in this case `s3://snowplow-data-<mycompany>/r2f-test/matching/runid=<yyyy-mm-dd-HH-MM-SS>`):
    The directory that contains the matching output
* `--maximum-matching-proportion` (In this case `0.01`):
    The maximum proportion of the input events that are allowed to match. If the actual proportion is higher the job will fail.

This process does not preserve the directory structure under the `enriched archive` (namely the `run=<runid>` subfolders).

## Find out more

|  **[Technical Docs][techdocs]**  |  **[Setup Guide][setup]**         |
|-----------------------------|-----------------------|
| [![i1][techdocs-image]][techdocs] | [![i2][setup-image]][setup] |

## Copyright and license

Copyright 2018 Snowplow Analytics Ltd.

Licensed under the [Apache License, Version 2.0][license] (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[techdocs]: https://github.com/snowplow-incubator/right-to-be-forgotten-spark-job/wiki/Technical-documentation
[setup]: https://github.com/snowplow-incubator/right-to-be-forgotten-spark-job/wiki/Setup-Guide
[techdocs-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/techdocs.png
[setup-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/setup.png

[spark]: http://spark.apache.org/
[snowplow]: http://snowplowanalytics.com
[emr]: http://aws.amazon.com/elasticmapreduce/

[license]: http://www.apache.org/licenses/LICENSE-2.0

[codacy-badge]: https://api.codacy.com/project/badge/Grade/422d7981055243a4abb8530306904dc2
[codacy-dashboard]: https://www.codacy.com/project/snowplow/right-to-be-forgotten-spark-job/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=snowplow-incubator/right-to-be-forgotten-spark-job&amp;utm_campaign=Badge_Grade_Dashboard

[travis-image]: https://travis-ci.org/snowplow-incubator/snowplow-right-to-be-forgotten-spark-job.svg?branch=master
[travis]: https://travis-ci.org/snowplow-incubator/right-to-be-forgotten-spark-job

[release-image]: https://img.shields.io/badge/release-0.1.0-orange.svg?style=flat
[releases]: https://github.com/snowplow-incubator/snowplow-right-to-be-forgotten-job/releases

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0
