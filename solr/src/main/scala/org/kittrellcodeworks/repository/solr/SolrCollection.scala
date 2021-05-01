package org.kittrellcodeworks.repository.solr

import org.apache.solr.client.solrj.response.{QueryResponse, UpdateResponse}
import org.apache.solr.client.solrj.{SolrClient, SolrRequest}
import org.apache.solr.common.params.SolrParams
import org.apache.solr.common.util.NamedList
import org.apache.solr.common.{SolrDocument, SolrDocumentList, SolrInputDocument}

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.concurrent.{ExecutionContext, Future}

/**
 * Wraps commands to a
 */
class SolrCollection(client: SolrClient, collectionName: String)(implicit ec: ExecutionContext) {

  // allows only a single operation on the collection at any given time.
  // Assumes that only one SolrCollection will be created per collection per client.
  private object latch

  private def Latched[T](body: => T): Future[T] = Future(latch.synchronized(body))

  def addOne(doc: SolrInputDocument, commitWithinMs: Int = -1): Future[UpdateResponse] = Latched {
    client.add(collectionName, doc, commitWithinMs)
    client.commit(collectionName)
  }

  def add(docs: Seq[SolrInputDocument], commitWithinMs: Int = -1): Future[UpdateResponse] = {
    if (docs.isEmpty)
      Future.successful { val r = new UpdateResponse(); r.setResponse(new NamedList[AnyRef]()); r }
    else {
      val docList: java.util.Collection[SolrInputDocument] = docs.asJava
      Latched {
        client.add(collectionName, docList, commitWithinMs)
        client.commit(collectionName)
      }
    }
  }

  def commit(waitFlush: Boolean = true, waitSearcher: Boolean = true, softCommit: Boolean = false): Future[UpdateResponse] =
    Latched(client.commit(collectionName, waitFlush, waitSearcher, softCommit))

  def optimize(waitFlush: Boolean = true, waitSearcher: Boolean = true, maxSegments: Int = 1): Future[UpdateResponse] =
    Latched(client.optimize(collectionName, waitFlush, waitSearcher, maxSegments))

  def rollback(): Future[UpdateResponse] =
    Latched(client.rollback(collectionName))

  def deleteById(id: String, commitWithinMs: Int = -1): Future[UpdateResponse] = Latched {
    client.deleteById(collectionName, id, commitWithinMs)
    client.commit(collectionName)
  }

  def deleteByIds(ids: Seq[String], commitWithinMs: Int = -1): Future[UpdateResponse] = Latched {
    client.deleteById(ids.asJava, commitWithinMs)
    client.commit(collectionName)
  }

  def deleteByQuery(query: String, commitWithinMs: Int = -1): Future[UpdateResponse] = Latched {
    client.deleteByQuery(collectionName, query, commitWithinMs)
    client.commit(collectionName)
  }

  def query(params: SolrParams, method: SolrRequest.METHOD = SolrRequest.METHOD.GET): Future[QueryResponse] =
    Latched(client.query(collectionName, params, method))

  def getById(id: String, params: SolrParams = null): Future[SolrDocument] =
    Latched(client.getById(collectionName, id, params))

  def getByIds(ids: Seq[String], params: SolrParams = null): Future[SolrDocumentList] =
    Latched(client.getById(collectionName, ids.asJava, params))

}
