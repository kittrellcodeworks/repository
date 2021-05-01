package org.kittrellcodeworks.repository.solr

import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.response.SolrPingResponse

import scala.concurrent.{ExecutionContext, Future}

class SolrConnection(client: SolrClient)(implicit ec: ExecutionContext) {

  private var collections = Map.empty[String, SolrCollection]

  def collection(collectionName: String): SolrCollection = collections.getOrElse(collectionName, {
    val coll = new SolrCollection(client, collectionName)
    collections += collectionName -> coll
    coll
  })

  def ping(): Future[SolrPingResponse] = Future(client.ping())

  def close(): Future[Unit] =
    Future(client.close()) // potentially results in Future.failed(IOException)

}
