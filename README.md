# Geomesa Database Web App Example

This Repo contains a simple Webapp example developed using the Geomesa and Hbase.

The Webapp has get and push type of functions implemented, where data can be send in Json format and queries later.

## Geomesa
GeoMesa is an open-source, distributed, spatio-temporal database built on a number of distributed cloud data storage systems, including Accumulo, HBase, Cassandra, and Kafka. Leveraging a highly parallelized indexing strategy, GeoMesa aims to provide as much of the spatial querying and data manipulation to Accumulo/Hbase as PostGIS does to Postgres.
GeoMesa does this by providing spatio-temporal data persistence on top of the Accumulo, HBase, and Cassandra distributed column-oriented databases for massive storage of point, line, and polygon data. It allows rapid access to this data via queries that take full advantage of geographical properties to specify distance and area. GeoMesa also provides support for near real time stream processing of spatio-temporal data by layering spatial semantics on top of the Apache Kafka messaging system.

## Hbase
Apache HBase is the Hadoop database, a distributed, scalable, big data store.

Use Apache HBase™ when you need random, realtime read/write access to your Big Data. This project's goal is the hosting of very large tables -- billions of rows X millions of columns -- atop clusters of commodity hardware. Apache HBase is an open-source, distributed, versioned, non-relational database modeled after Google's Bigtable: A Distributed Storage System for Structured Data by Chang et al. Just as Bigtable leverages the distributed data storage provided by the Google File System, Apache HBase provides Bigtable-like capabilities on top of Hadoop and HDFS. 

## Building
This is a apache Maven project, and can be compiled and tested using the mvn commands.
Requires, the Hbase database installed on local machine, at the default port running.
